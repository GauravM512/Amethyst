package dev.anthonyhfm.amethyst.hub.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HubImageCacheTest {
    @Test
    fun repeatedLoadsAndNewCacheInstancesReuseTheDownloadedImage() = runTest {
        Fixture().use { fixture ->
            val cache = fixture.cache(scope = backgroundScope)

            assertEquals(expected = "thumbnail", actual = cache.load(url = "thumbnail"))
            assertEquals(expected = "thumbnail", actual = cache.load(url = "thumbnail"))
            assertEquals(expected = 1, actual = fixture.requests)

            val reopened = fixture.cache(scope = backgroundScope)
            assertEquals(expected = "thumbnail", actual = reopened.load(url = "thumbnail"))
            assertEquals(expected = 1, actual = fixture.requests)
        }
    }

    @Test
    fun simultaneousLoadsShareOneRequestEvenWhenAConsumerIsCancelled() = runTest {
        Fixture().use { fixture ->
            val gate = CompletableDeferred<Unit>()
            val cache = fixture.cache(
                scope = backgroundScope,
                fetch = { url ->
                    fixture.requests++
                    gate.await()
                    url.encodeToByteArray()
                },
            )

            val first = async { cache.load(url = "avatar") }
            val second = async { cache.load(url = "avatar") }
            runCurrent()
            assertEquals(expected = 1, actual = fixture.requests)

            first.cancelAndJoin()
            gate.complete(value = Unit)
            assertEquals(expected = "avatar", actual = second.await())
            assertEquals(expected = 1, actual = fixture.requests)
        }
    }

    @Test
    fun expiredImagesAreRefetchedFromMemoryAndAfterRestart() = runTest {
        Fixture().use { fixture ->
            val cache = fixture.cache(scope = backgroundScope)
            cache.load(url = "thumbnail")
            fixture.now += 1001
            cache.load(url = "thumbnail")
            assertEquals(expected = 2, actual = fixture.requests)

            fixture.now += 1001
            fixture.cache(scope = backgroundScope).load(url = "thumbnail")
            assertEquals(expected = 3, actual = fixture.requests)
        }
    }

    @Test
    fun diskHitsDoNotExtendTheOriginalExpiry() = runTest {
        Fixture().use { fixture ->
            fixture.cache(scope = backgroundScope).load(url = "thumbnail")
            fixture.now += 900
            val reopened = fixture.cache(scope = backgroundScope)
            reopened.load(url = "thumbnail")
            assertEquals(expected = 1, actual = fixture.requests)

            fixture.now += 101
            reopened.load(url = "thumbnail")
            assertEquals(expected = 2, actual = fixture.requests)
        }
    }

    @Test
    fun failedDownloadsAreRetriedWithoutCachingTheError() = runTest {
        Fixture().use { fixture ->
            val cache = fixture.cache(
                scope = backgroundScope,
                fetch = { url ->
                    fixture.requests++
                    check(value = fixture.requests > 1)
                    url.encodeToByteArray()
                },
            )

            assertFailsWith<IllegalStateException> { cache.load(url = "avatar") }
            assertEquals(expected = "avatar", actual = cache.load(url = "avatar"))
            assertEquals(expected = 2, actual = fixture.requests)
        }
    }

    @Test
    fun corruptDiskEntriesAreReplacedByAValidDownload() = runTest {
        Fixture().use { fixture ->
            fixture.cache(scope = backgroundScope).load(url = "thumbnail")
            fixture.directory.listFiles()!!.single().writeText(text = "bad")

            val reopened = fixture.cache(scope = backgroundScope)
            assertEquals(expected = "thumbnail", actual = reopened.load(url = "thumbnail"))
            assertEquals(expected = 2, actual = fixture.requests)
            assertEquals(
                expected = "thumbnail",
                actual = fixture.cache(scope = backgroundScope).load(url = "thumbnail"),
            )
            assertEquals(expected = 2, actual = fixture.requests)
        }
    }

    @Test
    fun invalidationRemovesMemoryAndDiskAndCancelsAnOlderRequest() = runTest {
        Fixture().use { fixture ->
            val gate = CompletableDeferred<Unit>()
            val cache = fixture.cache(
                scope = backgroundScope,
                fetch = { url ->
                    fixture.requests++
                    if (fixture.requests == 1) {
                        gate.await()
                    }
                    url.encodeToByteArray()
                },
            )

            val first = async { cache.load(url = "avatar") }
            runCurrent()
            cache.invalidate(url = "avatar")
            gate.complete(value = Unit)
            first.join()
            assertTrue(actual = first.isCancelled)
            assertEquals(expected = "avatar", actual = cache.load(url = "avatar"))
            assertEquals(expected = 2, actual = fixture.requests)

            cache.invalidate(url = "avatar")
            assertTrue(actual = fixture.directory.listFiles()!!.isEmpty())
            cache.load(url = "avatar")
            assertEquals(expected = 3, actual = fixture.requests)
        }
    }

    @Test
    fun memoryEvictionKeepsTheMostRecentlyUsedImageAndFallsBackToDisk() = runTest {
        Fixture().use { fixture ->
            var decodes = 0
            val cache = fixture.cache(
                scope = backgroundScope,
                memoryLimit = 2,
                decode = { bytes ->
                    decodes++
                    bytes.decodeToString()
                },
            )

            cache.load(url = "a")
            cache.load(url = "b")
            cache.load(url = "a")
            cache.load(url = "c")
            cache.load(url = "a")
            assertEquals(expected = 3, actual = decodes)

            cache.load(url = "b")
            assertEquals(expected = 4, actual = decodes)
            assertEquals(expected = 3, actual = fixture.requests)
        }
    }

    @Test
    fun diskStorageStaysWithinItsLimit() = runTest {
        Fixture().use { fixture ->
            val cache = fixture.cache(scope = backgroundScope, diskLimit = 20)
            cache.load(url = "one")
            cache.load(url = "two")
            cache.load(url = "three")

            assertTrue(actual = fixture.directory.listFiles()!!.sumOf { it.length() } <= 20)
        }
    }

    @Test
    fun unavailableDiskDoesNotPreventDownloadingAndMemoryCaching() = runTest {
        Fixture().use { fixture ->
            fixture.directory.delete()
            fixture.directory.writeText(text = "not a directory")
            val cache = fixture.cache(scope = backgroundScope)

            assertEquals(expected = "avatar", actual = cache.load(url = "avatar"))
            assertEquals(expected = "avatar", actual = cache.load(url = "avatar"))
            assertEquals(expected = 1, actual = fixture.requests)
        }
    }

    private class Fixture : AutoCloseable {
        val directory = Files.createTempDirectory("hub-image-cache-test").toFile()
        var requests = 0
        var now = System.currentTimeMillis()

        fun cache(
            scope: CoroutineScope,
            memoryLimit: Long = 32 * 1024 * 1024,
            diskLimit: Long = 128 * 1024 * 1024,
            decode: suspend (ByteArray) -> String = { it.decodeToString() },
            fetch: suspend (String) -> ByteArray = { url ->
                requests++
                url.encodeToByteArray()
            },
        ) = HubImageCache(
            decode = decode,
            sizeOf = { it.length.toLong() },
            directory = { directory.absolutePath.toPath() },
            fetch = fetch,
            scope = CoroutineScope(
                context = scope.coroutineContext + SupervisorJob(parent = scope.coroutineContext[Job])
            ),
            now = { now },
            memoryLimit = memoryLimit,
            diskLimit = diskLimit,
            lifetimeMillis = 1000,
        )

        override fun close() {
            directory.deleteRecursively()
        }
    }
}

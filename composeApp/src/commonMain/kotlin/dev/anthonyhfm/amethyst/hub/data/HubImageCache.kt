package dev.anthonyhfm.amethyst.hub.data

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.path
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.Clock

internal class HubImageCache<T : Any>(
    private val decode: suspend (ByteArray) -> T,
    private val sizeOf: (T) -> Long,
    private val directory: () -> Path = {
        FileKit.cacheDir.path.toPath() / "hub-images-v1"
    },
    private val fetch: suspend (String) -> ByteArray = ::fetchHubImage,
    private val scope: CoroutineScope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val memoryLimit: Long = 32L * 1024 * 1024,
    private val diskLimit: Long = 128L * 1024 * 1024,
    private val lifetimeMillis: Long = 60L * 60 * 1000,
) {
    private data class Entry<T>(val image: T, val expiresAt: Long, val size: Long)

    private val mutex = Mutex()
    private val diskMutex = Mutex()
    private val memory = mutableMapOf<String, Entry<T>>()
    private val pending = mutableMapOf<String, Deferred<T>>()
    private var memorySize = 0L
    private val mutableRevision = MutableStateFlow(value = 0L)

    val revision = mutableRevision.asStateFlow()

    suspend fun load(url: String): T {
        val task = mutex.withLock {
            memory.remove(key = url)?.let { entry ->
                if (entry.expiresAt > now()) {
                    memory[url] = entry
                    return entry.image
                }
                memorySize -= entry.size
            }

            pending.getOrPut(key = url) {
                scope.async(start = CoroutineStart.LAZY) {
                    val job = currentCoroutineContext()[Job]
                    try {
                        loadUncached(url = url)
                    } finally {
                        withContext(context = NonCancellable) {
                            mutex.withLock {
                                if (pending[url] === job) {
                                    pending.remove(key = url)
                                }
                            }
                        }
                    }
                }
            }
        }

        task.start()
        return task.await()
    }

    suspend fun invalidate(url: String) {
        mutex.withLock {
            pending.remove(key = url)?.cancel()
            memory.remove(key = url)?.let { memorySize -= it.size }

            withContext(context = Dispatchers.IO) {
                diskMutex.withLock {
                    diskOperation {
                        FileSystem.SYSTEM.delete(path = diskPath(url = url))
                    }
                }
            }

            mutableRevision.value += 1
        }
    }

    private suspend fun loadUncached(url: String): T {
        val stored = diskMutex.withLock {
            diskOperation {
                val path = diskPath(url = url)
                val metadata = FileSystem.SYSTEM.metadataOrNull(path = path)
                    ?: return@diskOperation null
                if ((metadata.size ?: 0L) !in 9..(10L * 1024 * 1024 + 8)) {
                    FileSystem.SYSTEM.delete(path = path)
                    return@diskOperation null
                }

                FileSystem.SYSTEM.read(file = path) {
                    val expiresAt = readLong()
                    if (expiresAt <= now()) {
                        null
                    } else {
                        readByteArray() to expiresAt
                    }
                }
            }
        }

        if (stored != null) {
            val image = try {
                decode(stored.first)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

            if (image != null) {
                remember(url = url, image = image, expiresAt = stored.second)
                return image
            }
        }

        val bytes = fetch(url)
        val image = decode(bytes)
        val expiresAt = now() + lifetimeMillis

        diskMutex.withLock {
            currentCoroutineContext().ensureActive()
            diskOperation {
                val path = diskPath(url = url)
                FileSystem.SYSTEM.createDirectories(dir = path.parent!!)
                val temporary = path.parent!! / "${path.name}.tmp"
                try {
                    FileSystem.SYSTEM.write(file = temporary) {
                        writeLong(v = expiresAt)
                        write(source = bytes)
                    }
                    FileSystem.SYSTEM.atomicMove(source = temporary, target = path)
                    trimDisk()
                } finally {
                    FileSystem.SYSTEM.delete(path = temporary)
                }
            }
        }

        remember(url = url, image = image, expiresAt = expiresAt)
        return image
    }

    private suspend fun remember(url: String, image: T, expiresAt: Long) {
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            memory.remove(key = url)?.let { memorySize -= it.size }
            val size = sizeOf(image)
            if (size > memoryLimit) {
                return@withLock
            }

            memory[url] = Entry(image = image, expiresAt = expiresAt, size = size)
            memorySize += size
            while (memorySize > memoryLimit) {
                val oldest = memory.keys.first()
                memorySize -= memory.remove(key = oldest)!!.size
            }
        }
    }

    private fun diskPath(url: String): Path = directory() / "${url.encodeUtf8().sha256().hex()}.image"

    private fun trimDisk() {
        val paths = FileSystem.SYSTEM.list(dir = directory())
        paths.filter { it.name.endsWith(suffix = ".tmp") }
            .forEach { FileSystem.SYSTEM.delete(path = it) }

        val files = paths
            .filter { it.name.endsWith(suffix = ".image") }
            .map { it to FileSystem.SYSTEM.metadata(path = it) }
            .sortedBy { it.second.lastModifiedAtMillis ?: 0L }
        var total = files.sumOf { it.second.size ?: 0L }

        for ((path, metadata) in files) {
            val expired = (metadata.lastModifiedAtMillis ?: 0L) + lifetimeMillis <= now()
            if (expired || total > diskLimit) {
                FileSystem.SYSTEM.delete(path = path)
                total -= metadata.size ?: 0L
            }
        }
    }

    private inline fun <R> diskOperation(block: () -> R): R? = try {
        block()
    } catch (_: IOException) {
        null
    }
}

private val hubImageHttp by lazy {
    HttpClient {
        install(plugin = HttpTimeout) {
            connectTimeoutMillis = 7_000
            socketTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
    }
}

private suspend fun fetchHubImage(url: String): ByteArray =
    hubImageHttp.prepareGet(urlString = url).execute { response ->
        check(value = response.status.isSuccess()) { "Hub image request failed: ${response.status.value}" }
        val bytes = response.bodyAsChannel().readRemaining(max = 10L * 1024 * 1024 + 1).readByteArray()
        check(value = bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) { "Invalid Hub image size" }
        bytes
    }

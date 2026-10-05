package dev.anthonyhfm.amethyst.home.ui.views

import com.sun.net.httpserver.HttpServer
import dev.anthonyhfm.amethyst.core.data.settings.GlobalSettings
import dev.anthonyhfm.amethyst.hub.data.HubArtistSummary
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectCompatibility
import dev.anthonyhfm.amethyst.hub.data.HubProjectStatus
import dev.anthonyhfm.amethyst.hub.data.HubProjectType
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopHubDownloadTest {
    @Test
    fun dropboxFileLinksAreOfferedForImport() {
        val repository = HubRepository()

        try {
            listOf(
                "https://www.dropbox.com/s/abc123/project.ame?dl=0",
                "https://dropbox.com/scl/fi/abc123/project.als?rlkey=key&dl=0",
            ).forEach { url ->
                val project = project(id = "dropbox", sha256 = "0".repeat(64)).copy(
                    packageName = null,
                    downloadUrl = null,
                    externalDownloadUrl = url,
                )

                assertTrue(actual = DesktopHubDownload.canImport(project = project, repository = repository))
                assertEquals(
                    expected = url,
                    actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
                )
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun dropboxDescriptionMetadataIsOfferedForImport() {
        val repository = HubRepository()
        val url = "https://www.dropbox.com/scl/fi/abc123/project.zip?rlkey=key&dl=0"
        val project = project(id = "dropbox-metadata", sha256 = "0".repeat(64)).copy(
            packageName = null,
            downloadUrl = null,
            description = "Project description\n<!-- glacier-meta: {\"externalDownloadUrl\":\"$url\"} -->",
        )

        try {
            assertEquals(expected = url, actual = DesktopHubDownload.externalUrl(project = project))
            assertTrue(actual = DesktopHubDownload.canImport(project = project, repository = repository))
            assertEquals(expected = "Project description", actual = DesktopHubDownload.cleanDescription(project = project))
        } finally {
            repository.close()
        }
    }

    @Test
    fun unsupportedDropboxLinksRemainAvailableAsExternalPages() {
        val repository = HubRepository()

        try {
            listOf(
                "https://dropbox.com/scl/fo/abc123/folder?rlkey=key",
                "http://dropbox.com/s/abc123/project.ame",
                "https://dropbox.com.evil.test/s/abc123/project.ame",
            ).forEach { url ->
                val project = project(id = "unsupported-dropbox", sha256 = "0".repeat(64)).copy(
                    packageName = null,
                    downloadUrl = null,
                    externalDownloadUrl = url,
                )

                assertFalse(actual = DesktopHubDownload.canImport(project = project, repository = repository))
                assertEquals(
                    expected = url,
                    actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
                )
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun hubDownloadsTakePriorityOverSupportedDropboxLinks() {
        val repository = HubRepository()
        val original = project(id = "dropbox-priority", sha256 = "0".repeat(64)).copy(
            externalDownloadUrl = "https://dropbox.com/s/abc123/project.ame?dl=0",
        )

        try {
            listOf(
                original.copy(overrideDownloadUrl = "/projects/dropbox-priority/override"),
                original.copy(packageName = null),
                original.copy(downloadUrl = null),
            ).forEach { project ->
                assertNull(actual = DesktopHubDownload.externalUrl(project = project))
                assertTrue(actual = DesktopHubDownload.canImport(project = project, repository = repository))
                assertEquals(
                    expected = repository.client.resolveUrl(
                        pathOrUrl = project.overrideDownloadUrl ?: "/projects/dropbox-priority/download",
                    ),
                    actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
                )
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun malformedDownloadLinksAreNotOfferedForImport() {
        val repository = HubRepository()
        val project = project("bad-link", "0".repeat(64)).copy(
            packageName = null,
            downloadUrl = "https://[broken",
            externalDownloadUrl = "javascript:alert(1)",
        )
        try {
            assertFalse(DesktopHubDownload.canImport(project, repository))
            assertNull(DesktopHubDownload.downloadPageUrl(project, repository))
        } finally {
            repository.close()
        }
    }

    @Test
    fun amethystOverrideTakesPriorityOverExternalDownload() {
        val repository = HubRepository()
        val project = project(id = "with-override", sha256 = "0".repeat(64)).copy(
            externalDownloadUrl = "https://example.com/original.als",
            overrideDownloadUrl = "/projects/with-override/override",
            overrideName = "converted.ame",
        )

        try {
            assertNull(DesktopHubDownload.externalUrl(project = project))
            assertTrue(DesktopHubDownload.canImport(project = project, repository = repository))
            assertEquals(
                expected = repository.client.resolveUrl(pathOrUrl = "/projects/with-override/override"),
                actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
            )
        } finally {
            repository.close()
        }
    }

    @Test
    fun nativeDownloadUrlTakesPriorityOverUnsupportedExternalSource() {
        val repository = HubRepository()
        val project = project(id = "native-package", sha256 = "0".repeat(64)).copy(
            packageName = null,
            externalDownloadUrl = "https://example.com/original.ame",
        )

        try {
            assertNull(DesktopHubDownload.externalUrl(project = project))
            assertTrue(DesktopHubDownload.canImport(project = project, repository = repository))
            assertEquals(
                expected = repository.client.resolveUrl(pathOrUrl = "/projects/native-package/download"),
                actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
            )
        } finally {
            repository.close()
        }
    }

    @Test
    fun compatibleOriginalPackageTakesPriorityOverExternalMetadata() {
        val repository = HubRepository()
        val project = project(id = "compatible-original", sha256 = "0".repeat(64)).copy(
            projectType = HubProjectType.ableton,
            packageName = "original.als",
            downloadUrl = null,
            description = "<!-- glacier-meta: {\"externalDownloadUrl\":\"https://example.com/original.als\"} -->",
        )

        try {
            assertNull(DesktopHubDownload.externalUrl(project = project))
            assertTrue(DesktopHubDownload.canImport(project = project, repository = repository))
            assertEquals(
                expected = repository.client.resolveUrl(pathOrUrl = "/projects/compatible-original/download"),
                actual = DesktopHubDownload.downloadPageUrl(project = project, repository = repository),
            )
        } finally {
            repository.close()
        }
    }

    @Test
    fun responseFilenameControlsConversionExtensionAndPendingFileCanBeDiscarded() { runBlocking {
        val bytes = byteArrayOf(0x50, 0x4b, 3, 4, 0, 1, 2, 3)
        val server = serve(bytes, "attachment; filename*=UTF-8''live-set.zip")
        val id = "download-test-${UUID.randomUUID()}"
        val repository = HubRepository(baseUrl = "http://127.0.0.1:${server.address.port}")
        val project = project(id, sha256(bytes))
        try {
            assertFalse(DesktopHubDownload.canImport(project, repository)) // Production UI requires HTTPS.
            val file = DesktopHubDownload.download(project, repository) { }
            try {
                assertEquals("zip", file.extension)
                assertContentEquals(bytes, file.readBytes())
                assertNull(GlobalSettings.mobileProjects.firstOrNull { it.id == "hub-$id" })
            } finally {
                DesktopHubDownload.discardImport(file)
            }
            assertFalse(file.exists())
        } finally {
            repository.close()
            server.stop(0)
            testDirectory(id).deleteRecursively()
        }
    } }

    @Test
    fun rarResponseFilenameControlsArchiveExtension() = runBlocking {
        val bytes = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x01, 0x00, 0, 1, 2, 3)
        val server = serve(
            bytes = bytes,
            disposition = "attachment; filename*=UTF-8''Ableton%20%26%20Apollo.rar",
        )
        val id = "download-test-${UUID.randomUUID()}"
        val repository = HubRepository(baseUrl = "http://127.0.0.1:${server.address.port}")

        try {
            val file = DesktopHubDownload.download(
                project = project(id = id, sha256 = sha256(bytes = bytes)),
                repository = repository,
                onProgress = {},
            )

            try {
                assertEquals(expected = "rar", actual = file.extension)
                assertTrue(actual = file.name.endsWith(suffix = "Ableton & Apollo.rar"))
                assertContentEquals(expected = bytes, actual = file.readBytes())
                assertNull(actual = GlobalSettings.mobileProjects.firstOrNull { it.id == "hub-$id" })
            } finally {
                DesktopHubDownload.discardImport(file = file)
            }

            assertFalse(actual = file.exists())
        } finally {
            repository.close()
            server.stop(0)
            testDirectory(id = id).deleteRecursively()
        }
    }

    @Test
    fun checksumMismatchDoesNotRegisterOrKeepDownloadedFile() { runBlocking {
        val bytes = byteArrayOf(0x50, 0x4b, 3, 4, 8, 9)
        val server = serve(bytes, "attachment; filename=project.zip")
        val id = "download-test-${UUID.randomUUID()}"
        val repository = HubRepository(baseUrl = "http://127.0.0.1:${server.address.port}")
        try {
            assertFailsWith<IllegalStateException> {
                DesktopHubDownload.download(project(id, "0".repeat(64)), repository) { }
            }
            assertNull(GlobalSettings.mobileProjects.firstOrNull { it.id == "hub-$id" })
            assertTrue(File(testDirectory(id), "Original").listFiles().isNullOrEmpty())
        } finally {
            repository.close()
            server.stop(0)
            testDirectory(id).deleteRecursively()
        }
    } }

    @Test
    fun errorPagesAreNotRegisteredOrKeptAsProjects() = runBlocking {
        val bytes = "<!DOCTYPE html><html>Sign in to Dropbox</html>".toByteArray()
        val server = serve(bytes = bytes, disposition = "attachment; filename=project.zip")
        val id = "download-test-${UUID.randomUUID()}"
        val repository = HubRepository(baseUrl = "http://127.0.0.1:${server.address.port}")

        try {
            assertFailsWith<IllegalStateException> {
                DesktopHubDownload.download(
                    project = project(id = id, sha256 = sha256(bytes = bytes)),
                    repository = repository,
                    onProgress = {},
                )
            }
            assertNull(actual = GlobalSettings.mobileProjects.firstOrNull { it.id == "hub-$id" })
            assertTrue(actual = File(testDirectory(id = id), "Original").listFiles().isNullOrEmpty())
        } finally {
            repository.close()
            server.stop(0)
            testDirectory(id = id).deleteRecursively()
        }
    }

    private fun project(id: String, sha256: String) = HubProject(
        id = id,
        slug = "test",
        title = "Test",
        description = "",
        compatibility = HubProjectCompatibility.compatible,
        status = HubProjectStatus.public,
        artist = HubArtistSummary("test", "Test"),
        created = 0,
        updated = 0,
        packageName = "suggested.ame",
        packageSha256 = sha256,
        downloadUrl = "/projects/$id/download",
    )

    private fun serve(bytes: ByteArray, disposition: String): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                exchange.responseHeaders.add("Content-Type", "application/octet-stream")
                exchange.responseHeaders.add("Content-Disposition", disposition)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun testDirectory(id: String): File {
        val home = System.getProperty("user.home")
        val dataRoot = when {
            System.getProperty("os.name").lowercase().contains("mac") -> File(home, "Library/Application Support")
            System.getProperty("os.name").lowercase().contains("win") -> File(System.getenv("APPDATA") ?: home)
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share")
        }
        return File(dataRoot, "Amethyst/Hub/$id")
    }
}

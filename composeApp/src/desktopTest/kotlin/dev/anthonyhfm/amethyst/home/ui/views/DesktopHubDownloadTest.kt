package dev.anthonyhfm.amethyst.home.ui.views

import com.sun.net.httpserver.HttpServer
import dev.anthonyhfm.amethyst.core.data.settings.GlobalSettings
import dev.anthonyhfm.amethyst.hub.data.HubArtistSummary
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectCompatibility
import dev.anthonyhfm.amethyst.hub.data.HubProjectStatus
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

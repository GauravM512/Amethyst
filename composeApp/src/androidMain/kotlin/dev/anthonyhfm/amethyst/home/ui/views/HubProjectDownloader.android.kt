package dev.anthonyhfm.amethyst.home.ui.views

import android.net.Uri
import android.util.Base64
import dev.anthonyhfm.amethyst.core.util.MobileFileStorage
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.data.MobileProjectRecord
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.UUID

/** Streams a Hub project into the same persistent catalog used by the Projects tab. */
internal object HubProjectDownloader {
    private val supportedExtensions = setOf("ame", "als", "approj", "zip")

    suspend fun download(
        repository: HubRepository,
        project: HubProject,
        externalUrl: String?,
        onProgress: suspend (Float) -> Unit,
    ): PlatformFile = withContext(Dispatchers.IO) {
        val source = sourceUrl(repository, project, externalUrl)
        val expectedSize = project.overrideSize ?: project.packageSize
        val suggestedName = project.overrideName ?: project.packageName ?: "${project.title}.${extensionFor(project)}"

        val directory = File(MobileFileStorage.getAmethystDirectory(), "Hub/${project.id}/Original")
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create project directory" }
        var target: File? = null
        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream, application/zip, */*")
        }
        try {
            check(connection.responseCode in 200..299) { "Download failed: ${connection.responseCode}" }
            val mime = connection.contentType?.substringBefore(';')?.lowercase().orEmpty()
            check(mime !in setOf("text/html", "text/plain", "application/json")) { "Not a project file" }
            val responseName = connection.getHeaderField("Content-Disposition")?.let(::responseFilename)
            val extension = responseName?.let(::supportedExtension)
                ?: supportedExtension(suggestedName)
                ?: error("Unsupported project format")
            val destination = File(directory, "${UUID.randomUUID()}.$extension")
            target = destination
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize?.takeIf { it > 0 }
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var reportedPercent = -1
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var firstChunk = true
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (firstChunk) {
                            firstChunk = false
                            val prefix = String(buffer, 0, minOf(count, 128)).trimStart().lowercase()
                            check(count > 0 && !listOf("<!doctype html", "<html", "<?xml", "{", "[").any(prefix::startsWith)) {
                                "Not a project file"
                            }
                        }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        if (total != null) {
                            val percent = (downloaded * 100 / total).coerceIn(0, 100).toInt()
                            if (percent != reportedPercent) {
                                reportedPercent = percent
                                withContext(Dispatchers.Main) { onProgress(percent / 100f) }
                            }
                        }
                    }
                }
            }
            check(downloaded > 0) { "Empty project file" }
            val hash = digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
            HomeRepository.registerMobileProject(
                MobileProjectRecord(
                    id = "hub-${project.id}",
                    title = project.title,
                    originalPath = destination.absolutePath,
                    importedAt = System.currentTimeMillis(),
                    hubProjectId = project.id,
                    sourceHash = hash,
                )
            )
            withContext(Dispatchers.Main) { onProgress(1f) }
            PlatformFile(destination.absolutePath)
        } catch (error: Exception) {
            target?.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun canImport(repository: HubRepository, project: HubProject, externalUrl: String?): Boolean {
        if (!externalUrl.isNullOrBlank()) {
            val uri = Uri.parse(externalUrl)
            return uri.scheme == "https" && uri.host?.lowercase() in setOf(
                "drive.google.com", "www.drive.google.com", "drive.usercontent.google.com",
                "mediafire.com", "www.mediafire.com", "m.mediafire.com",
            )
        }
        val path = project.overrideDownloadUrl ?: project.downloadUrl
            ?: project.packageName?.let { "/projects/${project.id}/download" } ?: return false
        val uri = Uri.parse(repository.client.resolveUrl(path))
        return uri.scheme == "https" && uri.host == Uri.parse(repository.client.resolveUrl("/")).host
    }

    private fun extensionFor(project: HubProject): String = when (project.projectType.name) {
        "ableton" -> "als"
        "apollo" -> "approj"
        "unipad" -> "zip"
        else -> "ame"
    }

    private fun supportedExtension(filename: String): String? =
        filename.substringAfterLast('/').substringAfterLast('\\').substringAfterLast('.', "")
            .lowercase().takeIf { it in supportedExtensions }

    private fun responseFilename(header: String): String? {
        val encoded = Regex("filename\\*=[^']*''([^;]+)", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.get(1)
        if (encoded != null) return runCatching { URLDecoder.decode(encoded.trim(), "UTF-8") }.getOrNull()
        return Regex("filename\\s*=\\s*\\\"?([^\\\";]+)", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.get(1)?.trim()
    }

    private fun sourceUrl(repository: HubRepository, project: HubProject, externalUrl: String?): String {
        val raw = externalUrl?.takeIf(String::isNotBlank)
        if (raw != null) {
            val uri = Uri.parse(raw)
            require(uri.scheme == "https")
            val host = uri.host?.lowercase().orEmpty()
            if (host in setOf("drive.google.com", "www.drive.google.com", "drive.usercontent.google.com")) {
                val segments = uri.pathSegments
                val id = if (segments.size >= 3 && segments[0] == "file" && segments[1] == "d") segments[2]
                    else uri.getQueryParameter("id")
                require(id != null && id.matches(Regex("[A-Za-z0-9_-]+")))
                return Uri.parse("https://drive.usercontent.google.com/download").buildUpon()
                    .appendQueryParameter("id", id).appendQueryParameter("export", "download")
                    .appendQueryParameter("confirm", "t")
                    .apply { uri.getQueryParameter("resourcekey")?.let { appendQueryParameter("resourcekey", it) } }
                    .build().toString()
            }
            if (host in setOf("mediafire.com", "www.mediafire.com", "m.mediafire.com")) {
                return resolveMediaFire(uri)
            }
            error("External source is not directly importable")
        }
        val path = project.overrideDownloadUrl ?: project.downloadUrl
            ?: project.packageName?.let { "/projects/${project.id}/download" }
            ?: error("No download available")
        val url = repository.client.resolveUrl(path)
        val uri = Uri.parse(url)
        require(uri.scheme == "https" && uri.host == Uri.parse(repository.client.resolveUrl("/")).host)
        return url
    }

    private fun resolveMediaFire(uri: Uri): String {
        val segments = uri.pathSegments
        require(segments.size >= 2 && segments[0] in setOf("file", "download"))
        val key = segments[1]
        require(key.length in 10..20 && key.all(Char::isLetterOrDigit))
        val api = "https://www.mediafire.com/api/1.5/file/get_info.php?quick_key=$key&response_format=json"
        val payload = JSONObject(readText(api)).getJSONObject("response")
        require(payload.getString("result") == "Success")
        val info = payload.getJSONObject("file_info")
        require(info.getString("privacy") == "public" && info.getString("password_protected") == "no")
        val page = info.getJSONObject("links").getString("normal_download")
        require(Uri.parse(page).scheme == "https" && Uri.parse(page).host in setOf("mediafire.com", "www.mediafire.com", "m.mediafire.com"))
        val html = readText(page)
        val tag = Regex("<a\\b(?=[^>]*\\bid=[\\\"']downloadButton[\\\"'])[^>]*>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.value ?: error("MediaFire download unavailable")
        val href = Regex("\\bhref=[\\\"']([^\\\"']+)[\\\"']").find(tag)?.groupValues?.get(1)?.replace("&amp;", "&")
        val scrambled = Regex("\\bdata-scrambled-url=[\\\"']([^\\\"']+)[\\\"']").find(tag)?.groupValues?.get(1)
        val url = href ?: scrambled?.let { String(Base64.decode(it, Base64.DEFAULT)) } ?: error("MediaFire download unavailable")
        val host = Uri.parse(url).host.orEmpty()
        require(Uri.parse(url).scheme == "https" && host.matches(Regex("download[0-9]+\\.mediafire\\.com")))
        return url
    }

    private fun readText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
        }
        return try {
            check(connection.responseCode in 200..299)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }
}

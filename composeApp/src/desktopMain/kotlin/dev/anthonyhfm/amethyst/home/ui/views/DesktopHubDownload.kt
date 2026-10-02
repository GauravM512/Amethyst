package dev.anthonyhfm.amethyst.home.ui.views

import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.core.data.settings.GlobalSettings
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.data.MobileProjectRecord
import dev.anthonyhfm.amethyst.home.data.DesktopProjectStorage
import dev.anthonyhfm.amethyst.home.data.downloadedDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object DesktopHubDownload {
    private val extensions = setOf("ame", "als", "zip", "approj")
    private val driveHosts = setOf("drive.google.com", "www.drive.google.com", "drive.usercontent.google.com")
    private val mediaFireHosts = setOf("mediafire.com", "www.mediafire.com", "m.mediafire.com")
    private val pendingImports = ConcurrentHashMap<String, MobileProjectRecord>()

    fun externalUrl(project: HubProject): String? {
        if (
            project.overrideDownloadUrl != null ||
            project.downloadUrl != null ||
            project.packageName != null
        ) {
            return null
        }

        return project.externalDownloadUrl?.trim()?.takeIf(::isWebUrl)
            ?: Regex("<!--\\s*glacier-meta:\\s*(\\{[\\s\\S]*?})\\s*-->")
            .find(project.description)?.groupValues?.getOrNull(1)
            ?.let { runCatching { Json.parseToJsonElement(it).jsonObject["externalDownloadUrl"]?.jsonPrimitive?.contentOrNull }.getOrNull() }
            ?.trim()?.takeIf(::isWebUrl)
    }

    fun cleanDescription(project: HubProject): String = project.description
        .replace(Regex("<!--\\s*glacier-meta:\\s*\\{[\\s\\S]*?}\\s*-->"), "")
        .trim()

    fun canImport(project: HubProject, repository: HubRepository): Boolean {
        val external = externalUrl(project)
        if (external != null) {
            return driveId(external) != null || mediaFireKey(external) != null
        }

        val url = project.overrideDownloadUrl
            ?: project.downloadUrl
            ?: project.packageName?.let { "/projects/${project.id}/download" }
            ?: return false

        return runCatching {
            val resolved = URI.create(repository.client.resolveUrl(url))
            val hub = URI.create(repository.client.resolveUrl("/"))
            resolved.scheme == "https" && resolved.host == hub.host && resolved.port == hub.port
        }.getOrDefault(false)
    }

    fun downloadPageUrl(project: HubProject, repository: HubRepository): String? =
        externalUrl(project) ?: (project.overrideDownloadUrl
            ?: project.downloadUrl
            ?: project.packageName?.let { "/projects/${project.id}/download" })
            ?.let { runCatching { repository.client.resolveUrl(it) }.getOrNull() }
            ?.takeIf(::isWebUrl)

    fun completeImport(file: File) {
        val record = pendingImports[file.absolutePath] ?: return
        val oldPath = GlobalSettings.mobileProjects.firstOrNull { it.id == record.id }?.originalPath
        HomeRepository.registerMobileProject(record)
        pendingImports.remove(file.absolutePath)

        if (oldPath != null && oldPath != record.originalPath) {
            HomeRepository.removeRecentWorkspace(oldPath)
        }
    }

    fun discardImport(file: File) {
        if (pendingImports.remove(file.absolutePath) != null) {
            file.delete()
        }
    }

    fun openExternal(url: String) {
        val uri = runCatching { URI.create(url) }.getOrNull() ?: return

        if (isWebUrl(url) && Desktop.isDesktopSupported()) {
            runCatching {
                Desktop.getDesktop().browse(uri)
            }
        }
    }

    suspend fun download(
        project: HubProject,
        repository: HubRepository,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val external = externalUrl(project)
        val source = when {
            external == null -> {
                downloadPageUrl(project, repository) ?: error("No download")
            }

            driveId(external) != null -> {
                val id = driveId(external)!!
                val resourceKey = Regex("(?:^|&)resourcekey=([A-Za-z0-9_-]+)")
                    .find(URI.create(external).rawQuery.orEmpty())?.groupValues?.get(1)
                "https://drive.usercontent.google.com/download?id=${encode(id)}&export=download&confirm=t" +
                    (resourceKey?.let { "&resourcekey=${encode(it)}" } ?: "")
            }

            mediaFireKey(external) != null -> {
                resolveMediaFire(external)
            }

            else -> {
                error("Unsupported download source")
            }
        }

        val suggestedFilename = listOfNotNull(
            project.overrideName,
            project.packageName,
            "${project.title}.${when (project.projectType.name) {
                "ableton" -> "als"
                "apollo" -> "approj"
                "unipad" -> "zip"
                else -> "ame"
            }}"
        ).firstOrNull(::isSupportedFilename) ?: error("Unsupported file type")

        val safeId = project.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val directory = DesktopProjectStorage.directory
            .resolve("Hub")
            .resolve(safeId)
            .resolve("Original")
            .toFile()

        check(directory.isDirectory || directory.mkdirs()) {
            "Unable to create project directory"
        }

        val partial = File(directory, ".${UUID.randomUUID()}.download")
        var destination: File? = null

        try {
            val connection = URL(source).openConnection() as HttpURLConnection
            val digest = MessageDigest.getInstance("SHA-256")
            var responseFilename: String? = null

            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Amethyst Hub")
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }

                val mime = connection.contentType.orEmpty().substringBefore(';').lowercase()

                if (mime in setOf("text/html", "text/plain", "application/json")) {
                    error("Unexpected response")
                }

                responseFilename = connection.getHeaderField("Content-Disposition")
                    ?.let(::contentDispositionFilename)
                    ?.takeIf(::isSupportedFilename)

                val expected = connection.contentLengthLong.takeIf { it > 0 }
                    ?: project.overrideSize
                    ?: project.packageSize
                    ?: 0L

                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var reportedPercent = -1

                        while (true) {
                            val size = input.read(buffer)

                            if (size < 0) {
                                break
                            }

                            output.write(buffer, 0, size)
                            digest.update(buffer, 0, size)
                            written += size

                            if (expected > 0) {
                                val percent = ((written * 100) / expected).coerceIn(0, 100).toInt()

                                if (percent != reportedPercent) {
                                    reportedPercent = percent
                                    withContext(Dispatchers.Main) {
                                        onProgress(percent / 100f)
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            val prefix = partial.inputStream().use { stream ->
                String(stream.readNBytes(256), Charsets.UTF_8)
            }.trim().lowercase()

            if (partial.length() == 0L || listOf("<!doctype html", "<html", "<?xml", "{", "[").any(prefix::startsWith)) {
                error("Not a project file")
            }

            val hash = digest.digest().joinToString("") { "%02x".format(it) }

            val declaredHash = (if (external == null && project.overrideDownloadUrl != null) {
                project.overrideSha256
            } else if (external == null) {
                project.packageSha256
            } else {
                null
            })?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }

            check(declaredHash == null || hash.equals(declaredHash, ignoreCase = true)) {
                "Project checksum mismatch"
            }

            val filename = (responseFilename ?: suggestedFilename)
                .substringAfterLast('/')
                .substringAfterLast('\\')

            destination = File(directory, "${UUID.randomUUID()}-$filename")
            Files.move(partial.toPath(), destination.toPath())

            pendingImports[destination.absolutePath] = MobileProjectRecord(
                id = "hub-${project.id}",
                title = project.title,
                originalPath = destination.absolutePath,
                importedAt = System.currentTimeMillis(),
                hubProjectId = project.id,
                sourceHash = hash,
                hubDetails = project.downloadedDetails(),
            )

            withContext(Dispatchers.Main) {
                onProgress(1f)
            }

            destination
        } catch (error: Throwable) {
            partial.delete()
            destination?.let {
                pendingImports.remove(it.absolutePath)
                it.delete()
            }
            throw error
        }
    }

    private fun driveId(value: String): String? {
        val uri = runCatching { URI.create(value) }.getOrNull() ?: return null

        if (uri.scheme != "https" || uri.host?.lowercase() !in driveHosts) {
            return null
        }

        val id = Regex("/file/d/([A-Za-z0-9_-]+)").find(uri.path)?.groupValues?.get(1)
            ?: Regex("(?:^|&)id=([A-Za-z0-9_-]+)").find(uri.rawQuery.orEmpty())?.groupValues?.get(1)

        return id?.takeIf { it.isNotEmpty() }
    }

    private fun mediaFireKey(value: String): String? {
        val uri = runCatching { URI.create(value) }.getOrNull() ?: return null

        if (uri.scheme != "https" || uri.host?.lowercase() !in mediaFireHosts) {
            return null
        }

        return Regex("^/(?:file|download)/([A-Za-z0-9]{10,20})(?:/|$)")
            .find(uri.path)?.groupValues?.get(1)
    }

    private fun resolveMediaFire(value: String): String {
        val key = mediaFireKey(value) ?: error("Invalid MediaFire link")
        val api = "https://www.mediafire.com/api/1.5/file/get_info.php?quick_key=$key&response_format=json"
        val info = readMediaFirePage(api)

        if (!Regex("\"result\"\\s*:\\s*\"Success\"").containsMatchIn(info) ||
            !Regex("\"privacy\"\\s*:\\s*\"public\"").containsMatchIn(info) ||
            !Regex("\"password_protected\"\\s*:\\s*\"no\"").containsMatchIn(info)
        ) {
            error("MediaFire file unavailable")
        }

        val page = Regex("\"normal_download\"\\s*:\\s*\"([^\"]+)\"")
            .find(info)?.groupValues?.get(1)?.replace("\\/", "/") ?: error("MediaFire link unavailable")

        val pageUri = URI.create(page)

        if (pageUri.scheme != "https" || pageUri.host?.lowercase() !in mediaFireHosts) {
            error("MediaFire link unavailable")
        }

        val html = readMediaFirePage(page)
        val tag = Regex("<a\\b(?=[^>]*\\bid=[\"']downloadButton[\"'])[^>]*>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.value ?: error("MediaFire download unavailable")

        val href = Regex("\\bhref=[\"']([^\"']+)[\"']").find(tag)?.groupValues?.get(1)
            ?.replace("&amp;", "&")

        val scrambled = Regex("\\bdata-scrambled-url=[\"']([^\"']+)[\"']")
            .find(tag)?.groupValues?.get(1)

        val raw = href ?: scrambled?.let {
            runCatching { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }.getOrNull()
        } ?: error("MediaFire download unavailable")

        val uri = URI.create(raw)

        if (uri.scheme != "https" || !Regex("download[0-9]+\\.mediafire\\.com").matches(uri.host.orEmpty())) {
            error("MediaFire download unavailable")
        }

        return uri.toString()
    }

    private fun readMediaFirePage(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection

        return try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "Amethyst/1.0")

            check(connection.responseCode in 200..299) {
                "MediaFire unavailable"
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    private fun isWebUrl(value: String): Boolean = runCatching {
        val uri = URI.create(value)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun isSupportedFilename(value: String): Boolean =
        File(value.substringAfterLast('/').substringAfterLast('\\')).extension.lowercase() in extensions

    private fun contentDispositionFilename(header: String): String? {
        val encoded = Regex("filename\\*=[^']*''([^;]+)", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.get(1)

        if (encoded != null) {
            return runCatching {
                URLDecoder.decode(encoded.trim(), Charsets.UTF_8)
            }.getOrNull()
        }

        return Regex("filename\\s*=\\s*\\\"?([^\\\";]+)", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.get(1)?.trim()
    }
}

package dev.anthonyhfm.amethyst.hub.data

import io.ktor.http.URLBuilder
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodedPath

object DropboxDownloadLinks {
    private val origin = Regex(
        pattern = "^https://(?:www\\.)?dropbox\\.com(?::443)?/",
        option = RegexOption.IGNORE_CASE,
    )
    private val filePath = Regex(
        pattern = "^/(?:s/[A-Za-z0-9_-]+|scl/fi/[A-Za-z0-9_-]+)/[^/]+$",
    )

    fun resolve(value: String): String? {
        val raw = value.trim()

        if (!origin.containsMatchIn(input = raw) || raw.any { it.isWhitespace() || it.isISOControl() || it == '\\' }) {
            return null
        }

        return runCatching {
            val url = URLBuilder(urlString = raw)

            if (!filePath.matches(input = url.encodedPath)) {
                return null
            }

            url.build()
            url.encodedParameters.names()
                .filter { it.decodeURLQueryComponent() in setOf("dl", "raw") }
                .forEach { name ->
                    url.encodedParameters.remove(name = name)
                }
            url.parameters.append(name = "dl", value = "1")
            url.fragment = ""
            url.buildString()
        }.getOrNull()
    }
}

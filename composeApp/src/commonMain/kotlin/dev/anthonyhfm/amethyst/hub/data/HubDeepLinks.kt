package dev.anthonyhfm.amethyst.hub.data

import io.ktor.http.Url
import io.ktor.http.decodeURLPart

data class HubProjectDeepLink(
    val projectId: String,
    val username: String? = null,
    val slug: String? = null,
) {
    @Throws(Exception::class)
    suspend fun resolve(repository: HubRepository): HubProject {
        if (username != null && slug != null) {
            try {
                val project = repository.getPublishedProject.execute(username = username, slug = slug)
                if (project.id == projectId) {
                    return project
                }
            } catch (error: HubApiException) {
                if (error.statusCode != 404) {
                    throw error
                }
            }
        }

        try {
            return repository.getPublishedProjectById.execute(projectId = projectId)
        } catch (error: HubApiException) {
            if (error.statusCode != 404) {
                throw error
            }
        }

        var cursor: String? = null
        val seenCursors = mutableSetOf<String>()
        do {
            val page = repository.browseProjects.execute(cursor = cursor, limit = 50)
            page.items.firstOrNull { it.id == projectId }?.let { return it }
            cursor = page.nextCursor
        } while (cursor != null && seenCursors.add(element = cursor))

        throw HubApiException(errorCode = "not_found", statusCode = 404)
    }
}

object HubDeepLinks {
    private val projectLink = Regex(
        pattern = "^amethyst://project/([^/?#]+)/*(?:\\?[^#]*)?(?:#.*)?$",
        option = RegexOption.IGNORE_CASE,
    )
    private val projectIdPattern = Regex(pattern = "[A-Za-z0-9_-]{1,128}")

    fun parse(value: String): HubProjectDeepLink? {
        val match = projectLink.matchEntire(input = value) ?: return null
        return runCatching {
            val projectId = match.groupValues[1].decodeURLPart()
            if (!projectIdPattern.matches(input = projectId)) {
                return null
            }

            val parameters = Url(urlString = value).parameters
            HubProjectDeepLink(
                projectId = projectId,
                username = parameters["username"]?.takeIf { it.isNotBlank() },
                slug = parameters["slug"]?.takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }
}

package dev.anthonyhfm.amethyst.home.data

import kotlinx.serialization.Serializable
import dev.anthonyhfm.amethyst.hub.data.HubArtistSummary
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectType

/** Platform-independent catalog entry. Paths are resolved by MobileFileStorage. */
@Serializable
data class MobileProjectRecord(
    val id: String,
    val title: String,
    val originalPath: String,
    val importedAt: Long,
    val hubProjectId: String? = null,
    val convertedPath: String? = null,
    val sourceHash: String? = null,
    val convertedSourceHash: String? = null,
    val converterVersion: Int = 0,
    val hubDetails: DownloadedProjectDetails? = null,
)

@Serializable
data class DownloadedProjectDetails(
    val slug: String,
    val artist: HubArtistSummary,
    val thumbnailUrl: String? = null,
    val projectType: HubProjectType = HubProjectType.amethyst,
)

fun HubProject.downloadedDetails(): DownloadedProjectDetails = DownloadedProjectDetails(
    slug = slug,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    projectType = projectType,
)

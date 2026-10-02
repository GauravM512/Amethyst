package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.hub.data.HubArtistSummary
import dev.anthonyhfm.amethyst.hub.data.HubProjectType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadedProjectDetailsTest {
    @Test
    fun existingCatalogRecordsRemainReadableWithoutHubDetails() {
        val record = Json.decodeFromString<MobileProjectRecord>(
            string = """{"id":"hub-project","title":"Project","originalPath":"/Hub/project/Original/project.ame","importedAt":1,"hubProjectId":"project"}""",
        )

        assertEquals(expected = "project", actual = record.hubProjectId)
        assertNull(actual = record.hubDetails)
    }

    @Test
    fun downloadedArtworkAndArtistDetailsSurviveCatalogSerialization() {
        val record = MobileProjectRecord(
            id = "hub-project",
            title = "Project",
            originalPath = "/Hub/project/Original/project.ame",
            importedAt = 1,
            hubProjectId = "project",
            hubDetails = DownloadedProjectDetails(
                slug = "project",
                artist = HubArtistSummary(username = "artist", displayName = "Artist", avatarUrl = "/avatar"),
                thumbnailUrl = "/thumbnail",
                projectType = HubProjectType.ableton,
            ),
        )

        assertEquals(
            expected = record,
            actual = Json.decodeFromString<MobileProjectRecord>(string = Json.encodeToString(value = record)),
        )
    }
}

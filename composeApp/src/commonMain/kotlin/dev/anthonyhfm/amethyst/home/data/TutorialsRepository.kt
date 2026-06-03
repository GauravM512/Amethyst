package dev.anthonyhfm.amethyst.home.data

import amethyst.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class Tutorial(
    val id: String,
    val title: String,
    val content: String
)

object TutorialsRepository {
    private val AVAILABLE_TUTORIAL_IDS: List<String> = listOf()

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getTutorials(): List<Tutorial> {
        return AVAILABLE_TUTORIAL_IDS.mapNotNull { id ->
            try {
                val bytes = Res.readBytes("files/tutorials/$id.md")
                val content = bytes.decodeToString()
                val titleLine = content.lines().firstOrNull { it.trim().startsWith("# ") }
                val title = titleLine?.trim()?.removePrefix("# ")?.trim() ?: id
                Tutorial(id, title, content)
            } catch (e: Exception) {
                println("TutorialsRepository: failed to load tutorial '$id': ${e.message}")
                null
            }
        }
    }
}

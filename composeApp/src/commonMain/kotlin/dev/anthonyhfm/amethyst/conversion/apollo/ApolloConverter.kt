package dev.anthonyhfm.amethyst.conversion.apollo

import dev.anthonyhfm.amethyst.conversion.AmethystConverter
import dev.anthonyhfm.amethyst.conversion.apollo.data.ApolloDecoder
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData
import dev.anthonyhfm.amethyst.workspace.data.WorkspaceSettings
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.runBlocking

object ApolloConverter : AmethystConverter {
    fun convertFileToWorkspace(file: PlatformFile): SavableWorkspaceData {
        val bytes = runBlocking {
            file.readBytes()
        }

        val decoder = ApolloDecoder(bytes)

        return SavableWorkspaceData(
            settings = WorkspaceSettings(),
            lights = decoder.decode()
        )
    }

    // Accept the optional extra parameter but ignore it for Apollo format
    override fun convertZipToWorkspace(file: PlatformFile): SavableWorkspaceData {
        TODO("Not implemented yet")
    }

    override fun convertToWorkspace(path: String, palettePath: String?): SavableWorkspaceData {
        val file = PlatformFile(path)

        val bytes = runBlocking {
            file.readBytes()
        }

        val decoder = ApolloDecoder(bytes)

        return SavableWorkspaceData(
            settings = WorkspaceSettings(),
            lights = decoder.decode()
        )
    }
}
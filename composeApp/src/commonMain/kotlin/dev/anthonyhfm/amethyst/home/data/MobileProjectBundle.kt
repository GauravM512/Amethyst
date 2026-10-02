package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.core.util.AmethystProtoBuf
import dev.anthonyhfm.amethyst.core.util.Zip
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/** Internal mobile cache format, independent of the public .ame format. */
expect object MobileProjectBundle {
    suspend fun save(projectId: String, originalPath: String, workspace: SavableWorkspaceData): String?
    suspend fun load(bundlePath: String): SavableWorkspaceData?
}

internal object MobileProjectBundleCodec {
    @OptIn(ExperimentalSerializationApi::class)
    fun encodeHeader(workspace: SavableWorkspaceData): ByteArray = Zip.encode(
        AmethystProtoBuf.encodeToByteArray(
            workspace.copy(audioSources = workspace.audioSources.map { it.copy(rawData = ByteArray(0)) })
        )
    )

    @OptIn(ExperimentalSerializationApi::class)
    fun decodeHeader(bytes: ByteArray): SavableWorkspaceData =
        AmethystProtoBuf.decodeFromByteArray(Zip.decode(bytes))
}

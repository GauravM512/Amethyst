package dev.anthonyhfm.amethyst.workspace.data

import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.serialization.Serializable

@Serializable
data class SaveableWorkspaceData(
    val title: String = "Untitled Workspace",
    val settings: WorkspaceSettings = WorkspaceSettings(),
    val lights: StateChain = StateChain(),
    val sampling: StateChain = StateChain()
)

@Serializable
data class RecentWorkspace(
    val title: String,
    val path: String,
)
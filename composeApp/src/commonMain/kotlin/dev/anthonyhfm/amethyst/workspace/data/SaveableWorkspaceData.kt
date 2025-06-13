package dev.anthonyhfm.amethyst.workspace.data

import kotlinx.serialization.Serializable

@Serializable
data class SaveableWorkspaceData(
    val title: String = "Untitled Workspace",
    val author: String = "Unknown Author",
    val settings: WorkspaceSettings = WorkspaceSettings()
)
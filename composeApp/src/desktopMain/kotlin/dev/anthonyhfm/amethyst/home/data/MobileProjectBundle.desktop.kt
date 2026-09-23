package dev.anthonyhfm.amethyst.home.data

import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData

/** Desktop continues to use the existing .ame workflow. */
actual object MobileProjectBundle {
    actual suspend fun save(projectId: String, originalPath: String, workspace: SavableWorkspaceData): String? = null
    actual suspend fun load(bundlePath: String): SavableWorkspaceData? = null
}

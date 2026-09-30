package dev.anthonyhfm.amethyst.workspace

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspaceDirtyTest {
    @Test
    fun closingOnlyRequiresSavingAfterAChange() {
        val originalBpm = WorkspaceRepository.bpm.value

        try {
            WorkspaceRepository.markSaved(WorkspaceRepository.currentChangeRevision())
            assertFalse(WorkspaceRepository.hasUnsavedChanges())

            WorkspaceRepository.setBpm(originalBpm, undoable = false)
            assertFalse(WorkspaceRepository.hasUnsavedChanges())

            WorkspaceRepository.setBpm(originalBpm + 1.0, undoable = false)
            assertTrue(WorkspaceRepository.hasUnsavedChanges())

            val savedRevision = WorkspaceRepository.currentChangeRevision()
            WorkspaceRepository.markSaved(savedRevision)
            assertFalse(WorkspaceRepository.hasUnsavedChanges())

            WorkspaceRepository.setBpm(originalBpm + 2.0, undoable = false)
            WorkspaceRepository.markSaved(savedRevision)
            assertTrue(WorkspaceRepository.hasUnsavedChanges())
        } finally {
            WorkspaceRepository.setBpm(originalBpm, undoable = false)
            WorkspaceRepository.markSaved(WorkspaceRepository.currentChangeRevision())
        }
    }
}

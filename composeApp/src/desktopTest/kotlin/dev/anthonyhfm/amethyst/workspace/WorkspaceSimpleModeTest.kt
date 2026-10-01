package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.settings.data.GeneralSettings
import dev.anthonyhfm.amethyst.settings.data.SettingsRepository
import dev.anthonyhfm.amethyst.workspace.modes.defaults.LayoutWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.modes.defaults.LightsChainWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.modes.defaults.PerformanceWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.modes.defaults.SamplingChainWorkspaceMode
import dev.anthonyhfm.amethyst.workspace.modes.defaults.TimelineWorkspaceMode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorkspaceSimpleModeTest {
    @Test
    fun restrictedModesKeepTheCurrentModeAndReportUnavailability() = runTest {
        SettingsRepository.settingsGroups
        val originalSimpleMode = GeneralSettings.simpleMode.value
        val originalMode = WorkspaceRepository.mode.value

        try {
            GeneralSettings.simpleMode.update(value = true)
            val performance = PerformanceWorkspaceMode()
            WorkspaceRepository.switchMode(mode = performance, undoable = false)

            listOf(
                TimelineWorkspaceMode(),
                LightsChainWorkspaceMode(),
                SamplingChainWorkspaceMode(),
            ).forEach { restrictedMode ->
                val notification = async(start = CoroutineStart.UNDISPATCHED) {
                    WorkspaceRepository.simpleModeUnavailable.first()
                }

                assertFalse(actual = WorkspaceRepository.isModeAvailable(mode = restrictedMode))
                WorkspaceRepository.switchMode(mode = restrictedMode, undoable = false)
                assertSame(expected = performance, actual = WorkspaceRepository.mode.value)
                notification.await()
            }

            WorkspaceRepository.switchMode(mode = LayoutWorkspaceMode(), undoable = false)
            assertIs<LayoutWorkspaceMode>(value = WorkspaceRepository.mode.value)
        } finally {
            GeneralSettings.simpleMode.update(value = originalSimpleMode)
            WorkspaceRepository.switchMode(mode = originalMode, undoable = false)
        }
    }

    @Test
    fun enablingSimpleModeLeavesRestrictedWorkspacesAndDisablingRestoresTheirAvailability() {
        SettingsRepository.settingsGroups
        val originalSimpleMode = GeneralSettings.simpleMode.value
        val originalMode = WorkspaceRepository.mode.value

        try {
            GeneralSettings.simpleMode.update(value = false)
            val lights = LightsChainWorkspaceMode()
            WorkspaceRepository.switchMode(mode = lights, undoable = false)
            assertSame(expected = lights, actual = WorkspaceRepository.mode.value)

            GeneralSettings.simpleMode.update(value = true)
            assertIs<PerformanceWorkspaceMode>(value = WorkspaceRepository.mode.value)
            assertTrue(actual = WorkspaceRepository.isModeAvailable(mode = LayoutWorkspaceMode()))
            assertTrue(actual = WorkspaceRepository.isModeAvailable(mode = PerformanceWorkspaceMode()))

            GeneralSettings.simpleMode.update(value = false)
            listOf(
                TimelineWorkspaceMode(),
                LightsChainWorkspaceMode(),
                SamplingChainWorkspaceMode(),
            ).forEach { mode ->
                assertTrue(actual = WorkspaceRepository.isModeAvailable(mode = mode))
                WorkspaceRepository.switchMode(mode = mode, undoable = false)
                assertSame(expected = mode, actual = WorkspaceRepository.mode.value)
            }
        } finally {
            GeneralSettings.simpleMode.update(value = originalSimpleMode)
            WorkspaceRepository.switchMode(mode = originalMode, undoable = false)
        }
    }
}

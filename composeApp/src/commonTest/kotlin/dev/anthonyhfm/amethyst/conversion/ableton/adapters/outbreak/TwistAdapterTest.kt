package dev.anthonyhfm.amethyst.conversion.ableton.adapters.outbreak

import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDevice
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.data.Macro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TwistAdapterTest {
    @Test
    fun convertedTwistChangesTheRuntimePageOnNoteOn() {
        WorkspaceRepository.setMacros(listOf(Macro(id = "twist-page", value = 0)), undoable = false)

        try {
            val state = assertIs<MacroControlChainDeviceState>(
                TwistAdapter(blob = savedTwistSettings(targets = listOf(8, 0)))
                    .toDeviceStates()
                    .single()
            )
            val control = MacroControlChainDevice().apply { this.state.value = state }

            control.signalEnter(listOf(Signal.Midi("page-button", 0, 1, 127)))
            assertEquals(8, WorkspaceRepository.macros.value.single().value)

            control.signalEnter(listOf(Signal.Midi("page-button", 0, 1, 0)))
            assertEquals(8, WorkspaceRepository.macros.value.single().value)
        } finally {
            WorkspaceRepository.setMacros(listOf(Macro(id = "twist-page", value = 0)), undoable = false)
        }
    }

    @Test
    fun setModeUsesTheSavedMacroValueWithoutPageRenumbering() {
        listOf(0, 1, 8, 15).forEach { target ->
            val devices = TwistAdapter(blob = savedTwistSettings(targets = listOf(target, 0))).toDeviceStates()

            assertEquals(expected = 1, actual = devices.size)
            val control = assertIs<MacroControlChainDeviceState>(devices.single())
            assertEquals(expected = 0, actual = control.macro)
            assertEquals(expected = target, actual = control.value)
        }
    }

    @Test
    fun bothEnabledMacrosReceiveTheirSavedValues() {
        val devices = TwistAdapter(
            blob = savedTwistSettings(
                targets = listOf(0, 37),
                enabled = listOf(1, 1),
            )
        ).toDeviceStates()

        val group = assertIs<GroupChainDeviceState>(devices.single())
        val controls = group.groups.map { item ->
            assertIs<MacroControlChainDeviceState>(item.stateChain.devices.single())
        }

        assertEquals(expected = listOf(0, 1), actual = controls.map { it.macro })
        assertEquals(expected = listOf(0, 37), actual = controls.map { it.value })
    }

    private fun savedTwistSettings(
        targets: List<Int>,
        enabled: List<Int> = listOf(1, 0),
    ): String {
        val targetValues = (targets + List(8 - targets.size) { 0 }).joinToString(",")
        val enabledValues = (enabled + List(8 - enabled.size) { 0 }).joinToString(",")
        val zeros = List(8) { 0 }.joinToString(",")
        val lengths = List(8) { 344 }.joinToString(",")

        return """{
            "table[13]":[$enabledValues],
            "table[1]":[$targetValues],
            "table[7]":[$zeros],
            "table[8]":[$lengths],
            "table[6]":[$zeros],
            "Rate":[3],
            "Rate[3]":[3],
            "Rate[4]":[3],
            "Rate[5]":[3],
            "Rate[6]":[3],
            "Rate[7]":[3],
            "Rate[8]":[3],
            "Rate[9]":[3]
        }""".trimIndent()
    }
}

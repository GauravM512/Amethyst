package dev.anthonyhfm.amethyst.conversion.ableton.adapters.nev

import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WormholeAdapterTest {
    @Test
    fun importsEnabledMacroDestinationFromSerializedBlob() {
        val blob = """
            {
                "Destination_1": [37],
                "Destination_2": [0],
                "Destination_3": [0],
                "Destination_4": [0],
                "Destination_5": [0],
                "Destination_6": [0],
                "Destination_7": [0],
                "Destination_8": [0],
                "Enabler_1": [1],
                "Enabler_2": [0],
                "Enabler_3": [0],
                "Enabler_4": [0],
                "Enabler_5": [0],
                "Enabler_6": [0],
                "Enabler_7": [0],
                "Enabler_8": [0],
                "number[1]": [1]
            }
        """.trimIndent()

        val group = assertIs<GroupChainDeviceState>(
            WormholeAdapter(data = blob).toDeviceStates().single()
        )
        val macro = assertIs<MacroControlChainDeviceState>(
            group.groups.single().stateChain.devices.single()
        )

        assertEquals(expected = 0, actual = macro.macro)
        assertEquals(expected = 37, actual = macro.value)
    }
}

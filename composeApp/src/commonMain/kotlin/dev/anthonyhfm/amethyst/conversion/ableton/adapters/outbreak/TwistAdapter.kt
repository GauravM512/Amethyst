package dev.anthonyhfm.amethyst.conversion.ableton.adapters.outbreak

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.layer.LayerChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.SwitchChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TwistAdapter (
    private val data: ByteArray
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val dataObj: TwistData = Json {
            ignoreUnknownKeys = true
        }.decodeFromString(data.decodeToString())

        println("Twist Data: ${data.decodeToString()}")

        val deviceGroups = mutableListOf<Group>()

        dataObj.macroEnabledStates.withIndex().map { (index, num) ->
            if (num == 1) {
                deviceGroups.add(Group(
                    name = "Twist Macro ${index + 1}",
                    stateChain = StateChain(
                        devices = mutableListOf(SwitchChainDeviceState(
                            macro = index,
                            value = dataObj.pageSwitchNumbers[index]
                        ))
                    )
                ))
            }
        }

        return listOf(
            GroupChainDeviceState(
                groups = deviceGroups,
            )
        )
    }

    @Serializable
    data class TwistData(
        @SerialName("table[13]")
        val macroEnabledStates: List<Int>,

        @SerialName("table[1]")
        val pageSwitchNumbers: List<Int>

        // TODO: add smooth mode with delays (rate or ms)
        // TODO: handle slope for newer twist version
    )
}
package dev.anthonyhfm.amethyst.core.controls.automapping

import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDevice
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.LaunchpadPadFilter
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.pianoroll.PianoRollChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.coroutines.flow.update

internal object AutomappingChainMutation {
    
    fun toggleClipOnPad(
        parentDevice: GenericChainDevice<*>,
        launchpadId: String,
        localX: Int,
        localY: Int,
        clip: AutomappingSelectedClip,
    ): Boolean {
        val targetDevice = buildChainDeviceFromAutomappingClip(clip) ?: return false
        val targetState = StateChain.packDevice(targetDevice)
        val padFilter = LaunchpadPadFilter(launchpadId, localX, localY)

        val groups = when (parentDevice) {
            is GroupChainDevice -> parentDevice.state.value.groups
            is MultiGroupChainDevice -> parentDevice.state.value.groups
            else -> return false
        }

        val matchingGroup = groups.find { group ->
            val allClipDevices = group.chain.devices.value.filter { it is SampleChainDevice || it is PianoRollChainDevice }
            val existingClipDevice = allClipDevices.firstOrNull()
            existingClipDevice != null && StateChain.packDevice(existingClipDevice) == targetState
        }

        if (matchingGroup != null) {
            val coordFilter = matchingGroup.chain.devices.value.filterIsInstance<CoordinateFilterChainDevice>().firstOrNull() ?: CoordinateFilterChainDevice().also {
                matchingGroup.chain.add(it, 0, fromUser = false)
            }
            
            val currentFilters = coordFilter.state.value.padFilters
            if (currentFilters.contains(padFilter)) {
                coordFilter.state.update { it.copy(padFilters = currentFilters - padFilter) }

                if (coordFilter.state.value.padFilters.isEmpty()) {
                    when (parentDevice) {
                        is GroupChainDevice -> parentDevice.removeGroupById(matchingGroup.id)
                        is MultiGroupChainDevice -> parentDevice.removeGroupById(matchingGroup.id)
                    }
                }
            } else {
                coordFilter.state.update { it.copy(padFilters = currentFilters + padFilter) }
            }
        } else {
            val newGroup = Group(name = "Mapped")
            val coordFilter = CoordinateFilterChainDevice()
            coordFilter.state.update { it.copy(padFilters = listOf(padFilter)) }
            
            newGroup.chain.add(coordFilter, 0, fromUser = false)
            newGroup.chain.add(targetDevice, 1, fromUser = false)
            
            when (parentDevice) {
                is GroupChainDevice -> parentDevice.addGroup(newGroup)
                is MultiGroupChainDevice -> parentDevice.addGroup(newGroup)
            }
        }
        
        return true
    }
}

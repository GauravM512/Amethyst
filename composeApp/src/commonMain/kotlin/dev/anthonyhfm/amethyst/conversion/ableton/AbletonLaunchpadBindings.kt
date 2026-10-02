package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.mask.MaskChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData

internal fun SavableWorkspaceData.rebindLaunchpadBindings(
    previousLaunchpads: List<SavableWorkspaceData.SavableViewportLaunchpad>,
): SavableWorkspaceData {
    val replacements = previousLaunchpads.mapIndexedNotNull { index, previous ->
        val replacement = launchpadDevices.firstOrNull { current ->
            current.positionX == previous.positionX && current.positionY == previous.positionY
        } ?: if (launchpadDevices.size == previousLaunchpads.size) {
            launchpadDevices[index]
        } else {
            null
        }
        replacement?.let { previous.id to (previous to it) }
    }.toMap()

    if (replacements.isEmpty() || replacements.values.all { (previous, current) ->
            previous.id == current.id &&
                previous.positionX == current.positionX &&
                previous.positionY == current.positionY
        }) {
        return this
    }
    val replacementIds = replacements.mapValues { (_, pair) -> pair.second.id }

    return copy(
        sampling = sampling.rebindLaunchpadBindings(replacementIds),
        autoPlay = autoPlay.copy(
            actions = autoPlay.actions.mapValues { (_, actions) ->
                actions.map { action ->
                    val replacement = action.launchpadId?.let(replacements::get)
                    if (replacement == null) {
                        action
                    } else {
                        val (previous, current) = replacement
                        action.copy(
                            x = action.x + (current.positionX - previous.positionX).toInt(),
                            y = action.y + (current.positionY - previous.positionY).toInt(),
                            launchpadId = current.id,
                        )
                    }
                }
            },
        ),
    )
}

private fun StateChain.rebindLaunchpadBindings(replacementIds: Map<String, String>): StateChain = copy(
    devices = devices.map { state -> state.rebindLaunchpadBindings(replacementIds) },
)

private fun DeviceState.rebindLaunchpadBindings(replacementIds: Map<String, String>): DeviceState {
    val rebound = when (this) {
        is CoordinateFilterChainDeviceState -> copy(
            padFilters = padFilters.map { filter ->
                filter.copy(launchpadId = replacementIds[filter.launchpadId] ?: filter.launchpadId)
            },
        )
        is GroupChainDeviceState -> copy(
            groups = groups.map { group ->
                group.copy(stateChain = group.stateChain.rebindLaunchpadBindings(replacementIds))
            },
        )
        is MultiGroupChainDeviceState -> copy(
            groups = groups.map { group ->
                group.copy(stateChain = group.stateChain.rebindLaunchpadBindings(replacementIds))
            },
            preprocessChain = preprocessChain.rebindLaunchpadBindings(replacementIds),
        )
        is ChokeChainDeviceState -> copy(
            stateChain = stateChain.rebindLaunchpadBindings(replacementIds),
        )
        is MaskChainDeviceState -> copy(
            colorStateChain = colorStateChain.rebindLaunchpadBindings(replacementIds),
            shapeStateChain = shapeStateChain.rebindLaunchpadBindings(replacementIds),
        )
        else -> this
    }
    rebound.isMuted = isMuted
    rebound.isCollapsed = isCollapsed
    return rebound
}

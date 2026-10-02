package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.LaunchpadPadFilter
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import dev.anthonyhfm.amethyst.workspace.data.AutoPlayData
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData
import kotlin.test.Test
import kotlin.test.assertEquals

class AbletonLaunchpadBindingsTest {
    @Test
    fun replacingTwoLaunchpadsRebindsNestedSampleFiltersAndAutoPlay() {
        val previous = listOf(
            launchpad(id = "ableton-left", x = 0f),
            launchpad(id = "ableton-right", x = 10f),
        )
        val replacement = listOf(
            launchpad(id = "apollo-left", x = 20f),
            launchpad(id = "apollo-right", x = 30f),
        )
        val workspace = SavableWorkspaceData(
            launchpadDevices = replacement,
            sampling = StateChain(
                devices = listOf(
                    GroupChainDeviceState(
                        groups = previous.mapIndexed { index, device ->
                            Group(
                                name = "Samples $index",
                                stateChain = StateChain(
                                    devices = listOf(
                                        CoordinateFilterChainDeviceState(
                                            filters = listOf((index * 10 + 1) to 2),
                                            padFilters = listOf(
                                                LaunchpadPadFilter(
                                                    launchpadId = device.id,
                                                    localX = 1,
                                                    localY = 2,
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            )
                        },
                    ),
                ),
            ),
            autoPlay = AutoPlayData(
                actions = mapOf(
                    0.0 to listOf(
                        AutoPlayData.Action(x = 1, y = 2, down = true, launchpadId = previous[0].id),
                        AutoPlayData.Action(x = 11, y = 2, down = true, launchpadId = previous[1].id),
                    ),
                ),
            ),
        )

        val rebound = workspace.rebindLaunchpadBindings(previousLaunchpads = previous)
        val group = rebound.sampling.devices.single() as GroupChainDeviceState
        val filters = group.groups.map { it.stateChain.devices.single() as CoordinateFilterChainDeviceState }

        assertEquals(listOf("apollo-left", "apollo-right"), filters.map { it.padFilters.single().launchpadId })
        assertEquals(
            listOf("apollo-left", "apollo-right"),
            rebound.autoPlay.actions.getValue(0.0).map { it.launchpadId },
        )
        assertEquals(
            listOf(21 to 2, 31 to 2),
            rebound.autoPlay.actions.getValue(0.0).map { it.x to it.y },
        )
    }

    private fun launchpad(id: String, x: Float): SavableWorkspaceData.SavableViewportLaunchpad =
        SavableWorkspaceData.SavableViewportLaunchpad.LaunchpadPro(
            positionX = x,
            positionY = 0f,
            id = id,
        )
}

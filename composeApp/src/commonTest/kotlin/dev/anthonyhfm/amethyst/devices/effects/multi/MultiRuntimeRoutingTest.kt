package dev.anthonyhfm.amethyst.devices.effects.multi

import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiRuntimeRoutingTest {
    @Test
    fun forwardModeRoutesSuccessiveNotesToDifferentGroupsAndReleasesToTheirOrigins() {
        val routes = mutableListOf<Pair<Int, Int>>()
        val groups = List(3) { index ->
            Group(
                name = "Step $index",
                chain = Chain().apply {
                    signalExit = { signals ->
                        signals.filterIsInstance<Signal.Midi>().forEach { signal ->
                            routes.add(index to signal.velocity)
                        }
                    }
                },
            )
        }
        val multi = MultiGroupChainDevice().apply {
            state.value = MultiGroupChainDeviceState(
                type = MultiGroupChainDeviceState.TYPE.FORWARD,
                groups = groups,
            )
        }

        repeat(4) {
            multi.signalEnter(listOf(Signal.Midi("pad", 2, 3, 127)))
            multi.signalEnter(listOf(Signal.Midi("pad", 2, 3, 0)))
        }

        assertEquals(
            listOf(
                0 to 127, 0 to 0,
                1 to 127, 1 to 0,
                2 to 127, 2 to 0,
                0 to 127, 0 to 0,
            ),
            routes,
        )
    }
}

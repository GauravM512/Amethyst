package dev.anthonyhfm.amethyst.workspace.chain.ui

import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * # SignalIndicatorManager
 *
 * Used to subscribe to signal events for chains and their device slots.
 *
 * @see [Chain]
 * @see [ExpandingChainDevicePicker]
 */
object SignalIndicatorManager {
    private val chainSlotFlows: MutableMap<Chain, MutableMap<Int, MutableSharedFlow<Unit>>> = mutableMapOf()

    fun trigger(chain: Chain, slotIndex: Int) {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        val flow = slotMap.getOrPut(slotIndex) {
            MutableSharedFlow(extraBufferCapacity = 32)
        }
        flow.tryEmit(Unit)
    }

    fun events(chain: Chain, slotIndex: Int): SharedFlow<Unit> {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        return slotMap.getOrPut(slotIndex) {
            MutableSharedFlow(extraBufferCapacity = 32)
        }
    }
}

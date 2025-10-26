package dev.anthonyhfm.amethyst.workspace.chain.ui

import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SignalIndicatorManager {
    private val chainSlotFlows: MutableMap<Chain, MutableMap<Int, MutableStateFlow<Long>>> = mutableMapOf()

    fun trigger(chain: Chain, slotIndex: Int) {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        val flow = slotMap.getOrPut(slotIndex) { MutableStateFlow(0L) }

        flow.value += 1L
    }

    fun observe(chain: Chain, slotIndex: Int): StateFlow<Long> {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        return slotMap.getOrPut(slotIndex) { MutableStateFlow(0L) }
    }
}

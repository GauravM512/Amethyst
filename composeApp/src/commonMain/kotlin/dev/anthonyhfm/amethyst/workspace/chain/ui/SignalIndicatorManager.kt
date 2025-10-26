package dev.anthonyhfm.amethyst.workspace.chain.ui

import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Event-basierter Signal-Indikator Manager:
 * Jeder trigger(chain, slotIndex) emittiert genau ein Event in einen SharedFlow ohne Replay.
 * Recomposition ohne neuem Event führt nicht zu einem künstlichen Aufblitzen.
 */
object SignalIndicatorManager {
    private val chainSlotFlows: MutableMap<Chain, MutableMap<Int, MutableSharedFlow<Unit>>> = mutableMapOf()

    fun trigger(chain: Chain, slotIndex: Int) {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        val flow = slotMap.getOrPut(slotIndex) {
            MutableSharedFlow(extraBufferCapacity = 32) // ausreichend Buffer für schnelle Bursts
        }
        flow.tryEmit(Unit) // Event feuern; bei vollem Buffer älteste droppen (Default: SUSPEND, aber mit Buffer passt es)
    }

    fun events(chain: Chain, slotIndex: Int): SharedFlow<Unit> {
        val slotMap = chainSlotFlows.getOrPut(chain) { mutableMapOf() }
        return slotMap.getOrPut(slotIndex) {
            MutableSharedFlow(extraBufferCapacity = 32)
        }
    }
}

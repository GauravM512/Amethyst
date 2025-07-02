package dev.anthonyhfm.amethyst.core.heaven.elements

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.anthonyhfm.amethyst.devices.ChainDevice
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain

class Chain : SignalReceiver() {
    val devices: MutableState<List<ChainDevice<*>>> = mutableStateOf(emptyList())

    override fun midiEnter(input: List<Signal>) {
        if (devices.value.isEmpty()) {
            midiExit?.invoke(input)
        } else {
            devices.value[0].midiEnter(input)
        }
    }

    fun reroute() {
        if (devices.value.isNotEmpty()) {
            for (i in 1..devices.value.size) {
                devices.value[i - 1].midiExit = {
                    devices.value[i].midiEnter(it)
                }
            }

            devices.value[devices.value.lastIndex].midiExit = {
                midiExit?.invoke(it)
            }
        }
    }

    fun add(device: ChainDevice<*>, atIndex: Int? = null) {
        devices.value = devices.value.toMutableList().apply {
            if (atIndex != null) {
                add(index = atIndex, device)
            } else {
                add(device)
            }
        }

        reroute()
    }

    fun remove(index: Int) {
        devices.value = devices.value.toMutableList().apply {
            removeAt(index)
        }

        reroute()
    }
}
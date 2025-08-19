package dev.anthonyhfm.amethyst.core.heaven.elements

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.anthonyhfm.amethyst.devices.ChainDevice
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.core.controls.undo.UndoableAction

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

    fun add(device: ChainDevice<*>, atIndex: Int? = null, fromUser: Boolean = true) {
        devices.value = devices.value.toMutableList().apply {
            if (atIndex != null) {
                add(index = atIndex, device)
            } else {
                add(device)
            }
        }

        if (fromUser) {
            UndoManager.addAction(
                UndoableAction.ChainDeviceCreation(
                    parent = this,
                    device = device
                )
            )
        }

        reroute()
    }

    fun remove(index: Int, fromUser: Boolean = true) {
        if (index >= 0 && index < devices.value.size) {
            val deviceToRemove = devices.value[index]

            if (fromUser) {
                UndoManager.addAction(
                    UndoableAction.ChainDeviceRemoval(
                        parent = this,
                        device = deviceToRemove
                    )
                )
            }

            devices.value = devices.value.toMutableList().apply {
                removeAt(index)
            }
        }

        reroute()
    }

    fun remove(uuid: String, fromUser: Boolean = true) {
        val deviceToRemove = devices.value.find { it.selectionUUID == uuid }

        if (deviceToRemove != null) {
            if (fromUser) {
                UndoManager.addAction(
                    UndoableAction.ChainDeviceRemoval(
                        parent = this,
                        device = deviceToRemove
                    )
                )
            }

            devices.value = devices.value.toMutableList().apply {
                removeAll { it.selectionUUID == uuid }
            }
        } else {
            devices.value.map {
                when (it) {
                    is GroupChainDevice -> {
                        it.apply {
                            state.value.groups.forEach { group ->
                                group.chain.remove(uuid, fromUser)
                            }
                        }
                    }

                    is MultiGroupChainDevice -> {
                        it.apply {
                            state.value.groups.forEach { group ->
                                group.chain.remove(uuid, fromUser)
                            }
                        }
                    }

                    is ChokeChainDevice -> {
                        it.apply {
                            state.value.chain.remove(uuid, fromUser)
                        }
                    }

                    else -> it
                }
            }
        }

        reroute()
    }
}
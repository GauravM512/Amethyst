package dev.anthonyhfm.amethyst.core.engine.elements

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.core.controls.undo.UndoableAction
import dev.anthonyhfm.amethyst.devices.GenericChainDevice

class Chain : SignalReceiver() {
    val devices: MutableState<List<GenericChainDevice<*>>> = mutableStateOf(emptyList())

    override fun signalEnter(input: List<Signal>) {
        if (devices.value.isEmpty()) {
            signalExit?.invoke(input)
        } else {
            devices.value[0].signalEnter(input)
        }
    }

    fun reroute() {
        if (devices.value.isNotEmpty()) {
            for (i in 1..devices.value.size) {
                devices.value[i - 1].signalExit = {
                    devices.value[i].signalEnter(it)
                }
            }

            devices.value[devices.value.lastIndex].signalExit = {
                signalExit?.invoke(it)
            }
        }
    }

    fun add(device: GenericChainDevice<*>, atIndex: Int? = null, fromUser: Boolean = true) {
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

    fun findDeviceChain(deviceUUID: String): Chain? {
        if (devices.value.any { it.selectionUUID == deviceUUID }) {
            return this
        }

        devices.value.forEach { device ->
            when (device) {
                is GroupChainDevice -> {
                    device.state.value.groups.forEach { group ->
                        group.chain.findDeviceChain(deviceUUID)?.let { foundChain ->
                            return foundChain
                        }
                    }
                }

                is MultiGroupChainDevice -> {
                    device.state.value.groups.forEach { group ->
                        group.chain.findDeviceChain(deviceUUID)?.let { foundChain ->
                            return foundChain
                        }
                    }
                }

                is ChokeChainDevice -> {
                    device.state.value.chain.findDeviceChain(deviceUUID)?.let { foundChain ->
                        return foundChain
                    }
                }
            }
        }

        return null
    }
}
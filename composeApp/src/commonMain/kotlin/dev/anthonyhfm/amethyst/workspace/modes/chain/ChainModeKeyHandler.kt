package dev.anthonyhfm.amethyst.workspace.modes.chain

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager

object ChainModeKeyHandler {
    fun handleKeyInput(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type == KeyEventType.KeyDown) {
            when (keyEvent.key) {
                Key.Backspace, Key.Delete -> {
                    val selections = SelectionManager.selections.value.filter { it is Selectable.ChainDevice }

                    if (selections.isNotEmpty()) {
                        val devicesToDelete = selections.map { selection ->
                            val chainDevice = selection as Selectable.ChainDevice
                            val chain = chainDevice.parent
                            val deviceIndex = chain.devices.value.indexOfFirst { it.selectionUUID == chainDevice.device.selectionUUID }
                            Triple(chainDevice, chain, deviceIndex)
                        }.filter { it.third >= 0 }

                        val sortedDevicesToDelete = devicesToDelete.sortedByDescending { it.third }

                        sortedDevicesToDelete.forEach { (chainDevice, chain, _) ->
                            chain.remove(chainDevice.device.selectionUUID)
                        }

                        SelectionManager.clear()

                        if (sortedDevicesToDelete.isNotEmpty()) {
                            val (_, firstChain, firstIndex) = sortedDevicesToDelete.last() // Nimm das Device mit dem niedrigsten Index

                            val newSelectionIndex = when {
                                firstChain.devices.value.isEmpty() -> -1 // Keine Devices mehr vorhanden
                                firstIndex >= firstChain.devices.value.size -> firstChain.devices.value.size - 1 // Letztes Device wurde gelöscht
                                else -> firstIndex
                            }

                            if (newSelectionIndex >= 0 && newSelectionIndex < firstChain.devices.value.size) {
                                val newSelectedDevice = firstChain.devices.value[newSelectionIndex]
                                SelectionManager.select(
                                    Selectable.ChainDevice(
                                        parent = firstChain,
                                        device = newSelectedDevice
                                    ),
                                    single = true
                                )
                            }
                        }

                        if (devicesToDelete.isNotEmpty()) {
                            return true
                        }
                    }
                }

                Key.Escape -> {
                    SelectionManager.clear()

                    return true
                }
            }
        }

        return false
    }
}
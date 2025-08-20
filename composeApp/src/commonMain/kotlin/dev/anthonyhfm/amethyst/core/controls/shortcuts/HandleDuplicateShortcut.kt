package dev.anthonyhfm.amethyst.core.controls.shortcuts

import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.coroutines.flow.update

fun handleDuplicateShortcut(): Boolean {
    SelectionManager.selections.value.filterIsInstance<Selectable.ChainDevice>().map { selection ->
        val index = selection.parent.devices.value.indexOfFirst { it.selectionUUID == selection.selectionUUID }

        selection.parent.add(StateChain.unpackDevice(StateChain.packDevice(selection.device)), index)

        return@map selection
    }.apply {
        if (isNotEmpty()) return true
    }

    if (SelectionManager.selections.value.any { it is Selectable.GroupChainItem }) {
        val selectedGroupItems = SelectionManager.selections.value.filterIsInstance<Selectable.GroupChainItem>()

        // Group by parent device to handle multiple selections efficiently
        val groupedSelections = selectedGroupItems.groupBy { it.parent }

        groupedSelections.forEach { (parent, items) ->
            val indices = items.map { it.groupIndex }.sorted()

            when (parent) {
                is GroupChainDevice -> {
                    parent.duplicateGroups(indices)
                }

                is MultiGroupChainDevice -> {
                    parent.duplicateGroups(indices)
                }
            }
        }

        SelectionManager.clear()
        return true
    }

    return false
}
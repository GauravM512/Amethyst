package dev.anthonyhfm.amethyst.core.controls.shortcuts

import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import kotlinx.coroutines.flow.update

fun handleDeletionShortcut(): Boolean {
    when {
        SelectionManager.selections.value.any { it is Selectable.GradientStep } -> {
            SelectionManager.selections.value.filterIsInstance<Selectable.GradientStep>().forEach { step ->
                if (step.parent.state.value.gradientData.size - 1 < 1) return@forEach

                step.parent.state.update {
                    it.copy(
                        gradientData = it.gradientData.toMutableList().apply {
                            removeAll { it.selectionUUID == step.selectionUUID }
                        }
                    )
                }
            }

            return true
        }

        SelectionManager.selections.value.any { it is Selectable.GroupChainItem } -> {
            val selectedGroupItems = SelectionManager.selections.value.filterIsInstance<Selectable.GroupChainItem>()

            // Group by parent device to handle multiple selections efficiently
            val groupedSelections = selectedGroupItems.groupBy { it.parent }

            groupedSelections.forEach { (parent, items) ->
                val indices = items.map { it.groupIndex }

                when (parent) {
                    is GroupChainDevice -> {
                        parent.removeGroups(indices)
                    }

                    is MultiGroupChainDevice -> {
                        parent.removeGroups(indices)
                    }
                }
            }

            SelectionManager.clear()

            return true
        }
    }

    return false
}
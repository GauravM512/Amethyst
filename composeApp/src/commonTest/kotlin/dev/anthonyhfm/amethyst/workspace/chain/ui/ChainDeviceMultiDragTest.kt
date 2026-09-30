package dev.anthonyhfm.amethyst.workspace.chain.ui

import dev.anthonyhfm.amethyst.core.controls.selection.Selectable
import dev.anthonyhfm.amethyst.core.controls.selection.SelectionManager
import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.devices.effects.color.ColorChainDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class ChainDeviceMultiDragTest {
    @Test
    fun movesNonAdjacentSelectedDevicesInChainOrderAndUndoesTogether() {
        val chain = Chain().apply { collaborationSyncEnabled = false }
        val devices = List(5) { ColorChainDevice() }
        devices.forEach { chain.add(device = it, fromUser = false) }

        try {
            SelectionManager.replaceSelections(
                listOf(
                    Selectable.ChainDevice(parent = chain, device = devices[3]),
                    Selectable.ChainDevice(parent = chain, device = devices[1]),
                )
            )

            moveChainDevices(
                chain = chain,
                parentSelectionUUID = null,
                onAddDevice = null,
                onMoveDevice = null,
                draggedDevices = selectedChainDevicesForDrag(dragged = devices[3], originChain = chain),
                originChain = chain,
                insertionIndex = 5,
            )

            assertEquals(listOf(devices[0], devices[2], devices[4], devices[1], devices[3]), chain.devices.value)

            UndoManager.undo()
            assertEquals(devices, chain.devices.value)

            UndoManager.redo()
            assertEquals(listOf(devices[0], devices[2], devices[4], devices[1], devices[3]), chain.devices.value)
        } finally {
            SelectionManager.clear()
        }
    }

    @Test
    fun unselectedDragMovesOnlyItsOwnDevice() {
        val chain = Chain().apply { collaborationSyncEnabled = false }
        val devices = List(4) { ColorChainDevice() }
        devices.forEach { chain.add(device = it, fromUser = false) }

        try {
            SelectionManager.replaceSelections(
                listOf(
                    Selectable.ChainDevice(parent = chain, device = devices[0]),
                    Selectable.ChainDevice(parent = chain, device = devices[2]),
                )
            )

            val draggedDevices = selectedChainDevicesForDrag(dragged = devices[1], originChain = chain)
            assertEquals(listOf(devices[1]), draggedDevices)

            moveChainDevices(
                chain = chain,
                parentSelectionUUID = null,
                onAddDevice = null,
                onMoveDevice = null,
                draggedDevices = draggedDevices,
                originChain = chain,
                insertionIndex = 4,
            )

            assertEquals(listOf(devices[0], devices[2], devices[3], devices[1]), chain.devices.value)
            UndoManager.undo()
        } finally {
            SelectionManager.clear()
        }
    }

    @Test
    fun callbackReordersSelectedDevicesTowardStart() {
        val chain = Chain().apply { collaborationSyncEnabled = false }
        val devices = List(5) { ColorChainDevice() }
        devices.forEach { chain.add(device = it, fromUser = false) }

        moveChainDevices(
            chain = chain,
            parentSelectionUUID = null,
            onAddDevice = null,
            onMoveDevice = { fromIndex, toIndex ->
                val device = chain.devices.value[fromIndex]
                chain.remove(index = fromIndex, fromUser = false)
                chain.add(device = device, atIndex = toIndex, fromUser = false)
            },
            draggedDevices = listOf(devices[1], devices[3]),
            originChain = chain,
            insertionIndex = 0,
        )

        assertEquals(listOf(devices[1], devices[3], devices[0], devices[2], devices[4]), chain.devices.value)
    }

    @Test
    fun movesSelectionBetweenChainsAndRestoresBothOnUndo() {
        val origin = Chain().apply { collaborationSyncEnabled = false }
        val destination = Chain().apply { collaborationSyncEnabled = false }
        val devices = List(4) { ColorChainDevice() }
        val existing = ColorChainDevice()
        devices.forEach { origin.add(device = it, fromUser = false) }
        destination.add(device = existing, fromUser = false)

        try {
            SelectionManager.replaceSelections(
                listOf(
                    Selectable.ChainDevice(parent = origin, device = devices[1]),
                    Selectable.ChainDevice(parent = origin, device = devices[3]),
                )
            )

            moveChainDevices(
                chain = destination,
                parentSelectionUUID = null,
                onAddDevice = null,
                onMoveDevice = null,
                draggedDevices = selectedChainDevicesForDrag(dragged = devices[1], originChain = origin),
                originChain = origin,
                insertionIndex = 0,
            )

            assertEquals(listOf(devices[0], devices[2]), origin.devices.value)
            assertEquals(listOf(devices[1], devices[3], existing), destination.devices.value)
            assertEquals(
                listOf(destination, destination),
                SelectionManager.selections.value.filterIsInstance<Selectable.ChainDevice>().map { it.parent },
            )

            UndoManager.undo()
            assertEquals(devices, origin.devices.value)
            assertEquals(listOf(existing), destination.devices.value)
            assertEquals(
                listOf(origin, origin),
                SelectionManager.selections.value.filterIsInstance<Selectable.ChainDevice>().map { it.parent },
            )

            UndoManager.redo()
            assertEquals(listOf(devices[0], devices[2]), origin.devices.value)
            assertEquals(listOf(devices[1], devices[3], existing), destination.devices.value)
        } finally {
            SelectionManager.clear()
        }
    }
}

package dev.anthonyhfm.amethyst.core.controls.undo

import dev.anthonyhfm.amethyst.core.heaven.elements.Chain

sealed interface UndoableAction {
    data class ChainDeviceCreation(
        val parent: Chain,
        val device: dev.anthonyhfm.amethyst.devices.ChainDevice<*>,
    ) : UndoableAction

    data class ChainDeviceRemoval(
        val parent: Chain,
        val device: dev.anthonyhfm.amethyst.devices.ChainDevice<*>,
    ) : UndoableAction

    data class MovedChainDevice(
        val chainBefore: Chain,
        val chainAfter: Chain,
        val device: dev.anthonyhfm.amethyst.devices.ChainDevice<*>,
        val fromIndex: Int,
        val toIndex: Int,
    ) : UndoableAction
}
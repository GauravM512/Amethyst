package dev.anthonyhfm.amethyst.core.selection

import dev.anthonyhfm.amethyst.core.heaven.elements.Chain
import dev.anthonyhfm.amethyst.core.util.UUID
import dev.anthonyhfm.amethyst.core.util.randomUUID

interface Selectable {
    val selectionUUID: String

    data class VirtualViewportDevice(
        override val selectionUUID: String = UUID.randomUUID()
    ) : Selectable

    data class ChainDevice(
        val parent: Chain,
        val device: dev.anthonyhfm.amethyst.devices.ChainDevice<*>,
        override val selectionUUID: String = device.selectionUUID
    ) : Selectable
}
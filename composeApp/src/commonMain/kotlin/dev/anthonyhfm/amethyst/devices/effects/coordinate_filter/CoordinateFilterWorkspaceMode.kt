package dev.anthonyhfm.amethyst.devices.effects.coordinate_filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract

class CoordinateFilterWorkspaceMode : WorkspaceContract.WorkspaceMode {
    override val displayName: String = "Coordinate-Filter Picker"
    override val selectable: Boolean = false

    var onVirtualDevicePress: ((x: Int, y: Int, offset: Offset, size: IntSize) -> Unit)? = null
    var modeWakeup: (() -> Unit)? = null
    var modeClose: (() -> Unit)? = null

    fun virtualDevicePress(x: Int, y: Int, offset: Offset, size: IntSize) {
        onVirtualDevicePress?.invoke(x, y, offset, size)
    }

    fun wake() {
        modeWakeup?.invoke()
    }

    fun close() {
        modeClose?.invoke()
    }
}
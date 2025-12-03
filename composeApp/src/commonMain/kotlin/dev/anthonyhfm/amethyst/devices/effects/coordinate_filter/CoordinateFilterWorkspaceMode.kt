package dev.anthonyhfm.amethyst.devices.effects.coordinate_filter

import androidx.compose.ui.geometry.Offset
import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.Event
import dev.anthonyhfm.amethyst.workspace.WorkspaceContract

class CoordinateFilterWorkspaceMode : WorkspaceContract.WorkspaceMode {
    override val displayName: String = "Coordinate-Filter Picker"
    override val selectable: Boolean = false
    override val claimInputs: Boolean = true

    var onVirtualDeviceDragStart: ((x: Int, y: Int) -> Unit)? = null
    var onVirtualDeviceDrag: ((x: Int, y: Int) -> Unit)? = null
    var onVirtualDeviceDragEnd: (() -> Unit)? = null
    var modeWakeup: (() -> Unit)? = null
    var modeClose: (() -> Unit)? = null

    override fun onMidiInput(data: MidiInputData, offset: Offset) = {
        val x: Int = data.pitch % 10
        val y: Int = 9 - (data.pitch / 10)

        if (data.velocity != 0) {
            onVirtualDeviceDragStart?.invoke(x + offset.x.toInt(), y  + offset.y.toInt())
            onVirtualDeviceDragEnd?.invoke()
        }
    }

    fun virtualDeviceDragStart(x: Int, y: Int) {
        onVirtualDeviceDragStart?.invoke(x, y)
    }

    fun virtualDeviceDrag(x: Int, y: Int) {
        onVirtualDeviceDrag?.invoke(x, y)
    }

    fun virtualDeviceDragEnd() {
        onVirtualDeviceDragEnd?.invoke()
    }

    fun wake() {
        modeWakeup?.invoke()
    }

    fun close() {
        modeClose?.invoke()
    }
}
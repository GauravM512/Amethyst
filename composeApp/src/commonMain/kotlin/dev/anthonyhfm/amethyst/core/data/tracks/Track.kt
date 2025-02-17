package dev.anthonyhfm.amethyst.core.data.tracks

import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.devices.BaseDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class Track <Device : BaseDevice<*, *>> {
    abstract val name: String
    abstract var projectDeviceIndex: Int?

    protected val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    open fun addDevice(device: BaseDevice<*, *>, atIndex: Int? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            if (atIndex == null) {
                _devices.emit(
                    value = _devices.value.plus(device) as List<Device>
                )
            } else {
                val mutableList = _devices.value.toMutableList()

                mutableList.add(atIndex, device as Device)

                _devices.emit(mutableList)
            }
        }
    }

    abstract fun processMidiInputData(midiInputData: MidiInputData)
}
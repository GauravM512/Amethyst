package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.workspace.ui.viewport.elements.LaunchpadViewportElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ViewportRepository {
    private val _devices = MutableStateFlow<List<LaunchpadViewportElement>>(emptyList())
    val devices: StateFlow<List<LaunchpadViewportElement>> = _devices.asStateFlow()

    fun setDevices(newDevices: List<LaunchpadViewportElement>) {
        _devices.value = newDevices
        Heaven.devices = newDevices
    }

    fun addDevice(device: LaunchpadViewportElement) {
        if (_devices.value.any { it.launchpadId == device.launchpadId }) {
            return
        }

        _devices.update { it + device }
        Heaven.devices = _devices.value
        WorkspaceRepository.markDirty()
    }

    fun removeDevice(uuid: String) {
        val remainingDevices = _devices.value.filter { device ->
            device.selectionUUID != uuid && device.launchpadId != uuid
        }

        if (remainingDevices.size == _devices.value.size) {
            return
        }

        _devices.value = remainingDevices
        Heaven.devices = _devices.value
        WorkspaceRepository.markDirty()
    }

    fun clear() {
        _devices.value = emptyList()
        Heaven.devices = emptyList()
    }
}

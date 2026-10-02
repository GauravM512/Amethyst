package dev.anthonyhfm.amethyst.workspace.ui.components

import dev.anthonyhfm.amethyst.core.midi.AmethystMidiManager
import dev.anthonyhfm.amethyst.workspace.ViewportRepository
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.modes.defaults.LayoutWorkspaceMode

data class IosMidiDeviceOption(
    val id: String,
    val name: String,
)

fun iosMidiDeviceOptions(): List<IosMidiDeviceOption> =
    AmethystMidiManager.detectedDevices.value.map { device ->
        IosMidiDeviceOption(
            id = device.id,
            name = device.friendlyName,
        )
    }

fun iosConfiguredMidiDeviceId(uuid: String): String? {
    val element = ViewportRepository.devices.value.firstOrNull { it.selectionUUID == uuid }
        ?: return null
    val devices = AmethystMidiManager.detectedDevices.value

    return devices.firstOrNull { it.id == element.savedMidiDeviceId }?.id
        ?: devices.firstOrNull { it.id == element.savedInputPortId }?.id
        ?: devices.firstOrNull { it.friendlyName == element.savedInputPortName }?.id
}

fun iosSaveMidiDeviceConfiguration(uuid: String, deviceId: String?) {
    if (WorkspaceRepository.mode.value is LayoutWorkspaceMode) {
        WorkspaceRepository.changeMidiDeviceConfig(uuid = uuid, deviceId = deviceId)
    }
}

package dev.anthonyhfm.amethyst.workspace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
actual fun WorkspaceDeviceDialogs(
    deviceConfigurationUuid: String?,
    showDevicePicker: Boolean,
) {
    if (deviceConfigurationUuid != null) {
        key(deviceConfigurationUuid) {
            AndroidDeviceConfigurationSheet(
                uuid = deviceConfigurationUuid,
            )
        }
    }

    if (showDevicePicker) {
        AndroidInsertLaunchpadSheet()
    }
}

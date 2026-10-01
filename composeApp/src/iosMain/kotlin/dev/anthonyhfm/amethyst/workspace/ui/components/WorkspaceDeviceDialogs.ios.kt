package dev.anthonyhfm.amethyst.workspace.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun WorkspaceDeviceDialogs(
    deviceConfigurationUuid: String?,
    showDevicePicker: Boolean,
) {
    DefaultWorkspaceDeviceDialogs(
        deviceConfigurationUuid = deviceConfigurationUuid,
        showDevicePicker = showDevicePicker,
    )
}

package dev.anthonyhfm.amethyst.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun SettingsDialog(visible: Boolean, onDismiss: () -> Unit) {
    if (visible) Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Settings(
            onBack = onDismiss
        )
    }
}
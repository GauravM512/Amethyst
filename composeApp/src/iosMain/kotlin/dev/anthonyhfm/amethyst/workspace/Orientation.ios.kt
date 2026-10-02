package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.core.util.isPhone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.anthonyhfm.amethyst.workspace.ui.components.IosWorkspaceBridge

@Composable
actual fun ForceScreenOrientation(landscape: Boolean) {
    DisposableEffect(key1 = Unit) {
        IosWorkspaceBridge.onOrientationChanged?.invoke(false)

        onDispose {
            IosWorkspaceBridge.onOrientationChanged?.invoke(false)
        }
    }
}

@Composable
actual fun isMobilePhone(): Boolean {
    return isPhone
}

actual fun triggerSettingsShow(onShowCommonDialog: () -> Unit) {
    IosWorkspaceBridge.onShowSettings?.invoke()
}

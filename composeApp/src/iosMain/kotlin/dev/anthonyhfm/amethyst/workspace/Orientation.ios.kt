package dev.anthonyhfm.amethyst.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.anthonyhfm.amethyst.workspace.ui.components.IosWorkspaceBridge
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPhone

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
    return UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone
}

actual fun triggerSettingsShow(onShowCommonDialog: () -> Unit) {
    IosWorkspaceBridge.onShowSettings?.invoke()
}

package dev.anthonyhfm.amethyst.workspace

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ForceScreenOrientation(landscape: Boolean) {
    val context = LocalContext.current

    DisposableEffect(key1 = Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun isMobilePhone(): Boolean {
    val context = LocalContext.current
    val resources = context.resources
    return resources.configuration.smallestScreenWidthDp < 600
}

actual fun triggerSettingsShow(onShowCommonDialog: () -> Unit) {
    onShowCommonDialog()
}

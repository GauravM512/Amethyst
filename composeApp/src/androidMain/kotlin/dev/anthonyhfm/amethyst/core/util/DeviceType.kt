package dev.anthonyhfm.amethyst.core.util

import android.content.Context
import android.content.res.Resources

object AndroidDeviceType {
    private var applicationResources: Resources? = null

    fun initialize(context: Context) {
        applicationResources = context.applicationContext.resources
    }

    val isPhone: Boolean
        get() = (applicationResources ?: Resources.getSystem()).configuration.smallestScreenWidthDp < 600
}

actual val isPhone: Boolean
    get() = AndroidDeviceType.isPhone

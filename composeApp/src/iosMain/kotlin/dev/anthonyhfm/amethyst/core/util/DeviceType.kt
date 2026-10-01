package dev.anthonyhfm.amethyst.core.util

import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPhone

actual val isPhone: Boolean
    get() = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone

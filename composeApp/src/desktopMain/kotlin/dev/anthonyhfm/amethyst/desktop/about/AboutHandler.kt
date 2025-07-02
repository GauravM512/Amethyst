package dev.anthonyhfm.amethyst.desktop.about

import androidx.annotation.Dimension
import androidx.annotation.Size
import androidx.compose.material.Text
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.unit.IntSize
import java.awt.Desktop

fun setupAboutHandler() {
    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()

        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler { event ->
                ComposeDialog().apply {
                    title = "About Amethyst"

                    setSize(320, 320)

                    setContent {
                        Text("If you are reading this, you might be qualified enough for QA testing. This is obviously not done yet, and I dont know why you would try it anyway")
                    }

                    show()
                }
            }
        }
    }
}
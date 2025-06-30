package dev.anthonyhfm.amethyst.desktop

import com.formdev.flatlaf.FlatDarkLaf
import dev.anthonyhfm.amethyst.ui.theme.AMETHYST_THEME
import javax.swing.UIManager

class FlatUtilityLaf : FlatDarkLaf() {
    override fun initialize() {
        super.initialize()

        UIManager.put(
            "RootPane.background",
            java.awt.Color(
                AMETHYST_THEME.surface.red,
                AMETHYST_THEME.surface.green,
                AMETHYST_THEME.surface.blue,
            )
        )

        UIManager.put(
            "TitlePane.buttonHoverBackground",
            java.awt.Color(
                AMETHYST_THEME.surfaceVariant.red,
                AMETHYST_THEME.surfaceVariant.green,
                AMETHYST_THEME.surfaceVariant.blue,
            )
        )

        UIManager.put(
            "TitlePane.closeHoverBackground",
            java.awt.Color(
                AMETHYST_THEME.errorContainer.red,
                AMETHYST_THEME.errorContainer.green,
                AMETHYST_THEME.errorContainer.blue,
            )
        )

        listOf("TitlePane.embeddedForeground", "TitlePane.foreground").forEach {
            UIManager.put(
                it,
                java.awt.Color(
                    AMETHYST_THEME.onSurface.red,
                    AMETHYST_THEME.onSurface.green,
                    AMETHYST_THEME.onSurface.blue,
                )
            )
        }
    }

    override fun isDark(): Boolean {
        return true
    }

    override fun getName(): String? {
        return "Amethyst Utility Theme"
    }

    override fun getDescription(): String? {
        return null
    }
}
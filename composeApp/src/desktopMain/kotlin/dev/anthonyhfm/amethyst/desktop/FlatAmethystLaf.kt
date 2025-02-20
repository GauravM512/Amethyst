package dev.anthonyhfm.amethyst.desktop

import com.formdev.flatlaf.FlatDarkLaf
import dev.anthonyhfm.amethyst.ui.theme.AMETHYST_THEME
import javax.swing.UIManager

class FlatAmethystLaf : FlatDarkLaf() {
    override fun initialize() {
        super.initialize()

        UIManager.put(
            "RootPane.background",
            java.awt.Color(
                AMETHYST_THEME.surfaceVariant.red,
                AMETHYST_THEME.surfaceVariant.green,
                AMETHYST_THEME.surfaceVariant.blue,
            )
        )

        UIManager.put(
            "TitlePane.buttonHoverBackground",
            java.awt.Color(
                AMETHYST_THEME.surfaceBright.red,
                AMETHYST_THEME.surfaceBright.green,
                AMETHYST_THEME.surfaceBright.blue,
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
                    AMETHYST_THEME.onSurfaceVariant.red,
                    AMETHYST_THEME.onSurfaceVariant.green,
                    AMETHYST_THEME.onSurfaceVariant.blue,
                )
            )
        }
    }

    override fun isDark(): Boolean {
        return true
    }

    override fun getName(): String? {
        return "Amethyst Theme"
    }

    override fun getDescription(): String? {
        return null
    }
}
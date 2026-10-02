package dev.anthonyhfm.amethyst.home.data

import java.nio.file.Path

internal object DesktopProjectStorage {
    val directory: Path
        get() {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val base = when {
                os.contains("mac") -> Path.of(home, "Library", "Application Support")
                os.contains("win") -> Path.of(System.getenv("APPDATA") ?: home)
                else -> Path.of(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share")
            }

            return base.resolve("Amethyst")
        }
}

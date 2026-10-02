package dev.anthonyhfm.amethyst.core.data.settings

import com.russhwolf.settings.Settings
import dev.anthonyhfm.amethyst.workspace.data.RecentWorkspace
import dev.anthonyhfm.amethyst.home.data.MobileProjectRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object GlobalSettings {
    private val settings: Settings = Settings()

    var recentWorkspaces: List<RecentWorkspace>
        get() {
            val jsonString = settings.getString("recentWorkspaces", "[]")

            return Json.decodeFromString<List<RecentWorkspace>>(jsonString).distinctBy { it.path }
        }
        set(value) {
            settings.putString("recentWorkspaces", Json.encodeToString(value.distinctBy { it.path }))
        }

    var mobileProjects: List<MobileProjectRecord>
        get() = runCatching {
            Json.decodeFromString<List<MobileProjectRecord>>(settings.getString("mobileProjects", "[]"))
        }.getOrDefault(emptyList())
        set(value) {
            settings.putString("mobileProjects", Json.encodeToString(value.distinctBy { it.id }))
        }

    // Persisted recent colors using a serializable RGB data class
    var recentColors: List<RecentColorRGB>
        get() {
            val jsonString = settings.getString("recentColors", "[]")
            return Json.decodeFromString(ListSerializer(RecentColorRGB.serializer()), jsonString)
        }
        set(value) {
            settings.putString("recentColors", Json.encodeToString(ListSerializer(RecentColorRGB.serializer()), value))
        }
}

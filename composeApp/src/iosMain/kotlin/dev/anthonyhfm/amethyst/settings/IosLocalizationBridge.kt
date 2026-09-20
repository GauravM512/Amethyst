package dev.anthonyhfm.amethyst.settings

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.allStringResources
import dev.anthonyhfm.amethyst.settings.data.GeneralSettings
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSUserDefaults

/** Exposes the shared Compose string resources to the native SwiftUI shell. */
object IosLocalizationBridge {
    private const val languageKey = "AppleLanguages"
    private var activeLanguageTag: String? = null
    private val stringCache = mutableMapOf<String, String>()

    val languageTag: String
        get() = GeneralSettings.language.value.languageTag

    fun activateLanguage(languageTag: String) {
        if (activeLanguageTag != languageTag) {
            activeLanguageTag = languageTag
            stringCache.clear()
        }
        NSUserDefaults.standardUserDefaults.setObject(listOf(languageTag), forKey = languageKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    fun string(key: String, fallback: String): String {
        return stringCache.getOrPut(key) {
            val resource = Res.allStringResources[key] ?: return@getOrPut fallback
            runCatching { runBlocking { getString(resource) } }.getOrDefault(fallback)
        }
    }
}

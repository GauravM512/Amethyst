package dev.anthonyhfm.amethyst.conversion.ableton.utils

data class AbletonImporterConfig(
    val launchpads: List<ConfigLaunchpad> = listOf(ConfigLaunchpad()),
) {
    data class ConfigLaunchpad(
        val lightsTrack: String = "Lights Channel",
        val audioTrack: String = "Audio Channel",
    )
}

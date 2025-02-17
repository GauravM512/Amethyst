package dev.anthonyhfm.amethyst.core.data.project

import dev.anthonyhfm.amethyst.devices.DeviceState
import kotlinx.serialization.Serializable

@Serializable
data class AmethystProject(
    val name: String,
    val tracks: List<TrackData> = emptyList(),
    val deviceConfig: List<ProjectDeviceConfig> = emptyList()
)

@Serializable
data class TrackData(
    val name: String,
    val type: TrackType,
    val devices: List<DeviceState>,
    val deviceConfigIndex: Int? = null
) {
    enum class TrackType {
        EFFECT,
        AUDIO
    }
}
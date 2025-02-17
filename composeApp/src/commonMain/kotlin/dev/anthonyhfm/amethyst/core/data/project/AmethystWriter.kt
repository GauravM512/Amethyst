package dev.anthonyhfm.amethyst.core.data.project

import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin

object AmethystWriter {
    fun getTemplateProject(): AmethystProject {
        return AmethystProject(
            name = "New Project",
            tracks = listOf(
                TrackData(
                    name = "Samples",
                    type = TrackData.TrackType.AUDIO,
                    devices = emptyList(),
                    deviceConfigIndex = 0
                ),
                TrackData(
                    name = "Lights",
                    type = TrackData.TrackType.EFFECT,
                    devices = emptyList(),
                    deviceConfigIndex = 0
                )
            ),
            deviceConfig = listOf(
                ProjectDeviceConfig(
                    name = "Launchpad"
                )
            )
        )
    }
}
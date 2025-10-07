package dev.anthonyhfm.amethyst.core.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class RecentColorRGB(
    val r: Float,
    val g: Float,
    val b: Float
)


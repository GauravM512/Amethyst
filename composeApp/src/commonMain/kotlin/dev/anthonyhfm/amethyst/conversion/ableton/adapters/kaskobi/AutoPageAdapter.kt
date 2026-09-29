package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.outbreak.utils.rythmIndexToDuration
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AutoPageAdapter(private val blob: String) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val data = jsonDecoder.decodeFromString<AutoPageData>(blob)

        return mutableListOf<DeviceState>().apply {
            val delayIndex = data.delay.firstOrNull()?.toInt() ?: 0
            val synchronized = data.synchronized.firstOrNull() != 0
            val timing = if (synchronized && delayIndex == 0) {
                Duration.ZERO
            } else if (synchronized) {
                val time = timeSplits.getOrNull(delayIndex) ?: timeSplits.first()
                rythmIndexToDuration(
                    timing = "${time.first}/${time.second}",
                    bpm = AbletonConverter.bpm,
                    steps = 1
                )
            } else {
                (data.delayMs.firstOrNull() ?: 0f).toDouble().milliseconds
            }

            if (timing.inWholeMilliseconds > 0L) {
                add(
                    DelayChainDeviceState(
                        timing = Timing.Duration(timing)
                    )
                )
            }

            add(
                MacroControlChainDeviceState(
                    macro = 0,
                    value = data.targetPage.first() - 1,
                )
            )
        }
    }

    private val timeSplits = listOf(
        Pair(0, 0),
        Pair(1, 1024),
        Pair(1, 512),
        Pair(1, 256),
        Pair(1, 128),
        Pair(1, 64),
        Pair(1, 32),
        Pair(1, 16),
        Pair(1, 8),
        Pair(1, 4),
        Pair(1, 2),
        Pair(1, 1),
        Pair(2, 1),
        Pair(4, 1),
    )

    @Serializable
    private data class AutoPageData(
        @SerialName("live.numbox")
        val targetPage: List<Int>,

        @SerialName("live.numbox[43]")
        val delay: List<Float>,

        @SerialName("live.numbox[44]")
        val delayMs: List<Float> = emptyList(),

        @SerialName("live.text[39]")
        val synchronized: List<Int> = listOf(1)
    )
}

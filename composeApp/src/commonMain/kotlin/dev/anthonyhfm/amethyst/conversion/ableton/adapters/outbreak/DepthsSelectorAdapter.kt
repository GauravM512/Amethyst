package dev.anthonyhfm.amethyst.conversion.ableton.adapters.outbreak

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.AbletonLayout
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.layer.LayerChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.offset.OffsetChainDeviceState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DepthsSelectorAdapter(
    private val data: ByteArray,
    private val offset: IntOffset
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val dataObj: DepthsSelectorData = Json {
            ignoreUnknownKeys = true
        }.decodeFromString(data.decodeToString())

        if (AbletonConverter.projectLayout is AbletonLayout.Dual2Light) {
            if (dataObj.channelField.isNotEmpty()) {
                if (this.offset == IntOffset.Zero) {
                    if (dataObj.channelField.first() == 1) {
                        return listOf(
                            OffsetChainDeviceState(offsetX = 10, offsetY = 0),
                            LayerChainDeviceState(layer = dataObj.layerField.first())
                        )
                    }
                } else {
                    if (dataObj.channelField.first() > 1) {
                        return listOf(
                            OffsetChainDeviceState(offsetX = -10, offsetY = 0),
                            LayerChainDeviceState(layer = dataObj.layerField.first())
                        )
                    }
                }
            }
        }

        return listOf(LayerChainDeviceState(layer = dataObj.layerField.first()))
    }

    @Serializable
    data class DepthsSelectorData(
        @SerialName("live.numbox")
        val channelField: List<Int> = listOf(0),

        @SerialName("live.numbox[1]")
        val layerField: List<Int> = listOf(0),
    )
}
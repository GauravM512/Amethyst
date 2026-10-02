package dev.anthonyhfm.amethyst.conversion.ableton.adapters.dovitate

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDevice
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiFileImporter
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CycleLightsAdapter(
    private val device: MxDevice,
    private val offset: IntOffset = IntOffset.Zero,
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val data = jsonDecoder.decodeFromString<CycleLightsData>(device.decodeBlob())
        val length = data.cycleLength.firstOrNull()?.toInt() ?: 0
        val paths = orderedPaths(
            length = length,
            fileDrops = device.fileDropList.fileDropList.items.associate { drop ->
                val path = drop.ref.fileRef
                    .takeIf { it.path?.value?.isNotBlank() == true }
                    ?.resolvePath()

                drop.name?.value.orEmpty() to path
            },
        )

        if (paths.isEmpty()) {
            return emptyList()
        }

        val groups = paths.mapIndexed { index, path ->
            val keyframes = path?.let { loadKeyframes(it) }

            Group(
                name = path?.substringAfterLast('/') ?: "Cycle ${index + 1}",
                stateChain = StateChain(
                    devices = listOf(keyframes ?: CoordinateFilterChainDeviceState()),
                ),
            )
        }

        return listOf(
            MultiGroupChainDeviceState(
                type = MultiGroupChainDeviceState.TYPE.FORWARD,
                groups = groups,
            )
        )
    }

    private fun loadKeyframes(path: String): DeviceState? {
        val bytes = if (AbletonConverter.isZip) {
            AbletonConverter.readZipEntry(path)
        } else {
            runCatching {
                runBlocking { PlatformFile(path).readBytes() }
            }.getOrNull()
        } ?: return null

        return MidiFileImporter.loadData(
            data = bytes,
            palette = AbletonConverter.palette,
            bpm = AbletonConverter.bpm,
            launchpad = AbletonConverter.launchpadTarget(offset).midiImportTarget(),
        )
    }

    @Serializable
    private data class CycleLightsData(
        @SerialName("live.numbox[8]")
        val cycleLength: List<Float> = emptyList(),
    )

    companion object {
        private val slotParameterIndices = listOf(
            185, 186, 187, 188, 189, 190, 191, 192,
            197, 199, 200, 201, 202, 198, 203, 204,
        )

        internal fun orderedPaths(
            length: Int,
            fileDrops: Map<String, String?>,
        ): List<String?> {
            return slotParameterIndices
                .take(length.coerceIn(0, slotParameterIndices.size))
                .map { index -> fileDrops["live.drop[$index]"] }
        }
    }
}

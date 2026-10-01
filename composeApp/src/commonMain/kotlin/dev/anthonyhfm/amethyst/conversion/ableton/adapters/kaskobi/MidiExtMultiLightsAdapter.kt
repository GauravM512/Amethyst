package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

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
import kotlinx.serialization.Serializable

class MidiExtMultiLightsAdapter(
    private val device: MxDevice,
    private val offset: IntOffset,
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val settings = jsonDecoder.decodeFromString<MidiExtMultiSettings>(string = device.decodeBlob())
        val drops = device.fileDropList.fileDropList.items.associateBy { it.name?.value }
        val groups = slotNames.take(n = settings.cycleLength).mapIndexed { index, name ->
            val fileRef = drops[name]?.ref?.fileRef
            val path = fileRef?.resolvePath()
            val keyframes = path?.let { loadKeyframes(path = it) }

            Group(
                name = path?.substringAfterLast(delimiter = '/') ?: "Multi Item ${index + 1}",
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
            AbletonConverter.readZipEntry(path = path)
        } else {
            runCatching {
                runBlocking { PlatformFile(path = path).readBytes() }
            }.getOrNull()
        } ?: return null

        return MidiFileImporter.loadData(
            data = bytes,
            palette = AbletonConverter.palette,
            bpm = AbletonConverter.bpm,
            launchpad = AbletonConverter.launchpadTarget(offset = offset).midiImportTarget(),
        )
    }

    companion object {
        private val slotNames = listOf(
            "live.drop", "live.drop[7]", "live.drop[9]", "live.drop[10]",
            "live.drop[11]", "live.drop[8]", "live.drop[12]", "live.drop[13]",
            "live.drop[14]", "live.drop[15]", "live.drop[16]", "live.drop[17]",
            "live.drop[18]", "live.drop[19]", "live.drop[20]", "live.drop[21]",
        )
    }
}

@Serializable
internal data class MidiExtMultiSettings(
    val num: List<Int> = emptyList(),
) {
    val cycleLength: Int
        get() = (num.firstOrNull() ?: 1).coerceIn(minimumValue = 1, maximumValue = 16)
}

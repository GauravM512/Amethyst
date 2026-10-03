package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonLaunchpadLayout
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.InstrumentGroupAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.MxDeviceMidiEffectAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.utils.MultiPluginHashes.MIDIEXT_MULTI_SAMPLE_HASH
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceMidiEffect
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MidiExtMultiAdapterTest {
    @Test
    fun multiLightsImportsAllSixteenSlotsInPresentationOrder() = withProject { directory ->
        val slotNames = listOf(
            "live.drop", "live.drop[7]", "live.drop[9]", "live.drop[10]",
            "live.drop[11]", "live.drop[8]", "live.drop[12]", "live.drop[13]",
            "live.drop[14]", "live.drop[15]", "live.drop[16]", "live.drop[17]",
            "live.drop[18]", "live.drop[19]", "live.drop[20]", "live.drop[21]",
        )
        val drops = slotNames.mapIndexed { index, name ->
            val fileName = "slot-${index + 1}.mid"
            directory.resolve(relative = fileName).writeBytes(array = midiData(pitch = 36 + index))
            """<MxDFullFileDrop Id="$index"><Name Value="$name"/><FileRef>${fileRef(name = fileName)}</FileRef></MxDFullFileDrop>"""
        }.reversed().joinToString(separator = "")
        val device = AbletonConverter.xml.decodeFromString<MxDeviceMidiEffect>(
            string = maxDevice(name = "MIDIext2.1 Multi-Light.amxd", length = 16, drops = drops),
        )
        MxDeviceMidiEffectAdapter.fileHashMap[device.patchSlot.value.patchRef!!.fileRef.resolvePath()] =
            "4957d3dcbbb5b6fc53ed00f032cc9b24"

        val multi = assertIs<MultiGroupChainDeviceState>(
            value = MxDeviceMidiEffectAdapter(device = device).toDeviceStates().single(),
        )

        assertEquals(expected = MultiGroupChainDeviceState.TYPE.FORWARD, actual = multi.type)
        assertEquals(expected = (1..16).map { "slot-$it.mid" }, actual = multi.groups.map { it.name })
        assertEquals(
            expected = (36..51).toList(),
            actual = multi.groups.map { group ->
                assertIs<KeyframesChainDeviceState>(value = group.stateChain.devices.single())
                    .frames.first().entries.single().abletonPitch
            },
        )
    }

    @Test
    fun multiLightsKeepsEmptyAndMissingSlotsWithinCycleLength() = withProject { directory ->
        directory.resolve(relative = "first.mid").writeBytes(array = midiData(pitch = 36))
        val drops = """
            <MxDFullFileDrop><Name Value="live.drop"/><FileRef>${fileRef(name = "first.mid")}</FileRef></MxDFullFileDrop>
            <MxDFullFileDrop><Name Value="live.drop[9]"/><FileRef>${fileRef(name = "missing.mid")}</FileRef></MxDFullFileDrop>
            <MxDFullFileDrop><Name Value="live.drop[10]"/><FileRef>${fileRef(name = "first.mid")}</FileRef></MxDFullFileDrop>
        """.trimIndent()
        val device = AbletonConverter.xml.decodeFromString<MxDeviceMidiEffect>(
            string = maxDevice(name = "MIDIext2.1 Multi-Light.amxd", length = 3, drops = drops),
        )
        val multi = assertIs<MultiGroupChainDeviceState>(
            value = MidiExtMultiLightsAdapter(device = device, offset = IntOffset.Zero).toDeviceStates().single(),
        )

        assertEquals(expected = 3, actual = multi.groups.size)
        assertIs<KeyframesChainDeviceState>(value = multi.groups[0].stateChain.devices.single())
        assertIs<CoordinateFilterChainDeviceState>(value = multi.groups[1].stateChain.devices.single())
        assertIs<CoordinateFilterChainDeviceState>(value = multi.groups[2].stateChain.devices.single())
    }

    @Test
    fun multiSamplesImportsRackUsingBlobCycleLengthAndCompensatesPitch() = withProject {
        val branches = (0..3).joinToString(separator = "") { index ->
            instrumentBranch(
                index = index,
                name = "Sample ${index + 1}",
                key = 60 + index,
                devices = """
                    <OriginalSimpler><Player><MultiSampleMap><SampleParts/></MultiSampleMap></Player>
                    <Pitch><TransposeKey><Manual Value="${-index}"/></TransposeKey></Pitch><VolumeAndPan/></OriginalSimpler>
                """.trimIndent(),
            )
        }
        val container = instrumentRack(branches = branches)
        val max = maxDevice(name = "MIDIext2.1 Multi-Sample.amxd", length = 4)
        val outer = AbletonConverter.xml.decodeFromString<InstrumentGroupDevice>(
            string = instrumentRack(
                branches = instrumentBranch(index = 0, name = "Pad", key = 36, devices = max + container),
            ),
        )
        val group = assertIs<GroupChainDeviceState>(
            value = InstrumentGroupAdapter(device = outer).toDeviceStates().single(),
        )
        val multi = group.groups.single().stateChain.devices.filterIsInstance<MultiGroupChainDeviceState>().single()

        assertEquals(expected = 4, actual = multi.groups.size)
        assertEquals(expected = (1..4).map { "Sample $it" }, actual = multi.groups.map { it.name })
        assertTrue(actual = multi.groups.all { group ->
            assertIs<SampleChainDeviceState>(value = group.stateChain.devices.single()).transposeSemitones == 0f
        })
    }

    private fun maxDevice(name: String, length: Int, drops: String = ""): String {
        val blob = """{"live.numbox":[1.0],"num":[$length]}"""
            .encodeToByteArray().joinToString(separator = "") { "%02x".format(it) }
        return """
            <MxDeviceMidiEffect><PatchSlot><Value><MxDPatchRef>${fileRef(name = name)}</MxDPatchRef></Value></PatchSlot>
            <BlobSlot><Value><MxDBlob><Blob>$blob</Blob></MxDBlob></Value></BlobSlot>
            <ParameterList><ParameterList/></ParameterList><FileDropList><FileDropList>$drops</FileDropList></FileDropList></MxDeviceMidiEffect>
        """.trimIndent()
    }

    private fun fileRef(name: String): String =
        """<FileRef><RelativePath/><Name Value="$name"/><Type Value="2"/></FileRef>"""

    private fun instrumentRack(branches: String): String =
        """<InstrumentGroupDevice Id="0"><ChainSelector/><Branches>$branches</Branches></InstrumentGroupDevice>"""

    private fun instrumentBranch(index: Int, name: String, key: Int, devices: String): String = """
        <InstrumentBranch Id="$index"><Name><EffectiveName Value="$name"/></Name>
        <DeviceChain><MidiToAudioDeviceChain><Devices>$devices</Devices></MidiToAudioDeviceChain></DeviceChain>
        <ZoneSettings><KeyRange><Min Value="$key"/><Max Value="$key"/></KeyRange></ZoneSettings>
        <BranchSelectorRange><Min Value="0"/><Max Value="0"/></BranchSelectorRange>
        <MixerDevice><Speaker><Manual Value="true"/></Speaker></MixerDevice></InstrumentBranch>
    """.trimIndent()

    private fun withProject(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("amethyst-midiext-multi-").toFile()
        val previousFile = AbletonConverter.file
        val previousBpm = AbletonConverter.bpm
        val previousLayout = AbletonConverter.launchpadLayout
        val previousHashes = MxDeviceMidiEffectAdapter.fileHashMap.toMap()

        try {
            AbletonConverter.file = PlatformFile(path = directory.resolve(relative = "Project.als").absolutePath)
            AbletonConverter.bpm = 120.0
            AbletonConverter.launchpadLayout = AbletonLaunchpadLayout.create(count = 1)
            val patch = directory.resolve(relative = "MIDIext2.1 Multi-Sample.amxd")
            patch.writeBytes(array = byteArrayOf())
            MxDeviceMidiEffectAdapter.fileHashMap[PlatformFile(path = patch.absolutePath).path] = MIDIEXT_MULTI_SAMPLE_HASH
            block(directory)
        } finally {
            AbletonConverter.file = previousFile
            AbletonConverter.bpm = previousBpm
            AbletonConverter.launchpadLayout = previousLayout
            MxDeviceMidiEffectAdapter.fileHashMap.clear()
            MxDeviceMidiEffectAdapter.fileHashMap.putAll(from = previousHashes)
            directory.deleteRecursively()
        }
    }

    private fun midiData(pitch: Int): ByteArray {
        val track = listOf(0, 0x90, pitch, 5, 96, 0x80, pitch, 0, 0, 0xFF, 0x2F, 0)
        val header = listOf(0x4D, 0x54, 0x68, 0x64, 0, 0, 0, 6, 0, 0, 0, 1, 0, 96, 0x4D, 0x54, 0x72, 0x6B, 0, 0, 0, track.size)
        return (header + track).map(Int::toByte).toByteArray()
    }
}

package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonLaunchpadLayout
import dev.anthonyhfm.amethyst.conversion.ableton.data.FileRef
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.AbletonIndex
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceBlobSlot
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceFileDropList
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceMidiEffect
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceParameterList
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDevicePatchSlot
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxParameter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxParameterName
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiFileImporter
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDevice
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MidiLauncherAdapterTest {
    @Test
    fun xmlRetainsParameterNamesForEveryNumericType() {
        val intParameter = AbletonConverter.xml.decodeFromString<MxParameter.MxDIntParameter>(
            string = """<MxDIntParameter Id="1"><Name Value="Skip Silence"/><Index Value="17"/><Timeable><Manual Value="1"/></Timeable></MxDIntParameter>"""
        )
        val enumParameter = AbletonConverter.xml.decodeFromString<MxParameter.MxDEnumParameter>(
            string = """<MxDEnumParameter Id="2"><Name Value="Loop"/><Index Value="8"/><Timeable><Manual Value="1"/></Timeable></MxDEnumParameter>"""
        )
        val floatParameter = AbletonConverter.xml.decodeFromString<MxParameter.MxDFloatParameter>(
            string = """<MxDFloatParameter Id="3"><Name Value="msDelay"/><Index Value="30"/><Timeable><Manual Value="321.5"/></Timeable></MxDFloatParameter>"""
        )
        val unnamedParameter = AbletonConverter.xml.decodeFromString<MxParameter.MxDIntParameter>(
            string = """<MxDIntParameter Id="1"><Index Value="0"/><Timeable><Manual Value="1"/></Timeable></MxDIntParameter>"""
        )

        assertEquals(expected = "Skip Silence", actual = intParameter.name?.value)
        assertEquals(expected = "Loop", actual = enumParameter.name?.value)
        assertEquals(expected = "msDelay", actual = floatParameter.name?.value)
        assertNull(actual = unnamedParameter.name)
    }

    @Test
    fun basicLauncherHashFallbackFindsIndexInReorderedParameterList() {
        withMidiFile { path ->
            for ((hash, index) in mapOf(
                "2ef098a53fe4e9a4b035588561080343" to 0,
                "f135067227057b08f8d2d2ae66a22f8d" to 1
            )) {
                val parameters = listOf(
                    intParameter(index = 40, value = 0),
                    intParameter(index = index, value = 1)
                )
                val state = convert(path = path, hash = hash, parameters = parameters)

                assertTrue(actual = state.frames.first().entries.isNotEmpty())
                assertEquals(expected = 2, actual = state.frames.size)
                assertTrue(actual = state.renderedAnimation.isEmpty())

                val keyframes = KeyframesChainDevice()
                keyframes.state.value = state
                keyframes.renderAnimation()

                assertEquals(expected = 0, actual = keyframes.state.value.renderedAnimation.first().first)
                assertEquals(expected = 500, actual = keyframes.state.value.renderedAnimation.last().first)
            }
        }
    }

    @Test
    fun namedSkipSilenceOverridesFallbackAndDisabledSkipPreservesTiming() {
        withMidiFile { path ->
            val parameters = listOf(
                intParameter(index = 0, value = 1),
                intParameter(index = 73, value = 0, name = "Skip Silence")
            )
            val state = convert(
                path = path,
                hash = "2ef098a53fe4e9a4b035588561080343",
                parameters = parameters
            )

            assertTrue(actual = state.frames.first().entries.isEmpty())
            assertEquals(expected = 3, actual = state.frames.size)
            assertEquals(expected = 1000, actual = state.renderedAnimation.last().first)
        }
    }

    @Test
    fun namedSkipSilenceSupportsNewIndicesWithoutKnownHash() {
        withMidiFile { path ->
            val state = convert(
                path = path,
                hash = "unknown",
                parameters = listOf(intParameter(index = 73, value = 1, name = "Skip Silence"))
            )

            assertTrue(actual = state.frames.first().entries.isNotEmpty())
            assertEquals(expected = 2, actual = state.frames.size)
        }
    }

    @Test
    fun endOfTrackTimingAvoidsExtraSilenceAfterFinalNoteOff() {
        val target = MidiFileImporter.DeviceTarget(
            launchpadId = "launcher-test",
            offset = IntOffset.Zero
        )
        val track = listOf(
            0, 0x90, 36, 5,
            96, 0x80, 36, 0,
            0, 0xFF, 0x2F, 0
        )
        val preserved = MidiFileImporter.loadData(
            data = midiData(track = track),
            launchpad = target,
            preserveEndOfTrackTiming = true
        )
        val defaultTiming = MidiFileImporter.loadData(
            data = midiData(track = track),
            launchpad = target
        )

        assertEquals(expected = 500, actual = preserved.renderedAnimation.last().first)
        assertEquals(expected = 550, actual = defaultTiming.renderedAnimation.last().first)
    }

    @Test
    fun endOfTrackTimingRetainsTrailingSilenceAndHeldNoteDuration() {
        val target = MidiFileImporter.DeviceTarget(
            launchpadId = "launcher-test",
            offset = IntOffset.Zero
        )
        val trailingSilence = MidiFileImporter.loadData(
            data = midiData(
                track = listOf(
                    0, 0x90, 36, 5,
                    96, 0x80, 36, 0,
                    96, 0xFF, 0x2F, 0
                )
            ),
            launchpad = target,
            preserveEndOfTrackTiming = true
        )
        val heldNote = MidiFileImporter.loadData(
            data = midiData(
                track = listOf(
                    0, 0x90, 36, 5,
                    96, 0xFF, 0x2F, 0
                )
            ),
            launchpad = target,
            preserveEndOfTrackTiming = true
        )

        assertEquals(expected = 1000, actual = trailingSilence.renderedAnimation.last().first)
        assertEquals(expected = 500, actual = heldNote.renderedAnimation.last().first)
        assertEquals(expected = 1, actual = heldNote.frames.size)
        assertTrue(actual = heldNote.frames.single().entries.isNotEmpty())
    }

    @Test
    fun endOfTrackAtZeroDoesNotInventDurationForSingleFrameMidi() {
        val state = MidiFileImporter.loadData(
            data = midiData(
                track = listOf(
                    0, 0x90, 36, 5,
                    0, 0xFF, 0x2F, 0
                )
            ),
            launchpad = MidiFileImporter.DeviceTarget(
                launchpadId = "launcher-test",
                offset = IntOffset.Zero
            ),
            preserveEndOfTrackTiming = true
        )

        assertEquals(expected = 1, actual = state.frames.size)
        assertEquals(expected = 0, actual = state.renderedAnimation.last().first)
    }

    @Test
    fun repeatedNoteZeroAtEndOfTrackKeepsItsTrigger() {
        val state = MidiFileImporter.loadData(
            data = midiData(
                track = listOf(
                    0, 0x90, 0, 5,
                    96, 0x90, 0, 5,
                    0, 0xFF, 0x2F, 0
                )
            ),
            launchpad = MidiFileImporter.DeviceTarget(
                launchpadId = "launcher-test",
                offset = IntOffset.Zero
            ),
            preserveEndOfTrackTiming = true
        )

        assertEquals(expected = 2, actual = state.frames.size)
        assertTrue(actual = state.frames.all { it.triggersNoteZero })
        assertEquals(expected = 500, actual = state.renderedAnimation.last().first)
    }

    private fun intParameter(
        index: Int,
        value: Int,
        name: String? = null
    ): MxParameter.MxDIntParameter = MxParameter.MxDIntParameter(
        indexObj = AbletonIndex(value = index),
        timeable = MxParameter.MxParameterValue(manual = AbletonManual(value = value)),
        name = name?.let { MxParameterName(value = it) }
    )

    private fun convert(
        path: String,
        hash: String,
        parameters: List<MxParameter>
    ): KeyframesChainDeviceState {
        val device = MxDeviceMidiEffect(
            patchSlot = MxDevicePatchSlot(value = MxDevicePatchSlot.Value()),
            blobSlot = MxDeviceBlobSlot(
                value = MxDeviceBlobSlot.Value(
                    mxdBlob = MxDeviceBlobSlot.Value.MxDBlob(
                        blob = MxDeviceBlobSlot.Value.MxDBlob.Blob(value = "")
                    )
                )
            ),
            parameterList = MxDeviceParameterList(
                parameterList = MxDeviceParameterList.ParameterList(parameters = parameters)
            ),
            fileDropList = MxDeviceFileDropList(
                fileDropList = MxDeviceFileDropList.FileDropList(
                    items = listOf(
                        MxDeviceFileDropList.FileDropList.MxDFullFileDrop(
                            ref = MxDeviceFileDropList.FileDropList.MxDFullFileDrop.FileRefRef(
                                fileRef = FileRef(
                                    relativePath = FileRef.RelativePath(),
                                    path = FileRef.Path(value = path),
                                    type = FileRef.Type(value = 1)
                                )
                            )
                        )
                    )
                )
            )
        )

        return assertIs<KeyframesChainDeviceState>(
            value = MidiLauncherAdapter(
                device = device,
                hash = hash,
                offset = IntOffset.Zero
            ).toDeviceStates().single()
        )
    }

    private fun withMidiFile(block: (String) -> Unit) {
        val file = Files.createTempFile("amethyst-midi-launcher-", ".mid").toFile()
        val previousFile = AbletonConverter.file
        val previousBpm = AbletonConverter.bpm
        val previousLaunchpadLayout = AbletonConverter.launchpadLayout

        try {
            file.writeBytes(array = midiData())
            AbletonConverter.file = PlatformFile(path = file.absolutePath)
            AbletonConverter.bpm = 120.0
            AbletonConverter.launchpadLayout = AbletonLaunchpadLayout.create(count = 1)
            block(file.absolutePath)
        } finally {
            AbletonConverter.file = previousFile
            AbletonConverter.bpm = previousBpm
            AbletonConverter.launchpadLayout = previousLaunchpadLayout
            file.delete()
        }
    }

    private fun midiData(
        track: List<Int> = listOf(
            96, 0x90, 36, 5,
            96, 0x80, 36, 0,
            0, 0xFF, 0x2F, 0
        )
    ): ByteArray {
        val header = listOf(
            0x4D, 0x54, 0x68, 0x64,
            0, 0, 0, 6,
            0, 0,
            0, 1,
            0, 96,
            0x4D, 0x54, 0x72, 0x6B,
            0, 0, 0, track.size
        )

        return (header + track).map(Int::toByte).toByteArray()
    }
}

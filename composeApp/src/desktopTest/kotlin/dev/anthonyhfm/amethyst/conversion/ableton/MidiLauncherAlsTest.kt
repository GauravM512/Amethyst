package dev.anthonyhfm.amethyst.conversion.ableton

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.MxDeviceInstrumentAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.MxDeviceMidiEffectAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDeviceMidiEffect
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxParameter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiChainReader
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.PlaybackMode
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MidiLauncherAlsTest {
    @Test
    fun savedAbletonProjectImportsProSettingsAndRelativeMidiFile() {
        val directory = Files.createTempDirectory("amethyst-midi-launcher-als-").toFile()
        val previousFile = AbletonConverter.file
        val previousBpm = AbletonConverter.bpm
        val previousLayout = AbletonConverter.launchpadLayout
        val previousHashes = MxDeviceMidiEffectAdapter.fileHashMap.toMap()
        val previousInstrumentHashes = MxDeviceInstrumentAdapter.fileHashMap.toMap()

        try {
            val als = directory.resolve(relative = "Conversion Validation.als")
            val midi = directory.resolve(relative = "Samples/MIDI/Conversion Validation.mid")
            assertTrue(actual = midi.parentFile.mkdirs())
            als.writeBytes(array = resource(path = "Conversion Validation.als"))
            midi.writeBytes(array = resource(path = "Samples/MIDI/Conversion Validation.mid"))

            AbletonConverter.file = PlatformFile(path = als.absolutePath)
            val layout = AbletonLaunchpadLayout.create(count = 1)
            AbletonConverter.launchpadLayout = layout
            val ableton = AbletonXmlDecoder.decodeFile(
                path = als.absolutePath,
                xml = AbletonConverter.xml,
            )
            AbletonConverter.bpm = ableton.liveSet.masterTrack.deviceChain.mixer.tempo.manual.value
            assertEquals(expected = 120.0, actual = AbletonConverter.bpm)
            assertEquals(expected = 2, actual = ableton.liveSet.tracks.midiTracks.size)

            val track = ableton.liveSet.tracks.midiTracks.single {
                MidiChainReader.getAllDevicesOfType<MxDeviceMidiEffect>(track = it).any { device ->
                    device.id == 3
                }
            }
            val devices = MidiChainReader.getAllDevicesOfType<MxDeviceMidiEffect>(track = track)
            assertEquals(expected = 5, actual = devices.size)
            val pro = devices.single { it.id == 3 }
            val skipSilence = assertIs<MxParameter.MxDIntParameter>(
                value = pro.parameterList.parameterList.parameters.single { it.index == 17 }
            )
            assertEquals(expected = 1, actual = skipSilence.timeable.manual.value)
            val fileRef = assertNotNull(actual = pro.fileDropList.fileDropList.items.single().ref.fileRef)
            assertEquals(expected = "", actual = fileRef.path?.value)
            assertEquals(expected = midi.absolutePath, actual = fileRef.resolvePath())

            MxDeviceMidiEffectAdapter.fileHashMap.clear()
            devices.forEach { device ->
                val patchRef = assertNotNull(actual = device.patchSlot.value.patchRef?.fileRef)
                val hash = when (patchRef.relativePath.value?.substringAfterLast(delimiter = '/')) {
                    "Page%20Switcher%20v3.0.amxd" -> "feecaed62c2637a73325446a1ed1e25e"
                    "MIDI Launcher v6.0.2.amxd" -> "f135067227057b08f8d2d2ae66a22f8d"
                    "MIDI Launcher Pro v6.1.3.amxd" -> "a114e5d1a7710271501649668c14f1ab"
                    else -> error("Unexpected Max patch in saved fixture")
                }
                MxDeviceMidiEffectAdapter.fileHashMap[patchRef.resolvePath()] = hash
            }

            val chain = MidiChainReader(offset = IntOffset.Zero).readMidiChain(midiTrack = track)
            val delay = chain.devices.filterIsInstance<DelayChainDeviceState>().single()
            val timing = assertIs<Timing.Duration>(value = delay.timing)
            assertEquals(expected = 100L, actual = delay.delayMs)
            assertTrue(actual = kotlin.math.abs(timing.duration.inWholeNanoseconds / 1_000_000.0 - 100.0) < 0.001)

            val keyframes = chain.devices.filterIsInstance<KeyframesChainDeviceState>().single()
            val launchpadId = layout.target(index = 0).launchpad.id
            assertEquals(expected = PlaybackMode.Continuous, actual = keyframes.playbackMode)
            assertEquals(expected = 88, actual = keyframes.rootKey)
            assertEquals(expected = launchpadId, actual = keyframes.rootKeyLaunchpadId)
            assertTrue(actual = keyframes.isolate)
            assertEquals(expected = 3, actual = keyframes.frames.size)
            assertEquals(expected = 71, actual = keyframes.frames.first().entries.single().abletonPitch)
            assertEquals(expected = 42, actual = keyframes.frames[1].entries.single().abletonPitch)
            assertTrue(actual = keyframes.frames.last().entries.isEmpty())
            assertEquals(
                expected = listOf(250L, 250L, 500L),
                actual = keyframes.frames.map {
                    assertIs<Timing.Duration>(value = it.timing).duration.inWholeMilliseconds
                },
            )
            val mirrored = keyframes.frames.first().entries.single()
            assertEquals(expected = 8, actual = mirrored.localX)
            assertEquals(expected = 8, actual = mirrored.localY)
            assertEquals(expected = 8, actual = mirrored.x)
            assertEquals(expected = 8, actual = mirrored.y)
            assertTrue(actual = keyframes.frames.flatMap { it.entries }.all {
                it.launchpadId == launchpadId && it.isDeviceAnchored
            })

            val workspace = AbletonConverter.runLiveConversion(
                name = "Midi Launcher Validation",
                abletonData = ableton,
                reporter = null,
            )
            val importedDevices = allDevices(chain = workspace.lights)
            val importedDelay = importedDevices.filterIsInstance<DelayChainDeviceState>().single()
            assertEquals(expected = delay, actual = importedDelay)

            val importedKeyframes = importedDevices.filterIsInstance<KeyframesChainDeviceState>().single()
            val importedLaunchpadId = workspace.launchpadDevices.first().id
            assertEquals(expected = PlaybackMode.Continuous, actual = importedKeyframes.playbackMode)
            assertTrue(actual = importedKeyframes.infinity)
            assertEquals(expected = 88, actual = importedKeyframes.rootKey)
            assertEquals(expected = importedLaunchpadId, actual = importedKeyframes.rootKeyLaunchpadId)
            assertEquals(expected = 71, actual = importedKeyframes.frames.first().entries.single().abletonPitch)
            assertEquals(expected = 42, actual = importedKeyframes.frames[1].entries.single().abletonPitch)
            assertTrue(actual = importedKeyframes.frames.flatMap { it.entries }.all {
                it.launchpadId == importedLaunchpadId && it.isDeviceAnchored
            })
        } finally {
            AbletonConverter.file = previousFile
            AbletonConverter.bpm = previousBpm
            AbletonConverter.launchpadLayout = previousLayout
            MxDeviceMidiEffectAdapter.fileHashMap.clear()
            MxDeviceMidiEffectAdapter.fileHashMap.putAll(from = previousHashes)
            MxDeviceInstrumentAdapter.fileHashMap.clear()
            MxDeviceInstrumentAdapter.fileHashMap.putAll(from = previousInstrumentHashes)
            directory.deleteRecursively()
        }
    }

    private fun allDevices(chain: StateChain): List<DeviceState> =
        chain.devices.flatMap { device ->
            val children = when (device) {
                is GroupChainDeviceState -> device.groups.flatMap { allDevices(chain = it.stateChain) }
                is MultiGroupChainDeviceState ->
                    allDevices(chain = device.preprocessChain) +
                        device.groups.flatMap { allDevices(chain = it.stateChain) }
                is ChokeChainDeviceState -> allDevices(chain = device.stateChain)
                else -> emptyList()
            }

            listOf(device) + children
        }

    private fun resource(path: String): ByteArray =
        assertNotNull(actual = javaClass.getResourceAsStream("/conversion/ableton/midi-launcher-pro-6.1.3/$path"))
            .use { it.readBytes() }
}

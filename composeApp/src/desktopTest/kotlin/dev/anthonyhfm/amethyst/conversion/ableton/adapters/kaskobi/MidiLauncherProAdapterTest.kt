package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import androidx.compose.ui.graphics.Color
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
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.devices.ableton.AbletonNoteSpace
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState.ChokeMode
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDevice
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDevice
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.Frame
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesEntry
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.PlaybackMode
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MidiLauncherProAdapterTest {
    @Test
    fun importsMidiWithVersionSpecificSkipSilenceDespiteSparseReorderedParameters() {
        withMidiFile { path ->
            for ((hash, index) in listOf(
                latestHash to 17,
                "2d5d5420fea42678807d1569ce08b182" to 16,
                "34bcbf910a9985951a0dd6ead9f8fc4c" to 15,
            )) {
                val state = assertIs<KeyframesChainDeviceState>(
                    value = adapter(
                        hash = hash,
                        path = path,
                        parameters = listOf(
                            intParameter(index = 99, value = 0),
                            intParameter(index = index, value = 1),
                            intParameter(index = 4, value = 0),
                        ),
                    ).toDeviceStates().single()
                )

                assertEquals(expected = 2, actual = state.frames.size)
                assertTrue(actual = state.frames.first().entries.isNotEmpty())
                val rendered = render(state = state)
                assertEquals(expected = 0, actual = rendered.first().first)
                assertEquals(expected = 500, actual = rendered.last().first)
            }
        }
    }

    @Test
    fun namedSkipSilenceOverridesNewVersionIndexAndPreservesLeadingSilence() {
        withMidiFile { path ->
            val state = assertIs<KeyframesChainDeviceState>(
                value = adapter(
                    path = path,
                    parameters = listOf(
                        intParameter(index = 17, value = 1),
                        intParameter(index = 91, value = 0, name = "Skip Silence"),
                    ),
                ).toDeviceStates().single()
            )

            assertEquals(expected = 3, actual = state.frames.size)
            assertTrue(actual = state.frames.first().entries.isEmpty())
            assertEquals(expected = 1000, actual = render(state = state).last().first)
        }
    }

    @Test
    fun newVersionSkipSilenceDoesNotUseLegacyIndex() {
        val state = assertIs<KeyframesChainDeviceState>(
            value = convert(
                parameters = listOf(
                    intParameter(index = 16, value = 1),
                    intParameter(index = 17, value = 0),
                )
            ).single()
        )

        assertEquals(expected = 3, actual = state.frames.size)
        assertEquals(expected = 350, actual = render(state = state).last().first)
    }

    @Test
    fun millisecondsDelayRestoresNonlinearMaxParameterAsOneHundredMilliseconds() {
        val devices = convert(
            parameters = listOf(
                floatParameter(index = 30, value = 31320.877f),
                intParameter(index = 5, value = 0),
                intParameter(index = 4, value = 1),
            )
        )
        val delay = assertIs<DelayChainDeviceState>(value = devices.first())
        val timing = assertIs<Timing.Duration>(value = delay.timing)

        assertTrue(actual = kotlin.math.abs(timing.duration.inWholeNanoseconds / 1_000_000.0 - 100.0) < 0.001)
        assertEquals(expected = 100L, actual = delay.delayMs)
        assertEquals(
            expected = TimelineDuration.Finite(milliseconds = 100),
            actual = DelayChainDevice().apply { state.value = delay }.timelineDuration(
                context = TimelineDurationContext(bpm = 120.0)
            ),
        )
        assertIs<KeyframesChainDeviceState>(value = devices.last())
    }

    @Test
    fun everySyncedDelayIncludingTripletsUsesItsMusicalDuration() {
        val expected = listOf(
            Timing.Rythm.RythmTiming._1_128 to 16L,
            Timing.Rythm.RythmTiming._1_64 to 31L,
            Timing.Rythm.RythmTiming._1_48 to 42L,
            Timing.Rythm.RythmTiming._1_32 to 63L,
            Timing.Rythm.RythmTiming._1_24 to 83L,
            Timing.Rythm.RythmTiming._1_16 to 125L,
            Timing.Rythm.RythmTiming._1_12 to 167L,
            Timing.Rythm.RythmTiming._1_8 to 250L,
            Timing.Rythm.RythmTiming._1_4 to 500L,
            Timing.Rythm.RythmTiming._1_2 to 1000L,
            Timing.Rythm.RythmTiming._1_1 to 2000L,
        )
        val previousBpm = AbletonConverter.bpm

        try {
            AbletonConverter.bpm = 120.0
            expected.forEachIndexed { index, (timing, milliseconds) ->
                val delay = assertIs<DelayChainDeviceState>(
                    value = convert(
                        parameters = listOf(
                            enumParameter(index = 18, value = index),
                            intParameter(index = 4, value = 1),
                            intParameter(index = 5, value = 1),
                        )
                    ).first()
                )

                assertEquals(expected = Timing.Rythm(timing = timing), actual = delay.timing)
                assertEquals(expected = milliseconds, actual = delay.delayMs)
                val duration = assertIs<TimelineDuration.Finite>(
                    value = DelayChainDevice().apply { state.value = delay }.timelineDuration(
                        context = TimelineDurationContext(bpm = 120.0)
                    )
                )
                assertTrue(actual = kotlin.math.abs(duration.milliseconds - milliseconds) <= 1L)
            }
        } finally {
            AbletonConverter.bpm = previousBpm
        }
    }

    @Test
    fun disabledDelayAddsNoDeviceEvenWithNamedTimingParameters() {
        val devices = convert(
            parameters = listOf(
                intParameter(index = 4, value = 1),
                floatParameter(index = 98, value = 60000f, name = "msDelay"),
                intParameter(index = 96, value = 0, name = "Delay"),
                intParameter(index = 97, value = 1, name = "DelaySyncMS"),
            )
        )

        assertIs<KeyframesChainDeviceState>(value = devices.single())
    }

    @Test
    fun loopUsesContinuousPlaybackAndAnUnboundedTimeline() {
        val state = assertIs<KeyframesChainDeviceState>(
            value = convert(parameters = listOf(enumParameter(index = 8, value = 1))).single()
        )

        assertEquals(expected = PlaybackMode.Continuous, actual = state.playbackMode)
        assertEquals(
            expected = TimelineDuration.Unbounded,
            actual = KeyframesChainDevice().apply { this.state.value = state }.timelineDuration(
                context = TimelineDurationContext(bpm = 120.0)
            ),
        )
    }

    @Test
    fun loopingMidiHeldAcrossEndOfTrackKeepsItsPeriodWithoutAFinalBlackFlush() {
        withMidiFile(track = listOf(0, 0x90, 36, 5, 96, 0xFF, 0x2F, 0)) { path ->
            val state = assertIs<KeyframesChainDeviceState>(
                value = adapter(
                    path = path,
                    parameters = listOf(enumParameter(index = 8, value = 1)),
                ).toDeviceStates().single()
            )
            val rendered = render(state = state)
            val signals = rendered.flatMap { it.second }.filterIsInstance<Signal.LED>()

            assertEquals(expected = PlaybackMode.Continuous, actual = state.playbackMode)
            assertTrue(actual = state.infinity)
            assertEquals(expected = 500, actual = rendered.last().first)
            assertTrue(actual = signals.isNotEmpty())
            assertTrue(actual = signals.none { it.color == Color.Black })
        }
    }

    @Test
    fun mirroringTransformsRenderedNotesAndTrackedRootButRetainsRawMidiNotes() {
        for ((mode, pitch, x, y) in listOf(
            listOf(0, 36, 1, 8),
            listOf(1, 71, 8, 8),
            listOf(2, 64, 1, 1),
            listOf(3, 99, 8, 1),
        )) {
            val state = assertIs<KeyframesChainDeviceState>(
                value = convert(
                    parameters = listOf(
                        enumParameter(index = 10, value = mode),
                        intParameter(index = 7, value = 1),
                    ),
                    offset = IntOffset(x = 20, y = 30),
                ).single()
            )
            val mirrored = state.frames[1].entries.first()
            val raw = state.frames[1].entries.last()

            assertEquals(expected = pitch, actual = mirrored.abletonPitch)
            assertEquals(expected = x, actual = mirrored.localX)
            assertEquals(expected = y, actual = mirrored.localY)
            assertEquals(expected = x + 20, actual = mirrored.x)
            assertEquals(expected = y + 30, actual = mirrored.y)
            assertEquals(expected = x + y * 10, actual = state.rootKey)
            assertEquals(expected = "pro-test-launchpad", actual = state.rootKeyLaunchpadId)
            assertTrue(actual = state.isolate)
            assertEquals(expected = 127, actual = raw.abletonPitch)
            assertEquals(expected = 3, actual = raw.x)
            assertEquals(expected = 4, actual = raw.y)
            val rendered = render(state = state).flatMap { it.second }.filterIsInstance<Signal.LED>()
            val renderedNote = rendered.first { it.extras[AbletonNoteSpace.PITCH] == pitch }
            assertEquals(expected = x + 20, actual = renderedNote.x)
            assertEquals(expected = y + 30, actual = renderedNote.y)
            assertTrue(actual = rendered.any { it.extras[AbletonNoteSpace.PITCH] == 127 })
        }
    }

    @Test
    fun disabledTrackingLeavesRootUnsetAndDoesNotIsolate() {
        val state = assertIs<KeyframesChainDeviceState>(
            value = convert(parameters = listOf(intParameter(index = 7, value = 0))).single()
        )

        assertNull(actual = state.rootKey)
        assertNull(actual = state.rootKeyLaunchpadId)
        assertEquals(expected = false, actual = state.isolate)
    }

    @Test
    fun chokeChannelsAndModesWrapTheWholeDelayAndKeyframesChain() {
        for (channel in 1..32) {
            listOf(ChokeMode.Start, ChokeMode.NoteZero, ChokeMode.End, ChokeMode.Receive)
                .forEachIndexed { index, mode ->
                    val choke = assertIs<ChokeChainDeviceState>(
                        value = convert(
                            parameters = listOf(
                                enumParameter(index = 1, value = index),
                                intParameter(index = 4, value = 1),
                            ),
                            blob = """{"Choke Channel":[$channel]}""",
                        ).single()
                    )

                    assertEquals(expected = channel, actual = choke.target)
                    assertEquals(expected = mode, actual = choke.mode)
                    assertEquals(expected = 2, actual = choke.stateChain.devices.size)
                    assertIs<DelayChainDeviceState>(value = choke.stateChain.devices.first())
                    assertIs<KeyframesChainDeviceState>(value = choke.stateChain.devices.last())
                }
        }

        val unwrapped = convert(
            parameters = listOf(intParameter(index = 4, value = 1)),
            blob = """{"Choke Channel":[0]}""",
        )
        assertEquals(expected = 2, actual = unwrapped.size)
        assertIs<DelayChainDeviceState>(value = unwrapped.first())
        assertIs<KeyframesChainDeviceState>(value = unwrapped.last())
    }

    @Test
    fun excludedSettingsDoNotAlterConvertedOutput() {
        val baseline = assertIs<KeyframesChainDeviceState>(value = convert(parameters = emptyList()).single())
        val state = assertIs<KeyframesChainDeviceState>(
            value = convert(
                parameters = listOf(
                    enumParameter(index = 3, value = 2, name = "Input Response"),
                    enumParameter(index = 9, value = 2, name = "Shift Mode"),
                    intParameter(index = 14, value = 1, name = "Side Lights"),
                ),
                blob = """{"Input Response":[2],"Shift Mode":[2],"Side Lights":[1]}""",
            ).single()
        )

        assertEquals(expected = baseline, actual = state)
        assertEquals(
            expected = render(state = baseline).map { it.first },
            actual = render(state = state).map { it.first },
        )
    }

    @Test
    fun nestedConversionRetainsChokeMirroringTrackingAndLoopAfterJsonRoundtrip() {
        val devices = convert(
            parameters = listOf(
                enumParameter(index = 1, value = 2),
                intParameter(index = 4, value = 1),
                intParameter(index = 7, value = 1),
                enumParameter(index = 8, value = 1),
                enumParameter(index = 10, value = 3),
            ),
            blob = """{"Choke Channel":[32],"Root Note":[36]}""",
        )
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(baseClass = DeviceState::class) {
                    subclass(subclass = ChokeChainDeviceState::class, serializer = ChokeChainDeviceState.serializer())
                    subclass(subclass = DelayChainDeviceState::class, serializer = DelayChainDeviceState.serializer())
                    subclass(subclass = KeyframesChainDeviceState::class, serializer = KeyframesChainDeviceState.serializer())
                }
            }
        }
        val restored = json.decodeFromString<StateChain>(
            string = json.encodeToString(value = StateChain(devices = devices))
        )
        val choke = assertIs<ChokeChainDeviceState>(value = restored.devices.single())
        val keyframes = assertIs<KeyframesChainDeviceState>(value = choke.stateChain.devices.last())

        assertEquals(expected = 32, actual = choke.target)
        assertEquals(expected = ChokeMode.End, actual = choke.mode)
        assertIs<DelayChainDeviceState>(value = choke.stateChain.devices.first())
        assertEquals(expected = PlaybackMode.Continuous, actual = keyframes.playbackMode)
        assertEquals(expected = 18, actual = keyframes.rootKey)
        assertEquals(expected = "pro-test-launchpad", actual = keyframes.rootKeyLaunchpadId)
        assertTrue(actual = keyframes.isolate)
        assertEquals(expected = 99, actual = keyframes.frames[1].entries.first().abletonPitch)
        assertEquals(expected = 127, actual = keyframes.frames[1].entries.last().abletonPitch)
    }

    private fun convert(
        parameters: List<MxParameter>,
        blob: String = "{}",
        offset: IntOffset = IntOffset.Zero,
    ): List<DeviceState> = adapter(parameters = parameters, blob = blob, offset = offset).convertKeyframes(
        keyframes = fixture,
        launchpadId = "pro-test-launchpad",
    )

    private fun adapter(
        parameters: List<MxParameter>,
        hash: String = latestHash,
        path: String? = null,
        blob: String = "{}",
        offset: IntOffset = IntOffset.Zero,
    ): MidiLauncherProAdapter {
        val fileDrops = path?.let {
            listOf(
                MxDeviceFileDropList.FileDropList.MxDFullFileDrop(
                    ref = MxDeviceFileDropList.FileDropList.MxDFullFileDrop.FileRefRef(
                        fileRef = FileRef(
                            relativePath = FileRef.RelativePath(),
                            path = FileRef.Path(value = it),
                            type = FileRef.Type(value = 1),
                        )
                    )
                )
            )
        } ?: emptyList()

        return MidiLauncherProAdapter(
            device = MxDeviceMidiEffect(
                patchSlot = MxDevicePatchSlot(value = MxDevicePatchSlot.Value()),
                blobSlot = MxDeviceBlobSlot(
                    value = MxDeviceBlobSlot.Value(
                        mxdBlob = MxDeviceBlobSlot.Value.MxDBlob(
                            blob = MxDeviceBlobSlot.Value.MxDBlob.Blob(
                                value = blob.encodeToByteArray().joinToString(separator = "") {
                                    (it.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
                                }
                            )
                        )
                    )
                ),
                parameterList = MxDeviceParameterList(
                    parameterList = MxDeviceParameterList.ParameterList(parameters = parameters)
                ),
                fileDropList = MxDeviceFileDropList(
                    fileDropList = MxDeviceFileDropList.FileDropList(items = fileDrops)
                ),
            ),
            hash = hash,
            offset = offset,
        )
    }

    private fun intParameter(index: Int, value: Int, name: String? = null): MxParameter.MxDIntParameter =
        MxParameter.MxDIntParameter(
            indexObj = AbletonIndex(value = index),
            timeable = MxParameter.MxParameterValue(manual = AbletonManual(value = value)),
            name = name?.let { MxParameterName(value = it) },
        )

    private fun enumParameter(index: Int, value: Int, name: String? = null): MxParameter.MxDEnumParameter =
        MxParameter.MxDEnumParameter(
            indexObj = AbletonIndex(value = index),
            timeable = MxParameter.MxParameterValue(manual = AbletonManual(value = value)),
            name = name?.let { MxParameterName(value = it) },
        )

    private fun floatParameter(index: Int, value: Float, name: String? = null): MxParameter.MxDFloatParameter =
        MxParameter.MxDFloatParameter(
            indexObj = AbletonIndex(value = index),
            timeable = MxParameter.MxParameterValue(manual = AbletonManual(value = value)),
            name = name?.let { MxParameterName(value = it) },
        )

    private fun render(state: KeyframesChainDeviceState): List<Pair<Int, List<Signal>>> =
        KeyframesChainDevice().apply {
            this.state.value = state
            renderAnimation()
        }.state.value.renderedAnimation

    private fun withMidiFile(
        track: List<Int> = listOf(96, 0x90, 36, 5, 96, 0x80, 36, 0, 0, 0xFF, 0x2F, 0),
        block: (String) -> Unit,
    ) {
        val file = Files.createTempFile("amethyst-midi-launcher-pro-", ".mid").toFile()
        val previousFile = AbletonConverter.file
        val previousLayout = AbletonConverter.launchpadLayout
        val previousBpm = AbletonConverter.bpm

        try {
            val header = listOf(
                0x4D, 0x54, 0x68, 0x64, 0, 0, 0, 6, 0, 0, 0, 1, 0, 96,
                0x4D, 0x54, 0x72, 0x6B, 0, 0, 0, track.size,
            )
            file.writeBytes(array = (header + track).map(Int::toByte).toByteArray())
            AbletonConverter.file = PlatformFile(path = file.absolutePath)
            AbletonConverter.launchpadLayout = AbletonLaunchpadLayout.create(count = 1)
            AbletonConverter.bpm = 120.0
            block(file.absolutePath)
        } finally {
            AbletonConverter.file = previousFile
            AbletonConverter.launchpadLayout = previousLayout
            AbletonConverter.bpm = previousBpm
            file.delete()
        }
    }

    private companion object {
        const val latestHash = "a114e5d1a7710271501649668c14f1ab"
        val fixture = KeyframesChainDeviceState(
            frames = listOf(
                Frame(timing = Timing.Duration(duration = 100.milliseconds)),
                Frame(
                    timing = Timing.Duration(duration = 200.milliseconds),
                    entries = listOf(
                        KeyframesEntry(
                            x = 1,
                            y = 8,
                            r = 1f,
                            g = 0f,
                            b = 0f,
                            launchpadId = "pro-test-launchpad",
                            localX = 1,
                            localY = 8,
                            abletonPitch = 36,
                        ),
                        KeyframesEntry(x = 3, y = 4, r = 0f, g = 1f, b = 0f, abletonPitch = 127),
                    ),
                ),
                Frame(timing = Timing.Duration(duration = 50.milliseconds)),
            )
        )
    }
}

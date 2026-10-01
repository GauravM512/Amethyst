package dev.anthonyhfm.amethyst.devices.effects.keyframes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.heaven.Heaven
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiFileImporter
import dev.anthonyhfm.amethyst.devices.Chokeable
import dev.anthonyhfm.amethyst.devices.GenericChainDevice
import dev.anthonyhfm.amethyst.devices.TimelineDuration
import dev.anthonyhfm.amethyst.devices.TimelineDurationContext
import dev.anthonyhfm.amethyst.devices.ableton.AbletonNoteSpace
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDevice
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDevice
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.Frame
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesEntry
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.PlaybackMode
import dev.anthonyhfm.amethyst.ui.launchpad.viewport.ViewportLaunchpadPro
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class KeyframesPlaybackEndTest {
    @Test
    fun repeatedUnchangedNoteZeroMidiEventsEachChokeBeforeTrackingClip() {
        val previousDevices = Heaven.devices
        val launchpad = ViewportLaunchpadPro().apply {
            launchpadId = "tracked"
        }
        val track = listOf(
            0, 0x90, 0, 5,
            24, 0x90, 0, 5,
            24, 0x80, 0, 0,
            0, 0xFF, 0x2F, 0,
        )
        val header = listOf(
            0x4D, 0x54, 0x68, 0x64,
            0, 0, 0, 6,
            0, 0,
            0, 1,
            0, 96,
            0x4D, 0x54, 0x72, 0x6B,
            0, 0, 0, track.size,
        )
        val completion = CountDownLatch(2)
        val probe = PlaybackChokeProbe(completion = completion)
        val receiver = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 27, mode = ChokeChainDeviceState.ChokeMode.Receive)
            state.value.chain.add(device = probe, fromUser = false)
        }
        val imported = MidiFileImporter.loadData(
            data = (header + track).map(Int::toByte).toByteArray(),
            launchpad = MidiFileImporter.DeviceTarget(launchpadId = "tracked", offset = IntOffset.Zero),
            preserveEndOfTrackTiming = true,
        )
        val keyframes = KeyframesChainDevice().apply {
            state.value = imported.copy(
                rootKey = 81,
                rootKeyLaunchpadId = "tracked",
                isolate = true,
            )
        }
        val output = CopyOnWriteArrayList<Signal.LED>()
        val sender = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 27, mode = ChokeChainDeviceState.ChokeMode.NoteZero)
            state.value.chain.add(device = keyframes, fromUser = false)
            signalExit = { signals -> output.addAll(signals.filterIsInstance<Signal.LED>()) }
        }

        try {
            Heaven.devices = listOf(launchpad)
            assertEquals(2, imported.frames.count { it.triggersNoteZero })
            sender.signalEnter(n = listOf(Signal.LED(origin = launchpad, x = 2, y = 8, color = Color.Red)))
            assertTrue(completion.await(2, TimeUnit.SECONDS))
            assertEquals(2, probe.chokeCount.get())
            assertTrue(output.all { it.extras[KEYFRAMES_NOTE_ZERO_TRIGGER] != 1 })
        } finally {
            sender.onRemovedFromChain()
            receiver.onRemovedFromChain()
            Heaven.devices = previousDevices
            launchpad.close()
        }
    }

    @Test
    fun anchoredRootTrackingUpdatesPitchBeforeDownstreamPitcher() {
        val previousDevices = Heaven.devices
        val launchpad = ViewportLaunchpadPro().apply {
            launchpadId = "tracked"
        }
        val completion = CountDownLatch(1)
        val tracked = CopyOnWriteArrayList<Signal.LED>()
        val shifted = CopyOnWriteArrayList<Signal.LED>()
        val pitcher = AbletonPitcherChainDevice().apply {
            state.value = AbletonPitcherChainDeviceState(pitch = 1)
            signalExit = { signals ->
                shifted.addAll(signals.filterIsInstance<Signal.LED>())
                completion.countDown()
            }
        }
        val keyframes = KeyframesChainDevice().apply {
            state.value = KeyframesChainDeviceState(
                rootKey = 81,
                rootKeyLaunchpadId = "tracked",
                isolate = true,
                frames = listOf(
                    Frame(
                        timing = Timing.Duration(duration = 100.milliseconds),
                        entries = listOf(
                            KeyframesEntry(
                                x = 1,
                                y = 8,
                                r = 1f,
                                g = 0f,
                                b = 0f,
                                launchpadId = "tracked",
                                localX = 1,
                                localY = 8,
                                abletonPitch = 36,
                            ),
                        ),
                    ),
                ),
            )
            renderAnimation()
            signalExit = { signals ->
                tracked.addAll(signals.filterIsInstance<Signal.LED>())
                pitcher.signalEnter(n = signals)
            }
        }

        try {
            Heaven.devices = listOf(launchpad)
            keyframes.signalEnter(n = listOf(Signal.LED(origin = launchpad, x = 2, y = 8, color = Color.Red)))
            assertTrue(completion.await(2, TimeUnit.SECONDS))
            assertEquals(2, tracked.single().x)
            assertEquals(8, tracked.single().y)
            assertEquals(37, tracked.single().extras[AbletonNoteSpace.PITCH])
            assertEquals(3, shifted.single().x)
            assertEquals(8, shifted.single().y)
            assertEquals(38, shifted.single().extras[AbletonNoteSpace.PITCH])
        } finally {
            keyframes.onChoke()
            Heaven.devices = previousDevices
            launchpad.close()
        }
    }

    @Test
    fun noteZeroChokesBeforeDeviceIsolatedKeyTrackingFiltersHiddenPitch() {
        val previousDevices = Heaven.devices
        val launchpad = ViewportLaunchpadPro().apply {
            launchpadId = "tracked"
        }
        val completion = CountDownLatch(1)
        val clippedOutput = CountDownLatch(1)
        val probe = PlaybackChokeProbe(completion = completion)
        val receiver = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 28, mode = ChokeChainDeviceState.ChokeMode.Receive)
            state.value.chain.add(device = probe, fromUser = false)
        }
        val keyframes = KeyframesChainDevice().apply {
            state.value = KeyframesChainDeviceState(
                rootKey = 81,
                rootKeyLaunchpadId = "tracked",
                isolate = true,
                frames = listOf(
                    Frame(
                        timing = Timing.Duration(duration = 100.milliseconds),
                        triggersNoteZero = true,
                        entries = listOf(
                            KeyframesEntry(
                                x = -1,
                                y = -1,
                                r = 1f,
                                g = 0f,
                                b = 0f,
                                launchpadId = "tracked",
                                localX = -1,
                                localY = -1,
                                abletonPitch = 0,
                            ),
                        ),
                    ),
                ),
            )
            renderAnimation()
        }
        val sender = ChokeChainDevice().apply {
            state.value = state.value.copy(target = 28, mode = ChokeChainDeviceState.ChokeMode.NoteZero)
            state.value.chain.add(device = keyframes, fromUser = false)
            signalExit = { signals ->
                if (signals.filterIsInstance<Signal.LED>().any { it.color == Color.Black }) {
                    clippedOutput.countDown()
                }
            }
        }

        try {
            Heaven.devices = listOf(launchpad)
            sender.signalEnter(n = listOf(Signal.LED(origin = launchpad, x = 2, y = 8, color = Color.Red)))
            assertTrue(completion.await(2, TimeUnit.SECONDS))
            assertTrue(clippedOutput.await(2, TimeUnit.SECONDS))
            assertEquals(1, probe.chokeCount.get())
        } finally {
            sender.onRemovedFromChain()
            receiver.onRemovedFromChain()
            Heaven.devices = previousDevices
            launchpad.close()
        }
    }

    @Test
    fun finitePlaybackReportsCompletionAfterFinalRepeat() {
        val completion = CountDownLatch(1)
        val device = KeyframesChainDevice().apply {
            state.value = KeyframesChainDeviceState(
                repeats = 2,
                frames = listOf(Frame(timing = Timing.Duration(duration = 60.milliseconds))),
            )
            renderAnimation()
            onPlaybackEnd = { completion.countDown() }
        }

        try {
            device.signalEnter(n = listOf(Signal.LED(origin = null, x = 1, y = 1, color = Color.Red)))
            assertFalse(completion.await(80, TimeUnit.MILLISECONDS))
            assertTrue(completion.await(2, TimeUnit.SECONDS))
        } finally {
            device.onChoke()
        }
    }

    @Test
    fun continuousIterationsAndCancelledPlaybackDoNotReportCompletion() {
        val completion = CountDownLatch(1)
        val device = KeyframesChainDevice().apply {
            state.value = KeyframesChainDeviceState(
                playbackMode = PlaybackMode.Continuous,
                frames = listOf(Frame(timing = Timing.Duration(duration = 40.milliseconds))),
            )
            renderAnimation()
            onPlaybackEnd = { completion.countDown() }
        }

        try {
            device.signalEnter(n = listOf(Signal.LED(origin = null, x = 1, y = 1, color = Color.Red)))
            assertFalse(completion.await(180, TimeUnit.MILLISECONDS))
            device.onChoke()
            device.state.value = device.state.value.copy(playbackMode = PlaybackMode.Mono)
            device.signalEnter(n = listOf(Signal.LED(origin = null, x = 1, y = 1, color = Color.Red)))
            device.onChoke()
            assertFalse(completion.await(120, TimeUnit.MILLISECONDS))
        } finally {
            device.onChoke()
        }
    }

    @Test
    fun deviceAnchoredRootTracksSecondLaunchpadPosition() {
        val previousDevices = Heaven.devices
        val left = ViewportLaunchpadPro().apply {
            launchpadId = "left"
        }
        val right = ViewportLaunchpadPro().apply {
            launchpadId = "right"
            position.value = Offset(x = 10f, y = 0f)
        }
        val device = KeyframesChainDevice().apply {
            state.value = KeyframesChainDeviceState(rootKey = 81, rootKeyLaunchpadId = "right")
        }

        try {
            Heaven.devices = listOf(left, right)
            assertEquals(Pair(first = 11, second = 8), device.rootPosition())
            right.position.value = Offset(x = 20f, y = 5f)
            assertEquals(Pair(first = 21, second = 13), device.rootPosition())
            device.state.value = device.state.value.copy(rootKeyLaunchpadId = null)
            assertEquals(Pair(first = 1, second = 8), device.rootPosition())
        } finally {
            Heaven.devices = previousDevices
            left.close()
            right.close()
        }
    }
}

private class PlaybackChokeProbe(
    private val completion: CountDownLatch,
) : GenericChainDevice<DelayChainDeviceState>(), Chokeable {
    override val state = MutableStateFlow(DelayChainDeviceState())
    val chokeCount = AtomicInteger()

    @Composable
    override fun Content() = Unit

    override fun signalEnter(n: List<Signal>) = Unit

    override fun timelineDuration(context: TimelineDurationContext): TimelineDuration = TimelineDuration.None

    override fun onChoke() {
        chokeCount.incrementAndGet()
        completion.countDown()
    }
}

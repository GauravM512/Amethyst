package dev.anthonyhfm.amethyst.conversion.ableton.adapters.kaskobi

import androidx.compose.ui.unit.IntOffset
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MxParameter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.MidiFileImporter
import dev.anthonyhfm.amethyst.core.midi.data.XY_TO_DRUM_RACK
import dev.anthonyhfm.amethyst.core.util.Timing
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonNoteSpace
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.choke.ChokeChainDeviceState.ChokeMode
import dev.anthonyhfm.amethyst.devices.effects.delay.DelayChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.PlaybackMode
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

class MidiLauncherProAdapter(
    private val device: MxDevice,
    private val hash: String,
    private val offset: IntOffset
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val fileRef = device.fileDropList.fileDropList.items.firstOrNull()?.ref?.fileRef ?: return emptyList()
        val filePath = fileRef.resolvePath()
        val data = if (AbletonConverter.isZip) {
            AbletonConverter.readZipEntry(path = filePath) ?: return emptyList()
        } else {
            try {
                runBlocking { PlatformFile(filePath).readBytes() }
            } catch (e: Exception) {
                return emptyList()
            }
        }

        val launchpad = AbletonConverter.launchpadTarget(offset = offset).midiImportTarget()
        val keyframes = MidiFileImporter.loadData(
            data = data,
            palette = AbletonConverter.palette,
            bpm = AbletonConverter.bpm,
            launchpad = launchpad,
            preserveEndOfTrackTiming = true,
        )

        return convertKeyframes(keyframes = keyframes, launchpadId = launchpad.launchpadId)
    }

    internal fun convertKeyframes(
        keyframes: KeyframesChainDeviceState,
        launchpadId: String,
    ): List<DeviceState> {
        val parameters = MidiLauncherParameters(device = device, hash = hash)
        val skipSilenceIndex = when (hash) {
            LATEST_HASH -> 17
            else -> {
                if (device.parameterList.parameterList.parameters.any {
                    it.index == 16 && it is MxParameter.MxDIntParameter
                }) {
                    16
                } else {
                    15
                }
            }
        }
        val skipSilence = parameters.value(
            name = "Skip Silence",
            indicesByHash = mapOf(hash to skipSilenceIndex),
        ) == 1.0
        val trimmedFrames = if (skipSilence) {
            keyframes.frames.dropWhile { it.entries.isEmpty() }
        } else {
            keyframes.frames
        }
        var converted = keyframes.copy(
            frames = trimmedFrames,
            renderedAnimation = emptyList(),
        )

        if (hash != LATEST_HASH) {
            return listOf(converted)
        }

        fun parameter(name: String, index: Int): Double? = parameters.value(
            name = name,
            indicesByHash = mapOf(LATEST_HASH to index),
        )

        val settings = jsonDecoder.decodeFromString<MidiLauncherProSettings>(string = device.decodeBlob())
        val mirrorMode = parameter(name = "Mirror Mode", index = 10)?.toInt() ?: 0
        val keyTracking = parameter(name = "Key Tracking", index = 7) == 1.0
        val loop = parameter(name = "Loop", index = 8) == 1.0
        val rootPitch = mirroredPitch(
            pitch = settings.rootNote.firstOrNull() ?: 36,
            mode = mirrorMode,
        )
        val rootIndex = if (keyTracking) {
            AbletonNoteSpace.padIndex(pitch = rootPitch)
        } else {
            null
        }

        converted = converted.copy(
            frames = converted.frames.map { frame ->
                frame.copy(
                    entries = frame.entries.map entryMapping@{ entry ->
                        val pitch = entry.abletonPitch ?: return@entryMapping entry
                        val mirrored = mirroredPitch(pitch = pitch, mode = mirrorMode)
                        val index = AbletonNoteSpace.padIndex(pitch = mirrored) ?: return@entryMapping entry
                        val localX = index % 10
                        val localY = 9 - index / 10

                        entry.copy(
                            x = localX + offset.x,
                            y = localY + offset.y,
                            localX = localX,
                            localY = localY,
                            abletonPitch = mirrored,
                        )
                    }
                )
            },
            playbackMode = if (loop) {
                PlaybackMode.Continuous
            } else {
                PlaybackMode.Mono
            },
            rootKey = rootIndex?.let { it % 10 + (9 - it / 10) * 10 },
            rootKeyLaunchpadId = if (rootIndex != null) {
                launchpadId
            } else {
                null
            },
            isolate = keyTracking,
            infinity = loop,
        )

        val devices = buildList<DeviceState> {
            if (parameter(name = "Delay", index = 4) == 1.0) {
                val timing = if (parameter(name = "DelaySyncMS", index = 5) == 1.0) {
                    val index = parameter(name = "SyncDelay", index = 18)?.toInt() ?: 1
                    Timing.Rythm(
                        timing = SYNC_TIMINGS.getOrElse(index = index) {
                            Timing.Rythm.RythmTiming._1_64
                        }
                    )
                } else {
                    val stored = parameter(name = "msDelay", index = 30) ?: 31320.877
                    val normalized = (stored.coerceIn(minimumValue = 10.0, maximumValue = 60000.0) - 10.0) / 59990.0
                    val milliseconds = 10.0 + 59990.0 * normalized.pow(n = 10)
                    Timing.Duration(duration = milliseconds.milliseconds)
                }
                val delayMs = when (timing) {
                    is Timing.Duration -> timing.duration.inWholeMilliseconds
                    is Timing.Rythm -> (240000.0 / AbletonConverter.bpm * timing.timing.factor).roundToLong()
                }

                add(element = DelayChainDeviceState(timing = timing, delayMs = delayMs))
            }

            add(element = converted)
        }
        val channel = settings.chokeChannel.firstOrNull()?.coerceIn(0, 32) ?: 0
        if (channel == 0) {
            return devices
        }

        val chokeMode = when (parameter(name = "Choke Mode", index = 1)?.toInt()) {
            1 -> ChokeMode.NoteZero
            2 -> ChokeMode.End
            3 -> ChokeMode.Receive
            else -> ChokeMode.Start
        }

        return listOf(
            ChokeChainDeviceState(
                target = channel,
                mode = chokeMode,
                stateChain = StateChain(devices = devices),
            )
        )
    }

    private fun mirroredPitch(pitch: Int, mode: Int): Int {
        val index = AbletonNoteSpace.padIndex(pitch = pitch) ?: return pitch
        val x = index % 10
        val y = 9 - index / 10
        val mirroredX = if (mode == 1 || mode == 3) {
            9 - x
        } else {
            x
        }
        val mirroredY = if (mode == 2 || mode == 3) {
            9 - y
        } else {
            y
        }
        val mirroredIndex = mirroredX + (9 - mirroredY) * 10
        val mirroredPitch = XY_TO_DRUM_RACK.getOrNull(index = mirroredIndex) ?: return pitch

        return if (AbletonNoteSpace.padIndex(pitch = mirroredPitch) == mirroredIndex) {
            mirroredPitch
        } else {
            pitch
        }
    }

    private companion object {
        const val LATEST_HASH = "a114e5d1a7710271501649668c14f1ab"
        val SYNC_TIMINGS = listOf(
            Timing.Rythm.RythmTiming._1_128,
            Timing.Rythm.RythmTiming._1_64,
            Timing.Rythm.RythmTiming._1_48,
            Timing.Rythm.RythmTiming._1_32,
            Timing.Rythm.RythmTiming._1_24,
            Timing.Rythm.RythmTiming._1_16,
            Timing.Rythm.RythmTiming._1_12,
            Timing.Rythm.RythmTiming._1_8,
            Timing.Rythm.RythmTiming._1_4,
            Timing.Rythm.RythmTiming._1_2,
            Timing.Rythm.RythmTiming._1_1,
        )
    }
}

@Serializable
private data class MidiLauncherProSettings(
    @SerialName("Root Note")
    val rootNote: List<Int> = listOf(36),
    @SerialName("Choke Channel")
    val chokeChannel: List<Int> = listOf(0),
)

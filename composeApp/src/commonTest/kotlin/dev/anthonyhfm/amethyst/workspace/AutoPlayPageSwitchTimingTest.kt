package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.core.engine.elements.AudioChain
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.elements.SIGNAL_EXTRA_SILENT_REPLAY
import dev.anthonyhfm.amethyst.core.engine.elements.currentSignalMacroValues
import dev.anthonyhfm.amethyst.devices.AudioConfiguration
import dev.anthonyhfm.amethyst.devices.effects.coordinate_filter.CoordinateFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.macro_filter.MacroFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import dev.anthonyhfm.amethyst.workspace.data.AutoPlayData
import dev.anthonyhfm.amethyst.workspace.data.Macro
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoPlayPageSwitchTimingTest {
    @Test
    fun boundaryNotesUseNewPageAndPreviousNoteReleasesUseOldPage() {
        for (useAudioFrames in listOf(false, true)) {
            withPageRack { chain, received ->
                if (useAudioFrames) {
                    chain.prepareAudio(configuration = AudioConfiguration(sampleRate = 48_000, periodFrames = 64))
                }
                val release = AutoPlayData.Action(x = 1, y = 1, down = false)
                val press = AutoPlayData.Action(x = 1, y = 1, down = true)
                val pageSwitch = AutoPlayData.Action(x = 9, y = 2, down = true, beforeNotes = true)

                dispatch(chain = chain, actions = listOf(pageSwitch, release, press), useAudioFrames = useAudioFrames)

                assertEquals(expected = 2, actual = received.size)
                assertEquals(expected = listOf(0), actual = received[0].macroValues)
                assertEquals(expected = 0, actual = received[0].velocity)
                assertEquals(expected = listOf(1), actual = received[1].macroValues)
                assertEquals(expected = 127, actual = received[1].velocity)
                if (useAudioFrames) {
                    assertTrue(actual = received.all { it.audioTriggerBatch?.requestedTargetFrame == 1_000L })
                }
            }
        }
    }

    @Test
    fun recordedPageButtonsPreserveSimultaneousNoteSnapshot() = withPageRack { chain, received ->
        val actions = listOf(
            AutoPlayData.Action(x = 9, y = 2, down = true),
            AutoPlayData.Action(x = 1, y = 1, down = true),
        )

        dispatch(chain = chain, actions = actions)

        assertEquals(expected = 1, actual = autoPlayActionBatches(actions = actions).size)
        assertEquals(expected = listOf(0), actual = received.single().macroValues)
        assertEquals(expected = 1, actual = WorkspaceRepository.macros.value.single().value)
    }

    @Test
    fun silentReplayAppliesPageBeforeBoundaryNotes() = withPageRack { chain, received ->
        dispatch(
            chain = chain,
            actions = listOf(
                AutoPlayData.Action(x = 9, y = 2, down = true, beforeNotes = true),
                AutoPlayData.Action(x = 1, y = 1, down = true),
            ),
            silentReplay = true,
        )

        assertEquals(expected = listOf(1), actual = received.single().macroValues)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun savedPageSwitchesKeepPriorityAndLegacyActionsDefaultToNormal() {
        val action = AutoPlayData.Action(x = 9, y = 2, down = true, beforeNotes = true)
        val encoded = ProtoBuf.encodeToByteArray(serializer = AutoPlayData.Action.serializer(), value = action)
        assertEquals(
            expected = action,
            actual = ProtoBuf.decodeFromByteArray(deserializer = AutoPlayData.Action.serializer(), bytes = encoded),
        )
        val legacy = Json.decodeFromString(
            deserializer = AutoPlayData.Action.serializer(),
            string = """{"x":9,"y":2,"down":true}""",
        )
        assertFalse(actual = legacy.beforeNotes)
    }

    private fun dispatch(
        chain: AudioChain,
        actions: List<AutoPlayData.Action>,
        useAudioFrames: Boolean = false,
        silentReplay: Boolean = false,
    ) {
        for (batch in autoPlayActionBatches(actions = actions)) {
            val snapshot = currentSignalMacroValues()
            val signals = batch.map {
                Signal.Midi(
                    origin = null,
                    x = it.x,
                    y = it.y,
                    velocity = if (it.down) {
                        127
                    } else {
                        0
                    },
                    macroValues = snapshot,
                    extras = if (silentReplay) {
                        mapOf(SIGNAL_EXTRA_SILENT_REPLAY to 1)
                    } else {
                        emptyMap()
                    },
                )
            }
            if (useAudioFrames) {
                chain.signalEnterAtFrame(n = signals, targetFrame = 1_000L)
            } else {
                chain.signalEnter(n = signals)
            }
        }
    }

    private fun withPageRack(block: (AudioChain, MutableList<Signal.Midi>) -> Unit) {
        val previousMacros = WorkspaceRepository.macros.value
        val chain = AudioChain()
        try {
            WorkspaceRepository.setMacros(macros = listOf(Macro(id = "page", value = 0)), undoable = false)
            val rack = GroupChainDevice()
            rack.loadFromState(
                state = GroupChainDeviceState(
                    groups = listOf(0, 1).map { page ->
                        Group(
                            name = "Page $page",
                            stateChain = StateChain(
                                devices = listOf(
                                    MacroFilterChainDeviceState(macro = 0, allowedValues = setOf(page)),
                                    CoordinateFilterChainDeviceState(filters = listOf(1 to 1)),
                                ),
                            ),
                        )
                    } + Group(
                        name = "Page Switching",
                        stateChain = StateChain(
                            devices = listOf(
                                CoordinateFilterChainDeviceState(filters = listOf(9 to 2)),
                                MacroControlChainDeviceState(macro = 0, value = 1),
                            ),
                        ),
                    ),
                ),
            )
            val received = mutableListOf<Signal.Midi>()
            rack.state.value.groups.take(n = 2).forEach { group ->
                group.chain.signalExit = { received.addAll(elements = it.filterIsInstance<Signal.Midi>()) }
            }
            chain.devices.value = listOf(rack)
            chain.reroute()
            block(chain, received)
        } finally {
            chain.releaseAudio()
            WorkspaceRepository.setMacros(macros = previousMacros, undoable = false)
        }
    }
}

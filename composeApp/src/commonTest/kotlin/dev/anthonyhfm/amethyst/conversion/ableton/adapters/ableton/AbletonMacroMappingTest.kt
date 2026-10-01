package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiPitcher
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiVelocity
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonKeyMidi
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonMidiControllerRange
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState
import dev.anthonyhfm.amethyst.devices.ableton.AbletonVelocityChainDevice
import dev.anthonyhfm.amethyst.devices.ableton.AbletonVelocityChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals

class AbletonMacroMappingTest {
    @Test
    fun fractionalEndpointsRoundOnlyAfterMappingToIntegerSetting() {
        val controllerRange = AbletonConverter.xml.decodeFromString(
            deserializer = AbletonMidiControllerRange.serializer(),
            string = """
                <MidiControllerRange>
                    <Min Value="0"/>
                    <Max Value="57.109726"/>
                </MidiControllerRange>
            """.trimIndent(),
        )

        assertEquals(
            expected = 28.554863f,
            actual = AbletonMacroMapping.effectiveValue(
                manualValue = 0f,
                keyMidi = mappedTo(index = 0),
                controllerRange = controllerRange,
                parentMacroValues = listOf(63.5f),
            ),
        )
        assertEquals(
            expected = 29,
            actual = AbletonMacroMapping.effectiveInt(
                manualValue = 0,
                keyMidi = mappedTo(index = 0),
                controllerRange = controllerRange,
                parentMacroValues = listOf(63.5f),
            ),
        )
    }

    @Test
    fun reversedFractionalRangeAndOutOfRangeMacrosMapCorrectly() {
        val controllerRange = AbletonMidiControllerRange(
            min = AbletonMidiControllerRange.Endpoint(value = 57.109726f),
            max = AbletonMidiControllerRange.Endpoint(value = -10.5f),
        )

        for ((macroValue, expected) in listOf(-1f to 57.109726f, 128f to -10.5f)) {
            assertEquals(
                expected = expected,
                actual = AbletonMacroMapping.effectiveValue(
                    manualValue = 0f,
                    keyMidi = mappedTo(index = 0),
                    controllerRange = controllerRange,
                    parentMacroValues = listOf(macroValue),
                ),
            )
        }
    }

    @Test
    fun nonFiniteMacroValuesUseSavedValue() {
        for (macroValue in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(
                expected = 8,
                actual = AbletonMacroMapping.effectiveInt(
                    manualValue = 8,
                    keyMidi = mappedTo(index = 0),
                    controllerRange = range(minimum = 0, maximum = 127),
                    parentMacroValues = listOf(macroValue),
                ),
            )
        }
    }

    @Test
    fun finiteExtremeEndpointsDoNotOverflowDuringInterpolation() {
        assertEquals(
            expected = 0f,
            actual = AbletonMacroMapping.effectiveValue(
                manualValue = 1f,
                keyMidi = mappedTo(index = 0),
                controllerRange = AbletonMidiControllerRange(
                    min = AbletonMidiControllerRange.Endpoint(value = -Float.MAX_VALUE),
                    max = AbletonMidiControllerRange.Endpoint(value = Float.MAX_VALUE),
                ),
                parentMacroValues = listOf(63.5f),
            ),
        )
    }

    @Test
    fun mappedPitchUsesEnclosingRackMacro() {
        val source = AbletonConverter.xml.decodeFromString(
            deserializer = MidiPitcher.serializer(),
            string = """
                <MidiPitcher Id="1">
                    <Pitch>
                        <Manual Value="0"/>
                        <KeyMidi>
                            <Channel Value="16"/>
                            <NoteOrController Value="1"/>
                        </KeyMidi>
                        <MidiControllerRange>
                            <Min Value="-128"/>
                            <Max Value="128"/>
                        </MidiControllerRange>
                    </Pitch>
                </MidiPitcher>
            """.trimIndent(),
        )

        val mapped = AbletonAdapter.resolveAdapter(
            device = source,
            rackMacroValues = listOf(0f, 64.4921875f),
        )!!.toDeviceStates().single() as AbletonPitcherChainDeviceState
        val saved = AbletonAdapter.resolveAdapter(
            device = source,
        )!!.toDeviceStates().single() as AbletonPitcherChainDeviceState

        assertEquals(expected = 2, actual = mapped.pitch)
        assertEquals(expected = 0, actual = saved.pitch)
    }

    @Test
    fun nestedRackMacrosResolveMappedTimeAndColorIncludingZero() {
        val outerMacros = listOf(32f, 8f, 21f, 34f, 47f, 62f, 91f, 0f)
        val innerMacros = outerMacros.indices.map { index ->
            AbletonMacroMapping.effectiveValue(
                manualValue = 0f,
                keyMidi = mappedTo(index),
                controllerRange = range(0, 127),
                parentMacroValues = outerMacros,
            )
        }

        assertEquals(outerMacros, innerMacros)
        assertEquals(
            expected = 3,
            actual = AbletonMacroMapping.effectiveInt(
                manualValue = 4,
                keyMidi = mappedTo(0),
                controllerRange = range(1, 9),
                parentMacroValues = innerMacros,
            ),
        )

        assertEquals(
            expected = listOf(8, 21, 34, 47, 62, 91, 0),
            actual = (1..7).map { index ->
                AbletonMacroMapping.effectiveInt(
                    manualValue = 127,
                    keyMidi = mappedTo(index),
                    controllerRange = range(0, 127),
                    parentMacroValues = innerMacros,
                )
            },
        )
    }

    @Test
    fun unmappedOrMissingParentUsesSavedValue() {
        assertEquals(
            expected = 8,
            actual = AbletonMacroMapping.effectiveInt(
                manualValue = 8,
                keyMidi = mappedTo(0),
                controllerRange = range(0, 13),
                parentMacroValues = null,
            ),
        )
        assertEquals(
            expected = 8,
            actual = AbletonMacroMapping.effectiveInt(
                manualValue = 8,
                keyMidi = AbletonKeyMidi(
                    channel = AbletonManual(0),
                    noteOrController = AbletonManual(0),
                ),
                controllerRange = range(0, 13),
                parentMacroValues = listOf(0f),
            ),
        )
    }

    @Test
    fun pageMacroChangesDoNotOverrideIndependentRackVelocityMacro() {
        val source = MidiVelocity(
            id = 1,
            maxOut = MidiVelocity.MaxOut(
                manual = AbletonManual(127),
                keyMidi = mappedTo(1),
                midiControllerRange = range(0, 127),
            ),
            minOut = MidiVelocity.MinOut(
                manual = AbletonManual(127),
                keyMidi = mappedTo(1),
                midiControllerRange = range(0, 127),
            ),
        )
        val converted = MidiVelocityAdapter(
            device = source,
            rackMacroValues = listOf(42f, 73f),
        ).toDeviceStates().single() as AbletonVelocityChainDeviceState
        val runtime = AbletonVelocityChainDevice().apply {
            state.value = converted
        }
        val output = mutableListOf<Signal.Midi>()
        runtime.signalExit = { signals ->
            output.addAll(signals.filterIsInstance<Signal.Midi>())
        }

        runtime.signalEnter(
            n = listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 100, macroValues = listOf(0)))
        )
        runtime.signalEnter(
            n = listOf(Signal.Midi(origin = null, x = 1, y = 2, velocity = 100, macroValues = listOf(127)))
        )

        assertEquals(expected = 73, actual = converted.outLow)
        assertEquals(expected = 73, actual = converted.outHigh)
        assertEquals(expected = listOf(73, 73), actual = output.map(Signal.Midi::velocity))
    }

    private fun mappedTo(index: Int): AbletonKeyMidi = AbletonKeyMidi(
        channel = AbletonManual(16),
        noteOrController = AbletonManual(index),
    )

    private fun range(minimum: Int, maximum: Int): AbletonMidiControllerRange = AbletonMidiControllerRange(
        min = AbletonMidiControllerRange.Endpoint(value = minimum.toFloat()),
        max = AbletonMidiControllerRange.Endpoint(value = maximum.toFloat()),
    )
}

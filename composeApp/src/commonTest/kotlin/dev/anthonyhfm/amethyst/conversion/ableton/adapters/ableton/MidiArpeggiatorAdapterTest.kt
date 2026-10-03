package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiArpeggiator
import dev.anthonyhfm.amethyst.devices.ableton.AbletonArpeggiatorChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MidiArpeggiatorAdapterTest {
    @Test
    fun fractionalVelocityTargetDecodesAndRoundsForPalette() {
        val source = decode(velocity = "110.873016")

        assertEquals(expected = 110.873016f, actual = source.velocityTarget.manual.value)
        assertEquals(expected = color(velocity = 111), actual = convert(source = source).color)
    }

    @Test
    fun integerVelocityTargetsKeepTheirPaletteColors() {
        for (velocity in listOf(0, 1, 110, 127)) {
            assertEquals(
                expected = color(velocity = velocity),
                actual = convert(source = decode(velocity = velocity.toString())).color,
            )
        }
    }

    @Test
    fun disabledVelocityAcceptsFractionalTargetWithoutSettingColor() {
        assertNull(
            actual = convert(source = decode(velocity = "110.873016", enabled = false)).color,
        )
    }

    @Test
    fun velocityTargetsStayWithinPaletteBounds() {
        for ((velocity, index) in listOf("-0.75" to 0, "127.75" to 127)) {
            assertEquals(
                expected = color(velocity = index),
                actual = convert(source = decode(velocity = velocity)).color,
            )
        }
    }

    private fun convert(source: MidiArpeggiator): AbletonArpeggiatorChainDeviceState =
        assertIs<AbletonArpeggiatorChainDeviceState>(
            value = MidiArpeggiatorAdapter(device = source).toDeviceStates().single(),
        )

    private fun color(velocity: Int): Triple<Float, Float, Float> {
        val (red, green, blue) = AbletonConverter.palette[velocity]

        return Triple(first = red / 63f, second = green / 63f, third = blue / 63f)
    }

    private fun decode(velocity: String, enabled: Boolean = true): MidiArpeggiator =
        AbletonConverter.xml.decodeFromString(
            deserializer = MidiArpeggiator.serializer(),
            string = """
                <MidiArpeggiator Id="1">
                    <TransposeDistance><Manual Value="7"/></TransposeDistance>
                    <TransposeSteps><Manual Value="3"/></TransposeSteps>
                    <SyncState><Manual Value="true"/></SyncState>
                    <SyncedRate><Manual Value="4"/></SyncedRate>
                    <RepeatCount><Manual Value="2"/></RepeatCount>
                    <FreeRate><Manual Value="125"/></FreeRate>
                    <Gate><Manual Value="0.75"/></Gate>
                    <VelocitySwitch><Manual Value="$enabled"/></VelocitySwitch>
                    <VelocityTarget><Manual Value="$velocity"/></VelocityTarget>
                </MidiArpeggiator>
            """.trimIndent(),
        )
}

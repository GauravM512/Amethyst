package dev.anthonyhfm.amethyst.conversion.ableton.data.utils

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AbletonMidiControllerRangeTest {
    @Test
    fun integerAndFractionalEndpointsDecode() {
        val range = decode(minimum = "0", maximum = "57.109726")

        assertEquals(expected = 0f, actual = range.min.value)
        assertEquals(expected = 57.109726f, actual = range.max.value)
    }

    @Test
    fun negativeAndExponentEndpointsDecode() {
        val range = decode(minimum = "-12.5", maximum = "1.27e2")

        assertEquals(expected = -12.5f, actual = range.min.value)
        assertEquals(expected = 127f, actual = range.max.value)
    }

    @Test
    fun reversedEndpointsArePreserved() {
        val range = decode(minimum = "127.0", maximum = "0.5")

        assertEquals(expected = 127f, actual = range.min.value)
        assertEquals(expected = 0.5f, actual = range.max.value)
    }

    @Test
    fun nonFiniteAndMalformedEndpointsAreRejected() {
        for (invalid in listOf("NaN", "Infinity", "-Infinity", "1e100", "invalid")) {
            assertFails(message = "Minimum endpoint $invalid must be rejected") {
                decode(minimum = invalid, maximum = "127")
            }
            assertFails(message = "Maximum endpoint $invalid must be rejected") {
                decode(minimum = "0", maximum = invalid)
            }
        }
    }

    private fun decode(minimum: String, maximum: String): AbletonMidiControllerRange =
        AbletonConverter.xml.decodeFromString(
            deserializer = AbletonMidiControllerRange.serializer(),
            string = """
                <MidiControllerRange>
                    <Min Value="$minimum"/>
                    <Max Value="$maximum"/>
                </MidiControllerRange>
            """.trimIndent(),
        )
}

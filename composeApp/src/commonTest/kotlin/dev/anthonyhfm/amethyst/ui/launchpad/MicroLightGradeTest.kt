package dev.anthonyhfm.amethyst.ui.launchpad

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MicroLightGradeTest {
    @Test
    fun gradesDarkRgbValuesUsingMicroLightTable() {
        val source = Color(
            red = 1f / 63f,
            green = 2f / 63f,
            blue = 0f,
            alpha = 0.5f
        )
        val graded = source.applyMicroLightGrade()

        assertEquals(expected = 17f / 63f, actual = graded.red, absoluteTolerance = 0.001f)
        assertEquals(expected = 21f / 63f, actual = graded.green, absoluteTolerance = 0.001f)
        assertEquals(expected = 0f, actual = graded.blue, absoluteTolerance = 0.001f)
        assertEquals(expected = source.alpha, actual = graded.alpha)
    }

    @Test
    fun keepsBlackAndFullBrightness() {
        val black = Color.Black.applyMicroLightGrade()
        val white = Color.White.applyMicroLightGrade()

        assertEquals(expected = Color.Black, actual = black)
        assertEquals(expected = Color.White, actual = white)
    }
}

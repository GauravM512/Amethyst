package dev.anthonyhfm.amethyst.conversion.ableton.adapters.dovitate

import kotlin.test.Test
import kotlin.test.assertEquals

class CycleLightsAdapterTest {
    @Test
    fun stagesFollowPatchOrderInsteadOfSavedFileDropOrder() {
        val paths = CycleLightsAdapter.orderedPaths(
            length = 4,
            fileDrops = linkedMapOf(
                "live.drop[188]" to "Lights/four.mid",
                "live.drop[187]" to "Lights/three.mid",
                "live.drop[186]" to "Lights/two.mid",
                "live.drop[185]" to "Lights/one.mid",
            ),
        )

        assertEquals(
            expected = listOf("Lights/one.mid", "Lights/two.mid", "Lights/three.mid", "Lights/four.mid"),
            actual = paths,
        )
    }

    @Test
    fun emptyStagesRemainPartOfTheCycle() {
        val paths = CycleLightsAdapter.orderedPaths(
            length = 4,
            fileDrops = mapOf(
                "live.drop[185]" to "Lights/one.mid",
                "live.drop[188]" to "Lights/four.mid",
            ),
        )

        assertEquals(
            expected = listOf("Lights/one.mid", null, null, "Lights/four.mid"),
            actual = paths,
        )
    }

    @Test
    fun laterSlotsUseThePatchParameterMapping() {
        val paths = CycleLightsAdapter.orderedPaths(
            length = 16,
            fileDrops = mapOf(
                "live.drop[197]" to "Lights/nine.mid",
                "live.drop[198]" to "Lights/fourteen.mid",
                "live.drop[204]" to "Lights/sixteen.mid",
            ),
        )

        assertEquals(expected = "Lights/nine.mid", actual = paths[8])
        assertEquals(expected = "Lights/fourteen.mid", actual = paths[13])
        assertEquals(expected = "Lights/sixteen.mid", actual = paths[15])
    }
}

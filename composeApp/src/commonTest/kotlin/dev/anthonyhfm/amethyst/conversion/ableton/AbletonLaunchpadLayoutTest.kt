package dev.anthonyhfm.amethyst.conversion.ableton

import androidx.compose.ui.unit.IntOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbletonLaunchpadLayoutTest {
    @Test
    fun createsAtLeastOneLaunchpadAndPlacesAdditionalPadsTenColumnsApart() {
        assertEquals(1, AbletonLaunchpadLayout.create(0).launchpads.size)
        val layout = AbletonLaunchpadLayout.create(3)
        assertEquals(IntOffset.Zero, layout.target(0).offset)
        assertEquals(IntOffset(10, 0), layout.target(1).offset)
        assertEquals(IntOffset(20, 0), layout.target(2).offset)
        assertEquals(IntOffset(20, 0), layout.offsetBetween(0, 2))
        assertEquals(IntOffset(-10, 0), layout.offsetBetween(2, 1))
    }

    @Test
    fun resolvesTargetsByOffsetAndPreservesDeviceLocalCoordinates() {
        val layout = AbletonLaunchpadLayout.create(2)
        val target = layout.targetAt(IntOffset(10, 0))
        val entry = target.midiImportTarget().keyframesEntry(3, 4, 1f, 0.5f, 0f)
        assertEquals(target.launchpad.id, entry.launchpadId)
        assertEquals(13, entry.x)
        assertEquals(4, entry.y)
        assertEquals(3, entry.localX)
        assertEquals(4, entry.localY)
        assertEquals(target.launchpad.id, target.padFilter(3, 4).launchpadId)
        assertFailsWith<IllegalStateException> { layout.targetAt(IntOffset(5, 0)) }
    }
}

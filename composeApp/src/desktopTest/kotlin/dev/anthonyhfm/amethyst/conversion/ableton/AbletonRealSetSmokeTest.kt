package dev.anthonyhfm.amethyst.conversion.ableton

import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Set AMETHYST_ABLETON_FIXTURE to an existing Live set to check real-world import locally. */
class AbletonRealSetSmokeTest {
    @Test
    fun importsOptInRealLiveSet() {
        val fixture = System.getenv("AMETHYST_ABLETON_FIXTURE")?.takeIf(String::isNotBlank) ?: return
        val path = Path.of(fixture)
        require(Files.isRegularFile(path)) { "Missing Ableton fixture: $path" }

        val decoded = AbletonConverter.decodeAbletonAls(Files.readAllBytes(path))
        assertTrue(decoded.liveSet.tracks.midiTracks.isNotEmpty())

        val result = AbletonConverter.convertToWorkspace(PlatformFile(fixture), null, reporter = null)
        assertEquals(decoded.liveSet.masterTrack.deviceChain.mixer.tempo.manual.value, result.settings.bpm)
        assertTrue(result.lights.devices.isNotEmpty() || result.sampling.devices.isNotEmpty())
        assertTrue(result.launchpadDevices.isNotEmpty())
    }
}

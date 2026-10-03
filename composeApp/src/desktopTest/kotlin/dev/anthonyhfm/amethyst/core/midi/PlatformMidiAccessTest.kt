package dev.anthonyhfm.amethyst.core.midi

import dev.anthonyhfm.amethyst.nativeengine.MidiException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlatformMidiAccessTest {
    @Test
    fun unavailableMidiBackendDoesNotPreventWorkspaceInitialization() {
        val access = createDesktopMidiAccess(
            createAccess = {
                throw MidiException.BackendException(reason = "ALSA sequencer unavailable")
            },
        )

        assertNull(actual = access)

        val manager = AmethystMidiManager(
            midiAccess = access,
            elementsProvider = { emptyList() },
        )

        try {
            manager.startAutoDetectLoop()
            manager.refreshConnections()
        } finally {
            manager.close()
        }
    }

    @Test
    fun availableMidiBackendIsPreserved() {
        val access = FakeMidiAccess()

        assertSame(
            expected = access,
            actual = createDesktopMidiAccess(createAccess = { access }),
        )
    }

    @Test
    fun unexpectedInitializationFailuresArePropagated() {
        val failure = IllegalStateException("Unexpected initialization failure")

        val thrown = assertFailsWith<IllegalStateException> {
            createDesktopMidiAccess(createAccess = { throw failure })
        }

        assertSame(expected = failure, actual = thrown)
    }
}

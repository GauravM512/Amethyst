package dev.anthonyhfm.amethyst.core.midi

import dev.atsushieno.ktmidi.MidiAccess
import kotlinx.coroutines.CoroutineScope

expect var platformMidiAccess: MidiAccess?
package dev.anthonyhfm.amethyst.core.midi

import dev.atsushieno.ktmidi.MidiAccess
import dev.atsushieno.ktmidi.TraditionalCoreMidiAccess

actual var platformMidiAccess: MidiAccess? = TraditionalCoreMidiAccess()
package dev.anthonyhfm.amethyst.core.midi

import dev.atsushieno.ktmidi.MidiAccess
import dev.atsushieno.ktmidi.WebMidiAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.newCoroutineContext

actual val platformMidiAccess: MidiAccess
    get() = WebMidiAccess()

@OptIn(DelicateCoroutinesApi::class)
actual val IO_COROUTINE: CoroutineScope
    get() = GlobalScope
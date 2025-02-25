package dev.anthonyhfm.amethyst.core.heaven.elements

abstract class SignalReceiver {
    var midiExit: ((List<Signal>) -> Unit)? = null

    abstract fun midiEnter(n: List<Signal>)

    fun midiEnter(n: Signal) = midiEnter(listOf(n))
}
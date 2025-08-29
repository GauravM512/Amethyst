package dev.anthonyhfm.amethyst.core.engine.elements

abstract class SignalReceiver {
    var signalExit: ((List<Signal>) -> Unit)? = null

    abstract fun signalEnter(n: List<Signal>)

    fun signalEnter(n: Signal) = signalEnter(listOf(n))
}
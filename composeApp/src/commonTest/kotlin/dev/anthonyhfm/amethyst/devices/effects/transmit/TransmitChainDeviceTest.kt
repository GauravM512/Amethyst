package dev.anthonyhfm.amethyst.devices.effects.transmit

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TransmitChainDeviceTest {
    @AfterTest
    fun tearDown() {
        TransmitChainDevice.clearReceivers()
    }

    @Test
    fun receiverRoutesSignalsWithoutRenderingItsChain() {
        val received = mutableListOf<Signal>()
        val receiver = TransmitChainDevice().apply {
            state.value = TransmitChainDeviceState(
                mode = TransmitChainDeviceState.Mode.Receive,
                channel = 3,
            )
        }
        val receiverChain = Chain().apply {
            signalExit = { signals -> received.addAll(signals) }
            add(device = receiver, fromUser = false)
        }
        val sender = TransmitChainDevice().apply {
            state.value = TransmitChainDeviceState(
                mode = TransmitChainDeviceState.Mode.Send,
                channel = 3,
            )
        }
        val senderChain = Chain().apply {
            add(device = sender, fromUser = false)
        }
        val signal = Signal.LED(
            origin = null,
            x = 2,
            y = 4,
            color = Color.Red,
        )

        senderChain.signalEnter(listOf(signal))

        assertEquals<List<Signal>>(listOf(signal), received)

        receiver.state.value = receiver.state.value.copy(channel = 4)
        receiver.onStateRestored()
        senderChain.signalEnter(listOf(signal))

        assertEquals<List<Signal>>(listOf(signal), received)

        sender.state.value = sender.state.value.copy(channel = 4)
        senderChain.signalEnter(listOf(signal))

        assertEquals<List<Signal>>(listOf(signal, signal), received)

        receiverChain.remove(receiver.selectionUUID, fromUser = false)
        senderChain.signalEnter(listOf(signal))

        assertEquals<List<Signal>>(listOf(signal, signal), received)
    }
}

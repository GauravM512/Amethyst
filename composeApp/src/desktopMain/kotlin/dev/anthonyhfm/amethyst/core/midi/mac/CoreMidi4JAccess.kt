package dev.anthonyhfm.amethyst.core.midi.mac

import dev.atsushieno.ktmidi.MidiAccess
import dev.atsushieno.ktmidi.MidiInput
import dev.atsushieno.ktmidi.MidiOutput
import dev.atsushieno.ktmidi.MidiPortConnectionState
import dev.atsushieno.ktmidi.MidiPortDetails
import dev.atsushieno.ktmidi.OnMidiReceivedEventListener
import dev.atsushieno.ktmidi.PortCreatorContext
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException
import javax.sound.midi.MetaMessage
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.SysexMessage
import javax.sound.midi.Transmitter

class CoreMidi4JAccess : MidiAccess() {
    override val name: String = "CoreMidi4J"
    override val inputs: Iterable<MidiPortDetails>
        get() {
            val devices = CoreMidiDeviceProvider.getMidiDeviceInfo()
                .map { MidiSystem.getMidiDevice(it) }
                .filter { it.maxTransmitters == -1 || it.maxTransmitters > 0 }
            val ids = buildStableIds("in", devices.map { it.deviceInfo.name ?: "unknown" })
            return devices.zip(ids).map { (dev, id) -> CoreMidi4JMidiTransmitterPortDetails(dev, id) }
        }
    override val outputs: Iterable<MidiPortDetails>
        get() {
            val devices = CoreMidiDeviceProvider.getMidiDeviceInfo()
                .map { MidiSystem.getMidiDevice(it) }
                .filter { it.maxReceivers == -1 || it.maxReceivers > 0 }
            val ids = buildStableIds("out", devices.map { it.deviceInfo.name ?: "unknown" })
            return devices.zip(ids).map { (dev, id) -> CoreMidi4JMidiReceiverPortDetails(dev, id) }
        }

    init {
        if (!CoreMidiDeviceProvider.isLibraryLoaded()) {
            throw CoreMidiException("CoreMidi is not loaded correctly")
        }
    }

    override suspend fun openInput(portId: String): MidiInput {
        val port = inputs.firstOrNull { i -> i.id == portId }
        if (port == null || port !is CoreMidi4JMidiTransmitterPortDetails)
            throw IllegalArgumentException("Input port $portId was not found")
        if (!port.device.isOpen)
            port.device.open()
        val transmitter = port.device.transmitter
        return CoreMidi4JMidiInput(port, transmitter)
    }

    override suspend fun openOutput(portId: String): MidiOutput {
        val port = outputs.firstOrNull { i -> i.id == portId }
        if (port == null || port !is CoreMidi4JMidiReceiverPortDetails)
            throw IllegalArgumentException("Output port $portId was not found")
        if (!port.device.isOpen)
            port.device.open()
        val receiver = port.device.receiver
        return CoreMidi4JMidiOutput(port, receiver)
    }

    override suspend fun createVirtualInputSender(context: PortCreatorContext): MidiOutput {
        throw UnsupportedOperationException()
    }

    override suspend fun createVirtualOutputReceiver(context: PortCreatorContext): MidiInput {
        throw UnsupportedOperationException()
    }
}

internal typealias CoreMidi4JMidiMessage = javax.sound.midi.MidiMessage

private fun buildStableIds(prefix: String, names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { rawName ->
        val sanitized = rawName.replace("[^A-Za-z0-9._-]".toRegex(), "_").take(64)
        val count = seen.getOrDefault(sanitized, 0)
        seen[sanitized] = count + 1
        if (count == 0) "$prefix:$sanitized" else "$prefix:$sanitized:$count"
    }
}

internal abstract class CoreMidi4JMidiPortDetails(override val id: String, private val info: MidiDevice.Info) : MidiPortDetails {
    override val manufacturer: String? = info.vendor
    override val name: String?
        get() {
            return info.name
                .replace("CoreMIDI4J - ", "")
        }
    override val version: String? = info.version
    override val midiTransportProtocol = 1
}

private class CoreMidi4JMidiTransmitterPortDetails(val device: MidiDevice, id: String) :
    CoreMidi4JMidiPortDetails(id, device.deviceInfo)

private class CoreMidi4JMidiReceiverPortDetails(val device: MidiDevice, id: String) :
    CoreMidi4JMidiPortDetails(id, device.deviceInfo)

private fun toCoreMidi4JMidiMessage(data: ByteArray, start: Int, length: Int): CoreMidi4JMidiMessage {
    if (length <= 0) throw IllegalArgumentException("non-positive length")
    val arr = if (start == 0 && length == data.size) data else data.drop(start).take(length - start).toByteArray()
    return when (arr[0]) {
        0xF0.toByte(), 0xF7.toByte() -> SysexMessage(arr, length)
        0xFF.toByte() -> MetaMessage(arr[1].toInt(), arr.drop(2).toByteArray(), length - 2)
        else -> ShortMessage(
            arr[0].toUByte().toInt(),
            arr.getOrElse(1) { _ -> 0 }.toInt(),
            arr.getOrElse(2) { _ -> 0 }.toInt()
        )
    }
}

private class CoreMidi4JMidiInput(val port: CoreMidi4JMidiTransmitterPortDetails, private val transmitter: Transmitter) : MidiInput {

    override val details: MidiPortDetails = port

    private val state: MidiPortConnectionState = MidiPortConnectionState.OPEN

    override val connectionState: MidiPortConnectionState
        get() = state

    override fun close() {
        transmitter.close()
    }

    private var listener: OnMidiReceivedEventListener? = null

    override fun setMessageReceivedListener(listener: OnMidiReceivedEventListener) {
        this.listener = listener
    }

    init {
        transmitter.receiver = object : Receiver {
            override fun close() {}

            override fun send(msg: CoreMidi4JMidiMessage?, timestampInMicroseconds: Long) {
                if (msg == null)
                    return
                var start = 0
                var length = msg.length
                // Message begins with 0xF7 is an additional sysex message
                if (msg.message[0] == 0xF7.toByte()) {
                    start = 1
                    length--
                }
                listener?.onEventReceived(msg.message, start, length, timestampInMicroseconds * 1000)
            }
        }
    }
}

private class CoreMidi4JMidiOutput(val port: CoreMidi4JMidiReceiverPortDetails, private val receiver: Receiver) : MidiOutput {

    override val details: MidiPortDetails
        get() = port

    private val state: MidiPortConnectionState = MidiPortConnectionState.OPEN

    override val connectionState: MidiPortConnectionState
        get() = state

    override fun close() {
        receiver.close()
    }

    private var multiPacketSysex = false
    override fun send(mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long) {
        val msg: CoreMidi4JMidiMessage
        if (multiPacketSysex) {
            // If a multi-packet sysex message ends with 0xF7, it means that it is the last packet.
            if (mevent[offset + length - 1] == 0xF7.toByte())
                multiPacketSysex = false

            // CoreMidi4J requires that an additional sysex message must begin with 0xF7.
            val buffer = ByteArray(length + 1)
            buffer[0] = 0xF7.toByte()
            mevent.copyInto(buffer, 1, offset, length)
            msg = toCoreMidi4JMidiMessage(buffer, 0, length + 1)
        } else {
            // If a sysex doesn't end with 0xF7, it is a multi-packet sysex message.
            if (mevent[offset] == 0xF0.toByte() && mevent[offset + length - 1] != 0xF7.toByte())
                multiPacketSysex = true
            msg = toCoreMidi4JMidiMessage(mevent, offset, length)
        }
        receiver.send(msg, timestampInNanoseconds)
    }
}

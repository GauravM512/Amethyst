package dev.anthonyhfm.amethyst.core.midi.linux

import dev.atsushieno.ktmidi.*
import javax.sound.midi.*
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException

internal typealias JvmMidiMessage = javax.sound.midi.MidiMessage

private object AlsaNameResolver {
    private var initialized = false
    private val clientNameByCard = mutableMapOf<Int, String>()
    private val portNameByCardPort = mutableMapOf<Pair<Int, Int>, String>() // (card, port) -> name

    private fun parseAconnect(output: String) {
        clientNameByCard.clear()
        portNameByCardPort.clear()
        val clientRegex = Regex("""client\s+(\d+):\s+'([^']+)'\s+\[.*?card=(\d+)""")
        val portRegex = Regex("""\s+(\d+)\s+'([^']+)'""")
        var currentCard: Int? = null
        for (line in output.lineSequence()) {
            val c = clientRegex.find(line)
            if (c != null) {
                val clientName = c.groupValues[2]
                val card = c.groupValues[3].toInt()
                clientNameByCard[card] = clientName
                currentCard = card
                continue
            }
            val p = portRegex.find(line)
            if (p != null && currentCard != null) {
                val port = p.groupValues[1].toInt()
                val name = p.groupValues[2]
                portNameByCardPort[currentCard!! to port] = name
            }
        }
    }

    @Synchronized
    private fun ensure() {
        if (initialized) return
        initialized = true
        try {
            val proc = ProcessBuilder("aconnect", "-l")
                .redirectErrorStream(true)
                .start()
            val text = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            parseAconnect(text)
        } catch (_: Exception) {

        }
    }

    fun prettyNameFor(deviceInfoName: String?): String? {
        if (deviceInfoName == null) return null
        val m = Regex(""".*\[hw:(\d+),(\d+),(\d+)]""").find(deviceInfoName) ?: return null
        val card = m.groupValues[1].toInt()
        val sub = m.groupValues[3].toInt()
        ensure()
        return portNameByCardPort[card to sub]
            ?: clientNameByCard[card]
    }
}

private fun buildStableIds(prefix: String, names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { rawName ->
        val sanitized = rawName.replace("[^A-Za-z0-9._-]".toRegex(), "_").take(64)
        val count = seen.getOrDefault(sanitized, 0)
        seen[sanitized] = count + 1
        if (count == 0) "$prefix:$sanitized" else "$prefix:$sanitized:$count"
    }
}

class LinuxJVMAccess : MidiAccess() {
    override val name: String get() = "Linux JVM"

    override val inputs: Iterable<MidiPortDetails>
        get() {
            val devices = MidiSystem.getMidiDeviceInfo()
                .map { MidiSystem.getMidiDevice(it) }
                .filter { dev -> dev.maxTransmitters != 0 }
                .filter { dev ->
                    val n = dev.deviceInfo.name ?: ""
                    !n.contains("Real Time Sequencer", ignoreCase = true) &&
                            !n.contains("Gervill", ignoreCase = true)
                }
                .filter { dev -> dev.maxTransmitters == -1 || dev.transmitters.size < dev.maxTransmitters }
            val ids = buildStableIds("hw-in", devices.map { it.deviceInfo.name ?: "unknown" })
            return devices.zip(ids).map { (dev, id) ->
                val pretty = AlsaNameResolver.prettyNameFor(dev.deviceInfo.name)
                JvmMidiTransmitterPortDetails(dev, id, pretty)
            }
        }

    override val outputs: Iterable<MidiPortDetails>
        get() {
            val devices = MidiSystem.getMidiDeviceInfo()
                .map { MidiSystem.getMidiDevice(it) }
                .filter { dev -> dev.maxReceivers != 0 }
                .filter { dev ->
                    val n = dev.deviceInfo.name ?: ""
                    !n.contains("Real Time Sequencer", ignoreCase = true) &&
                            !n.contains("Gervill", ignoreCase = true)
                }
                .filter { dev -> dev.maxReceivers == -1 || dev.receivers.size < dev.maxReceivers }
            val ids = buildStableIds("hw-out", devices.map { it.deviceInfo.name ?: "unknown" })
            return devices.zip(ids).map { (dev, id) ->
                val pretty = AlsaNameResolver.prettyNameFor(dev.deviceInfo.name)
                JvmMidiReceiverPortDetails(dev, id, pretty)
            }
        }

    override suspend fun openInput(portId: String): MidiInput {
        val port = inputs.firstOrNull { it.id == portId }
        if (port !is JvmMidiTransmitterPortDetails)
            throw IllegalArgumentException("Input port $portId was not found")
        if (!port.device.isOpen) port.device.open()
        return JvmMidiInput(port)
    }

    override suspend fun openOutput(portId: String): MidiOutput {
        val port = outputs.firstOrNull { it.id == portId }
        if (port !is JvmMidiReceiverPortDetails)
            throw IllegalArgumentException("Output port $portId was not found")
        if (!port.device.isOpen) port.device.open()
        return JvmMidiOutput(port)
    }

    override suspend fun createVirtualInputSender(context: PortCreatorContext): MidiOutput {
        throw UnsupportedOperationException()
    }

    override suspend fun createVirtualOutputReceiver(context: PortCreatorContext): MidiInput {
        throw UnsupportedOperationException()
    }
}

internal abstract class JvmMidiPortDetails(
    override val id: String,
    private val info: MidiDevice.Info,
    private val prettyName: String?
) : MidiPortDetails {
    override val manufacturer: String? = info.vendor
    override val name: String? = prettyName ?: info.name
    override val version: String? = info.version
    override val midiTransportProtocol = 1 // MIDI 1.0
}

private class JvmMidiTransmitterPortDetails(
    val device: MidiDevice,
    id: String,
    prettyName: String?
) : JvmMidiPortDetails(id, device.deviceInfo, prettyName)

private class JvmMidiReceiverPortDetails(
    val device: MidiDevice,
    id: String,
    prettyName: String?
) : JvmMidiPortDetails(id, device.deviceInfo, prettyName)

private fun toJvmMidiMessage(data: ByteArray, start: Int, length: Int): JvmMidiMessage {
    require(length > 0) { "non-positive length" }
    val end = start + length
    require(start in 0..data.lastIndex && end <= data.size) { "slice OOB" }
    val arr = data.copyOfRange(start, end)
    val status = arr[0].toInt() and 0xFF

    return when {
        status == 0xF0 || status == 0xF7 -> SysexMessage(arr, arr.size)
        status == 0xFF -> {
            val type = if (arr.size > 1) arr[1].toInt() and 0xFF else 0
            val meta = if (arr.size > 2) arr.copyOfRange(2, arr.size) else ByteArray(0)
            MetaMessage(type, meta, meta.size)
        }
        else -> {
            val sm = ShortMessage()
            when (arr.size) {
                1 -> sm.setMessage(status)
                2 -> sm.setMessage(status, arr[1].toInt() and 0xFF, 0)
                else -> sm.setMessage(status, arr[1].toInt() and 0xFF, arr[2].toInt() and 0xFF)
            }
            sm
        }
    }
}

private class JvmMidiInput(private val port: JvmMidiTransmitterPortDetails) : MidiInput {
    override val details: MidiPortDetails = port
    private val state = MidiPortConnectionState.OPEN
    override val connectionState get() = state

    private var listener: OnMidiReceivedEventListener? = null
    private var transmitter: Transmitter? = null

    init {
        transmitter = port.device.transmitter
        transmitter?.receiver = object : Receiver {
            override fun close() {}
            override fun send(msg: JvmMidiMessage?, timestampUs: Long) {
                if (msg == null) return
                var start = 0
                var len = msg.length
                if (msg.message.isNotEmpty() && msg.message[0] == 0xF7.toByte()) {
                    start = 1
                    len -= 1
                }
                val tsNs = if (timestampUs > 0) timestampUs * 1000 else 0L
                listener?.onEventReceived(msg.message, start, len, tsNs)
            }
        }
    }

    override fun setMessageReceivedListener(listener: OnMidiReceivedEventListener) {
        this.listener = listener
    }

    override fun close() {
        try {
            transmitter?.close()
        } finally {
            transmitter = null
            if (port.device.transmitters.isEmpty() && port.device.receivers.isEmpty()) {
                port.device.close()
            }
        }
    }
}

private class JvmMidiOutput(private val port: JvmMidiReceiverPortDetails) : MidiOutput {
    override val details: MidiPortDetails get() = port
    private val state = MidiPortConnectionState.OPEN
    override val connectionState get() = state

    private var receiver: Receiver? = port.device.receiver
    private var multiPacketSysex = false

    override fun send(mevent: ByteArray, offset: Int, length: Int, timestampNs: Long) {
        if (length <= 0) return
        val first = mevent[offset]
        val last = mevent[offset + length - 1]
        val msg: JvmMidiMessage = if (multiPacketSysex) {
            val buffer = ByteArray(length + 1)
            buffer[0] = 0xF7.toByte()
            mevent.copyInto(buffer, 1, offset, offset + length)
            if (last == 0xF7.toByte()) multiPacketSysex = false
            toJvmMidiMessage(buffer, 0, buffer.size)
        } else {
            if (first == 0xF0.toByte() && last != 0xF7.toByte()) multiPacketSysex = true
            toJvmMidiMessage(mevent, offset, length)
        }
        val tsUs = if (timestampNs > 0) timestampNs / 1000 else -1L
        receiver?.send(msg, tsUs)
    }

    override fun close() {
        try {
            receiver?.close()
        } finally {
            receiver = null
            if (port.device.transmitters.isEmpty() && port.device.receivers.isEmpty()) {
                port.device.close()
            }
        }
    }
}

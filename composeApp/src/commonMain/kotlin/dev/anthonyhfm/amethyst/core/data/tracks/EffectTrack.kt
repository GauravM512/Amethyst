package dev.anthonyhfm.amethyst.core.data.tracks

import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.core.midi.data.MidiInputData
import dev.anthonyhfm.amethyst.core.midi.devices.DeviceType
import dev.anthonyhfm.amethyst.devices.BaseDevice
import dev.anthonyhfm.amethyst.devices.effects.EffectDevice
import dev.atsushieno.ktmidi.MidiOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class EffectTrack(
    override val name: String,
    override var projectDeviceIndex: Int? = null
) : Track<EffectDevice>() {
    var midiOutput: MidiOutput? = null
    var deviceType: DeviceType? = null

    override fun addDevice(device: BaseDevice<*>, atIndex: Int?) {
        super.addDevice(device, atIndex)

        CoroutineScope(Dispatchers.Main).launch {
            _devices.emit(
                _devices.value.mapIndexed { index, effectPlugin ->
                    if (index + 1 < _devices.value.size) {
                        effectPlugin.midiOutput = {
                            CoroutineScope(Dispatchers.IO).launch {
                                _devices.value[index + 1].passData(it)
                            }
                        }

                        return@mapIndexed effectPlugin
                    } else {
                        effectPlugin.midiOutput = {
                            outputMidiEffectData(it)
                        }

                        return@mapIndexed effectPlugin
                    }
                }
            )
        }
    }

    override fun processMidiInputData(midiInputData: MidiInputData) {
        val white = if (midiInputData.velocity == 0) 0 else 63

        val midiEffectData = MidiEffectData(
            x = midiInputData.pitch % 10,
            y = midiInputData.pitch / 10,
            r = white,
            g = white,
            b = white
        )

        if (devices.value.isEmpty()) {
            outputMidiEffectData(midiEffectData)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                devices.value[0].passData(midiEffectData)
            }
        }
    }

    private fun outputMidiEffectData(data: MidiEffectData) {
        CoroutineScope(Dispatchers.IO).launch {
            val outputData = deviceType?.getEffectSysEx(data) ?: return@launch

            midiOutput?.send(
                mevent = outputData,
                length = outputData.size,
                offset = 0,
                timestampInNanoseconds = 0
            )
        }
    }
}
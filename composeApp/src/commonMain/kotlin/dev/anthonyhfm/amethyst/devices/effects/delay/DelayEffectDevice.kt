package dev.anthonyhfm.amethyst.devices.effects.delay

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.EffectDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import dev.anthonyhfm.amethyst.ui.components.TextDial
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

class DelayEffectDevice : EffectDevice<DelayEffectDeviceState>() {
    override val state = MutableStateFlow(DelayEffectDeviceState())

    @Composable
    override fun Content() {
        val deviceState by state.collectAsState()

        AmethystPlugin(
            title = "Delay",
            modifier = Modifier
                .width(100.dp)
        ) {
            TextDial(
                headline = "Delay",
                text = "${deviceState.delayMs} ms",
                value = deviceState.delayMs / 1000f,
                onValueChange = {
                    state.update {
                        it.copy(
                            delayMs = (it.delayMs * 1000).toInt()
                        )
                    }
                }
            )
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        delay(state.value.delayMs.milliseconds)

        midiOutput(data)
    }
}

@Serializable
data class DelayEffectDeviceState(
    val delayMs: Int = 200
) : DeviceState()
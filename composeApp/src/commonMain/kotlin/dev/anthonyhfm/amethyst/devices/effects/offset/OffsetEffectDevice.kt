package dev.anthonyhfm.amethyst.devices.effects.offset

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.EffectDevice
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

class OffsetEffectDevice : EffectDevice<OffsetEffectDeviceState>() {
    override val state = MutableStateFlow(OffsetEffectDeviceState())

    @Composable
    override fun Content() {
        val deviceState by state.collectAsState()

        AmethystPlugin(
            title = "Offset",
            modifier = Modifier
                .width(145.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = "No UI for this yet lmao"
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Offset X: ${deviceState.offsetX}"
                )

                Text(
                    text = "Offset Y: ${deviceState.offsetY}"
                )

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(2.dp)
                            .clickable {
                                state.update {
                                    it.copy(
                                        offsetY = it.offsetY + 1
                                    )
                                }
                            }
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(2.dp)
                            .clickable {
                                state.update {
                                    it.copy(
                                        offsetY = it.offsetY - 1
                                    )
                                }
                            }
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(2.dp)
                            .clickable {
                                state.update {
                                    it.copy(
                                        offsetX = it.offsetX - 1
                                    )
                                }
                            }
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(2.dp)
                            .clickable {
                                state.update {
                                    it.copy(
                                        offsetX = it.offsetX + 1
                                    )
                                }
                            }
                    )
                }
            }
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        midiOutput(
            data.copy(
                x = data.x + state.value.offsetX,
                y = data.y + state.value.offsetY
            )
        )
    }
}

@Serializable
data class OffsetEffectDeviceState(
    val offsetX: Int = 0,
    val offsetY: Int = 0
) : DeviceState()
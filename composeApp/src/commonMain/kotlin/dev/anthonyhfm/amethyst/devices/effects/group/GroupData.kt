package dev.anthonyhfm.amethyst.devices.effects.group

import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.EffectDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GroupData(
    val name: String,
    val deviceStates: List<DeviceState> = emptyList(),

    @Transient
    val devices: MutableStateFlow<List<EffectDevice<*>>> = MutableStateFlow(emptyList())
)
package dev.anthonyhfm.amethyst.devices

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

interface BaseDevice <Data, State : DeviceState> {
    val state: MutableStateFlow<State>

    @Composable
    fun Content()

    suspend fun passData(data: Data)
}
package dev.anthonyhfm.amethyst.devices

import androidx.compose.runtime.Composable

interface BaseDevice <Data> {
    @Composable
    fun Content()

    suspend fun passData(data: Data)
}
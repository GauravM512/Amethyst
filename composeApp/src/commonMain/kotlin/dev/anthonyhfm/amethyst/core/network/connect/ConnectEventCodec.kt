package dev.anthonyhfm.amethyst.core.network.connect

import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectEvent
import dev.anthonyhfm.amethyst.core.util.AmethystProtoBuf
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun ConnectEvent.encodeToBytes(): ByteArray =
    AmethystProtoBuf.encodeToByteArray(ConnectEvent.serializer(), this)

@OptIn(ExperimentalSerializationApi::class)
fun ByteArray.decodeToConnectEvent(): ConnectEvent =
    AmethystProtoBuf.decodeFromByteArray(ConnectEvent.serializer(), this)

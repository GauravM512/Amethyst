package dev.anthonyhfm.amethyst.core.util

import dev.anthonyhfm.amethyst.devices.DeviceSerializationModule
import dev.anthonyhfm.amethyst.timeline.TimelineSerializationModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
val AmethystProtoBuf = ProtoBuf {
    serializersModule = SerializersModule {
        include(DeviceSerializationModule)
        include(TimelineSerializationModule)
    }
}
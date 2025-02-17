package dev.anthonyhfm.amethyst.core.data.project

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

object AmethystReader {
    @OptIn(ExperimentalSerializationApi::class)
    fun readFromFile(byteArray: ByteArray) {
        val project = ProtoBuf.decodeFromByteArray(
            deserializer = AmethystProject.serializer(),
            bytes = byteArray
        )


    }
}
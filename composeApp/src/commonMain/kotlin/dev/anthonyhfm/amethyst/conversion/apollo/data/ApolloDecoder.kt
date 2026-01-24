package dev.anthonyhfm.amethyst.conversion.apollo.data

import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain

class ApolloDecoder(
    data: ByteArray,
) {
    private val MAX_APOLLO_VERSION = 32
    private val reader: ApolloBinaryReader = data.asApolloBinaryReader()

    fun decode(): StateChain {
        reader.expectMagic()

        val version = reader.readInt32()

        if (version > MAX_APOLLO_VERSION) {
            error("Apollo version $version is not supported. Current max supported version is $MAX_APOLLO_VERSION")
        }

        return StateChain()
    }
}
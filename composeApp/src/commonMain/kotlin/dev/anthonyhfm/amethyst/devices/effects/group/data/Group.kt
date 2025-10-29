package dev.anthonyhfm.amethyst.devices.effects.group.data

import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.util.UUID
import dev.anthonyhfm.amethyst.core.util.randomUUID
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.random.Random

@Serializable
data class Group(
    val name: String,
    @Transient
    val chain: Chain = Chain(),
    var stateChain: StateChain = StateChain(),

    @Transient
    val id: String = UUID.randomUUID(),
)
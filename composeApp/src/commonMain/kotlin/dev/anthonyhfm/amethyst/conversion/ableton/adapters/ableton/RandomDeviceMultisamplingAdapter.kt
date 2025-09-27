package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.XmlElement
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState.TYPE
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain

class RandomDeviceMultisamplingAdapter (
    private val randomDeviceXml: XmlElement,
    private val containerXml: XmlElement
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val randomChance = randomDeviceXml.querySelector("Chance").first()
            .querySelector("Manual").first().attributes["Value"]?.toDoubleOrNull() ?: 0.0

        val multiSteps = randomDeviceXml.querySelector("Choices").first()
            .querySelector("Manual").first().attributes["Value"]?.toDoubleOrNull() ?: 1.0

        val altModeEnabled = randomDeviceXml.querySelector("Alternate").first()
            .querySelector("Manual").first().attributes["Value"] == "true"

        if (randomChance == 1.0 && altModeEnabled) {
            return listOf(
                MultiGroupChainDeviceState(
                    type = TYPE.FORWARD,
                    groups = List(multiSteps.toInt()) { step ->
                        val branches: List<XmlElement> = containerXml.localQuerySelector("Branches").first().children
                        val branch = branches.getOrNull(step)

                        val name = branch?.querySelector("UserName")?.getOrNull(0)?.attributes?.get("Value")
                            ?: "Chain ${step + 1}"

                        Group(
                            name = name,
                            stateChain = StateChain(
                                devices = mutableListOf<DeviceState>().apply {
                                    branch?.let {
                                        addAll(
                                            // TODO: remove pitch element before sample when using instrument rack (used in some projects)
                                            it.querySelector("DeviceChain").first()
                                                .querySelector("Devices").first()
                                                .children.mapNotNull { child ->
                                                    resolveAdapter(child)
                                                        ?.toDeviceStates()
                                                        ?.firstOrNull()
                                                }
                                        )
                                    }
                                }
                            )
                        )
                    }
                )
            )
        }

        println("Random device does not have Chance 100% and Alt mode, skipping...; Values: chance=$randomChance, steps=$multiSteps, alt=$altModeEnabled")
        return listOf()
    }
}
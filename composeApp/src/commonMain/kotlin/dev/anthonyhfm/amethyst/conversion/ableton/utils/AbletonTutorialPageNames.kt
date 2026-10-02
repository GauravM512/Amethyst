package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.DrumGroupDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiEffectGroupDevice

internal class AbletonTutorialPageNames(
    private val namedPages: Map<Int, Set<Int>>,
    private val availablePages: Set<Int>,
) {
    fun resolve(number: Int, hasPageZero: Boolean): Int? {
        if (namedPages.isNotEmpty()) {
            return namedPages[number]?.singleOrNull()
        }

        val offset = if (!hasPageZero && 0 in availablePages) {
            1
        } else {
            0
        }
        return (number - offset).takeIf { it in availablePages }
    }

    companion object {
        private data class PageBranch(
            val name: String,
            val minimum: Int,
            val maximum: Int,
        )

        private val pageName = Regex(pattern = "\\s*Page\\s+(\\d{1,2})\\s*", option = RegexOption.IGNORE_CASE)

        fun pageNumber(name: String): Int? =
            pageName.matchEntire(input = name)?.groupValues?.get(index = 1)?.toIntOrNull()

        fun fromTracks(tracks: List<MidiTrack>): AbletonTutorialPageNames {
            val namedPages = mutableMapOf<Int, MutableSet<Int>>()
            val availablePages = mutableSetOf<Int>()

            for (device in tracks.flatMap { it.deviceChain.devices }) {
                val branches: List<PageBranch>
                val hasKeyMidiMapping: Boolean
                val sourceOffset: Int

                when (device) {
                    is InstrumentGroupDevice -> {
                        branches = device.branches.branches.map {
                            PageBranch(
                                name = it.name.effectiveName?.value.orEmpty(),
                                minimum = it.branchSelectorRange.min.value,
                                maximum = it.branchSelectorRange.max.value,
                            )
                        }
                        hasKeyMidiMapping = device.chainSelector.keyMidi != null
                        sourceOffset = AbletonPageIndexing.sourceOffset(
                            selectorMinimum = device.chainSelector.midiControllerRange?.min?.value,
                        )
                    }
                    is MidiEffectGroupDevice -> {
                        branches = device.branches.branches.map {
                            PageBranch(
                                name = it.name.effectiveName?.value.orEmpty(),
                                minimum = it.branchSelectorRange.min.value,
                                maximum = it.branchSelectorRange.max.value,
                            )
                        }
                        hasKeyMidiMapping = device.chainSelector.keyMidi != null
                        sourceOffset = AbletonPageIndexing.sourceOffset(
                            selectorMinimum = device.chainSelector.midiControllerRange?.min?.value,
                        )
                    }
                    is DrumGroupDevice -> {
                        branches = device.branches.branches.map {
                            PageBranch(
                                name = it.name.effectiveName?.value.orEmpty(),
                                minimum = it.branchSelectorRange.min.value,
                                maximum = it.branchSelectorRange.max.value,
                            )
                        }
                        hasKeyMidiMapping = device.chainSelector.keyMidi != null
                        sourceOffset = 0
                    }
                    else -> continue
                }

                val controlsPages = AbletonPageIndexing.controlsPages(
                    hasKeyMidiMapping = hasKeyMidiMapping,
                    selectorRanges = branches.map { it.minimum to it.maximum },
                )
                if (!controlsPages) {
                    continue
                }

                for ((name, minimum, maximum) in branches) {
                    val page = AbletonPageIndexing.normalizeSelectorValue(
                        value = minimum,
                        sourceOffset = sourceOffset,
                    )
                    if (minimum != maximum || page !in 0..15) {
                        continue
                    }

                    availablePages.add(element = page)
                    val number = pageNumber(name = name) ?: continue
                    namedPages.getOrPut(key = number) { mutableSetOf() }.add(element = page)
                }
            }

            return AbletonTutorialPageNames(namedPages = namedPages, availablePages = availablePages)
        }
    }
}

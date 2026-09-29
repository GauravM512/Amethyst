package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonKeyMidi
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonMidiControllerRange
import kotlin.math.roundToInt

internal object AbletonMacroMapping {
    fun effectiveValue(
        manualValue: Float,
        keyMidi: AbletonKeyMidi?,
        controllerRange: AbletonMidiControllerRange?,
        parentMacroValues: List<Float>?,
    ): Float {
        if (keyMidi?.channel?.value != 16) {
            return manualValue
        }

        val macroIndex = keyMidi.noteOrController?.value ?: return manualValue
        val macroValue = parentMacroValues?.getOrNull(macroIndex) ?: return manualValue
        val minimum = controllerRange?.min?.value?.toFloat() ?: 0f
        val maximum = controllerRange?.max?.value?.toFloat() ?: 127f

        return minimum + macroValue.coerceIn(0f, 127f) / 127f * (maximum - minimum)
    }

    fun effectiveInt(
        manualValue: Int,
        keyMidi: AbletonKeyMidi?,
        controllerRange: AbletonMidiControllerRange?,
        parentMacroValues: List<Float>?,
    ): Int = effectiveValue(
        manualValue = manualValue.toFloat(),
        keyMidi = keyMidi,
        controllerRange = controllerRange,
        parentMacroValues = parentMacroValues,
    ).roundToInt()
}

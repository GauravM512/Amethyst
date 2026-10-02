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
        if (!macroValue.isFinite()) {
            return manualValue
        }

        val minimum = controllerRange?.min?.value?.toDouble() ?: 0.0
        val maximum = controllerRange?.max?.value?.toDouble() ?: 127.0

        return (
            minimum + macroValue.coerceIn(0f, 127f).toDouble() / 127.0 * (maximum - minimum)
        ).toFloat()
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

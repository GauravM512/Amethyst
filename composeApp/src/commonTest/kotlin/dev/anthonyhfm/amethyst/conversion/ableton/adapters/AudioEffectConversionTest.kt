package dev.anthonyhfm.amethyst.conversion.ableton.adapters

import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.*
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import dev.anthonyhfm.amethyst.devices.audio.effects.DuckerChainDeviceState
import dev.anthonyhfm.amethyst.devices.audio.effects.EqEightChainDeviceState
import dev.anthonyhfm.amethyst.devices.audio.effects.LimiterChainDeviceState
import dev.anthonyhfm.amethyst.devices.audio.effects.StereoGainChainDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AudioEffectConversionTest {
    @Test
    fun eq8PreservesBandsAndMutedState() {
        val input = Eq8(
            on = AbletonOn(AbletonManual(false)),
            mode = AbletonValue(2),
            globalGain = AbletonFloatParameter(3f),
            band0 = Eq8Band(parameterA = Eq8BandParameter(
                isOn = AbletonOn(AbletonManual(true)),
                freq = AbletonFloatParameter(440f),
                gain = AbletonFloatParameter(-6f),
            )),
        )
        val result = assertIs<EqEightChainDeviceState>(AbletonAdapter.resolveAdapter(input)!!.toDeviceStates().single())
        assertEquals(2, result.channelMode)
        assertEquals(3f, result.globalGainDb)
        assertEquals(440f, result.bandsA.first().frequencyHz)
        assertEquals(-6f, result.bandsA.first().gainDb)
        assertTrue(result.bandsA.first().enabled)
        assertTrue(result.isMuted)
    }

    @Test
    fun stereoGainConvertsLinearGainAndPreservesPhaseAndWidth() {
        val input = StereoGain(
            gain = AbletonFloatParameter(0.5f),
            stereoWidth = AbletonFloatParameter(1.5f),
            phaseInvertL = AbletonOn(AbletonManual(true)),
            mono = AbletonOn(AbletonManual(true)),
        )
        val result = assertIs<StereoGainChainDeviceState>(AbletonAdapter.resolveAdapter(input)!!.toDeviceStates().single())
        assertTrue(result.gainDb in -6.03f..-6.01f)
        assertEquals(1.5f, result.width)
        assertTrue(result.phaseInvertLeft)
        assertTrue(result.mono)
    }

    @Test
    fun limiterClampsCeilingAndMapsLookahead() {
        val input = Limiter(ceiling = AbletonFloatParameter(2f), lookahead = AbletonIntParameter(2))
        val result = assertIs<LimiterChainDeviceState>(AbletonAdapter.resolveAdapter(input)!!.toDeviceStates().single())
        assertEquals(0f, result.ceilingDb)
        assertEquals(6f, result.lookaheadMs)
    }

    @Test
    fun compressorRequiresEnabledSidechainAndValidRouting() {
        val base = Compressor2(sideChain = CompressorSideChain(
            onOff = AbletonOn(AbletonManual(true)),
            routedInput = CompressorRoutedInput(
                routable = CompressorRoutable(AbletonValue("AudioIn/Track.17/PostFxOut")),
            ),
        ))
        val converted = assertIs<DuckerChainDeviceState>(AbletonAdapter.resolveAdapter(base)!!.toDeviceStates().single())
        assertEquals("ableton-track-17-postfx", converted.sidechainBusId)
        assertEquals(2f, converted.ratio)
        assertTrue(AbletonAdapter.resolveAdapter(base.copy(sideChain = CompressorSideChain()))!!.toDeviceStates().isEmpty())
        assertTrue(AbletonAdapter.resolveAdapter(base.copy(
            sideChain = base.sideChain.copy(routedInput = CompressorRoutedInput()),
        ))!!.toDeviceStates().isEmpty())
    }
}

package dev.anthonyhfm.amethyst.conversion.ableton.utils

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonLaunchpadLayout
import dev.anthonyhfm.amethyst.conversion.ableton.AbletonXmlDecoder
import dev.anthonyhfm.amethyst.conversion.ableton.data.AbletonDevice
import dev.anthonyhfm.amethyst.conversion.ableton.data.AutomationEnvelopes
import dev.anthonyhfm.amethyst.conversion.ableton.data.DeviceChain
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiClip
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice
import dev.anthonyhfm.amethyst.workspace.data.AutoPlayData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbletonTutorialDetectorTest {
    @Test
    fun kwikFlipClipBoundariesSelectRackPagesBeforeNotes() = withConverter {
        val localProject = System.getenv("AMETHYST_KWIK_FLIP_ALS")
        val tracks = if (localProject != null) {
            val ableton = AbletonXmlDecoder.decodeFile(path = localProject, xml = AbletonConverter.xml)
            AbletonConverter.bpm = ableton.liveSet.masterTrack.deviceChain.mixer.tempo.manual.value
            ableton.liveSet.tracks.midiTracks
        } else {
            listOf(
                track(
                    name = "Samples",
                    clips = listOf(
                        clip(name = "Page 1", start = 0.0),
                        clip(name = "Page 2", start = 64.0),
                        clip(name = "Page 3", start = 192.0),
                        clip(name = "Page 2", start = 256.0),
                    ),
                ),
                track(id = 2, name = "Tutorial", clips = emptyList(), devices = emptyList()),
            )
        }
        val layout = AbletonLayout.Single(audioTrack = tracks.first(), lightsTrack = null)
        val actions = AbletonTutorialDetector.getAutoPlayData(layout = layout, tracks = tracks).actions
        val expectedPages = listOf(0.0 to 0, 64.0 to 1, 192.0 to 2, 256.0 to 1)

        assertEquals(expected = 4, actual = pagePresses(actions = actions).size)
        for ((beat, page) in expectedPages) {
            val time = AbletonTutorialDetector.beatsToMilliseconds(beats = beat, bpm = AbletonConverter.bpm)
            val press = actions.getValue(key = time).first()
            assertEquals(expected = 9, actual = press.x)
            assertEquals(expected = page + 1, actual = press.y)
            assertTrue(actual = press.down)
            assertTrue(actual = press.beforeNotes)
            assertEquals(
                expected = AbletonConverter.launchpadLayout?.target(index = 0)?.launchpad?.id,
                actual = press.launchpadId,
            )
            assertTrue(actual = press.copy(down = false) in actions.getValue(key = time + 50.0))
            val notes = actions.getValue(key = time).filter { it.x % 10 in 1..8 && it.y in 1..8 }
            assertTrue(actual = notes.isNotEmpty())
            assertTrue(actual = notes.none { it.beforeNotes })
        }
    }

    @Test
    fun macroAutomationSuppressesClipNameFallback() = withConverter {
        val tutorial = track(clips = listOf(clip(name = "Page 1", start = 0.0), clip(name = "Page 2", start = 64.0)))
            .copy(
                automationEnvelopes = AutomationEnvelopes(
                    envelopes = AutomationEnvelopes.Envelopes(
                        envelopes = listOf(
                            AutomationEnvelopes.AutomationEnvelope(
                                envelopeTarget = AutomationEnvelopes.EnvelopeTarget(
                                    pointeeId = AutomationEnvelopes.PointeeId(value = 123),
                                ),
                                automation = AutomationEnvelopes.Automation(
                                    events = AutomationEnvelopes.Events(
                                        floatEvents = listOf(AutomationEnvelopes.FloatEvent(time = 0.0, value = 2f)),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        assertEquals(expected = listOf(3), actual = pagePresses(actions = detect(track = tutorial)).map { it.y })
    }

    @Test
    fun recordedPageButtonSuppressesClipNameFallback() = withConverter {
        for (pitch in listOf(100, 108)) {
            val tutorial = track(
                clips = listOf(clip(name = "Page 1", start = 0.0, pitch = pitch), clip(name = "Page 2", start = 64.0)),
            )
            assertEquals(expected = 1, actual = pagePresses(actions = detect(track = tutorial)).size)
        }
    }

    @Test
    fun rackNamesResolvePageOneToItsActualSelector() = withConverter {
        val tutorial = track(devices = listOf(rack(pages = listOf("Page 1" to 1, "Page 2" to 2))))
        assertEquals(expected = listOf(2), actual = pagePresses(actions = detect(track = tutorial)).map { it.y })
    }

    @Test
    fun oneBasedRackSelectorUsesConvertedPageIndex() = withConverter {
        val tutorial = track(devices = listOf(rack(pages = listOf("Page 1" to 1, "Page 2" to 2), minimum = 1)))
        assertEquals(expected = listOf(1), actual = pagePresses(actions = detect(track = tutorial)).map { it.y })
    }

    @Test
    fun unnamedRackPagesInferLabelBaseFromDiscoveredPages() = withConverter {
        val devices = listOf(rack(pages = listOf("A" to 0, "B" to 1, "C" to 2)))
        val oneBased = track(clips = listOf(clip(name = "Page 2", start = 0.0)), devices = devices)
        assertEquals(expected = listOf(2), actual = pagePresses(actions = detect(track = oneBased)).map { it.y })

        val zeroBased = track(
            clips = listOf(clip(name = "Page 0", start = 0.0), clip(name = "Page 2", start = 64.0)),
            devices = devices,
        )
        assertEquals(expected = listOf(1, 3), actual = pagePresses(actions = detect(track = zeroBased)).map { it.y })
    }

    @Test
    fun unknownAndAmbiguousPagesDoNotProduceSwitches() = withConverter {
        val withoutPages = track(devices = emptyList())
        assertTrue(actual = pagePresses(actions = detect(track = withoutPages)).isEmpty())

        val unknown = track(clips = listOf(clip(name = "Page 9", start = 0.0)))
        assertTrue(actual = pagePresses(actions = detect(track = unknown)).isEmpty())

        val ambiguous = track(devices = listOf(rack(pages = listOf("Page 1" to 0, "Page 1" to 1))))
        assertTrue(actual = pagePresses(actions = detect(track = ambiguous)).isEmpty())

        val unrelated = track(clips = listOf(clip(name = "Tutorial Page 1 take 2", start = 0.0)))
        assertTrue(actual = pagePresses(actions = detect(track = unrelated)).isEmpty())
    }

    @Test
    fun duplicateLabelsDoNotRepeatSwitchesAndSecondBankUsesLeftButtons() = withConverter {
        val tutorial = track(
            clips = listOf(
                clip(name = "page 9", start = 32.0),
                clip(name = "Page 9", start = 64.0),
                clip(name = "Page 10", start = 96.0),
            ),
            devices = listOf(rack(pages = listOf("Page 9" to 8, "Page 10" to 9))),
        )
        val actions = detect(track = tutorial)
        assertEquals(expected = listOf(1, 2), actual = pagePresses(actions = actions).map { it.y })
        assertTrue(actual = pagePresses(actions = actions).all { it.x == 0 })
        assertTrue(actual = actions.getValue(key = 0.0).first().down)
    }

    @Test
    fun dualLayoutKeepsEachControllersPageMapping() = withConverter(count = 2) {
        val left = track(id = 1, name = "Samples Left")
        val right = track(
            id = 2,
            name = "Samples Right",
            devices = listOf(rack(pages = listOf("Page 1" to 2, "Page 2" to 3))),
        )
        val layout = AbletonLayout.Dual2Light(audioLeft = left, audioRight = right, lightsLeft = null, lightsRight = null)
        val actions = AbletonTutorialDetector.getAutoPlayData(layout = layout, tracks = listOf(left, right)).actions
        val presses = pagePresses(actions = actions)
        assertEquals(expected = listOf(9 to 1, 19 to 3), actual = presses.map { it.x to it.y })
        assertEquals(
            expected = listOf(0, 1).map { AbletonConverter.launchpadLayout?.target(index = it)?.launchpad?.id },
            actual = presses.map { it.launchpadId },
        )
    }

    private fun detect(track: MidiTrack): Map<Double, List<AutoPlayData.Action>> =
        AbletonTutorialDetector.getAutoPlayData(
            layout = AbletonLayout.Single(audioTrack = track, lightsTrack = null),
            tracks = listOf(track),
        ).actions

    private fun pagePresses(actions: Map<Double, List<AutoPlayData.Action>>): List<AutoPlayData.Action> =
        actions.toSortedMap().values.flatten().filter { it.down && it.x % 10 in setOf(0, 9) && it.y in 1..8 }

    private fun withConverter(count: Int = 1, block: () -> Unit) {
        val previousBpm = AbletonConverter.bpm
        val previousLayout = AbletonConverter.launchpadLayout
        try {
            AbletonConverter.bpm = 120.0
            AbletonConverter.launchpadLayout = AbletonLaunchpadLayout.create(count = count)
            block()
        } finally {
            AbletonConverter.bpm = previousBpm
            AbletonConverter.launchpadLayout = previousLayout
        }
    }

    private fun track(
        id: Int = 1,
        name: String = "Tutorial",
        clips: List<MidiClip> = listOf(clip(name = "Page 1", start = 0.0)),
        devices: List<AbletonDevice> = listOf(rack()),
    ): MidiTrack = MidiTrack(
        id = id,
        _name = MidiTrack.Name(effectiveName = MidiTrack.Name.EffectiveName(value = name)),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(devices = DeviceChain.DeviceChain.Devices(devices = devices)),
            mainSequencer = DeviceChain.MainSequencer(
                clipTimeable = DeviceChain.MainSequencer.ClipTimeable(
                    arrangerAutomation = DeviceChain.MainSequencer.ClipTimeable.ArrangerAutomation(
                        events = DeviceChain.MainSequencer.ClipTimeable.ArrangerAutomation.Events(clips = clips),
                    ),
                ),
            ),
        ),
    )

    private fun clip(name: String, start: Double, pitch: Int = 36): MidiClip = MidiClip(
        time = start,
        clipName = MidiClip.ClipName(value = name),
        currentStart = MidiClip.CurrentTimeStamp(value = start),
        currentEnd = MidiClip.CurrentTimeStamp(value = start + 64.0),
        notes = MidiClip.Notes(
            keyTracks = MidiClip.Notes.KeyTracks(
                tracks = listOf(
                    MidiClip.Notes.KeyTracks.KeyTrack(
                        midiKey = MidiClip.Notes.KeyTracks.KeyTrack.MidiKey(value = pitch),
                        notes = MidiClip.Notes.KeyTracks.KeyTrack.Notes(
                            notes = listOf(
                                MidiClip.Notes.KeyTracks.KeyTrack.Notes.MidiNoteEvent(
                                    time = 0.0,
                                    duration = 0.25,
                                    velocity = 100f,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun rack(
        pages: List<Pair<String, Int>> = listOf("Page 1" to 0, "Page 2" to 1, "Page 3" to 2),
        minimum: Int = 0,
    ): InstrumentGroupDevice {
        val branches = pages.mapIndexed { index, (name, page) ->
            """
                <InstrumentBranch Id="$index">
                    <Name><EffectiveName Value="$name"/></Name>
                    <DeviceChain><MidiToAudioDeviceChain><Devices/></MidiToAudioDeviceChain></DeviceChain>
                    <ZoneSettings><KeyRange><Min Value="0"/><Max Value="127"/></KeyRange></ZoneSettings>
                    <BranchSelectorRange><Min Value="$page"/><Max Value="$page"/></BranchSelectorRange>
                    <MixerDevice><Speaker><Manual Value="true"/></Speaker></MixerDevice>
                </InstrumentBranch>
            """.trimIndent()
        }.joinToString(separator = "\n")

        return AbletonConverter.xml.decodeFromString(
            deserializer = InstrumentGroupDevice.serializer(),
            string = """
                <InstrumentGroupDevice Id="0">
                    <Branches>$branches</Branches>
                    <ChainSelector>
                        <MidiControllerRange><Min Value="$minimum"/><Max Value="127"/></MidiControllerRange>
                        <AutomationTarget Id="123"/>
                    </ChainSelector>
                </InstrumentGroupDevice>
            """.trimIndent(),
        )
    }
}

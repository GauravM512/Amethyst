package dev.anthonyhfm.amethyst.workspace

import androidx.compose.ui.graphics.Color
import dev.anthonyhfm.amethyst.core.engine.elements.Chain
import dev.anthonyhfm.amethyst.core.engine.elements.Signal
import dev.anthonyhfm.amethyst.core.engine.elements.currentSignalMacroValues
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.group.GroupChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.macro_filter.MacroFilterChainDevice
import dev.anthonyhfm.amethyst.devices.effects.macro_filter.MacroFilterChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDevice
import dev.anthonyhfm.amethyst.devices.effects.switch.MacroControlChainDeviceState
import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain
import dev.anthonyhfm.amethyst.workspace.data.Macro
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MacroPageSwitchingTest {

    @BeforeTest
    fun setup() {
        WorkspaceRepository.setMacros(listOf(Macro(id = "macro-0", value = 0)), undoable = false)
    }

    @AfterTest
    fun tearDown() {
        WorkspaceRepository.setMacros(listOf(Macro(id = "macro-0", value = 0)), undoable = false)
    }

    @Test
    fun effectOnPageATriggersWhenSameButtonSwitchesToPageBInSameRack() {
        val page1Signals = mutableListOf<Signal>()
        val page2Signals = mutableListOf<Signal>()

        val page1Filter = MacroFilterChainDevice().apply {
            state.value = MacroFilterChainDeviceState(macro = 0, allowedValues = setOf(0))
        }
        val page1Chain = Chain().apply {
            devices.value = listOf(page1Filter)
            reroute()
            signalExit = { page1Signals.addAll(it) }
        }

        val page2Filter = MacroFilterChainDevice().apply {
            state.value = MacroFilterChainDeviceState(macro = 0, allowedValues = setOf(1))
        }
        val page2Chain = Chain().apply {
            devices.value = listOf(page2Filter)
            reroute()
            signalExit = { page2Signals.addAll(it) }
        }

        val pageSwitcher = MacroControlChainDevice().apply {
            state.value = MacroControlChainDeviceState(macro = 0, value = 1, macroId = "macro-0")
        }
        val pageSwitchChain = Chain().apply {
            devices.value = listOf(pageSwitcher)
            reroute()
        }

        val groupDevice = GroupChainDevice().apply {
            state.value = GroupChainDeviceState(
                groups = listOf(
                    Group(name = "Page 1 Effect", stateChain = StateChain()).apply {
                        chain.devices.value = page1Chain.devices.value
                        chain.reroute()
                        chain.signalExit = page1Chain.signalExit
                    },
                    Group(name = "Page 2 Effect", stateChain = StateChain()).apply {
                        chain.devices.value = page2Chain.devices.value
                        chain.reroute()
                        chain.signalExit = page2Chain.signalExit
                    },
                    // Page Switching is placed at the end of the groups
                    Group(name = "Page Switching", stateChain = StateChain()).apply {
                        chain.devices.value = pageSwitchChain.devices.value
                        chain.reroute()
                    }
                )
            )
        }

        val macroSnapshot = currentSignalMacroValues()
        assertEquals(listOf(0), macroSnapshot)

        val inputSignals = listOf(
            Signal.LED(
                origin = null,
                x = 9,
                y = 2,
                color = Color.White,
                layer = 0,
                macroValues = macroSnapshot,
            )
        )

        groupDevice.signalEnter(inputSignals)

        // Page 1 effect should have triggered
        assertEquals(1, page1Signals.size, "Page 1 effect should have triggered")
        // Page 2 effect should NOT have triggered
        assertEquals(0, page2Signals.size, "Page 2 effect should not have triggered")
        // Macro 0 should now be switched to 1 (Page 2)
        assertEquals(1, WorkspaceRepository.macros.value[0].value, "Macro 0 should be switched to Page 2")
    }

    @Test
    fun crossChainPageSwitchingPreservesPageAEffectOnOtherChain() {
        val lightsSignals = mutableListOf<Signal>()

        // lightsChain has a Page 1 effect (active when macro 0 == 0)
        val lightsFilter = MacroFilterChainDevice().apply {
            state.value = MacroFilterChainDeviceState(macro = 0, allowedValues = setOf(0))
        }
        val lightsChain = Chain().apply {
            devices.value = listOf(lightsFilter)
            reroute()
            signalExit = { lightsSignals.addAll(it) }
        }

        // samplingChain has Page Switching to Page 2 (value 1)
        val pageSwitcher = MacroControlChainDevice().apply {
            state.value = MacroControlChainDeviceState(macro = 0, value = 1, macroId = "macro-0")
        }
        val samplingChain = Chain().apply {
            devices.value = listOf(pageSwitcher)
            reroute()
        }

        // Pad press on (9, 2) while on Page 1 (macro 0 == 0)
        val macroSnapshot = currentSignalMacroValues()
        assertEquals(listOf(0), macroSnapshot)

        val midiSignals = listOf(
            Signal.Midi(
                origin = null,
                x = 9,
                y = 2,
                velocity = 127,
                macroValues = macroSnapshot,
            )
        )
        val ledSignals = listOf(
            Signal.LED(
                origin = null,
                x = 9,
                y = 2,
                color = Color.White,
                layer = 0,
                macroValues = macroSnapshot,
            )
        )

        // As in AmethystMidiManager: samplingChain runs first, then lightsChain
        samplingChain.signalEnter(midiSignals)
        assertEquals(1, WorkspaceRepository.macros.value[0].value, "Sampling chain switched macro to 1")

        lightsChain.signalEnter(ledSignals)
        // Lights effect on Page 1 must still trigger because ledSignals captured macroSnapshot == [0]
        assertEquals(1, lightsSignals.size, "Lights effect on Page 1 should have received signal")
    }

    @Test
    fun abletonAdaptersPlacePageSwitchingAtEndOfGroups() {
        val branch = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch(
            id = 0,
            name = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.Name(
                effectiveName = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.Name.EffectiveName("Branch 0")
            ),
            deviceChain = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.DeviceChain(
                deviceChain = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.DeviceChain.MidiToAudioDeviceChain(
                    devices = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.DeviceChain.MidiToAudioDeviceChain.Devices(emptyList())
                )
            ),
            zoneSettings = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.ZoneSettings(
                keyRange = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.ZoneSettings.KeyRange(
                    min = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.ZoneSettings.KeyRange.MinMax(0),
                    max = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.ZoneSettings.KeyRange.MinMax(127),
                )
            ),
            branchSelectorRange = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.BranchSelectorRange(
                min = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.BranchSelectorRange.MinMax(1),
                max = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.BranchSelectorRange.MinMax(1),
            ),
            masterDevice = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.MixerDevice(
                speaker = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches.InstrumentBranch.MixerDevice.Speaker(
                    dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual(true)
                )
            )
        )
        val instrumentContainer = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice(
            id = 1,
            on = dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn(manual = dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual(true)),
            chainSelector = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.ChainSelector(
                keyMidi = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.ChainSelector.KeyMidi(),
            ),
            branches = dev.anthonyhfm.amethyst.conversion.ableton.data.devices.InstrumentGroupDevice.Branches(listOf(branch))
        )
        dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter.launchpadLayout =
            dev.anthonyhfm.amethyst.conversion.ableton.AbletonLaunchpadLayout.create(1)
        val adapter = dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton.InstrumentGroupAdapter(
            device = instrumentContainer,
            offset = androidx.compose.ui.unit.IntOffset.Zero,
            outputOffset = androidx.compose.ui.unit.IntOffset.Zero,
            chainDepth = 0,
        )
        val result = adapter.toDeviceStates()
        val groupState = kotlin.test.assertIs<GroupChainDeviceState>(result.first())
        assertEquals("Branch 0", groupState.groups.first().name)
        assertEquals("Page Switching", groupState.groups.last().name)
    }

    @Test
    fun keyframesRenderAnimationPreservesEntriesEvenWithoutMappedDevices() {
        val entry = dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesEntry(
            x = 4,
            y = 8,
            r = 1f,
            g = 1f,
            b = 1f,
            launchpadId = "unmapped-uuid",
            localX = 4,
            localY = 8,
        )
        val frame = dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.Frame(
            timing = dev.anthonyhfm.amethyst.core.util.Timing.Rythm(dev.anthonyhfm.amethyst.core.util.Timing.Rythm.RythmTiming._1_16),
            entries = listOf(entry),
        )
        val kf = dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDevice().apply {
            state.value = dev.anthonyhfm.amethyst.devices.effects.keyframes.KeyframesChainDeviceContract.KeyframesChainDeviceState(
                frames = listOf(frame),
            )
            renderAnimation()
        }

        val rendered = kf.state.value.renderedAnimation
        val nonEmpties = rendered.filter { it.second.isNotEmpty() }
        kotlin.test.assertTrue(nonEmpties.isNotEmpty(), "Rendered animation must have non-empty signals")
        val signal = nonEmpties.first().second.first() as Signal.LED
        assertEquals(4, signal.x)
        assertEquals(8, signal.y)
        assertEquals(Color.White, signal.color)
    }

}

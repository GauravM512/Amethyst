@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package dev.anthonyhfm.amethyst.workspace

import dev.anthonyhfm.amethyst.core.controls.undo.UndoManager
import dev.anthonyhfm.amethyst.core.util.AmethystProtoBuf
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDevice
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDeviceState
import dev.anthonyhfm.amethyst.devices.audio.sample.resolvedRawData
import dev.anthonyhfm.amethyst.devices.effects.group.data.Group
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDevice
import dev.anthonyhfm.amethyst.devices.effects.multi.MultiGroupChainDeviceState
import dev.anthonyhfm.amethyst.timeline.TimelineRepository
import dev.anthonyhfm.amethyst.timeline.data.AudioEntry
import dev.anthonyhfm.amethyst.timeline.data.AudioSource
import dev.anthonyhfm.amethyst.timeline.data.AudioTimelineTrack
import dev.anthonyhfm.amethyst.timeline.data.StemKind
import dev.anthonyhfm.amethyst.timeline.data.StemMetadata
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository.AudioSourceRemovalResult
import dev.anthonyhfm.amethyst.workspace.audio.AudioLibraryRepository
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioLibraryUnlinkTest {
    private val source = AudioSource(
        id = "original",
        fileName = "original.wav",
        rawData = ByteArray(size = 600) { it.toByte() },
        sampleRate = 1_000,
        channels = 2,
        bitDepth = 24,
    )

    @BeforeTest
    @AfterTest
    fun resetWorkspace() {
        TimelineRepository.updateTracksSnapshot(updatedTracks = emptyList())
        WorkspaceRepository.samplingChain.restoreDevices(restored = emptyList())
        AudioLibraryRepository.clear()
        UndoManager.clear()
    }

    @Test
    fun removalRequiresTheSecondaryOptionAndPreservesExactUsedPcm() {
        val device = installUsages()
        val blocked = WorkspaceRepository.removeAudioSource(sourceId = source.id)
        assertIs<AudioSourceRemovalResult.InUse>(value = blocked)
        assertEquals(expected = 1, actual = blocked.timelineClipCount)
        assertEquals(expected = 1, actual = blocked.sampleDeviceCount)
        assertEquals(expected = source.id, actual = currentEntry().sourceId)
        assertEquals(expected = listOf(source), actual = AudioLibraryRepository.all())

        assertEquals(
            expected = AudioSourceRemovalResult.Removed,
            actual = WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true),
        )

        val entry = currentEntry()
        val retained = assertNotNull(actual = entry.source())
        assertNull(actual = AudioLibraryRepository.get(id = source.id))
        assertTrue(actual = AudioLibraryRepository.all().isEmpty())
        assertTrue(actual = AudioLibraryRepository.sourceOrder.value.isEmpty())
        assertFalse(actual = retained.isLibraryAsset)
        assertEquals(expected = 0L, actual = entry.clipStartSample)
        assertEquals(expected = 40L, actual = entry.clipEndSample)
        assertEquals(expected = 40_000L, actual = entry.durationUs)
        assertContentEquals(
            expected = source.rawData.copyOfRange(fromIndex = 120, toIndex = 360),
            actual = retained.rawData,
        )
        assertEquals(expected = retained.id, actual = device.state.value.sourceId)
        assertContentEquals(expected = retained.rawData, actual = device.state.value.resolvedRawData())
        assertEquals(expected = 0L, actual = device.state.value.sourceStartFrame)
        assertEquals(expected = 40L, actual = device.state.value.sourceEndFrameExclusive)
        assertEquals(expected = 0.5f, actual = device.state.value.loopStartPosition!!, absoluteTolerance = 0.00001f)
        assertEquals(expected = 0.75f, actual = device.state.value.loopEndPosition!!, absoluteTolerance = 0.00001f)
        assertEquals(expected = -6f, actual = device.state.value.volumeDb)
        assertEquals(expected = 3f, actual = device.state.value.transposeSemitones)
        assertTrue(actual = device.state.value.isMuted)
        assertTrue(actual = device.state.value.isCollapsed)
    }

    @Test
    fun undoAndRedoRestoreReferencesPcmAndLibraryOrderTogether() {
        val device = installUsages()
        val originalState = device.state.value
        val other = source.copy(id = "other", rawData = byteArrayOf(1, 2, 3, 4, 5, 6))
        AudioLibraryRepository.add(source = other)
        WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true)
        val retainedId = currentEntry().sourceId

        UndoManager.undo()

        assertEquals(expected = listOf(source.id, other.id), actual = AudioLibraryRepository.sourceOrder.value)
        assertEquals(expected = source.id, actual = currentEntry().sourceId)
        assertEquals(expected = 20L, actual = currentEntry().clipStartSample)
        assertEquals(expected = originalState, actual = device.state.value)
        assertNull(actual = AudioLibraryRepository.get(id = retainedId))
        assertContentEquals(expected = source.rawData, actual = currentEntry().source()!!.rawData)

        UndoManager.redo()

        assertEquals(expected = listOf(other.id), actual = AudioLibraryRepository.sourceOrder.value)
        assertEquals(expected = retainedId, actual = currentEntry().sourceId)
        assertEquals(expected = retainedId, actual = device.state.value.sourceId)
        assertNull(actual = AudioLibraryRepository.get(id = source.id))
        assertEquals(expected = 240, actual = currentEntry().source()!!.rawData.size)
    }

    @Test
    fun nestedSampleDevicesKeepTheirOwnDistinctRegion() {
        val device = installUsages()
        device.state.value = device.state.value.copy(
            sourceStartFrame = 70L,
            sourceEndFrameExclusive = 90L,
            startPosition = 0.7f,
            endPosition = 0.9f,
            loopStartPosition = null,
            loopEndPosition = null,
        )
        WorkspaceRepository.samplingChain.restoreDevices(restored = emptyList())
        val group = Group(name = "Nested samples").apply {
            chain.restoreDevices(restored = listOf(device))
        }
        val container = MultiGroupChainDevice().apply {
            state.value = MultiGroupChainDeviceState(groups = listOf(group))
        }
        WorkspaceRepository.samplingChain.restoreDevices(restored = listOf(container))

        val usage = WorkspaceRepository.removeAudioSource(sourceId = source.id)
        assertIs<AudioSourceRemovalResult.InUse>(value = usage)
        assertEquals(expected = 1, actual = usage.sampleDeviceCount)
        WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true)

        assertEquals(expected = 2, actual = WorkspaceRepository.saveWorkspace().audioSources.size)
        assertContentEquals(
            expected = source.rawData.copyOfRange(fromIndex = 420, toIndex = 540),
            actual = device.state.value.resolvedRawData(),
        )
        assertEquals(expected = 20L, actual = device.state.value.sourceEndFrameExclusive)
        assertEquals(expected = 40L, actual = currentEntry().clipEndSample)

        UndoManager.undo()
        assertEquals(expected = source.id, actual = device.state.value.sourceId)
        assertEquals(expected = 70L, actual = device.state.value.sourceStartFrame)
    }

    @Test
    fun sourcesWithoutTheNewVisibilityFieldStillLoadAsLibraryAssets() {
        val bytes = AmethystProtoBuf.encodeToByteArray(serializer = AudioSource.serializer(), value = source)
        val decoded = AmethystProtoBuf.decodeFromByteArray(deserializer = AudioSource.serializer(), bytes = bytes)
        AudioLibraryRepository.load(sources = listOf(decoded))

        assertTrue(actual = decoded.isLibraryAsset)
        assertEquals(expected = listOf(source.id), actual = AudioLibraryRepository.sourceOrder.value)
    }

    @Test
    fun savingAndReloadingKeepsRetainedPcmHiddenAndResolvable() {
        installUsages()
        WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true)
        val saved = WorkspaceRepository.saveWorkspace()
        val decoded = AmethystProtoBuf.decodeFromByteArray(
            deserializer = SavableWorkspaceData.serializer(),
            bytes = AmethystProtoBuf.encodeToByteArray(
                serializer = SavableWorkspaceData.serializer(),
                value = saved,
            ),
        )

        AudioLibraryRepository.load(sources = decoded.audioSources)
        TimelineRepository.updateTracksSnapshot(updatedTracks = decoded.timelineData)
        val sample = assertIs<SampleChainDeviceState>(value = decoded.sampling.devices.single())

        assertTrue(actual = AudioLibraryRepository.all().isEmpty())
        assertFalse(actual = decoded.audioSources.single().isLibraryAsset)
        assertEquals(expected = 240, actual = decoded.audioSources.single().rawData.size)
        assertContentEquals(expected = currentEntry().source()!!.rawData, actual = sample.resolvedRawData())
        assertEquals(expected = currentEntry().sourceId, actual = sample.sourceId)
    }

    @Test
    fun reimportingDoesNotReuseAnInvisibleInstanceAndUnusedInstancesAreNotSaved() {
        installUsages()
        WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true)
        val retained = currentEntry().source()!!
        val imported = retained.copy(id = "imported", isLibraryAsset = true)

        assertEquals(expected = imported.id, actual = AudioLibraryRepository.add(source = imported).id)
        assertEquals(expected = listOf(imported.id), actual = AudioLibraryRepository.sourceOrder.value)

        TimelineRepository.updateTracksSnapshot(updatedTracks = emptyList())
        WorkspaceRepository.samplingChain.restoreDevices(restored = emptyList())
        assertEquals(expected = listOf(imported.id), actual = WorkspaceRepository.saveWorkspace().audioSources.map { it.id })
    }

    @Test
    fun removingAStemGroupRetainsOnlyReferencedChildRegions() {
        AudioLibraryRepository.load(sources = listOf(source))
        val stems = StemKind.entries.mapIndexed { index, kind ->
            source.copy(
                id = "stem-$index",
                rawData = ByteArray(size = 600) { (it + index + 1).toByte() },
                stemMetadata = StemMetadata(parentSourceId = source.id, kind = kind, modelId = "test"),
            )
        }
        AudioLibraryRepository.addStemGroup(parentSourceId = source.id, stems = stems)
        val track = AudioTimelineTrack().apply {
            entries[0L] = entry(sourceId = stems[1].id)
            entries[100L] = entry(sourceId = stems[2].id).copy(startTimeMs = 100L, startTimeUs = 100_000L)
        }
        TimelineRepository.updateTracksSnapshot(updatedTracks = listOf(track))

        WorkspaceRepository.removeAudioSource(sourceId = source.id, unlinkInstances = true)

        val saved = WorkspaceRepository.saveWorkspace()
        assertTrue(actual = AudioLibraryRepository.all().isEmpty())
        assertEquals(expected = 2, actual = saved.audioSources.size)
        assertTrue(actual = saved.audioSources.all { !it.isLibraryAsset && it.stemMetadata == null })
        assertContentEquals(
            expected = stems[1].rawData.copyOfRange(fromIndex = 120, toIndex = 360),
            actual = currentEntry().source()!!.rawData,
        )
        UndoManager.undo()
        assertEquals(expected = listOf(source.id) + stems.map { it.id }, actual = AudioLibraryRepository.sourceOrder.value)
        assertEquals(expected = stems[1].id, actual = currentEntry().sourceId)
    }

    private fun installUsages(): SampleChainDevice {
        AudioLibraryRepository.load(sources = listOf(source))
        val track = AudioTimelineTrack().apply { entries[0L] = entry(sourceId = source.id) }
        TimelineRepository.updateTracksSnapshot(updatedTracks = listOf(track))
        val device = SampleChainDevice().apply {
            state.value = SampleChainDeviceState(
                sourceId = source.id,
                fileName = source.fileName,
                sampleRate = source.sampleRate,
                channels = source.channels,
                bitDepth = source.bitDepth,
                isLoaded = true,
                totalDurationMs = source.totalDurationMs,
                startPosition = 0.2f,
                endPosition = 0.6f,
                sourceStartFrame = 20L,
                sourceEndFrameExclusive = 60L,
                loopStartPosition = 0.4f,
                loopEndPosition = 0.5f,
                volumeDb = -6f,
                transposeSemitones = 3f,
            ).apply {
                isMuted = true
                isCollapsed = true
            }
        }
        WorkspaceRepository.samplingChain.restoreDevices(restored = listOf(device))
        return device
    }

    private fun entry(sourceId: String): AudioEntry = AudioEntry(
        startTimeMs = 0L,
        durationMs = 40L,
        fileName = source.fileName,
        sourceId = sourceId,
        clipStartSample = 20L,
        clipEndSample = 60L,
        sampleRate = source.sampleRate,
        channels = source.channels,
        bitDepth = source.bitDepth,
    )

    private fun currentEntry(): AudioEntry =
        (TimelineRepository.tracks.value.first() as AudioTimelineTrack).entries.getValue(key = 0L)
}

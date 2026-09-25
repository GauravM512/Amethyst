package dev.anthonyhfm.amethyst.conversion.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.data.Ableton
import dev.anthonyhfm.amethyst.conversion.ableton.data.LiveSetData
import dev.anthonyhfm.amethyst.conversion.ableton.data.MasterTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.DeviceChain
import dev.anthonyhfm.amethyst.conversion.ableton.data.MidiTrack
import dev.anthonyhfm.amethyst.conversion.ableton.data.FileRef
import dev.anthonyhfm.amethyst.conversion.ableton.data.OriginalSimpler
import dev.anthonyhfm.amethyst.conversion.ableton.data.Tracks
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiPitcher
import dev.anthonyhfm.amethyst.conversion.ableton.data.devices.MidiVelocity
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonManual
import dev.anthonyhfm.amethyst.conversion.ableton.data.utils.AbletonOn
import dev.anthonyhfm.amethyst.core.util.Zip
import dev.anthonyhfm.amethyst.devices.ableton.AbletonPitcherChainDeviceState
import dev.anthonyhfm.amethyst.devices.audio.sample.SampleChainDeviceState
import dev.anthonyhfm.amethyst.devices.effects.color.ColorChainDeviceState
import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs

class AbletonFileImportTest {
    private fun set(tempo: Double = 123.0, tracks: List<MidiTrack> = emptyList()) = Ableton(
        majorVersion = 5, minorVersion = "12.0", creator = "fixture", revision = "1",
        liveSet = LiveSetData(
            tracks = Tracks(tracks),
            masterTrack = MasterTrack(MasterTrack.DeviceChain(
                mixer = MasterTrack.Mixer(MasterTrack.Mixer.Tempo(AbletonManual(tempo))),
            )),
        ),
    )

    private fun als(tempo: Double = 123.0, tracks: List<MidiTrack> = emptyList()): ByteArray =
        Zip.encode(AbletonConverter.xml.encodeToString(Ableton.serializer(), set(tempo, tracks)).encodeToByteArray())

    private fun lightTrack() = MidiTrack(
        id = 7,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName("Lights")),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(
                DeviceChain.DeviceChain.Devices(listOf(
                    MidiPitcher(id = 42, on = AbletonOn(AbletonManual(false)), pitch = MidiPitcher.Pitch(AbletonManual(5))),
                )),
            ),
        ),
    )

    private fun sampleTrack() = MidiTrack(
        id = 8,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName("Samples")),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(DeviceChain.DeviceChain.Devices(listOf(
                OriginalSimpler(
                    player = OriginalSimpler.Player(OriginalSimpler.Player.MultiSampleMap(
                        OriginalSimpler.Player.MultiSampleMap.SampleParts(
                            OriginalSimpler.Player.MultiSampleMap.SampleParts.MultiSamplePart(
                                sampleRef = OriginalSimpler.Player.MultiSampleMap.SampleParts.MultiSamplePart.SampleRef(
                                    FileRef(
                                        relativePath = FileRef.RelativePath(items = listOf(FileRef.RelativePath.RelativePathElement("Samples"))),
                                        name = FileRef.Name("kick.wav"),
                                        type = FileRef.Type(1),
                                    ),
                                ),
                                sampleStart = OriginalSimpler.Player.MultiSampleMap.SampleParts.MultiSamplePart.SampleStart(0),
                                sampleEnd = OriginalSimpler.Player.MultiSampleMap.SampleParts.MultiSamplePart.SampleEnd(441),
                            ),
                        ),
                    )),
                    volumeAndPan = OriginalSimpler.VolumeAndPan(),
                ),
            ))),
        ),
    )

    private fun velocityTrack() = MidiTrack(
        id = 9,
        _name = MidiTrack.Name(MidiTrack.Name.EffectiveName("Colors")),
        deviceChain = DeviceChain(
            deviceChain = DeviceChain.DeviceChain(DeviceChain.DeviceChain.Devices(listOf(
                MidiVelocity(id = 9, maxOut = MidiVelocity.MaxOut(AbletonManual(1))),
            ))),
        ),
    )

    private fun shortWav(): ByteArray {
        val pcm = ByteArray(441 * 2)
        return ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".encodeToByteArray()); putInt(36 + pcm.size); put("WAVE".encodeToByteArray())
            put("fmt ".encodeToByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(44100); putInt(88200); putShort(2); putShort(16)
            put("data".encodeToByteArray()); putInt(pcm.size); put(pcm)
        }.array()
    }

    @Test
    fun gzippedAlsDecodesAndConvertsFromFile() {
        val file = Files.createTempFile("ableton-set-", ".als")
        try {
            Files.write(file, als())
            val decoded = AbletonConverter.decodeAbletonAls(Files.readAllBytes(file))
            assertEquals(123.0, decoded.liveSet.masterTrack.deviceChain.mixer.tempo.manual.value)
            val result = AbletonConverter.convertToWorkspace(PlatformFile(file.toString()), null, reporter = null)
            assertEquals(123.0, result.settings.bpm)
            assertTrue(result.title.startsWith("ableton-set-"))
            assertEquals(1, result.launchpadDevices.size)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun legacyMainTrackAndUnknownXmlNodesStillDecode() {
        val xml = AbletonConverter.xml.encodeToString(Ableton.serializer(), set())
            .replace("MasterTrack", "MainTrack")
            .replace("</LiveSet>", "<UnknownFutureFeature Value=\"1\"/></LiveSet>")
        val decoded = AbletonConverter.decodeAbletonAls(Zip.encode(xml.encodeToByteArray()))
        assertEquals(123.0, decoded.liveSet.masterTrack.deviceChain.mixer.tempo.manual.value)
    }

    @Test
    fun nonEmptyAlsPreservesTrackDeviceSettingsThroughXmlAndConversion() {
        val file = Files.createTempFile("ableton-device-", ".als")
        try {
            Files.write(file, als(tracks = listOf(lightTrack())))
            val decoded = AbletonConverter.decodeAbletonAls(Files.readAllBytes(file))
            assertEquals("Lights", decoded.liveSet.tracks.midiTracks.single().name)
            assertIs<MidiPitcher>(decoded.liveSet.tracks.midiTracks.single().deviceChain.devices.single())
            val result = AbletonConverter.convertToWorkspace(PlatformFile(file.toString()), null, reporter = null)
            val pitcher = assertIs<AbletonPitcherChainDeviceState>(result.lights.devices.single())
            assertEquals(5, pitcher.pitch)
            assertTrue(pitcher.isMuted)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun zipImportSelectsShortestAlsPathAndCleansArchiveState() {
        val file = Files.createTempFile("ableton-archive-", ".zip")
        try {
            ZipOutputStream(Files.newOutputStream(file)).use { zip ->
                mapOf(
                    "Project/Backups/Old.als" to als(99.0),
                    "Project/Current.als" to als(140.0),
                    "Project/Samples/ignore.txt" to "sample".encodeToByteArray(),
                ).forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            val result = AbletonConverter.convertZipToWorkspace(PlatformFile(file.toString()), reporter = null)
            assertEquals("Current", result.title)
            assertEquals(140.0, result.settings.bpm)
            assertFalse(AbletonConverter.isZip)
            assertTrue(AbletonConverter.zipEntries.isEmpty())
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun zipImportDecodesReferencedSampleAndKeepsSourceLinked() {
        val file = Files.createTempFile("ableton-samples-", ".zip")
        try {
            ZipOutputStream(Files.newOutputStream(file)).use { zip ->
                mapOf(
                    "Project/Current.als" to als(tracks = listOf(sampleTrack())),
                    "Project/Samples/kick.wav" to shortWav(),
                ).forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
            val result = AbletonConverter.convertZipToWorkspace(PlatformFile(file.toString()), reporter = null)
            assertEquals(1, result.audioSources.size)
            assertEquals("kick.wav", result.audioSources.single().fileName)
            val sample = assertIs<SampleChainDeviceState>(result.sampling.devices.single())
            assertTrue(sample.isLoaded)
            assertEquals(result.audioSources.single().id, sample.sourceId)
            assertEquals(0L, sample.sourceStartFrame)
            assertEquals(441L, sample.sourceEndFrameExclusive)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun corruptApolloCompanionFallsBackToAbletonConversion() {
        val file = Files.createTempFile("ableton-apollo-", ".zip")
        try {
            ZipOutputStream(Files.newOutputStream(file)).use { zip ->
                mapOf(
                    "Project/Current.als" to als(tracks = listOf(lightTrack())),
                    "Project/Lights.approj" to byteArrayOf(1, 2, 3),
                ).forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
            val result = AbletonConverter.convertZipToWorkspace(PlatformFile(file.toString()), reporter = null)
            assertEquals("Current", result.title)
            assertIs<AbletonPitcherChainDeviceState>(result.lights.devices.single())
            assertFalse(AbletonConverter.isZip)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun emptyCustomPaletteFallsBackToBuiltInColors() {
        val alsFile = Files.createTempFile("ableton-palette-", ".als")
        val paletteFile = Files.createTempFile("ableton-empty-palette-", ".txt")
        try {
            Files.write(alsFile, als(tracks = listOf(velocityTrack())))
            Files.writeString(paletteFile, "invalid palette")
            val result = AbletonConverter.convertToWorkspace(
                PlatformFile(alsFile.toString()), paletteFile.toString(), reporter = null,
            )
            assertIs<ColorChainDeviceState>(result.lights.devices.single())
        } finally {
            Files.deleteIfExists(alsFile)
            Files.deleteIfExists(paletteFile)
        }
    }

    @Test
    fun invalidArchiveFailsAndResetsConversionState() {
        val file = Files.createTempFile("ableton-invalid-", ".zip")
        try {
            Files.write(file, byteArrayOf(1, 2, 3))
            assertFails { AbletonConverter.convertZipToWorkspace(PlatformFile(file.toString()), reporter = null) }
            assertFalse(AbletonConverter.isZip)
            assertTrue(AbletonConverter.zipEntries.isEmpty())
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

package dev.anthonyhfm.amethyst.conversion.ableton.adapters.ableton

import dev.anthonyhfm.amethyst.conversion.ableton.AbletonConverter
import dev.anthonyhfm.amethyst.conversion.ableton.adapters.AbletonAdapter
import dev.anthonyhfm.amethyst.conversion.ableton.utils.XmlElement
import dev.anthonyhfm.amethyst.core.audio.AudioPlayer
import dev.anthonyhfm.amethyst.devices.DeviceState
import dev.anthonyhfm.amethyst.devices.audio.clip.ClipChainDeviceState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.runBlocking

class OriginalSimplerAdapter(
    private val xml: XmlElement
) : AbletonAdapter() {
    override fun toDeviceStates(): List<DeviceState> {
        val player = xml.localQuerySelector("Player")[0]
        val samplePart = player.querySelector("MultiSamplePart").getOrNull(0) ?: return emptyList()
        val sampleRef = samplePart.localQuerySelector("SampleRef")[0]

        val projectPath = AbletonConverter.file!!.parent()!!.path

        val filePath: String = when (AbletonConverter.liveVersion) {
            AbletonConverter.LiveVersion.LIVE_11 -> {
                val relativePath = sampleRef.querySelector("RelativePath")[0].attributes["Value"] ?: ""

                "$projectPath/$relativePath"
            }

            else -> {
                val relativePathElements = sampleRef.localQuerySelector("FileRef")[0]
                    .localQuerySelector("RelativePath").first()

                val fileName = sampleRef.localQuerySelector("FileRef")[0]
                    .localQuerySelector("Name").first()
                    .attributes["Value"] ?: ""

                var pathString = AbletonConverter.file!!.parent()!!.path
                relativePathElements.children.forEach {
                    pathString += "/${it.attributes["Dir"]}"
                }

                "$pathString/$fileName"
            }
        }

        val sampleStart = samplePart.querySelector("SampleStart")[0].attributes["Value"]?.toLong() ?: 0L
        val sampleEnd = samplePart.querySelector("SampleEnd")[0].attributes["Value"]?.toLong() ?: 0L

        val audioFile = PlatformFile(filePath)

        println()

        var clipKey = ""
        runBlocking {
            val clip = AudioPlayer.getAudioClip(audioFile.readBytes(), sampleStart, sampleEnd)
            clipKey = clip!!.key

            AbletonConverter.audioClips.add(clip)
        }

        return listOf(
            ClipChainDeviceState(
                audioKey = clipKey
            )
        )
    }
}
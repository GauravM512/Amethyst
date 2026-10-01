package dev.anthonyhfm.amethyst.ui.dnd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.engine.echo.Echo
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class FileDropNativeHoverTest {
    @Test
    fun nativeHoverProvidesAudioMetadataBeforeDropOnScaledDisplays() {
        val directory = Files.createTempDirectory("amethyst-native-hover").toFile()
        val audioFile = directory.resolve("Überblick + drums.wav")
        val format = AudioFormat(8000f, 16, 1, true, false)
        AudioInputStream(ByteArray(size = 16000).inputStream(), format, 8000L).use { audio ->
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, audioFile)
        }
        var hoverFiles = emptyList<PlatformFile>()
        var hoverOffset: Offset? = null
        var hovering = false
        var dropCount = 0
        val scene = ImageComposeScene(width = 400, height = 400, density = Density(density = 2f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fileDropTarget(
                            onHover = { active, offset, files ->
                                hovering = active
                                hoverOffset = offset
                                hoverFiles = files
                            },
                            onDrop = { _, _ -> dropCount++ }
                        )
                )
            }
        }
        try {
            scene.render(nanoTime = 0L)
            val composeScene = ImageComposeScene::class.java.getDeclaredField("scene").let { field ->
                field.isAccessible = true
                field.get(scene) as ComposeScene
            }
            val bridge = Class.forName("dev.nucleusframework.window.tao.dnd.TaoSceneDnD")
            val instance = bridge.getField("INSTANCE").get(null)
            val enter = bridge.getMethod(
                "onDragEnter",
                ComposeSceneDragAndDropNode::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            )
            val accepted = enter.invoke(
                instance,
                composeScene.rootDragAndDropNode,
                120,
                140,
                arrayOf(audioFile.absolutePath)
            ) as Boolean

            assertTrue(actual = accepted)
            assertTrue(actual = hovering)
            assertEquals(expected = Offset(x = 88f, y = 108f), actual = hoverOffset)
            assertEquals(expected = listOf(audioFile.absolutePath), actual = hoverFiles.map { it.path })
            assertEquals(expected = 0, actual = dropCount)
            val metadata = runBlocking { Echo.probeAudioFile(filePath = hoverFiles.single().path) }
            assertNotNull(actual = metadata)
            assertEquals(expected = 1000L, actual = metadata.durationMs)

            bridge.getMethod("onDragLeave", ComposeSceneDragAndDropNode::class.java)
                .invoke(instance, composeScene.rootDragAndDropNode)
            assertEquals(expected = false, actual = hovering)
            assertTrue(actual = hoverFiles.isEmpty())
        } finally {
            scene.close()
            directory.deleteRecursively()
        }
    }
}

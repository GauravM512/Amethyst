package dev.anthonyhfm.amethyst.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.StringReader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDropTransferableTest {
    @Test
    fun linuxUriListHandlesCommentsUnicodeAndLocalhost() {
        val directory = Files.createTempDirectory("amethyst-drop").toFile()
        try {
            val file = directory.resolve("Überblick + drums.wav").apply { writeBytes(byteArrayOf()) }
            val uri = file.toURI().toASCIIString()
            val text = listOf(
                "# Files",
                uri,
                uri.replace(oldValue = "file:/", newValue = "file://localhost/"),
                "https://example.com/audio.wav",
                "file:///missing.wav"
            ).joinToString(separator = "\r\n")
            val transferable = TestTransferable(
                flavor = DataFlavor("text/uri-list;class=java.io.Reader"),
                value = StringReader(text)
            )

            assertTrue(actual = supportsFileDrop(transferable = transferable))
            assertEquals(
                expected = listOf(file),
                actual = readDroppedFiles(transferable = transferable)
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun windowsFileListPreservesPathsWithoutParsingAsUris() {
        val file = java.io.File("C:\\Users\\Anthony\\Music\\Überblick + drums.wav")
        val transferable = TestTransferable(
            flavor = DataFlavor.javaFileListFlavor,
            value = listOf(file)
        )

        assertEquals(
            expected = listOf(file),
            actual = readDroppedFiles(transferable = transferable)
        )
    }

    @Test
    fun textFallbackAcceptsFilesAndRejectsDirectories() {
        val directory = Files.createTempDirectory("amethyst-drop").toFile()
        try {
            val file = directory.resolve("audio.wav").apply { writeBytes(byteArrayOf()) }
            val transferable = TestTransferable(
                flavor = DataFlavor.stringFlavor,
                value = "${directory.path}\n${file.path}"
            )

            assertEquals(
                expected = listOf(file),
                actual = readDroppedFiles(transferable = transferable)
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun dropCoordinatesUseTheSamePixelsAsLayoutBoundsOnScaledDisplays() {
        assertEquals(
            expected = Offset(x = 300f, y = 40f),
            actual = fileDropLocalOffset(
                positionInRoot = Offset(x = 700f, y = 240f),
                targetBoundsInRoot = Rect(left = 400f, top = 200f, right = 1000f, bottom = 300f)
            )
        )
    }

    private class TestTransferable(
        private val flavor: DataFlavor,
        private val value: Any
    ) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(flavor)

        override fun isDataFlavorSupported(candidate: DataFlavor): Boolean = candidate == flavor

        override fun getTransferData(candidate: DataFlavor): Any {
            if (!isDataFlavorSupported(candidate = candidate)) {
                throw UnsupportedFlavorException(candidate)
            }
            return value
        }
    }
}

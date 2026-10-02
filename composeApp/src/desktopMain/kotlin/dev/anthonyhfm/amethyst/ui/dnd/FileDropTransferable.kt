package dev.anthonyhfm.amethyst.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI

internal fun fileDropLocalOffset(
    positionInRoot: Offset,
    targetBoundsInRoot: Rect
): Offset = positionInRoot - targetBoundsInRoot.topLeft

internal fun supportsFileDrop(transferable: Transferable): Boolean =
    transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
        transferable.isDataFlavorSupported(DataFlavor.stringFlavor) ||
        transferable.transferDataFlavors.any { flavor -> flavor.isMimeTypeEqual("text/uri-list") }

internal fun readDroppedFiles(transferable: Transferable): List<File> {
    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        val files = runCatching {
            (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                ?.filterIsInstance<File>()
        }.getOrNull()
        if (!files.isNullOrEmpty()) {
            return files
        }
    }

    val uriFlavors = transferable.transferDataFlavors.filter { flavor ->
        flavor.isMimeTypeEqual("text/uri-list") && flavor.isFlavorTextType
    }
    for (flavor in uriFlavors) {
        val files = runCatching {
            flavor.getReaderForText(transferable).use { reader ->
                parseDroppedFileText(text = reader.readText(), uriList = true)
            }
        }.getOrDefault(emptyList())
        if (files.isNotEmpty()) {
            return files
        }
    }

    return if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        runCatching {
            parseDroppedFileText(
                text = transferable.getTransferData(DataFlavor.stringFlavor) as String,
                uriList = false
            )
        }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
}

internal fun parseDroppedFileText(
    text: String,
    uriList: Boolean
): List<File> =
    text.lineSequence().mapNotNull { line ->
        val value = line.trim()
        if (value.isEmpty() || (uriList && value.startsWith('#'))) {
            return@mapNotNull null
        }
        runCatching {
            val file = if (value.startsWith("file:", ignoreCase = true)) {
                val uri = URI(value)
                val localUri = if (uri.host.equals("localhost", ignoreCase = true)) {
                    URI(uri.scheme, null, uri.path, null, null)
                } else {
                    uri
                }
                File(localUri)
            } else if (!uriList) {
                File(value)
            } else {
                return@runCatching null
            }
            file.takeIf { it.isFile }
        }.getOrNull()
    }.distinct().toList()

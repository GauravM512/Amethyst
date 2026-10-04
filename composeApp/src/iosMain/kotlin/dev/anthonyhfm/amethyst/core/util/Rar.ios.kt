package dev.anthonyhfm.amethyst.core.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import platform.posix.memset
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.ERAR_END_ARCHIVE
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.ERAR_SUCCESS
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RARCloseArchive
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RARHeaderDataEx
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAROpenArchiveDataEx
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAROpenArchiveEx
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RARProcessFile
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RARReadHeaderEx
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAR_OM_EXTRACT
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAR_OM_LIST
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAR_SKIP
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RAR_TEST
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.RHDF_DIRECTORY
import swiftPMImport.dev.anthonyhfm.amethyst.composeApp.UNRARCALLBACK_MESSAGES

@OptIn(ExperimentalForeignApi::class)
actual object Rar {
    actual fun getEntries(file: PlatformFile): List<ZipEntry> = readArchive(file = file, readData = true)

    actual fun getPaths(file: PlatformFile): List<String> =
        readArchive(file = file, readData = false).map { entry -> entry.path }

    private fun readArchive(
        file: PlatformFile,
        readData: Boolean,
    ): List<ZipEntry> {
        val scoped = file.startAccessingSecurityScopedResource()
        val output = RarEntryBuffer()
        val outputRef = StableRef.create(any = output)

        return try {
            val path = file.nsUrl.path ?: return emptyList()

            memScoped {
                val openData = alloc<RAROpenArchiveDataEx>()
                memset(__b = openData.ptr, __c = 0, __len = sizeOf<RAROpenArchiveDataEx>().toULong())
                openData.ArcName = path.cstr.ptr
                openData.OpenMode = if (readData) {
                    RAR_OM_EXTRACT.toUInt()
                } else {
                    RAR_OM_LIST.toUInt()
                }
                openData.Callback = staticCFunction(function = ::readRarData)
                openData.UserData = outputRef.asCPointer().rawValue.toLong()

                val archive = RAROpenArchiveEx(ArchiveData = openData.ptr)
                    ?: error("Could not open RAR archive: ${openData.OpenResult}")

                try {
                    check(value = openData.OpenResult == ERAR_SUCCESS.toUInt()) {
                        "Could not open RAR archive: ${openData.OpenResult}"
                    }

                    val entries = mutableListOf<ZipEntry>()
                    val header = alloc<RARHeaderDataEx>()
                    memset(__b = header.ptr, __c = 0, __len = sizeOf<RARHeaderDataEx>().toULong())

                    while (true) {
                        val headerResult = RARReadHeaderEx(hArcData = archive, HeaderData = header.ptr)
                        if (headerResult == ERAR_END_ARCHIVE) {
                            break
                        }
                        checkRarResult(result = headerResult)

                        val entryPath = header.readPath()
                        val isDirectory = header.Flags and RHDF_DIRECTORY.toUInt() != 0u
                        val size = if (readData && !isDirectory) {
                            val unpackedSize = (header.UnpSizeHigh.toULong() shl 32) or header.UnpSize.toULong()
                            require(value = unpackedSize <= Int.MAX_VALUE.toULong()) {
                                "RAR entry is too large: $entryPath"
                            }
                            unpackedSize.toInt()
                        } else {
                            0
                        }
                        output.data = ByteArray(size = size)
                        output.offset = 0

                        checkRarResult(
                            result = RARProcessFile(
                                hArcData = archive,
                                Operation = if (readData) {
                                    RAR_TEST
                                } else {
                                    RAR_SKIP
                                },
                                DestPath = null,
                                DestName = null,
                            )
                        )
                        check(value = output.offset == size) {
                            "Incomplete RAR entry: $entryPath"
                        }

                        entries += ZipEntry(
                            path = entryPath,
                            data = output.data,
                            isDirectory = isDirectory,
                        )
                    }

                    entries
                } finally {
                    RARCloseArchive(hArcData = archive)
                }
            }
        } catch (exception: Exception) {
            println("Error reading RAR file: ${exception.message}")
            emptyList()
        } finally {
            outputRef.dispose()
            if (scoped) {
                file.stopAccessingSecurityScopedResource()
            }
        }
    }
}

private class RarEntryBuffer {
    var data: ByteArray = ByteArray(size = 0)
    var offset: Int = 0
}

@OptIn(ExperimentalForeignApi::class)
private fun checkRarResult(result: Int) {
    check(value = result == ERAR_SUCCESS) {
        "Could not read RAR archive: $result"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun RARHeaderDataEx.readPath(): String = buildString {
    for (index in 0 until 1024) {
        val codePoint = FileNameW[index]
        if (codePoint == 0) {
            break
        }
        require(value = codePoint in 0..0x10FFFF) {
            "Invalid RAR filename"
        }
        if (codePoint <= 0xFFFF) {
            append(value = codePoint.toChar())
        } else {
            val supplementary = codePoint - 0x10000
            append(value = ((supplementary shr 10) + 0xD800).toChar())
            append(value = ((supplementary and 0x3FF) + 0xDC00).toChar())
        }
    }
}.replace(oldChar = '\\', newChar = '/')

@OptIn(ExperimentalForeignApi::class)
private fun readRarData(
    message: UInt,
    userData: Long,
    data: Long,
    size: Long,
): Int {
    if (message != UNRARCALLBACK_MESSAGES.UCM_PROCESSDATA.value) {
        return -1
    }

    return try {
        val output = userData.toCPointer<ByteVar>()?.asStableRef<RarEntryBuffer>()?.get() ?: return -1
        if (size < 0 || size > output.data.size.toLong() - output.offset) {
            return -1
        }
        if (size > 0) {
            val source = data.toCPointer<ByteVar>() ?: return -1
            output.data.usePinned { pinned ->
                memcpy(
                    __dst = pinned.addressOf(index = output.offset),
                    __src = source,
                    __n = size.toULong(),
                )
            }
            output.offset += size.toInt()
        }
        1
    } catch (exception: Exception) {
        -1
    }
}

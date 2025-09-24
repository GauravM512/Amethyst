package dev.anthonyhfm.amethyst.conversion.ableton.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.*
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.Foundation.*
import platform.darwin.*

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun PlatformFile.getFileHash(): String {
    val nsString = NSString.stringWithString(this.path)
    val fileData = NSData.dataWithContentsOfFile(nsString)
        ?: return ""

    return memScoped {
        val digest = allocArray<UByteVar>(CC_MD5_DIGEST_LENGTH)

        CC_MD5(fileData.bytes, fileData.length.toUInt(), digest)

        (0 until CC_MD5_DIGEST_LENGTH).joinToString("") {
            digest[it].toString(16).padStart(2, '0')
        }
    }
}
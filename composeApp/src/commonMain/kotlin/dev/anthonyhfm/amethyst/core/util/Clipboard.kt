package dev.anthonyhfm.amethyst.core.util

import dev.anthonyhfm.amethyst.workspace.chain.data.StateChain

object Clipboard {

}

sealed interface ClipboardContent {
    // data class Chain(chain: StateChain) : ClipboardContent
    // data class File(val filePath: String) : ClipboardContent
}
package dev.anthonyhfm.amethyst.ui.contextmenu

import androidx.compose.ui.graphics.vector.ImageVector

data class ContextMenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)
package dev.anthonyhfm.amethyst.ui.modifier

import androidx.compose.ui.Modifier

expect fun Modifier.rightClickable(
    onRightClick: () -> Unit
): Modifier
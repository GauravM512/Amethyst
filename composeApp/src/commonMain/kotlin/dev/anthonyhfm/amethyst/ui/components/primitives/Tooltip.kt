package dev.anthonyhfm.amethyst.ui.components.primitives

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.composeunstyled.LocalContentColor
import com.composeunstyled.RelativeAlignment
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.ui.theme.background
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import kotlinx.coroutines.delay

@Composable
fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    placement: RelativeAlignment = RelativeAlignment.BottomCenter,
    enabled: Boolean = true,
    anchor: @Composable () -> Unit,
) {
    Tooltip(
        modifier = modifier,
        placement = placement,
        enabled = enabled,
        anchor = anchor,
        content = {
            Text(
                text = text,
                style = Theme[typography][small],
                fontSize = 12.sp,
            )
        },
    )
}

@Composable
fun Tooltip(
    modifier: Modifier = Modifier,
    placement: RelativeAlignment = RelativeAlignment.BottomCenter,
    enabled: Boolean = true,
    anchor: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(hovered, enabled) {
        if (hovered && enabled) {
            delay(400L)
            isVisible = true
        } else {
            isVisible = false
        }
    }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
    ) {
        anchor()
        
        if (isVisible) {
            Popup(
                popupPositionProvider = TooltipPositionProvider(placement, 8),
                onDismissRequest = { isVisible = false }
            ) {
                CompositionLocalProvider(LocalContentColor provides Theme[colors][background]) {
                    Box(
                        modifier = Modifier
                            .background(Theme[colors][foreground], shape = SmallShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private class TooltipPositionProvider(
    private val placement: RelativeAlignment,
    private val offsetPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset {
        var x = 0
        var y = 0
        
        val str = placement.toString()
        val isTop = str.contains("Top", ignoreCase = true)
        val isStart = str.contains("Start", ignoreCase = true)
        val isEnd = str.contains("End", ignoreCase = true)
        
        if (isTop) {
            y = anchorBounds.top - popupContentSize.height - offsetPx
            x = when {
                isStart -> anchorBounds.left
                isEnd -> anchorBounds.right - popupContentSize.width
                else -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            }
        } else if (str.contains("Center", ignoreCase = true) && (isStart || isEnd)) {
            y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
            x = if (isStart) {
                anchorBounds.left - popupContentSize.width - offsetPx
            } else {
                anchorBounds.right + offsetPx
            }
        } else {
            y = anchorBounds.bottom + offsetPx
            x = when {
                isStart -> anchorBounds.left
                isEnd -> anchorBounds.right - popupContentSize.width
                else -> anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            }
        }
        
        // Constrain to window bounds so the tooltip is not cropped by the window edges
        val minX = 0
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val minY = 0
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        
        return IntOffset(
            x = x.coerceIn(minX, maxX),
            y = y.coerceIn(minY, maxY)
        )
    }
}

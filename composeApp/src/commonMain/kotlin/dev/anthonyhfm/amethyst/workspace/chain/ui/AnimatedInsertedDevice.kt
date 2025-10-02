package dev.anthonyhfm.amethyst.workspace.chain.ui

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AnimatedInsertedDevice(
    id: String,
    content: @Composable () -> Unit,
) {
    val shouldAnimate = remember(id) { DeviceInsertionAnimator.pending.contains(id) }
    if (!shouldAnimate) {
        content()
        return
    }

    var phase by remember(id) { mutableStateOf(0) } // 0: initial, 1: overshoot, 2: settle

    LaunchedEffect(id) { DeviceInsertionAnimator.consume(id) }

    LaunchedEffect(phase) {
        if (phase == 0) {
            phase = 1
        } else if (phase == 1) {
            kotlinx.coroutines.delay(280)
            phase = 2
        }
    }

    val targetScale = when (phase) {
        0 -> 0.60f
        1 -> 1.06f
        else -> 1f
    }
    val targetAlpha = when (phase) {
        0 -> 0f
        else -> 1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = when (phase) {
            0 -> tween(1)
            1 -> tween(260, easing = EaseOutBack)
            else -> tween(180, easing = LinearOutSlowInEasing)
        }
    )
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = when (phase) {
            0 -> tween(1)
            1 -> tween(200)
            else -> tween(140)
        }
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) { content() }
}

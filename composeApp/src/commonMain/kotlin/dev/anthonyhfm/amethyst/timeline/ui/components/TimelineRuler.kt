package dev.anthonyhfm.amethyst.timeline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anthonyhfm.amethyst.timeline.TimelineRepository
import dev.anthonyhfm.amethyst.timeline.utils.GridUtils

@Composable
fun TimelineRuler(
    zoomLevel: Float,
    scrollState: ScrollState,
    bpm: Double,
    gridType: GridUtils.GridType,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val rulerHeight = 32.dp
    val backgroundColor = Color(0xFF2B2B2B)
    val textColor = Color(0xFFCCCCCC)
    val majorTickColor = Color(0xFFFFFFFF).copy(alpha = 0.8f)
    val minorTickColor = Color(0xFFFFFFFF).copy(alpha = 0.4f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rulerHeight)
            .background(backgroundColor)
            .pointerInput(zoomLevel) {
                detectTapGestures { offset ->
                    if (zoomLevel > 0f) {
                        val clickX = offset.x
                        val scrollOffsetPx = scrollState.value.toFloat()
                        val timeMs = ((scrollOffsetPx + clickX) / zoomLevel).toLong().coerceAtLeast(0L)
                        TimelineRepository.setPlayheadPosition(timeMs)
                    }
                }
            }
    ) {
        if (zoomLevel <= 0f) return@Canvas

        val intervals = GridUtils.computeWithGridType(zoomLevel, bpm, gridType)
        val intervalMs = intervals.intervalMs
        val majorIntervalMs = intervals.majorIntervalMs

        if (intervalMs <= 0L) return@Canvas

        val scrollOffsetPx = scrollState.value.toFloat()
        val viewportWidthPx = size.width

        val startTimeMsInclusive = (scrollOffsetPx / zoomLevel.toDouble()).toLong().coerceAtLeast(0L)
        val firstGridTimeMs = if (startTimeMsInclusive == 0L) 0L
            else ((startTimeMsInclusive / intervalMs) * intervalMs).coerceAtLeast(0L)
        val endTimeMsExclusive = ((scrollOffsetPx + viewportWidthPx) / zoomLevel.toDouble()).toLong()
            .coerceAtLeast(firstGridTimeMs)

        val beatMs = (60000.0 / bpm).toLong().coerceAtLeast(1L)
        val barMs = beatMs * 4

        var t = firstGridTimeMs
        while (t <= endTimeMsExclusive + intervalMs) {
            val x = (t.toDouble() * zoomLevel.toDouble() - scrollOffsetPx.toDouble()).toFloat()

            if (x > viewportWidthPx + 1f) break
            if (x >= -1f) {
                val isMajor = (t % majorIntervalMs == 0L)
                val tickHeight = if (isMajor) size.height * 0.6f else size.height * 0.3f

                drawLine(
                    color = if (isMajor) majorTickColor else minorTickColor,
                    start = Offset(x, size.height - tickHeight),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            t += intervalMs
        }

        val pixelSpacingForLabels = 40f
        val minLabelInterval = (pixelSpacingForLabels / zoomLevel).toLong().coerceAtLeast(1L)

        val showBars = barMs >= minLabelInterval
        val showBeats = beatMs >= minLabelInterval && beatMs < barMs

        if (showBars) {
            val firstBar = ((startTimeMsInclusive / barMs)).coerceAtLeast(0L)
            val lastBar = ((endTimeMsExclusive / barMs) + 1).coerceAtLeast(firstBar)

            for (barIndex in firstBar..lastBar) {
                val barTimeMs = barIndex * barMs
                val x = (barTimeMs.toDouble() * zoomLevel.toDouble() - scrollOffsetPx.toDouble()).toFloat()

                if (x >= -10f && x <= viewportWidthPx + 10f) {
                    val barNumber = barIndex + 1
                    val label = "$barNumber"

                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(x + 4.dp.toPx(), 4.dp.toPx()),
                        style = TextStyle(
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )

                    if (showBeats) {
                        for (beat in 1..3) {
                            val beatTimeMs = barTimeMs + (beat * beatMs)
                            val beatX = (beatTimeMs.toDouble() * zoomLevel.toDouble() - scrollOffsetPx.toDouble()).toFloat()

                            if (beatX >= -10f && beatX <= viewportWidthPx + 10f) {
                                val beatLabel = "${beat + 1}"

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = beatLabel,
                                    topLeft = Offset(beatX + 4.dp.toPx(), 4.dp.toPx()),
                                    style = TextStyle(
                                        color = textColor.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else if (showBeats) {
            val firstBeat = ((startTimeMsInclusive / beatMs)).coerceAtLeast(0L)
            val lastBeat = ((endTimeMsExclusive / beatMs) + 1).coerceAtLeast(firstBeat)

            for (beatIndex in firstBeat..lastBeat) {
                val beatTimeMs = beatIndex * beatMs
                val x = (beatTimeMs.toDouble() * zoomLevel.toDouble() - scrollOffsetPx.toDouble()).toFloat()

                if (x >= -10f && x <= viewportWidthPx + 10f) {
                    val barNumber = (beatIndex / 4) + 1
                    val beatInBar = (beatIndex % 4) + 1
                    val label = "$barNumber.$beatInBar"

                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(x + 4.dp.toPx(), 4.dp.toPx()),
                        style = TextStyle(
                            color = textColor,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}


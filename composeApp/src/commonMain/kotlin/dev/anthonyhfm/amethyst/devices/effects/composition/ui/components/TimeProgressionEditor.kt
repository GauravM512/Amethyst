package dev.anthonyhfm.amethyst.devices.effects.composition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.devices.effects.composition.automation.CompositionAutomationPoint
import dev.anthonyhfm.amethyst.devices.effects.composition.graph.CompositionNode
import dev.anthonyhfm.amethyst.devices.effects.composition.nodes.LocalNodeChangeCallbacks
import dev.anthonyhfm.amethyst.devices.effects.composition.nodes.TimeProgressionNodeState
import dev.anthonyhfm.amethyst.ui.components.primitives.ContextMenu
import dev.anthonyhfm.amethyst.ui.components.primitives.DefaultShape
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.secondary
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import kotlin.math.abs

@Composable
fun TimeProgressionEditor(
    node: CompositionNode,
    playhead: Float,
    onNodeChange: (CompositionNode) -> Unit,
) {
    val curve = node.state as? TimeProgressionNodeState ?: return
    val changeCallbacks = LocalNodeChangeCallbacks.current
    var selectedPointId by remember(node.id) { mutableStateOf<String?>(null) }
    var dragging by remember(node.id) { mutableStateOf(false) }
    val sorted = curve.points.sortedBy(CompositionAutomationPoint::progress)
    fun setPoints(points: List<CompositionAutomationPoint>, discrete: Boolean = false) {
        if (discrete) {
            changeCallbacks.onStart()
        } else if (!dragging) {
            dragging = true
            changeCallbacks.onStart()
        }

        onNodeChange(
            node.copy(
                state = curve.copy(
                    points = points.sortedBy(CompositionAutomationPoint::progress),
                ),
            ),
        )

        if (discrete) {
            changeCallbacks.onFinish()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(color = Theme[colors][secondary], shape = DefaultShape),
    ) {
        val plotStart = 36.dp
        val plotTop = 8.dp
        val plotEnd = 8.dp
        val plotBottom = 24.dp
        val plotWidth = (maxWidth - plotStart - plotEnd).coerceAtLeast(1.dp)
        val plotHeight = (maxHeight - plotTop - plotBottom).coerceAtLeast(1.dp)
        val density = LocalDensity.current
        val legendColor = Theme[colors][mutedForeground]

        ContextMenu(
            modifier = Modifier
                .fillMaxSize(),
            onRightClick = { position ->
                with(density) {
                    val nearest = sorted.minByOrNull { point ->
                        val center = Offset(
                            x = plotStart.toPx() + point.progress * plotWidth.toPx(),
                            y = plotTop.toPx() + (1f - (point.value + 1f) / 2f) * plotHeight.toPx(),
                        )
                        (center - position).getDistance()
                    }
                    selectedPointId = nearest?.takeIf { point ->
                        val center = Offset(
                            x = plotStart.toPx() + point.progress * plotWidth.toPx(),
                            y = plotTop.toPx() + (1f - (point.value + 1f) / 2f) * plotHeight.toPx(),
                        )
                        (center - position).getDistance() <= 22.dp.toPx()
                    }?.pointId
                }
            },
            trigger = {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AutomationCanvas(
                        points = curve.points,
                        playhead = playhead,
                        selectedPointId = selectedPointId,
                        bipolar = false,
                        onSelect = { selectedPointId = it },
                        onAdd = { progress, value ->
                            if (progress > 0.001f && progress < 0.999f && curve.points.none { abs(it.progress - progress) < 0.002f }) {
                                val point = CompositionAutomationPoint(progress = progress, value = value)
                                setPoints(
                                    points = curve.points + point,
                                    discrete = true,
                                )
                                selectedPointId = point.pointId
                            }
                        },
                        onMove = { id, progress, value ->
                            val index = sorted.indexOfFirst { it.pointId == id }
                            if (index >= 0) {
                                val allowedProgress = when (index) {
                                    0 -> 0f
                                    sorted.lastIndex -> 1f
                                    else -> {
                                        val lower = sorted[index - 1].progress + 0.001f
                                        val upper = sorted[index + 1].progress - 0.001f
                                        if (lower <= upper) {
                                            progress.coerceIn(lower, upper)
                                        } else {
                                            sorted[index].progress
                                        }
                                    }
                                }
                                setPoints(
                                    points = sorted.map { point ->
                                        if (point.pointId == id) {
                                            point.copy(progress = allowedProgress, value = value)
                                        } else {
                                            point
                                        }
                                    },
                                )
                            }
                        },
                        onMoveHandle = { id, incoming, time, value ->
                            setPoints(
                                points = curve.points.map { point ->
                                    if (point.pointId != id) {
                                        point
                                    } else if (incoming) {
                                        point.copy(inHandleTime = time, inHandleValue = value)
                                    } else {
                                        point.copy(outHandleTime = time, outHandleValue = value)
                                    }
                                },
                            )
                        },
                        onDragFinished = {
                            if (dragging) {
                                changeCallbacks.onFinish()
                                dragging = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = plotStart, top = plotTop, end = plotEnd, bottom = plotBottom)
                            .semantics {
                                contentDescription = "Time progression curve: input time to output time"
                            },
                    )

                    listOf(1f, 0.5f, 0f).forEach { value ->
                        Text(
                            text = "${(value * 100).toInt()}%",
                            style = Theme[typography][small],
                            fontSize = 10.sp,
                            color = legendColor,
                            modifier = Modifier
                                .offset(x = 4.dp, y = plotTop + plotHeight * (1f - value) - 6.dp),
                        )
                    }

                    Text(
                        text = "Output time",
                        style = Theme[typography][small],
                        fontSize = 10.sp,
                        color = legendColor,
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .padding(start = plotStart + 8.dp, top = plotTop),
                    )

                    Text(
                        text = "0%",
                        style = Theme[typography][small],
                        fontSize = 10.sp,
                        color = legendColor,
                        modifier = Modifier
                            .align(alignment = Alignment.BottomStart)
                            .padding(start = plotStart, bottom = 4.dp),
                    )

                    Text(
                        text = "Input time →",
                        style = Theme[typography][small],
                        fontSize = 10.sp,
                        color = legendColor,
                        modifier = Modifier
                            .align(alignment = Alignment.BottomCenter)
                            .padding(start = plotStart - plotEnd, bottom = 4.dp),
                    )

                    Text(
                        text = "100%",
                        style = Theme[typography][small],
                        fontSize = 10.sp,
                        color = legendColor,
                        modifier = Modifier
                            .align(alignment = Alignment.BottomEnd)
                            .padding(end = plotEnd, bottom = 4.dp),
                    )
                }
            },
        ) {
            AutomatableContextMenuItem(
                label = "Delete point",
                icon = Lucide.Trash2,
                enabled = sorted.indexOfFirst { it.pointId == selectedPointId } in 1 until sorted.lastIndex,
                onClick = {
                    setPoints(
                        points = sorted.filterNot { it.pointId == selectedPointId },
                        discrete = true,
                    )
                    selectedPointId = null
                },
            )
        }
    }
}

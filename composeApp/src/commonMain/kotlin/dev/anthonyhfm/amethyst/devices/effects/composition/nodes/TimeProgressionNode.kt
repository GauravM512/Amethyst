package dev.anthonyhfm.amethyst.devices.effects.composition.nodes

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TrendingUp
import dev.anthonyhfm.amethyst.devices.effects.composition.EvaluationContext
import dev.anthonyhfm.amethyst.devices.effects.composition.automation.CompositionAutomationPoint
import dev.anthonyhfm.amethyst.devices.effects.composition.automation.segmentValueAt
import dev.anthonyhfm.amethyst.devices.effects.composition.graph.CompositionNode
import dev.anthonyhfm.amethyst.devices.effects.composition.ui.components.TimeProgressionEditor
import kotlinx.serialization.Serializable

@Serializable
data class TimeProgressionNodeState(
    val points: List<CompositionAutomationPoint> = listOf(
        CompositionAutomationPoint(progress = 0f, value = -1f),
        CompositionAutomationPoint(progress = 1f, value = 1f),
    ),
) : CompositionNodeState

object TimeProgressionNode : TransformNode() {
    override val type = "time-progression"
    override val label = "Time Progression"
    override val icon = Lucide.TrendingUp
    override val pickerCategory = CompositionNodePickerCategory.Time
    override val bodyWidth = 480.dp
    override val bodyHeight = 220.dp

    override fun defaultState(): CompositionNodeState = TimeProgressionNodeState()

    override fun inputContext(
        node: CompositionNode,
        context: EvaluationContext,
    ): EvaluationContext {
        val state = node.state as? TimeProgressionNodeState ?: return context
        val ordered = state.points.sortedBy(CompositionAutomationPoint::progress)
        if (ordered.isEmpty()) {
            return context
        }
        val progress = context.progress.coerceIn(0f, 1f)
        val value = when {
            progress <= ordered.first().progress -> ordered.first().value
            progress >= ordered.last().progress -> ordered.last().value
            else -> {
                val endIndex = ordered.indexOfFirst { it.progress >= progress }
                val start = ordered[endIndex - 1]
                val end = ordered[endIndex]
                val fraction = ((progress - start.progress) / (end.progress - start.progress).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                start.segmentValueAt(end, fraction)
            }
        }
        return context.copy(progress = ((value.coerceIn(-1f, 1f) + 1f) / 2f))
    }

    @Composable
    override fun NodeBody(
        node: CompositionNode,
        onNodeChange: (CompositionNode) -> Unit,
    ) {
        TimeProgressionEditor(
            node = node,
            playhead = LocalCompositionPlaybackProgress.current,
            onNodeChange = onNodeChange,
        )
    }
}

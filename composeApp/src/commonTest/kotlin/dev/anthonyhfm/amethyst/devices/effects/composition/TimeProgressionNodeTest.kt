package dev.anthonyhfm.amethyst.devices.effects.composition

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.anthonyhfm.amethyst.devices.effects.composition.automation.CompositionAutomationPoint
import dev.anthonyhfm.amethyst.devices.effects.composition.graph.CompositionNode
import dev.anthonyhfm.amethyst.devices.effects.composition.graph.NodePosition
import dev.anthonyhfm.amethyst.devices.effects.composition.nodes.TimeProgressionNode
import dev.anthonyhfm.amethyst.devices.effects.composition.nodes.TimeProgressionNodeState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeProgressionNodeTest {
    private val bounds = IntOffset(0, 0) to IntSize(8, 8)

    @Test
    fun defaultCurveLeavesProgressUnchanged() {
        val node = CompositionNode(
            type = TimeProgressionNode.type,
            position = NodePosition(x = 0f, y = 0f),
        )
        val context = EvaluationContext(bounds = bounds, outputOrigin = null, progress = 0.25f)

        assertEquals(0.25f, TimeProgressionNode.inputContext(node = node, context = context).progress, 0.001f)
    }

    @Test
    fun curveCanPauseAndReverse() {
        val context = EvaluationContext(bounds = bounds, outputOrigin = null, progress = 0.25f)
        val reversed = CompositionNode(
            type = TimeProgressionNode.type,
            position = NodePosition(x = 0f, y = 0f),
            state = TimeProgressionNodeState(points = listOf(
                CompositionAutomationPoint(progress = 0f, value = 1f),
                CompositionAutomationPoint(progress = 1f, value = -1f),
            )),
        )
        val paused = reversed.copy(state = TimeProgressionNodeState(points = listOf(
            CompositionAutomationPoint(progress = 0f, value = 0f),
            CompositionAutomationPoint(progress = 1f, value = 0f),
        )))

        assertEquals(0.75f, TimeProgressionNode.inputContext(node = reversed, context = context).progress, 0.001f)
        assertEquals(0.5f, TimeProgressionNode.inputContext(node = paused, context = context).progress, 0.001f)
    }

    @Test
    fun curveStateSurvivesSerialization() {
        val state = TimeProgressionNodeState(points = listOf(
            CompositionAutomationPoint(progress = 0f, value = -1f),
            CompositionAutomationPoint(progress = 0.5f, value = 0.5f),
            CompositionAutomationPoint(progress = 1f, value = 1f),
        ))
        val encoded = Json.encodeToString(
            serializer = TimeProgressionNodeState.serializer(),
            value = state,
        )
        val decoded = Json.decodeFromString(
            deserializer = TimeProgressionNodeState.serializer(),
            string = encoded,
        )

        assertEquals(state, decoded)
    }
}

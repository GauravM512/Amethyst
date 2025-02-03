package dev.anthonyhfm.amethyst.editor.trackeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.editor.trackeditor.ui.AddComponentSpacer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrackEditor(
    selectedTrack: Int? = null
) {
    val viewModel = koinViewModel<TrackEditorViewModel>()
    val state: TrackEditorState by viewModel.state.collectAsState()

    LaunchedEffect(selectedTrack) {
        viewModel.selectTrack(selectedTrack)
    }

    Row(
        modifier = Modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .height(280.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.5.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        if (state.trackSelected && state.effects != null) {
            val effects by state.effects!!.collectAsState()

            effects.forEachIndexed { index, effectPlugin ->
                AddComponentSpacer(
                    expanded = false,
                    onAddComponent = {
                        viewModel.onAddEffect(it, index)
                    }
                )

                effectPlugin.Content()
            }

            AddComponentSpacer(
                expanded = true,
                onAddComponent = {
                    viewModel.onAddEffect(it)
                }
            )
        }
    }
}
package dev.anthonyhfm.amethyst.timeline.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.timeline.TimelineViewModel
import dev.anthonyhfm.amethyst.timeline.ui.components.TimelineRuler
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository

@Composable
fun TimelineView(
    viewModel: TimelineViewModel
) {
    val scrollState = rememberScrollState()
    val tracks by viewModel.tracks.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val bpm by WorkspaceRepository.bpm.collectAsState()
    val gridType by WorkspaceRepository.gridType.collectAsState()

    LaunchedEffect(scrollState) {
        viewModel.setScrollState(scrollState)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 200.dp)
        ) {
            TimelineRuler(
                zoomLevel = zoomLevel,
                scrollState = scrollState,
                bpm = bpm,
                gridType = gridType
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            TrackListView(
                tracks = tracks,
                onAddLightsTrack = { viewModel.addMidiTrack() },
                onAddAudioTrack = { viewModel.addAudioTrack() }
            )

            TimelineLaneView(
                viewModel = viewModel,
                scrollState = scrollState,
                selectionViewportRelative = true,
                onDoubleClickLightsLane = { trackIndex, timeMs ->
                    viewModel.onDoubleClickMidiTrack(trackIndex, timeMs)
                }
            )
        }
    }
}

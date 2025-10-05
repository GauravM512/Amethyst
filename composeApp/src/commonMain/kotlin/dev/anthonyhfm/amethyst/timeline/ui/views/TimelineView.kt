package dev.anthonyhfm.amethyst.timeline.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.timeline.TimelineViewModel

@Composable
fun TimelineView(
    viewModel: TimelineViewModel
) {
    val scrollState = rememberScrollState()
    val tracks by viewModel.tracks.collectAsState()

    // Set scroll state in ViewModel
    LaunchedEffect(scrollState) {
        viewModel.setScrollState(scrollState)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TrackListView(
            tracks = tracks
        )

        TimelineLaneView(viewModel, scrollState)
    }
}

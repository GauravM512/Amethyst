package dev.anthonyhfm.amethyst.timeline.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimelineView() {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .fillMaxSize(),

        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TrackListView()

        TimelineLaneView(scrollState)
    }
}
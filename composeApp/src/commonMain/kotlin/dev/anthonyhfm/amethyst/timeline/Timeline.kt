package dev.anthonyhfm.amethyst.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.anthonyhfm.amethyst.timeline.ui.views.TimelineView

@Composable
fun Timeline() {
    val viewModel: TimelineViewModel = viewModel { TimelineViewModel() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            Spacer(
                modifier = Modifier
                    .statusBarsPadding()
                    .height(80.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            TimelineView(viewModel)
        }
    }
}
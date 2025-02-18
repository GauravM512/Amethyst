package dev.anthonyhfm.amethyst.advanced_editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.anthonyhfm.amethyst.advanced_editor.projectsettings.ProjectSettingsPanel
import dev.anthonyhfm.amethyst.advanced_editor.trackeditor.TrackEditor
import dev.anthonyhfm.amethyst.advanced_editor.tracks.Tracks
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdvancedEditor() {
    val viewModel = koinViewModel<AdvancedEditorViewModel>()
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Tracks(
                        selectedTrack = state.selectedTrack,
                        onSelectTrack = {
                            viewModel.selectTrack(it)
                        }
                    )
                }

                ProjectSettingsPanel()
            }

            TrackEditor(
                selectedTrack = state.selectedTrack
            )
        }
    }
}
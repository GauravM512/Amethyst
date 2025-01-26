package dev.anthonyhfm.amethyst.editor.plugins.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.editor.plugins.EffectPlugin
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import dev.anthonyhfm.amethyst.ui.previewdevices.LaunchpadPro
import dev.anthonyhfm.amethyst.ui.previewdevices.PreviewState
import dev.anthonyhfm.amethyst.ui.previewdevices.rememberPreviewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PreviewEffectPlugin : EffectPlugin() {
    override var isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private var previewState: PreviewState? = null

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()

        previewState = rememberPreviewState()

        AmethystPlugin(
            title = "Group",
            enabled = isEnabled.collectAsState().value,
            modifier = Modifier
                .width(230.dp),
            onChangeEnabled = {
                scope.launch {
                    isEnabled.emit(it)
                }
            }
        ) {
            previewState?.let {
                LaunchpadPro(
                    previewState = it,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(0.9f)
                )
            }
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        previewState?.sendToPreview(data = data)

        midiOutput(data)
    }
}
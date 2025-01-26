package dev.anthonyhfm.amethyst.editor.plugins.group

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.anthonyhfm.amethyst.core.midi.data.MidiEffectData
import dev.anthonyhfm.amethyst.editor.plugins.EffectPlugin
import dev.anthonyhfm.amethyst.ui.components.AmethystPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * # The Group Plugin
 *
 * The Group Plugin is very different to the other plugins because of its ability to contain other plugins
 */
class GroupPlugin : EffectPlugin() {
    override var isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()

        Row {
            AmethystPlugin(
                title = "Group",
                enabled = isEnabled.collectAsState().value,
                modifier = Modifier,
                onChangeEnabled = {
                    scope.launch {
                        isEnabled.emit(it)
                    }
                }
            ) {

            }
        }
    }

    override suspend fun passData(data: MidiEffectData) {
        midiOutput(data)
    }
}
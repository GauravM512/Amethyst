package dev.anthonyhfm.amethyst.workspace.ui.components

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.workspace_device_settings_automatic
import amethyst.composeapp.generated.resources.workspace_device_settings_cancel
import amethyst.composeapp.generated.resources.workspace_device_settings_description
import amethyst.composeapp.generated.resources.workspace_device_settings_disconnected
import amethyst.composeapp.generated.resources.workspace_device_settings_midi_device_label
import amethyst.composeapp.generated.resources.workspace_device_settings_no_devices
import amethyst.composeapp.generated.resources.workspace_device_settings_save
import amethyst.composeapp.generated.resources.workspace_device_settings_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.core.midi.AmethystMidiManager
import dev.anthonyhfm.amethyst.workspace.ViewportRepository
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.modes.defaults.LayoutWorkspaceMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AndroidDeviceConfigurationSheet(
    uuid: String,
) {
    val elements by ViewportRepository.devices.collectAsState()
    val element = elements.firstOrNull { it.selectionUUID == uuid }
    val devices by AmethystMidiManager.detectedDevices.collectAsState()
    val mode by WorkspaceRepository.mode.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val savedDeviceId = remember(uuid, element) { element?.savedMidiDeviceId }
    var selectedDeviceId by rememberSaveable(uuid) { mutableStateOf(savedDeviceId) }
    var selectionChanged by rememberSaveable(uuid) { mutableStateOf(false) }

    LaunchedEffect(devices, element) {
        if (!selectionChanged) {
            selectedDeviceId = devices.firstOrNull { it.id == savedDeviceId }?.id
                ?: devices.firstOrNull { it.id == element?.savedInputPortId }?.id
                ?: devices.firstOrNull { it.friendlyName == element?.savedInputPortName }?.id
                ?: savedDeviceId
        }
    }

    LaunchedEffect(element, mode) {
        if (element == null || mode !is LayoutWorkspaceMode) {
            WorkspaceRepository.closeDeviceConfigurator()
        }
    }

    val selectionAvailable = selectedDeviceId == null || devices.any { it.id == selectedDeviceId }
    val dismiss = {
        coroutineScope.launch {
            sheetState.hide()
            WorkspaceRepository.closeDeviceConfigurator()
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = WorkspaceRepository::closeDeviceConfigurator,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .selectableGroup(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                    ) {
                        Text(
                            text = stringResource(resource = Res.string.workspace_device_settings_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )

                        Text(
                            text = stringResource(resource = Res.string.workspace_device_settings_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            text = stringResource(resource = Res.string.workspace_device_settings_midi_device_label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                item {
                    MidiDeviceChoice(
                        name = stringResource(resource = Res.string.workspace_device_settings_automatic),
                        selected = selectedDeviceId == null,
                        onSelect = {
                            selectionChanged = true
                            selectedDeviceId = null
                        },
                    )
                }

                items(
                    items = devices,
                    key = { it.id },
                ) { device ->
                    MidiDeviceChoice(
                        name = device.friendlyName,
                        selected = selectedDeviceId == device.id,
                        onSelect = {
                            selectionChanged = true
                            selectedDeviceId = device.id
                        },
                    )
                }

                if (devices.isEmpty() || !selectionAvailable) {
                    item {
                        Text(
                            text = stringResource(
                                resource = if (!selectionAvailable) {
                                    Res.string.workspace_device_settings_disconnected
                                } else {
                                    Res.string.workspace_device_settings_no_devices
                                }
                            ),
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Button(
                    onClick = {
                        if (WorkspaceRepository.mode.value is LayoutWorkspaceMode) {
                            WorkspaceRepository.changeMidiDeviceConfig(
                                uuid = uuid,
                                deviceId = selectedDeviceId,
                            )
                        }
                        dismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = element != null && mode is LayoutWorkspaceMode && selectionAvailable,
                ) {
                    Text(text = stringResource(resource = Res.string.workspace_device_settings_save))
                }

                TextButton(
                    onClick = dismiss,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(text = stringResource(resource = Res.string.workspace_device_settings_cancel))
                }
            }
        }
    }
}

@Composable
private fun MidiDeviceChoice(
    name: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = name)
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            ),
    )
}

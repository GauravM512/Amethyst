package dev.anthonyhfm.amethyst.workspace.ui.components

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.ui_launchpad_midifighter_black_variant
import amethyst.composeapp.generated.resources.ui_launchpad_midifighter_white_variant
import amethyst.composeapp.generated.resources.ui_primitive_sheet_close
import amethyst.composeapp.generated.resources.workspace_viewport_launchpad_actions_style_dialog_title
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.anthonyhfm.amethyst.ui.components.primitives.ScaleToFit
import dev.anthonyhfm.amethyst.ui.launchpad.viewport.ViewportMidiFighter64
import dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData.SavableViewportLaunchpad.MidiFighter64.MidiFighter64Style
import dev.anthonyhfm.amethyst.workspace.ui.viewport.elements.LaunchpadViewportElement
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AndroidDeviceStyleSheet(
    element: LaunchpadViewportElement,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val dismiss = {
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(resource = Res.string.workspace_viewport_launchpad_actions_style_dialog_title),
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (element is ViewportMidiFighter64) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 144.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .selectableGroup(),
                    contentPadding = PaddingValues(all = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                ) {
                    items(
                        items = MidiFighter64Style.entries,
                        key = { it.name },
                    ) { style ->
                        MidiFighterStyleChoice(
                            style = style,
                            selected = element.style == style,
                            onSelect = { element.selectStyle(style = style) },
                        )
                    }
                }
            } else {
                element.StyleConfigContent(onDismiss = dismiss)
            }

            TextButton(
                onClick = dismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(text = stringResource(resource = Res.string.ui_primitive_sheet_close))
            }
        }
    }
}

@Composable
private fun MidiFighterStyleChoice(
    style: MidiFighter64Style,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val preview = remember(style) {
        ViewportMidiFighter64(
            interactive = false,
            initialStyle = style,
        )
    }

    DisposableEffect(preview) {
        onDispose { preview.close() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            width = if (selected) {
                2.dp
            } else {
                1.dp
            },
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(size = 140.dp)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                ScaleToFit {
                    preview.Content()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                )

                Text(
                    text = stringResource(
                        resource = if (style == MidiFighter64Style.Black) {
                            Res.string.ui_launchpad_midifighter_black_variant
                        } else {
                            Res.string.ui_launchpad_midifighter_white_variant
                        },
                    ),
                    modifier = Modifier
                        .weight(weight = 1f),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

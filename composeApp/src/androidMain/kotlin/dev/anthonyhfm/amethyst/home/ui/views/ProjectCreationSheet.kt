package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import dev.anthonyhfm.amethyst.home.ui.views.ProjectCreationSheetContract.Effect
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreationSheet(
    onDismiss: () -> Unit,
    openWorkspace: () -> Unit,
    projectPath: String? = null,
) {
    val viewModel = viewModel(key = projectPath) {
        ProjectCreationSheetViewModel(projectPath = projectPath)
    }
    val state by viewModel.state.collectAsState()
    val isEditing = projectPath != null

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                Effect.OpenWorkspace -> openWorkspace()
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) {
                            stringResource(Res.string.home_project_creation_sheet_edit_title)
                        } else {
                            stringResource(Res.string.home_project_creation_sheet_new_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = stringResource(Res.string.home_project_creation_close_desc),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Text(
                text = if (isEditing) {
                    stringResource(Res.string.home_project_creation_sheet_edit_desc)
                } else {
                    stringResource(Res.string.home_project_creation_sheet_new_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { name ->
                    viewModel.onEvent(
                        event = ProjectCreationSheetContract.Event.OnChangeName(value = name)
                    )
                },
                label = {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_name_label))
                },
                placeholder = {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_name_placeholder))
                },
                isError = state.name.isNotEmpty() && !state.isNameValid,
                supportingText = if (state.name.isNotEmpty() && !state.isNameValid) {
                    {
                        Text(text = stringResource(Res.string.home_project_creation_sheet_name_error))
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.author,
                onValueChange = { author ->
                    viewModel.onEvent(
                        event = ProjectCreationSheetContract.Event.OnChangeAuthor(value = author)
                    )
                },
                label = {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_author_label))
                },
                placeholder = {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_author_placeholder))
                },
                supportingText = {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_author_desc))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.isNameValid) {
                            viewModel.onEvent(event = ProjectCreationSheetContract.Event.OnClickSubmit)
                        }
                    }
                ),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.End,
                ),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.home_project_creation_sheet_cancel))
                }

                Button(
                    onClick = {
                        viewModel.onEvent(event = ProjectCreationSheetContract.Event.OnClickSubmit)
                    },
                    enabled = state.isNameValid,
                ) {
                    Text(
                        text = if (isEditing) {
                            stringResource(Res.string.home_project_creation_sheet_save)
                        } else {
                            stringResource(Res.string.home_project_creation_sheet_create)
                        }
                    )
                }
            }
        }
    }
}

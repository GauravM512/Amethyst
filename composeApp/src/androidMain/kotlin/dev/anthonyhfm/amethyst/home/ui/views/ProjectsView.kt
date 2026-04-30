package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.ui.views.ProjectsViewContract.Event
import dev.anthonyhfm.amethyst.workspace.data.RecentWorkspace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsView(
    navigator: NavHostController,
    onOpenWorkspace: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel = viewModel {
        ProjectsViewModel(
            navigator = navigator,
            snackbarHostState = snackbarHostState,
        )
    }

    var recentProjects by remember { mutableStateOf(HomeRepository.recentWorkspaces()) }

    // Bottom sheet state
    var showCreateSheet by remember { mutableStateOf(false) }
    var editProjectPath by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentBackStackEntry by produceState<NavBackStackEntry?>(
        initialValue = navigator.currentBackStackEntry,
    ) {
        navigator.currentBackStackEntryFlow.collect { value = it }
    }

    LaunchedEffect(currentBackStackEntry) {
        recentProjects = HomeRepository.recentWorkspaces()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProjectsViewContract.Effect.OpenWorkspace -> onOpenWorkspace()
                ProjectsViewContract.Effect.ShowCreateSheet -> showCreateSheet = true
                is ProjectsViewContract.Effect.ShowEditSheet -> editProjectPath = effect.projectPath
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ProjectCreationSheet(
                onDismiss = { showCreateSheet = false },
                openWorkspace = onOpenWorkspace,
                projectPath = null,
            )
        }
    }

    editProjectPath?.let { path ->
        ModalBottomSheet(
            onDismissRequest = { editProjectPath = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ProjectCreationSheet(
                onDismiss = { editProjectPath = null },
                openWorkspace = {
                    editProjectPath = null
                    recentProjects = HomeRepository.recentWorkspaces()
                },
                projectPath = path,
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Recent Projects") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(Event.OnClickOpenProject) }) {
                        Icon(
                            imageVector = Lucide.FolderOpen,
                            contentDescription = "Open Project",
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(Event.OnClickNewProject) }) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "New Project",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp),
            )
        },
    ) { innerPadding ->
        if (recentProjects.isEmpty()) {
            EmptyProjectsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                onOpenProject = { viewModel.onEvent(Event.OnClickOpenProject) },
                onNewProject = { viewModel.onEvent(Event.OnClickNewProject) },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recentProjects, key = { it.path }) { project ->
                    RecentProjectItem(
                        project = project,
                        onOpen = { viewModel.onEvent(Event.OpenProjectFromHistory(project)) },
                        onEdit = { viewModel.onEvent(Event.OnClickEditProject(project)) },
                        onDelete = {
                            viewModel.onEvent(Event.OnClickDeleteProject(project.path))
                            recentProjects = HomeRepository.recentWorkspaces()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectsState(
    modifier: Modifier = Modifier,
    onOpenProject: () -> Unit,
    onNewProject: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Icon(
                imageVector = Lucide.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Recent Projects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Open an existing workspace or create a new project to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNewProject,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
                Text("New Project")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenProject,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Lucide.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
                Text("Open File")
            }
        }
    }
}

@Composable
private fun RecentProjectItem(
    project: RecentWorkspace,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(project.path) { mutableStateOf(false) }
    val folderLabel = remember(project.path) { displayFolderPath(project.path) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = folderLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Project options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        leadingIcon = { Icon(Lucide.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        leadingIcon = { Icon(Lucide.Pencil, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Remove from recent",
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

private fun displayFolderPath(path: String): String {
    val normalized = path.replace('\\', '/')
    val sep = normalized.lastIndexOf('/')
    if (sep <= 0) return normalized
    val parent = normalized.substring(0, sep).trimEnd('/')
    return abbreviateHomePrefix(parent)
}

private fun abbreviateHomePrefix(path: String): String {
    listOf("/Users/", "/home/").forEach { prefix ->
        if (path.startsWith(prefix)) {
            val next = path.indexOf('/', prefix.length)
            return if (next == -1) "~" else "~${path.substring(next)}"
        }
    }
    return path
}

package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
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
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.UserRound
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.data.AndroidLocalProjectDeletion
import dev.anthonyhfm.amethyst.home.nav.HomeNavRoute
import dev.anthonyhfm.amethyst.hub.data.HubProject
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
    val context = LocalContext.current
    val account = remember(context) { AndroidHubAccount.get(context) }
    val hubProjects = remember { mutableStateMapOf<String, HubProject>() }
    val localAuthors = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(recentProjects) {
        recentProjects.forEach { recent ->
            val record = HomeRepository.mobileProjectForPath(recent.path)
            if (record?.hubProjectId != null && record.hubProjectId !in hubProjects) {
                runCatching {
                    account.repository.browseProjects.execute(limit = 50, query = recent.title)
                        .items.firstOrNull { it.id == record.hubProjectId }
                }.getOrNull()?.let { hubProjects[record.hubProjectId] = it }
            } else if (record?.hubProjectId == null && recent.path.endsWith(".ame", ignoreCase = true) && recent.path !in localAuthors) {
                HomeRepository.loadProjectDetails(recent.path)?.author?.trim()?.takeIf { it.isNotEmpty() }
                    ?.let { localAuthors[recent.path] = it }
            }
        }
    }

    // Bottom sheet state
    var showCreateSheet by remember { mutableStateOf(false) }
    var editProjectPath by remember { mutableStateOf<String?>(null) }
    var projectToDelete by remember { mutableStateOf<RecentWorkspace?>(null) }
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
                ProjectsViewContract.Effect.ProjectDeleted -> recentProjects = HomeRepository.recentWorkspaces()
                ProjectsViewContract.Effect.ShowCreateSheet -> showCreateSheet = true
                is ProjectsViewContract.Effect.ShowEditSheet -> editProjectPath = effect.projectPath
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text(stringResource(Res.string.home_projects_delete_local)) },
            text = { Text(stringResource(Res.string.home_projects_delete_local_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    projectToDelete = null
                    viewModel.onEvent(Event.OnClickDeleteProject(project.path))
                }) {
                    Text(stringResource(Res.string.home_projects_delete_local), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

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
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.home_projects_title)) },
                actions = {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.onEvent(Event.OnClickOpenProject) }) {
                                Icon(
                                    imageVector = Lucide.FolderOpen,
                                    contentDescription = stringResource(Res.string.home_projects_action_open_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { viewModel.onEvent(Event.OnClickNewProject) }) {
                                Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = stringResource(Res.string.home_project_creation_sheet_new_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
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
            val localProjects = recentProjects.filter { HomeRepository.mobileProjectForPath(it.path)?.hubProjectId == null }
            val downloadedProjects = recentProjects.filter { HomeRepository.mobileProjectForPath(it.path)?.hubProjectId != null }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (localProjects.isNotEmpty()) item(key = "local_heading") {
                    Text(
                        stringResource(Res.string.home_projects_local_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(localProjects, key = { it.path }) { project ->
                    RecentProjectItem(
                        project = project,
                        showDivider = project.path != localProjects.last().path,
                        hubProject = null,
                        isHubDownload = false,
                        author = localAuthors[project.path],
                        onOpen = { viewModel.onEvent(Event.OpenProjectFromHistory(project)) },
                        onViewHub = null,
                        onViewArtist = null,
                        onEdit = if (HomeRepository.mobileProjectForPath(project.path) == null && project.path.endsWith(".ame", ignoreCase = true))
                            ({ viewModel.onEvent(Event.OnClickEditProject(project)) }) else null,
                        onDelete = if (AndroidLocalProjectDeletion.targetFor(project.path) != null)
                            ({ projectToDelete = project }) else null,
                    )
                }
                if (downloadedProjects.isNotEmpty()) item(key = "downloaded_heading") {
                    Text(
                        stringResource(Res.string.home_projects_downloaded_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(downloadedProjects, key = { it.path }) { project ->
                    val hub = HomeRepository.mobileProjectForPath(project.path)?.hubProjectId?.let(hubProjects::get)
                    RecentProjectItem(
                        project = project,
                        showDivider = project.path != downloadedProjects.last().path,
                        hubProject = hub,
                        isHubDownload = true,
                        author = null,
                        onOpen = { viewModel.onEvent(Event.OpenProjectFromHistory(project)) },
                        onViewHub = hub?.let { { navigator.navigate(HomeNavRoute.HubDetail(it.artist.username, it.slug)) } },
                        onViewArtist = hub?.let { { navigator.navigate(HomeNavRoute.HubDetail(it.artist.username, null)) } },
                        onEdit = null,
                        onDelete = if (AndroidLocalProjectDeletion.targetFor(project.path) != null)
                            ({ projectToDelete = project }) else null,
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
                text = stringResource(Res.string.home_projects_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.home_projects_empty_desc),
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
                Text(stringResource(Res.string.home_project_creation_sheet_new_title))
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
                Text(stringResource(Res.string.home_projects_open_button))
            }
        }
    }
}

@Composable
private fun RecentProjectItem(
    project: RecentWorkspace,
    showDivider: Boolean,
    hubProject: HubProject?,
    isHubDownload: Boolean,
    author: String?,
    onOpen: () -> Unit,
    onViewHub: (() -> Unit)?,
    onViewArtist: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var menuExpanded by remember(project.path) { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hubProject?.thumbnailUrl != null) {
                HubArtwork(hubProject.thumbnailUrl, Modifier.size(58.dp), MaterialTheme.shapes.medium)
            } else {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hubProject != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HubArtwork(hubProject.artist.avatarUrl, Modifier.size(18.dp), CircleShape, Icons.Default.Person)
                        Text(hubProject.artist.displayName.ifBlank { "@${hubProject.artist.username}" }, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else Text(
                    text = author ?: when {
                        isHubDownload -> stringResource(Res.string.home_projects_recent_downloaded)
                        project.path.endsWith(".ame", ignoreCase = true) -> stringResource(Res.string.home_project_unknown_author)
                        project.path.endsWith(".als", ignoreCase = true) -> stringResource(Res.string.home_hub_catalog_ableton)
                        project.path.endsWith(".approj", ignoreCase = true) -> stringResource(Res.string.home_hub_catalog_apollo)
                        else -> stringResource(Res.string.home_projects_recent_local)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(Res.string.home_projects_item_options_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.home_projects_item_menu_open)) },
                        leadingIcon = { Icon(Lucide.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        },
                    )
                    if (onViewHub != null) DropdownMenuItem(
                        text = { Text(stringResource(Res.string.home_projects_view_hub)) },
                        leadingIcon = { Icon(Lucide.Globe, contentDescription = null) },
                        onClick = { menuExpanded = false; onViewHub() },
                    )
                    if (onViewArtist != null) DropdownMenuItem(
                        text = { Text(stringResource(Res.string.home_projects_view_artist)) },
                        leadingIcon = { Icon(Lucide.UserRound, contentDescription = null) },
                        onClick = { menuExpanded = false; onViewArtist() },
                    )
                    if (onEdit != null) DropdownMenuItem(
                        text = { Text(stringResource(Res.string.home_projects_item_menu_edit)) },
                        leadingIcon = { Icon(Lucide.Pencil, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    if (onDelete != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(Res.string.home_projects_delete_local),
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
        if (showDivider) HorizontalDivider(modifier = Modifier.padding(start = 86.dp), color = MaterialTheme.colorScheme.outlineVariant)
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

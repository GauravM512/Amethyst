package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import com.composeunstyled.Icon
import com.composeunstyled.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Wifi
import dev.anthonyhfm.amethyst.ui.components.primitives.ToastProvider
import dev.anthonyhfm.amethyst.ui.components.primitives.rememberToastState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.composeunstyled.rememberDialogState
import com.composeunstyled.theme.Theme
import dev.anthonyhfm.amethyst.core.network.lan.DiscoveredSession
import dev.anthonyhfm.amethyst.core.network.user.LocalUserRepository
import dev.anthonyhfm.amethyst.home.HomeCommandSurface
import dev.anthonyhfm.amethyst.home.data.HomeRepository
import dev.anthonyhfm.amethyst.home.data.DesktopDownloadedProjectDeletion
import dev.anthonyhfm.amethyst.home.data.DownloadedProjectDetails
import dev.anthonyhfm.amethyst.home.data.downloadedDetails
import dev.anthonyhfm.amethyst.home.account.DesktopHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubProjectDeepLink
import dev.anthonyhfm.amethyst.home.ui.views.RecentViewContract.Event
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialog
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialogCancel
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialogDescription
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialogFooter
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialogHeader
import dev.anthonyhfm.amethyst.ui.components.primitives.AlertDialogTitle
import dev.anthonyhfm.amethyst.ui.components.primitives.Card
import dev.anthonyhfm.amethyst.ui.components.primitives.CardDescription
import dev.anthonyhfm.amethyst.ui.components.primitives.CardHeader
import dev.anthonyhfm.amethyst.ui.components.primitives.CardTitle
import dev.anthonyhfm.amethyst.ui.components.primitives.DefaultShape
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenu
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuContent
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuItem
import dev.anthonyhfm.amethyst.ui.components.primitives.DropdownMenuTrigger
import dev.anthonyhfm.amethyst.ui.components.primitives.Input
import dev.anthonyhfm.amethyst.ui.components.primitives.Progress
import dev.anthonyhfm.amethyst.ui.components.primitives.ScrollArea
import dev.anthonyhfm.amethyst.ui.components.primitives.TypographyH2
import dev.anthonyhfm.amethyst.ui.components.primitives.TypographyLead
import dev.anthonyhfm.amethyst.ui.components.primitives.TypographyMuted
import dev.anthonyhfm.amethyst.ui.theme.background
import dev.anthonyhfm.amethyst.ui.theme.border
import dev.anthonyhfm.amethyst.ui.theme.card
import dev.anthonyhfm.amethyst.ui.theme.cardForeground
import dev.anthonyhfm.amethyst.ui.theme.colors
import dev.anthonyhfm.amethyst.ui.theme.destructive
import dev.anthonyhfm.amethyst.ui.theme.destructiveForeground
import dev.anthonyhfm.amethyst.ui.theme.foreground
import dev.anthonyhfm.amethyst.ui.theme.mutedForeground
import dev.anthonyhfm.amethyst.ui.theme.p
import dev.anthonyhfm.amethyst.ui.theme.popoverForeground
import dev.anthonyhfm.amethyst.ui.theme.primaryForeground
import dev.anthonyhfm.amethyst.ui.theme.small
import dev.anthonyhfm.amethyst.ui.theme.typography
import dev.anthonyhfm.amethyst.workspace.data.RecentWorkspace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

@Composable
fun RecentView(
    navigator: NavHostController,
    onOpenWorkspace: () -> Unit = { },
    onNavigateHub: (DesktopHubDestination) -> Unit = { },
) {
    val toastState = rememberToastState()
    val viewModel = viewModel {
        RecentViewModel(
            navigator = navigator,
            toastState = toastState
        )
    }

    var recentProjects: List<RecentWorkspace> by remember { mutableStateOf(loadRecentProjects()) }
    val localProjects = recentProjects.filter { HomeRepository.mobileProjectForPath(it.path)?.hubProjectId == null }
    val downloadedProjects = recentProjects.filter { HomeRepository.mobileProjectForPath(it.path)?.hubProjectId != null }
    val downloadedDetails = remember { mutableStateMapOf<String, DownloadedProjectDetails>() }
    val repository = remember { DesktopHubAccount.get().repository }
    val deletion = remember { DesktopDownloadedProjectDeletion() }
    val scope = rememberCoroutineScope()
    var projectToRemove by remember { mutableStateOf<RecentWorkspace?>(null) }
    var removingProject by remember { mutableStateOf(false) }
    var joiningSession by remember { mutableStateOf<DiscoveredSession?>(null) }
    val state by viewModel.state.collectAsState()
    val localUser by LocalUserRepository.localUser.collectAsState()

    val currentBackStackEntry by navigator.currentBackStackEntryFlow.collectAsState(initial = navigator.currentBackStackEntry)
    LaunchedEffect(currentBackStackEntry) {
        recentProjects = loadRecentProjects()
    }

    LaunchedEffect(key1 = downloadedProjects) {
        downloadedProjects.forEach { project ->
            val record = HomeRepository.mobileProjectForPath(path = project.path) ?: return@forEach
            val projectId = record.hubProjectId ?: return@forEach
            val cached = record.hubDetails

            if (cached != null) {
                downloadedDetails[project.path] = cached
            } else if (project.path !in downloadedDetails) {
                try {
                    val details = HubProjectDeepLink(projectId = projectId)
                        .resolve(repository = repository)
                        .downloadedDetails()
                    val current = HomeRepository.mobileProjectForPath(path = project.path)

                    if (current?.hubProjectId == projectId) {
                        HomeRepository.registerMobileProject(record = current.copy(hubDetails = details))
                        downloadedDetails[project.path] = details
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                RecentViewContract.Effect.OpenWorkspace -> {
                    println("[RecentView ${System.currentTimeMillis()}] effect OpenWorkspace received; calling onOpenWorkspace")
                    onOpenWorkspace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        HomeCommandSurface.commands.collect { command ->
            when (command) {
                HomeCommandSurface.HomeCommand.NewProject ->
                    viewModel.onEvent(Event.OnClickNewProject)
                HomeCommandSurface.HomeCommand.OpenProject ->
                    viewModel.onEvent(Event.OnClickOpenProject)
            }
        }
    }

    ToastProvider(state = toastState) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 24.dp, end = 12.dp),
            ) {
                ScrollArea(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp, bottom = 16.dp),
                    ) {
                        RecentViewHeader()

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (state.discoveredSessions.isNotEmpty()) {
                                TypographyMuted(stringResource(Res.string.home_recent_collaboration_network_status))
                                state.discoveredSessions.forEach { session ->
                                    DiscoveredSessionCard(
                                        session = session,
                                        onJoin = { joiningSession = session },
                                    )
                                }
                            }

                            if (recentProjects.isEmpty()) {
                                EmptyRecentProjectsCard()
                            } else {
                                if (localProjects.isNotEmpty()) {
                                    RecentProjectsSectionTitle(stringResource(Res.string.home_projects_local_section))
                                }
                                localProjects.forEach { project ->
                                    RecentProjectCard(
                                        project = project,
                                        onOpen = { viewModel.onEvent(Event.OpenProjectFromHistory(project)) },
                                        onEdit = { viewModel.onEvent(Event.OnClickEditProject(project)) },
                                        onDelete = {
                                            HomeRepository.removeRecentWorkspace(project.path)
                                            recentProjects = loadRecentProjects()
                                        },
                                    )
                                }
                                if (downloadedProjects.isNotEmpty()) {
                                    if (localProjects.isNotEmpty()) {
                                        Spacer(
                                            modifier = Modifier
                                                .height(height = 8.dp),
                                        )
                                    }

                                    RecentProjectsSectionTitle(
                                        title = stringResource(resource = Res.string.home_projects_downloaded_section),
                                    )

                                    DownloadedProjectsGrid(
                                        projects = downloadedProjects,
                                        details = downloadedDetails,
                                        onOpen = { viewModel.onEvent(event = Event.OpenProjectFromHistory(project = it)) },
                                        onNavigate = onNavigateHub,
                                        onRemove = { projectToRemove = it },
                                    )
                                }
                            }
                        }
                    }
                }

                RecentActions(
                    onOpenProject = { viewModel.onEvent(Event.OnClickOpenProject) },
                    onCreateProject = { viewModel.onEvent(Event.OnClickNewProject) },
                )
            }

            projectToRemove?.let { project ->
                RemoveDownloadedProjectDialog(
                    project = project,
                    busy = removingProject,
                    onDismiss = { projectToRemove = null },
                    onConfirm = {
                        removingProject = true
                        scope.launch {
                            try {
                                val record = HomeRepository.mobileProjectForPath(path = project.path)
                                val deleted = withContext(context = Dispatchers.IO) {
                                    deletion.delete(path = project.path)
                                }

                                if (deleted) {
                                    record?.convertedPath?.let { path ->
                                        HomeRepository.removeRecentWorkspace(path = path)
                                    }
                                    HomeRepository.removeRecentWorkspace(path = project.path)
                                    downloadedDetails.remove(key = project.path)
                                    recentProjects = loadRecentProjects()
                                }

                                toastState.show(
                                    title = getString(
                                        resource = if (deleted) {
                                            Res.string.home_projects_removed_downloaded
                                        } else {
                                            Res.string.home_projects_remove_downloaded_error
                                        },
                                    ),
                                )
                            } finally {
                                removingProject = false
                                projectToRemove = null
                            }
                        }
                    },
                )
            }

            joiningSession?.let { session ->
                JoinSessionDialog(
                    session = session,
                    initialUserName = localUser.name,
                    onDismiss = { joiningSession = null },
                    onJoin = { userName ->
                        viewModel.onEvent(Event.OnClickJoinSession(session, userName))
                        joiningSession = null
                    },
                )
            }

            if (state.initialSyncProgress.active) {
                InitialSyncProgressDialog(state.initialSyncProgress)
            }
        }
    }
}

@Composable
private fun RemoveDownloadedProjectDialog(
    project: RecentWorkspace,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogState = rememberDialogState(initiallyVisible = true)

    AlertDialog(
        state = dialogState,
        onDismiss = {
            if (!busy) {
                onDismiss()
            }
        },
        modifier = Modifier
            .widthIn(max = 460.dp),
    ) {
        AlertDialogHeader {
            AlertDialogTitle(text = stringResource(resource = Res.string.home_projects_remove_downloaded))
            AlertDialogDescription(text = project.title)
        }

        AlertDialogDescription(text = stringResource(resource = Res.string.home_projects_remove_downloaded_confirm))

        AlertDialogFooter {
            Button(
                onClick = onDismiss,
                variant = ButtonVariant.Outline,
                enabled = !busy,
            ) {
                Text(text = stringResource(resource = Res.string.common_cancel))
            }

            Button(
                onClick = onConfirm,
                variant = ButtonVariant.Destructive,
                enabled = !busy,
            ) {
                Text(
                    text = stringResource(
                        resource = if (busy) {
                            Res.string.home_projects_removing_downloaded
                        } else {
                            Res.string.home_projects_remove_downloaded
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun RecentProjectsSectionTitle(title: String) {
    Text(
        text = title,
        style = Theme[typography][p],
        fontWeight = FontWeight.SemiBold,
        color = Theme[colors][foreground],
    )
}

@Composable
private fun InitialSyncProgressDialog(
    progress: dev.anthonyhfm.amethyst.core.network.CollaborationManager.InitialSyncProgress,
) {
    val dialogState = rememberDialogState(initiallyVisible = true)
    AlertDialog(
        state = dialogState,
        modifier = Modifier.widthIn(min = 360.dp, max = 460.dp),
        onDismiss = {},
    ) {
        AlertDialogHeader {
            AlertDialogTitle(stringResource(Res.string.home_recent_join_loading_title))
            AlertDialogDescription(progress.phase.ifBlank { stringResource(Res.string.home_recent_join_loading_desc) })
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Progress(value = progress.progress ?: 0f)
            if (progress.detail.isNotBlank()) {
                TypographyMuted(progress.detail)
            }
        }
    }
}

@Composable
private fun DiscoveredSessionCard(
    session: DiscoveredSession,
    onJoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, DefaultShape)
            .clip(DefaultShape)
            .border(1.dp, Theme[colors][border], DefaultShape)
            .background(Theme[colors][card])
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Lucide.Wifi,
            contentDescription = null,
            tint = Color(session.session.host.color),
            modifier = Modifier.size(20.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = session.session.name,
                style = Theme[typography][p],
                fontWeight = FontWeight.SemiBold,
                color = Theme[colors][cardForeground],
            )
            Text(
                text = "Host: ${session.session.host.name.ifBlank { stringResource(Res.string.home_recent_collaboration_session_unknown_host) }} · ${session.session.participants.size} participants",
                style = Theme[typography][small],
                color = Theme[colors][mutedForeground],
            )
        }

        Button(
            onClick = onJoin,
            size = ButtonSize.Small,
        ) {
            Text(stringResource(Res.string.home_recent_join_button))
        }
    }
}

@Composable
private fun JoinSessionDialog(
    session: DiscoveredSession,
    initialUserName: String,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
) {
    val dialogState = rememberDialogState()
    var userName by remember(initialUserName) { mutableStateOf(initialUserName) }

    LaunchedEffect(Unit) {
        dialogState.visible = true
    }

    AlertDialog(
        state = dialogState,
        onDismiss = onDismiss,
    ) {
        AlertDialogHeader {
            AlertDialogTitle("Join ${session.session.name}")
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TypographyMuted("Host: ${session.session.host.name.ifBlank { stringResource(Res.string.home_recent_collaboration_session_unknown_host) }}")
            Input(
                value = userName,
                onValueChange = { userName = it },
                placeholder = stringResource(Res.string.home_recent_join_dialog_name_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AlertDialogFooter {
            AlertDialogCancel(onClick = onDismiss) {
                Text(stringResource(Res.string.home_recent_join_dialog_cancel))
            }
            Button(
                onClick = { onJoin(userName.trim()) },
                size = ButtonSize.Small,
                enabled = userName.isNotBlank(),
            ) {
                Text(stringResource(Res.string.home_recent_join_button))
            }
        }
    }
}

@Composable
private fun RecentViewHeader() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypographyH2(stringResource(Res.string.home_recent_title))

            TypographyLead(stringResource(Res.string.home_recent_subtitle))
        }
    }
}

@Composable
private fun RecentActions(
    onOpenProject: () -> Unit,
    onCreateProject: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .background(Theme[colors][background])
            .padding(horizontal = 12.dp, vertical = 20.dp),
    ) {
        Button(
            onClick = onCreateProject,
            variant = ButtonVariant.Default,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null,
                tint = buttonContentColor(ButtonVariant.Default),
            )

            Text(stringResource(Res.string.home_recent_new_project))
        }

        Button(
            onClick = onOpenProject,
            variant = ButtonVariant.Outline,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Lucide.FolderOpen,
                contentDescription = null,
                tint = buttonContentColor(ButtonVariant.Outline),
            )

            Text(stringResource(Res.string.home_recent_open_project))
        }
    }
}

@Composable
private fun EmptyRecentProjectsCard(
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
    ) {
        CardHeader {
            CardTitle(stringResource(Res.string.home_recent_empty_title))
            CardDescription("Create a new project or open an existing `.ame` workspace. Alternatively, convert an Ableton, Apollo or UniPad project into an Amethyst workspace!")
        }

        // CardContent {
        //     TypographyMuted("Open an existing `.ame` workspace or create a new project to start building your next performance.")
        // }
    }
}

@Composable
private fun RecentProjectCard(
    project: RecentWorkspace,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var actionsExpanded by remember(project.path) { mutableStateOf(false) }
    val folderPathLabel = remember(project.path) { displayFolderPath(project.path) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, DefaultShape)
            .clip(DefaultShape)
            .border(1.dp, Theme[colors][border], DefaultShape)
            .background(Theme[colors][card])
            .clickable(onClick = onOpen)
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
                style = Theme[typography][p],
                fontWeight = FontWeight.SemiBold,
                color = Theme[colors][cardForeground],
            )
            Text(
                text = folderPathLabel,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = Theme[typography][small],
                color = Theme[colors][mutedForeground],
            )
        }

        DropdownMenu(
            expanded = actionsExpanded,
            onExpandRequest = { actionsExpanded = true },
            onDismissRequest = { actionsExpanded = false },
        ) {
            DropdownMenuTrigger(
                onClick = { actionsExpanded = true },
            ) {
                Button(
                    onClick = { actionsExpanded = true },
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Icon,
                ) {
                    Icon(
                        imageVector = Lucide.Ellipsis,
                        contentDescription = stringResource(Res.string.home_recent_actions_desc),
                        tint = buttonContentColor(ButtonVariant.Ghost),
                    )
                }
            }

            DropdownMenuContent(
                expanded = actionsExpanded,
                onDismissRequest = { actionsExpanded = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        actionsExpanded = false
                        onOpen()
                    },
                ) {
                    Icon(
                        imageVector = Lucide.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Theme[colors][popoverForeground],
                    )
                    Text(stringResource(Res.string.home_recent_menu_open))
                }
                DropdownMenuItem(
                    onClick = {
                        actionsExpanded = false
                        onEdit()
                    },
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Theme[colors][popoverForeground],
                    )
                    Text(stringResource(Res.string.home_recent_menu_edit))
                }
                DropdownMenuItem(
                    onClick = {
                        actionsExpanded = false
                        onDelete()
                    },
                    destructive = true,
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Theme[colors][destructive],
                    )
                    Text(stringResource(Res.string.home_recent_menu_remove))
                }
            }
        }
    }
}

@Composable
private fun buttonContentColor(variant: ButtonVariant): Color {
    return when (variant) {
        ButtonVariant.Default -> Theme[colors][primaryForeground]
        ButtonVariant.Secondary -> Theme[colors][foreground]
        ButtonVariant.Destructive -> Theme[colors][destructiveForeground]
        ButtonVariant.Outline -> Theme[colors][foreground]
        ButtonVariant.Ghost -> Theme[colors][foreground]
        ButtonVariant.Link -> Theme[colors][foreground]
    }
}

private fun loadRecentProjects(): List<RecentWorkspace> {
    return HomeRepository.recentWorkspaces()
}

private fun displayFolderPath(path: String): String {
    val normalizedPath = path.replace('\\', '/')
    val separatorIndex = normalizedPath.lastIndexOf('/')
    if (separatorIndex <= 0) return normalizedPath

    val parentPath = normalizedPath.substring(0, separatorIndex).trimEnd('/')
    return abbreviateHomePrefix(parentPath)
}

private fun abbreviateHomePrefix(path: String): String {
    val unixHomePrefixes = listOf("/Users/", "/home/")

    unixHomePrefixes.forEach { prefix ->
        if (path.startsWith(prefix)) {
            val userSeparatorIndex = path.indexOf('/', startIndex = prefix.length)
            return if (userSeparatorIndex == -1) "~" else "~${path.substring(userSeparatorIndex)}"
        }
    }

    val windowsUsersMarker = "/Users/"
    if (path.length > 2 && path[1] == ':' && path.contains(windowsUsersMarker)) {
        val markerIndex = path.indexOf(windowsUsersMarker)
        val userSeparatorIndex = path.indexOf('/', startIndex = markerIndex + windowsUsersMarker.length)
        return if (userSeparatorIndex == -1) "~" else "~${path.substring(userSeparatorIndex)}"
    }

    return path
}

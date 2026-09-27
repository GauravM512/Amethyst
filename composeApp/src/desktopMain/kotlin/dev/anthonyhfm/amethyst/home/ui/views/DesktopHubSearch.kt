package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.Icon
import com.composeunstyled.theme.Theme
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import dev.anthonyhfm.amethyst.hub.data.*
import dev.anthonyhfm.amethyst.ui.components.primitives.*
import dev.anthonyhfm.amethyst.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class DesktopHubFilters(
    val sort: HubProjectSort = HubProjectSort.newest,
    val type: HubProjectType? = null,
    val compatibility: HubProjectCompatibility? = null,
    val difficulty: String? = null,
) {
    val active get() = sort != HubProjectSort.newest || type != null || compatibility != null || difficulty != null
}

@Composable
internal fun DesktopHubSearch(
    repository: HubRepository,
    query: String,
    onQueryChange: (String) -> Unit,
    browseAll: Boolean,
    initialSort: HubProjectSort,
    sessionRevision: Int,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSignIn: () -> Unit,
) {
    var filters by remember(initialSort) { mutableStateOf(DesktopHubFilters(sort = initialSort)) }
    var search by remember { mutableStateOf<HubSearchResult?>(null) }
    var projects by remember { mutableStateOf<List<HubProject>>(emptyList()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var moreError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var generation by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val trimmed = query.trim()
    val browse = browseAll || filters.active

    LaunchedEffect(repository, sessionRevision, trimmed, browse, filters, retry) {
        generation++
        search = null; projects = emptyList(); cursor = null; error = false; moreError = false
        if (trimmed.isEmpty() && !browse) { loading = false; return@LaunchedEffect }
        loading = true
        try {
            delay(300)
            val found = if (trimmed.isNotEmpty() && !browseAll) repository.search.execute(trimmed, 30) else null
            val page = if (browse) repository.browseProjects.execute(
                limit = 24, query = trimmed.ifEmpty { null }, sort = filters.sort,
                type = filters.type, compatibility = filters.compatibility, difficulty = filters.difficulty,
            ) else null
            search = found; projects = page?.items.orEmpty(); cursor = page?.nextCursor
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(if (browseAll) Res.string.home_hub_catalog_title else Res.string.home_hub_search_prompt_title), style = Theme[typography][h2].copy(color = Theme[colors][foreground]))
            Text(stringResource(if (browseAll) Res.string.home_hub_catalog_subtitle else Res.string.home_hub_search_prompt_description), style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
        }
        Input(query, onQueryChange, Modifier.fillMaxWidth(), placeholder = stringResource(if (browseAll) Res.string.home_hub_catalog_search else Res.string.home_hub_search_placeholder))
        if (browseAll) DesktopHubFilterBar(filters) { filters = it }
        when {
            trimmed.isEmpty() && !browse -> DesktopHubMessage(stringResource(Res.string.home_hub_search_prompt_description))
            loading -> DesktopHubMessage(stringResource(Res.string.home_hub_detail_loading))
            error -> DesktopHubMessage(stringResource(if (browseAll) Res.string.home_hub_catalog_error else Res.string.home_hub_search_error)) {
                Button(onClick = { retry++ }) { Text(stringResource(Res.string.home_hub_retry)) }
            }
            else -> {
                val visibleProjects = if (browse) projects else search?.projects.orEmpty()
                val artists = search?.artists.orEmpty()
                if (visibleProjects.isEmpty() && artists.isEmpty()) DesktopHubMessage(stringResource(Res.string.home_hub_catalog_empty))
                else {
                    if (artists.isNotEmpty()) {
                        DesktopHubSectionTitle(stringResource(Res.string.home_hub_search_artists), search?.artistCount)
                        DesktopHubRow {
                            artists.forEach { artist ->
                                Row(Modifier.width(240.dp).clickable { onOpenArtist(artist.username) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DesktopHubArtwork(artist.avatarUrl, Modifier.size(52.dp).then(Modifier), "👤")
                                    Column {
                                        Text(artist.displayName.ifBlank { artist.username }, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1)
                                        Text("@${artist.username}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    if (visibleProjects.isNotEmpty()) {
                        DesktopHubSectionTitle(stringResource(Res.string.home_hub_search_projects), if (browse) null else search?.projectCount)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth >= 900.dp) 3 else if (maxWidth >= 600.dp) 2 else 1
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                visibleProjects.chunked(columns).forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                        row.forEach { project ->
                                            DesktopHubProjectCard(project, repository, Modifier.weight(1f), onOpenArtist, onOpenProject, onSignIn)
                                        }
                                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                    if (cursor != null) Button(onClick = {
                        val currentCursor = cursor ?: return@Button
                        val request = generation
                        scope.launch {
                            loadingMore = true; moreError = false
                            try {
                                val page = repository.browseProjects.execute(
                                    cursor = currentCursor, limit = 24, query = trimmed.ifEmpty { null },
                                    sort = filters.sort, type = filters.type, compatibility = filters.compatibility, difficulty = filters.difficulty,
                                )
                                if (request == generation) {
                                    val seen = projects.mapTo(mutableSetOf()) { it.id }
                                    projects = projects + page.items.filter { seen.add(it.id) }
                                    cursor = page.nextCursor
                                }
                            } catch (_: Exception) { if (request == generation) moreError = true }
                            finally { if (request == generation) loadingMore = false }
                        }
                    }, enabled = !loadingMore, variant = ButtonVariant.Outline) {
                        Text(if (loadingMore) stringResource(Res.string.home_hub_detail_loading) else stringResource(if (moreError) Res.string.home_hub_retry else Res.string.home_hub_detail_load_more))
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopHubProjectCard(
    project: HubProject,
    repository: HubRepository,
    modifier: Modifier = Modifier,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var liked by remember(project.id, project.isLiked) { mutableStateOf(project.isLiked) }
    var likePending by remember(project.id) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth()) {
            DesktopHubArtwork(
                project.thumbnailUrl,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenProject(project.artist.username, project.slug) },
            )
            DesktopHubArtwork(
                project.artist.avatarUrl,
                Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp).size(38.dp).clip(CircleShape)
                    .clickable { onOpenArtist(project.artist.username) },
                "👤",
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.artist.displayName.ifBlank { project.artist.username }, modifier = Modifier.clickable { onOpenArtist(project.artist.username) }, style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
                Text(project.title, modifier = Modifier.clickable { onOpenProject(project.artist.username, project.slug) }, style = Theme[typography][p].copy(color = Theme[colors][foreground]), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(onClick = {
                if (!repository.client.isAuthenticated) { onSignIn(); return@Button }
                scope.launch {
                    likePending = true
                    try { liked = repository.toggleProjectLike.execute(project.id).liked }
                    catch (_: Exception) { /* Keep the last confirmed state. */ }
                    finally { likePending = false }
                }
            }, enabled = !likePending, variant = if (liked) ButtonVariant.Secondary else ButtonVariant.Ghost,
                size = ButtonSize.Icon, shape = CircleShape) {
                Icon(Lucide.Heart, contentDescription = stringResource(if (liked) Res.string.home_hub_detail_unlike else Res.string.home_hub_detail_like), modifier = Modifier.size(19.dp), tint = Theme[colors][foreground])
            }
        }
        Text("${project.projectType.name.replaceFirstChar { it.uppercase() }} · ${project.compatibility.name.replaceFirstChar { it.uppercase() }}", style = Theme[typography][small].copy(color = Theme[colors][mutedForeground]), maxLines = 1)
    }
}

@Composable
private fun DesktopHubSectionTitle(title: String, count: Long?) {
    Text(if (count == null) title else "$title · $count", style = Theme[typography][h3].copy(color = Theme[colors][foreground]))
}

@Composable
private fun DesktopHubFilterBar(filters: DesktopHubFilters, onChange: (DesktopHubFilters) -> Unit) {
    val sort = listOf(HubProjectSort.newest to Res.string.home_hub_catalog_newest, HubProjectSort.popular to Res.string.home_hub_catalog_popular, HubProjectSort.views to Res.string.home_hub_catalog_most_viewed, HubProjectSort.title to Res.string.home_hub_catalog_title_sort)
    val type = listOf(null to Res.string.home_hub_catalog_all, HubProjectType.amethyst to Res.string.home_hub_catalog_amethyst, HubProjectType.ableton to Res.string.home_hub_catalog_ableton, HubProjectType.apollo to Res.string.home_hub_catalog_apollo, HubProjectType.unipad to Res.string.home_hub_catalog_unipad)
    val compatibility = listOf(null to Res.string.home_hub_catalog_all, HubProjectCompatibility.compatible to Res.string.home_hub_catalog_compatible, HubProjectCompatibility.partially to Res.string.home_hub_catalog_partially, HubProjectCompatibility.incompatible to Res.string.home_hub_catalog_incompatible, HubProjectCompatibility.unknown to Res.string.home_hub_catalog_unknown)
    val difficulty = listOf(null to Res.string.home_hub_catalog_all, "1-3" to Res.string.home_hub_catalog_beginner, "4-7" to Res.string.home_hub_catalog_intermediate, "8-10" to Res.string.home_hub_catalog_expert)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        DesktopHubFilter(stringResource(Res.string.home_hub_catalog_sort), filters.sort, sort) { onChange(filters.copy(sort = it)) }
        DesktopHubFilter(stringResource(Res.string.home_hub_detail_format), filters.type, type) { onChange(filters.copy(type = it)) }
        DesktopHubFilter(stringResource(Res.string.home_hub_detail_compatibility), filters.compatibility, compatibility) { onChange(filters.copy(compatibility = it)) }
        DesktopHubFilter(stringResource(Res.string.home_hub_detail_difficulty), filters.difficulty, difficulty) { onChange(filters.copy(difficulty = it)) }
        if (filters.active) Button(onClick = { onChange(DesktopHubFilters()) }, variant = ButtonVariant.Link) { Text(stringResource(Res.string.home_hub_catalog_reset)) }
    }
}

@Composable
private fun <T> DesktopHubFilter(title: String, selected: T, options: List<Pair<T, StringResource>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onExpandRequest = { expanded = true }, onDismissRequest = { expanded = false }) {
        DropdownMenuTrigger(onClick = { expanded = true }) {
            Button(onClick = { expanded = true }, variant = ButtonVariant.Outline) {
                Text("$title: ${stringResource(options.first { it.first == selected }.second)}  ▾")
            }
        }
        DropdownMenuContent(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) -> DropdownMenuItem(onClick = { expanded = false; onSelect(value) }) { Text(stringResource(label)) } }
        }
    }
}

package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.hub.data.HubArtist
import dev.anthonyhfm.amethyst.hub.data.HubProject
import dev.anthonyhfm.amethyst.hub.data.HubProjectCompatibility
import dev.anthonyhfm.amethyst.hub.data.HubProjectSort
import dev.anthonyhfm.amethyst.hub.data.HubProjectType
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.hub.data.HubSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class HubFilters(
    val sort: HubProjectSort = HubProjectSort.newest,
    val type: HubProjectType? = null,
    val compatibility: HubProjectCompatibility? = null,
    val difficulty: String? = null,
) {
    val active: Boolean get() = sort != HubProjectSort.newest || type != null || compatibility != null || difficulty != null
}

@Composable
internal fun HubSearchContent(
    repository: HubRepository,
    query: String,
    browseAll: Boolean,
    initialSort: HubProjectSort,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
) {
    var filters by remember(initialSort) { mutableStateOf(HubFilters(sort = initialSort)) }
    var result by remember { mutableStateOf<HubSearchResult?>(null) }
    var filteredProjects by remember { mutableStateOf<List<HubProject>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var moreError by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var requestGeneration by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val trimmed = query.trim()
    val browse = browseAll || filters.active

    LaunchedEffect(trimmed, browseAll, filters, retry) {
        requestGeneration++
        result = null
        filteredProjects = emptyList()
        nextCursor = null
        error = false
        moreError = false
        loadingMore = false
        if (trimmed.isEmpty() && !browse) { loading = false; return@LaunchedEffect }
        loading = true
        try {
            delay(300)
            val found = if (trimmed.isEmpty()) null else repository.search.execute(trimmed, 30)
            val page = if (browse) repository.browseProjects.execute(
                limit = 24,
                query = trimmed.ifEmpty { null },
                sort = filters.sort,
                type = filters.type,
                compatibility = filters.compatibility,
                difficulty = filters.difficulty,
            ) else null
            coroutineContext.ensureActive()
            result = found
            filteredProjects = page?.items.orEmpty()
            nextCursor = page?.nextCursor
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        HubFilterBar(filters, onChange = { filters = it })
        when {
            trimmed.isEmpty() && !browse -> HubSearchMessage(
                stringResource(Res.string.home_hub_search_prompt_title),
                stringResource(Res.string.home_hub_search_prompt_description),
            )
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error -> HubSearchMessage(stringResource(Res.string.home_hub_search_error_title), stringResource(Res.string.home_hub_search_error)) {
                Button(onClick = { retry++ }) { Text(stringResource(Res.string.home_hub_retry)) }
            }
            else -> {
                val projects = if (browse) filteredProjects else result?.projects.orEmpty()
                val artists = result?.artists.orEmpty()
                if (projects.isEmpty() && artists.isEmpty()) HubSearchMessage(
                    stringResource(Res.string.home_hub_catalog_empty),
                    stringResource(Res.string.home_hub_catalog_empty_description),
                ) else LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    if (artists.isNotEmpty()) {
                        item(key = "artists_heading") { HubResultHeading(stringResource(Res.string.home_hub_search_artists), result?.artistCount ?: artists.size.toLong()) }
                        items(artists, key = { "artist:${it.username}" }) { artist ->
                            HubArtistResult(artist) { onOpenArtist(artist.username) }
                        }
                    }
                    if (projects.isNotEmpty()) {
                        item(key = "projects_heading") { HubResultHeading(stringResource(Res.string.home_hub_search_projects), if (browse) projects.size.toLong() else result?.projectCount ?: projects.size.toLong()) }
                        items(projects, key = { "project:${it.id}" }) { project ->
                            HubProjectResult(project) { onOpenProject(project.artist.username, project.slug) }
                        }
                    }
                    if (nextCursor != null) item(key = "load_more") {
                        TextButton(
                            onClick = {
                                val cursor = nextCursor ?: return@TextButton
                                val generation = requestGeneration
                                scope.launch {
                                    loadingMore = true
                                    moreError = false
                                    try {
                                        val page = repository.browseProjects.execute(
                                            cursor = cursor, limit = 24, query = trimmed.ifEmpty { null },
                                            sort = filters.sort, type = filters.type,
                                            compatibility = filters.compatibility, difficulty = filters.difficulty,
                                        )
                                        if (generation != requestGeneration) return@launch
                                        val seen = filteredProjects.mapTo(mutableSetOf()) { it.id }
                                        filteredProjects = filteredProjects + page.items.filter { seen.add(it.id) }
                                        nextCursor = page.nextCursor
                                    } catch (_: Exception) { if (generation == requestGeneration) moreError = true }
                                    finally { if (generation == requestGeneration) loadingMore = false }
                                }
                            },
                            enabled = !loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        ) {
                            if (loadingMore) CircularProgressIndicator(Modifier.size(20.dp))
                            else Text(stringResource(if (moreError) Res.string.home_hub_retry else Res.string.home_hub_detail_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubFilterBar(filters: HubFilters, onChange: (HubFilters) -> Unit) {
    val sortOptions = listOf(
        HubProjectSort.newest to Res.string.home_hub_catalog_newest,
        HubProjectSort.popular to Res.string.home_hub_catalog_popular,
        HubProjectSort.views to Res.string.home_hub_catalog_most_viewed,
        HubProjectSort.title to Res.string.home_hub_catalog_title_sort,
    )
    val typeOptions = listOf(
        null to Res.string.home_hub_catalog_all,
        HubProjectType.amethyst to Res.string.home_hub_catalog_amethyst,
        HubProjectType.ableton to Res.string.home_hub_catalog_ableton,
        HubProjectType.apollo to Res.string.home_hub_catalog_apollo,
        HubProjectType.unipad to Res.string.home_hub_catalog_unipad,
    )
    val compatibilityOptions = listOf(
        null to Res.string.home_hub_catalog_all,
        HubProjectCompatibility.compatible to Res.string.home_hub_catalog_compatible,
        HubProjectCompatibility.partially to Res.string.home_hub_catalog_partially,
        HubProjectCompatibility.incompatible to Res.string.home_hub_catalog_incompatible,
        HubProjectCompatibility.unknown to Res.string.home_hub_catalog_unknown,
    )
    val difficultyOptions = listOf(
        null to Res.string.home_hub_catalog_all,
        "1-3" to Res.string.home_hub_catalog_beginner,
        "4-7" to Res.string.home_hub_catalog_intermediate,
        "8-10" to Res.string.home_hub_catalog_expert,
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item { HubFilterChip(stringResource(Res.string.home_hub_catalog_sort), filters.sort, HubProjectSort.newest, sortOptions) { onChange(filters.copy(sort = it)) } }
        item { HubFilterChip(stringResource(Res.string.home_hub_detail_format), filters.type, null, typeOptions) { onChange(filters.copy(type = it)) } }
        item { HubFilterChip(stringResource(Res.string.home_hub_detail_compatibility), filters.compatibility, null, compatibilityOptions) { onChange(filters.copy(compatibility = it)) } }
        item { HubFilterChip(stringResource(Res.string.home_hub_detail_difficulty), filters.difficulty, null, difficultyOptions) { onChange(filters.copy(difficulty = it)) } }
        if (filters.active) item { TextButton(onClick = { onChange(HubFilters()) }) { Text(stringResource(Res.string.home_hub_catalog_reset)) } }
    }
}

@Composable
private fun <T> HubFilterChip(title: String, selected: T, default: T, options: List<Pair<T, StringResource>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second?.let { stringResource(it) } ?: title
    Box {
        FilterChip(selected = selected != default, onClick = { expanded = true }, label = { Text(if (selected == default) title else selectedLabel) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

@Composable
private fun HubResultHeading(title: String, count: Long) {
    Text("$title · $count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
}

@Composable
private fun HubArtistResult(artist: HubArtist, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(artist.displayName.ifBlank { artist.username }) },
        supportingContent = { Text("@${artist.username}") },
        leadingContent = { HubArtwork(artist.avatarUrl, Modifier.size(52.dp), androidx.compose.foundation.shape.CircleShape, Icons.Default.Person) },
        modifier = Modifier.fillMaxWidth().then(Modifier.clickable(onClick = onClick)),
    )
    HorizontalDivider(Modifier.padding(start = 84.dp))
}

@Composable
internal fun HubProjectResult(project: HubProject, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(project.title, maxLines = 1) },
        supportingContent = { Text(project.artist.displayName.ifBlank { "@${project.artist.username}" }, maxLines = 1) },
        leadingContent = { HubArtwork(project.thumbnailUrl, Modifier.size(58.dp), MaterialTheme.shapes.medium) },
        modifier = Modifier.fillMaxWidth().then(Modifier.clickable(onClick = onClick)),
    )
    HorizontalDivider(Modifier.padding(start = 90.dp))
}

@Composable
private fun HubSearchMessage(title: String, description: String, action: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

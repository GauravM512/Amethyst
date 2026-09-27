package dev.anthonyhfm.amethyst.home.ui.views

import amethyst.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.composeunstyled.Icon
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.X
import dev.anthonyhfm.amethyst.home.account.DesktopHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubProjectSort
import dev.anthonyhfm.amethyst.hub.data.HubRepository
import dev.anthonyhfm.amethyst.ui.components.primitives.Button
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonSize
import dev.anthonyhfm.amethyst.ui.components.primitives.ButtonVariant
import dev.anthonyhfm.amethyst.ui.components.primitives.ScrollArea
import dev.anthonyhfm.amethyst.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import java.net.URI

enum class DesktopHubSection { Home, Projects, Liked, Search }

/** One Studio destination with the same content hierarchy as Hub on the web. */
@Composable
fun BrowserView(
    repository: HubRepository,
    section: DesktopHubSection,
    onSectionChange: (DesktopHubSection) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSignIn: () -> Unit,
    sessionRevision: Int,
) {
    val account = remember { DesktopHubAccount.get() }.account
    var catalogSort by remember { mutableStateOf(HubProjectSort.newest) }
    var searchQuery by remember { mutableStateOf("") }
    var catalogQuery by remember { mutableStateOf("") }
    var sectionBeforeSearch by remember { mutableStateOf(DesktopHubSection.Home) }

    fun openHref(href: String?) {
        val uri = href?.let { runCatching { URI.create(it) }.getOrNull() } ?: return
        val path = uri.path?.trim('/') ?: return
        when {
            path == "projects" -> {
                catalogSort = if (uri.query?.split('&')?.contains("sort=popular") == true) HubProjectSort.popular else HubProjectSort.newest
                onSectionChange(DesktopHubSection.Projects)
            }
            path == "search" -> onSectionChange(DesktopHubSection.Search)
            path == "liked" || path == "projects/liked" -> onSectionChange(DesktopHubSection.Liked)
            path.startsWith("@") -> {
                val parts = path.removePrefix("@").split('/')
                if (parts.size >= 2) onOpenProject(parts[0], parts[1]) else onOpenArtist(parts[0])
            }
            path.startsWith("artists/") -> onOpenArtist(path.removePrefix("artists/"))
            path.startsWith("projects/") -> {
                val parts = path.split('/')
                if (parts.size >= 3) onOpenProject(parts[1].removePrefix("@"), parts[2])
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val contentWidth = maxWidth.coerceAtMost(1200.dp)
        ScrollArea(Modifier.fillMaxSize().clipToBounds()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.width(contentWidth).padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Theme[colors][card]).clickable(onClick = onSignIn),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (account == null) Icon(Lucide.UserRound, contentDescription = stringResource(Res.string.account_section_title), modifier = Modifier.size(23.dp), tint = Theme[colors][foreground])
                            else DesktopHubAvatar(account.username, account.avatarUrl, 40.dp)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (section == DesktopHubSection.Search) onSectionChange(sectionBeforeSearch)
                                else { sectionBeforeSearch = section; onSectionChange(DesktopHubSection.Search) }
                            },
                            variant = if (section == DesktopHubSection.Search) ButtonVariant.Secondary else ButtonVariant.Ghost,
                            size = ButtonSize.Icon,
                            shape = CircleShape,
                        ) {
                            Icon(
                                if (section == DesktopHubSection.Search) Lucide.X else Lucide.Search,
                                contentDescription = stringResource(Res.string.home_hub_search_placeholder),
                                modifier = Modifier.size(20.dp),
                                tint = Theme[colors][foreground],
                            )
                        }
                    }
                    when (section) {
                        DesktopHubSection.Home -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(Res.string.home_widescreen_navbar_group_home), style = Theme[typography][h2].copy(color = Theme[colors][foreground]))
                                Text(stringResource(Res.string.home_hub_home_subtitle), style = Theme[typography][p].copy(color = Theme[colors][mutedForeground]))
                            }
                            DesktopHubFeed(repository, sessionRevision, ::openHref, onOpenArtist, onOpenProject, onSignIn)
                        }
                        DesktopHubSection.Projects -> DesktopHubSearch(repository, catalogQuery, { catalogQuery = it }, true, catalogSort, sessionRevision, onOpenArtist, onOpenProject, onSignIn)
                        DesktopHubSection.Liked -> DesktopHubLikedProjects(
                            repository = repository,
                            sessionRevision = sessionRevision,
                            onNavigate = {
                                when (it) {
                                    is DesktopHubDestination.Artist -> onOpenArtist(it.username)
                                    is DesktopHubDestination.Project -> onOpenProject(it.username, it.slug)
                                }
                            },
                            onSignIn = onSignIn,
                            onBack = { onSectionChange(DesktopHubSection.Home) },
                            showBack = false,
                            onProjects = { onSectionChange(DesktopHubSection.Projects) },
                        )
                        DesktopHubSection.Search -> DesktopHubSearch(repository, searchQuery, { searchQuery = it }, false, HubProjectSort.newest, sessionRevision, onOpenArtist, onOpenProject, onSignIn)
                    }
                }
            }
        }
    }
}

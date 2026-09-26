package dev.anthonyhfm.amethyst.home.ui.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubAccount
import dev.anthonyhfm.amethyst.settings.data.SettingsRepository
import dev.anthonyhfm.amethyst.settings.ui.SettingsRenderer
import dev.anthonyhfm.amethyst.settings.AppLocaleRefreshBoundary
import org.jetbrains.compose.resources.stringResource

private enum class ProfilePage { Main, Security, Email }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(onRequestAuth: () -> Unit, onRequestEditProfile: () -> Unit) {
    val context = LocalContext.current
    val accountState = remember { AndroidHubAccount.get(context) }
    val account = accountState.account
    var page by remember { mutableStateOf(ProfilePage.Main) }
    var showPassword by remember { mutableStateOf(false) }

    BackHandler(page != ProfilePage.Main) { page = ProfilePage.Main }
    LaunchedEffect(account) { if (account == null) page = ProfilePage.Main }

    AppLocaleRefreshBoundary {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(when (page) {
                    ProfilePage.Main -> stringResource(Res.string.profile_title)
                    ProfilePage.Security -> stringResource(Res.string.account_security_title)
                    ProfilePage.Email -> stringResource(Res.string.account_email_title)
                }) },
                navigationIcon = {
                    if (page != ProfilePage.Main) IconButton(onClick = { page = ProfilePage.Main }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (page) {
                ProfilePage.Main -> LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "profile_header") {
                        ProfileHeader(account, onAuth = { accountState.prepareAuth(); onRequestAuth() }, onEdit = onRequestEditProfile)
                    }
                    if (accountState.error != null) item(key = "profile_error") {
                        Text(accountState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (account != null) item(key = "profile_account") {
                        ProfileSection(stringResource(Res.string.account_section_title)) {
                            ProfileRow(stringResource(Res.string.account_security_title)) { page = ProfilePage.Security }
                            HorizontalDivider()
                            ProfileRow(stringResource(Res.string.account_email_title), account.email ?: stringResource(Res.string.common_not_added)) { page = ProfilePage.Email }
                        }
                    }
                    items(SettingsRepository.settingsGroups, key = { "settings_${it.displayTitle}" }) { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(group.title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
                            group.settings.forEachIndexed { index, setting ->
                                SettingsRenderer(index == 0, index == group.settings.lastIndex, setting)
                            }
                        }
                    }
                    if (account != null) item(key = "profile_sign_out") {
                        OutlinedButton(onClick = accountState::signOut, enabled = !accountState.busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.account_sign_out), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                ProfilePage.Security -> SecurityPage(account, accountState, onPassword = { showPassword = true })
                ProfilePage.Email -> EmailPage(account, accountState)
            }
        }
    }

    if (showPassword && account != null) PasswordDialog(accountState, onDismiss = { showPassword = false })
    }
}

@Composable
internal fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column { content() } }
    }
}

@Composable
internal fun ProfileRow(title: String, trailing: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trailing != null) Text(trailing, modifier = Modifier.widthIn(max = 160.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun ProfileHeader(account: HubAccount?, onAuth: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileAvatar(account?.avatarUrl, 92)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                account?.displayName?.takeIf { it.isNotBlank() } ?: account?.username ?: stringResource(Res.string.account_signed_out),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (account == null) Button(onClick = onAuth) { Text(stringResource(Res.string.account_sign_in)) }
            else {
                Text("@${account.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (account.bio.isNotBlank()) Text(account.bio, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (account != null) IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.profile_edit))
        }
    }
}

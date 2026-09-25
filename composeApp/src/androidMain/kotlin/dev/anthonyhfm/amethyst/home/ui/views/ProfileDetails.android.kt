package dev.anthonyhfm.amethyst.home.ui.views

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.HubAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileScreen(state: AndroidHubAccount, onDismiss: () -> Unit) {
    val account = state.account ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatarReadError = stringResource(Res.string.profile_avatar_read_error)
    var displayName by remember(account.id) { mutableStateOf(account.displayName) }
    var bio by remember(account.id) { mutableStateOf(account.bio) }
    LaunchedEffect(Unit) { state.clearMessages() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val bytes = withContext(Dispatchers.IO) { readAvatar(context, uri) }
            if (bytes == null) state.showError(avatarReadError)
            else state.updateAvatar(bytes, context.contentResolver.getType(uri) ?: "image/jpeg")
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss, enabled = !state.busy) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { state.updateProfile(displayName, bio, onDismiss) }, enabled = !state.busy) {
                        Text(stringResource(Res.string.common_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(state.account?.avatarUrl, 96)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { picker.launch("image/*") }, enabled = !state.busy) { Text(stringResource(Res.string.common_change)) }
                        if (state.account?.avatarUrl != null) TextButton(onClick = state::removeAvatar, enabled = !state.busy) { Text(stringResource(Res.string.common_remove)) }
                    }
                }
                OutlinedTextField(displayName, { displayName = it }, label = { Text(stringResource(Res.string.profile_display_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it }, label = { Text(stringResource(Res.string.profile_bio)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                StatusText(state.error, state.message)
                if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private fun readAvatar(context: Context, uri: android.net.Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= 5 * 1024 * 1024)
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
}.getOrNull()

@Composable
internal fun SecurityPage(account: HubAccount?, state: AndroidHubAccount, onPassword: () -> Unit) {
    if (account == null) return
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileSection(stringResource(Res.string.account_password)) { ProfileRow(stringResource(Res.string.account_change_password), onClick = onPassword) }
        ProfileSection(stringResource(Res.string.account_two_factor_authentication)) {
            ListItem(headlineContent = { Text(stringResource(Res.string.account_authenticator_app)) }, supportingContent = { Text(stringResource(if (account.totpEnabled) Res.string.common_enabled else Res.string.common_not_enabled)) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer))
        }
        StatusText(state.error, state.message)
    }
}

@Composable
internal fun PasswordDialog(state: AndroidHubAccount, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    val mismatch = stringResource(Res.string.account_error_password_mismatch)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.account_change_password)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PasswordField(current, { current = it }, stringResource(Res.string.account_current_password))
            PasswordField(replacement, { replacement = it }, stringResource(Res.string.account_new_password))
            PasswordField(confirmation, { confirmation = it }, stringResource(Res.string.account_repeat_new_password))
            StatusText(validation ?: state.error, state.message)
        } },
        confirmButton = { TextButton(onClick = {
            validation = if (replacement != confirmation) mismatch else null
            if (validation == null) state.changePassword(current, replacement, onDismiss)
        }, enabled = !state.busy && current.isNotBlank() && replacement.isNotBlank()) { Text(stringResource(Res.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
internal fun EmailPage(account: HubAccount?, state: AndroidHubAccount) {
    if (account == null) return
    var email by remember(account.id) { mutableStateOf(account.email ?: "") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val updated = stringResource(Res.string.account_email_verification_sent)
    val removed = stringResource(Res.string.account_email_removed)
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileSection(stringResource(Res.string.account_current_email)) {
            ListItem(headlineContent = { Text(stringResource(Res.string.account_email_title)) }, supportingContent = { Text(account.email ?: stringResource(Res.string.common_not_added)) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer))
        }
        Text(stringResource(Res.string.account_change_email), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(Res.string.account_new_email_address)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        PasswordField(password, { password = it }, stringResource(Res.string.account_current_password))
        if (account.totpEnabled) OutlinedTextField(code, { code = it }, label = { Text(stringResource(Res.string.account_authenticator_code)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Button(onClick = { state.changeEmail(password, email, code, updated) }, enabled = !state.busy && email.isNotBlank() && password.isNotBlank()) { Text(stringResource(Res.string.account_update_email)) }
        if (account.email != null) OutlinedButton(onClick = { state.removeEmail(password, code, removed) }, enabled = !state.busy && password.isNotBlank()) { Text(stringResource(Res.string.account_remove_email), color = MaterialTheme.colorScheme.error) }
        StatusText(state.error, state.message)
    }
}

package dev.anthonyhfm.amethyst.home.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.*
import dev.anthonyhfm.amethyst.home.account.AndroidHubAccount
import dev.anthonyhfm.amethyst.hub.data.rememberHubImage
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileAvatar(path: String?, size: Int) {
    val bitmap = rememberHubImage(path = path)

    Box(
        modifier = Modifier
            .size(size = size.dp)
            .clip(shape = CircleShape)
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(Res.string.profile_picture),
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(Res.string.profile_picture),
                modifier = Modifier
                    .size(size = (size * .65f).dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthScreen(state: AndroidHubAccount, onDismiss: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var mfaCode by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    val usernameRequired = stringResource(Res.string.account_error_username_required)
    val passwordRequired = stringResource(Res.string.account_error_password_required)
    val mismatch = stringResource(Res.string.account_error_password_mismatch)
    val resetUsernameFirst = stringResource(Res.string.account_reset_username_first)
    val resetSent = stringResource(Res.string.account_reset_sent)

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.account_section_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(stringResource(Res.string.account_welcome_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.account_sync_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.mfaChallenge != null) {
                    Text(stringResource(Res.string.account_two_factor_authentication), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.account_mfa_sign_in_description))
                    OutlinedTextField(mfaCode, { mfaCode = it }, label = { Text(stringResource(Res.string.account_authentication_code)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Button(onClick = { state.completeMfa(mfaCode) }, enabled = !state.busy && mfaCode.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.account_verify_continue)) }
                } else {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(stringResource(Res.string.account_sign_in), stringResource(Res.string.account_create)).forEachIndexed { index, title ->
                            SegmentedButton(selected = register == (index == 1), onClick = { register = index == 1; validation = null; state.clearMessages() }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(title) }
                        }
                    }
                    OutlinedTextField(username, { username = it }, label = { Text(stringResource(Res.string.account_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (register) {
                        OutlinedTextField(displayName, { displayName = it }, label = { Text(stringResource(Res.string.profile_display_name)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(email, { email = it }, label = { Text(stringResource(Res.string.account_email_optional)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    }
                    PasswordField(password, { password = it }, stringResource(Res.string.account_password))
                    if (register) PasswordField(confirmation, { confirmation = it }, stringResource(Res.string.account_repeat_password))
                    Button(onClick = {
                        validation = when {
                            username.trim().isEmpty() -> usernameRequired
                            password.isEmpty() -> passwordRequired
                            register && password != confirmation -> mismatch
                            else -> null
                        }
                        if (validation == null) state.signIn(username, password, register, displayName, email)
                    }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (register) stringResource(Res.string.account_create) else stringResource(Res.string.account_sign_in)) }
                    if (!register) TextButton(onClick = {
                        validation = if (username.trim().isEmpty()) resetUsernameFirst else null
                        if (validation == null) state.requestReset(username, resetSent)
                    }, enabled = !state.busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(Res.string.account_forgot_password)) }
                }
                StatusText(validation ?: state.error, state.message)
                if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
internal fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun StatusText(error: String?, message: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    if (message != null) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
}

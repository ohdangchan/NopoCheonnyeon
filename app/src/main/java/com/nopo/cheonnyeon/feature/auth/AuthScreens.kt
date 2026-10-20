package com.nopo.cheonnyeon.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.rule.AuthInvalid
import com.nopo.cheonnyeon.domain.rule.AuthValidation

@StringRes
private fun AuthInvalid.messageRes(): Int = when (this) {
    AuthInvalid.EMAIL -> R.string.auth_invalid_email
    AuthInvalid.PASSWORD_SHORT -> R.string.auth_invalid_password_short
    AuthInvalid.PASSWORD_WEAK -> R.string.auth_invalid_password_weak
    AuthInvalid.PASSWORD_MISMATCH -> R.string.auth_invalid_password_mismatch
    AuthInvalid.NICK_LENGTH -> R.string.auth_invalid_nick_length
    AuthInvalid.NICK_CHARS -> R.string.auth_invalid_nick_chars
}

@Composable
private fun AuthFrame(title: String, subtitle: String, content: @Composable () -> Unit) {
    val c = NopoTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(title, style = NopoTheme.typography.headlineMedium, color = c.primary)
        Text(subtitle, style = NopoTheme.typography.bodyMedium, color = c.ink2)
        content()
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    error: AuthInvalid? = null,
    supporting: String? = null,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = (error?.let { stringResource(it.messageRes()) } ?: supporting)?.let { text -> { Text(text) } },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorLine(state: AuthUiState) {
    val error = state.error ?: return
    Text(error.message(LocalContext.current), color = NopoTheme.colors.stamp, style = NopoTheme.typography.bodyMedium)
}

@Composable
fun LoginScreen(vm: AuthViewModel, onSignUp: () -> Unit, onReset: () -> Unit, onDiagnostics: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val ready = AuthValidation.email(email) == null && password.isNotEmpty()
    AuthFrame(stringResource(R.string.auth_login_title), stringResource(R.string.auth_login_subtitle)) {
        Field(email, { email = it }, R.string.auth_email, keyboard = KeyboardType.Email)
        Field(password, { password = it }, R.string.auth_password, password = true)
        ErrorLine(state)
        NopoButton(
            text = stringResource(if (state.busy) R.string.auth_working else R.string.auth_login_action),
            onClick = { vm.signIn(email, password) },
            enabled = ready && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        NopoButton(stringResource(R.string.auth_to_sign_up), onSignUp, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        NopoButton(stringResource(R.string.auth_to_reset), onReset, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.auth_offline_note), color = NopoTheme.colors.ink2, style = NopoTheme.typography.bodySmall)
        if (onDiagnostics != null) {
            NopoButton(stringResource(R.string.diag_title), onDiagnostics, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SignUpScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nick by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    val nickInvalid = AuthValidation.nickname(nick).takeIf { nick.isNotEmpty() }
    val emailInvalid = AuthValidation.email(email).takeIf { email.isNotEmpty() }
    val pwInvalid = AuthValidation.password(password).takeIf { password.isNotEmpty() }
    val confirmInvalid = AuthValidation.passwordConfirm(password, confirm).takeIf { confirm.isNotEmpty() }
    val ready = AuthValidation.nickname(nick) == null && AuthValidation.email(email) == null &&
        AuthValidation.password(password) == null && AuthValidation.passwordConfirm(password, confirm) == null &&
        state.nick != NickStatus.TAKEN && agreed
    AuthFrame(stringResource(R.string.auth_sign_up_title), stringResource(R.string.auth_sign_up_subtitle)) {
        Field(
            nick, { nick = it; vm.onNicknameChanged(it) }, R.string.auth_nickname, error = nickInvalid,
            supporting = when (state.nick) {
                NickStatus.CHECKING -> stringResource(R.string.auth_nick_checking)
                NickStatus.AVAILABLE -> stringResource(R.string.auth_nick_available)
                NickStatus.TAKEN -> stringResource(R.string.auth_nick_taken)
                else -> stringResource(R.string.auth_nickname_hint)
            },
        )
        Field(email, { email = it }, R.string.auth_email, error = emailInvalid, keyboard = KeyboardType.Email)
        Field(password, { password = it }, R.string.auth_password, error = pwInvalid, password = true, supporting = stringResource(R.string.auth_password_hint))
        Field(confirm, { confirm = it }, R.string.auth_password_confirm, error = confirmInvalid, password = true)
        Text(stringResource(R.string.auth_privacy_note), color = NopoTheme.colors.ink2, style = NopoTheme.typography.bodySmall)
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().heightIn(min = NopoTheme.spacing.minTouch).toggleable(value = agreed, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = { agreed = it }),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(checked = agreed, onCheckedChange = null)
            Text(stringResource(R.string.auth_consent), color = NopoTheme.colors.ink, style = NopoTheme.typography.bodyMedium, modifier = Modifier.padding(start = NopoTheme.spacing.sm))
        }
        androidx.compose.material3.TextButton(onClick = { showPolicy = !showPolicy }) {
            Text(stringResource(if (showPolicy) R.string.settings_policy_hide else R.string.auth_consent_view))
        }
        if (showPolicy) Text(stringResource(R.string.privacy_policy_full), color = NopoTheme.colors.ink2, style = NopoTheme.typography.bodySmall)
        ErrorLine(state)
        NopoButton(
            text = stringResource(if (state.busy) R.string.auth_working else R.string.auth_sign_up_action),
            onClick = { vm.signUp(email, password, nick) },
            enabled = ready && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        NopoButton(stringResource(R.string.auth_to_login), onBack, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ResetPasswordScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    AuthFrame(stringResource(R.string.auth_reset_title), stringResource(R.string.auth_reset_subtitle)) {
        Field(email, { email = it }, R.string.auth_email, keyboard = KeyboardType.Email)
        ErrorLine(state)
        if (state.resetSent) {
            Text(stringResource(R.string.auth_reset_sent), color = NopoTheme.colors.primary, style = NopoTheme.typography.bodyMedium)
        }
        NopoButton(
            text = stringResource(if (state.busy) R.string.auth_working else R.string.auth_reset_action),
            onClick = { vm.sendReset(email) },
            enabled = AuthValidation.email(email) == null && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        NopoButton(stringResource(R.string.auth_to_login), onBack, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

/** 로그인은 됐지만 프로필(users 문서)이 없는 계정의 복구 화면 (FR-AUTH-07) */
@Composable
fun NicknameSetupScreen(vm: AuthViewModel, onSignOut: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nick by rememberSaveable { mutableStateOf("") }
    val invalid = AuthValidation.nickname(nick).takeIf { nick.isNotEmpty() }
    val ready = AuthValidation.nickname(nick) == null && state.nick != NickStatus.TAKEN
    AuthFrame(stringResource(R.string.auth_nick_setup_title), stringResource(R.string.auth_nick_setup_subtitle)) {
        Field(
            nick, { nick = it; vm.onNicknameChanged(it) }, R.string.auth_nickname, error = invalid,
            supporting = when (state.nick) {
                NickStatus.CHECKING -> stringResource(R.string.auth_nick_checking)
                NickStatus.AVAILABLE -> stringResource(R.string.auth_nick_available)
                NickStatus.TAKEN -> stringResource(R.string.auth_nick_taken)
                else -> stringResource(R.string.auth_nickname_hint)
            },
        )
        ErrorLine(state)
        NopoButton(
            text = stringResource(if (state.busy) R.string.auth_working else R.string.auth_nick_setup_action),
            onClick = { vm.createProfile(nick) },
            enabled = ready && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        NopoButton(stringResource(R.string.auth_sign_out), onSignOut, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

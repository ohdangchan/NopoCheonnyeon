package com.nopo.cheonnyeon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nopo.cheonnyeon.core.common.message
import kotlinx.coroutines.launch
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

private const val DEV_TAPS = 5

/** 설정 (S-903): 앱 정보·데이터 출처·오픈소스 라이선스·로그아웃. 앱 정보를 5번 누르면 진단 도구가 열린다 (SC-8). */
@Composable
fun SettingsScreen(container: AppContainer, signOutButton: @Composable () -> Unit, devTools: @Composable () -> Unit, onDiagnostics: () -> Unit) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    var taps by rememberSaveable { mutableIntStateOf(0) }
    var catalogInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }

    var nickname by remember { mutableStateOf<String?>(null) }
    val email = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email }
    LaunchedEffect(Unit) {
        nickname = container.auth.currentNickname()
    }
    LaunchedEffect(Unit) {
        catalogInfo = container.settings.catalogVersion() to container.catalog.all().size
    }

    Column(
        Modifier.fillMaxSize().background(c.paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        Text(stringResource(R.string.tab_settings), style = NopoTheme.typography.headlineMedium, color = c.primary)

        Section(stringResource(R.string.settings_account_info)) {
            Text(stringResource(R.string.settings_nickname, nickname ?: "-"), color = c.ink)
            Text(stringResource(R.string.settings_email, email ?: "-"), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        Section(stringResource(R.string.settings_app_info)) {
            Text(
                stringResource(R.string.settings_version, version),
                color = c.ink,
                modifier = Modifier.fillMaxWidth().clickable { taps++ },
            )
            catalogInfo?.let { (v, n) -> Text(stringResource(R.string.settings_catalog, n, v), color = c.ink2, style = NopoTheme.typography.bodySmall) }
        }
        Section(stringResource(R.string.settings_data_source)) {
            Text(stringResource(R.string.settings_data_source_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        Section(stringResource(R.string.settings_privacy)) {
            Text(stringResource(R.string.settings_privacy_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        NopoButton(
            stringResource(if (showLicenses) R.string.settings_licenses_hide else R.string.settings_licenses),
            { showLicenses = !showLicenses },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showLicenses) Text(stringResource(R.string.settings_licenses_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        NopoButton(
            stringResource(if (showPolicy) R.string.settings_policy_hide else R.string.settings_policy),
            { showPolicy = !showPolicy },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showPolicy) Text(stringResource(R.string.privacy_policy_full), color = c.ink2, style = NopoTheme.typography.bodySmall)

        if (taps >= DEV_TAPS) {
            Section(stringResource(R.string.settings_dev)) {
                NopoButton(stringResource(R.string.diag_title), onDiagnostics, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                devTools()
            }
        }
        AccountSection(container, c)
        signOutButton()
    }
}

/** 닉네임 변경·회원 탈퇴 (S-408, FR-AUTH-08·09). 온라인에서만 된다. */
@Composable
private fun AccountSection(container: AppContainer, c: com.nopo.cheonnyeon.core.designsystem.theme.NopoColors) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var nickDialog by rememberSaveable { mutableStateOf(false) }
    var visDialog by rememberSaveable { mutableStateOf(false) }
    var visibility by remember { mutableStateOf("ALL") }
    var visSaved by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(visDialog) {
        if (visDialog) container.auth.currentUid?.let { me -> container.profiles.profile(me).valueOrNull?.let { visibility = it.visibility } }
    }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var nick by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<com.nopo.cheonnyeon.core.common.AppError?>(null) }
    val nickInvalid = com.nopo.cheonnyeon.domain.rule.AuthValidation.nickname(nick).takeIf { nick.isNotEmpty() }

    Section(stringResource(R.string.settings_account)) {
        NopoButton(stringResource(R.string.settings_change_nick), { error = null; nick = ""; nickDialog = true }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        NopoButton(stringResource(R.string.settings_visibility), { error = null; visSaved = false; visDialog = true }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        if (visSaved) Text(stringResource(R.string.visibility_saved), color = c.primary, style = NopoTheme.typography.bodySmall)
        NopoButton(stringResource(R.string.settings_delete_account), { error = null; password = ""; deleteDialog = true }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }

    if (visDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!busy) visDialog = false },
            title = { Text(stringResource(R.string.settings_visibility)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
                    listOf("ALL" to R.string.visibility_all, "COUNT" to R.string.visibility_count, "PRIVATE" to R.string.visibility_private).forEach { (key, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { visibility = key },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(selected = visibility == key, onClick = { visibility = key })
                            Text(stringResource(label), color = c.ink)
                        }
                    }
                    Text(stringResource(R.string.visibility_note), color = c.ink2, style = NopoTheme.typography.bodySmall)
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.currentUid?.let { container.profiles.setVisibility(it, visibility) }
                            busy = false
                            error = r?.errorOrNull
                            if (r is com.nopo.cheonnyeon.core.common.AppResult.Success) { visSaved = true; visDialog = false }
                        }
                    },
                ) { Text(stringResource(R.string.reviews_save)) }
            },
            dismissButton = { androidx.compose.material3.TextButton({ visDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
        )
    }

    if (nickDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!busy) nickDialog = false },
            title = { Text(stringResource(R.string.settings_change_nick)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = nick, onValueChange = { nick = it }, singleLine = true,
                        label = { Text(stringResource(R.string.auth_nickname)) },
                        isError = nickInvalid != null,
                        supportingText = { Text(stringResource(R.string.auth_nickname_hint)) },
                    )
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !busy && com.nopo.cheonnyeon.domain.rule.AuthValidation.nickname(nick) == null,
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.changeNickname(nick)
                            busy = false
                            error = r.errorOrNull
                            if (r is com.nopo.cheonnyeon.core.common.AppResult.Success) nickDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.reviews_save)) }
            },
            dismissButton = { androidx.compose.material3.TextButton({ nickDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
        )
    }

    if (deleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!busy) deleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    Text(stringResource(R.string.settings_delete_warn))
                    androidx.compose.material3.OutlinedTextField(
                        value = password, onValueChange = { password = it }, singleLine = true,
                        label = { Text(stringResource(R.string.auth_password)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !busy && password.isNotEmpty(),
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.deleteAccount(password)
                            busy = false
                            error = r.errorOrNull
                            if (r is com.nopo.cheonnyeon.core.common.AppResult.Success) {
                                deleteDialog = false
                                container.clearLocalUserData()
                            }
                        }
                    },
                ) { Text(stringResource(R.string.settings_delete_confirm)) }
            },
            dismissButton = { androidx.compose.material3.TextButton({ deleteDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = NopoTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        Text(title, style = NopoTheme.typography.titleSmall, color = c.primary)
        content()
    }
}

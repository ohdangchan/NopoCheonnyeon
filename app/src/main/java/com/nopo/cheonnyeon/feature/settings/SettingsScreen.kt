package com.nopo.cheonnyeon.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoColors
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.rule.AuthValidation
import com.nopo.cheonnyeon.feature.store.MenuPhotos
import kotlinx.coroutines.launch

private const val DEV_TAPS = 5

/** 설정 (S-903): 앱 정보·데이터 출처·오픈소스 라이선스·로그아웃. 앱 정보를 5번 누르면 진단 도구가 열린다 (SC-8). */
@Composable
fun SettingsScreen(
    container: AppContainer,
    signOutButton: @Composable () -> Unit,
    devTools: @Composable () -> Unit,
    onDiagnostics: () -> Unit
) {
    val c = NopoTheme.colors
    val context = LocalContext.current
    var taps by rememberSaveable { mutableIntStateOf(0) }
    var catalogInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    val version = remember { runCatching { context.packageManager.getPackageInfo(
        context.packageName,
        0
    ).versionName }.getOrNull().orEmpty() }

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
                modifier = Modifier.fillMaxWidth().heightIn(min = NopoTheme.spacing.minTouch).clickable(role = Role.Button) { taps++ },
            )
            catalogInfo?.let { (
                v,
                n
            ) -> Text(stringResource(R.string.settings_catalog, n, v), color = c.ink2, style = NopoTheme.typography.bodySmall) }
        }
        Section(stringResource(R.string.settings_verify)) {
            val locOnly by container.settings.allowLocationOnly.collectAsStateWithLifecycle(initialValue = true)
            val locScope = androidx.compose.runtime.rememberCoroutineScope()
            Row(Modifier.fillMaxWidth().heightIn(min = NopoTheme.spacing.minTouch), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_location_only), Modifier.weight(1f), color = c.ink)
                Switch(locOnly, { v -> locScope.launch { container.settings.setAllowLocationOnly(v) } })
            }
            Text(stringResource(R.string.settings_location_only_help), color = c.ink2, style = NopoTheme.typography.bodySmall)
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
        if (showLicenses) {
            stringArrayResource(R.array.licenses_list).forEach { row ->
                val (name, license, purpose) = row.split("|")
                Column(
                    Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs)
                ) {
                    Text(name, color = c.ink, style = NopoTheme.typography.titleSmall)
                    Text(license, color = c.primary, style = NopoTheme.typography.labelMedium)
                    Text(purpose, color = c.ink2, style = NopoTheme.typography.bodySmall)
                }
            }
            // 대표 메뉴 사진 출처 (위키미디어 공용, CC0·CC BY): 작성자·라이선스·원본 링크
            Text("대표 메뉴 사진 출처 (크기를 줄이고 잘라서 사용했어요)", color = c.ink, style = NopoTheme.typography.titleSmall)
            MenuPhotos.credits(context).values.forEach { cr ->
                Column(
                    Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).clickable(role = Role.Button) {
                        runCatching { context.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(cr.page)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }.padding(NopoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xxs),
                ) {
                    Text(cr.title, color = c.ink, style = NopoTheme.typography.labelLarge)
                    Text(
                        listOf(cr.artist, cr.license).filter { it.isNotBlank() }.joinToString(" · "),
                        color = c.primary,
                        style = NopoTheme.typography.labelMedium
                    )
                    Text("Wikimedia Commons", color = c.ink2, style = NopoTheme.typography.bodySmall)
                }
            }
            Text(stringResource(R.string.settings_licenses_body), color = c.ink2, style = NopoTheme.typography.bodySmall)
        }
        NopoButton(
            stringResource(if (showPolicy) R.string.settings_policy_hide else R.string.settings_policy),
            { showPolicy = !showPolicy },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showPolicy) Text(stringResource(R.string.privacy_policy_full), color = c.ink2, style = NopoTheme.typography.bodySmall)

        if (taps >= DEV_TAPS) {
            Section(stringResource(R.string.settings_dev)) {
                NopoButton(
                    stringResource(R.string.diag_title),
                    onDiagnostics,
                    style = NopoButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
                val demoOn by container.settings.demoData.collectAsStateWithLifecycle(initialValue = true)
                val demoScope = androidx.compose.runtime.rememberCoroutineScope()
                Row(Modifier.fillMaxWidth().heightIn(min = NopoTheme.spacing.minTouch), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_demo_data), Modifier.weight(1f), color = c.ink)
                    Switch(demoOn, { v -> demoScope.launch { container.settings.setDemoData(v) } })
                }
                val demoAdded = androidx.compose.runtime.remember { mutableStateOf<Int?>(null) }
                NopoButton(stringResource(R.string.settings_demo_passport_fill), {
                    demoScope.launch { demoAdded.value = container.visits.seedDemoVisits(container.demoReviews.load().myVisits) }
                }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                NopoButton(stringResource(R.string.settings_demo_passport_clear), {
                    demoScope.launch { container.visits.clearDemoVisits(); demoAdded.value = -1 }
                }, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                demoAdded.value?.let { n ->
                    Text(
                        if (n < 0) stringResource(R.string.settings_demo_passport_cleared) else stringResource(
                            R.string.settings_demo_passport_done,
                            n
                        ),
                        color = c.primary,
                        style = NopoTheme.typography.bodySmall
                    )
                }
                devTools()
            }
        }
        AccountSection(container, c)
        signOutButton()
    }
}

/** 닉네임 변경·회원 탈퇴 (S-408, FR-AUTH-08·09). 온라인에서만 된다. */
@Composable
private fun AccountSection(container: AppContainer, c: NopoColors) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var nickDialog by rememberSaveable { mutableStateOf(false) }
    var visDialog by rememberSaveable { mutableStateOf(false) }
    var visibility by remember { mutableStateOf("ALL") }
    var visSaved by remember { mutableStateOf(false) }
    LaunchedEffect(visDialog) {
        if (visDialog) container.auth
            .currentUid?.let { me -> container.profiles.profile(me).valueOrNull?.let { visibility = it.visibility } }
    }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var nick by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<AppError?>(null) }
    val nickInvalid = AuthValidation.nickname(nick).takeIf { nick.isNotEmpty() }

    Section(stringResource(R.string.settings_account)) {
        NopoButton(
            stringResource(R.string.settings_change_nick),
            { error = null; nick = ""; nickDialog = true },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth()
        )
        NopoButton(
            stringResource(R.string.settings_visibility),
            { error = null; visSaved = false; visDialog = true },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth()
        )
        if (visSaved) Text(stringResource(R.string.visibility_saved), color = c.primary, style = NopoTheme.typography.bodySmall)
        NopoButton(
            stringResource(R.string.settings_delete_account),
            { error = null; password = ""; deleteDialog = true },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (visDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) visDialog = false },
            title = { Text(stringResource(R.string.settings_visibility)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
                    listOf(
                        "ALL" to R.string.visibility_all,
                        "COUNT" to R.string.visibility_count,
                        "PRIVATE" to R.string.visibility_private
                    ).forEach { (key, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { visibility = key },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = visibility == key, onClick = { visibility = key })
                            Text(stringResource(label), color = c.ink)
                        }
                    }
                    Text(stringResource(R.string.visibility_note), color = c.ink2, style = NopoTheme.typography.bodySmall)
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.currentUid?.let { container.profiles.setVisibility(it, visibility) }
                            busy = false
                            error = r?.errorOrNull
                            if (r is AppResult.Success) { visSaved = true; visDialog = false }
                        }
                    },
                ) { Text(stringResource(R.string.reviews_save)) }
            },
            dismissButton = { TextButton({ visDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
        )
    }

    if (nickDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) nickDialog = false },
            title = { Text(stringResource(R.string.settings_change_nick)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = nick, onValueChange = { nick = it }, singleLine = true,
                        label = { Text(stringResource(R.string.auth_nickname)) },
                        isError = nickInvalid != null,
                        supportingText = { Text(stringResource(R.string.auth_nickname_hint)) },
                    )
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && AuthValidation.nickname(nick) == null,
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.changeNickname(nick)
                            busy = false
                            error = r.errorOrNull
                            if (r is AppResult.Success) nickDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.reviews_save)) }
            },
            dismissButton = { TextButton({ nickDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) deleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    Text(stringResource(R.string.settings_delete_warn))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, singleLine = true,
                        label = { Text(stringResource(R.string.auth_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    error?.let { Text(it.message(context), color = c.stamp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && password.isNotEmpty(),
                    onClick = {
                        busy = true
                        scope.launch {
                            val r = container.auth.deleteAccount(password)
                            busy = false
                            error = r.errorOrNull
                            if (r is AppResult.Success) {
                                deleteDialog = false
                                container.clearLocalUserData()
                            }
                        }
                    },
                ) { Text(stringResource(R.string.settings_delete_confirm)) }
            },
            dismissButton = { TextButton({ deleteDialog = false }, enabled = !busy) { Text(stringResource(R.string.detail_back)) } },
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

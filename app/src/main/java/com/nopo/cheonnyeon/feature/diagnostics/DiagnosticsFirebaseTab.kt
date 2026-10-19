package com.nopo.cheonnyeon.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonSize
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme

/** SPK-5 Firebase 탭. 수동 주입(AppContainer)은 S-104에서 붙인다. */
@Composable
fun DiagnosticsFirebaseTab(
    modifier: Modifier = Modifier,
    viewModel: DiagFirebaseViewModel = viewModel { DiagFirebaseViewModel(Firebase.auth, Firebase.firestore) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    // 비밀번호는 회전 복원용 저장소에도 남기지 않는다
    var password by androidx.compose.runtime.remember { mutableStateOf("") }
    val busy = state.running != null
    val c = NopoTheme.colors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        item {
            Text(
                text = state.signedInUid?.let { stringResource(R.string.diag_fb_signed_in, it.take(8)) }
                    ?: stringResource(R.string.diag_fb_signed_out),
                style = NopoTheme.typography.titleSmall,
                color = if (state.signedInUid != null) c.primary else c.ink2,
            )
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.diag_fb_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.diag_fb_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val canSubmit = !busy && email.isNotBlank() && password.length >= 8
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
            ) {
                NopoButton(stringResource(R.string.diag_fb_sign_up), { viewModel.signUp(email, password) },
                    size = NopoButtonSize.Medium, enabled = canSubmit)
                NopoButton(stringResource(R.string.diag_fb_sign_in), { viewModel.signIn(email, password) },
                    size = NopoButtonSize.Medium, style = NopoButtonStyle.Outline, enabled = canSubmit)
                NopoButton(stringResource(R.string.diag_fb_write_read), viewModel::writeAndRead,
                    size = NopoButtonSize.Medium, style = NopoButtonStyle.Stamp,
                    enabled = !busy && state.signedInUid != null)
                NopoButton(stringResource(R.string.diag_fb_sign_out), viewModel::signOut,
                    size = NopoButtonSize.Medium, style = NopoButtonStyle.Outline,
                    enabled = !busy && state.signedInUid != null)
            }
        }
        item {
            Text(stringResource(R.string.diag_fb_hint), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
        if (busy) {
            item { Text(stringResource(R.string.diag_fb_running), style = NopoTheme.typography.labelLarge, color = c.ink2) }
        }
        items(state.log) { line ->
            val result = stringResource(if (line.ok) R.string.diag_fb_ok else R.string.diag_fb_fail)
            Text(
                text = stringResource(R.string.diag_fb_line, stepLabel(line.step), result, line.millis.toInt()) +
                    (line.detail?.let { " · $it" } ?: ""),
                style = NopoTheme.extraTypography.metric,
                color = if (line.ok) c.ink else c.stamp,
            )
        }
    }
}

@Composable
private fun stepLabel(step: DiagStep): String = stringResource(
    when (step) {
        DiagStep.SIGN_UP -> R.string.diag_step_sign_up
        DiagStep.SIGN_IN -> R.string.diag_step_sign_in
        DiagStep.SIGN_OUT -> R.string.diag_step_sign_out
        DiagStep.WRITE -> R.string.diag_step_write
        DiagStep.READ -> R.string.diag_step_read
    },
)

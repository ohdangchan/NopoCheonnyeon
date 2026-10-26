package com.nopo.cheonnyeon.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val QUICK_LANGUAGES = listOf("English", "日本語", "简体中文", "Español", "Français")

/**
 * 가게 소개글·리뷰를 외국인 친구에게 보여 주거나 외국인이 쓸 때: 원하는 언어를 고르거나 직접 입력하면 Gemini가 바로 번역한다.
 * 다시 누르면 원문으로 돌아간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslateBar(container: AppContainer, store: Store) {
    val translator = container.translator
    if (!translator.available) return
    val c = NopoTheme.colors
    val scope = rememberCoroutineScope()
    val translation by translator.state.collectAsStateWithLifecycle()
    val current = translation?.takeIf { it.storeId == store.id }
    var open by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf("") }
    var pendingLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun start(language: String) {
        scope.launch {
            if (container.settings.aiConsent.first()) translator.translate(store.id, language) else pendingLanguage = language
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            AssistChip(onClick = { open = !open }, label = { Text(stringResource(R.string.translate_button)) })
            if (current?.active == true) {
                AssistChip(onClick = { translator.clear() }, label = { Text(stringResource(R.string.translate_original)) })
            }
        }
        if (open) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                QUICK_LANGUAGES.forEach { lang ->
                    FilterChip(selected = current?.language == lang && current.active, onClick = { start(lang) }, label = { Text(lang) })
                }
                FilterChip(selected = false, onClick = { custom = true }, label = { Text(stringResource(R.string.translate_custom)) })
            }
        }
        if (current?.loading == true) Text(stringResource(R.string.translate_loading, current.language),
            style = NopoTheme.typography.bodySmall, color = c.ink2)
        current?.error?.let { Text(it.message(context), style = NopoTheme.typography.bodySmall, color = c.stamp) }
        if (current?.active == true) Text(stringResource(R.string.translate_notice, current.language),
            style = NopoTheme.typography.bodySmall, color = c.ink2)
    }

    if (custom) {
        AlertDialog(
            onDismissRequest = { custom = false },
            title = { Text(stringResource(R.string.translate_custom_title)) },
            text = {
                OutlinedTextField(
                    value = customText, onValueChange = { customText = it.take(30) }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.translate_custom_hint)) },
                    keyboardOptions = KeyboardOptions.Default,
                )
            },
            confirmButton = { TextButton({ custom = false; if (customText.isNotBlank()) start(customText) }) { Text(stringResource(R.string.translate_do)) } },
            dismissButton = { TextButton({ custom = false }) { Text(stringResource(R.string.detail_back)) } },
        )
    }
    pendingLanguage?.let { lang ->
        AiConsentDialog(
            onAgree = { pendingLanguage = null; scope.launch { container.settings.setAiConsent(true); translator.translate(store.id,
                lang) } },
            onDismiss = { pendingLanguage = null },
        )
    }
}

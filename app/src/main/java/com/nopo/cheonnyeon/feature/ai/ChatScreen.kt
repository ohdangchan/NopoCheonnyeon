package com.nopo.cheonnyeon.feature.ai

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.ai.ChatContextInfo
import com.nopo.cheonnyeon.data.ai.ChatMessage
import com.nopo.cheonnyeon.data.ai.ChatStoreRef
import com.nopo.cheonnyeon.data.ai.ChatTurn
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.rule.ChatCandidates
import com.nopo.cheonnyeon.platform.location.FusedLocationAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class ChatUiState(
    val available: Boolean = true,
    val consent: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val error: AppError? = null,
    val remainingYears: Int = Title.MASTER.requiredYears,
    /** 예전 기록 불러오기를 마쳤는가(검색창에서 넘어온 첫 질문은 그 뒤에 보낸다) */
    val loaded: Boolean = false,
)

/**
 * 지도 화면 위쪽에서 여는 AI 노포 도우미. 말로 찾기, 천년 경로 설계, 오늘의 노포 코스를 물어보고 조언을 받는다.
 * 대화는 이 기기의 저장 공간에 쌓여 예전 기록을 모두 볼 수 있다(서버에 올리지 않음).
 */
class ChatViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState(available = container.ai.available))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private var nextId = 0L
    private val owner: String get() = container.auth.currentUid.orEmpty()

    init {
        viewModelScope.launch {
            val history = container.chatStore.load(owner)
            nextId = (history.maxOfOrNull { it.id } ?: 0L) + 1
            val total = container.database.myVisitDao().observeTotalYears().first()
            _state.update {
                it.copy(
                    messages = history, consent = container.settings.aiConsent.first(),
                    remainingYears = (Title.MASTER.requiredYears - total).coerceAtLeast(0), loaded = true,
                )
            }
        }
    }

    fun agree() { viewModelScope.launch { container.settings.setAiConsent(true); _state.update { it.copy(consent = true) } } }

    fun agreeAndSend(text: String) {
        viewModelScope.launch { container.settings.setAiConsent(true); _state.update { it.copy(consent = true) }; send(text) }
    }

    fun send(raw: String) {
        val text = raw.trim().take(300)
        val s = _state.value
        if (text.isEmpty() || s.sending || !s.available) return
        val userMsg = ChatMessage(nextId++, ChatMessage.ROLE_USER, text, container.clock.now().toEpochMilli())
        _state.update { it.copy(messages = it.messages + userMsg, sending = true, error = null) }
        viewModelScope.launch {
            container.chatStore.append(owner, userMsg)
            val info = buildContext(text)
            val turns = _state.value.messages.dropLast(1).map { ChatTurn(it.isUser, it.text) }
            when (val r = container.ai.chat(turns, text, info)) {
                is AppResult.Success -> {
                    val byId = info.candidates.associateBy { it.store.id }
                    val refs = r.value.picks.mapNotNull { (id, reason) -> byId[id]?.let { ChatStoreRef(id, it.store.name, it.years,
                        reason) } }
                    val bot = ChatMessage(nextId++, ChatMessage.ROLE_BOT, r.value.text, container.clock.now().toEpochMilli(), refs)
                    container.chatStore.append(owner, bot)
                    _state.update { it.copy(messages = it.messages + bot, sending = false) }
                }
                is AppResult.Failure -> _state.update { it.copy(sending = false, error = r.error) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { container.chatStore.clear(owner); _state.update { it.copy(messages = emptyList(), error = null) } }
    }

    /** 지금 시각·위치(권한이 있으면)·세월·후보 가게를 모은다. 위치는 이 호출에서만 쓰고 저장하지 않는다 */
    private suspend fun buildContext(text: String): ChatContextInfo {
        val now = container.clock.now().atZone(SeoulZone)
        val nowText = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " (" + listOf("월", "화", "수", "목", "금", "토",
            "일")[now.dayOfWeek.value - 1] + "요일, 서울 시간)"
        val granted = ContextCompat.checkSelfPermission(container.appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(container.appContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fix = container.lastFix.value ?: if (granted) runCatching { FusedLocationAdapter(container.appContext)
            .quickFix() }.getOrNull() else null
        val visits = container.database.myVisitDao().observeAll().first()
        val total = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded }
        val candidates = ChatCandidates.pick(
            container.catalog.all(), visits.map { it.storeId }.toSet(), fix?.let { it.lat to it.lng }, text, now.year,
        )
        return ChatContextInfo(
            nowText = nowText,
            locationText = if (fix != null) "현재 위치 사용 가능(거리 km는 후보 목록에 있음)" else "알 수 없음(위치 권한 없음)",
            totalYears = total,
            candidates = candidates,
        )
    }
}

@Composable
fun ChatScreen(vm: ChatViewModel, initialQuery: String = "", onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var askConsent by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.sending) {
        val last = state.messages.size + (if (state.sending) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }
    // 지도 검색창에서 말로 물어보고 넘어왔으면 그 질문을 바로 보낸다(한 번만)
    var handled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialQuery, state.loaded) {
        if (!handled && state.loaded && initialQuery.isNotBlank()) {
            handled = true
            if (state.consent) vm.send(initialQuery) else askConsent = initialQuery
        }
    }
    fun submit(text: String) {
        if (text.isBlank()) return
        if (!state.consent) { askConsent = text; return }
        input = ""
        vm.send(text)
    }

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = NopoTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text(stringResource(R.string.detail_back)) }
            Text(stringResource(R.string.chat_title), style = NopoTheme.typography.headlineMedium, color = c.primary,
                modifier = Modifier.weight(1f))
            if (state.messages.isNotEmpty()) TextButton({ confirmClear = true }) { Text(stringResource(R.string.chat_clear)) }
        }
        if (!state.available) {
            Text(stringResource(R.string.ai_unavailable), color = c.stamp, modifier = Modifier.padding(NopoTheme.spacing.lg))
            return@Column
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = NopoTheme.spacing.lg),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                        Text(stringResource(R.string.chat_intro), style = NopoTheme.typography.bodyMedium, color = c.ink2)
                        Text(stringResource(R.string.chat_history_note), style = NopoTheme.typography.bodySmall, color = c.ink2)
                    }
                }
            }
            items(state.messages, key = { it.id }) { m -> Bubble(m, onOpenStore) }
            if (state.sending) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = c.primary, strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.chat_thinking), color = c.ink, style = NopoTheme.typography.titleMedium)
                }
            }
            state.error?.let { e -> item { Text(e.message(context), color = c.stamp) } }
        }
        // 예시 질문: 눌러서 바로 물어볼 수 있다(접고 펼칠 수 있음)
        var showSamples by rememberSaveable { mutableStateOf(true) }
        Column(Modifier.fillMaxWidth().padding(horizontal = NopoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { showSamples = !showSamples },
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.chat_samples_title), style = NopoTheme.typography.labelMedium, color = c.primary,
                    modifier = Modifier.weight(1f))
                Text(stringResource(if (showSamples) R.string.chat_samples_hide else R.string.chat_samples_show),
                    style = NopoTheme.typography.labelMedium, color = c.ink2)
            }
            if (showSamples) {
                listOf(
                    stringResource(R.string.chat_sample_find),
                    stringResource(R.string.chat_sample_route, state.remainingYears),
                    stringResource(R.string.chat_sample_today),
                ).forEach { sample ->
                    Text(
                        sample, style = NopoTheme.typography.bodyMedium, color = c.ink,
                        modifier = Modifier.fillMaxWidth().background(c.tealSoft, NopoTheme.shapes.md)
                            .clickable(enabled = !state.sending, role = Role.Button) { submit(sample) }
                            .padding(horizontal = NopoTheme.spacing.md, vertical = NopoTheme.spacing.sm),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(NopoTheme.spacing.md), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(300) },
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                modifier = Modifier.weight(1f).background(c.surface, NopoTheme.shapes.md),
                maxLines = 3,
            )
            NopoButton(stringResource(R.string.chat_send), { submit(input) }, enabled = input.isNotBlank() && !state.sending)
        }
    }
    askConsent?.let { pending ->
        AiConsentDialog(onAgree = { askConsent = null; input = ""; vm.agreeAndSend(pending) }, onDismiss = { askConsent = null })
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.chat_clear_title)) },
            text = { Text(stringResource(R.string.chat_clear_body)) },
            confirmButton = { TextButton({ confirmClear = false; vm.clearHistory() }) { Text(stringResource(R.string.chat_clear)) } },
            dismissButton = { TextButton({ confirmClear = false }) { Text(stringResource(R.string.detail_back)) } },
        )
    }
}

@Composable
private fun Bubble(m: ChatMessage, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        if (m.text.isNotBlank()) {
            Text(
                m.text,
                style = NopoTheme.typography.bodyMedium,
                color = if (m.isUser) c.onPrimary else c.ink,
                modifier = Modifier.background(if (m.isUser) c.primary else c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
            )
        }
        m.stores.forEachIndexed { i, s ->
            Column(
                Modifier.fillMaxWidth().background(c.tealSoft,
                    NopoTheme.shapes.md).clickable(role = Role.Button) { onOpenStore(s.id) }.padding(NopoTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("${i + 1}. ${s.name} · ${stringResource(R.string.years_value, s.years)}", style = NopoTheme.typography.titleSmall,
                    color = c.primary)
                if (s.reason.isNotBlank()) Text(s.reason, style = NopoTheme.typography.bodySmall, color = c.ink)
            }
        }
    }
}


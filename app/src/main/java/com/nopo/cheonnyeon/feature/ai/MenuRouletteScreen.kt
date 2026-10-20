package com.nopo.cheonnyeon.feature.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.message
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.ai.TasteInput
import com.nopo.cheonnyeon.data.ai.TasteReview
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.MenuPick
import com.nopo.cheonnyeon.domain.rule.MenuRoulette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RouletteUiState(
    val available: Boolean = true,
    val consent: Boolean = false,
    val loading: Boolean = false,
    /** 지금까지 돌린 라운드(최대 10, 한 라운드 3개). 서로 다른 메뉴 최대 30개 */
    val rounds: List<List<MenuPick>> = emptyList(),
    val summary: String = "",
    val error: AppError? = null,
    /** 분석에 쓴 리뷰·사진 수 */
    val usedReviews: Int = 0,
    val usedPhotos: Int = 0,
    /** 추천 메뉴 → 그 메뉴가 있는 노포 */
    val storesByMenu: Map<String, List<Store>> = emptyMap(),
) {
    val latest: List<MenuPick> get() = rounds.lastOrNull().orEmpty()
    val total: Int get() = rounds.sumOf { it.size }
    val canSpin: Boolean get() = MenuRoulette.canSpin(rounds)
}

/** AI 메뉴 추천 룰렛: 내 리뷰·음식 사진을 분석해 메뉴 3개를 추천하고, 마음에 안 들면 다시 돌린다(최대 10번, 총 30개) */
class MenuRouletteViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(RouletteUiState(available = container.ai.available))
    val state: StateFlow<RouletteUiState> = _state.asStateFlow()
    private var taste: TasteInput? = null

    init {
        viewModelScope.launch { _state.update { it.copy(consent = container.settings.aiConsent.first()) } }
    }

    /** 동의를 저장하고 바로 첫 룰렛을 돌린다 */
    fun agreeAndSpin() {
        viewModelScope.launch {
            container.settings.setAiConsent(true)
            _state.update { it.copy(consent = true) }
            spin()
        }
    }

    fun spin() {
        val s = _state.value
        if (s.loading || !s.canSpin || !s.consent || !s.available) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val input = taste ?: loadTaste().also { taste = it }
            _state.update { it.copy(usedReviews = input.reviews.size, usedPhotos = input.photos.size) }
            when (val r = container.ai.recommendMenus(input, _state.value.rounds)) {
                is AppResult.Success -> {
                    val stores = matchStores(r.value.picks)
                    _state.update {
                        it.copy(
                            loading = false, rounds = it.rounds + listOf(r.value.picks),
                            summary = r.value.summary.ifBlank { it.summary }, storesByMenu = it.storesByMenu + stores,
                        )
                    }
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** 30개를 다 본 뒤 처음부터 다시 */
    fun reset() = _state.update { it.copy(rounds = emptyList(), summary = "", error = null, storesByMenu = emptyMap()) }

    /** 내 리뷰(서버·시험용 가게·시연용)와 음식 사진, 없으면 방문한 가게의 메뉴로 취향 입력을 만든다 */
    private suspend fun loadTaste(): TasteInput {
        val catalog = container.catalog.all().associateBy { it.id }
        val nick = container.auth.currentNickname().orEmpty()
        val items = ArrayList<com.nopo.cheonnyeon.data.remote.ReviewItem>()
        container.auth.currentUid?.let { uid -> items += container.reviews.byUser(uid).valueOrNull.orEmpty() }
        catalog.values.filter { it.isTest }.forEach { t ->
            container.settings.testReviews(t.id).forEach { r ->
                items += com.nopo.cheonnyeon.data.remote.ReviewItem(r.id, t.id, t.name, "", nick, "", r.rating, r.text, 1, r.at, photos = r.photos)
            }
        }
        if (container.settings.demoData.first() && container.database.myVisitDao().demoVisitCount() > 0) {
            items += container.demoReviews.myReviews(nick)
        }
        val sorted = items.distinctBy { it.id }.sortedByDescending { it.updatedAtMillis }
        var reviews = sorted.map { r ->
            val st = catalog[r.storeId]
            TasteReview(st?.name ?: r.storeName, st?.menuItems.orEmpty(), r.rating, r.text)
        }
        if (reviews.isEmpty()) {
            // 리뷰가 없으면 방문(도장)한 가게의 메뉴로 대신한다
            reviews = container.visits.visitRefs().mapNotNull { catalog[it.storeId] }.filter { it.menuItems.isNotEmpty() }
                .map { TasteReview(it.name, it.menuItems, 4, "") }
        }
        val photos = sorted.flatMap { it.photos }.filter { !it.startsWith("asset:") }.take(6)
        return TasteInput(reviews, photos)
    }

    private suspend fun matchStores(picks: List<MenuPick>): Map<String, List<Store>> {
        val foods = container.catalog.all().filter { it.isFood && !it.isTest && it.menuItems.isNotEmpty() }
        return picks.associate { p ->
            val k = MenuRoulette.key(p.menu)
            p.menu to foods.filter { s -> s.menuItems.any { m -> val mk = MenuRoulette.key(m); mk.length >= 2 && k.length >= 2 && (mk.contains(k) || k.contains(mk)) } }
                .sortedBy { it.foundedYear ?: Int.MAX_VALUE }.take(3)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuRouletteScreen(vm: MenuRouletteViewModel, onBack: () -> Unit, onOpenStore: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var askConsent by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = NopoTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text(stringResource(R.string.detail_back)) }
            Text(stringResource(R.string.roulette_title), style = NopoTheme.typography.headlineMedium, color = c.primary)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = NopoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wheel(spinning = state.loading, Modifier.size(200.dp))
            Text(stringResource(R.string.roulette_intro), style = NopoTheme.typography.bodyMedium, color = c.ink2)
            if (!state.available) {
                Text(stringResource(R.string.ai_unavailable), color = c.stamp)
            } else {
                if (state.usedReviews > 0 || state.usedPhotos > 0) {
                    Text(stringResource(R.string.roulette_used, state.usedReviews, state.usedPhotos), style = NopoTheme.typography.bodySmall, color = c.ink2)
                }
                if (state.summary.isNotBlank()) Text(state.summary, style = NopoTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.fillMaxWidth().background(c.tealSoft, NopoTheme.shapes.md).padding(NopoTheme.spacing.md))
                state.error?.let { Text(it.message(context), color = c.stamp) }
                state.latest.forEachIndexed { i, p -> PickCard(i + 1, p, state.storesByMenu[p.menu].orEmpty(), onOpenStore) }
                if (state.rounds.isNotEmpty()) {
                    Text(stringResource(R.string.roulette_seen, state.total, MenuRoulette.MAX_MENUS), style = NopoTheme.typography.labelMedium, color = c.ink2)
                    if (state.rounds.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.rounds.dropLast(1).flatten().forEach { Text(it.menu, style = NopoTheme.typography.labelSmall, color = c.ink2, modifier = Modifier.background(c.surface, NopoTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp)) }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(c.surface).padding(NopoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            when {
                !state.available -> Unit
                !state.canSpin -> {
                    Text(stringResource(R.string.roulette_done, MenuRoulette.MAX_MENUS), style = NopoTheme.typography.bodyMedium, color = c.ink)
                    NopoButton(stringResource(R.string.roulette_reset), vm::reset, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                }
                else -> NopoButton(
                    stringResource(
                        when {
                            state.loading -> R.string.roulette_spinning
                            state.rounds.isEmpty() -> R.string.roulette_spin
                            else -> R.string.roulette_again
                        },
                        state.rounds.size + 1, MenuRoulette.MAX_ROUNDS,
                    ),
                    { if (state.consent) vm.spin() else askConsent = true },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (askConsent) AiConsentDialog(onAgree = { askConsent = false; vm.agreeAndSpin() }, onDismiss = { askConsent = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickCard(n: Int, p: MenuPick, stores: List<Store>, onOpenStore: (String) -> Unit) {
    val c = NopoTheme.colors
    Column(Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            Box(Modifier.size(28.dp).background(c.stamp, NopoTheme.shapes.pill), contentAlignment = Alignment.Center) { Text(n.toString(), color = c.onStamp, style = NopoTheme.typography.labelMedium) }
            Text(p.menu, style = NopoTheme.typography.titleLarge, color = c.ink)
        }
        if (p.reason.isNotBlank()) Text(p.reason, style = NopoTheme.typography.bodyMedium, color = c.ink)
        if (p.ingredients.isNotEmpty()) {
            Text(stringResource(R.string.roulette_ingredients), style = NopoTheme.typography.labelMedium, color = c.ink2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.ingredients.forEach { Text(it, style = NopoTheme.typography.labelSmall, color = c.onPrimary, modifier = Modifier.background(c.primary, NopoTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp)) }
            }
        }
        if (stores.isNotEmpty()) {
            Text(stringResource(R.string.roulette_stores), style = NopoTheme.typography.labelMedium, color = c.ink2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                stores.forEach { s -> TextButton({ onOpenStore(s.id) }) { Text(s.name) } }
            }
        }
    }
}

/** 돌아가는 룰렛판. 분석하는 동안 빠르게 돌고, 끝나면 서서히 멈춘다 */
@Composable
private fun Wheel(spinning: Boolean, modifier: Modifier = Modifier) {
    val c = NopoTheme.colors
    val angle = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) angle.animateTo(angle.value + 360f, tween(800, easing = LinearEasing))
        } else if (angle.value != 0f) {
            angle.animateTo(angle.value + 540f, tween(1500, easing = FastOutSlowInEasing))
        }
    }
    val colors = listOf(c.brass, c.stamp, c.primary, c.tealSoft, c.brassSoft, c.sign)
    Canvas(modifier) {
        val r = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        rotate(angle.value, center) {
            val sweep = 360f / colors.size
            colors.forEachIndexed { i, col ->
                drawArc(col, i * sweep, sweep, true, topLeft = Offset(center.x - r * 0.92f, center.y - r * 0.92f), size = Size(r * 1.84f, r * 1.84f))
            }
        }
        drawCircle(c.paper, r * 0.16f, center)
        // 위쪽 바늘
        val tip = Path().apply {
            moveTo(center.x, center.y - r * 0.8f)
            lineTo(center.x - r * 0.09f, center.y - r * 1.02f)
            lineTo(center.x + r * 0.09f, center.y - r * 1.02f)
            close()
        }
        drawPath(tip, c.ink)
    }
}

/** 리뷰 글·음식 사진이 Google Gemini로 전송된다는 안내와 동의 (처음 한 번) */
@Composable
fun AiConsentDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_consent_title)) },
        text = { Text(stringResource(R.string.ai_consent_body)) },
        confirmButton = { TextButton(onAgree) { Text(stringResource(R.string.ai_consent_agree)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.detail_back)) } },
    )
}

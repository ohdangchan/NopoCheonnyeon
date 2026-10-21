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
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
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
    /** 룰렛 화면에서 흘러가는 사진: 내 리뷰 사진(없으면 내가 간 가게의 대표·리뷰 사진). Coil 모델(주소 문자열 또는 바이트) */
    val reel: List<Any> = emptyList(),
    /** [reel]이 내 리뷰 사진인지(true) 가게 사진 대체인지(false) */
    val reelIsMine: Boolean = true,
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
        viewModelScope.launch {
            val (photos, mine) = loadReel()
            _state.update { it.copy(reel = photos, reelIsMine = mine) }
        }
    }

    /** 룰렛에서 흘러갈 사진 모으기: 내 리뷰 사진, 없으면 내가 간 가게의 대표 사진·리뷰 사진 */
    private suspend fun loadReel(): Pair<List<Any>, Boolean> {
        val mine = collectMyReviews().sortedByDescending { it.updatedAtMillis }
        val own = mine.flatMap { it.photos }.mapNotNull(::photoModel).distinct()
        if (own.isNotEmpty()) return own.take(MAX_REEL) to true
        val catalog = container.catalog.all().associateBy { it.id }
        val visited = container.visits.visitRefs().mapNotNull { catalog[it.storeId] }
        val fallback = visited.flatMap { s ->
            s.photoUrls.take(1) + (if (container.settings.demoData.first()) container.demoReviews.forStore(s.id).flatMap { it.photos }.take(2).mapNotNull(::photoModelString) else emptyList())
        }.distinct()
        return fallback.take(MAX_REEL) to false
    }

    private fun photoModelString(p: String): String? = (photoModel(p) as? String)

    /** 리뷰 사진 문자열 → Coil 모델(시연용 "asset:" 경로는 주소, 실제 사진은 바이트) */
    private fun photoModel(p: String): Any? =
        if (p.startsWith("asset:")) "file:///android_asset/" + p.removePrefix("asset:") else com.nopo.cheonnyeon.platform.camera.ReviewPhotoCodec.decode(p)

    /** 내 리뷰(서버·시험용 가게·시연용) 전부 */
    private suspend fun collectMyReviews(): List<com.nopo.cheonnyeon.data.remote.ReviewItem> {
        val nick = container.auth.currentNickname().orEmpty()
        val items = ArrayList<com.nopo.cheonnyeon.data.remote.ReviewItem>()
        container.auth.currentUid?.let { uid -> items += container.reviews.byUser(uid).valueOrNull.orEmpty() }
        container.catalog.all().filter { it.isTest }.forEach { t ->
            container.settings.testReviews(t.id).forEach { r ->
                items += com.nopo.cheonnyeon.data.remote.ReviewItem(r.id, t.id, t.name, "", nick, "", r.rating, r.text, 1, r.at, photos = r.photos)
            }
        }
        if (container.settings.demoData.first() && container.database.myVisitDao().demoVisitCount() > 0) {
            items += container.demoReviews.myReviews(nick)
        }
        return items.distinctBy { it.id }
    }

    /** 시연용 사진(자산)도 AI에 보낼 수 있게 줄여서 base64 WebP로 */
    private suspend fun assetBase64(path: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            container.appContext.assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }
                ?.let { com.nopo.cheonnyeon.platform.camera.ReviewPhotoCodec.encode(it, 0) }
        }.getOrNull()
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
        val sorted = collectMyReviews().sortedByDescending { it.updatedAtMillis }
        var reviews = sorted.map { r ->
            val st = catalog[r.storeId]
            TasteReview(st?.name ?: r.storeName, st?.menuItems.orEmpty(), r.rating, r.text)
        }
        if (reviews.isEmpty()) {
            // 리뷰가 없으면 방문(도장)한 가게의 메뉴로 대신한다
            reviews = container.visits.visitRefs().mapNotNull { catalog[it.storeId] }.filter { it.menuItems.isNotEmpty() }
                .map { TasteReview(it.name, it.menuItems, 4, "") }
        }
        // 최근 사진부터 최대 6장(시연용 자산 사진은 줄여서 함께 보낸다)
        val photos = sorted.flatMap { it.photos }.take(MAX_AI_PHOTOS).mapNotNull { p ->
            if (p.startsWith("asset:")) assetBase64(p.removePrefix("asset:")) else p
        }
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

private const val MAX_REEL = 16
private const val MAX_AI_PHOTOS = 6

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
            if (state.reel.isNotEmpty()) {
                PhotoReel(state.reel, state.loading, Modifier.fillMaxWidth().height(250.dp))
                Text(
                    stringResource(if (state.reelIsMine) R.string.roulette_reel_mine else R.string.roulette_reel_visited, state.reel.size),
                    style = NopoTheme.typography.labelMedium, color = c.primary,
                )
            } else {
                Wheel(spinning = state.loading, Modifier.size(200.dp))
            }
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

/**
 * 룰렛 화면의 사진 릴: 내 리뷰 사진이 두 줄로 반대 방향으로 흘러가고, AI가 분석하는 동안은 빠르게 돌다가 끝나면 천천히 멈춘다.
 * 가운데 틀과 레이저 선이 "이 사진들을 AI가 보고 있다"는 느낌을 준다.
 */
@Composable
private fun PhotoReel(photos: List<Any>, spinning: Boolean, modifier: Modifier = Modifier) {
    val c = NopoTheme.colors
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemW = 112.dp
    val gap = 8.dp
    val unit = with(density) { (itemW + gap).toPx() }
    var pos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val speed = androidx.compose.runtime.remember { Animatable(45f) }
    LaunchedEffect(spinning) {
        speed.animateTo(if (spinning) 2200f else 45f, tween(if (spinning) 600 else 1800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L) pos += speed.value * (t - last) / 1_000_000_000f
                last = t
            }
        }
    }
    val sweep = androidx.compose.animation.core.rememberInfiniteTransition(label = "sweep")
    val scan by sweep.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(900, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "scan")
    // 한 줄은 사진을 여러 번 이어 붙여 끊김 없이 돈다
    val reps = maxOf(2, kotlin.math.ceil(14f / photos.size).toInt() + 1)
    val loop = unit * photos.size
    fun rowPhotos(shift: Int) = List(photos.size * reps) { photos[(it + shift) % photos.size] }
    Box(modifier.clip(NopoTheme.shapes.md).background(c.sign)) {
        Column(Modifier.fillMaxSize().padding(vertical = NopoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(gap)) {
            listOf(0 to -1, photos.size / 2 to 1).forEach { (shift, dir) ->
                Box(Modifier.fillMaxWidth().height(itemW).clip(NopoTheme.shapes.md)) {
                    val x = if (dir < 0) -(pos % loop) else -loop + ((pos * 0.85f) % loop)
                    Row(
                        Modifier.wrapContentWidth(Alignment.Start, unbounded = true).offset { androidx.compose.ui.unit.IntOffset(x.toInt(), 0) },
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        rowPhotos(shift).forEach { m ->
                            coil3.compose.AsyncImage(
                                model = m, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(itemW).clip(NopoTheme.shapes.sm),
                            )
                        }
                    }
                }
            }
        }
        // 가운데 분석 틀과 레이저
        Canvas(Modifier.fillMaxSize()) {
            val fw = itemW.toPx() + 28.dp.toPx()
            val left = (size.width - fw) / 2
            drawRoundRect(c.laser.copy(alpha = if (spinning) 0.9f else 0.5f), androidx.compose.ui.geometry.Offset(left, 6.dp.toPx()), Size(fw, size.height - 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            if (spinning) {
                val y = 6.dp.toPx() + (size.height - 12.dp.toPx()) * scan
                drawLine(c.laser, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(left + fw, y), 3.dp.toPx())
            }
            // 양쪽 가장자리를 어둡게
            drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.sign, androidx.compose.ui.graphics.Color.Transparent), 0f, 90.dp.toPx()), size = Size(90.dp.toPx(), size.height))
            drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, c.sign), size.width - 90.dp.toPx(), size.width), androidx.compose.ui.geometry.Offset(size.width - 90.dp.toPx(), 0f), Size(90.dp.toPx(), size.height))
        }
    }
}

package com.nopo.cheonnyeon.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nopo.cheonnyeon.core.designsystem.component.StarRating
import com.nopo.cheonnyeon.core.designsystem.component.StarRatingInput
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.data.remote.ReviewItem
import com.nopo.cheonnyeon.data.remote.ReviewPage
import com.nopo.cheonnyeon.data.remote.ReviewSummary
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.ReviewCheck
import com.nopo.cheonnyeon.domain.rule.ReviewInvalid
import com.nopo.cheonnyeon.domain.rule.ReviewRules
import com.nopo.cheonnyeon.domain.rule.TitlePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 리뷰를 쓸 수 있는 자격 (FR-STORE-04) */
enum class ReviewEligibility { NOT_VISITED, SYNCING, OK }

data class ReviewsUiState(
    val loading: Boolean = true,
    val summary: ReviewSummary? = null,
    val items: List<ReviewItem> = emptyList(),
    val hasMore: Boolean = false,
    /** 이 가게에 내가 남긴 리뷰들(최신순) */
    val mine: List<ReviewItem> = emptyList(),
    val eligibility: ReviewEligibility = ReviewEligibility.NOT_VISITED,
    /** 오늘 이 가게를 실제로 인증했는지(시연용 도장은 제외). 아니면 리뷰 전에 방문 인증을 먼저 한다 */
    val verifiedToday: Boolean = false,
    val error: AppError? = null,
    val saving: Boolean = false,
    val reported: Boolean = false,
    val stats: com.nopo.cheonnyeon.data.remote.StoreStats? = null,
    /** 시연용 예시 리뷰(실제 리뷰 뒤에 표시) */
    val demoItems: List<ReviewItem> = emptyList(),
    /** 시험용 가게: 리뷰가 서버가 아니라 이 기기에만 저장된다 */
    val localOnly: Boolean = false,
    /** AI 리뷰 분석 */
    val insight: com.nopo.cheonnyeon.data.ai.StoreInsight? = null,
    val insightLoading: Boolean = false,
    val insightError: AppError? = null,
    /** 단골 충성도 점수(AI) */
    val loyalty: com.nopo.cheonnyeon.data.ai.LoyaltyAnalysis? = null,
    val loyaltyLoading: Boolean = false,
    val loyaltyError: AppError? = null,
)

class ReviewsViewModel(private val container: AppContainer, private val store: Store) : ViewModel() {
    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()
    private var lastPage: ReviewPage? = null

    /** 리뷰 편집 중 고른 사진. 화면을 돌려도 ViewModel이 살아 있어 유지된다(Bundle에는 용량이 커서 넣지 않음). 저장·닫기 때 비운다 */
    var draftPhotos: List<String>? = null

    val myUid: String? get() = container.auth.currentUid
    val storeId: String get() = store.id
    val pendingReview get() = container.pendingReviewStoreId
    fun clearPendingReview() { container.pendingReviewStoreId.value = null }

    init {
        refresh()
        // 인증을 마치고 돌아오면 이 ViewModel은 그대로라 자격이 예전 값으로 남는다. 방문 기록이 생기거나 확인됨으로 바뀌면 다시 불러온다.
        viewModelScope.launch {
            container.database.myVisitDao().observeAll()
                .map { list -> list.firstOrNull { it.storeId == store.id }?.let { it.synced to it.visitCount } }
                .distinctUntilChanged()
                .drop(1)
                .collect { refresh() }
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (store.isTest) { refreshTestStore(); return@launch }
            val visit = container.database.myVisitDao().byStore(store.id)
            val eligibility = when {
                visit == null -> ReviewEligibility.NOT_VISITED
                !visit.synced -> ReviewEligibility.SYNCING
                else -> ReviewEligibility.OK
            }
            val summary = container.reviews.summary(store.id)
            val page = container.reviews.page(store.id)
            val stats = container.storeStats.get(store.id).valueOrNull
            val mine = if (eligibility == ReviewEligibility.OK) container.reviews.mine(store.id).valueOrNull.orEmpty() else emptyList()
            lastPage = page.valueOrNull
            val demoOn = container.settings.demoData.first()
            val demo = if (demoOn) {
                // 시연용 패스포트로 이 가게에 남긴 내 리뷰가 있으면 맨 앞에 둔다
                val mineDemo = if (container.database.myVisitDao().demoVisitCount() > 0) container.demoReviews.myReviews(container.auth.currentNickname().orEmpty(), store.id) else emptyList()
                mineDemo + container.demoReviews.forStore(store.id)
            } else emptyList()
            _state.update {
                it.copy(
                    loading = false,
                    summary = com.nopo.cheonnyeon.data.remote.DemoReviews.merge(summary.valueOrNull, demo),
                    demoItems = demo,
                    items = page.valueOrNull?.items.orEmpty(),
                    hasMore = page.valueOrNull?.hasMore ?: false,
                    mine = mine,
                    stats = mergeDemoStats(stats, demo),
                    eligibility = eligibility,
                    verifiedToday = isVerifiedToday(visit),
                    error = summary.errorOrNull ?: page.errorOrNull,
                )
            }
        }
    }

    /** 시험용 가게는 서버에 없다: 방문 인증을 해야 리뷰를 쓸 수 있고, 내 리뷰는 이 기기에만 저장하며, 나머지는 시연용 예시다 */
    private suspend fun refreshTestStore() {
        val demo = if (container.settings.demoData.first()) container.demoReviews.forStore(store.id) else emptyList()
        val visited = container.database.myVisitDao().byStore(store.id) != null
        val own = if (visited) loadTestReviews() else emptyList()
        val summary = if (own.isEmpty()) com.nopo.cheonnyeon.data.remote.DemoReviews.merge(null, demo)
        else com.nopo.cheonnyeon.data.remote.DemoReviews.merge(ReviewSummary(own.size, own.sumOf { it.rating }.toDouble() / own.size), demo)
        _state.update {
            it.copy(
                loading = false, summary = summary, demoItems = demo, items = own, mine = own, hasMore = false, localOnly = true,
                stats = mergeDemoStats(null, demo),
                eligibility = if (visited) ReviewEligibility.OK else ReviewEligibility.NOT_VISITED, error = null,
                verifiedToday = isVerifiedToday(container.database.myVisitDao().byStore(store.id)),
            )
        }
    }

    /** 오늘(서울 날짜) 실제로 인증한 기록이 있는가. 시연용 도장(DEMO)은 인증으로 치지 않는다 */
    private fun isVerifiedToday(visit: com.nopo.cheonnyeon.data.local.MyVisitEntity?): Boolean =
        visit != null && visit.method != com.nopo.cheonnyeon.data.remote.DemoReviews.METHOD_DEMO &&
            java.time.Instant.ofEpochMilli(visit.lastVisitedAt).atZone(com.nopo.cheonnyeon.core.common.SeoulZone).toLocalDate() == java.time.LocalDate.now(com.nopo.cheonnyeon.core.common.SeoulZone)

    /** "리뷰 남기기": 아직 오늘 인증하지 않았으면 인증 화면으로 보내고, 인증에 성공하면 편집기가 열리게 표시해 둔다 */
    fun requestVerifyForReview() { container.reviewIntentStoreId.value = store.id }

    private suspend fun loadTestReviews(): List<ReviewItem> {
        val visits = container.database.myVisitDao().byStore(store.id)?.visitCount ?: 1
        val nickname = container.auth.currentNickname().orEmpty()
        val uid = container.auth.currentUid.orEmpty()
        return container.settings.testReviews(store.id).map { r ->
            ReviewItem(
                id = r.id, storeId = store.id, storeName = store.name, uid = uid, nickname = nickname, titleId = "",
                rating = r.rating, text = r.text, visitCount = visits, updatedAtMillis = r.at, photos = r.photos,
            )
        }
    }

    val aiAvailable: Boolean get() = container.ai.available
    val translation: StateFlow<com.nopo.cheonnyeon.data.ai.StoreTranslation?> get() = container.translator.state
    /** 보이는 리뷰 글을 번역 대상으로 등록한다 */
    fun registerForTranslation(texts: List<String>) = container.translator.addSources(store.id, texts)
    val aiConsent get() = container.settings.aiConsent
    fun agreeAi() { viewModelScope.launch { container.settings.setAiConsent(true) } }

    /** 이 가게의 다른 이용자 리뷰(시연용 포함)를 AI가 분석한다 */
    fun analyze() {
        val s = _state.value
        if (s.insightLoading) return
        _state.update { it.copy(insightLoading = true, insightError = null) }
        viewModelScope.launch {
            val reviews = (s.items + s.demoItems).filter { it.uid != myUid || it.demo }.map { com.nopo.cheonnyeon.data.ai.InsightReview(it.rating, it.text) }
            when (val r = container.ai.analyzeStore(store.id, store.name, store.menuItems, reviews)) {
                is AppResult.Success -> _state.update { it.copy(insightLoading = false, insight = r.value) }
                is AppResult.Failure -> _state.update { it.copy(insightLoading = false, insightError = r.error) }
            }
        }
    }

    /** 이 가게의 재방문 지표를 다른 가게들과 견줘 AI가 단골 충성도를 100점 만점으로 평가한다 */
    fun analyzeLoyalty() {
        val s = _state.value
        if (s.loyaltyLoading) return
        _state.update { it.copy(loyaltyLoading = true, loyaltyError = null) }
        viewModelScope.launch {
            val all = s.items + s.demoItems
            val visitCounts = all.map { it.visitCount }
            val stats = s.stats
            val input = com.nopo.cheonnyeon.domain.rule.LoyaltyInput(
                visitors = stats?.visitors ?: all.size,
                revisitors = stats?.revisitors ?: all.count { it.visitCount >= 2 },
                avgVisitCount = if (visitCounts.isEmpty()) 1.0 else visitCounts.average(),
                reviewCount = all.size,
            )
            // 비교 대상: 시연용 예시 데이터에 있는 다른 가게들(리뷰 5개 이상)
            val peers = container.demoReviews.load().byStore.filter { (id, list) -> id != store.id && list.size >= 5 }.map { (_, list) ->
                com.nopo.cheonnyeon.domain.rule.LoyaltyInput(list.size, list.count { it.visitCount >= 2 }, list.map { it.visitCount }.average(), list.size)
            }
            val avg = s.summary?.average
            when (val r = container.ai.loyalty(store.name, input, peers, avg)) {
                is AppResult.Success -> _state.update { it.copy(loyaltyLoading = false, loyalty = r.value) }
                is AppResult.Failure -> _state.update { it.copy(loyaltyLoading = false, loyaltyError = r.error) }
            }
        }
    }

    fun loadMore() {
        val current = lastPage ?: return
        viewModelScope.launch {
            val next = container.reviews.page(store.id, current)
            next.valueOrNull?.let { p ->
                lastPage = p
                _state.update { it.copy(items = it.items + p.items, hasMore = p.hasMore) }
            }
        }
    }

    fun report(reviewId: String, reason: String) {
        viewModelScope.launch {
            val r = container.reviews.report(reviewId, reason)
            _state.update { it.copy(error = r.errorOrNull, reported = r is AppResult.Success) }
        }
    }

    /** [reviewId]가 null이면 새 리뷰로 쌓고, 있으면 그 리뷰를 고친다 */
    fun save(reviewId: String?, rating: Int, text: String, photos: List<String>, onDone: () -> Unit) {
        if (_state.value.saving) return
        if (store.isTest) {
            viewModelScope.launch {
                val check = com.nopo.cheonnyeon.domain.rule.ReviewRules.validate(rating, text)
                if (check is ReviewCheck.Valid) {
                    val now = container.clock.now().toEpochMilli()
                    container.settings.saveTestReview(store.id, com.nopo.cheonnyeon.data.local.TestReview(reviewId ?: "local_$now", check.rating, now, check.text, photos))
                    com.nopo.cheonnyeon.core.common.Analytics.reviewSaved(check.text.isNotBlank())
                    onDone(); refresh()
                }
            }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val visit = container.database.myVisitDao().byStore(store.id)
            val total = container.database.myVisitDao().observeTotalYears().first()
            val result = container.reviews.save(
                store.id, store.name, rating, text, visit?.visitCount ?: 1, TitlePolicy.titleFor(total).name, reviewId = reviewId, photos = photos,
            )
            _state.update { it.copy(saving = false, error = result.errorOrNull) }
            if (result is AppResult.Success) { com.nopo.cheonnyeon.core.common.Analytics.reviewSaved(text.isNotBlank()); onDone(); refresh() }
        }
    }

    fun delete(reviewId: String, onDone: () -> Unit) {
        if (_state.value.saving) return
        if (store.isTest) {
            viewModelScope.launch { container.settings.deleteTestReview(store.id, reviewId); onDone(); refresh() }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = container.reviews.delete(reviewId)
            _state.update { it.copy(saving = false, error = result.errorOrNull) }
            if (result is AppResult.Success) { onDone(); refresh() }
        }
    }
}

/** 시연용 예시 리뷰어를 방문자로, 단골 2회 이상을 재방문자로 세어 서버 집계에 더한다 */
internal fun mergeDemoStats(server: com.nopo.cheonnyeon.data.remote.StoreStats?, demo: List<ReviewItem>): com.nopo.cheonnyeon.data.remote.StoreStats? {
    if (demo.isEmpty()) return server
    val people = demo.filter { it.uid == com.nopo.cheonnyeon.data.remote.DemoReviews.DEMO_UID }
    if (people.isEmpty()) return server
    return com.nopo.cheonnyeon.data.remote.StoreStats(
        visitors = (server?.visitors ?: 0) + people.size,
        revisitors = (server?.revisitors ?: 0) + people.count { it.visitCount >= 2 },
    )
}

/** 평점 요약 + 리뷰 목록 + 평점 남기기 (S-506, S-802, FR-STORE-03·04) */
@Composable
fun StoreReviewsSection(vm: ReviewsViewModel, onOpenProfile: (String) -> Unit, onVerify: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    /** 고치는 중인 내 리뷰 ID. null이면 새 리뷰를 쓰는 중 */
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var reportTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val myUid = vm.myUid
    val pending by vm.pendingReview.collectAsStateWithLifecycle()
    LaunchedEffect(pending, state.loading, state.eligibility) {
        if (pending == vm.storeId && !state.loading && state.eligibility == ReviewEligibility.OK) {
            editId = null
            editing = true
            vm.clearPendingReview()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
        Text(stringResource(R.string.reviews_title), style = NopoTheme.typography.titleLarge, color = c.ink)
        state.summary?.let { s ->
            if (s.count == 0 || s.average == null) {
                Text(stringResource(R.string.reviews_none), color = c.ink2)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                    StarRating(s.average.toFloat())
                    Text(stringResource(R.string.reviews_summary, s.average, s.count), style = NopoTheme.typography.bodyMedium, color = c.ink)
                }
            }
        }
        state.stats?.takeIf { it.visitors > 0 }?.let { st ->
            val pct = com.nopo.cheonnyeon.domain.rule.RevisitStats.percent(st.visitors, st.revisitors)
            Text(
                if (pct != null) stringResource(R.string.revisit_line_percent, st.visitors, st.revisitors, pct) else stringResource(R.string.revisit_line, st.visitors, st.revisitors),
                style = NopoTheme.typography.bodyMedium, color = c.ink,
            )
        }
        state.error?.let { Text(it.message(LocalContext.current), color = c.stamp) }
        AiInsightSection(vm, state)
        LoyaltySection(vm, state)

        // 리뷰는 방문 인증(영수증)을 한 뒤에만 쓴다: 오늘 인증했으면 바로 편집기, 아니면 인증 화면으로
        val canWriteNow = state.eligibility == ReviewEligibility.OK && state.verifiedToday
        NopoButton(
            stringResource(R.string.reviews_write),
            { if (canWriteNow) { editId = null; editing = true } else { vm.requestVerifyForReview(); onVerify() } },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!canWriteNow) Text(stringResource(R.string.reviews_verify_first), style = NopoTheme.typography.bodySmall, color = c.ink2)
        if (state.eligibility == ReviewEligibility.SYNCING) Text(AppError.E_VISIT_SYNCING.message(LocalContext.current), style = NopoTheme.typography.bodySmall, color = c.ink2)

        if (state.localOnly) Text(stringResource(R.string.reviews_test_store), style = NopoTheme.typography.bodySmall, color = c.ink2)
        val translation by vm.translation.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(state.items, state.demoItems) { vm.registerForTranslation((state.items + state.demoItems).map { it.text }) }
        val tr = translation?.takeIf { it.storeId == vm.storeId }
        fun ReviewItem.shown() = if (tr == null || text.isBlank()) this else copy(text = tr.pick(text.trim()))
        state.items.forEach { r0 ->
            val r = r0.shown()
            val own = r.uid == myUid
            ReviewCard(r, onOpenProfile, onReport = if (!own) ({ reportTarget = r.id }) else null, onEdit = if (own) ({ editId = r.id; editing = true }) else null)
        }
        state.demoItems.forEach { r -> ReviewCard(r.shown(), onOpenProfile, onReport = null, onEdit = null) }
        if (state.reported) Text(stringResource(R.string.reviews_reported), style = NopoTheme.typography.bodySmall, color = c.primary)
        if (state.hasMore) NopoButton(stringResource(R.string.reviews_more), vm::loadMore, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }

    reportTarget?.let { id ->
        val reasons = listOf(R.string.report_reason_abuse, R.string.report_reason_ad, R.string.report_reason_false, R.string.report_reason_other)
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(stringResource(R.string.reviews_report_title)) },
            text = {
                Column {
                    reasons.forEach { res ->
                        val label = stringResource(res)
                        TextButton(onClick = { vm.report(id, label); reportTarget = null }) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text(stringResource(R.string.detail_back)) } },
        )
    }

    if (editing) {
        ReviewEditor(
            initial = state.mine.firstOrNull { it.id == editId },
            draftPhotos = vm.draftPhotos,
            onPhotosChanged = { vm.draftPhotos = it },
            saving = state.saving,
            error = state.error,
            onSave = { rating, text, photos -> vm.save(editId, rating, text, photos) { editing = false; vm.draftPhotos = null } },
            onDelete = editId?.let { id -> { vm.delete(id) { editing = false; vm.draftPhotos = null } } },
            onDismiss = { editing = false; vm.draftPhotos = null },
        )
    }
}

@Composable
private fun ReviewCard(r: ReviewItem, onOpenProfile: (String) -> Unit, onReport: (() -> Unit)?, onEdit: (() -> Unit)? = null) {
    val c = NopoTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        Row(Modifier.heightIn(min = NopoTheme.spacing.minTouch).then(if (r.demo) Modifier else Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpenProfile(r.uid) }), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            Text(r.nickname, style = NopoTheme.typography.titleSmall, color = c.ink)
            Text(stringResource(R.string.reviews_visit_count, r.visitCount), style = NopoTheme.typography.bodySmall, color = c.ink2)
            Text(
                java.time.Instant.ofEpochMilli(r.updatedAtMillis).atZone(com.nopo.cheonnyeon.core.common.SeoulZone).toLocalDate().toString(),
                style = NopoTheme.typography.bodySmall, color = c.ink2,
            )
        }
        StarRating(r.rating.toFloat())
        if (r.text.isNotBlank()) Text(r.text, style = NopoTheme.typography.bodyMedium, color = c.ink)
        if (r.photos.isNotEmpty()) ReviewPhotoRow(r.photos, Modifier.fillMaxWidth())
        if (onReport != null) TextButton(onClick = onReport) { Text(stringResource(R.string.reviews_report)) }
        if (onEdit != null) TextButton(onClick = onEdit) { Text(stringResource(R.string.reviews_edit)) }
    }
}

/** 별점만으로도 저장할 수 있고, 글은 500자·10줄까지. 작성 중인 글은 화면을 돌려도 유지된다 (초안 보존). */
@Composable
private fun ReviewEditor(
    initial: ReviewItem?,
    draftPhotos: List<String>?,
    onPhotosChanged: (List<String>) -> Unit,
    saving: Boolean,
    error: AppError?,
    onSave: (Int, String, List<String>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var rating by rememberSaveable { mutableIntStateOf(initial?.rating ?: 0) }
    var text by rememberSaveable { mutableStateOf(initial?.text.orEmpty()) }
    // 사진은 용량이 커서 화면 상태 저장(Bundle)에 넣지 않고 ViewModel의 초안에 둔다(화면을 돌려도 유지, 앱이 완전히 종료되면 사라짐)
    var photos by remember { mutableStateOf(draftPhotos ?: initial?.photos.orEmpty()) }
    var capturing by rememberSaveable { mutableStateOf(false) }
    val check = ReviewRules.validate(rating.coerceAtLeast(0), text)
    val invalid = (check as? ReviewCheck.Invalid)?.reason
    val problem = when (invalid) {
        ReviewInvalid.TEXT_TOO_LONG -> stringResource(R.string.reviews_too_long, ReviewRules.TEXT_MAX)
        ReviewInvalid.TOO_MANY_LINES -> stringResource(R.string.reviews_too_many_lines, ReviewRules.MAX_LINES)
        else -> null
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.reviews_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                StarRatingInput(rating = rating, onRatingChange = { rating = it }, enabled = !saving)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.reviews_text_hint)) },
                    supportingText = { Text(problem ?: "${text.trim().length} / ${ReviewRules.TEXT_MAX}") },
                    isError = problem != null,
                    minLines = 3,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                ReviewPhotoPicker(photos, { photos = it; onPhotosChanged(it) }, onCamera = { capturing = true }, enabled = !saving)
                error?.let { Text(it.message(LocalContext.current), color = NopoTheme.colors.stamp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rating, text, photos) }, enabled = !saving && check is ReviewCheck.Valid) {
                Text(stringResource(if (saving) R.string.auth_working else R.string.reviews_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete, enabled = !saving) { Text(stringResource(R.string.reviews_delete)) }
                TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.detail_back)) }
            }
        },
    )
    if (capturing) ReviewPhotoCapture(onCaptured = { photos = (photos + it).take(ReviewRules.MAX_PHOTOS); onPhotosChanged(photos); capturing = false }, onDismiss = { capturing = false })
}

@com.nopo.cheonnyeon.core.designsystem.component.NopoPreviews
@Composable
private fun ReviewCardPreview() {
    com.nopo.cheonnyeon.core.designsystem.component.PreviewSurface {
        ReviewCard(ReviewItem("R1", "BN-1", "노포천년식당", "u1", "국밥순례자", "ELDER", 4, "수육이 부드럽고 공기밥이 푸짐해요.", 2, 0L), onOpenProfile = {}, onReport = {})
        ReviewCard(ReviewItem("R2", "BN-1", "노포천년식당", "demo", "골목식객", "REGULAR", 5, "예시 리뷰입니다.", 1, 0L, demo = true), onOpenProfile = {}, onReport = null)
    }
}

/** AI 리뷰 분석: 특장점·추천 메뉴·아쉬운 점 (리뷰가 3개 이상일 때) */
@Composable
private fun AiInsightSection(vm: ReviewsViewModel, state: ReviewsUiState) {
    if (!vm.aiAvailable) return
    val c = NopoTheme.colors
    val total = state.items.size + state.demoItems.size
    var askConsent by rememberSaveable { mutableStateOf(false) }
    val consent by vm.aiConsent.collectAsStateWithLifecycle(initialValue = false)
    if (state.insight == null) {
        if (total < 3) {
            Text(stringResource(R.string.insight_need_reviews), style = NopoTheme.typography.bodySmall, color = c.ink2)
        } else {
            NopoButton(
                stringResource(if (state.insightLoading) R.string.insight_loading else R.string.insight_button),
                { if (consent) vm.analyze() else askConsent = true },
                enabled = !state.insightLoading,
                style = NopoButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    state.insightError?.let { Text(it.message(LocalContext.current), color = c.stamp) }
    state.insight?.let { ins ->
        Column(
            Modifier.fillMaxWidth().background(c.tealSoft, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.insight_title), style = NopoTheme.typography.titleSmall, color = c.primary)
            if (ins.oneLine.isNotBlank()) Text(ins.oneLine, style = NopoTheme.typography.titleMedium, color = c.ink)
            if (ins.strengths.isNotEmpty()) {
                Text(stringResource(R.string.insight_strengths), style = NopoTheme.typography.labelMedium, color = c.ink2)
                ins.strengths.forEach { Text("• $it", style = NopoTheme.typography.bodyMedium, color = c.ink) }
            }
            if (ins.menus.isNotEmpty()) {
                Text(stringResource(R.string.insight_menus), style = NopoTheme.typography.labelMedium, color = c.ink2)
                ins.menus.forEach { m -> Text("• ${m.menu}" + if (m.reason.isNotBlank()) " — ${m.reason}" else "", style = NopoTheme.typography.bodyMedium, color = c.ink) }
            }
            if (ins.weaknesses.isNotEmpty()) {
                Text(stringResource(R.string.insight_weaknesses), style = NopoTheme.typography.labelMedium, color = c.ink2)
                ins.weaknesses.forEach { Text("• $it", style = NopoTheme.typography.bodyMedium, color = c.stamp) }
            }
            if (ins.bestFor.isNotBlank()) {
                Text(stringResource(R.string.insight_best_for), style = NopoTheme.typography.labelMedium, color = c.ink2)
                Text(ins.bestFor, style = NopoTheme.typography.bodyMedium, color = c.ink)
            }
            Text(stringResource(R.string.insight_disclaimer), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
    }
    if (askConsent) com.nopo.cheonnyeon.feature.ai.AiConsentDialog(onAgree = { vm.agreeAi(); askConsent = false; vm.analyze() }, onDismiss = { askConsent = false })
}

/** 단골 충성도 점수(100점 만점): 재방문율을 다른 가게와 견준 AI 평가 */
@Composable
private fun LoyaltySection(vm: ReviewsViewModel, state: ReviewsUiState) {
    if (!vm.aiAvailable) return
    val c = NopoTheme.colors
    val visitors = state.stats?.visitors ?: (state.items.size + state.demoItems.size)
    if (visitors < 3) return
    var askConsent by rememberSaveable { mutableStateOf(false) }
    val consent by vm.aiConsent.collectAsStateWithLifecycle(initialValue = false)
    val result = state.loyalty
    if (result == null) {
        NopoButton(
            stringResource(if (state.loyaltyLoading) R.string.loyalty_loading else R.string.loyalty_button),
            { if (consent) vm.analyzeLoyalty() else askConsent = true },
            enabled = !state.loyaltyLoading, style = NopoButtonStyle.Outline, modifier = Modifier.fillMaxWidth(),
        )
    }
    state.loyaltyError?.let { Text(it.message(LocalContext.current), color = c.stamp) }
    result?.let { r ->
        Column(Modifier.fillMaxWidth().background(c.brassSoft, NopoTheme.shapes.md).padding(NopoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs)) {
            Text(stringResource(R.string.loyalty_title), style = NopoTheme.typography.titleSmall, color = c.primary)
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
                Text(stringResource(R.string.loyalty_score, r.score), style = NopoTheme.typography.displayMedium, color = c.stamp)
                if (r.label.isNotBlank()) Text(r.label, style = NopoTheme.typography.titleMedium, color = c.ink)
            }
            androidx.compose.material3.LinearProgressIndicator(progress = { r.score / 100f }, modifier = Modifier.fillMaxWidth(), color = c.stamp, trackColor = c.surface)
            Text(stringResource(R.string.loyalty_compare, r.peerCount, r.topPercent), style = NopoTheme.typography.bodySmall, color = c.ink2)
            Text(r.summary, style = NopoTheme.typography.bodyMedium, color = c.ink)
            r.reasons.forEach { Text("• $it", style = NopoTheme.typography.bodyMedium, color = c.ink) }
            Text(stringResource(R.string.loyalty_basis, r.input.visitors, r.input.revisitors, (r.input.rate * 100).toInt(), r.baseline), style = NopoTheme.typography.bodySmall, color = c.ink2)
            Text(stringResource(R.string.loyalty_disclaimer), style = NopoTheme.typography.bodySmall, color = c.ink2)
        }
    }
    if (askConsent) com.nopo.cheonnyeon.feature.ai.AiConsentDialog(onAgree = { vm.agreeAi(); askConsent = false; vm.analyzeLoyalty() }, onDismiss = { askConsent = false })
}

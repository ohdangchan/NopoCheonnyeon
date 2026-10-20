package com.nopo.cheonnyeon.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 리뷰를 쓸 수 있는 자격 (FR-STORE-04) */
enum class ReviewEligibility { NOT_VISITED, SYNCING, OK }

data class ReviewsUiState(
    val loading: Boolean = true,
    val summary: ReviewSummary? = null,
    val items: List<ReviewItem> = emptyList(),
    val hasMore: Boolean = false,
    val mine: ReviewItem? = null,
    val eligibility: ReviewEligibility = ReviewEligibility.NOT_VISITED,
    val error: AppError? = null,
    val saving: Boolean = false,
    val reported: Boolean = false,
    val stats: com.nopo.cheonnyeon.data.remote.StoreStats? = null,
)

class ReviewsViewModel(private val container: AppContainer, private val store: Store) : ViewModel() {
    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()
    private var lastPage: ReviewPage? = null

    val myUid: String? get() = container.auth.currentUid

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val visit = container.database.myVisitDao().byStore(store.id)
            val eligibility = when {
                visit == null -> ReviewEligibility.NOT_VISITED
                !visit.synced -> ReviewEligibility.SYNCING
                else -> ReviewEligibility.OK
            }
            val summary = container.reviews.summary(store.id)
            val page = container.reviews.page(store.id)
            val stats = container.storeStats.get(store.id).valueOrNull
            val mine = if (eligibility == ReviewEligibility.OK) container.reviews.mine(store.id).valueOrNull else null
            lastPage = page.valueOrNull
            _state.update {
                it.copy(
                    loading = false,
                    summary = summary.valueOrNull,
                    items = page.valueOrNull?.items.orEmpty(),
                    hasMore = page.valueOrNull?.hasMore ?: false,
                    mine = mine,
                    stats = stats,
                    eligibility = eligibility,
                    error = summary.errorOrNull ?: page.errorOrNull,
                )
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

    fun save(rating: Int, text: String, onDone: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val visit = container.database.myVisitDao().byStore(store.id)
            val total = container.database.myVisitDao().observeTotalYears().first()
            val result = container.reviews.save(
                store.id, store.name, rating, text, visit?.visitCount ?: 1, TitlePolicy.titleFor(total).name, existing = _state.value.mine != null,
            )
            _state.update { it.copy(saving = false, error = result.errorOrNull) }
            if (result is AppResult.Success) { onDone(); refresh() }
        }
    }

    fun delete(onDone: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = container.reviews.delete(store.id)
            _state.update { it.copy(saving = false, error = result.errorOrNull) }
            if (result is AppResult.Success) { onDone(); refresh() }
        }
    }
}

/** 평점 요약 + 리뷰 목록 + 평점 남기기 (S-506, S-802, FR-STORE-03·04) */
@Composable
fun StoreReviewsSection(vm: ReviewsViewModel, onOpenProfile: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = NopoTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    var reportTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val myUid = vm.myUid

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

        when (state.eligibility) {
            ReviewEligibility.NOT_VISITED -> Text(stringResource(R.string.reviews_need_visit), style = NopoTheme.typography.bodySmall, color = c.ink2)
            ReviewEligibility.SYNCING -> Text(AppError.E_VISIT_SYNCING.message(LocalContext.current), style = NopoTheme.typography.bodySmall, color = c.ink2)
            ReviewEligibility.OK -> NopoButton(
                stringResource(if (state.mine != null) R.string.reviews_edit else R.string.reviews_write),
                { editing = true },
                style = NopoButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.items.forEach { r -> ReviewCard(r, onOpenProfile, onReport = if (r.uid != myUid) ({ reportTarget = r.id }) else null) }
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
            initial = state.mine,
            saving = state.saving,
            error = state.error,
            onSave = { rating, text -> vm.save(rating, text) { editing = false } },
            onDelete = if (state.mine != null) ({ vm.delete { editing = false } }) else null,
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun ReviewCard(r: ReviewItem, onOpenProfile: (String) -> Unit, onReport: (() -> Unit)?) {
    val c = NopoTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, NopoTheme.shapes.md).padding(NopoTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.xs),
    ) {
        Row(Modifier.heightIn(min = NopoTheme.spacing.minTouch).clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpenProfile(r.uid) }, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            Text(r.nickname, style = NopoTheme.typography.titleSmall, color = c.ink)
            Text(stringResource(R.string.reviews_visit_count, r.visitCount), style = NopoTheme.typography.bodySmall, color = c.ink2)
            Text(
                java.time.Instant.ofEpochMilli(r.updatedAtMillis).atZone(com.nopo.cheonnyeon.core.common.SeoulZone).toLocalDate().toString(),
                style = NopoTheme.typography.bodySmall, color = c.ink2,
            )
        }
        StarRating(r.rating.toFloat())
        if (r.text.isNotBlank()) Text(r.text, style = NopoTheme.typography.bodyMedium, color = c.ink)
        if (onReport != null) TextButton(onClick = onReport) { Text(stringResource(R.string.reviews_report)) }
    }
}

/** 별점만으로도 저장할 수 있고, 글은 500자·10줄까지. 작성 중인 글은 화면을 돌려도 유지된다 (초안 보존). */
@Composable
private fun ReviewEditor(
    initial: ReviewItem?,
    saving: Boolean,
    error: AppError?,
    onSave: (Int, String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var rating by rememberSaveable { mutableIntStateOf(initial?.rating ?: 0) }
    var text by rememberSaveable { mutableStateOf(initial?.text.orEmpty()) }
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
                error?.let { Text(it.message(LocalContext.current), color = NopoTheme.colors.stamp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rating, text) }, enabled = !saving && check is ReviewCheck.Valid) {
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
}

package com.nopo.cheonnyeon.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StoreDetailUiState(
    val loading: Boolean = true,
    val store: Store? = null,
    val years: Int = 0,
    val visit: MyVisitEntity? = null,
    val myTotalYears: Int = 0,
)

class StoreDetailViewModel(private val container: AppContainer, private val storeId: String) : ViewModel() {
    /** 가게 정보 오류 제보 (FR-STORE-06) */
    suspend fun reportInfo(reason: String) = container.reviews.reportStoreInfo(storeId, reason)

    /** 저장한 노포 여부 (저장하기 버튼) */
    val bookmarked: StateFlow<Boolean> = container.settings.bookmarks.map { storeId in it }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    val ratings = container.ratings
    val lastFix = container.lastFix

    init { container.refreshLocation() }

    fun toggleBookmark() { viewModelScope.launch { container.settings.toggleBookmark(storeId) } }

    /** AI 번역 상태(이 가게 것만) */
    val translation: StateFlow<com.nopo.cheonnyeon.data.ai.StoreTranslation?> = container.translator.state
        .map { t -> t?.takeIf { it.storeId == storeId } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    init {
        // 번역할 글(소개글·한 줄 소개)을 모아 둔다. 리뷰는 리뷰 영역이 모은다
        viewModelScope.launch {
            container.catalog.byId(storeId)?.let { s -> container.translator.addSources(s.id, listOfNotNull(s.summary, s.story)) }
        }
        // 다른 가게로 옮겨 오면 이전 번역은 지운다
        viewModelScope.launch { container.translator.state.value?.takeIf { it.storeId != storeId }?.let { container.translator.clear() } }
    }

    /** 인증 직후 "리뷰 남기기"로 들어왔는지 */
    val reviewRequested: StateFlow<Boolean> = container.pendingReviewStoreId.map { it == storeId }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    private val _state = MutableStateFlow(StoreDetailUiState())
    val state: StateFlow<StoreDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val store = container.catalog.byId(storeId)
            val year = LocalDate.now(SeoulZone).year
            val dao = container.database.myVisitDao()
            combine(dao.observeAll(), dao.observeTotalYears()) { visits, total -> visits.firstOrNull { it.storeId == storeId } to total }
                .collect { (visit, total) ->
                    _state.value = StoreDetailUiState(
                        loading = false,
                        store = store,
                        years = store?.let { YearsCalculator.yearsAtVisit(it.foundedYear, year) } ?: 0,
                        visit = visit,
                        myTotalYears = total,
                    )
                }
        }
    }
}

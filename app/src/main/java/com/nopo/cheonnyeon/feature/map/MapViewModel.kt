package com.nopo.cheonnyeon.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.SeoulZone
import com.nopo.cheonnyeon.core.util.GeoDistance
import com.nopo.cheonnyeon.domain.catalog.Program
import com.nopo.cheonnyeon.domain.catalog.StoreFilter
import com.nopo.cheonnyeon.domain.catalog.StoreFilters
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.rule.YearsCalculator
import com.nopo.cheonnyeon.platform.location.Fix
import com.nopo.cheonnyeon.platform.location.FusedLocationAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ListSort { NEAREST, YEARS }

data class MapUiState(
    val loading: Boolean = true,
    val filter: StoreFilter = StoreFilter(foodOnly = true),
    val filtered: List<Store> = emptyList(),
    val total: Int = 0,
    val visitedIds: Set<String> = emptySet(),
    val totalYears: Int = 0,
    val selectedId: String? = null,
    val listMode: Boolean = false,
    val sort: ListSort = ListSort.NEAREST,
    val fix: Fix? = null,
    val locating: Boolean = false,
    val currentYear: Int = LocalDate.now(SeoulZone).year,
) {
    val selected: Store? get() = filtered.firstOrNull { it.id == selectedId }

    /** 검색어가 있으면 지도 대신 목록을 보여준다 (좌표 없는 가게도 찾을 수 있게) */
    val showList: Boolean get() = listMode || filter.query.isNotBlank()

    fun yearsOf(s: Store) = YearsCalculator.yearsAtVisit(s.foundedYear, currentYear)

    fun distanceTo(s: Store): Double? {
        val f = fix ?: return null
        if (s.lat == null || s.lng == null) return null
        return GeoDistance.meters(f.lat, f.lng, s.lat, s.lng)
    }

    /** 목록 정렬 결과 */
    val sorted: List<Store>
        get() = when {
            sort == ListSort.NEAREST && fix != null -> StoreFilters.sortByDistance(filtered, fix.lat, fix.lng)
            else -> StoreFilters.sortByYears(filtered, currentYear)
        }
}

class MapViewModel(private val container: AppContainer) : ViewModel() {
    private val local = MutableStateFlow(MapUiState())
    private val allStores = MutableStateFlow<List<Store>>(emptyList())
    private val location = FusedLocationAdapter(container.appContext)

    val state: StateFlow<MapUiState> = combine(
        local,
        allStores,
        container.database.myVisitDao().observeAll(),
    ) { ui, stores, visits ->
        val visited = visits.map { it.storeId }.toSet()
        ui.copy(
            loading = stores.isEmpty() && ui.loading,
            filtered = StoreFilters.apply(stores, ui.filter, visited),
            total = stores.size,
            visitedIds = visited,
            totalYears = visits.filter { !it.storeId.startsWith(Store.TEST_PREFIX) }.sumOf { it.yearsAwarded },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MapUiState())

    init {
        viewModelScope.launch {
            container.catalog.changes.collectLatest {
                // 첫 실행에는 적재가 끝날 때까지 잠시 기다린다
                var list = container.catalog.all()
                var tries = 0
                while (list.isEmpty() && tries++ < 40) {
                    kotlinx.coroutines.delay(250)
                    list = container.catalog.all()
                }
                allStores.value = list
                if (list.isEmpty()) local.update { it.copy(loading = false) }
            }
        }
    }

    fun setQuery(q: String) = local.update { it.copy(filter = it.filter.copy(query = q), selectedId = null) }
    fun toggleFood() = local.update { it.copy(filter = it.filter.copy(foodOnly = !it.filter.foodOnly), selectedId = null) }
    fun toggleProgram(p: Program) = local.update { it.copy(filter = it.filter.copy(program = if (it.filter.program == p) null else p), selectedId = null) }
    fun toggleUnvisited() = local.update { it.copy(filter = it.filter.copy(unvisitedOnly = !it.filter.unvisitedOnly), selectedId = null) }
    fun select(id: String?) = local.update { it.copy(selectedId = id) }
    fun toggleListMode() = local.update { it.copy(listMode = !it.listMode) }
    fun setSort(s: ListSort) = local.update { it.copy(sort = s) }

    /** 내 위치 한 번 찾기 (권한은 화면이 먼저 확인한다). 최대 8초 */
    fun findMe() {
        if (local.value.locating) return
        local.update { it.copy(locating = true) }
        viewModelScope.launch {
            val fix = try {
                location.bestFix(timeoutMs = 8_000L)
            } catch (_: Exception) {
                null
            }
            local.update { it.copy(locating = false, fix = fix ?: it.fix) }
        }
    }
}

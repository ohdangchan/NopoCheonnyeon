package com.nopo.cheonnyeon.feature.map

import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.data.catalog.CatalogRepository
import com.nopo.cheonnyeon.data.local.AppDatabase
import com.nopo.cheonnyeon.data.local.MyVisitDao
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.domain.catalog.Program
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** S-502: 지도 화면 상태 (필터·검색·세월 합계·지도 오류 전환) */
class MapViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val stores = listOf(
        Store(id = "BN-1", name = "백년식당", programs = listOf("BAEKNYEON"), isFood = true, lat = 37.5, lng = 127.0, foundedYear = 1950),
        Store(id = "OR-1", name = "오래식당", programs = listOf("ORAE"), isFood = true, lat = 37.6, lng = 127.1, foundedYear = 1990),
        Store(id = "BN-2", name = "백년공방", programs = listOf("BAEKNYEON"), isFood = false, lat = 37.4, lng = 127.2),
    )
    private val visits = MutableStateFlow<List<MyVisitEntity>>(emptyList())

    private fun newVm(): MapViewModel {
        val catalog = mockk<CatalogRepository>()
        every { catalog.changes } returns MutableStateFlow(0)
        coEvery { catalog.all() } returns stores
        val dao = mockk<MyVisitDao>()
        every { dao.observeAll() } returns visits
        val db = mockk<AppDatabase>()
        every { db.myVisitDao() } returns dao
        val container = mockk<AppContainer>()
        every { container.catalog } returns catalog
        every { container.database } returns db
        return MapViewModel(container)
    }

    @Test fun `처음에는 음식점만 보인다`() {
        val vm = newVm()
        assertEquals(setOf("BN-1", "OR-1"), vm.state.value.filtered.map { it.id }.toSet())
        assertEquals(3, vm.state.value.total)
    }

    @Test fun `음식점만 필터를 끄면 전체가 보인다`() {
        val vm = newVm()
        vm.toggleFood()
        assertEquals(3, vm.state.value.filtered.size)
    }

    @Test fun `백년가게 칩은 백년가게만 남긴다 그리고 다시 누르면 풀린다`() {
        val vm = newVm()
        vm.toggleProgram(Program.BAEKNYEON)
        assertEquals(listOf("BN-1"), vm.state.value.filtered.map { it.id })
        vm.toggleProgram(Program.BAEKNYEON)
        assertEquals(2, vm.state.value.filtered.size)
    }

    @Test fun `B-03 내 세월은 테스트 가게 방문을 뺀다`() {
        val vm = newVm()
        visits.value = listOf(
            MyVisitEntity("BN-1", 76, "RECEIPT", 1, 1),
            MyVisitEntity("TEST-NOPO", 50, "RECEIPT", 1, 1),
        )
        assertEquals(76, vm.state.value.totalYears)
        assertEquals(setOf("BN-1", "TEST-NOPO"), vm.state.value.visitedIds)
    }

    @Test fun `안 가 본 곳 필터는 방문한 가게를 뺀다`() {
        val vm = newVm()
        visits.value = listOf(MyVisitEntity("BN-1", 76, "RECEIPT", 1, 1))
        vm.toggleUnvisited()
        assertEquals(listOf("OR-1"), vm.state.value.filtered.map { it.id })
    }

    @Test fun `검색어가 있으면 목록 화면으로 바뀐다`() {
        val vm = newVm()
        assertFalse(vm.state.value.showList)
        vm.setQuery("오래")
        assertTrue(vm.state.value.showList)
        assertEquals(listOf("OR-1"), vm.state.value.filtered.map { it.id })
    }

    @Test fun `B-18 지도 오류가 나면 목록으로 전환하고 한 번만 기록한다`() {
        val vm = newVm()
        vm.onMapFailed()
        assertTrue(vm.state.value.listMode)
        assertTrue(vm.state.value.mapFailed)
        vm.toggleListMode() // 사용자가 지도로 돌아가려 해도 mapFailed 표시는 유지된다
        assertFalse(vm.state.value.listMode)
        vm.onMapFailed() // 이미 실패로 기록된 뒤에는 다시 목록으로 강제하지 않는다
        assertFalse(vm.state.value.listMode)
    }

    @Test fun `가게를 고르고 해제할 수 있다`() {
        val vm = newVm()
        vm.select("BN-1")
        assertEquals("BN-1", vm.state.value.selected?.id)
        vm.select(null)
        assertEquals(null, vm.state.value.selected)
    }
}

package com.nopo.cheonnyeon.data.catalog

import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.data.local.StoreDao
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 가게 목록 조회. 1,300곳 안팎이라 한 번 읽어 메모리에 두고 필터·정렬은 메모리에서 한다 (S-304). */
class CatalogRepository(
    private val dao: StoreDao,
    private val dispatchers: DispatcherProvider,
) {
    private val lock = Mutex()
    private var cache: List<Store>? = null

    /** 목록이 바뀌면(적재·테스트 가게) 숫자가 올라간다. 화면은 이걸 보고 다시 읽는다 */
    val changes = kotlinx.coroutines.flow.MutableStateFlow(0)

    suspend fun all(): List<Store> = lock.withLock {
        cache ?: withContext(dispatchers.io) { dao.all().map { it.toDomain() } }.also { if (it.isNotEmpty()) cache = it }
    }

    suspend fun byId(id: String): Store? = all().firstOrNull { it.id == id }

    /** 카탈로그를 다시 적재한 뒤 호출 */
    suspend fun invalidate() {
        lock.withLock { cache = null }
        changes.value += 1
    }

    /** 테스트 가게를 지운다 (FR-TST-03). 방문 기록은 VisitRepository.clearTestData가 지운다 */
    suspend fun deleteTestStores() {
        withContext(dispatchers.io) { dao.deleteTestStores() }
        invalidate()
    }

    /** 진단용 테스트 가게를 현재 위치에 만든다 (FR-TST-01). 카탈로그를 다시 적재해도 지워지지 않는다. */
    suspend fun createTestStore(lat: Double, lng: Double): Store {
        val store = Store(
            id = Store.TEST_PREFIX + "NOPO", name = "노포천년테스트식당", phone = "020000000", foundedYear = 1980,
            programs = listOf("BAEKNYEON"), sido = "테스트", sigungu = "테스트", address = "테스트 가게", lat = lat, lng = lng, isTest = true,
            summary = "진단 화면에서 만든 시험용 가게예요. 세월과 한도에 들어가지 않아요.",
            categoryGroup = "음식점", menuItems = listOf("설렁탕", "수육", "공기밥"), hours = "매일 10:00~21:00",
            story = "1980년 문을 연 뒤 한 솥 국물로 사십 년 넘게 손님을 맞아 온 시연용 노포예요. 영수증 인증을 직접 해 볼 수 있게 현재 위치에 만든 가게라서 세월과 하루 한도에는 들어가지 않아요.",
            photoUrls = (1..3).map { "file:///android_asset/demo/" + Store.TEST_PREFIX + "NOPO_" + it + ".jpg" },
        )
        withContext(dispatchers.io) { dao.insertAll(listOf(com.nopo.cheonnyeon.data.local.StoreEntity.from(store))) }
        invalidate()
        return store
    }
}

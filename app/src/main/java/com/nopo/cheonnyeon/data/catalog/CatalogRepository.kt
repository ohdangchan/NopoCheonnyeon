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

    suspend fun all(): List<Store> = lock.withLock {
        cache ?: withContext(dispatchers.io) { dao.all().map { it.toDomain() } }.also { if (it.isNotEmpty()) cache = it }
    }

    suspend fun byId(id: String): Store? = all().firstOrNull { it.id == id }

    /** 카탈로그를 다시 적재한 뒤 호출 */
    suspend fun invalidate() = lock.withLock { cache = null }
}

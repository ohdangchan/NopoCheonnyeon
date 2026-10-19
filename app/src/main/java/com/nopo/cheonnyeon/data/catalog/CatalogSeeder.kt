package com.nopo.cheonnyeon.data.catalog

import android.content.Context
import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.data.local.SettingsStore
import com.nopo.cheonnyeon.data.local.StoreDao
import com.nopo.cheonnyeon.data.local.StoreEntity
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * assets/stores.json → Room 적재 (아키텍처 6.2). 저장된 버전과 다를 때만 읽고, 한 트랜잭션으로 교체한다.
 * 실패하면 이전 데이터를 그대로 두고 다음 실행에 다시 시도한다 (실패는 Result로 돌려준다).
 */
class CatalogSeeder(
    private val context: Context,
    private val dao: StoreDao,
    private val settings: SettingsStore,
    private val dispatchers: DispatcherProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** @return 이번에 새로 적재했으면 true, 이미 최신이면 false */
    suspend fun seedIfNeeded(): Result<Boolean> = withContext(dispatchers.io) {
        runCatching {
            val raw = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val catalog = json.decodeFromString<CatalogJson>(raw)
            if (catalog.version == settings.catalogVersion() && dao.count() > 0) return@runCatching false
            dao.replaceAll(catalog.stores.map { StoreEntity.from(it.toDomain()) })
            settings.setCatalogVersion(catalog.version)
            true
        }
    }

    companion object {
        const val ASSET = "stores.json"
    }
}

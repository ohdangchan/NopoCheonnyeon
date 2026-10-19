package com.nopo.cheonnyeon.app

import android.content.Context
import android.util.Log
import com.nopo.cheonnyeon.core.common.Clock
import com.nopo.cheonnyeon.core.common.DefaultDispatchers
import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.core.common.SystemClock
import com.nopo.cheonnyeon.data.catalog.CatalogRepository
import com.nopo.cheonnyeon.data.catalog.CatalogSeeder
import com.nopo.cheonnyeon.data.local.AppDatabase
import com.nopo.cheonnyeon.data.local.SettingsStore
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.domain.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 직접 만드는 의존성 모음 (아키텍처 3장). 동기화·방문 저장소는 E3 뒷부분부터 여기에 추가한다. */
class AppContainer(
    context: Context,
    val clock: Clock = SystemClock,
    val dispatchers: DispatcherProvider = DefaultDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: AppDatabase = AppDatabase.create(context)
    val settings = SettingsStore(context)
    val catalog = CatalogRepository(database.storeDao(), dispatchers)
    val auth = AuthRepository()
    private val catalogSeeder = CatalogSeeder(context, database.storeDao(), settings, dispatchers)

    init {
        // 카탈로그 적재는 백그라운드에서. 실패하면 이전 데이터를 쓰고 다음 실행에 다시 시도한다
        scope.launch {
            catalogSeeder.seedIfNeeded()
                .onFailure { Log.e("Nopo", "카탈로그 적재 실패", it) }
                .onSuccess { Log.d("Nopo", "카탈로그 적재 완료 (새로 적재=$it)"); catalog.invalidate() }
        }
    }

    /** 내비게이션이 보는 로그인 게이트 상태 */
    val sessionState: StateFlow<SessionState> = auth.sessionState.stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)
}

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
import com.google.firebase.firestore.FirebaseFirestore
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.data.visit.SyncScheduler
import com.nopo.cheonnyeon.data.visit.VisitRepository
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
    val appContext: Context,
    val clock: Clock = SystemClock,
    val dispatchers: DispatcherProvider = DefaultDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: AppDatabase = AppDatabase.create(appContext)
    val settings = SettingsStore(appContext)
    val catalog = CatalogRepository(database.storeDao(), dispatchers)
    val auth = AuthRepository()
    val visits = VisitRepository(database.myVisitDao(), database.pendingOpDao(), { FirebaseFirestore.getInstance() }, { auth.currentUid }, clock, dispatchers)
    val reviews = com.nopo.cheonnyeon.data.remote.ReviewRepository({ FirebaseFirestore.getInstance() }, { auth.currentUid })
    val profiles = com.nopo.cheonnyeon.data.remote.ProfileRepository({ FirebaseFirestore.getInstance() }, reviews)
    val connectivity = com.nopo.cheonnyeon.platform.network.ConnectivityMonitor(appContext)
    val syncScheduler = SyncScheduler(appContext)
    private val catalogSeeder = CatalogSeeder(appContext, database.storeDao(), settings, dispatchers)

    init {
        // 카탈로그 적재는 백그라운드에서. 실패하면 이전 데이터를 쓰고 다음 실행에 다시 시도한다
        scope.launch {
            catalogSeeder.seedIfNeeded()
                .onFailure { Log.e("Nopo", "카탈로그 적재 실패", it) }
                .onSuccess { Log.d("Nopo", "카탈로그 적재 완료 (새로 적재=$it)"); catalog.invalidate() }
        }
    }

    /** 앱을 켤 때 올라가지 못한 기록이 있으면 다시 보낸다 */
    fun syncPending() = syncScheduler.enqueue()

    init {
        // 로그인되면 서버 기록으로 기기를 채우고(재설치 복원), 대기열을 보낸다
        scope.launch {
            auth.sessionState.collect { st ->
                if (st == SessionState.SignedIn) {
                    visits.restoreFromServer()
                    syncScheduler.enqueue()
                }
            }
        }
    }

    /** 로그아웃: 이 기기의 사용자 데이터를 지우고 나간다 (대기열 경고는 화면이 먼저 보여 준다) */
    fun signOut() {
        scope.launch {
            clearLocalUserData()
            auth.signOut()
        }
    }

    /** 방문 기록·대기열·인증샷 삭제 (로그아웃·탈퇴 때). 카탈로그는 남긴다 */
    suspend fun clearLocalUserData() {
        visits.clearUserData()
        java.io.File(appContext.filesDir, "proofs").deleteRecursively()
    }

    /** 내비게이션이 보는 로그인 게이트 상태 */
    val sessionState: StateFlow<SessionState> = auth.sessionState.stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)
}

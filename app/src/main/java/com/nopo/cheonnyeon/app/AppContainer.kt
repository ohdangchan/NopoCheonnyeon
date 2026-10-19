package com.nopo.cheonnyeon.app

import com.google.firebase.auth.FirebaseAuth
import com.nopo.cheonnyeon.core.common.Clock
import com.nopo.cheonnyeon.core.common.DefaultDispatchers
import com.nopo.cheonnyeon.core.common.DispatcherProvider
import com.nopo.cheonnyeon.core.common.SystemClock
import com.nopo.cheonnyeon.domain.model.SessionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted

/** 직접 만드는 의존성 모음 (아키텍처 3장). 저장소·동기화는 E3부터 여기에 추가한다. */
class AppContainer(
    val clock: Clock = SystemClock,
    val dispatchers: DispatcherProvider = DefaultDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val authUid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /** 내비게이션이 보는 로그인 게이트 상태. 프로필(NeedsNickname) 판정은 E4에서 추가 */
    val sessionState: StateFlow<SessionState> = authUid
        .map { if (it == null) SessionState.SignedOut else SessionState.SignedIn }
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)
}

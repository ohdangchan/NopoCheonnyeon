package com.nopo.cheonnyeon.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.core.common.Analytics
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.domain.rule.AuthValidation
import com.nopo.cheonnyeon.platform.auth.GoogleSignIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NickStatus { IDLE, CHECKING, AVAILABLE, TAKEN, UNKNOWN }

data class AuthUiState(
    val busy: Boolean = false,
    val error: AppError? = null,
    /** 비밀번호 재설정 메일을 보냈다 */
    val resetSent: Boolean = false,
    val nick: NickStatus = NickStatus.IDLE,
)

private const val NICK_CHECK_DELAY_MS = 400L

/** 로그인 성공 뒤 화면이 넘어가지 않아도 "로그인 중…" 표시를 푸는 시간 */
private const val SIGN_IN_SETTLE_MS = 8_000L

/** 로그인·가입·재설정·닉네임 복구 화면이 함께 쓴다. 성공하면 세션 상태가 바뀌어 내비게이션이 알아서 이동한다. */
class AuthViewModel(private val repo: AuthRepository, private val onKeepLogin: (Boolean) -> Unit = {}) : ViewModel() {
    /** 로그인 상태 유지 체크: 꺼 두면 앱을 다시 켤 때 로그아웃된다 */
    fun setKeepLogin(keep: Boolean) = onKeepLogin(keep)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var nickJob: Job? = null

    fun signIn(email: String, password: String) =
        run(keepBusy = true, onSuccess = { Analytics.login() }) { repo.signIn(email, password) }

    fun signUp(email: String, password: String, nickname: String) =
        run(onSuccess = { Analytics.signUp() }) { repo.signUp(email, password, nickname) }

    /** 구글 로그인: [token] 은 [GoogleSignIn] 이 받은 ID 토큰 */
    fun googleSignIn(token: String) =
        run(keepBusy = true, onSuccess = { Analytics.login() }) { repo.signInWithGoogle(token) }

    fun showMessage(error: AppError) = _state.update { it.copy(error = error) }

    fun createProfile(nickname: String) = run { repo.createProfile(nickname) }

    fun sendReset(email: String) =
        run(onSuccess = { _state.update { it.copy(resetSent = true) } }) { repo.sendPasswordReset(email) }

    /** 로그인 성공 뒤 화면이 넘어가기까지 "로그인 중…" 을 유지했다가, 세션이 정해지면 푼다 */
    fun settle() = _state.update { if (it.busy) it.copy(busy = false) else it }

    fun clearError() = _state.update { it.copy(error = null) }

    /** 입력을 멈추고 0.4초 뒤에 중복 확인 (입력 중 안내, FR-AUTH-01) */
    fun onNicknameChanged(nick: String) {
        nickJob?.cancel()
        if (AuthValidation.nickname(nick) != null) {
            _state.update { it.copy(nick = NickStatus.IDLE) }
            return
        }
        _state.update { it.copy(nick = NickStatus.CHECKING) }
        nickJob = viewModelScope.launch {
            delay(NICK_CHECK_DELAY_MS)
            val status = when (repo.isNicknameAvailable(nick)) {
                true -> NickStatus.AVAILABLE
                false -> NickStatus.TAKEN
                null -> NickStatus.UNKNOWN
            }
            _state.update { it.copy(nick = status) }
        }
    }

    private fun run(keepBusy: Boolean = false, onSuccess: () -> Unit = {}, block: suspend () -> AppResult<Unit>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, resetSent = false) }
        viewModelScope.launch {
            val r = block()
            val stay = keepBusy && r is AppResult.Success
            _state.update { it.copy(busy = stay, error = r.errorOrNull) }
            if (r is AppResult.Success) onSuccess()
            if (stay) {
                delay(SIGN_IN_SETTLE_MS)
                settle()
            }
        }
    }
}

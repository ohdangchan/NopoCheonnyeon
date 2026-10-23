package com.nopo.cheonnyeon.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.domain.rule.AuthValidation
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

/** 로그인·가입·재설정·닉네임 복구 화면이 함께 쓴다. 성공하면 세션 상태가 바뀌어 내비게이션이 알아서 이동한다. */
class AuthViewModel(private val repo: AuthRepository, private val onKeepLogin: (Boolean) -> Unit = {}) : ViewModel() {
    /** 로그인 상태 유지 체크: 꺼 두면 앱을 다시 켤 때 로그아웃된다 */
    fun setKeepLogin(keep: Boolean) = onKeepLogin(keep)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var nickJob: Job? = null

    fun signIn(email: String, password: String) = run(onSuccess = { com.nopo.cheonnyeon.core.common.Analytics.login() }) { repo.signIn(email, password) }

    fun signUp(email: String, password: String, nickname: String) = run(onSuccess = { com.nopo.cheonnyeon.core.common.Analytics.signUp() }) { repo.signUp(email, password, nickname) }

    /** 구글 로그인: [token] 은 [com.nopo.cheonnyeon.platform.auth.GoogleSignIn] 이 받은 ID 토큰 */
    fun googleSignIn(token: String) = run(onSuccess = { com.nopo.cheonnyeon.core.common.Analytics.login() }) { repo.signInWithGoogle(token) }

    fun showMessage(error: AppError) = _state.update { it.copy(error = error) }

    fun createProfile(nickname: String) = run { repo.createProfile(nickname) }

    fun sendReset(email: String) = run(onSuccess = { _state.update { it.copy(resetSent = true) } }) { repo.sendPasswordReset(email) }

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
            delay(400)
            val ok = repo.isNicknameAvailable(nick)
            _state.update { it.copy(nick = when (ok) { true -> NickStatus.AVAILABLE; false -> NickStatus.TAKEN; null -> NickStatus.UNKNOWN }) }
        }
    }

    private fun run(onSuccess: () -> Unit = {}, block: suspend () -> AppResult<Unit>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, resetSent = false) }
        viewModelScope.launch {
            val r = block()
            _state.update { it.copy(busy = false, error = r.errorOrNull) }
            if (r is AppResult.Success) onSuccess()
        }
    }
}

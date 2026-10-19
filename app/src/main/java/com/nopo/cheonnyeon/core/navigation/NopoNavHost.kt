package com.nopo.cheonnyeon.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.SessionState
import com.nopo.cheonnyeon.feature.diagnostics.DiagnosticsScreen
import kotlinx.coroutines.flow.StateFlow

/** 세션 상태가 바뀌면 그래프를 통째로 바꾼다: 로그아웃 → 로그인, 로그인 → 홈 (FR-AUTH-06). */
@Composable
fun NopoNavHost(session: StateFlow<SessionState>) {
    val nav = rememberNavController()
    val state by session.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val target = when (state) {
            SessionState.Loading -> Routes.SPLASH
            SessionState.SignedOut, SessionState.NeedsNickname -> Routes.LOGIN
            SessionState.SignedIn -> Routes.HOME
        }
        if (nav.currentDestination?.route != target && nav.currentDestination?.route != Routes.DIAGNOSTICS) {
            nav.navigate(target) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { Placeholder(R.string.placeholder_splash, null) }
        composable(Routes.LOGIN) { Placeholder(R.string.placeholder_login) { nav.navigate(Routes.DIAGNOSTICS) } }
        composable(Routes.HOME) { Placeholder(R.string.placeholder_home) { nav.navigate(Routes.DIAGNOSTICS) } }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen() }
    }
}

/** E4~E5에서 실제 화면으로 교체될 자리. 디버그 빌드에서만 진단 화면으로 가는 버튼이 보인다. */
@Composable
private fun Placeholder(textRes: Int, onDiagnostics: (() -> Unit)?) {
    val c = NopoTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(NopoTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(textRes), color = c.ink, style = NopoTheme.typography.titleMedium)
        if (BuildConfig.DEBUG && onDiagnostics != null) {
            NopoButton(text = stringResource(R.string.diag_title), onClick = onDiagnostics)
        }
    }
}

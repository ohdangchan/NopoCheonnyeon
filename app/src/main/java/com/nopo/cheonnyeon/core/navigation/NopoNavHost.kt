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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.NopoApp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.SessionState
import com.nopo.cheonnyeon.feature.auth.AuthViewModel
import com.nopo.cheonnyeon.feature.auth.LoginScreen
import com.nopo.cheonnyeon.feature.auth.OnboardingScreen
import kotlinx.coroutines.launch
import com.nopo.cheonnyeon.feature.auth.NicknameSetupScreen
import com.nopo.cheonnyeon.feature.auth.ResetPasswordScreen
import com.nopo.cheonnyeon.feature.auth.SignUpScreen
import com.nopo.cheonnyeon.feature.diagnostics.DiagnosticsScreen
import com.nopo.cheonnyeon.feature.main.MainScreen
import com.nopo.cheonnyeon.feature.profile.ProfileScreen
import com.nopo.cheonnyeon.feature.profile.ProfileViewModel
import com.nopo.cheonnyeon.feature.verify.VerifyScreen
import com.nopo.cheonnyeon.feature.verify.VerifyViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.nopo.cheonnyeon.feature.store.StoreDetailScreen
import com.nopo.cheonnyeon.feature.store.StoreDetailViewModel
import com.nopo.cheonnyeon.feature.store.ReviewsViewModel
import com.nopo.cheonnyeon.feature.store.StoreReviewsSection
import com.nopo.cheonnyeon.core.common.factoryOf
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** 세션 상태가 바뀌면 그래프를 통째로 바꾼다: 로그아웃 → 로그인, 프로필 없음 → 닉네임, 로그인 → 홈 (FR-AUTH-06·07). */
@Composable
fun NopoNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val state by container.sessionState.collectAsStateWithLifecycle()
    val authVm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(container.auth) as T
    })

    val onboardingDone by container.settings.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(state, onboardingDone) {
        val current = nav.currentDestination?.route
        val target = when (state) {
            SessionState.Loading -> Routes.SPLASH
            SessionState.SignedOut -> when {
                onboardingDone == null -> Routes.SPLASH
                onboardingDone == false -> Routes.ONBOARDING
                current in Routes.signedOutRoutes -> current
                else -> Routes.LOGIN
            }
            SessionState.NeedsNickname -> Routes.NICKNAME
            SessionState.SignedIn -> if (current == Routes.DIAGNOSTICS || current == Routes.HALL || current?.startsWith("store/") == true || current?.startsWith("profile/") == true || current?.startsWith("verify/") == true) current else Routes.HOME
        }
        if (target != null && current != target) nav.navigate(target) { popUpTo(0) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { Placeholder(R.string.placeholder_splash) }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = { scope.launch { container.settings.setOnboardingDone() } })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                vm = authVm,
                onSignUp = { authVm.clearError(); nav.navigate(Routes.SIGN_UP) },
                onReset = { authVm.clearError(); nav.navigate(Routes.RESET) },
                onDiagnostics = if (BuildConfig.DEBUG) ({ nav.navigate(Routes.DIAGNOSTICS) }) else null,
            )
        }
        composable(Routes.SIGN_UP) { SignUpScreen(authVm, onBack = { authVm.clearError(); nav.popBackStack() }) }
        composable(Routes.RESET) { ResetPasswordScreen(authVm, onBack = { authVm.clearError(); nav.popBackStack() }) }
        composable(Routes.NICKNAME) { NicknameSetupScreen(authVm, onSignOut = { container.signOut() }) }
        composable(Routes.HOME) {
            MainScreen(container, onOpenStore = { nav.navigate(Routes.store(it)) }, onOpenProfile = { nav.navigate(Routes.profile(it)) }, onDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) }, onOpenHall = { nav.navigate(Routes.HALL) })
        }
        composable(Routes.STORE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: StoreDetailViewModel = viewModel(factory = factoryOf { StoreDetailViewModel(container, id) })
            StoreDetailScreen(vm, onBack = { nav.popBackStack() }, onVerify = { nav.navigate(Routes.verify(id)) }, reviews = { store ->
                val reviewsVm: ReviewsViewModel = viewModel(key = "reviews_" + store.id, factory = factoryOf { ReviewsViewModel(container, store) })
                StoreReviewsSection(reviewsVm, onOpenProfile = { nav.navigate(Routes.profile(it)) })
            })
        }
        composable(Routes.VERIFY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: VerifyViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    VerifyViewModel(container, id, extras.createSavedStateHandle()) as T
            })
            VerifyScreen(vm, onClose = { nav.popBackStack() })
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
            val uid = entry.arguments?.getString("uid").orEmpty()
            val vm: ProfileViewModel = viewModel(key = "profile_$uid", factory = factoryOf { ProfileViewModel(container, uid) })
            ProfileScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) })
        }
        composable(Routes.HALL) {
            val vm: com.nopo.cheonnyeon.feature.passport.HallOfFameViewModel = viewModel(factory = factoryOf { com.nopo.cheonnyeon.feature.passport.HallOfFameViewModel(container) })
            com.nopo.cheonnyeon.feature.passport.HallOfFameScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) })
        }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen() }
    }
}

/** E5에서 실제 화면으로 교체될 자리 */
@Composable
private fun Placeholder(textRes: Int, extra: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(textRes), color = NopoTheme.colors.ink, style = NopoTheme.typography.titleMedium)
        extra()
    }
}

package com.nopo.cheonnyeon.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
fun NopoNavHost(container: AppContainer, debugRoute: String? = null) {
    val nav = rememberNavController()
    val state by container.sessionState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val authVm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(container.auth) { keep -> scope.launch { container.settings.setKeepLogin(keep) } } as T
    })

    val onboardingDone by container.settings.onboardingDone.collectAsStateWithLifecycle(initialValue = null)

    // 로딩 화면은 앱을 켤 때마다(처음 설치든 다시 켜든) 가장 먼저, 최소 1.4초 보여 준다
    var splashDone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1400); splashDone = true }

    LaunchedEffect(state, onboardingDone, splashDone) {
        if (!splashDone) return@LaunchedEffect
        if (state != SessionState.Loading) authVm.settle()
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
            SessionState.SignedIn -> if ((BuildConfig.DEBUG && debugRoute != null && (current in Routes.signedOutRoutes || current == Routes.ONBOARDING)) || current == Routes.DIAGNOSTICS || current == Routes.HALL || current == Routes.ROULETTE || current == Routes.CHAT || current?.startsWith("store/") == true || current?.startsWith("profile/") == true || current?.startsWith("verify/") == true || current?.startsWith("review/") == true) current else Routes.HOME
        }
        if (target != null && current != target) nav.navigate(target) { popUpTo(0) { inclusive = true } }
    }

    // 디버그 빌드 전용: adb 로 `--es debug_route login` 처럼 주면 로그인 상태에서도 그 화면을 볼 수 있다
    androidx.compose.runtime.LaunchedEffect(debugRoute) {
        if (BuildConfig.DEBUG && debugRoute != null) { kotlinx.coroutines.delay(1500); nav.navigate(debugRoute) }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { com.nopo.cheonnyeon.feature.splash.SplashScreen() }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(container, onDone = { scope.launch { container.settings.setOnboardingDone() } })
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
            MainScreen(container, onOpenStore = { nav.navigate(Routes.store(it)) }, onOpenProfile = { nav.navigate(Routes.profile(it)) }, onDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) }, onOpenHall = { nav.navigate(Routes.HALL) }, onOpenRoulette = { nav.navigate(Routes.ROULETTE) }, onOpenChat = { q -> nav.navigate(Routes.chat(q)) }, onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
        }
        composable(Routes.NOTIFICATIONS) {
            val vm: com.nopo.cheonnyeon.feature.notifications.NotificationsViewModel = viewModel(factory = factoryOf { com.nopo.cheonnyeon.feature.notifications.NotificationsViewModel(container) })
            com.nopo.cheonnyeon.feature.notifications.NotificationsScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) }, onOpenTab = { t -> container.requestedTab.value = t; nav.popBackStack(Routes.HOME, false) })
        }
        composable(Routes.STORE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: StoreDetailViewModel = viewModel(factory = factoryOf { StoreDetailViewModel(container, id) })
            StoreDetailScreen(vm, onBack = { nav.popBackStack() }, onVerify = { nav.navigate(Routes.verify(id)) }, translateBar = { s -> com.nopo.cheonnyeon.feature.ai.TranslateBar(container, s) }, reviews = { store ->
                val reviewsVm: ReviewsViewModel = viewModel(key = "reviews_" + store.id, factory = factoryOf { ReviewsViewModel(container, store) })
                StoreReviewsSection(reviewsVm, onOpenProfile = { nav.navigate(Routes.profile(it)) }, onVerify = { nav.navigate(Routes.verify(store.id)) })
            })
        }
        if (BuildConfig.DEBUG) {
            // 디버그 전용 미리보기: 가짜 값으로 인증 완료·정보 확인 화면을 그려 본다
            composable("debug/done") {
                val store = androidx.compose.runtime.produceState<com.nopo.cheonnyeon.domain.model.Store?>(null) { value = container.catalog.byId("BN-20250295902") }.value
                if (store != null) com.nopo.cheonnyeon.feature.verify.DoneScreen(
                    com.nopo.cheonnyeon.feature.verify.VerifyUiState(
                        store = store, step = com.nopo.cheonnyeon.feature.verify.VerifyStep.DONE,
                        award = com.nopo.cheonnyeon.data.visit.AwardResult(store.name, 46, com.nopo.cheonnyeon.domain.model.StampGrade.NORMAL, true, 468, com.nopo.cheonnyeon.domain.model.Title.entries[4], com.nopo.cheonnyeon.domain.model.VerifyMethod.RECEIPT, false),
                        region = com.nopo.cheonnyeon.domain.rule.RegionProgress("서울", 3, 120),
                    ), onReview = {}, onClose = { nav.popBackStack() },
                )
            }
            composable("debug/confirm") {
                val store = androidx.compose.runtime.produceState<com.nopo.cheonnyeon.domain.model.Store?>(null) { value = container.catalog.byId("BN-20250295902") }.value
                if (store != null) com.nopo.cheonnyeon.feature.verify.ConfirmScreen(
                    com.nopo.cheonnyeon.feature.verify.VerifyUiState(
                        store = store,
                        confirm = com.nopo.cheonnyeon.feature.verify.ReceiptInfo(store.name, java.time.LocalDateTime.now(), 47000L, store.address, "h", "12345678"),
                    ), onRetake = { nav.popBackStack() }, onConfirm = { nav.popBackStack() },
                )
            }
        }
        composable(Routes.VERIFY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: VerifyViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    VerifyViewModel(container, id, extras.createSavedStateHandle()) as T
            })
            VerifyScreen(vm, onClose = { nav.popBackStack() }, onReview = { nav.popBackStack(); nav.navigate(Routes.review(id)) })
        }
        composable(Routes.REVIEW, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val store = androidx.compose.runtime.produceState<com.nopo.cheonnyeon.domain.model.Store?>(null, id) { value = container.catalog.byId(id) }.value
            if (store != null) {
                val reviewsVm: ReviewsViewModel = viewModel(key = "reviews_" + store.id, factory = factoryOf { ReviewsViewModel(container, store) })
                com.nopo.cheonnyeon.feature.store.ReviewWriteScreen(
                    reviewsVm, store, onBack = { nav.popBackStack() },
                    onOpenStore = { nav.popBackStack(); nav.navigate(Routes.store(store.id)) },
                    onTab = { t -> container.requestedTab.value = t; nav.popBackStack(Routes.HOME, false) },
                )
            }
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
            val uid = entry.arguments?.getString("uid").orEmpty()
            val vm: ProfileViewModel = viewModel(key = "profile_$uid", factory = factoryOf { ProfileViewModel(container, uid) })
            ProfileScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) })
        }
        composable(Routes.HALL) {
            val vm: com.nopo.cheonnyeon.feature.passport.HallOfFameViewModel = viewModel(factory = factoryOf { com.nopo.cheonnyeon.feature.passport.HallOfFameViewModel(container) })
            com.nopo.cheonnyeon.feature.passport.RankingScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) }, onTab = { t -> container.requestedTab.value = t; nav.popBackStack(Routes.HOME, false) })
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })) { entry ->
            val vm: com.nopo.cheonnyeon.feature.ai.ChatViewModel = viewModel(factory = factoryOf { com.nopo.cheonnyeon.feature.ai.ChatViewModel(container) })
            com.nopo.cheonnyeon.feature.ai.ChatScreen(vm, initialQuery = entry.arguments?.getString("q").orEmpty(), onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) })
        }
        composable(Routes.ROULETTE) {
            val vm: com.nopo.cheonnyeon.feature.ai.NopoRouletteViewModel = viewModel(factory = factoryOf { com.nopo.cheonnyeon.feature.ai.NopoRouletteViewModel(container) })
            com.nopo.cheonnyeon.feature.ai.NopoRouletteScreen(
                vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) },
                onTab = { t -> container.requestedTab.value = t; nav.popBackStack(Routes.HOME, false) },
            )
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

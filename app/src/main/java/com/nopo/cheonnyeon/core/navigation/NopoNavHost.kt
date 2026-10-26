package com.nopo.cheonnyeon.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.BuildConfig
import com.nopo.cheonnyeon.core.common.factoryOf
import com.nopo.cheonnyeon.data.visit.AwardResult
import com.nopo.cheonnyeon.domain.model.SessionState
import com.nopo.cheonnyeon.domain.model.StampGrade
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.Title
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.domain.rule.RegionProgress
import com.nopo.cheonnyeon.feature.ai.ChatScreen
import com.nopo.cheonnyeon.feature.ai.ChatViewModel
import com.nopo.cheonnyeon.feature.ai.NopoRouletteScreen
import com.nopo.cheonnyeon.feature.ai.NopoRouletteViewModel
import com.nopo.cheonnyeon.feature.ai.TranslateBar
import com.nopo.cheonnyeon.feature.auth.AuthViewModel
import com.nopo.cheonnyeon.feature.auth.LoginScreen
import com.nopo.cheonnyeon.feature.auth.NicknameSetupScreen
import com.nopo.cheonnyeon.feature.auth.OnboardingScreen
import com.nopo.cheonnyeon.feature.auth.ResetPasswordScreen
import com.nopo.cheonnyeon.feature.auth.SignUpScreen
import com.nopo.cheonnyeon.feature.diagnostics.DiagnosticsScreen
import com.nopo.cheonnyeon.feature.main.MainScreen
import com.nopo.cheonnyeon.feature.notifications.NotificationsScreen
import com.nopo.cheonnyeon.feature.notifications.NotificationsViewModel
import com.nopo.cheonnyeon.feature.passport.HallOfFameViewModel
import com.nopo.cheonnyeon.feature.passport.RankingScreen
import com.nopo.cheonnyeon.feature.profile.ProfileScreen
import com.nopo.cheonnyeon.feature.profile.ProfileViewModel
import com.nopo.cheonnyeon.feature.splash.SplashScreen
import com.nopo.cheonnyeon.feature.store.ReviewsViewModel
import com.nopo.cheonnyeon.feature.store.ReviewWriteScreen
import com.nopo.cheonnyeon.feature.store.StoreDetailScreen
import com.nopo.cheonnyeon.feature.store.StoreDetailViewModel
import com.nopo.cheonnyeon.feature.store.StoreReviewsSection
import com.nopo.cheonnyeon.feature.verify.ConfirmScreen
import com.nopo.cheonnyeon.feature.verify.DoneScreen
import com.nopo.cheonnyeon.feature.verify.ReceiptInfo
import com.nopo.cheonnyeon.feature.verify.VerifyScreen
import com.nopo.cheonnyeon.feature.verify.VerifyStep
import com.nopo.cheonnyeon.feature.verify.VerifyUiState
import com.nopo.cheonnyeon.feature.verify.VerifyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** 세션 상태가 바뀌면 그래프를 통째로 바꾼다: 로그아웃 → 로그인, 프로필 없음 → 닉네임, 로그인 → 홈 (FR-AUTH-06·07). */
@Composable
fun NopoNavHost(container: AppContainer, debugRoute: String? = null) {
    val nav = rememberNavController()
    val state by container.sessionState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val authVm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(container.auth) { keep ->
            scope.launch { container.settings.setKeepLogin(keep) }
        } as T
    })

    val onboardingDone by container.settings.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
    // 별도 화면의 하단 탭: 홈 화면으로 돌아가 고른 탭을 연다
    val openMainTab: (Int) -> Unit = { tab -> container.requestedTab.value = tab; nav.popBackStack(Routes.HOME, false) }

    // 로딩 화면은 앱을 켤 때마다(처음 설치든 다시 켜든) 가장 먼저, 최소 1.4초 보여 준다
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1400); splashDone = true }

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
            SessionState.SignedIn -> if (keepsRouteWhenSignedIn(current, debugRoute)) current else Routes.HOME
        }
        if (target != null && current != target) nav.navigate(target) { popUpTo(0) { inclusive = true } }
    }

    // 디버그 빌드 전용: adb 로 `--es debug_route login` 처럼 주면 로그인 상태에서도 그 화면을 볼 수 있다
    LaunchedEffect(debugRoute) {
        if (BuildConfig.DEBUG && debugRoute != null) { delay(1500); nav.navigate(debugRoute) }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen() }
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
            MainScreen(
                container,
                onOpenStore = { nav.navigate(Routes.store(it)) },
                onOpenProfile = { nav.navigate(Routes.profile(it)) },
                onDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) },
                onOpenHall = { nav.navigate(Routes.HALL) },
                onOpenRoulette = { nav.navigate(Routes.ROULETTE) },
                onOpenChat = { q -> nav.navigate(Routes.chat(q)) },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
            )
        }
        composable(Routes.NOTIFICATIONS) {
            val vm: NotificationsViewModel = viewModel(factory = factoryOf { NotificationsViewModel(container) })
            NotificationsScreen(
                vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) }, onOpenTab = openMainTab,
            )
        }
        composable(Routes.STORE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: StoreDetailViewModel = viewModel(factory = factoryOf { StoreDetailViewModel(container, id) })
            StoreDetailScreen(vm,
                onBack = { nav.popBackStack() },
                onVerify = { nav.navigate(Routes.verify(id)) },
                translateBar = { s -> TranslateBar(
                container,
                s
            ) }, reviews = { store ->
                val reviewsVm: ReviewsViewModel = viewModel(
                    key = "reviews_" + store.id,
                    factory = factoryOf { ReviewsViewModel(container, store) }
                )
                StoreReviewsSection(
                    reviewsVm,
                    onOpenProfile = { nav.navigate(Routes.profile(it)) },
                    onVerify = { nav.navigate(Routes.verify(store.id)) }
                )
            })
        }
        if (BuildConfig.DEBUG) {
            // 디버그 전용 미리보기: 가짜 값으로 인증 완료·정보 확인 화면을 그려 본다
            composable("debug/done") {
                val store = produceState<Store?>(null) { value = container.catalog.byId("BN-20250295902") }.value
                if (store != null) DoneScreen(
                    VerifyUiState(
                        store = store, step = VerifyStep.DONE,
                        award = AwardResult(store.name, 46, StampGrade.NORMAL, true, 468, Title.entries[4], VerifyMethod.RECEIPT, false),
                        region = RegionProgress("서울", 3, 120),
                    ), onReview = {}, onClose = { nav.popBackStack() },
                )
            }
            composable("debug/confirm") {
                val store = produceState<Store?>(null) { value = container.catalog.byId("BN-20250295902") }.value
                if (store != null) ConfirmScreen(
                    VerifyUiState(
                        store = store,
                        confirm = ReceiptInfo(store.name, LocalDateTime.now(), 47000L, store.address, "h", "12345678"),
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
            val store = produceState<Store?>(null, id) { value = container.catalog.byId(id) }.value
            if (store != null) {
                val reviewsVm: ReviewsViewModel = viewModel(
                    key = "reviews_" + store.id,
                    factory = factoryOf { ReviewsViewModel(container, store) }
                )
                ReviewWriteScreen(
                    reviewsVm, store, onBack = { nav.popBackStack() },
                    onOpenStore = { nav.popBackStack(); nav.navigate(Routes.store(store.id)) },
                    onTab = openMainTab,
                )
            }
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
            val uid = entry.arguments?.getString("uid").orEmpty()
            val vm: ProfileViewModel = viewModel(key = "profile_$uid", factory = factoryOf { ProfileViewModel(container, uid) })
            ProfileScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) })
        }
        composable(Routes.HALL) {
            val vm: HallOfFameViewModel = viewModel(factory = factoryOf { HallOfFameViewModel(container) })
            RankingScreen(vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) }, onTab = openMainTab)
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })) { entry ->
            val vm: ChatViewModel = viewModel(factory = factoryOf { ChatViewModel(container) })
            ChatScreen(
                vm,
                initialQuery = entry.arguments?.getString("q").orEmpty(),
                onBack = { nav.popBackStack() },
                onOpenStore = { nav.navigate(Routes.store(it)) }
            )
        }
        composable(Routes.ROULETTE) {
            val vm: NopoRouletteViewModel = viewModel(factory = factoryOf { NopoRouletteViewModel(container) })
            NopoRouletteScreen(
                vm, onBack = { nav.popBackStack() }, onOpenStore = { nav.navigate(Routes.store(it)) },
                onTab = openMainTab,
            )
        }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen() }
    }
}

/** 로그인된 상태에서 지금 화면을 그대로 둘 것인가: 별도 화면(가게·인증·후기·프로필·랭킹·룰렛·도우미·진단)과 디버그 미리보기 */
private fun keepsRouteWhenSignedIn(current: String?, debugRoute: String?): Boolean {
    if (current == null) return false
    val debugPreview = BuildConfig.DEBUG && debugRoute != null && (current in Routes.signedOutRoutes || current == Routes.ONBOARDING)
    val standalone = current in STANDALONE_ROUTES
    val detail = DETAIL_ROUTE_PREFIXES.any { current.startsWith(it) }
    return debugPreview || standalone || detail
}

private val STANDALONE_ROUTES = setOf(Routes.DIAGNOSTICS, Routes.HALL, Routes.ROULETTE, Routes.CHAT)
private val DETAIL_ROUTE_PREFIXES = listOf("store/", "profile/", "verify/", "review/")

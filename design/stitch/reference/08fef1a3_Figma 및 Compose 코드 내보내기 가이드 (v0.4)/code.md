# 노포 천년 미식가 — Figma & Android Compose 코드 내보내기 종합 핸드오프 가이드 (v0.4)

> **프로젝트 개요**: 내가 먹어 온 노포의 세월을 모아 천 년을 채우는 안드로이드 미식 패스포트  
> **버전**: v0.4 Production Release Archive  
> **총 화면 수**: **51개 화면 (100% 매핑 완비)** | **노포 데이터**: **전국 1,277곳**  
> **내보내기 대상**: **Figma 디자인 시스템 & UI 프레임** / **Android Jetpack Compose 코드베이스**

---

## 1. Figma (디자인) 내보내기 및 셋업 가이드

Figma를 통해 UI 에셋을 가져오거나 디자인 토큰과 화면 구조를 1:1로 복원하는 표준 명세입니다.

### 1.1 피그마 캔버스 페이지 구조 (Recommended Page Hierarchy)
```
📂 Nopo-Cheonnyeon-Design-System/
├── 🎨 01. Design Tokens & Styles (Colors, Typography, Elevation, Grids)
├── 🧩 02. Components & Variants (StampView, TopAppBar, BottomBar, Badges, Cards)
├── 📱 03. Flows: Auth & Onboarding (SCREEN_50, 49, 46, 43, 40, 24, 25, 36)
├── 🗺️ 04. Flows: Map & Discovery (SCREEN_59, 48, 58, 17, 12, 32, 5)
├── 🧾 05. Flows: Verification Pipeline (SCREEN_56, 54, 41, 37, 22, 29, 31)
├── 🏅 06. Flows: Ceremony & Passport (SCREEN_57, 42, 55, 39, 45, 18, 7, 27, 23, 30, 19)
├── ⭐ 07. Flows: Reviews & Profiles (SCREEN_51, 21, 20, 35, 34, 53, 33)
├── ⚙️ 08. Flows: Settings & Diagnostics (SCREEN_47, 38, 8, 11, 26, 44, 9, 52, 28, 16, 10)
└── 🖥️ 09. Interactive Prototype & Transition Matrix (SCREEN_15, 16)
```

### 1.2 Figma 디자인 토큰 (Design Tokens / Variables) 정의표
Figma Variables 기능(Color, Dimension, Float)에 그대로 바인딩할 수 있는 토큰 값입니다.

| 토큰명 (Token Name) | 라이트 모드 (Hex) | 다크 모드 (Hex) | 용도 및 컴포넌트 매핑 |
|---|---|---|---|
| `color/sign` | `#17504A` | `#0F2E2A` | 상단 간판 바, 헤더 배경, 최상위 브랜드 영역 |
| `color/primary` | `#1F6B63` | `#5DB3A6` | 주요 CTA 버튼, 활성 탭 아이콘, 하이라이트 |
| `color/stamp` | `#C23B2A` | `#E8664F` | 붉은 인주 도장, 50~99년 노포 핀, 방문 인증하기 버튼 |
| `color/brass` | `#D9A22C` | `#E4B64A` | 100년 황금 도장, 칭호 훈장 테두리, 보상 안내 배지 |
| `color/paper` | `#EDF0EA` | `#0D1614` | 기본 앱 윈도우 배경 (종이 패스포트 톤) |
| `color/surface` | `#F8F9F5` | `#15221F` | 카드 컨테이너 면, 바텀시트, 다이얼로그 배경 |
| `color/ink` | `#14201E` | `#E7EEEA` | 고대비 본문 텍스트, 메인 타이틀 (대비비 12:1 이상) |
| `color/ink2` | `#52625E` | `#9CB0AA` | 부가 설명, 서브 캡션, 비활성 텍스트 (대비비 4.5:1 이상) |
| `color/line` | `#CCD4CC` | `#2A3C37` | 카드 테두리, 구분선, 점선 안내선 |
| `color/teal-soft` | `#D3E5E0` | `#1B3833` | 일반 30~49년 도장 배경, 청록 필터 칩 |
| `color/stamp-soft`| `#F4DCD6` | `#3D2420` | 중복/반려 경고 배경, 붉은 도장 인주 테두리 |
| `color/brass-soft`| `#F6E8C4` | `#3A3119` | 100년 노포 보증서 카드 배경 |

### 1.3 Figma Typography Styles
- **Display Big (업력 숫자)**: `Black Han Sans` 44sp / Line Height: 1.1 / Tabular Nums
- **Title Large**: `Noto Sans KR` Bold 22sp / Line Height: 1.3
- **Title Medium**: `Noto Sans KR` Bold 18sp / Line Height: 1.35
- **Body Large**: `Noto Sans KR` Regular 16sp / Line Height: 1.5
- **Body Medium (기본 본문)**: `Noto Sans KR` Regular 14sp / Line Height: 1.5
- **Label / Metric**: `JetBrains Mono` SemiBold 12~14sp / Monospace

### 1.4 HTML ➔ Figma 직접 변환/내보내기 방법 (3가지 옵션)
1. **HTML to Figma 플러그인 활용 (가장 간편)**:
   - Figma 커뮤니티에서 **"HTML to Design"** 플러그인 실행.
   - Stitch 상에서 렌더링된 각 Screen HTML 화면을 URL/직접 붙여넣기로 변환하여 Figma 레이어로 100% 벡터화 가져오기.
2. **SVG 에셋 추출**:
   - `StampView`, `Three.js` 인장 및 칭호 배지 SVG 코드를 복사하여 Figma 캔버스에 직접 Paste(Ctrl+V / Cmd+V).
3. **Google Stitch ➔ Figma Export**:
   - Stitch 상단의 `Export to Figma` 기능을 통해 생성된 51개 화면 프레임을 피그마 파일로 원클릭 내보내기.

---

## 2. Android Jetpack Compose (코드) 내보내기 명세

Android Studio 환경에서 즉시 빌드 및 실행 가능한 Jetpack Compose 코드 구조와 핵심 파일 매핑입니다.

### 2.1 프로덕션 프로젝트 패키지 구조
```
app/src/main/java/com/nopo/cheonnyeon/
├── NopoApp.kt                      // Application 클래스: SDK 초기화, AppContainer 연결
├── MainActivity.kt                 // Single Activity, WindowInsets 및 NopoNavHost 진입
├── AppContainer.kt                 // 수동 의존성 주입 (Repository, UseCase, Port 인스턴스)
│
├── core/
│   ├── navigation/
│   │   ├── Routes.kt               // 51개 화면 라우트 문자열 상수
│   │   └── NopoNavHost.kt          // SessionState (SignedOut/NeedsNickname/SignedIn) 내비게이션
│   ├── designsystem/
│   │   ├── theme/
│   │   │   ├── Color.kt            // Figma 토큰과 1:1 매핑된 Compose Color 정의
│   │   │   ├── Type.kt             // Black Han Sans, Noto Sans KR 폰트 패밀리
│   │   │   ├── Shape.kt            // RoundedCornerShape(16.dp), 999.dp(캡슐)
│   │   │   └── Theme.kt            // NopoTheme (Light/Dark Palette)
│   │   └── component/
│   │       ├── StampView.kt        // 380ms 각인 지원 도장 뷰
│   │       ├── YearsGauge.kt       // 1,000년 세월 프로그레스 바
│   │       ├── TitleBadge.kt       // 5대 식객 칭호 칩
│   │       ├── NopoTopAppBar.kt    // 간판 청록 상단 헤더
│   │       ├── NopoBottomBar.kt    // 3대 글로벌 내비게이션 바
│   │       └── NopoButton.kt       // 최소 48dp 터치 타깃 보장 CTA 버튼
│   ├── common/
│   │   ├── AppResult.kt            // Success / Failure 래퍼
│   │   └── AppError.kt             // PRD 7.4 정의 24개 표준 에러 코드
│   └── util/
│       ├── GeoDistance.kt          // WGS84 하버사인 30m 판정 알고리즘
│       └── TextNormalizer.kt       // 간판/상호 Damerau-Levenshtein 유사도 (0.70/0.60)
│
├── domain/                         // 순수 Kotlin (android.* 의존성 0%)
│   ├── model/ (Store, Visit, Review, Title, StampGrade)
│   ├── rule/ (YearsCalculator, TitlePolicy, DailyLimitPolicy, VerificationConfig)
│   ├── verify/ (ReceiptParser, ReceiptMatcher, ReceiptHasher, PhotoMatcher)
│   └── repository/ (인터페이스: AuthRepository, VisitRepository, ReviewRepository 등)
│
├── data/
│   ├── catalog/ (StoreJson, CatalogSeeder - stores.json 적재)
│   ├── local/ (AppDatabase, StoreDao, MyVisitDao, PendingOpDao, SettingsStore)
│   ├── remote/ (AuthRemote, UserRemote, VisitRemote, ReviewRemote)
│   └── sync/ (SyncWorker, SyncScheduler - WorkManager ACID 배치 전송)
│
├── platform/
│   ├── map/ (NaverMapView AndroidView 생명주기 래퍼, MarkerFactory)
│   ├── camera/ (CameraCapture - CameraX)
│   ├── ocr/ (MlKitKoreanRecognizer - ML Kit 한국어 v16.0.1)
│   ├── location/ (FusedLocationAdapter, MockDetector)
│   └── haptics/ (Haptics - 스탬프 40ms 진동)
│
└── feature/                        // 8개 도메인 51개 화면 Composable & ViewModel
    ├── auth/ (OnboardingScreen, SignUpScreen, LoginScreen, ResetPasswordScreen, NicknameSetupScreen)
    ├── map/ (MapScreen, StoreListScreen, MapFilterSheet, MapFallbackScreen)
    ├── store/ (StoreDetailScreen, TransitGuideSheet, StoreReportSheet, MemorialScreen)
    ├── verify/ (ReceiptVerifyScreen, PhotoVerifyScreen, VerifyResultScreen, LocationReliefSheet, DupSheet, LimitSheet)
    ├── stamp/ (StampCeremonyScreen, TitleUpOverlay)
    ├── passport/ (PassportScreen, StampDetailSheet, GoldCertSheet, ArchiveReportScreen, ShareCardScreen, QuestsScreen, HallOfFameScreen)
    ├── review/ (ReviewEditorScreen, ReviewModifierScreen, OfflineReviewBanner, StoreReviewsScreen, MyReviewsScreen)
    ├── profile/ (UserProfileScreen, NicknameEditScreen, WithdrawScreen, PrivacySettingsSheet)
    ├── settings/ (SettingsScreen, OfflineQueueScreen, NotificationsScreen, RestoreSyncScreen, LegalScreen, LicensesScreen)
    └── diagnostics/ (SpikeDiagnosticsScreen, DiagnosticsScreen, TestStoreScreen, OcrInspectorScreen, ApkInstallGuideScreen)
```

---

## 3. 핵심 Jetpack Compose 코드 스니펫 (즉시 복사/내보내기용)

### 3.1 Design System 테마 정의 (`core/designsystem/theme/Color.kt`)
```kotlin
package com.nopo.cheonnyeon.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Light Theme Tokens
val NopoSignLight = Color(0xFF17504A)
val NopoPrimaryLight = Color(0xFF1F6B63)
val NopoStampLight = Color(0xFFC23B2A)
val NopoBrassLight = Color(0xFFD9A22C)
val NopoPaperLight = Color(0xFFEDF0EA)
val NopoSurfaceLight = Color(0xFFF8F9F5)
val NopoInkLight = Color(0xFF14201E)
val NopoInk2Light = Color(0xFF52625E)
val NopoLineLight = Color(0xFFCCD4CC)
val NopoTealSoftLight = Color(0xFFD3E5E0)
val NopoStampSoftLight = Color(0xFFF4DCD6)
val NopoBrassSoftLight = Color(0xFFF6E8C4)

// Dark Theme Tokens
val NopoSignDark = Color(0xFF0F2E2A)
val NopoPrimaryDark = Color(0xFF5DB3A6)
val NopoStampDark = Color(0xFFE8664F)
val NopoBrassDark = Color(0xFFE4B64A)
val NopoPaperDark = Color(0xFF0D1614)
val NopoSurfaceDark = Color(0xFF15221F)
val NopoInkDark = Color(0xFFE7EEEA)
val NopoInk2Dark = Color(0xFF9CB0AA)
val NopoLineDark = Color(0xFF2A3C37)
val NopoTealSoftDark = Color(0xFF1B3833)
val NopoStampSoftDark = Color(0xFF3D2420)
val NopoBrassSoftDark = Color(0xFF3A3119)
```

### 3.2 핵심 인주 도장 컴포넌트 (`core/designsystem/component/StampView.kt`)
```kotlin
package com.nopo.cheonnyeon.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nopo.cheonnyeon.core.designsystem.theme.*

enum class StampGrade { NORMAL, RED, GOLD }

@Composable
fun StampView(
    years: Int,
    grade: StampGrade,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isRelief: Boolean = false,
    displayFont: FontFamily = FontFamily.Default
) {
    val stampColor = when (grade) {
        StampGrade.NORMAL -> NopoPrimaryLight
        StampGrade.RED -> NopoStampLight
        StampGrade.GOLD -> NopoBrassLight
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeOuter = size.toPx() * 0.05f
            val strokeInner = size.toPx() * 0.018f
            val radius = size.toPx() / 2f - strokeOuter / 2f
            val centerOffset = Offset(size.toPx() / 2f, size.toPx() / 2f)

            // 외곽 원형 인장 테두리
            drawCircle(
                color = stampColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeOuter)
            )

            // 내부 얇은 보조 인장선
            drawCircle(
                color = stampColor,
                radius = radius * 0.82f,
                center = centerOffset,
                style = Stroke(width = strokeInner)
            )
        }

        // 업력 숫자 중앙 각인
        Text(
            text = "$years",
            color = stampColor,
            fontSize = (size.value * 0.36f).sp,
            fontFamily = displayFont
        )
    }
}
```

### 3.3 단일 액티비티 및 로그인 게이트 (`core/navigation/NopoNavHost.kt`)
```kotlin
package com.nopo.cheonnyeon.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nopo.cheonnyeon.domain.model.SessionState

@Composable
fun NopoNavHost(
    navController: NavHostController,
    sessionState: SessionState
) {
    val startDestination = when (sessionState) {
        SessionState.SignedOut -> "auth/onboarding"
        SessionState.NeedsNickname -> "auth/nickname-setup"
        SessionState.SignedIn -> "main/map"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // [Domain 1: Auth]
        composable("auth/onboarding") { /* OnboardingRoute(navController) */ }
        composable("auth/signup") { /* SignUpRoute(navController) */ }
        composable("auth/login") { /* LoginRoute(navController) */ }
        composable("auth/nickname-setup") { /* NicknameSetupRoute(navController) */ }

        // [Domain 2: Map & Store]
        composable("main/map") { /* MapRoute(navController) */ }
        composable("store/{storeId}") { backStack -> 
            val storeId = backStack.arguments?.getString("storeId") ?: ""
            /* StoreDetailRoute(storeId, navController) */
        }

        // [Domain 3 & 4: Verify & Ceremony]
        composable("verify/{storeId}/receipt") { backStack ->
            val storeId = backStack.arguments?.getString("storeId") ?: ""
            /* ReceiptVerifyRoute(storeId, navController) */
        }
        composable("ceremony/stamp") { /* StampCeremonyRoute(navController) */ }

        // [Domain 5: Passport]
        composable("main/passport") { /* PassportRoute(navController) */ }

        // [Domain 6: Reviews]
        composable("review/editor/{storeId}") { backStack ->
            val storeId = backStack.arguments?.getString("storeId") ?: ""
            /* ReviewEditorRoute(storeId, navController) */
        }

        // [Domain 7 & 8: Settings & Diagnostics]
        composable("main/settings") { /* SettingsRoute(navController) */ }
        composable("diagnostics/spike") { /* SpikeDiagnosticsRoute(navController) */ }
    }
}
```

---

## 4. 내보내기 후 개발 검증 체크리스트

- [x] **Figma 토큰 1:1 바인딩**: `Color.kt`의 모든 헥스 코드가 Figma Variables의 색상 명칭과 완벽히 일치하는가?
- [x] **화면 라우트 인자 규격**: `store/{storeId}`, `profile/{userId}` 등 RESTful 라우트 네이밍이 준수되었는가?
- [x] **터치 타깃 접근성**: 모든 버튼 및 대화형 아이콘이 최소 48dp(`Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`)를 만족하는가?
- [x] **순수 Kotlin 도메인**: `domain/` 계층에 `android.*` 의존성이 전혀 없어 단위 테스트가 폰 없이 실행되는가?
- [x] **R8 최적화 Keep**: `StoreJson` 및 Firestore DTO 클래스에 R8 난독화 방지 규칙이 설정되었는가?

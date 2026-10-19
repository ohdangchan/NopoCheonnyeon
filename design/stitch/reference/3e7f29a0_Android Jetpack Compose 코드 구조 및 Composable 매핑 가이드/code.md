# 노포 천년 미식가 — Android Jetpack Compose 코드 구조 & Composable 매핑 명세서 (v0.4)

본 문서는 '노포 천년 미식가' 앱의 전체 51개 화면 및 핵심 기능(v0.4)을 **Android Jetpack Compose**, **Navigation Compose**, **단방향 데이터 흐름(UDF)**, **MVVM 아키텍처**에 맞춰 1:1로 구현하기 위한 프로덕션 코드 구조 및 Composable 매핑 명세입니다.

---

## 1. 아키텍처 원칙 및 패키지 계층 구조 (Package Tree)

```
com.nopo.cheonnyeon/
├── NopoApp.kt                      // Application: AppContainer, 지도 SDK, Firebase 초기화
├── MainActivity.kt                 // Single Activity, NopoNavHost 진입점, WindowInsets 처리
├── AppContainer.kt                 // 수동 DI 컨테이너 (저장소, 포트, 유스케이스 인스턴스)
│
├── core/
│   ├── navigation/
│   │   ├── Routes.kt               // 내비게이션 라우트 상수 및 인자 규격
│   │   ├── NopoNavHost.kt          // AnimatedNavHost, SessionState 기반 로그인 게이트
│   │   └── NavExtensions.kt        // navigateSafely, popBackStackWithLifecycle
│   ├── designsystem/
│   │   ├── theme/                  // Color, Type, Shape, NopoTheme
│   │   └── component/              // StampView, YearsGauge, TitleBadge, NopoTopBar, NopoBottomBar
│   ├── common/
│   │   ├── AppResult.kt            // Success / Failure 공통 래퍼
│   │   ├── AppError.kt             // PRD 7.4 정의 오류 코드 매핑
│   │   └── DispatcherProvider.kt   // Coroutine Dispatchers 주입
│   └── util/
│       ├── GeoDistance.kt          // WGS84 하버사인 거리 계산 (30m 판정)
│       └── TextNormalizer.kt       // 상호/메뉴 정규화 및 유사도 알고리즘 (Damerau-Levenshtein)
│
├── domain/                         // 순수 Kotlin (Android 의존성 0%)
│   ├── model/                      // Store, Visit, Review, UserProfile, Title, StampGrade
│   ├── rule/                       // YearsCalculator, StampGradePolicy, TitlePolicy, VerificationConfig
│   ├── verify/                     // ReceiptParser, ReceiptMatcher, ReceiptHasher, PhotoMatcher
│   └── repository/                 // CatalogRepository, AuthRepository, VisitRepository 등
│
├── data/
│   ├── catalog/                    // StoreJson, CatalogSeeder
│   ├── local/                      // AppDatabase, StoreDao, MyVisitDao, PendingOpDao, SettingsStore
│   ├── remote/                     // AuthRemote, UserRemote, VisitRemote, ReviewRemote
│   └── sync/                       // SyncWorker, SyncScheduler (WorkManager)
│
├── platform/
│   ├── map/                        // NaverMapView (AndroidView 감싸기), MarkerFactory, ClusterSupport
│   ├── camera/                     // CameraCapture (CameraX ImageAnalysis/Capture)
│   ├── ocr/                        // MlKitKoreanRecognizer (ML Kit Text Recognition v2)
│   ├── location/                   // FusedLocationAdapter, MockDetector, LocationSettingsChecker
│   └── haptics/                    // Haptics (스탬프 타격 진동 40ms)
│
└── feature/                        // Feature별 Screen / ViewModel / UiState 패키지
    ├── auth/                       // Onboarding, Login, SignUp, ResetPassword, NicknameSetup
    ├── map/                        // MapScreen, StoreListScreen, MapFilterSheet, MapFallbackScreen
    ├── store/                      // StoreDetailScreen, StoreReviewsSheet, RouteTransitSheet
    ├── verify/                     // ReceiptVerifyScreen, PhotoVerifyScreen, VerifyResultScreen, LimitSheet
    ├── stamp/                      // StampCeremonyScreen, TitleUpOverlay
    ├── passport/                   // PassportScreen, StampDetailSheet, ArchiveReportScreen, MemorialScreen
    ├── review/                     // ReviewEditorScreen, MyReviewsScreen, ReportSheet
    ├── profile/                    // UserProfileScreen, HallOfFameScreen, NicknameEditScreen, WithdrawScreen
    ├── settings/                   // SettingsScreen, DataSourceScreen, PrivacyPolicyScreen, LicensesScreen
    └── diagnostics/                // SpikeDiagnosticsScreen, TestStoreFactoryScreen, OcrInspectorScreen
```

---

## 2. 네비게이션 그래프 및 라우트 명세 (Navigation Routes)

`core/navigation/Routes.kt`에 정의되는 표준 라우트와 매개변수 구조입니다:

| 루트 그룹 | 라우트 경로 | 전달 인자 | 대응 화면 ID |
|---|---|---|---|
| **Auth** | `auth/onboarding` | - | `SCREEN_47` |
| | `auth/signup` | - | `SCREEN_46` |
| | `auth/login` | - | `SCREEN_43` |
| | `auth/reset-password` | - | `SCREEN_40` |
| | `auth/nickname-setup` | `userId: String` | `SCREEN_37` |
| **Main (Tabs)** | `main/map` | - | `SCREEN_56` |
| | `main/passport` | - | `SCREEN_52` (도장 0개: `SCREEN_36`) |
| | `main/settings` | - | `SCREEN_44` |
| **Store & Map** | `store/{storeId}` | `storeId: String` | `SCREEN_55` |
| | `store/{storeId}/transit` | `storeId: String, lat: Double, lng: Double` | `SCREEN_29` |
| | `store/{storeId}/report` | `storeId: String` | `SCREEN_30` |
| | `map/list` | `query: String?, filter: String?` | `SCREEN_45` |
| | `map/fallback-list` | `errorCode: String` | `SCREEN_9` |
| | `map/filter` | - (BottomSheet) | `SCREEN_14` |
| **Verify Flow** | `verify/{storeId}/receipt` | `storeId: String` | `SCREEN_53` |
| | `verify/{storeId}/photo` | `storeId: String` | `SCREEN_51` |
| | `verify/result` | `resultCode: String, storeId: String` | `SCREEN_38` (`SCREEN_19`, `SCREEN_26`, `SCREEN_28`) |
| | `verify/{storeId}/location-relief` | `storeId: String` | `SCREEN_34` |
| **Ceremony** | `ceremony/stamp` | `storeId: String, years: Int, grade: String` | `SCREEN_54` |
| | `ceremony/title-up` | `newTitleId: String, years: Int` | `SCREEN_39` |
| **Passport & Sub**| `passport/stamp/{storeId}` | `storeId: String` | `SCREEN_42` (100년: `SCREEN_15`) |
| | `passport/report-analytics`| - | `SCREEN_4` |
| | `passport/share-card` | - | `SCREEN_24` |
| | `passport/memorial` | - | `SCREEN_2` |
| | `passport/quests` | - | `SCREEN_20` |
| | `passport/hall-of-fame` | - | `SCREEN_27` |
| **Review & User** | `review/editor/{storeId}` | `storeId: String, isEdit: Boolean` | `SCREEN_48`, `SCREEN_18` |
| | `review/store/{storeId}` | `storeId: String` | `SCREEN_32` |
| | `review/my` | - | `SCREEN_31` |
| | `profile/{userId}` | `userId: String` | `SCREEN_50` |
| | `profile/edit-nickname` | - | `SCREEN_21` |
| | `profile/withdraw` | - | `SCREEN_22` |
| **System & Diag** | `settings/privacy` | - | `SCREEN_23` |
| | `settings/offline-queue` | - | `SCREEN_35` |
| | `settings/notifications` | - | `SCREEN_5` |
| | `settings/restore-sync` | - | `SCREEN_8` |
| | `diagnostics/spike` | - | `SCREEN_6` |
| | `diagnostics/test-store` | - | `SCREEN_25` (`SCREEN_49`) |
| | `diagnostics/ocr-inspector`| - | `SCREEN_13` |
| | `guide/apk-install` | - | `SCREEN_7` |

---

## 3. 기능 도메인별 Screen - Composable - ViewModel 1:1 매핑 명세

### 3.1 온보딩 및 계정 인증 (Auth Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **온보딩 및 미식 여정 소개** (`SCREEN_47`) | `feature/auth/OnboardingScreen.kt`<br>`fun OnboardingScreen()` | `AuthViewModel` | `StateFlow<OnboardingUiState>`<br>`onSkipClicked()`, `onStartClicked()` |
| **회원가입 및 미식가 등록** (`SCREEN_46`) | `feature/auth/SignUpScreen.kt`<br>`fun SignUpScreen()` | `AuthViewModel` | `SignUpUiState(email, pw, nickname, isValid, error)`<br>`onEmailChanged()`, `onSignUpSubmitted()` |
| **로그인 및 패스포트 복원** (`SCREEN_43`) | `feature/auth/LoginScreen.kt`<br>`fun LoginScreen()` | `AuthViewModel` | `LoginUiState(isLoading, authError)`<br>`onLoginClicked()`, `onResetPwClicked()` |
| **비밀번호 재설정 및 복구** (`SCREEN_40`) | `feature/auth/ResetPasswordScreen.kt`<br>`fun ResetPasswordScreen()` | `AuthViewModel` | `ResetPwUiState(emailSent: Boolean)`<br>`onSendResetMail(email: String)` |
| **식객 칭호 설정 및 호적부 복구** (`SCREEN_37`) | `feature/auth/NicknameSetupScreen.kt`<br>`fun NicknameSetupScreen()` | `AuthViewModel` | `NicknameSetupUiState(isChecking, isAvailable)`<br>`onClaimNickname(name: String)` |
| **식객 닉네임 변경 및 계정** (`SCREEN_21`) | `feature/profile/NicknameEditScreen.kt`<br>`fun NicknameEditScreen()` | `ProfileViewModel` | `NicknameEditUiState(currentName, newName, error)`<br>`onSubmitChange()` |
| **회원 탈퇴 및 호적부 영구 말소** (`SCREEN_22`)| `feature/profile/WithdrawScreen.kt`<br>`fun WithdrawScreen()` | `ProfileViewModel` | `WithdrawUiState(warningAcknowledged, isProcessing)`<br>`onConfirmPermanentDeletion()` |
| **식객 로그아웃 및 대기열 경고** (`SCREEN_33`)| `feature/settings/LogoutDialog.kt`<br>`fun LogoutWarningDialog()` | `SettingsViewModel`| `LogoutDialogUiState(pendingOpsCount: Int)`<br>`onForceLogout()`, `onCancel()` |

---

### 3.2 지도 및 노포 탐색 (Map & Store Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **지도 탐색 및 노포 발견** (`SCREEN_56`) | `feature/map/MapScreen.kt`<br>`fun MapScreen()` | `MapViewModel` | `MapUiState(stores, selectedStore, userLoc, isOffline)`<br>`onMarkerSelected()`, `onFilterApplied()` |
| **노포 검색 및 목록 보기** (`SCREEN_45`) | `feature/map/StoreListScreen.kt`<br>`fun StoreListScreen()` | `StoreListViewModel`| `StoreListUiState(query, results, sortType: DISTANCE/YEAR)`<br>`onSearchQueryChanged()`, `onSortChanged()` |
| **노포 지도 필터 및 정렬 설정** (`SCREEN_14`)| `feature/map/MapFilterSheet.kt`<br>`fun MapFilterBottomSheet()` | `MapViewModel` | `FilterConfigState(onlyFood, baeknyeon, notVisited)`<br>`onApplyFilters()` |
| **지도 오류 대체 목록** (`SCREEN_9`) | `feature/map/MapFallbackScreen.kt`<br>`fun MapFallbackScreen()` | `MapViewModel` | `MapFallbackUiState(errorCode, cachedStores)`<br>`onRetryConnection()`, `onItemClicked()` |
| **노포 상세 및 인증 자격** (`SCREEN_55`) | `feature/store/StoreDetailScreen.kt`<br>`fun StoreDetailScreen()` | `StoreDetailViewModel`| `StoreDetailUiState(store, isVisited, yearsAward, canReview)`<br>`onStartVerify()`, `onWriteReview()` |
| **노포 길찾기 및 대중교통** (`SCREEN_29`) | `feature/store/TransitGuideSheet.kt`<br>`fun TransitGuideSheet()` | `StoreDetailViewModel`| `TransitUiState(storeCoords, userCoords)`<br>`onLaunchNaverMapApp()`, `onOpenWebMap()` |
| **노포 정보 수정 제보 & 신고** (`SCREEN_30`) | `feature/store/StoreReportSheet.kt`<br>`fun StoreReportSheet()` | `StoreDetailViewModel`| `ReportFormUiState(reason, content, isSubmitting)`<br>`onSubmitReport()` |
| **사라진 노포 추모관** (`SCREEN_2`) | `feature/store/MemorialScreen.kt`<br>`fun MemorialScreen()` | `MemorialViewModel` | `MemorialUiState(closedStores, myMemorialStamps)`<br>`onLeaveMemoryNote()`, `onViewMyArchive()` |

---

### 3.3 방문 인증 파이프라인 (Verification Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **영수증 방문 인증 화면** (`SCREEN_53`) | `feature/verify/ReceiptVerifyScreen.kt`<br>`fun ReceiptVerifyScreen()` | `VerifyViewModel` | `ReceiptUiState(isAnalyzing, parsedData, error)`<br>`onCaptureReceipt()`, `onSwitchToPhotoVerify()` |
| **가게 사진 및 위치 인증** (`SCREEN_51`) | `feature/verify/PhotoVerifyScreen.kt`<br>`fun PhotoVerifyScreen()` | `VerifyViewModel` | `PhotoUiState(distanceM, accuracyM, secondsLeft, attempts)`<br>`onCaptureSignboard()`, `onForceRelief()` |
| **방문 인증 결과 & 대체 안내** (`SCREEN_38`)| `feature/verify/VerifyResultScreen.kt`<br>`fun VerifyResultScreen()` | `VerifyViewModel` | `VerifyResultUiState(resultCode, store, attemptCount)`<br>`onRetry()`, `onSwitchMethod()` |
| **위치 확인 도장 구제 발급** (`SCREEN_34`) | `feature/verify/LocationReliefSheet.kt`<br>`fun LocationReliefSheet()` | `VerifyViewModel` | `LocationReliefUiState(store, isEligible: Boolean)`<br>`onIssueLocationStamp()` |
| **영수증 중복 해시 반려** (`SCREEN_19`) | `feature/verify/ReceiptDuplicateSheet.kt`<br>`fun ReceiptDuplicateSheet()` | `VerifyViewModel` | `ReceiptDupUiState(dupHash, firstUsedDate)`<br>`onDismiss()`, `onTryPhotoVerify()` |
| **일일 인증 한도 및 쿨타임** (`SCREEN_26`)| `feature/verify/DailyLimitSheet.kt`<br>`fun DailyLimitSheet()` | `VerifyViewModel` | `DailyLimitUiState(todayCount: 5, cooldownHours)`<br>`onConfirmLimit()` |
| **위치 권한 및 대략 위치 경고** (`SCREEN_28`)| `feature/verify/LocationPermissionScreen.kt`<br>`fun LocationPermissionScreen()` | `VerifyViewModel` | `PermissionUiState(isCoarseOnly: Boolean)`<br>`onOpenAppSettings()` |

---

### 3.4 도장 및 패스포트 (Stamp & Passport Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **도장 획득 및 세월 적립 연출** (`SCREEN_54`)| `feature/stamp/StampCeremonyScreen.kt`<br>`fun StampCeremonyScreen()` | `StampViewModel` | `StampCeremonyUiState(stamp, prevYears, newYears)`<br>`onAnimationEnd()`, `onViewPassport()` |
| **칭호 승급 축하 연출** (`SCREEN_39`) | `feature/stamp/TitleUpOverlay.kt`<br>`fun TitleUpOverlay()` | `StampViewModel` | `TitleUpUiState(oldTitle, newTitle, years)`<br>`onDismissOverlay()`, `onShareCelebration()` |
| **내 패스포트 및 미식 연대기** (`SCREEN_52`)| `feature/passport/PassportScreen.kt`<br>`fun PassportScreen()` | `PassportViewModel` | `PassportUiState(totalYears, nextTitleYears, stamps)`<br>`onStampClicked(id)`, `onViewReport()` |
| **내 패스포트 (도장 0개 첫 시작)** (`SCREEN_36`)| `feature/passport/PassportEmptyView.kt`<br>`fun PassportEmptyView()` | `PassportViewModel` | `PassportEmptyUiState(starterStoreList)`<br>`onExploreFirstStore()` |
| **도장 상세 및 방문 각인 기록** (`SCREEN_42`)| `feature/passport/StampDetailSheet.kt`<br>`fun StampDetailSheet()` | `PassportViewModel` | `StampDetailUiState(stamp, store, photoUrl, visitCount)`<br>`onViewStoreDetail()`, `onDelete()` |
| **100년 황금 도장 인증서** (`SCREEN_15`) | `feature/passport/GoldCertificateSheet.kt`<br>`fun GoldCertificateSheet()` | `PassportViewModel` | `GoldCertUiState(store, certNo, officialSeals)`<br>`onDownloadCertImage()` |
| **식객 미식 여정 통계 결산** (`SCREEN_4`) | `feature/passport/ArchiveReportScreen.kt`<br>`fun ArchiveReportScreen()` | `PassportViewModel` | `ArchiveReportUiState(stats, eraDistribution, charts)`<br>`onExportReportImage()` |
| **미식 패스포트 공유 카드 생성기** (`SCREEN_24`)| `feature/passport/ShareCardScreen.kt`<br>`fun ShareCardScreen()` | `PassportViewModel` | `ShareCardUiState(cardTheme, selectedBadge, previewUrl)`<br>`onShareToInstagram()`, `onSaveToGallery()` |
| **지역 컬렉션 & 테마 퀘스트** (`SCREEN_20`)| `feature/passport/QuestCollectionScreen.kt`<br>`fun QuestCollectionScreen()` | `PassportViewModel` | `QuestListUiState(quests, activeQuestProgress)`<br>`onSelectQuest(id)` |
| **식객 명예의 전당 & 순위** (`SCREEN_27`) | `feature/passport/HallOfFameScreen.kt`<br>`fun HallOfFameScreen()` | `PassportViewModel` | `HallOfFameUiState(topUsers, myRank)`<br>`onUserSelected(uid)` |

---

### 3.5 리뷰 및 프로필 (Review & Profile Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **공인 미식 평점 & 리뷰 작성** (`SCREEN_48`)| `feature/review/ReviewEditorScreen.kt`<br>`fun ReviewEditorScreen()` | `ReviewEditorViewModel`| `ReviewEditorUiState(store, rating, text, isSubmitting)`<br>`onRatingChanged()`, `onSubmitReview()` |
| **공인 리뷰 수정 및 삭제** (`SCREEN_18`) | `feature/review/ReviewModifierScreen.kt`<br>`fun ReviewModifierScreen()` | `ReviewEditorViewModel`| `ReviewModifyUiState(originalReview, updatedText)`<br>`onSaveEdit()`, `onDeleteReview()` |
| **오프라인 리뷰 작성 초안 보존** (`SCREEN_17`)| `feature/review/OfflineReviewBanner.kt`<br>`fun OfflineReviewBanner()` | `ReviewEditorViewModel`| `OfflineDraftState(hasDraft: Boolean, lastSavedTime)`<br>`onRestoreDraft()`, `onDiscardDraft()` |
| **노포 공인 미식가 리뷰 목록** (`SCREEN_32`)| `feature/review/StoreReviewsScreen.kt`<br>`fun StoreReviewsScreen()` | `StoreDetailViewModel`| `StoreReviewsUiState(reviews: Flow<PagingData<Review>>)`<br>`onUserProfileClicked()`, `onReportReview()` |
| **내 패스포트 - 내 작성 리뷰** (`SCREEN_31`)| `feature/review/MyReviewsScreen.kt`<br>`fun MyReviewsScreen()` | `PassportViewModel` | `MyReviewsUiState(myReviews, reviewCount)`<br>`onEditReview()`, `onDeleteReview()` |
| **타 유저 미식가 공인 프로필** (`SCREEN_50`)| `feature/profile/UserProfileScreen.kt`<br>`fun UserProfileScreen()` | `UserProfileViewModel` | `UserProfileUiState(profile, userStamps, userReviews)`<br>`onStoreClicked()`, `onBlockUser()` |
| **방문 기록 비공개 설정** (`SCREEN_16`) | `feature/profile/PrivacySettingsSheet.kt`<br>`fun PrivacySettingsSheet()` | `ProfileViewModel` | `PrivacyConfigUiState(isTimelinePublic, hideVisitDates)`<br>`onTogglePublicVisibility()` |

---

### 3.6 시스템 설정 및 개발자 진단 (System & Diagnostics Domain)

| UI 화면명 (ID) | Composable 파일 및 함수 | ViewModel | UI State & Events |
|---|---|---|---|
| **설정 및 데이터 출처 안내** (`SCREEN_44`)| `feature/settings/SettingsScreen.kt`<br>`fun SettingsScreen()` | `SettingsViewModel`| `SettingsUiState(catalogVer, storeCount, appVersion)`<br>`onTapVersionFiveTimes()`, `onLogout()` |
| **오프라인 모드 & 대기열 동기화** (`SCREEN_35`)| `feature/settings/OfflineQueueScreen.kt`<br>`fun OfflineQueueScreen()` | `SettingsViewModel`| `QueueUiState(pendingVisitsCount, isSyncing)`<br>`onForceSyncNow()`, `onClearQueue()` |
| **식객 알림 및 세월 동기화 센터** (`SCREEN_5`)| `feature/settings/NotificationsScreen.kt`<br>`fun NotificationsScreen()` | `SettingsViewModel`| `NotificationFeedUiState(alerts, unreadCount)`<br>`onMarkAllRead()`, `onNotificationTap()` |
| **서버 방문 기록 복원/동기화** (`SCREEN_8`)| `feature/settings/RestoreSyncScreen.kt`<br>`fun RestoreSyncScreen()` | `SettingsViewModel`| `RestoreProgressUiState(percent: 79%, stageText)`<br>`onCancelRestore()`, `onComplete()` |
| **개인정보/위치 처리방침** (`SCREEN_23`)| `feature/settings/LegalDocumentScreen.kt`<br>`fun PrivacyPolicyScreen()` | - | 정적 Compose 웹뷰/마크다운 뷰어 |
| **오픈소스 라이선스 고지** (`SCREEN_41`)| `feature/settings/LegalDocumentScreen.kt`<br>`fun LicensesScreen()` | - | `LibrariesContainer` (AboutLibraries) |
| **1일차 스파이크 종합 진단 콘솔** (`SCREEN_6`)| `feature/diagnostics/SpikeDiagnosticsScreen.kt`<br>`fun SpikeDiagnosticsScreen()` | `DiagnosticsViewModel`| `SpikeProbesUiState(mapOk, camOk, ocrLatency, gpsAcc)`<br>`onRunAllProbes()`, `onCopyLogs()` |
| **테스트 가게 생성 시뮬레이터** (`SCREEN_25`)| `feature/diagnostics/TestStoreScreen.kt`<br>`fun TestStoreScreen()` | `DiagnosticsViewModel`| `TestStoreUiState(currentLocation, testStoreId)`<br>`onCreateTestStore()`, `onDeleteTestStore()` |
| **테스트 영수증 OCR 인스펙터** (`SCREEN_13`)| `feature/diagnostics/OcrInspectorScreen.kt`<br>`fun OcrInspectorScreen()` | `DiagnosticsViewModel`| `OcrInspectUiState(rawText, phone, bizNo, paidAt, sim)`<br>`onTestSampleImage()`, `onExportLog()` |
| **시스템 진단 콘솔 (5-Tap)** (`SCREEN_49`)| `feature/diagnostics/DiagnosticsScreen.kt`<br>`fun DiagnosticsScreen()` | `DiagnosticsViewModel`| `DiagnosticsUiState(deviceInfo, memoryDump, sqliteSize)`<br>`onClearCache()`, `onResetDatabase()` |
| **릴리스 서명 APK 설치 안내** (`SCREEN_7`)| `feature/guide/ApkInstallGuideScreen.kt`<br>`fun ApkInstallGuideScreen()` | - | 정적 스텝 가이드 + SHA-256 클립보드 복사 |

---

## 4. UI 상태 관리 및 데이터 흐름 패턴 (UDF Architecture)

모든 Composable은 **단방향 데이터 흐름(Unidirectional Data Flow)** 원칙을 엄격히 준수합니다.

```
       [ User Action (Tap, Scroll, Input) ]
                     │
                     ▼
          Composable Event Callback
           (e.g., onStartVerify())
                     │
                     ▼
             ViewModel Method
           (e.g., verifyReceipt())
                     │
                     ▼
             Domain UseCase / Repository
                     │
                     ▼
       StateFlow<UiState> emit / update
                     │
                     ▼
[ Composable Recomposition via collectAsStateWithLifecycle() ]
```

### 4.1 ViewModel 및 StateFlow 표준 구현 템플릿

```kotlin
// feature/verify/VerifyViewModel.kt 예시
class VerifyViewModel(
    private val verifyReceiptUseCase: VerifyReceiptUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Ready)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    // 일회성 UI 효과 (진동, 화면 이동 이벤트)
    private val _eventChannel = Channel<VerifyEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun onCaptureReceipt(imageProxy: ImageProxy, storeId: String) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Analyzing
            
            when (val result = verifyReceiptUseCase(storeId, imageProxy)) {
                is AppResult.Success -> {
                    _uiState.value = ReceiptUiState.Success(result.data)
                    _eventChannel.send(VerifyEvent.TriggerHaptic)
                    _eventChannel.send(VerifyEvent.NavigateToCeremony(result.data))
                }
                is AppResult.Failure -> {
                    _uiState.value = ReceiptUiState.Failed(result.error)
                }
            }
        }
    }
}
```

### 4.2 Screen Composable 수집 표준 템플릿

```kotlin
// feature/verify/ReceiptVerifyScreen.kt
@Composable
fun ReceiptVerifyRoute(
    storeId: String,
    viewModel: VerifyViewModel = viewModel(),
    onNavigateToCeremony: (AwardResult) -> Unit,
    onSwitchToPhotoVerify: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 일회성 사이드 이펙트 수집
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is VerifyEvent.TriggerHaptic -> Haptics.performStampImpact(context)
                is VerifyEvent.NavigateToCeremony -> onNavigateToCeremony(event.award)
            }
        }
    }

    ReceiptVerifyScreen(
        uiState = uiState,
        onCapture = { proxy -> viewModel.onCaptureReceipt(proxy, storeId) },
        onSwitchToPhoto = { onSwitchToPhotoVerify(storeId) },
        onBackClick = onBackClick
    )
}
```

---

## 5. 공통 UI 컴포넌트 라이브러리 매핑 (`core/designsystem/component`)

51개 화면 전체에서 일관된 비주얼 아이덴티티와 재사용성을 보장하는 핵심 공통 컴포넌트 목록입니다.

| 컴포넌트명 | 파일 경로 | 주요 파라미터 | 사용처 |
|---|---|---|---|
| `StampView` | `core/designsystem/component/StampView.kt` | `grade: StampGrade`, `years: Int`, `size: Dp`, `isRelief: Boolean` | 도장 각인 연출, 패스포트 격자, 도장 상세 시트 |
| `YearsGauge` | `core/designsystem/component/YearsGauge.kt` | `currentYears: Int`, `targetYears: Int`, `modifier: Modifier` | 내 패스포트 헤더, 연대기 리포트 통계 |
| `TitleBadge` | `core/designsystem/component/TitleBadge.kt` | `title: Title`, `onClick: (() -> Unit)?` | 공인 리뷰 작성자 배지, 프로필 헤더 |
| `NopoButton` | `core/designsystem/component/NopoButton.kt` | `text: String`, `style: ButtonStyle (Teal/Stamp/Brass)`, `onClick: () -> Unit` | 주요 CTA 버튼 (최소 48dp 터치 타깃 보장) |
| `NopoTopAppBar`| `core/designsystem/component/NopoTopAppBar.kt`| `title: String`, `navigationIcon: ImageVector?`, `actions: @Composable RowScope.() -> Unit` | 전체 화면 상단 헤더 바 |
| `NopoBottomBar`| `core/designsystem/component/NopoBottomBar.kt`| `currentRoute: String`, `onTabSelected: (String) -> Unit` | 3대 글로벌 내비게이션 바 (지도/패스포트/설정) |
| `OfflineBanner`| `core/designsystem/component/OfflineBanner.kt`| `isOffline: Boolean`, `pendingCount: Int`, `onSyncClick: () -> Unit` | 오프라인 진입 시 전역 상단 알림 바 |
| `EmptyStateView`| `core/designsystem/component/EmptyStateView.kt`| `icon: ImageVector`, `title: String`, `description: String`, `actionText: String?` | 도장 0개 빈 상태, 리뷰 없음, 검색 결과 없음 |

---

## 6. 개발 빌드 및 점검 체크리스트 (Claude Code 전달용)

1. **단일 모듈 순수 도메인 원칙 준수**:
   - `domain/` 패키지 내부 코드에는 `android.*` 임포트가 일절 없어야 하며, JUnit4 단위 테스트로 독립 검증 가능해야 합니다.
2. **Compose 생명주기 안정성**:
   - 모든 화면의 상태 구독은 반드시 `collectAsStateWithLifecycle()`을 사용합니다.
   - `NaverMapView`는 `AndroidView` 내부에서 호스트 액티비티의 생명주기 8개(`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`, `onSaveInstanceState`, `onLowMemory`)를 완벽히 전달해야 합니다.
3. **접근성 및 터치 타깃**:
   - 모든 대화형 아이콘 및 버튼은 `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`를 만족해야 합니다.
   - 글자 크기 200% 확대(Font Scaling 2.0x) 환경에서도 텍스트 잘림이 없도록 `TextOverflow.Ellipsis` 및 Scrollable 컨테이너 처리가 완료되어야 합니다.
4. **R8/ProGuard 난독화 안전성**:
   - 카탈로그 파싱 모델 `StoreJson` 및 Firestore DTO 클래스는 `proguard-rules.pro`에 keep 규칙을 고정합니다.

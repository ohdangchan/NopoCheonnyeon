> **[v2 문서]** 2026-10-23 작성. 원본 `docs/nopo-02-architecture.md` 를 복사해 시안(design/redesign/1~6.webp) 우선 기준으로 고친 문서입니다.
> 원본은 그대로 두며, 이 문서와 원본이 다르면 **이 v2 문서가 우선**입니다. 변경 요약은 [v2-00-changes.md](v2-00-changes.md) 참조.

# 노포 천년 미식가 — 아키텍처 설계서 v0.4

## ★ v0.3 최종 반영 (2026-10-23 밤) — 구조 변경 사항
- **화면 복제 방식**: `core/designsystem/mock` — 시안 폭 760px 좌표(`MockStage`·`Modifier.at`), 글자는 `snapTextPx` 단계표, 시안 조각은 `res/drawable-nodpi/mk_*·mk2_*` (생성 도구 `tools/crop_mockups*.py`, 아이콘 `tools/make_icon.py`).
- **로그인 게이트**: `AuthRepository.sessionState`(프로필 확인 5초 제한) + `AuthViewModel.settle()`로 로그인 직후 화면 전환까지 busy 유지. 구글은 Credential Manager(`platform/auth/GoogleSignIn`).
- **AI**: `GeminiClient`(Firebase AI Logic, 호출 시간 로그) · `AiRepository.warmUp()`(룰렛 열 때 연결 예열) · 룰렛 AI는 4초 제한 후 `fallbackReason` · 촬영 도우미 1.8초 제한 · 번역은 `StoreTranslator`가 소개글(2개)을 먼저, 나머지를 4개씩 동시 번역.
- **룰렛**: `feature/ai/RouletteWheel.kt`(Canvas로 그린 완전한 판) + `PhotoReel(rows, frame)`, 상태 `picks`·`shownIds`(30곳 비중복).
- **인증**: `VerifyViewModel`에 `myLat/myLng`(지도 표시 전용, 저장·전송 안 함), GPS 화면의 `VerifyMap`(네이버 지도 마커 2개), 가게 사진 단계 레이아웃 재구성, `ReceiptScanOverlay` 연두 스캔선.
- **탭 연동**: `AppContainer.requestedTab`·`requestedExploreChip`(홈 칩 → 탐색), 알림 `onOpenTab`.
- **위치 제한**: `VerificationConfig.LOCATION_TIMEOUT_MS = 10초`.
- 변경 없음: 수동 주입, Room·DataStore·WorkManager, 영수증 위치 미사용·이미지 미저장 규칙, 패키지명 동일 규칙.

**남은 사용자 확인·결정 (아직 유효)**
- 구글 로그인: Firebase 콘솔 Google 사용 설정 + SHA 지문 + 새 `google-services.json` (`google-login-setup.md`)
- 릴리스 서명 키 SHA 등록, 고객센터 이메일 운영 주소 교체(지금은 `wow.sillyer@gmail.com` 임시)
- 서울관광재단 비짓서울 사진·이야기 글: 전화 답변만 있고 **서면 허가 대기**
- 폴드8 지도 탭은 실기기 미확인(테스트 가게 제외 + 8초 안에 지도가 안 뜨면 목록으로 대체하는 방어만 적용)

관련 문서: `nopo-01-PRD.md`(요구사항), `nopo-03-stories.md`(구현 단위), `nopo-06-spike-plan.md`(1일차 기능 검증), `nopo-07-firebase-setup.md`(서버 설정)

> **⚠ 2026-10-20 현황 주의**: 이 문서는 설계 기준이며 구현과 다른 곳이 있다 — Room은 `fallbackToDestructiveMigration` 사용 중(6.1), 실제 보안 규칙(`firebase/firestore.rules`)은 닉네임 변경 허용(6.3), 릴리스 abi는 `arm64-v8a`만(7.3), R8은 `-dontoptimize`(7.4), 패키지 `feature/stamp`·`feature/review`·`platform/haptics`·`data/sync`·`domain/repository`는 만들어지지 않았다. 자세한 목록은 `docs/nopo-11-backlog.md` C-01.

> 설계 원칙은 하나입니다. **실제 폰에서 버그 없이 도는 것.** 그래서 v0.3보다 라이브러리를 줄이고(Hilt·비공식 지도 래퍼 제거), 공식 SDK를 직접 쓰며, 모든 기기 기능 호출에 제한 시간과 실패 처리를 둡니다.

> 라이브러리 버전: 확인된 값은 네이버 지도 SDK `3.24.0`, ML Kit 한국어 `16.0.1`뿐입니다. 나머지는 Android Studio 최신 안정판의 새 프로젝트 템플릿이 만든 버전과, Android Studio가 제안하는 최신 안정 버전을 쓰고 `libs.versions.toml`에 고정합니다.

---

## 1. 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| 개발 도구 | Android Studio + Claude Code (JetBrains 플러그인) | 사용자 결정. Claude Code가 빌드·설치·로그 확인까지 터미널에서 수행 |
| 언어 / UI | Kotlin, Jetpack Compose, Material 3 | |
| 구조 | MVVM + 단방향 데이터 흐름 | 화면 상태가 한 방향으로만 바뀌어 상태 버그가 줄어듦 |
| 의존성 주입 | **수동 주입 (`AppContainer`)** | Hilt의 플러그인·코드 생성 설정 오류를 없앰. 화면 수가 적어 충분 |
| 비동기 | Coroutines, Flow, `kotlinx-coroutines-play-services`(`await()`) | |
| 내비게이션 | Navigation Compose | 로그인 게이트 그래프 |
| 지도 | **네이버 지도 SDK 3.24.0 공식 `MapView`를 `AndroidView`로 직접 사용** | v0.3의 비공식 Compose 래퍼 제거. 공식 SDK의 마커·클러스터링 사용 |
| 위치 | Play 서비스 Fused Location | 정확도·모의 위치 판정 |
| 카메라 | CameraX (`ImageCapture`) | 기기별 차이를 라이브러리가 흡수 |
| QR 만들기 | ZXing core 3.5.3(Apache-2.0, 2026-10-20 추가) | 패스포트 공유 카드의 QR. 읽기는 하지 않음 |
| 글자 인식 | ML Kit Text Recognition v2 한국어, **앱에 포함** | 첫 사용 다운로드 없음, 오프라인 동작 |
| 로그인·서버 | Firebase Authentication(이메일/비밀번호), Cloud Firestore | 공식 Android SDK, 오프라인 캐시 내장 |
| 크래시 | Firebase Crashlytics | QA 중 크래시 0 확인 |
| 기기 DB | Room (KSP) | 카탈로그, 내 방문, 전송 대기열 |
| 설정 저장 | DataStore (Preferences) | |
| 백그라운드 전송 | WorkManager | 앱을 닫아도 연결되면 대기열 전송 |
| JSON | kotlinx.serialization | `stores.json` 파싱 |
| 이미지 | Coil | 인증샷 표시 |
| (선택, P1) 연출 | Lottie (`lottie-compose`) | 칭호 승급 연출 S-705. P0에는 넣지 않음 |
| 디버그 도구 | LeakCanary(디버그만), StrictMode(디버그만) | 메모리 누수·메인 스레드 작업 조기 발견 |
| 테스트 | JUnit4, MockK, Turbine, kotlinx-coroutines-test, Compose UI Test | |

---

## 2. 프로젝트 구조

단일 앱 모듈 + 패키지 분리. 저장소 루트에 문서·데이터·도구·Firebase 설정을 함께 둔다.

```
NopoCheonnyeon/
├─ CLAUDE.md                     Claude Code 작업 지침
├─ .claude/settings.json         명령 허용 목록
├─ .claude/agents/nopo-qa.md     QA 서브에이전트
├─ docs/                         PRD·아키텍처·스토리·체크리스트·기획안 HTML
├─ data/                         수집 CSV, 확인용 SQLite, README
├─ tools/                        수집·좌표 보강·카탈로그 생성 스크립트
├─ firebase/                     firestore.rules, firestore.indexes.json, firebase.json
├─ gradle/libs.versions.toml
└─ app/src/main/
    ├─ AndroidManifest.xml
    ├─ assets/stores.json         카탈로그 (tools/build_catalog.py 출력)
    ├─ res/font/                  black_han_sans.ttf, noto_sans_kr_*.ttf
    └─ java/com/nopo/cheonnyeon/
        ├─ NopoApp.kt              Application: AppContainer, 지도 SDK·Firebase·카탈로그 초기화
        ├─ MainActivity.kt         단일 액티비티, NopoNavHost
        ├─ AppContainer.kt         수동 주입: 저장소·포트·유스케이스 생성
        ├─ core/
        │   ├─ designsystem/       theme(Color, Type, Shape, NopoTheme), component(StampView, YearsGauge, TitleBadge, StarRating, NopoButton, EmptyState, OfflineBanner)
        │   ├─ common/             AppResult, AppError, Clock, DispatcherProvider
        │   └─ util/               GeoDistance, TextNormalizer, Similarity, BusinessNumber, PhoneNumber
        ├─ domain/
        │   ├─ model/              Store, Visit, Review, UserProfile, Title, StampGrade, VerifyMethod
        │   ├─ rule/               YearsCalculator, StampGradePolicy, TitlePolicy, DailyLimitPolicy, VerificationConfig, ReviewRules
        │   ├─ verify/             ReceiptParser, ReceiptMatcher, ReceiptHasher, PhotoMatcher, VerifyReceiptUseCase, VerifyPhotoUseCase
        │   └─ repository/         CatalogRepository, AuthRepository, ProfileRepository, VisitRepository, ReviewRepository (인터페이스)
        ├─ data/
        │   ├─ catalog/            StoreJson(직렬화 모델), CatalogSeeder
        │   ├─ local/              AppDatabase, StoreDao, MyVisitDao, PendingOpDao, 엔터티, SettingsStore(DataStore)
        │   ├─ remote/             AuthRemote, UserRemote, VisitRemote, ReviewRemote, ReceiptHashRemote, NicknameRemote
        │   ├─ sync/               SyncWorker, SyncScheduler
        │   └─ repository/         각 저장소 구현
        ├─ platform/
        │   ├─ location/           LocationPort, FusedLocationAdapter, MockDetector, LocationSettingsChecker
        │   ├─ camera/             CameraCapture (CameraX)
        │   ├─ ocr/                TextRecognizerPort, MlKitKoreanRecognizer
        │   ├─ map/                NaverMapView (AndroidView + 생명주기 전달), MarkerFactory, ClusterSupport
        │   └─ haptics/            Haptics
        └─ feature/
            ├─ diagnostics/        DiagnosticsScreen(탭: 지도·카메라·OCR·위치·Firebase), TestStoreFactory  ← 1일차 오전에 가장 먼저 만듦
            ├─ auth/               OnboardingScreen, LoginScreen, SignUpScreen, ResetPasswordScreen, NicknameSetupScreen, AuthViewModel
            ├─ map/                MapScreen, StoreListScreen, MapViewModel
            ├─ store/              StoreDetailScreen, StoreDetailViewModel, StoreReviewsSection
            ├─ verify/             ReceiptVerifyScreen, PhotoVerifyScreen, VerifyResultScreen, VerifyViewModel
            ├─ stamp/              StampCeremonyScreen
            ├─ passport/           PassportScreen, StampDetailSheet, MyReviewsTab, PassportViewModel
            ├─ review/             ReviewEditorScreen, ReviewEditorViewModel
            ├─ profile/            UserProfileScreen, UserProfileViewModel
            └─ settings/           SettingsScreen, DataSourceScreen, LicensesScreen
```

의존 방향: `feature → domain ← data / platform`. `domain`은 안드로이드 코드에 의존하지 않는다(순수 Kotlin) → 핵심 규칙은 폰 없이 단위 테스트로 검증.

---

## 3. 화면 흐름과 로그인 게이트

```
앱 시작 → CatalogSeeder(필요 시) → 로그인 상태?
  ├─ 아니오 → [auth 그래프] 온보딩 → 로그인 ↔ 가입 ↔ 비밀번호 재설정
  └─ 예 → users/{uid} 문서 있음?
        ├─ 아니오 → 닉네임 정하기
        └─ 예 → [main 그래프] 하단 탭: 지도 | 패스포트 | 설정
              지도 → 가게 상세 → 영수증 인증 → (사진·위치 인증) → 결과/도장 연출
              가게 상세 → 리뷰 작성 / 리뷰 항목 → 유저 프로필 → 가게 상세 …
              설정 → 앱 정보 5회 탭 → 진단(테스트 가게)
```
- `NopoNavHost`는 `AuthRepository.sessionState: StateFlow<SessionState>`(SignedOut / NeedsNickname / SignedIn)만 보고 그래프를 바꾼다. 화면 코드가 직접 이동 판단을 하지 않는다.
- 경로 인자: `store/{storeId}`, `verify/{storeId}`, `profile/{uid}`, `review/{storeId}`.

---

## 4. 계층별 책임과 상태 모델

| 계층 | 책임 | 하지 않는 일 |
|---|---|---|
| Composable | 상태 그리기, 이벤트 전달 | 규칙 판단, 저장소 호출 |
| ViewModel | `StateFlow<UiState>` 노출, 이벤트 처리, `SavedStateHandle`로 복원 | `Context` 보관 |
| UseCase | 사용자 행동 하나(영수증 인증, 사진 인증) | UI |
| Repository | 기기 DB·서버·대기열을 하나로 | 화면 로직 |
| Port | 위치·카메라·OCR·지도 감싸기 | 규칙 판단 |

인증 화면 상태 예:
```kotlin
sealed interface ReceiptUiState {
    data object Ready : ReceiptUiState
    data object Analyzing : ReceiptUiState
    data class Failed(val error: VerifyError, val attempts: Int) : ReceiptUiState
    data class Success(val award: AwardResult) : ReceiptUiState
    data class PendingSync(val award: AwardResult) : ReceiptUiState   // 오프라인, 확인 중
}
sealed interface PhotoUiState {
    data class Locating(val secondsLeft: Int, val bestAccuracyM: Int?) : PhotoUiState
    data class TooFar(val distanceM: Int, val accuracyM: Int) : PhotoUiState
    data object ReadyToCapture : PhotoUiState
    data object Analyzing : PhotoUiState
    data class NoMatch(val attempts: Int, val canRecordLocationOnly: Boolean) : PhotoUiState
    data class Success(val award: AwardResult) : PhotoUiState
    data class Failed(val error: VerifyError) : PhotoUiState
}
```
일회성 효과(진동, 화면 이동)는 `Channel` → `receiveAsFlow()`.

---

## 5. 핵심 컴포넌트

### 5.1 영수증 인증 (위치 사용 안 함)
```
VerifyReceiptUseCase(storeId, image):
  1 DailyLimitPolicy.check(uid, today, storeId)          → E_DAILY_LIMIT / E_ALREADY_TODAY
  2 TextRecognizerPort.read(image)  (withTimeout 10s)     → E_RECEIPT_UNREADABLE
  3 ReceiptParser.parse(text)                             → ParsedReceipt(phone, bizNo, paidAt, amount, nameLine)
  4 ReceiptMatcher.match(parsed, store, now)              → E_RECEIPT_MISMATCH / E_RECEIPT_EXPIRED / E_RECEIPT_NO_DATE
  5 ReceiptHasher.hash(parsed)
  6 VisitRepository.recordVisit(store, RECEIPT, hash)     → 기기 저장 + 대기열 → 서버 묶음 쓰기
       서버 거부(해시 중복) → 기기 기록 되돌림 + E_RECEIPT_DUP
  7 TitlePolicy 비교 → AwardResult(years, grade, titleUp, pending)
```
- 이미지는 `ImageProxy`에서 바로 `InputImage`로 넘기고 파일로 저장하지 않는다. 처리 후 `close()`.

### 5.2 사진·위치 인증
```
VerifyPhotoUseCase(storeId):
  1 store.lat/lng 없음 → E_NO_COORD
  2 권한: FINE 없음 → E_LOC_APPROX_ONLY / E_LOC_DENIED
  3 LocationSettingsChecker: 기기 위치 꺼짐 → 시스템 켜기 대화상자
  4 FusedLocationAdapter.bestFix(15s): 1초 간격 업데이트, 정확도 ≤ 10m면 조기 종료, 가장 작은 accuracy 선택
     isMock(API 31+) / isFromMockProvider(이하) → E_LOC_MOCK
  5 distance ≤ 30 && accuracy ≤ 30 ? 아니면 E_FAR / E_ACCURACY (값 표시, 재측정)
  6 촬영 → 파일 저장(filesDir/stamps/{storeId}.jpg, 긴 변 1080px) → OCR
  7 PhotoMatcher: 가게명·별칭 유사도 ≥ 0.60 또는 전화번호 → 성공(photoVerified=true)
     실패 → attempts++, 3회 실패 && ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL → '위치 확인 도장' 선택지(method=LOCATION_ONLY)
  8 VisitRepository.recordVisit(...)
```
- 위치 업데이트는 `callbackFlow` + `awaitClose { removeLocationUpdates }`로 반드시 해제.
- 측정 중 남은 초와 현재 최선 정확도를 화면에 보여 준다.

### 5.3 방문 기록과 서버 동기화
- 기기 먼저: `my_visits`에 기록하고 UI를 즉시 갱신 → `pending_ops`에 작업 추가 → `SyncScheduler.enqueue()`.
- `SyncWorker`(네트워크 연결 조건, 지수 백오프): 작업마다 Firestore **묶음 쓰기(batch)** 한 번:
  - 첫 방문: `receiptHashes/{hash}` 생성(영수증일 때) + `users/{uid}/visits/{storeId}` 생성 + `users/{uid}` 합계 갱신(`totalYears += years`, `visitCount += 1`, `titleId`)
  - 재방문: `visits/{storeId}`의 `visitCount += 1`, `lastVisitedAt`
- 해시 문서가 이미 있으면 보안 규칙상 “수정”으로 처리돼 거부(PERMISSION_DENIED) → 중복으로 판정하고 기기 기록을 되돌린 뒤 알림.
  - 단, 되돌리기 전에 `users/{uid}/visits/{storeId}`를 서버에서 한 번 읽는다. 문서가 있으면 이미 올라간 기록(앞선 전송이 성공했는데 응답만 못 받은 경우)이므로 **성공으로 처리**하고, 없을 때만 중복으로 본다. 다른 이유의 거부를 “중복 영수증”으로 잘못 알리고 기록을 지우는 일을 막기 위함.
- 동기화는 한 길로만: `WorkManager.enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, …)`. 같은 작업이 동시에 두 번 돌지 않게 한다.
- 앱이 화면에 있을 때 네트워크 조건 작업은 보통 몇 초 안에 실행되어 인증 직후 평점 버튼이 곧 켜진다. 실기기 확인(M-48)에서 지연이 길면 expedited 작업으로 바꾼다(API 30 이하는 `getForegroundInfo()` 구현이 필요하므로 그때만).
- 내 패스포트 숫자는 기기 `my_visits` 합으로 계산(오프라인에서도 일치), 다른 유저는 서버 `users/{uid}` 값.
- 로그인 직후 `VisitRepository.restoreFromServer()`로 서버 방문 기록을 내려받아 `my_visits`를 채운다.

### 5.4 리뷰
- 문서 ID `"{storeId}_{uid}"` → 사용자당 가게당 1개가 구조로 보장.
- 별점만 저장할 때도 `text`는 빈 문자열 `""`로 넣는다(규칙이 문자열을 요구). 새 리뷰의 `createdAt`은 `FieldValue.serverTimestamp()`(규칙이 서버 시각과 같은지 확인), 수정 때는 기존 값을 그대로 둔다.
- 저장: 트랜잭션으로 기존 리뷰 읽기 → 새 글이면 `users/{uid}.reviewCount + 1`, 수정이면 `createdAt` 유지 → `set`.
- 삭제: 트랜잭션으로 리뷰 삭제 + `reviewCount - 1`.
- 평균·개수: Firestore 집계 쿼리
```kotlin
val q = db.collection("reviews").whereEqualTo("storeId", storeId)
val snap = q.aggregate(AggregateField.count(), AggregateField.average("rating"))
    .get(AggregateSource.SERVER).await()
val count = snap.count
val avg = snap.get(AggregateField.average("rating"))   // Double?
```
- 목록: `whereEqualTo("storeId", id).orderBy("updatedAt", DESC).limit(20)`, 다음 페이지는 `startAfter(마지막 문서)`.
- 리뷰 저장·삭제는 트랜잭션이라 **온라인에서만** 된다(Firestore 트랜잭션은 오프라인에서 실패). 오프라인이면 저장 버튼을 막고 `E_REVIEW_OFFLINE`, 초안은 `SavedStateHandle`에 유지.
- 보안 규칙은 `visits/{storeId}` 문서가 서버에 있어야 리뷰를 허용한다. 그래서 평점 버튼은 `my_visits.syncState == SYNCED`일 때만 활성. 그 전에는 `E_VISIT_SYNCING` 표시 + `SyncScheduler.enqueueNow()`, 동기화 완료를 관찰해 자동 활성.

### 5.5 계정과 닉네임
```
가입: 닉네임 형식 검사 → nicknames/{소문자} 존재 확인(공개 읽기)
    → createUserWithEmailAndPassword
    → 트랜잭션: nicknames/{소문자} 없음 확인 후 생성 + users/{uid} 생성
    → 실패(경합) → NeedsNickname 상태 → 닉네임 정하기 화면
로그인: signInWithEmailAndPassword → users 문서 확인 → restoreFromServer
로그아웃: 대기열이 비어 있지 않으면 경고 → Room의 사용자 데이터 삭제(카탈로그 유지) → signOut
```
- 오류 매핑: `FirebaseAuthUserCollisionException` → AUTH_EMAIL_IN_USE, `FirebaseAuthWeakPasswordException` → AUTH_WEAK_PASSWORD, `FirebaseAuthInvalidCredentialsException`·`FirebaseAuthInvalidUserException` → AUTH_WRONG_CREDENTIAL, `FirebaseTooManyRequestsException` → AUTH_TOO_MANY, `FirebaseNetworkException` → E_NETWORK.

### 5.6 지도
- `NaverMapView`: `AndroidView(factory = { MapView(it).apply { onCreate(null) } })` + `LocalLifecycleOwner`의 생명주기를 `onStart/onResume/onPause/onStop/onDestroy`로 전달, `onSaveInstanceState`, `onLowMemory` 처리. **`onCreate`를 빠뜨리면 지도가 빈 화면일 수 있다**(공식 문서는 8개 메서드 전달을 요구). `getMapAsync`로 `NaverMap` 획득.
- 인증 실패 리스너(`NaverMapSdk.onAuthFailedListener`)로 401/429/800을 받아 진단 화면과 지도 오류 상태(목록으로 전환)에 표시.
- 마커: 공식 SDK의 클러스터링 기능을 우선 사용. 1일차 기능 검증에서 카탈로그 전체(1,000개 이상) 마커로 끊김을 확인하고, 문제가 있으면 화면 영역 안 가게만 표시 + 축소 시 격자 묶음으로 대체.
- 마커 아이콘 `OverlayImage`는 등급별 3개만 만들어 재사용.

### 5.7 카탈로그 적재
- `assets/stores.json` = `{ "version": <int>, "generatedAt": "...", "stores": [ ... ] }`
- `CatalogSeeder`: DataStore의 `catalogVersion`과 다르면 파일을 읽어 한 트랜잭션으로 `stores` 테이블 교체. 실패 시 이전 데이터 유지, 다음 실행에 재시도. 첫 실행은 스플래시에서 진행 표시.

### 5.8 테스트 가게
- `TestStoreFactory.createAt(lat, lng)` → `stores`에 `id="TEST-0001"`, `isTest=true` 행 추가.
- 인증은 같은 유스케이스를 쓰되 `SyncWorker`가 `isTest` 기록을 건너뛰고, 영수증 해시 중복은 기기에서만 검사.
- `DailyLimitPolicy`는 테스트 가게를 검사하지도, 세지도 않는다(같은 날 반복 시험 가능). 도장 연출을 다시 보려면 진단 화면의 “테스트 기록 삭제”.

---

## 6. 데이터 모델

### 6.1 Room
```kotlin
@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: String,          // '|' 구분
    val programs: String,         // "BAEKNYEON|ORAE"
    val categoryGroup: String,
    val isFood: Boolean,
    val sido: String,
    val sigungu: String,
    val address: String,
    val phone: String?,
    val foundedYear: Int?,
    val foundedDate: String?,
    val lat: Double?,
    val lng: Double?,
    val menuItems: String,        // '|' 구분
    val hours: String?,
    val amenities: String,        // '|' 구분
    val summary: String?,
    val generation: Int?,
    val sourceUrl: String?,
    val isTest: Boolean = false
)

@Entity(tableName = "my_visits")
data class MyVisitEntity(
    @PrimaryKey val storeId: String,
    val storeName: String,
    val sigungu: String,
    val yearsAwarded: Int,
    val grade: String,            // NORMAL | RED | GOLD
    val method: String,           // RECEIPT | PHOTO | LOCATION_ONLY
    val photoVerified: Boolean,
    val firstVisitedAt: Long,     // epoch ms
    val lastVisitedAt: Long,
    val visitCount: Int,
    val photoPath: String?,
    val receiptHash: String?,
    val syncState: String,        // SYNCED | PENDING | FAILED
    val isTest: Boolean
)

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,             // VISIT_FIRST | VISIT_REPEAT
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int,
    val lastError: String?
)
```
- 데이터베이스 버전 1부터 시작. 스키마를 바꾸면 버전을 올리고 마이그레이션을 쓴다. **현재(v2)**: v1에서만 `fallbackToDestructiveMigrationFrom(true, 1)`로 다시 만들고(개발 중 DB), v2 이상은 `Migration`을 추가해야 한다. 스키마는 `app/schemas/`에 기록(`room.schemaLocation`). 실제 엔터티는 이 절의 예와 다르다(`MyVisitEntity`는 `synced: Boolean`, `StoreEntity`는 목록을 JSON 문자열로 저장 + `photoUrls`).
- 카탈로그는 Room에 미리 만든 DB 파일을 넣지 않고 JSON을 적재한다. 미리 만든 DB는 스키마가 조금만 달라도 앱이 시작되지 않는 위험이 있다.

### 6.2 Firestore
PRD 8.2 표 참고. 시간은 `Timestamp`, 화면 표시는 `Asia/Seoul` 날짜만. 방문 문서의 `firstVisitedAt`·`lastVisitedAt`과 `receiptHashes.createdAt`은 서울 기준 그날 0시로 잘라 저장한다(인증 시각의 시:분 비저장). 기기 `my_visits`에는 정확한 시각을 둬도 된다.

### 6.3 보안 규칙 (`firebase/firestore.rules`)
> **정본은 `firebase/firestore.rules` 파일이다.** 아래 예는 초기 설계이며 실제 규칙은 닉네임 변경 허용(+`nicknameLower` 일치 검사), `users.visitsVisibility`(방문 기록 공개 범위, 본인 외에는 ALL일 때만 `visits` 읽기), `storeStats/{storeId}`(방문자·재방문자 카운터, 한 번에 +1까지) 등이 추가돼 있다.
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }
    function isMe(uid) { return signedIn() && request.auth.uid == uid; }
    function userKeys() {
      return ['nickname','nicknameLower','titleId','totalYears','visitCount','reviewCount','createdAt','updatedAt'];
    }

    match /users/{uid} {
      allow read: if signedIn();
      allow create: if isMe(uid)
        && request.resource.data.keys().hasOnly(userKeys())
        && request.resource.data.nickname is string
        && request.resource.data.nickname.size() >= 2
        && request.resource.data.nickname.size() <= 12
        && request.resource.data.nicknameLower == request.resource.data.nickname.lower()
        && request.resource.data.totalYears == 0
        && request.resource.data.visitCount == 0
        && request.resource.data.reviewCount == 0
        && request.resource.data.createdAt == request.time;   // FieldValue.serverTimestamp()
      allow update: if isMe(uid)
        && request.resource.data.keys().hasOnly(userKeys())
        && request.resource.data.nickname == resource.data.nickname
        && request.resource.data.createdAt == resource.data.createdAt
        && request.resource.data.totalYears >= resource.data.totalYears
        && request.resource.data.totalYears <= resource.data.totalYears + 200
        && request.resource.data.visitCount >= resource.data.visitCount
        && request.resource.data.visitCount <= resource.data.visitCount + 1
        && request.resource.data.reviewCount >= 0;
      allow delete: if isMe(uid);

      match /visits/{storeId} {
        allow read: if signedIn();
        allow create: if isMe(uid)
          && request.resource.data.storeId == storeId
          && request.resource.data.yearsAwarded is int
          && request.resource.data.yearsAwarded >= 30
          && request.resource.data.yearsAwarded <= 200
          && request.resource.data.visitCount == 1
          && request.resource.data.method in ['RECEIPT', 'PHOTO', 'LOCATION_ONLY'];
        allow update: if isMe(uid)
          && request.resource.data.yearsAwarded == resource.data.yearsAwarded
          && request.resource.data.firstVisitedAt == resource.data.firstVisitedAt
          && request.resource.data.visitCount == resource.data.visitCount + 1;
        allow delete: if isMe(uid);
      }
    }

    match /nicknames/{name} {
      allow get: if true;    // 가입 전 중복 확인용 단건 조회만 (목록 조회 불가)
      allow create: if signedIn()
        && request.resource.data.uid == request.auth.uid
        && name == name.lower()
        && name.size() >= 2 && name.size() <= 12;
      allow delete: if signedIn() && resource.data.uid == request.auth.uid;
    }

    match /reviews/{reviewId} {
      function validReview() {
        let d = request.resource.data;
        return d.uid == request.auth.uid
          && reviewId == d.storeId + '_' + request.auth.uid
          && d.rating is int && d.rating >= 1 && d.rating <= 5
          && d.text is string && d.text.size() <= 500
          && exists(/databases/$(database)/documents/users/$(request.auth.uid)/visits/$(d.storeId));
      }
      allow read: if signedIn();
      allow create: if signedIn() && validReview()
        && request.resource.data.createdAt == request.time;
      allow update: if signedIn() && validReview()
        && resource.data.uid == request.auth.uid
        && request.resource.data.createdAt == resource.data.createdAt;
      allow delete: if signedIn() && resource.data.uid == request.auth.uid;
    }

    match /receiptHashes/{hash} {
      allow create: if signedIn() && request.resource.data.uid == request.auth.uid;
    }

    match /reports/{id} {
      allow create: if signedIn() && request.resource.data.reporterUid == request.auth.uid;
    }

    // 1일차 기능 검증(SPK-5)과 진단 화면의 쓰기·읽기 왕복 시험용
    match /diagnostics/{uid} {
      allow read, write: if isMe(uid);
    }
  }
}
```
> 이 규칙은 앱이 판정하는 구조의 방어선입니다. 조작된 앱이 세월 값을 부풀리는 것까지 완전히 막지는 못합니다(상한 200년/회로 제한). 실제 출시 전에는 인증 판정을 서버 함수로 옮깁니다.

### 6.4 색인 (`firebase/firestore.indexes.json`)
```json
{
  "indexes": [
    { "collectionGroup": "reviews", "queryScope": "COLLECTION",
      "fields": [ { "fieldPath": "storeId", "order": "ASCENDING" }, { "fieldPath": "updatedAt", "order": "DESCENDING" } ] },
    { "collectionGroup": "reviews", "queryScope": "COLLECTION",
      "fields": [ { "fieldPath": "uid", "order": "ASCENDING" }, { "fieldPath": "updatedAt", "order": "DESCENDING" } ] }
  ],
  "fieldOverrides": []
}
```

---

## 7. Gradle 구성

### 7.1 저장소 (`settings.gradle.kts`)
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://repository.map.naver.com/archive/maven")
    }
}
```

### 7.2 플러그인과 의존성 (`libs.versions.toml` 발췌)
```toml
[versions]
naverMap = "3.24.0"
mlkitKorean = "16.0.1"
# 나머지(agp, kotlin, ksp, composeBom, firebaseBom, room, camerax, playServicesLocation,
# navigation, lifecycle, workmanager, datastore, coil, serialization, coroutines)는
# Android Studio 최신 안정판 기준 값으로 채운다.

[libraries]
naver-map = { module = "com.naver.maps:map-sdk", version.ref = "naverMap" }
mlkit-text-korean = { module = "com.google.mlkit:text-recognition-korean", version.ref = "mlkitKorean" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { module = "com.google.firebase:firebase-auth" }
firebase-firestore = { module = "com.google.firebase:firebase-firestore" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }
```
플러그인: `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`, `com.google.devtools.ksp`(Room), `com.google.gms.google-services`, `com.google.firebase.crashlytics`.
- **AGP 9.0 이상은 Kotlin이 내장**이라 `org.jetbrains.kotlin.android`(kotlin-android)를 넣으면 “no longer required … since AGP 9.0” 오류로 동기화가 실패한다. 새 프로젝트 템플릿에 그 플러그인이 **있을 때만** 유지하고, 없으면 넣지 않는다. 이때 `android.kotlinOptions {}` 대신 `kotlin { compilerOptions {} }`를 쓰고, kapt는 쓰지 않는다(Room은 KSP).

### 7.3 앱 모듈 핵심 (`app/build.gradle.kts`)
```kotlin
val localProps = java.util.Properties().apply {
    val f = rootProject.file("local.properties"); if (f.exists()) f.inputStream().use { load(it) }
}
val keystoreProps = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties"); if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.nopo.cheonnyeon"
    defaultConfig {
        applicationId = "com.nopo.cheonnyeon"
        minSdk = 26
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["NAVER_MAP_KEY_ID"] = localProps.getProperty("NAVER_MAP_KEY_ID", "")
    }
    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        // debug 블록에 패키지 접미사를 두지 않는다 — NCP·Firebase에 등록한 패키지명과 같아야 지도·로그인이 동작
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // keystore.properties가 없으면 디버그 키로 서명 — 서명 없는 APK는 폰에 설치되지 않는다
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
            ndk { abiFilters += listOf("arm64-v8a") }   // (실제) arm64만 — D-15, 147MB → 약 48MB. 네이버 Client Secret은 릴리스에 넣지 않는다(B-14)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
}
```
- 디버그와 릴리스 패키지명이 같아야 네이버 지도 인증(패키지명 기준)과 Firebase가 둘 다 동작한다. `applicationIdSuffix`를 붙이지 않는다.
- `local.properties`, `keystore.properties`, `*.jks`, `google-services.json`은 `.gitignore`.

### 7.4 R8 (`proguard-rules.pro`)
```
# kotlinx.serialization 카탈로그 모델
-keepclassmembers class com.nopo.cheonnyeon.data.catalog.** { *; }
# Firestore toObject() 로 매핑하는 클래스를 쓰는 경우
-keepclassmembers class com.nopo.cheonnyeon.data.remote.dto.** { *; }
# 네이버 지도·ML Kit·Firebase는 라이브러리에 포함된 규칙을 사용. 릴리스에서 문제가 생기면 각 SDK 문서의 keep 규칙을 추가
```
QA는 반드시 릴리스 APK로 한다(디버그에서 되던 것이 R8 때문에 깨지는 경우를 잡기 위해).
> **(실제)** R8 최적화를 켜면 Crashlytics와 충돌해 릴리스가 시작 시 `IllegalAccessError`로 종료되어 `app/src/main/keepRules/rules.keep`에 `-dontoptimize`를 넣었다(축소·난독화는 유지). 지우지 말 것.

---

## 8. 매니페스트
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-feature android:name="android.hardware.camera.any" android:required="false"/>
<uses-feature android:name="android.hardware.location.gps" android:required="false"/>

<application ...>
    <meta-data android:name="com.naver.maps.map.NCP_KEY_ID" android:value="${NAVER_MAP_KEY_ID}"/>
</application>
```
- 정밀·대략 위치를 함께 선언하고, 사용자가 대략적 위치만 허용한 경우를 처리한다.
- 백그라운드 위치·저장소 권한은 쓰지 않는다(인증샷은 앱 내부 저장소).

---

## 9. 안정성 설계 규칙 (코드 리뷰 기준)

| 영역 | 규칙 |
|---|---|
| 상태 수집 | `collectAsStateWithLifecycle()`만 사용 |
| 제한 시간 | 위치 15초, OCR 10초, 서버 15초 `withTimeout`. 초과 시 정해진 오류 코드 |
| 예외 | 기기·서버 호출은 `runCatching` 후 `AppError`로 변환. UI까지 예외를 던지지 않음 |
| 권한 | `rememberLauncherForActivityResult`, “다시 묻지 않음”이면 설정 화면 이동 버튼. 화면 복귀(`ON_RESUME`) 때 권한 다시 확인 |
| 생명주기 | CameraX는 `bindToLifecycle`, 화면 이탈 시 `unbindAll`. MapView 생명주기 전달. ML Kit 인식기는 ViewModel `onCleared`에서 `close()` |
| 프로세스 종료 | 인증 단계·시도 횟수·리뷰 초안은 `SavedStateHandle` |
| 메인 스레드 | DB·파일·이미지 처리는 `Dispatchers.IO`/`Default`. 디버그에서 StrictMode 경고 0 |
| 메모리 | 사진은 긴 변 1,600px(OCR)/1,080px(저장)로 줄여 처리, 처리 후 해제. 디버그에서 LeakCanary 경고 0 |
| 시간 | `java.time` + `ZoneId.of("Asia/Seoul")`. 기기 시계 의존 판단(24시간)은 서버 시간과 10분 이상 차이 나면 경고 |
| 문자열 | 사용자 문구는 전부 `strings.xml` |
| 키 | 코드·저장소에 키 문자열 금지 |
| 로그 | 태그 `Nopo`, 개인정보(이메일, 좌표, 영수증 내용) 로그 금지. Crashlytics에는 오류 코드만 |

---

## 10. 테스트 전략

| 층 | 도구 | 대상 |
|---|---|---|
| 단위 | JUnit4, MockK, Turbine, coroutines-test | `domain/*`, `core/util`, ViewModel 상태 전이 |
| UI | Compose UI Test | 로그인 오류 표시, 리뷰 작성 검증, 도장 0개 화면, 오류 코드별 문구 |
| 계측 | AndroidX Test | Room DAO, CatalogSeeder |
| 서버 규칙 | Firebase 콘솔 규칙 플레이그라운드(필수), 에뮬레이터(P1) | 남의 문서 쓰기 거부, 미방문 리뷰 거부 |
| 실기기 | 수동 + `adb shell monkey` | 위치·카메라·OCR·지도·회전·백그라운드·오프라인 |
| OCR 샘플 | 영수증 20장, 간판 10장 + 테스트 이미지 | 인식률 |

목록과 케이스는 `nopo-04-dev-qa-checklist.md`.

---

## 11. 결정 기록 (ADR)

| ID | 결정 | 이유 | v0.3 대비 |
|---|---|---|---|
| ADR-01 | 단일 모듈 + 패키지 분리 | 빌드 설정 단순화 | 유지 |
| ADR-02 | 지도는 공식 `MapView`를 직접 감쌈 | 비공식 래퍼의 호환성 위험 제거 | 변경 |
| ADR-03 | OCR은 기기 안(앱 포함) | 개인정보, 오프라인 | 유지 |
| ADR-04 | 영수증 기본, 사진·위치 대체 | 사용자 요구 | 변경 |
| ADR-05 | 서버 판정 없음, 규칙으로 방어 | 무료 요금제로 시작 | 유지 |
| ADR-06 | 수동 주입(Hilt 제거) | 빌드 오류 위험 감소 | 변경 |
| ADR-07 | 이메일 로그인 + 닉네임 문서로 중복 방지 | 계정 필수 요구 | 변경 |
| ADR-08 | 카탈로그는 JSON → Room 적재 | 미리 만든 DB의 스키마 불일치 위험 회피 | 변경 |
| ADR-09 | 기기 먼저 기록, WorkManager로 서버 전송 | 오프라인·불안정 연결에서 데이터 손실 방지 | 변경 |
| ADR-10 | 리뷰 문서 ID = `storeId_uid` | 1인 1리뷰를 구조로 보장 | 신규 |
| ADR-11 | 평균은 집계 쿼리 | 별도 합계 문서·서버 함수 불필요 | 신규 |
| ADR-12 | QA는 릴리스 APK로 | R8로 인한 릴리스 전용 버그 조기 발견 | 신규 |

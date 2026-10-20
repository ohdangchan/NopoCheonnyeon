# 개발 현황·인수인계 (이어서 개발하는 에이전트용)

> 최종 갱신: 2026-10-20 (전체 문서·코드 재독 후 작성). **이 문서를 먼저 읽고**, 그다음 `CLAUDE.md` → `docs/nopo-01-PRD.md` 순서로 읽는다.
> 목표: **오늘 오후 해커톤에서 공기계에 설치해 심사위원에게 시연**. 기획된 기능 전부 구현 + QA 체크리스트 통과 문서(`docs/qa-report.md`).
> 이 문서 6장 "코드 점검에서 찾은 차이·미구현"은 이번 재독에서 새로 확인한 것이다. 문서와 코드가 다르면 **코드가 현재 사실**이고, 기준 우선순위는 `CLAUDE.md`의 PRD → 아키텍처 → 스토리 순이다.

## 0. 사용자 지시 모음 (시간순, 그대로 따를 것)
**2026-10-20 이전 세션에서 받은 지시 (이 문서 이전 판에서 옮김)**
- 모든 것은 사전 승인됨. 사용자에게 묻지 말고 기획안(PRD)에 적힌 모든 것을 개발하고 QA까지 진행한다.
- 사용자가 직접 해야 하는 시험(실제 영수증 촬영, 실제 가게 앞 30m 인증)은 "통과했다고 가정"하고 개발을 계속하며, **마지막 QA 문서에 수동 시험 항목으로 넣는다.**
- 사진 없는 가게는 글자 카드로 대신한다. 이후 개선안: 웹에서 사진을 내려받아 앱에 포함 또는 구글 Places API (1,277곳×3장 ≈ $20, 약관 확인 필요).
- 글자 크기는 알아서 (본문 ≥14sp, 짧은 라벨 ≥12sp).
- 한도 도달 시: 세션 95% 또는 주간 99%가 되면 그때까지의 모든 것을 커밋·푸시하고 이 문서를 최신으로 고쳐 다른 에이전트가 바로 이어받게 한다. 세션 한도가 풀리면 주간 99%까지 계속 진행.
- 모든 채팅·진행 안내는 **한국어** (자동 메모리 `korean-only-replies`에도 저장됨).

**이번 세션(2026-10-20, 모델을 Sonnet 5.5로 바꾼 직후)에서 받은 지시**
1. "이전 대화 기억해?" → 대화 자체는 기억하지 못하고 저장소·메모리·커밋으로 아는 것만 안다고 답함.
2. "이 폴더의 모든 문서와 코드를 하나도 빠짐없이 읽고 분석해서, 지금까지의 작업 기록과 내가 내린 지시까지 정리해 다른 에이전트가 이어서 진행할 수 있도록 철저히 준비해줘." → 이 문서가 그 결과.
3. "지금까지 개발한 모든 문서들 https://github.com/ohdangchan 여기에 푸시 커밋 싱크 해줘." → 원격 `origin = https://github.com/ohdangchan/NopoCheonnyeon.git` (이미 연결돼 있음). 비밀 파일은 `.gitignore`로 제외된 상태이며 커밋에 포함하지 않았다.
4. (2차 지시) "다른 에이전트가 할 거니까 너는 인수인계 준비를 더 철저히. **Stitch 화면 파일들까지 생성된 모든 파일을 반드시 다 읽고** 완벽히 준비. `docs/spike-report.md` 등 필요한 문서를 모두 생성. QA 목록을 다시 확인해 **실기기 테스트가 필요한 모든 부분을 전부 정리해 다시 만들 것**." → 아래 "읽은 범위"와 신규 문서(`spike-report`, `nopo-09`~`nopo-11`, `apk-install-guide`, `privacy-notice`, `docs/README`)가 그 결과. 이 세션에서는 **기능 코드를 수정하지 않았다**(다른 에이전트가 이어서 구현).

### 이번 인수인계에서 읽은 범위 (2026-10-20)
- 전부 읽음: `CLAUDE.md`, `START-HERE.md`, `docs/*.md` 전부, `design/stitch/PRD-차이점.md`·`README.md`, `data/README.md`, `.claude/*`, `firebase/firestore.rules`, Gradle 파일·`libs.versions.toml`·매니페스트·`rules.keep`, `strings.xml` 342개, 앱 코드 전체(도메인·유틸·데이터·플랫폼·기능 화면·디자인 시스템·진단 탭), 테스트 함수 목록.
- 텍스트 추출 후 전부 읽음: Stitch 화면 51개 `code.html`(보이는 문구), `docs/nopo-planning.html`(하반기 장 위주 정독, 상단 장은 PRD와 같은 내용). 화면 이미지는 `receipt_verify`·`map` 두 장만 직접 확인.
- **훑기만 함(내용 신뢰 안 함)**: `design/stitch/reference/` 43개 폴더(검증되지 않은 "완료" 주장 문서, `DESIGN.md`만 코드와 대조해 유효 확인), `data/raw/*.jsonl`·CSV·SQLite·`stats.json`(수치만), `tools/*.py` 본문(머리 설명만), PNG 이미지 대부분(`screen.png` 51장 중 2장), `design/stitch/screens/*/meta.json`.


## 1. 프로젝트 요약
- 앱: 노포 천년 미식가 (`com.nopo.cheonnyeon`). 전국 백년가게·서울 오래가게 1,277곳을 지도에서 찾아 **영수증(위치 미사용) 또는 가게 30m 이내 사진**으로 방문 인증 → 도장 → "세월(업력 합계)" 적립 → 칭호. 평점·리뷰·유저 프로필.
- 스택: Kotlin 2.2.10, Jetpack Compose(BOM 2026.02.01), AGP 9.4.1, Gradle 9.6.0, minSdk 26 / target·compile 37, 단일 모듈 `:app`. MVVM + 단방향 흐름, 수동 DI(`app/AppContainer.kt`), Room 2.8.5(DB v2), DataStore, WorkManager, Coil 3, 네이버 지도 SDK 3.24.0, CameraX 1.6.2, ML Kit 한국어 OCR 16.0.1(앱 포함), Fused Location 21.4.0, Firebase BoM 34.19.0(Auth·Firestore·Crashlytics). **버전을 임의로 올리지 않는다** (`gradle/libs.versions.toml`).
- 서버: Firebase 프로젝트 `nopocheonnyeon` (계정 songpa24@iceu.kr), Firestore `(default)` **asia-northeast3**, 규칙·색인은 `firebase/`.
- 카탈로그: `app/src/main/assets/stores.json` v26101921 — 1,277곳, 좌표 1,181곳(92.5%), 공식 사진 86곳, 음식점 834곳. 생성: `python tools/build_catalog.py`.
- 저장소 규모: 추적 파일 약 455개. 앱 코드 `app/src/main/java/com/nopo/cheonnyeon` 약 8,300줄(테스트 포함), 단위 테스트 19개 파일/145케이스, 문자열 리소스 342개.

## 2. 문서 지도 (무엇을 어디서 읽나)
| 파일 | 내용 | 상태 |
|---|---|---|
| `CLAUDE.md` | 작업 규칙(필독): 스토리 단위, 테스트 먼저, 금지 사항, 명령 | 최신 (v0.4) |
| `START-HERE.md` | 패키지 안내·키 준비·데이터 현황 | 2026-10-18 기준, 좌표 "0"은 낡음(지금 1,181) |
| `docs/nopo-01-PRD.md` | 요구사항 80개, 규칙값 `VerificationConfig`, 오류 코드(7.4), 결정 D-01~D-14 | 기준 문서 |
| `docs/nopo-02-architecture.md` | 구조·보안 규칙·Gradle·ADR | 일부 낡음 (6장 참고) |
| `docs/nopo-03-stories.md` | 스토리 76개·일정·컷라인 | 체크 표시 안 됨 (진행도는 이 문서 4장) |
| `docs/nopo-04-dev-qa-checklist.md` | U/I/M/S/R 항목, 디버깅 포인트 | 체크 표시 안 됨 (결과는 `qa-report.md`) |
| `docs/nopo-05-precheck-checklist.md` | 사용자가 직접 할 일 40개 | 참고 |
| `docs/nopo-06-spike-plan.md` | 1일차 기능 검증 계획 | 결과는 `spike-report.md` |
| `docs/nopo-07-firebase-setup.md` | 서버 설정. 0장에 실제 연결 정보·nam5 함정 | 최신 |
| `docs/nopo-08-claude-code-guide.md` | Claude Code 작업 루프 | 참고 |
| `docs/qa-report.md` | 2026-10-20 QA 결과(PASS/수동/보류) | 최신 |
| `docs/demo-guide.md` | 해커톤 시연 절차·문제 대처·발표 수치 | 최신 |
| `docs/nopo-planning.html` | 기획 전체 24장 한 장 문서 | PRD와 다르면 PRD 우선 |
| `docs/archive/` | 구버전 보관, **참조 금지** | — |
| `docs/README.md` | docs 색인(신규) | 최신 |
| `docs/spike-report.md` | 1일차 기능 검증 결과(소급, 수치 재측정 필요) | 신규 |
| `docs/nopo-09-real-device-qa.md` | **실기기 QA 전체 목록과 상태(기준)** | 신규 |
| `docs/nopo-10-stitch-implementation-map.md` | Stitch 51화면 ↔ 구현 현황 | 신규 |
| `docs/nopo-11-backlog.md` | 결함·누락·데이터·문서 정리 백로그 | 신규 |
| `docs/apk-install-guide.md` | APK 설치 안내(S-1006) | 신규 |
| `docs/privacy-notice.md` | 개인정보 안내 초안(운영자 정보 비움) | 신규 |
| `design/stitch/README.md`, `PRD-차이점.md` | Stitch 51화면 매핑과 PRD와 다른 점. **화면을 만들기 전에 반드시 확인** | 최신 |
| `design/stitch/reference/` | 검증 안 된 "완료" 주장 문서 모음. **스크립트 실행 금지**(규칙·strings.xml을 덮어씀) | 위험 |
| `data/README.md` | 데이터 열 설명·수집 방법·한계 | 수치는 수집 시점(좌표 0)이라 낡음 |
| `.claude/settings.json`, `.claude/agents/nopo-qa.md` | 명령 허용/키 파일 읽기 차단, QA 서브에이전트 | 최신 |

## 3. 비밀 파일 (저장소에 없음, 이 PC에만 있음)
| 파일 | 용도 | 없으면 |
|---|---|---|
| `local.properties` | `NAVER_MAP_KEY_ID`, `NAVER_MAP_CLIENT_SECRET` (`local.properties.sample` 참고) | 지도 인증 실패 |
| `app/google-services.json` | Firebase 설정 (`firebase apps:sdkconfig ANDROID 1:401283218421:android:1cf2a80d59a68bf853bebc -o app/google-services.json`) | 빌드 실패 |
| `keystore.properties` + `C:/Users/HB_OPERATION_NICK/AndroidStudioProjects/jks/nopo_release_key` | 릴리스 서명 (샘플: `app/keystore.properties.sample`) | 릴리스가 디버그 키로 서명됨 |
- `.stitch-key`, `STITCH_API_KEY`, `NCP_KEY*`, `KAKAO_REST_KEY`는 환경 변수/무시 파일로만. 채팅에 노출된 Stitch 키·네이버 IAM 키는 사용자가 폐기해야 한다. `.claude/settings.json`이 위 비밀 파일 읽기를 막는다.

## 4. 진행 상태

### 4-1. 스토리 (커밋 메시지 `S-xxx: 요약`, 26개 커밋, 전부 2026-10-19~20)
완료: E0 진단(SPK-0~6, S-001~006) · E1 기반(S-101~106) · E2 도메인 규칙 전부(S-201~212) · E3(S-301~307) · E4(S-401~408 모두, P1 포함) · E5(S-501~506) · E6(S-601~609) · E7(S-701~704) · E8(S-801~805, P1 포함) · E9(S-901~903) · S-1002(릴리스 서명·R8).
미완/부분: S-008 보고서(`docs/spike-report.md` 소급 작성됨, **측정 수치는 미기록 → 재측정 필요**), S-308(규칙 플레이그라운드 I-1~I-10 점검 기록 없음 — 배포는 됨), **S-705 칭호 승급 연출(P1, 텍스트 한 줄만)**, S-1001(공식 지정리스트 CSV 미반영, 좌표 없는 96곳 미보강), S-1003~S-1006(실기기 QA 일부만, Monkey 미완주, 최종 배포 안내 미작성).

### 4-2. 커밋 이력 (최신순 요약)
`d37ce99` Crashlytics(릴리스만)·R8 `-dontoptimize`·릴리스 실기기 확인 → `f7e651d` 오프라인 배너·확인 중 도장·업력 확인 중·인증샷 저장·설정 계정 정보 → `9c0b497` 닉네임 변경/탈퇴·리뷰 신고·시도별 클러스터 원·디버그 가상 방문 → `0b5e39d` 유저 프로필·내 리뷰 탭 → `219802e` dev-status 첫 판 → `40fe500` 서명 값 trim → `8a4b0cc` 온보딩·설정 → `49d289b` 리뷰·서버 복원·로그아웃 정리 → `69f1098` 패스포트·도장 연출 → `0d2df2b` 방문 기록·동기화·인증 화면·테스트 가게 → `2b02768` 지도·상세 → `dea13fb` 가입/로그인 → `49988ec` 사진 서버 Accept 헤더 → `95874b8` 공식 사진 → `2579e3c` 좌표 1,181곳 → `15c44c6` Room·카탈로그 → `e20d2e8` 기반·도메인 규칙 → 진단 탭들(`893454c`,`bd7a3c7`,`778710b`) → `22af130` 진단 뼈대 → `57e9c5b` 초기 설정.

### 4-3. 이번 세션의 미커밋 작업 (이 인수인계 커밋에 포함)
- `DiagnosticsSampleOcr.kt`(신규) + `DiagnosticsOcrTab.kt` 한 줄 연결 + `app/src/debug/assets/test_receipt.png`·`test_sign.png`(신규): **디버그 전용** — 카메라 없이 샘플 영수증·간판 이미지를 실제 ML Kit로 읽어 파서·판정을 돌려 보는 버튼 2개. 문구가 하드코딩(한글)이지만 디버그 전용이라 허용된 예외로 둔다. 릴리스에는 자산이 없고 `BuildConfig.DEBUG` 가드가 있다.
- `.idea/gradle.xml`(Android Studio 자동 변경)·`.idea/vcs.xml`(신규): IDE 설정, 기능 영향 없음.

### 4-4. PRD 기능 구현 현황 (요구사항 ID 기준)
| 영역 | 상태 | 비고 |
|---|---|---|
| SPK/TST 진단·테스트 가게 | 완료 | 설정 > 앱 정보 5탭. 진단 탭: 지도·카메라·OCR·위치·Firebase·사진·디자인 7개 |
| AUTH-01~10 | 완료 | 가입(닉네임 중복 실시간)·로그인·재설정·로그아웃 경고·게이트·복구·닉네임 변경·탈퇴. 탈퇴·신고의 실기기 시험은 수동 |
| MAP-01~07 | 완료 | 줌<9에서 시·도별 개수 원, 이상은 화면 영역 마커 최대 250개, 검색어 있으면 목록 고정 |
| STORE-01~05 | 완료 | 사진 페이저(없으면 글자 카드), 보상 박스, 네이버 지도 길찾기(앱 → 웹 대체), 전화 걸기(P1)도 구현 |
| STORE-06 정보 오류 신고 | **미구현 (P1)** | 리뷰 신고(FR-REV-08)만 있음 |
| VER-01~16 | 대부분 완료 | **VER-03 영수증 가이드 틀 오버레이 미구현**. 갤러리 선택 없음(준수) |
| STP-01~05, 07 | 완료 | STP-06 승급 연출은 P1 미완 |
| PAS-01~04 | 완료 | PAS-05 공유 카드는 P2 |
| REV-01~08 | 완료 | 편집기는 별도 화면이 아니라 `AlertDialog` (6장) |
| PRF-01~06 | 완료 | PRF-07 비공개 설정 P1 미구현 |
| DATA-01~04 | 완료 | 좌표 없는 96곳은 영수증 인증만 가능(`E_NO_COORD`) |
| SYS-01~04 | 완료 | 설정 > 라이선스는 접이식 텍스트, 개인정보 안내 텍스트 |

## 5. 코드 지도 (`app/src/main/java/com/nopo/cheonnyeon`)
- `NopoApp.kt` — StrictMode(디버그), Crashlytics 수집은 `!DEBUG`에서만, Coil `ImageLoader`에 `Accept: image/*` 헤더(사진 서버 sbiz24 500 회피), 시작 시 `container.syncPending()`.
- `app/AppContainer.kt` — 수동 DI. 카탈로그 적재(백그라운드) → `catalog.invalidate()`. 로그인되면 `visits.restoreFromServer()` + 동기화. `signOut()`이 방문·대기열·`filesDir/proofs/` 삭제. `sessionState`는 `Eagerly`로 `Loading` 시작.
- `core/navigation/NopoNavHost.kt`, `Routes.kt` — 세션 상태로 그래프 통째 전환(`popUpTo(0)`). 경로: splash·onboarding·login·sign_up·reset·nickname·home·`store/{id}`·`verify/{id}`·`profile/{uid}`·diagnostics. 하단 탭(지도·패스포트·설정)은 `feature/main/MainScreen.kt`가 상태로 처리(별도 nav 경로 아님). 로그인 화면에는 디버그 빌드에서만 진단 진입 버튼.
- `core/common` — `AppError`(PRD 7.4 코드 25개, 문구는 `strings.xml`의 `err_<소문자코드>`), `AppResult`, `Clock`/`SeoulZone`, `DispatcherProvider`, `factoryOf`.
- `core/util` — `GeoDistance`(하버사인), `TextNormalizer`, `Similarity`(`names`=전체 편집거리, `partial`=간판 속 부분 일치, Damerau-Levenshtein), `BusinessNumber`(체크섬), `PhoneNumber`.
- `domain/rule` — `VerificationConfig`(**기준값 단일 위치**, 아래 표), `YearsCalculator`(바닥 30, 미래 창업연도는 `require` 예외), `StampGradePolicy`, `TitlePolicy`, `DailyLimitPolicy`(서울 자정·테스트 가게 제외), `ReviewRules`, `AuthValidation`.
- `domain/verify` — `ReceiptParser`(전화·사업자번호·결제일시·금액·상호), `ReceiptMatcher`(가게 확인 → 날짜), `ReceiptHasher`(SHA-256, 분 단위), `PhotoMatcher`, `VerifyReceiptUseCase`(object, 순수 함수).
- `domain/catalog/StoreFilter.kt` — 필터·검색·정렬·영역. `domain/model` — Store, Title, StampGrade, VerifyMethod, SessionState, VisitRef.
- `data/local` — Room(`stores`, `my_visits`, `pending_ops`; **DB v2, `fallbackToDestructiveMigration`**), `Daos.kt`, `SettingsStore`(DataStore: 온보딩·카탈로그 버전·음식점 필터). `data/catalog` — `CatalogSeeder`(버전이 같으면 건너뜀, 한 트랜잭션 교체), `CatalogRepository`(메모리 캐시 + `changes` 카운터 + 테스트 가게 생성 `TEST-NOPO`).
- `data/visit` — `VisitRepository`(기기 먼저 기록 → `pending_ops` → Firestore 배치, 서울 0시로 자른 시각 저장, 복원, 중복 영수증 되돌림), `SyncWorker`/`SyncScheduler`(`enqueueUniqueWork("sync", APPEND_OR_REPLACE)`, 네트워크 제약, 지수 백오프 30초).
- `data/remote` — `AuthRepository`(세션 Flow, 가입·복구·변경·탈퇴, 15초 타임아웃), `ReviewRepository`(집계 쿼리 평균·개수, 20개 페이징, 저장/삭제/신고), `ProfileRepository`.
- `platform` — `camera/CameraCapture`(메모리 촬영 `captureInMemory`는 파일 안 만듦, `captureToFile`), `location/FusedLocationAdapter`(`bestFix` 15초·정확도 ≤10m 조기 종료), `map/NaverMapView`(생명주기 8개 전달), `map/ReverseGeocoder`(진단용), `network/ConnectivityMonitor`, `ocr/MlKitKoreanRecognizer`, `permission/PermissionState`(ON_RESUME 재확인).
- `feature` — `auth`(온보딩·로그인·가입·재설정·닉네임), `main`(탭·오프라인 배너·로그아웃 경고·**테스트 가게 버튼**·**디버그 가상 방문 버튼**), `map`(MapScreen/ViewModel/StoreSheet/목록), `store`(상세·리뷰 섹션·사진), `verify`(VerifyViewModel/Screen: RECEIPT→LOCATE→PHOTO→DONE, 도장 연출 포함), `passport`(도장 격자·상세 다이얼로그·내 리뷰), `profile`, `settings`, `diagnostics`(7탭).
- `core/designsystem` — 토큰(Color/Type/Dimens/NopoTheme), 컴포넌트(StampView·YearsGauge·TitleBadge·StarRating·NopoButton·EmptyState·OfflineBanner), `DesignCatalogScreen`(진단 "디자인" 탭).
- 테스트: `app/src/test` 19개 파일 — 순수 도메인·유틸·검증만. ViewModel·Repository·Room·Compose UI 테스트는 **없음**(`androidTest`는 템플릿 예제뿐).

`VerificationConfig` 값: 영수증 24시간·미래 허용 10분·상호 유사도 ≥0.70(전화 일치 OR), 사진 거리 ≤30m·정확도 ≤30m·글자 유사도 ≥0.60·글자 3회 실패 후 위치 확인 도장 허용, 위치 타임아웃 15초(조기 종료 10m, 간격 1초), 하루 5곳.

## 6. 코드 점검에서 찾은 차이·미구현 (이번 재독 결과, 우선순위 순)
> 전체 목록·완료 조건은 **`docs/nopo-11-backlog.md`**(B-01~B-24, D-01~, C-01~). 아래는 핵심 요약이며 B-02 이후 항목(중복 알림 미수집, 위치 켜기 대화상자, 진단 정보 복사·테스트 기록 삭제 누락, 프로필 단순화)은 backlog에만 자세히 있다.
0. **(최우선) 서버가 중복 영수증을 거부해도 사용자에게 알리지 않는다** — `VisitRepository.duplicates` 흐름을 수집하는 코드가 없다(B-02). "확인 중" 도장이 조용히 사라진다.
1. **중복 영수증 판정 로직이 아키텍처 5.3·U-306-1과 다름** — `VisitRepository.sync()`는 `PERMISSION_DENIED` + `receiptHash != null`이면 곧바로 "중복 영수증"으로 보고 기기 기록을 지운다. 문서는 되돌리기 전에 `users/{uid}/visits/{storeId}`를 서버에서 한 번 읽어, 이미 있으면 성공으로 처리하라고 한다(응답만 못 받은 재전송, 또는 다른 이유의 거부를 오판하지 않기 위해). **미구현.** 시연 영향은 낮지만 데이터 손실 위험이 있어 먼저 고칠 가치가 있다. 재방문(`firstVisit=false`)에 영수증이 거부되면 `visitDao.delete`는 안 하고 알림만 보낸다(의도된 동작으로 보임).
2. **자동 테스트 공백** — nopo-04의 U-VM-*, U-301/302/306(Room·Repository·동기화) 항목에 해당하는 테스트가 없다. `testDebugUnitTest` 145개는 전부 도메인·유틸이다.
3. **`MapViewModel.totalYears`가 테스트 방문까지 합산** (`visits.sumOf { it.yearsAwarded }`). 패스포트·상세는 `TEST-` 제외인데 지도 상단 "내 세월" 알약만 테스트 가게 방문을 포함해 값이 어긋난다. `PassportViewModel`처럼 `storeId.startsWith(Store.TEST_PREFIX)`를 거를 것.
4. **리뷰 저장·삭제가 트랜잭션이 아니라 배치** (`ReviewRepository.save/delete`). 문서(아키텍처 5.4)는 트랜잭션. 배치는 오프라인에서 큐에 쌓였다가 나중에 거부될 수 있으나, 현재는 `withTimeout 12s`와 `UNAVAILABLE → E_REVIEW_OFFLINE` 매핑으로 대응한다. 타임아웃 시에도 `E_REVIEW_OFFLINE`으로 변환된다(`CancellationException` 분기). 오프라인 실기기 시험(M-49)은 아직 안 했다.
5. **리뷰 편집기·도장 연출이 문서 구조와 다름** — `ReviewEditorScreen`/`StampCeremonyScreen`/`StampDetailSheet`는 별도 화면이 아니라 각각 `StoreReviewsSection`의 `AlertDialog`, `VerifyScreen.ResultSection`, `PassportScreen.StampDetail`(AlertDialog). 기능은 있으나 Stitch 시안보다 단순하다.
6. **미구현 UI**: 영수증 촬영 가이드 틀(FR-VER-03), 칭호 승급 연출(S-705), 정보 오류 신고(FR-STORE-06), 방문 기록 비공개(FR-PRF-07), 개인정보 처리방침 별도 화면(현재 설정 화면 텍스트), 라이선스 목록 자동 생성(현재 고정 문자열 + `assets/licenses/*.txt` 2개), `Haptics` 클래스(진동은 `VerifyScreen`에 직접 구현), `Lottie`는 쓰지 않음.
7. **문서가 낡은 부분(고칠 것)**: ① 아키텍처 6.1은 `fallbackToDestructiveMigration`을 쓰지 말라고 하나 실제로는 사용 중(출시 전 정식 마이그레이션 필요, DB v2). ② 아키텍처 6.3 규칙 예시는 users 업데이트에서 `nickname == resource.data.nickname`을 요구하지만 **실제 `firebase/firestore.rules`는 닉네임 변경을 허용**하도록 바뀌어 있다(규칙 파일이 정본). ③ 아키텍처 7.3의 `abiFilters`는 `arm64-v8a, armeabi-v7a`이나 실제는 `arm64-v8a`만(D-15). ④ 아키텍처 R8 설명은 최적화 켬이나 실제는 `-dontoptimize`(Crashlytics 충돌). ⑤ 스토리/체크리스트 체크박스 미표시. ⑥ `START-HERE`/`data/README` 좌표 수치. ⑦ PRD에 D-15(arm64만)·`-dontoptimize` 결정 미반영. ⑧ (해결) `docs/spike-report.md` 작성됨.
8. **세션 게이트의 오프라인 동작**: `AuthRepository.hasProfile()`은 확인 실패(오프라인·타임아웃)면 "프로필 있음"으로 간주해 `SignedIn`으로 들어간다(오프라인에서 앱이 막히지 않게 한 의도). 프로필 문서가 실제로 없는 계정이 오프라인 상태로 진입할 수 있음.
9. **하드코딩 문자열 예외**: `MainScreen`의 `DebugVisitButton`("가상 방문 기록…", "기록할 가게가 없어요"), `DiagnosticsSampleOcr`는 한글 리터럴(디버그 전용). `CatalogRepository.createTestStore`의 테스트 가게 이름·소개도 리터럴(진단용). 사용자 화면 문구 규칙 위반은 이 디버그/진단 범위 밖에서는 찾지 못했다(`nopo-qa`로 재검증 권장).
10. **`CLAUDE.md` 규칙 점검 결과**: `applicationIdSuffix` 없음 ✔, 갤러리 선택 없음 ✔, 영수증 이미지는 `captureInMemory`로 파일 없이 처리 ✔(인증샷만 `filesDir/proofs/`에 JPEG 85, 기기 안에만), 위치는 사진 인증에서만 사용 ✔, 좌표·영수증 내용은 서버/로그에 없음 ✔(`ReverseGeocoder`는 좌표를 저장·로그하지 않음), 키는 `local.properties`→`BuildConfig`/매니페스트 플레이스홀더 ✔(다만 `NAVER_MAP_CLIENT_SECRET`이 `BuildConfig`로 APK에 들어감 — 알려진 위험).

## 7. 알려진 문제·결정 (유지할 것)
- **R8**: 최적화를 켜면 Crashlytics와 충돌해 릴리스가 시작 시 `IllegalAccessError`로 종료 → `app/src/main/keepRules/rules.keep`의 `-dontoptimize`를 지우지 말 것. 릴리스는 반드시 폰에 설치해 확인. (`app/build.gradle.kts`의 `optimization { enable = true; packageScope = … }`는 AGP 9 방식.)
- **Monkey**: 개인 폰에서 돌리면 앱 밖(런처)으로 나가 다른 앱이 열린다. 쓰지 말 것. 전용 기기/에뮬레이터 필요(이 PC 메모리 부족으로 에뮬레이터 불가). 1차 4,329회·2차 3,647회에서 중단, 앱 크래시 없음.
- 디버그와 릴리스는 서명이 달라 폰에서 서로 덮어쓸 수 없다(`adb uninstall com.nopo.cheonnyeon` 후 설치).
- 사진 서버(sbiz24.kr)는 `Accept` 헤더 없는 요청에 500 → `NopoApp.newImageLoader`가 헤더를 붙임.
- 리뷰 쓰기는 방문 문서가 서버에 있어야 허용(규칙) → 테스트 가게는 서버로 안 보내므로 리뷰 불가(의도). 평점 버튼은 `my_visits.synced`일 때만 활성(`E_VISIT_SYNCING`).
- 가상 방문(디버그 버튼)은 서버에 실제 방문 문서를 만든다(시험 계정으로만).
- D-15: 릴리스 APK는 arm64-v8a만 (147MB → 약 48MB).
- Firestore 데이터베이스를 만들기 전에 `firebase deploy --only firestore`를 하지 말 것(기본 nam5 미국으로 생성됨, 바꿀 수 없음). 배포: `cd firebase && firebase deploy --only firestore:indexes,firestore:rules --project nopocheonnyeon`.
- Git-Bash에서 한글 curl은 인코딩이 깨져 네이버 지오코딩이 0건. 파일 여러 개를 heredoc으로 한 번에 만들면 파싱 오류 → Write 도구나 파이썬 파일 사용.
- `design/stitch/reference/` 안의 "즉시 자동 복사" 스크립트 문서는 실행 금지.

## 8. 명령 (Windows)
- 단위 테스트 `.\gradlew.bat testDebugUnitTest` · 린트 `lintDebug` · 디버그 설치 `installDebug` (폰 R5KL806MA4W, Galaxy Z Fold SM-F971N, Android 17 API 37)
- 릴리스 `.\gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk`; 설치 `adb install -r …` (adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`)
- 로그 `adb logcat -d -v time Nopo:D AndroidRuntime:E *:S`
- 카탈로그 재생성 `python tools/build_catalog.py`; 좌표 `python tools/geocode.py --provider naver|kakao`; 사진 주소 `python tools/fetch_photos.py`; 공식 지정리스트 `python tools/import_official_list.py --csv 파일.csv`; Stitch 재수집 `$env:STITCH_API_KEY="…"; python tools/fetch_stitch.py`
- 시험 재료: `tools/test-images/test_receipt_live.html`(현재 시각 영수증), `test_receipt.png`(날짜 고정 → 24시간 뒤 만료 확인용), `test_sign.png`

## 9. 남은 일 (우선순위 순)
1. **사용자 수동 QA** (**`docs/nopo-09-real-device-qa.md`**, 이전 목록은 `qa-report.md` 5장): 영수증 실촬영, 실제 가게 앞 30m 인증, 오프라인 전송("확인 중" 도장 → 복귀 후 확정), 릴리스 APK에서 영수증·사진·리뷰 재확인, 구형 기기·큰 글씨·다크 모드, 회원 탈퇴·리뷰 신고(두 번째 계정), 길찾기 앱 실행, 서명 키 백업 2곳.
2. backlog B-01·B-02(중복 영수증 서버 확인·알림)·B-03(지도 세월 합계) 수정 + 해당 테스트 추가 → 6장 2번(Repository/ViewModel 테스트).
3. 영수증 가이드 틀(FR-VER-03), 칭호 승급 연출(S-705), 정보 오류 신고(FR-STORE-06), 필요 시 리뷰 편집기·도장 연출을 시안에 맞춰 보강(`design/stitch/PRD-차이점.md` 먼저).
4. 사진 확대: 공식 사진 86곳(7%) 외에는 글자 카드. 웹 사진 내려받기 또는 구글 Places API(≈$20, 약관 확인).
5. 좌표 없는 96곳 보강(카카오 키 필요) → `geocode.py --provider kakao` → `build_catalog.py` → 앱 재적재(카탈로그 version 변경됨).
6. 출시 전: Room 정식 마이그레이션, 네이버 Client Secret 한도 설정/교체, 공식 지정리스트 반영(S-1001), 문서 정리(스토리/체크리스트 체크, 7장의 낡은 문서 수정, PRD 결정 반영), `nopo-qa` 에이전트로 최종 검증.
7. 작업 방식 재확인: 한 번에 스토리 하나, 계획 5줄, 도메인은 테스트 먼저, `testDebugUnitTest` → `assembleDebug` → `installDebug`, 커밋 `S-xxx: 요약`, 새 라이브러리·버전 변경은 먼저 사용자에게 물을 것(단, 0장의 "사전 승인" 범위 안의 개발은 묻지 않고 진행).

## 10. 진행 로그 (2026-10-20, 사용자 "묻지 말고 모든 개발·테스트 계속" 지시 이후)
사용자 결정: P1·시안 수준 보강을 **전부 진행**, 사진 출처는 **사용자가 구글 API 키를 줄 때까지 보류**(글자 카드 유지), **재방문율 기능 추가**(FR-STORE-07). 이 기간 커밋: `6f050bc` `c82bb47` `180c657` 이후 `B-09·B-10·B-11·B-25`, `B-13·B-14`, 본 로그 시점 마지막 커밋은 `git log` 참고.
| 항목 | 상태 |
|---|---|
| B-01·B-02 중복 영수증(서버 방문 확인 후 판정, 알림 다이얼로그) | 구현, 실기기 미확인 |
| B-03 지도 세월에서 테스트 방문 제외 | 구현 |
| B-05 인증 화면 위치 켜기 대화상자 | 구현, 실기기 미확인 |
| B-06 진단 정보(Play 서비스·권한·마지막 측정) + 복사 | 구현 |
| B-07 테스트 가게·기록 삭제(개발자 도구) | 구현 |
| B-08 프로필(가입일·게이지·탭·등급 도장·첫 방문일·단골·20개씩) | 구현 |
| B-09 영수증 가이드 틀 / B-10 가게 정보 오류 제보 / B-11 칭호 승급 연출 / B-25 연타 방지 | 구현 |
| B-12 방문 기록 공개 범위(설정) + 규칙 | 구현, **규칙 배포 대기** |
| F-01 재방문율(`storeStats` 카운터, 상세 평점 영역에 표시) | 구현, **규칙 배포 대기** |
| B-13 Room 마이그레이션 체계 / B-14 릴리스에서 Client Secret 제거 | 구현(릴리스 APK dex에서 secret 검색 0건) |
| 시안 수준: 결과 화면 인증 전→후·게이지·칭호, 하루 한도 화면 오늘 인증 목록 | 구현 |
| 단위 테스트 | 152개 통과(RevisitStats 7개 추가) |
| 폰 설치 | 릴리스 APK(사용자 키)를 `adb install -r`로 덮어 설치, 시작 후 `AndroidRuntime` 오류 0. **화면 조작 시험은 하지 않음** — `nopo-09`의 해당 항목은 모두 "미실시" 유지 |

**막힌 것(사용자 필요):** ① **Firebase CLI 로그인 만료** → `firebase login --reauth` 후 `cd firebase && firebase deploy --only firestore:rules --project nopocheonnyeon` (storeStats·공개 범위 규칙). 배포 전에는 재방문율이 0명, 공개 범위 저장은 오류 문구. 앱 자체는 정상(집계 쓰기는 실패해도 방문 기록에 영향 없게 분리). ② 구글 Places 키(사진). ③ 영수증 실촬영 등 수동 QA(`nopo-09`).
**남은 개발 후보:** B-04(ViewModel·Repository·Room 테스트, Firestore 포트 분리 필요), B-15~B-24, 시안 보강(동기화 복원 진행 화면, 로그아웃 경고 대기 목록, 내 리뷰 수정·삭제 버튼, 개인정보 처리방침 화면, 라이선스 목록), D-01~D-04 데이터 보강.

### 10-2. 이어진 진행 (같은 날)
- B-04 일부: `SyncRejectionPolicy`(+테스트 4개) → 단위 테스트 156개. 로그아웃 경고에 올라가지 않은 가게 목록, 로그인 직후 "서버에서 기록을 불러오는 중" 배너(`AppContainer.restoring`), 설정의 개인정보 처리방침 전체 보기·라이선스 목록, 내 리뷰 "수정·삭제하러 가기".
- lint 오류 0건(Compose `LocalContext` 리소스 호출 수정). 쓰지 않는 문자열 제거, `err_*`는 `getIdentifier`로 읽으므로 `tools:ignore="UnusedResources"`.
- 이 PC에서 `./gradlew.bat lintDebug testDebugUnitTest assembleDebug`가 통과하는 상태에서 커밋.

### 10-3. nopo-qa 점검 반영
`nopo-qa` 에이전트(정적 읽기·빌드)가 높음 3건을 지적해 수정했다(Q-01~Q-03, `nopo-11` 참고). 규칙 보강분도 **Firebase 로그인 재인증 후 배포**해야 적용된다. 한글 리터럴은 디버그 전용 버튼·진단 샘플·@Preview에만 남아 있다(허용). 에이전트가 제안한 폰 확인 목록은 `nopo-09`의 O-01·R-09·V-06·V-10 등과 겹친다.

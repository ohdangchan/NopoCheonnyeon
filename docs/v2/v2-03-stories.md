> **[v2 문서]** 2026-10-23 작성. 원본 `docs/nopo-03-stories.md` 를 복사해 시안(design/redesign/1~6.webp) 우선 기준으로 고친 문서입니다.
> 원본은 그대로 두며, 이 문서와 원본이 다르면 **이 v2 문서가 우선**입니다. 변경 요약은 [v2-00-changes.md](v2-00-changes.md) 참조.

# 노포 천년 미식가 — 샤딩 및 스토리 분할 v0.4

관련 문서: `nopo-01-PRD.md`, `nopo-02-architecture.md`, `nopo-06-spike-plan.md`, `nopo-04-dev-qa-checklist.md`

> **진행 상태는 이 문서에 체크하지 않는다.** 스토리별 완료·미완 현황은 `docs/dev-status.md` 4장과 `docs/nopo-11-backlog.md`, 실기기 시험 결과는 `docs/nopo-09-real-device-qa.md`가 기준이다.

## 사용법 (Claude Code)
1. 위에서부터 **스토리 하나씩** Claude Code에 맡긴다. 요청문 틀은 `nopo-08-claude-code-guide.md` 3장.
2. 스토리 = 만들 파일 + 수락 기준(AC) + 먼저 쓸 테스트(U-번호, `nopo-04`). AC와 테스트가 모두 통과해야 완료.
3. 완료할 때마다 `./gradlew testDebugUnitTest assembleDebug` → 폰 설치 → 확인 → 커밋 `S-xxx: 요약`.
4. 규모: S ≈ 25분, M ≈ 50분, L ≈ 1.5시간. Claude Code가 코드를 쓰고 사람은 계획 확인·폰 테스트를 하는 기준이며, 처음 해 보면 1.5배로 잡는다.
5. ★ = 실기기 확인 필수 스토리.

---

## 일정과 시간 예산

| 일차 | 오전 | 오후·저녁 |
|---|---|---|
| 1 | **E0 기능 검증(스파이크)** — 지도·카메라·한국어 OCR·위치·Firebase 실기기 확인 | E1 기반, 좌표 보강 후 S-301 카탈로그 적재, E2 규칙 시작 |
| 2 | E2 규칙 나머지, E3 데이터·동기화 | E4 계정 |
| 3 | E5 지도·가게 | E6 방문 인증(영수증·사진·위치) |
| 4 (예비 겸 개발) | E7 도장·패스포트, E8 리뷰·프로필 | E9 테스트 가게·설정, 밀린 스토리 |
| 5 | **E10 실기기 QA·버그 수정** | 릴리스 APK 확정 |

| 에픽 | 스토리 수 | 추정 |
|---|---|---|
| E0 기능 검증 | 8 | 4.5h |
| E1 기반 | 6 | 3h |
| E2 도메인 규칙 | 12 | 7h |
| E3 데이터·동기화 | 8 | 5.5h |
| E4 계정 | 8 | 4.5h |
| E5 지도·가게 | 6 | 4.5h |
| E6 방문 인증 | 9 | 5.5h |
| E7 도장·패스포트 | 5 | 3h |
| E8 리뷰·프로필 | 5 | 4h |
| E9 테스트 가게·설정 | 3 | 1.5h |
| E10 릴리스·QA | 6 | 8h |
| **합계** | **76** | **개발 약 43h + QA 8h = 약 51h** |

> 스토리 크기(S≈25분, M≈50분, L≈1.5시간)로 더하면 개발이 약 43시간이라, 하루 12시간이면 3일 반이 걸린다. 그래서 **4일차는 예비를 겸한 개발일, 5일차는 QA만** 한다. **QA 하루는 줄이지 않는다.** 3일 안에 끝내야 하면 처음부터 P1 스토리(S-408, S-705, S-805)와 연출 다듬기를 빼고 E9 설정 화면을 최소로 만든다.

---

## E0. 1일차 오전 기능 검증 (모두 ★, 상세는 `nopo-06`)

| ID | 내용 | 파일 | AC |
|---|---|---|---|
| S-001 | 프로젝트 생성 + 진단 화면 뼈대 | `MainActivity.kt`, `feature/diagnostics/DiagnosticsScreen.kt` | 폰에 설치·실행, 회전 시 크래시 없음 |
| S-002 | 지도 탭 | `platform/map/NaverMapView.kt`, `DiagnosticsMapTab.kt` | SPK-1 체크 전부 |
| S-003 | 카메라 탭 | `platform/camera/CameraCapture.kt`, `DiagnosticsCameraTab.kt` | SPK-2 체크 전부 |
| S-004 | 한국어 OCR 탭 | `platform/ocr/MlKitKoreanRecognizer.kt`, `DiagnosticsOcrTab.kt` | SPK-3 체크 전부 |
| S-005 | 위치 탭 | `platform/location/FusedLocationAdapter.kt`, `MockDetector.kt`, `DiagnosticsLocationTab.kt` | SPK-4 체크 전부, 노포 앞 측정값 기록 |
| S-006 | Firebase 탭(필수) | `DiagnosticsFirebaseTab.kt` | SPK-5 체크 전부 |
| S-007 | 릴리스 APK | `app/build.gradle.kts`, `proguard-rules.pro`, `keystore.properties.sample` | 릴리스 APK에서 다섯 탭(지도·카메라·OCR·위치·Firebase) 동작, `keystore.properties`가 없으면 디버그 키로 서명돼 폰에 설치됨 |
| S-008 | 보고서 | `docs/spike-report.md` | 기기 2대 결과와 결정 기록 |

**게이트**: S-002~S-006(기기 기능 네 가지 + Firebase) 통과 전에는 E1 이후로 넘어가지 않는다.

---

## E1. 기반

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-101 | S | 빌드 정리 | `libs.versions.toml`, `.gitignore`, `local.properties.sample` | 버전이 카탈로그 한 곳, 키 파일 무시 |
| S-102 | S | 디자인 토큰·글꼴 | `core/designsystem/theme/*`, `res/font/*` | PRD 10.1 색, 라이트·다크 프리뷰. 글꼴은 Google Fonts(OFL)의 Black Han Sans·Noto Sans KR TTF를 소문자 파일명으로 넣음(내려받기는 Claude Code에 맡겨도 됨). 릴리스 APK 크기를 확인해 너무 크면 본문은 시스템 글꼴로 대체 |
| S-103 | M | 공통 컴포넌트 | `StampView`, `YearsGauge`, `TitleBadge`, `StarRating`, `NopoButton`, `EmptyState`, `OfflineBanner` | 프리뷰, 터치 48dp, 접근성 설명 |
| S-104 | S | 수동 주입·앱 초기화 | `NopoApp.kt`, `AppContainer.kt` | 지도 SDK·Firebase·Crashlytics 초기화, 디버그 StrictMode·LeakCanary |
| S-105 | M | 내비게이션 + 로그인 게이트 | `core/navigation/NopoNavHost.kt`, `Routes.kt` | SessionState에 따라 그래프 전환, 뒤로 가기로 우회 불가 |
| S-106 | S | 오류 모델·문구 | `core/common/AppError.kt`, `res/values/strings.xml` | PRD 7.4 표의 모든 코드에 문구 |

## E2. 도메인 규칙 (순수 Kotlin, 테스트 먼저)

| ID | 규모 | 내용 | 파일 | 테스트 |
|---|---|---|---|---|
| S-201 | S | 업력 계산(30 바닥) | `domain/rule/YearsCalculator.kt` | U-201-* |
| S-202 | S | 도장 등급 | `StampGradePolicy.kt` | U-202-* |
| S-203 | S | 칭호 | `TitlePolicy.kt` | U-203-* |
| S-204 | S | 거리 | `core/util/GeoDistance.kt` | U-204-* |
| S-205 | M | 정규화·유사도 | `TextNormalizer.kt`, `Similarity.kt` | U-205-* |
| S-206 | S | 사업자번호·전화번호 | `BusinessNumber.kt`, `PhoneNumber.kt` | U-206-* |
| S-207 | L | 영수증 파서 | `domain/verify/ReceiptParser.kt` | U-207-* |
| S-208 | M | 영수증 판정 | `ReceiptMatcher.kt` | U-208-* |
| S-209 | S | 영수증 해시 | `ReceiptHasher.kt` | U-209-* |
| S-210 | M | 사진 판정 | `PhotoMatcher.kt` | U-210-* |
| S-211 | S | 하루 한도 | `DailyLimitPolicy.kt` | U-211-* |
| S-212 | S | 리뷰 검증 | `ReviewRules.kt` | U-212-* |

## E3. 데이터·동기화

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-301 | M | 카탈로그 적재 | `data/catalog/StoreJson.kt`, `CatalogSeeder.kt`, `AppDatabase.kt`, `StoreDao.kt`, `assets/stores.json` | 적재 전에 `tools/geocode.py` → `tools/build_catalog.py`로 좌표를 채운 `stores.json` 사용(좌표 채움 비율 기록), 첫 실행 적재, 버전 변경 시 교체, 실패 시 이전 데이터 유지, **릴리스 빌드에서도** 파싱 |
| S-302 | S | 내 방문·대기열 테이블 | `MyVisitEntity`, `PendingOpEntity`, DAO | 같은 가게 재기록 시 `yearsAwarded` 유지 |
| S-303 | S | 설정 저장 | `SettingsStore.kt` (DataStore) | 온보딩·카탈로그 버전·필터 |
| S-304 | M | 카탈로그 질의 | `CatalogRepository` | 영역 질의, 검색(이름·지역·메뉴), 필터 조합 |
| S-305 | M | Firestore 원격 클래스 | `data/remote/*` | 경로·필드가 PRD 8.2와 일치 |
| S-306 | L | 방문 기록·동기화 ★ | `VisitRepository`, `SyncWorker`, `SyncScheduler` | 기기 먼저 기록, 묶음 쓰기, 해시 중복 시 되돌림, 비행기 모드 → 복귀 시 전송, 서버 방문 시각은 서울 0시로 잘라 저장, 동기화 상태를 화면이 관찰 가능 |
| S-307 | S | 서버 기록 복원 ★ | `VisitRepository.restoreFromServer()` | 재설치 후 로그인 시 세월·도장 수 일치 |
| S-308 | S | 보안 규칙·색인 배포 | `firebase/*` | 콘솔 규칙 플레이그라운드로 I-1~I-10 확인 (진단 문서 규칙 포함) |

## E4. 계정 (모두 ★)

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-401 | M | 인증 저장소 | `AuthRepository`, `AuthRemote`, `NicknameRemote` | SessionState(SignedOut/NeedsNickname/SignedIn), 오류 매핑 |
| S-402 | S | 온보딩 | `OnboardingScreen` | 3장, 건너뛰기, 재실행 시 안 나옴 |
| S-403 | M | 가입 | `SignUpScreen`, `AuthViewModel` | 입력 검증, 닉네임 중복 안내, 가입 즉시 지도 |
| S-404 | S | 로그인 | `LoginScreen` | 오류 문구, 로그인 유지 |
| S-405 | S | 비밀번호 재설정 | `ResetPasswordScreen` | 계정 존재 여부와 무관한 같은 안내 |
| S-406 | S | 닉네임 정하기(복구) | `NicknameSetupScreen` | users 문서 없는 계정이 앱을 못 씀 |
| S-407 | S | 로그아웃 | `SettingsScreen` 일부 | 대기열 경고, 사용자 데이터 정리 |
| S-408 | M | (P1) 닉네임 변경·탈퇴 | | |

## E5. 지도·가게

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-501 | M | 지도 정식화 ★ | `platform/map/*` | 진단 코드 재사용, 마커 등급 아이콘 재사용, 클러스터링 또는 영역 표시 |
| S-502 | M | 지도 화면 ★ | `MapScreen`, `MapViewModel` | 권한 4경우, 필터 칩, 오류 시 목록 전환 |
| S-503 | S | 하단 시트 | `StoreSheet` | 업력·거리·배지·평점 |
| S-504 | M | 검색·목록 | `StoreListScreen` | 좌표 없는 가게 검색, 가까운 순/업력 순 |
| S-505 | M | 가게 상세 | `StoreDetailScreen`, `StoreDetailViewModel` | 빈 필드 숨김, 보상 미리보기, 길찾기 |
| S-506 | M | 평점 요약·리뷰 목록 | `StoreReviewsSection` | 집계 쿼리 평균·개수, 20개씩 더보기, 평점 버튼 자격(인증 + 서버 동기화 완료, 그 전에는 `E_VISIT_SYNCING`) |

## E6. 방문 인증 (모두 ★)

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-601 | S | 카메라 정식화 | `CameraCapture` | 영수증 모드는 파일 저장 없음, 사진 모드는 1080px 저장 |
| S-602 | S | OCR 정식화 | `MlKitKoreanRecognizer` | 10초 제한, 축소, close |
| S-603 | M | 영수증 유스케이스 | `VerifyReceiptUseCase` | 위치 미사용, 7.1 흐름, 모든 실패가 코드 |
| S-604 | M | 영수증 화면 | `ReceiptVerifyScreen` | 가이드 틀, 분석 중, 실패별 문구, “가게 사진으로 인증” |
| S-605 | S | 위치 정식화 | `FusedLocationAdapter`, `LocationSettingsChecker` | 15초, 최선값, 해제 보장 |
| S-606 | M | 사진·위치 유스케이스 | `VerifyPhotoUseCase` | 30m·정확도 30m, 모의 위치, 좌표 없음, 3회 실패 처리 |
| S-607 | M | 사진·위치 화면 | `PhotoVerifyScreen` | 남은 초·정확도 표시, 재측정, 3회 후 위치 확인 도장 선택지 |
| S-608 | S | 결과 화면 | `VerifyResultScreen` | 코드별 다음 행동 버튼 |
| S-609 | M | 오프라인 대기 | `VerifyViewModel`, `SyncWorker` 연동 | “확인 중” 도장, 연결 후 확정 또는 중복 알림 |

## E7. 도장·패스포트

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-701 | M | 도장 연출 ★ | `StampCeremonyScreen` | 380ms, 600ms 카운트, 진동 1회, 애니메이션 끄기 대응 |
| S-702 | M | 패스포트 | `PassportScreen` | 게이지, 3열 격자, 빈 상태 |
| S-703 | S | 도장 상세 | `StampDetailSheet` | 인증 방법 표시, 인증샷 없으면 그림 |
| S-704 | S | 내 리뷰 탭 | `MyReviewsTab` | 수정·삭제 이동 |
| S-705 | S | (P1) 칭호 승급 연출 | `TitleUpOverlay` | |

## E8. 리뷰·프로필 (모두 ★)

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-801 | M | 리뷰 저장소 | `ReviewRepository`, `ReviewRemote` | 트랜잭션 저장·삭제(온라인 전용, 오프라인이면 `E_REVIEW_OFFLINE`), reviewCount 일치, 미방문 거부 |
| S-802 | M | 리뷰 작성 | `ReviewEditorScreen`, `ReviewEditorViewModel` | 별점만 저장 가능, 500자, 초안 보존(오프라인 안내 뒤에도), 수정·삭제 |
| S-803 | S | 리뷰 → 프로필 이동 | `StoreReviewsSection` | 닉네임·별점 영역 탭 |
| S-804 | L | 유저 프로필 | `UserProfileScreen`, `UserProfileViewModel` | 칭호·세월·다녀온 노포·매긴 평점, 페이징, 없는 유저 |
| S-805 | S | (P1) 리뷰 신고 | | |

## E9. 테스트 가게·설정

| ID | 규모 | 내용 | 파일 | AC |
|---|---|---|---|---|
| S-901 | S | 진단 화면 정리 | `DiagnosticsScreen` | 앱 정보 5회 탭 진입, 정보 복사 |
| S-902 | M | 테스트 가게 ★ | `TestStoreFactory` | 현재 위치 생성, 실제 로직으로 인증, 서버 미전송, 삭제 |
| S-903 | S | 설정 화면 | `SettingsScreen`, `DataSourceScreen`, `LicensesScreen` | 카탈로그 버전·가게 수·출처 |

## E10. 릴리스·QA

| ID | 규모 | 내용 | AC |
|---|---|---|---|
| S-1001 | M | 카탈로그 최종 점검 | 공식 지정리스트 CSV를 받았으면 `tools/import_official_list.py` → `tools/build_catalog.py`, 주소 없는 오래가게는 `--provider kakao` 이름 검색으로 좌표, `COORD_FROM_NAME` 가게 위치 확인 → 앱 재적재 확인 |
| S-1002 | S | 릴리스 서명·R8 | 릴리스 APK가 전 기능 동작 |
| S-1003 | L | 실기기 QA 1차 ★ | `nopo-04`의 M 항목 전부 기록 |
| S-1004 | M | 안정성 테스트 ★ | Monkey 5,000회, 회전·백그라운드·프로세스 종료·권한 철회 |
| S-1005 | L | 버그 수정 | 막힘·높음 0건 |
| S-1006 | S | 최종 APK 배포 | APK + 설치 안내, 서명 키 백업 2곳 |

---

## 의존 관계
```
E0(게이트) → E1 → E2(병렬 가능) ─┐
                 S-301 → S-302 → S-306 → S-307
E4(S-401→…) ─────────────────────┤
E5(S-501→S-502→S-505→S-506) ─────┤
E6(S-601,S-602,S-605 → S-603,S-606 → S-604,S-607 → S-608 → S-609)
E7(S-701 → S-702 → S-703,S-704)
E8(S-801 → S-802 → S-803 → S-804)
E9(S-901 → S-902), S-903
E10(S-1001 → S-1002 → S-1003 → S-1004 → S-1005 → S-1006)
```

## 컷라인 (밀릴 때 빼는 순서)
1. P1: S-408, S-705, S-805
2. 지도 클러스터 연출 다듬기(기능은 유지)
3. 도장 연출 세부(380ms 연출은 유지, 효과음·입자 효과 제외)
4. **QA·버그 수정 시간은 절대 줄이지 않는다**

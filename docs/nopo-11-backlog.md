# 백로그·알려진 결함 (2026-10-20, 전체 코드 재독 결과)

우선순위: **P0** 시연·QA 전에 고칠 것 / **P1** 출시 전 / **P2** 다음 버전. 각 항목은 근거(파일)와 완료 조건(AC)을 적었다. 고칠 때는 `CLAUDE.md` 규칙(스토리 단위, 도메인은 테스트 먼저, `testDebugUnitTest` → `assembleDebug` → 설치, 실기기 확인)을 따른다. 시험 항목 번호는 `nopo-09-real-device-qa.md`.

## 결함·누락 (코드 확인 완료)
| ID | 우선 | 내용 | 근거 | AC |
|---|---|---|---|---|
| B-01 | P0 (**2026-10-20 구현, 실기기 미확인**) | **중복 영수증 판정이 문서와 다름.** 동기화가 `PERMISSION_DENIED`면 방문 문서를 확인하지 않고 곧바로 "중복 영수증"으로 보고 첫 방문 기록을 지운다. 전송은 성공했는데 응답만 못 받아 재시도되면(규칙상 해시 문서가 이미 있어 거부) **정상 기록이 사라진다** | `data/visit/VisitRepository.kt` `sync()` (아키텍처 5.3, U-306-1) | 거부되면 `users/{uid}/visits/{storeId}`를 읽어 있으면 성공 처리(대기열 제거, `markSynced`), 없을 때만 되돌림. Repository 단위 테스트(가짜 Firestore 또는 포트 분리) |
| B-02 | P0 (**2026-10-20 구현, 실기기 미확인**) | **중복 영수증을 사용자에게 알리지 않는다.** `VisitRepository.duplicates` 흐름을 **아무도 수집하지 않는다**(`grep duplicates` 결과 선언만 있음). 서버가 거부하면 "확인 중" 도장이 아무 설명 없이 사라진다. `AppError.E_RECEIPT_DUP` 문구는 있으나 화면에서 쓰지 않는다 | `MainScreen`/`NopoNavHost`에 수집 코드 없음 | 오프라인/지연 전송에서 거부 시 "이미 사용한 영수증이에요" 알림(스낵바/다이얼로그) + 사진 인증 안내. 실기기 R-09·O-05 |
| B-03 | P1 (**2026-10-20 구현, 실기기 미확인**) | **지도 상단 "내 세월"이 테스트 방문을 포함.** `MapViewModel.state`는 `visits.sumOf { it.yearsAwarded }`, 패스포트·상세는 `TEST-` 제외 | `feature/map/MapViewModel.kt` | `observeTotalYears()`(테스트 제외)와 같은 값. T-08 |
| B-04 |  (**2026-10-20 일부: 동기화 거부 판정을 SyncRejectionPolicy로 분리해 단위 테스트 추가. ViewModel·Room 테스트는 아직 없음**) | **ViewModel·Repository·Room 자동 테스트 없음.** nopo-04의 U-VM-1~7, U-301-*, U-302-1, U-306-* 대응 테스트가 없다(`androidTest`는 템플릿) | `app/src/test`, `app/src/androidTest` | 각 항목 테스트 추가. `VisitRepository`는 Firestore 의존을 포트로 분리해야 테스트 가능 |
| B-05 | P1 (**2026-10-20 구현, 실기기 미확인**) | **인증 화면에 "기기 위치 꺼짐" 처리가 없다.** `FusedLocationAdapter.checkSettings()`는 진단 탭만 호출. 위치가 꺼져 있으면 15초 기다렸다가 `E_LOC_TIMEOUT` | `VerifyViewModel.measureLocation` (아키텍처 5.2 3단계, S-605) | 측정 전 `checkSettings()` → 시스템 "위치 켜기" 대화상자(`ResolvableApiException`) 처리. P-06 |
| B-06 | P1 (**2026-10-20 구현, 실기기 미확인**) | **진단 화면 정보 부족.** FR-SPK-03·S-901: 기기·Android·Play 서비스 버전·권한 상태·지도 인증 결과·마지막 위치 정확도·마지막 OCR 시간 + **텍스트로 복사**가 요구되나 모델·OS·앱 버전 3줄만 있다 | `feature/diagnostics/DiagnosticsScreen.kt DeviceInfo` | 위 항목 표시 + 복사 버튼(클립보드). Y-02 |
| B-07 | P1 (**2026-10-20 구현, 실기기 미확인**) | **"테스트 기록 삭제" 없음**(FR-TST-03, S-902 "삭제"). 테스트 도장은 앱 데이터 삭제로만 지울 수 있다 | `CatalogRepository`, `MainScreen` | 테스트 가게·테스트 방문 삭제 버튼(서버 무관). Y-07 |
| B-08 | P1 (**2026-10-20 구현, 실기기 미확인**) | **프로필 화면이 PRD보다 단순.** FR-PRF-02(가입일·게이지), FR-PRF-03(지역·등급·첫 방문일(날짜만)·단골 횟수, 20개씩), FR-PRF-04(탭 분리·날짜), 현재는 최대 60개를 한 화면에 가게명·세월만 표시. `ProfileRepository.profile()`은 `createdAt`을 읽지 않음 | `feature/profile/ProfileScreen.kt`, `data/remote/ProfileRepository.kt` | PRD 5.8 수락 기준 충족, 시안 `profile/user_profile` 참고(`PRD-차이점.md` 게이지 라벨 오류 주의) |
| B-09 | P1 (**2026-10-20 구현, 실기기 미확인**) | **영수증 가이드 틀 오버레이 없음**(FR-VER-03). 카메라 미리보기에 틀·안내만 텍스트로 있음 | `feature/verify/VerifyScreen.kt CameraArea` | 영수증 모양 틀/모서리 표시. R-12 |
| B-10 | P1 (**2026-10-20 구현, 실기기 미확인**) | **정보 오류 신고(FR-STORE-06) 없음.** 리뷰 신고만 있다(`reports` 컬렉션, 사유 4종) | `StoreReviewsSection` | 가게 상세에서 정보 오류 신고(사유 선택, 사진 첨부 없음). 규칙 `reports`는 `reporterUid`만 검사하므로 `kind`·`storeId` 필드 추가 시 규칙 확인 |
| B-11 | P1 (**2026-10-20 구현, 실기기 미확인**) | **칭호 승급 연출(S-705) 없음.** 결과 화면에 "새 칭호: …" 텍스트 한 줄만 | `VerifyScreen.ResultSection` | 승급 오버레이(시안 `stamp/title_promotion`, "東네 어른" 오타·특권 문구 제외). Lottie 도입 전 먼저 물을 것(`CLAUDE.md`) |
| B-12 | P1 (**2026-10-20 구현, 실기기 미확인**) | **방문 기록 비공개 설정(FR-PRF-07) 없음.** 서버 규칙은 로그인 사용자 누구나 `visits` 읽기 | `firebase/firestore.rules` | `users` 문서에 공개 범위 필드 + 규칙/화면. 시안 `settings/privacy_settings` |
| B-13 | P1 (**2026-10-20 구현: v1에서만 재생성, v2 이상은 Migration 필수, `app/schemas/` 스키마 기록. Migration 테스트는 없음**) | **Room 정식 마이그레이션 필요.** `fallbackToDestructiveMigration(dropAllTables = true)`는 스키마(현재 v2) 변경 시 **`my_visits`·`pending_ops`(미전송 인증)까지 삭제**한다 | `data/local/AppDatabase.kt` | 출시 전 `Migration` 작성, `exportSchema` 켜고 스키마 JSON 보관, 마이그레이션 테스트 |
| B-14 | P1 (**2026-10-20 구현: 릴리스 APK에서 Client Secret 제거, dex 검색 0건 확인. 공개 배포 전 콘솔에서 기존 키 교체는 사용자 몫**) | **네이버 Client Secret이 APK에 포함**(`BuildConfig.NAVER_MAP_CLIENT_SECRET`, 진단 역지오코딩 용도) | `app/build.gradle.kts`, `platform/map/ReverseGeocoder.kt` | 릴리스에서는 제거하거나 콘솔에서 사용 한도·허용 패키지 설정, 키 교체 |
| B-15 |  (**2026-10-20 구현: 저장·삭제를 트랜잭션으로, 실기기 오프라인 시험(M-49)은 미실시**) | 리뷰 저장·삭제가 **배치**(문서는 트랜잭션). 오프라인은 12초 타임아웃과 `UNAVAILABLE` 매핑으로 막음 | `data/remote/ReviewRepository.kt` | 필요 시 트랜잭션 전환, M-49 확인 |
| B-16 | P2 | 리뷰 편집기·도장 연출·도장 상세가 별도 화면이 아니라 `AlertDialog`/인라인(아키텍처 2장의 `ReviewEditorScreen` 등과 다름) | `StoreReviewsSection`, `VerifyScreen`, `PassportScreen` | 시안에 맞춰 분리(선택). 도장 연출은 시안의 인증 전/후 게이지·칭호·"평점 남기기" 연결 포함 |
| B-17 | P2 | 시계 오차 경고 없음(아키텍처 9장: 서버 시간과 10분 이상 차이 나면 경고) | — | `Timestamp` 비교 또는 서버 시각 문서 |
| B-18 |  (**2026-10-20 구현, 실기기 미확인**) | **지도 오류 시 자동 목록 전환 없음.** 인증 오류가 나면 상단 빨간 배너만 표시하고 사용자가 "목록으로 보기"를 눌러야 한다(PRD 10.2 "지도 인증 오류(목록으로 전환)") | `feature/map/MapScreen.kt` | `authError != null`이면 자동 목록 모드 + 안내. M-09 |
| B-19 | P2 | 세션 게이트: 프로필 확인이 오프라인·타임아웃이면 "있음"으로 간주 | `AuthRepository.hasProfile` | 의도된 동작이나 문서화·시험 필요(A-17) |
| B-20 |  (**2026-10-20 구현(E_REVIEW_OFFLINE 문구만 PRD와 일치, E_NETWORK는 로그인 오류에도 쓰여 현행 유지)**) | 안내 문구가 PRD 7.4와 약간 다름(예: `E_REVIEW_OFFLINE` "리뷰는 온라인에서만 쓸 수 있어요" vs PRD "인터넷에 연결되면 저장할 수 있어요…", `E_NETWORK` "네트워크에 연결되지 않았어요" vs "인터넷이 연결되면 자동으로 저장할게요") | `res/values/strings.xml` | PRD와 맞추거나 PRD 개정(결정 기록) |
| B-21 |  (**2026-10-20 구현: lint 오류 0, 쓰지 않는 문자열 정리·err_* 예외 표시**) | 쓰지 않는 문자열 리소스(`placeholder_*`, `tab_*_placeholder`, `map_zoom_in_hint` 등), `android.media.ExifInterface` 사용(진단), `String.toUri` 권장 등 lint 경고 | `lintDebug` | 정리 |
| B-22 |  (**2026-10-20 구현: 라이브러리별 목록(버전은 libs.versions.toml이 정본)**) | 라이선스 화면이 고정 문자열(+`assets/licenses` 2개). 시안은 라이브러리별 목록 | `SettingsScreen` | 라이브러리별 고지(의존성에서 생성) |
| B-23 |  (**2026-10-20 일부: 설정에서 개인정보 처리방침 전체 보기(운영자·연락처 없음). 별도 화면·법적 검토는 남음**) | 개인정보 처리방침 별도 화면 없음(설정에 요약 문장). `docs/privacy-notice.md`에 초안 작성됨 | `SettingsScreen` | 실제 운영 정보(운영자·연락처) 확정 후 화면화 |
| B-24 | P2 | 하드코딩 한글: `MainScreen.DebugVisitButton`, `DiagnosticsSampleOcr`, `CatalogRepository.createTestStore`(테스트 가게 이름·소개) — 디버그/진단 한정 | — | 허용 예외로 두되 `nopo-qa` 검사에서 제외 목록으로 명시 |
| B-25 | P1 (**2026-10-20 구현, 실기기 미확인**) | **촬영 버튼 중복 탭 방지 확인.** 이전 세션에서 진단 "파일로 촬영" 한 번에 사진 2장이 저장됐다(2.1초 간격). `VerifyViewModel`은 `busy`로 막지만 진단 탭(`DiagnosticsCameraTab`)과 `capturePhoto`/`captureReceipt`의 연타 방어를 실기기로 재현·확인 | `DiagnosticsCameraTab.kt`, `VerifyViewModel.kt` | 연속 탭에도 촬영 1회, 인증샷 1장 |
| B-26 | P1 | **Firebase 시험 계정·문서 정리**: `spk5.f971n@nopo.test`, `spk5.emu37@nopo.test`와 `diagnostics/{uid}` | Firebase 콘솔 | QA 마지막 날 삭제 |

## 신규 기능 요청 (사용자, 2026-10-20)
| ID | 우선 | 내용 | 설계(제안) | AC |
|---|---|---|---|---|
| F-01 | P1 (**2026-10-20 구현, 규칙 배포·실기기 미확인**) | **가게별 재방문율**을 가게 상세·리뷰 목록·프로필에서 보기 | 정의: 재방문율 = 그 가게를 인증한 사용자 중 **2회 이상** 인증한 사용자 비율, "N명 중 K명(P%)"로 표시. 서버 함수 없이 Firestore **컬렉션 그룹 집계 쿼리**: `collectionGroup("visits").whereEqualTo("storeId", id)`의 count와 `…whereGreaterThanOrEqualTo("visitCount", 2)`의 count. 필요 변경: ① `firestore.rules`에 `match /{path=**}/visits/{storeId} { allow read: if signedIn(); }` ② 복합 색인 `visits`(COLLECTION_GROUP) `storeId ASC + visitCount ASC` 배포 ③ `ReviewRepository` 옆에 `VisitStatsRepository.revisitRate(storeId)` ④ 가게 상세 평점 영역에 "재방문 K/N명", 리뷰 항목에는 이미 있는 "단골 N회" 유지, 프로필의 "다녀온 노포"에 단골 횟수(B-08과 함께). 표본이 작으면(방문자 <5명) 비율 대신 "N명 중 K명"만 표시 | 상세에 재방문 수치 표시, 규칙·색인 배포 후 실기기 확인, 방문자 0명이면 줄 숨김, 사용자 개인 식별 없이 집계만 노출(PRD 8.2 날짜만 공개 원칙 유지). PRD에 FR-STORE-07 추가 |

> **✔ 배포 완료(2026-10-20):** `firebase/firestore.rules`(storeStats·visitsVisibility·Q-03 보강)와 색인을 `nopocheonnyeon`(asia-northeast3)에 배포했다. 재방문율·공개 범위의 실기기 확인(`nopo-09` N-01·N-03)은 아직이다. F-01은 컬렉션 그룹 쿼리 대신 `storeStats/{storeId}` 카운터를 쓴다.

## nopo-qa 점검(2026-10-20, 6f050bc..60755a5)에서 나온 항목
| ID | 상태 | 내용 |
|---|---|---|
| Q-01 | **수정함** | 영수증 해시 없는 기록이 PERMISSION_DENIED로 거부되면 대기열 맨 앞을 영구히 막음 → 건너뛰고 계속 보내며 10회 거부되면 대기열에서 뺌(기기 기록은 "확인 중"으로 남음) |
| Q-02 | **수정함** | 재방문 응답 유실 때 "중복 영수증" 오판 → 서버 방문 횟수가 기기 횟수 이상이면 이미 올라간 것으로 판정(`SyncRejectionPolicy`, 테스트 5개) |
| Q-03 | **수정함(배포 대기)** | `storeStats` 임의 조작 → 내 방문 문서가 있는 가게에만 올릴 수 있게 규칙 보강, `is int` 검사, users create의 `visitsVisibility` 값 검증. **같은 사용자가 반복 호출로 부풀리는 것은 규칙만으로 막을 수 없음**(서버 함수가 필요) |
| Q-04 | 남음(낮음) | `storeStats` 쓰기가 실패하면 재시도 없이 버려짐(통계 손실) → 별도 대기열 또는 배치 포함 |
| Q-05 | **수정함** | `VisitRepository.record`가 로컬 기록과 대기열 추가를 한 트랜잭션으로 묶지 않음(그 사이 앱 종료 시 서버로 안 올라감) → `database.withTransaction` |
| Q-06 | **수정함(복원 완료를 최대 10초 기다린 뒤 기록)** | 재설치 직후 `restoreFromServer` 전에 인증하면 서버에 있는 가게를 첫 방문으로 보고 세월 중복 가산 가능 → 인증 전 복원 완료 대기 |
| Q-07 | 알려진 한계 | 공개 범위가 PRIVATE여도 `reviews`·`users`(세월·방문 수·칭호)는 로그인한 누구나 읽음(리뷰는 공개 설계). 설정 화면 문구에는 "남긴 평점은 항상 공개"로 안내함. 서버 판정 없는 구조의 한계(규칙은 users 수정에서 totalYears 200/회 상한만 둠) |
| Q-08 | 남음(낮음) | payload 파싱 실패 항목을 조용히 삭제, 같은 가게의 대기열 항목이 여럿일 때 첫 항목 성공 직후 `markSynced` |

## 데이터·콘텐츠
| ID | 우선 | 내용 | 방법 |
|---|---|---|---|
| D-01 | P1 | 좌표 없는 가게 96곳(사진·위치 인증 불가, `E_NO_COORD`) | 카카오 REST 키 → `python tools/geocode.py --provider kakao` → `python tools/build_catalog.py` → 앱 재적재. `COORD_FROM_NAME` 가게는 지도에서 위치 확인 |
| D-02 | P1 | 공식 지정리스트 CSV 미반영(1,511행 중 소상공인마당에 없는 가게) | `python tools/import_official_list.py --csv …` → `build_catalog.py` (S-1001) |
| D-03 | P1 | 공식 사진 86곳(7%)만 있고 나머지는 글자 카드 | 웹 사진 내려받기(앱 포함) 또는 Google Places(1,277곳×3장 ≈ $20, 약관 확인) |
| D-04 | P1 | 소개글은 수집 도구 요약문 — 출시 전 직접 재작성 또는 출처 표기 | 가게별 |
| D-05 | P2 | 업력 30년 미만으로 나오는 백년가게 207곳(재등록일 추정)은 앱에서 30년으로 계산 | 창업연도 보강 |
| D-06 | P2 | 서울 오래가게 22곳 제외(상호 미확인·폐업 의심), 2017 회차 2곳 이름 미확인 | `data/README.md` |
| D-07 | P2 | 자주 시험할 가게 5곳의 좌표를 지도에서 눈으로 확인 | 현장 |

## 문서 정리 (낡은 내용)
| ID | 내용 |
|---|---|
| C-01 | `nopo-02-architecture.md`: 6.1 `fallbackToDestructiveMigration` 금지 문구(현재 사용 중), 6.3 규칙 예시(실제 파일은 닉네임 변경 허용), 7.3 `abiFilters`(arm64만), 7.4 R8(`-dontoptimize`), 2장 패키지(`feature/stamp`·`feature/review`·`platform/haptics`·`data/sync`·`domain/repository` 없음 — 실제는 각각 verify·store·VerifyScreen 내부·`data/visit`·없음) |
| C-02 | `nopo-03-stories.md`/`nopo-04-dev-qa-checklist.md` 체크 표시 → 진행도는 `dev-status.md` 4장, 시험 결과는 `nopo-09`로 일원화(이 두 문서는 계획서로 유지) |
| C-03 | `nopo-01-PRD.md`: D-15(arm64만), R8 `-dontoptimize`, Crashlytics 릴리스만 수집, 닉네임 변경 허용 규칙을 결정 기록에 추가 |
| C-04 | `START-HERE.md` 데이터 현황(좌표 0 → 1,181곳), `data/README.md` 상단 표 |
| C-05 | `design/stitch/README.md` "공식 사진 86곳" 등 수치와 `PRD-차이점.md`의 "음식점 834곳"은 최신. 시안의 "음식점 751곳"은 백년가게 음식점 수라 혼동 주의 |

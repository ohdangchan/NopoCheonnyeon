# 노포 천년 미식가 — 전체 51개 화면 목록 및 유저 플로우 최종 종합 정리 보고서 (v0.4)

> **프로젝트 개요**: 내가 먹어 온 노포의 세월을 모아 천 년을 채우는 안드로이드 미식 패스포트  
> **기술 스택**: Android (Kotlin, Jetpack Compose, Navigation Compose, Room, DataStore, WorkManager), Firebase (Auth, Firestore, Crashlytics), Naver Map SDK v3.24.0, CameraX, ML Kit 한국어 OCR v16.0.1, Fused Location  
> **문서 버전**: v0.4 (프로덕션 릴리스 & 실기기 무결점 동작 기준)  
> **총 제작 화면 수**: **51개 화면** (정상 경로, 예외/대체 처리, 엣지 케이스, 시스템 거버넌스 및 개발자 진단 콘솔 100% 완비)

---

## 1. 전체 51개 화면 마스터 카탈로그 (Master Inventory)

51개 전체 화면을 8개 기능 도메인별로 분류하고, 각각의 **화면 번호(ID)**, **화면명**, **대응 Composable 및 라우트**, **핵심 기능 및 PRD 스펙 근거**를 1:1로 매핑한 총괄 색인표입니다.

| 순번 | 화면 ID | 화면명 (Screen Title) | 라우트 경로 / Composable | 핵심 기능 및 요구사항 (PRD/아키텍처 근거) |
|---|---|---|---|---|
| **[도메인 1: 온보딩 & 계정 인증 (Auth & Onboarding - 8개 화면)]** | | | | |
| 1 | `SCREEN_50` | 온보딩 및 미식 여정 소개 | `auth/onboarding`<br>`OnboardingScreen` | 3장 페이저 스와이프, 노포 세월 적립 컨셉 소개, 건너뛰기 (`FR-AUTH-02`) |
| 2 | `SCREEN_49` | 회원가입 및 미식가 등록 | `auth/signup`<br>`SignUpScreen` | 이메일/비밀번호(8자+숫자), 닉네임 실시간 중복 체크, 즉시 가입 (`FR-AUTH-01`) |
| 3 | `SCREEN_46` | 로그인 및 패스포트 복원 | `auth/login`<br>`LoginScreen` | 이메일 로그인, 로그인 유지, 비번 재설정 링크, Firebase 인증 에러 3종 처리 (`FR-AUTH-02,03`) |
| 4 | `SCREEN_43` | 비밀번호 재설정 및 복구 안내 | `auth/reset-password`<br>`ResetPasswordScreen` | 계정 존재 여부를 숨기는 보안 안내 문구 메일 전송 (`FR-AUTH-04`) |
| 5 | `SCREEN_40` | 식객 칭호 설정 및 호적부 복구 | `auth/nickname-setup`<br>`NicknameSetupScreen` | 프로필 문서 없는 계정의 닉네임 복구 게이트 (`FR-AUTH-07`) |
| 6 | `SCREEN_24` | 식객 닉네임 변경 및 계정 관리 | `profile/edit-nickname`<br>`NicknameEditScreen` | 닉네임 유일성 체크 후 변경, 작성 리뷰 닉네임 일괄 갱신 (`FR-AUTH-08`) |
| 7 | `SCREEN_25` | 회원 탈퇴 및 호적부 영구 말소 | `profile/withdraw`<br>`WithdrawScreen` | 호적부/방문/리뷰 영구 파기 경고 및 Firebase Auth 계정 삭제 (`FR-AUTH-09`) |
| 8 | `SCREEN_36` | 식객 로그아웃 및 대기열 경고 | `dialog/logout`<br>`LogoutWarningDialog` | 로컬 SQLite 전송 대기열(`pending_ops`) 잔여 시 데이터 유실 방지 경고 (`FR-AUTH-05`) |
| **[도메인 2: 지도 & 노포 탐색 (Map & Store Discovery - 7개 화면)]** | | | | |
| 9 | `SCREEN_59` | 지도 탐색 및 노포 발견 | `main/map`<br>`MapScreen` | 네이버 지도 SDK 3.24.0, 내 위치, 등급별 마커(청록/붉은/금빛), 바텀시트 (`FR-MAP-01~05`) |
| 10 | `SCREEN_48` | 노포 검색 및 목록 보기 | `map/list`<br>`StoreListScreen` | 상호·지역·메뉴 실시간 검색, 거리순/업력순 정렬, 좌표 미보강 가게 지원 (`FR-MAP-06,07`) |
| 11 | `SCREEN_58` | 노포 상세 및 인증 자격 | `store/{storeId}`<br>`StoreDetailScreen` | 업력 대형 타이포, 세월 보상 미리보기(+N년), 길찾기, 인증/평점 버튼 (`FR-STORE-01~04`) |
| 12 | `SCREEN_17` | 노포 지도 필터 및 정렬 상세 설정 | `map/filter`<br>`MapFilterBottomSheet` | 음식점만, 백년가게/오래가게, 미방문 노포 필터 토글 칩 (`FR-MAP-03`) |
| 13 | `SCREEN_12` | 지도 오류 대체 목록 및 오프라인 탐색 | `map/fallback-list`<br>`MapFallbackScreen` | 네이버 지도 인증 실패(401/429/800) 및 음영 구역 시 텍스트 목록 자동 전환 (`FR-MAP-07`) |
| 14 | `SCREEN_32` | 노포 길찾기 및 대중교통 안내 | `store/{id}/transit`<br>`TransitGuideSheet` | 네이버 지도 / 카카오맵 / 카카오 T 딥링크 연동 및 도보/대중교통 길안내 (`FR-STORE-05`) |
| 15 | `SCREEN_5` | 사라진 노포 추모관 및 맛의 기억 | `passport/memorial`<br>`MemorialScreen` | 재개발/화재로 폐업한 노포 아카이브, 영구 보존 도장, 추억 한 줄 헌정 (기획안 13장) |
| **[도메인 3: 방문 인증 파이프라인 (Verification Pipeline - 7개 화면)]** | | | | |
| 16 | `SCREEN_56` | 영수증 방문 인증 화면 | `verify/{id}/receipt`<br>`ReceiptVerifyScreen` | 위치 미사용, 카메라 가이드틀, ML Kit OCR, 전화/상호 및 24시간 검증 (`FR-VER-01~07`) |
| 17 | `SCREEN_54` | 가게 사진 및 위치 인증 | `verify/{id}/photo`<br>`PhotoVerifyScreen` | 30m 반경 및 ±30m 정밀 GPS 검증, 간판/메뉴판 OCR 대조 (`FR-VER-09,10`) |
| 18 | `SCREEN_41` | 방문 인증 결과 및 대체 인증 안내 | `verify/result`<br>`VerifyResultScreen` | 인식 실패, 24시간 초과 등 오류 코드별 안내 및 사진 대체 인증 유도 (`FR-VER-16`) |
| 19 | `SCREEN_37` | 위치 확인 도장 구제 및 대체 발급 | `verify/{id}/relief`<br>`LocationReliefSheet` | 사진 OCR 3회 실패 시 30m 현장 확인 도장 구제 발급 (`FR-VER-11`) |
| 20 | `SCREEN_22` | 영수증 중복 해시 반려 안내 | `verify/{id}/dup`<br>`ReceiptDuplicateSheet` | SHA-256 영수증 해시 중복 시 `E_RECEIPT_DUP` 경고 및 안전 롤백 (`FR-VER-06`) |
| 21 | `SCREEN_29` | 일일 인증 한도 및 단골 쿨타임 안내 | `verify/{id}/limit`<br>`DailyLimitSheet` | 하루 5곳 한도 초과(`E_DAILY_LIMIT`) 및 동일 노포 24시간 쿨타임 안내 (`FR-VER-14`) |
| 22 | `SCREEN_31` | 위치 권한 안내 및 대략적 위치 경고 | `permission/location`<br>`LocationPermissionScreen` | 정밀 위치(FINE) 권한 필수 안내 및 대략적 위치 허용 시 설정 이동 유도 (`E_LOC_APPROX_ONLY`) |
| **[도메인 4: 도장 및 세월 의식 (Ceremony & Title Progression - 2개 화면)]** | | | | |
| 23 | `SCREEN_57` | 도장 획득 및 세월 적립 연출 | `ceremony/stamp`<br>`StampCeremonyScreen` | 380ms 수축 낙하 각인, 600ms 카운터 롤링, 40ms 햅틱 진동 (`FR-STP-01~03`) |
| 24 | `SCREEN_42` | 칭호 승급 축하 연출 | `ceremony/title-up`<br>`TitleUpOverlay` | 100/300/600/1,000년 돌파 시 축하 파티클, 황동 인장 및 칭호 훈장 수여 (`FR-STP-04,06`) |
| **[도메인 5: 패스포트 & 아카이브 (Passport & Chronicling - 9개 화면)]** | | | | |
| 25 | `SCREEN_55` | 내 패스포트 및 미식 연대기 | `main/passport`<br>`PassportScreen` | 총 누적 세월, 3열 도장 격자, 칭호 게이지, 내 리뷰 탭 전환 (`FR-PAS-01,02`) |
| 26 | `SCREEN_39` | 내 패스포트 (도장 0개 첫 시작) | `passport/empty`<br>`PassportEmptyView` | 신규 유저 온보딩용 빈 격자, "첫 노포를 탐방해보세요" 스타터 가이드 (`FR-PAS-02`) |
| 27 | `SCREEN_45` | 도장 상세 및 방문 각인 기록 | `passport/stamp/{id}`<br>`StampDetailSheet` | 첫 방문일, 적립 세월, 인증 방법(영수증/사진/구제), 현장 인증샷, 단골 수 (`FR-PAS-03`) |
| 28 | `SCREEN_18` | 100년 노포 황금 도장 상세 및 인증서 | `passport/gold-cert/{id}`<br>`GoldCertificateSheet` | 100년 이상 노포 방문 시 발급되는 공식 디지털 공인 인증서 및 인장 (`FR-STP-03`) |
| 29 | `SCREEN_7` | 식객 미식 여정 통계 및 연대기 결산 | `passport/report-analytics`<br>`ArchiveReportScreen` | 연대별 분포, 권역별 맵, 국밥/면 음식 선호도, 최장수 노포 배지, 이미지 저장 (`FR-PAS-01`) |
| 30 | `SCREEN_27` | 미식 패스포트 공유 카드 생성기 | `passport/share-card`<br>`ShareCardScreen` | 인스타그램 스토리/피드 규격 9:16 세월 카드, 3종 레트로 테마, 갤러리 저장 (`FR-PAS-05`) |
| 31 | `SCREEN_23` | 지역 컬렉션 및 테마 미식 퀘스트 | `passport/quests`<br>`QuestCollectionScreen` | 을지로 노포 골목, 백년 평양냉면 순례 등 테마 퀘스트 및 완성 배지 (기획안 13장) |
| 32 | `SCREEN_30` | 식객 명예의 전당 및 칭호 순위 | `passport/hall-of-fame`<br>`HallOfFameScreen` | 전국 천년 미식가 랭킹, 주간/월간 최다 탐방 랭커 및 내 순위 비교 (기획안 13장) |
| 33 | `SCREEN_19` | 방문 기록 비공개 및 프라이버시 설정 | `profile/privacy`<br>`PrivacySettingsSheet` | 타 유저에게 내 방문 연대기/리뷰 공개 여부 설정 (`FR-PRF-07`) |
| **[도메인 6: 공인 리뷰 & 식객 프로필 (Review & Social Profile - 7개 화면)]** | | | | |
| 34 | `SCREEN_51` | 공인 미식 평점 및 리뷰 작성 | `review/editor/{id}`<br>`ReviewEditorScreen` | 별점 1~5점 필수, 글 0~500자 선택, 실시간 글자수 카운터 (`FR-REV-01~07`) |
| 35 | `SCREEN_21` | 공인 리뷰 수정 및 삭제 에디터 | `review/modifier/{id}`<br>`ReviewModifierScreen` | 기존 작성 리뷰 원문 유지, 수정 및 삭제 트랜잭션 처리 (`FR-REV-03`) |
| 36 | `SCREEN_20` | 오프라인 리뷰 작성 및 초안 보존 안내 | `review/offline-draft`<br>`OfflineReviewBanner` | 오프라인 시 `E_REVIEW_OFFLINE` 안내 및 초안 메모리/DB 안전 보존 (`FR-REV-06`) |
| 37 | `SCREEN_35` | 노포 공인 미식가 리뷰 목록 | `review/store/{id}`<br>`StoreReviewsScreen` | 서버 집계 평균 별점, 20개씩 무한 페이징, 식객 칭호 배지 표시 (`FR-STORE-03`) |
| 38 | `SCREEN_34` | 내 패스포트 - 내 작성 리뷰 목록 | `review/my`<br>`MyReviewsScreen` | 본인이 작성한 전체 공인 리뷰 연대기 모아보기 및 수정/삭제 링크 (`FR-PAS-04`) |
| 39 | `SCREEN_53` | 타 유저 미식가 공인 프로필 | `profile/{uid}`<br>`UserProfileScreen` | 타 식객의 칭호, 누적 세월, 다녀온 노포(날짜만 공개), 매긴 평점 열람 (`FR-PRF-01~06`) |
| 40 | `SCREEN_33` | 노포 정보 수정 제보 및 리뷰 신고 | `store/{id}/report`<br>`StoreReportSheet` | 영업시간/휴무일/폐업 제보 및 부적절한 리뷰 신고 (`FR-STORE-06, FR-REV-08`) |
| **[도메인 7: 시스템 설정 & 동기화 센터 (Settings & Sync - 6개 화면)]** | | | | |
| 41 | `SCREEN_47` | 설정 및 데이터 출처 안내 | `main/settings`<br>`SettingsScreen` | 카탈로그 버전, 1,277곳 데이터 출처, 계정 관리, 앱 버전 5-Tap 진입 (`FR-SYS-01,03`) |
| 42 | `SCREEN_38` | 오프라인 모드 및 대기열 동기화 | `settings/offline-queue`<br>`OfflineQueueScreen` | 오프라인 캐시 현황, 미전송 대기열 작업 목록, 수동 '지금 즉시 동기화' (`FR-VER-15`) |
| 43 | `SCREEN_8` | 식객 알림 및 세월 동기화 센터 | `settings/notifications`<br>`NotificationsScreen` | 오프라인 도장 동기화 확정 통보, 칭호 승급 알림, 24시간 쿨타임 해제 안내 (`FR-VER-15`) |
| 44 | `SCREEN_11` | 서버 방문 기록 복원 및 동기화 | `settings/restore-sync`<br>`RestoreSyncScreen` | 재설치/기기변경 시 Firestore 호적부 다운로드 및 Room DB 복원 마이그레이션 (`FR-AUTH-10`) |
| 45 | `SCREEN_26` | 개인정보 및 위치정보 처리방침 고지 | `settings/privacy-policy`<br>`PrivacyPolicyScreen` | 영수증 이미지 비저장, 좌표 미전송, 개인정보 최소 수집 법적 고지 (`FR-SYS-01`) |
| 46 | `SCREEN_44` | 오픈소스 라이선스 고지 | `settings/licenses`<br>`LicensesScreen` | 네이버 지도 SDK, ML Kit, Firebase 등 오픈소스 라이브러리 고지 (`FR-SYS-01`) |
| **[도메인 8: 개발자 진단 & QA 도구 (Developer & QA Tools - 5개 화면)]** | | | | |
| 47 | `SCREEN_9` | 1일차 스파이크 종합 진단 콘솔 | `diagnostics/spike`<br>`SpikeDiagnosticsScreen` | 지도·카메라·OCR·위치·Firebase 5대 기능 실시간 프로브 측정 및 로그 복사 (`S-001~008`) |
| 48 | `SCREEN_52` | 시스템 진단 및 테스트 가게 관리 | `diagnostics/main`<br>`DiagnosticsScreen` | 단말기 스펙, Room/DataStore 캐시 초기화, 5-Tap 히든 콘솔 (`S-901`) |
| 49 | `SCREEN_28` | 테스트 가게 생성 및 실기기 시뮬레이터 | `diagnostics/test-store`<br>`TestStoreScreen` | 현재 위치에 가상 노포 즉시 생성, 실제 검증 파이프라인 무제한 반복 테스트 (`S-902`) |
| 50 | `SCREEN_16` | 테스트 영수증 검증기 및 OCR 인스펙터 | `diagnostics/ocr-inspector`<br>`OcrInspectorScreen` | 카메라 실시간 OCR 추출 필드(상호, 전화, 일시, 금액) 정규식 디버깅 콘솔 (`S-602`) |
| 51 | `SCREEN_10` | 릴리스 서명 APK 설치 및 보안 안내 | `guide/apk-install`<br>`ApkInstallGuideScreen` | '출처를 알 수 없는 앱 허용', Play 프로텍트 해제 가이드 및 SHA-256 지문 고지 (`S-1006`) |

---

## 2. 핵심 8대 엔드투엔드 유저 플로우 (End-to-End User Flows)

51개 화면이 유기적으로 결합되어 구동되는 8개 핵심 서비스 흐름입니다.

```
[FLOW 1: 온보딩 및 회원가입]
SCREEN_50 (온보딩) ──> SCREEN_49 (회원가입) ──> SCREEN_59 (지도 메인)
        │                      │ (경합 발생 시)
        └───> SCREEN_46 (로그인) ──> SCREEN_40 (닉네임 설정/호적부 복구)
                     │ (비번 분실)
                     └───> SCREEN_43 (비밀번호 재설정)

[FLOW 2: 노포 탐색 및 상세 조회]
SCREEN_59 (지도) ──[마커 탭]──> 바텀시트 ──[자세히 보기]──> SCREEN_58 (노포 상세)
   │        │                                                      │
   │        └──[필터 칩]──> SCREEN_17 (지도 필터 시트)              ├──> SCREEN_32 (대중교통 길찾기)
   │                                                               └──> SCREEN_33 (정보 제보/신고)
   └──[검색창]──> SCREEN_48 (검색/목록) ──[아이템 탭]───────────────┘
   │
   └──[네트워크/지도 오류]──> SCREEN_12 (오프라인 대체 목록)

[FLOW 3: 기본 영수증 방문 인증 (P0 핵심)]
SCREEN_58 (상세) ──[방문 인증하기]──> SCREEN_56 (영수증 촬영)
                                            │
               ┌────────────────────────────┴─────────────────────────────┐
        [성공 (일치+24h)]                                            [오류 발생]
               ▼                                                          ▼
     SCREEN_57 (도장 각인 연출)                                     SCREEN_41 (결과/대체 안내)
               │                                                          │
               ├──[세월 누적 & 햅틱]                                      ├──[24h 초과/인식불가] ──> 사진인증
               └──[칭호 승급 조건 충족] ──> SCREEN_42 (칭호 승급)          ├──[중복 영수증] ──> SCREEN_22 (중복 반려)
                                                                          └──[일일 5회 한도] ──> SCREEN_29 (한도 안내)

[FLOW 4: 30m 사진 & 위치 대체 인증 및 구제 (P0 핵심)]
SCREEN_41 (결과/대체 안내) ──[가게 사진으로 인증]──> SCREEN_31 (위치 권한 안내 - 미허용 시)
                                                                │
                                                                ▼
                                                    SCREEN_54 (사진/위치 인증)
                                                                │
                            ┌───────────────────────────────────┴────────────────────────────────────┐
                   [성공 (30m+간판OCR)]                                                [3회 실패 (한자/악천후)]
                            ▼                                                                        ▼
                  SCREEN_57 (도장 각인 연출)                                          SCREEN_37 (위치확인 도장 구제)
                                                                                                     │
                                                                                                     └──> SCREEN_57 (도장 각인)

[FLOW 5: 패스포트 열람 및 미식 연대기 결산]
SCREEN_55 (내 패스포트) ────[도장 0개 첫 진입]────> SCREEN_39 (패스포트 빈 상태)
   │
   ├──[도장 셀 클릭]──> SCREEN_45 (도장 상세 시트)
   │                            │ (100년 이상 노포)
   │                            └──> SCREEN_18 (100년 황금 인증서)
   │
   ├──[연대기 결산]──> SCREEN_7 (미식 여정 통계 결산)
   ├──[공유하기]────> SCREEN_27 (미식 공유 카드 생성기)
   ├──[퀘스트 탭]───> SCREEN_23 (지역/테마 퀘스트)
   ├──[명예의 전당]─> SCREEN_30 (식객 순위 랭킹)
   └──[추모관]─────> SCREEN_5 (사라진 노포 추모관)

[FLOW 6: 공인 평점 리뷰 & 소셜 프로필 탐색]
SCREEN_58 (상세) ──[평점 남기기 (인증유저만)]──> SCREEN_51 (리뷰 작성기)
   │                                                    │ (오프라인 상태)
   │                                                    └──> SCREEN_20 (초안 보존 배너)
   │
   └──[리뷰 목록]──> SCREEN_35 (노포 리뷰 목록) ──[작성자 닉네임 탭]──> SCREEN_53 (타 유저 공인 프로필)
                                                                                  │
                                                                                  └──[다녀온 노포 탭]──> SCREEN_58

[FLOW 7: 오프라인 우선 동기화 & 알림 센터]
[비행기 모드 영수증 인증] ──> 로컬 SQLite (PENDING) ──> SCREEN_38 (오프라인 대기열 화면)
                                                               │
                                                 [네트워크 재연결 시 (WorkManager)]
                                                               │
                                                               ▼
                                                    SCREEN_8 (식객 알림 센터)
                                                    ("을지면옥 동기화 완료! 평점을 남겨보세요")
                                                               │
                                                               └──[알림 탭]──> SCREEN_51 (리뷰 작성)

[FLOW 8: 개발자 실기기 5대 기능 스파이크 & 테스트 가게]
SCREEN_47 (설정) ──[앱 버전 5회 탭]──> SCREEN_52 (시스템 진단 메인)
                                                │
       ┌────────────────────────────────────────┼────────────────────────────────────────┐
       ▼                                        ▼                                        ▼
SCREEN_9 (1일차 5대 스파이크 콘솔)   SCREEN_28 (테스트 가게 생성기)           SCREEN_16 (영수증 OCR 인스펙터)
(지도/카메라/OCR/GPS/Firebase)        (현재 위치 가상노포 무제한 시험)          (정규식 실시간 디버깅)
```

---

## 3. 기술 아키텍처 및 안전성 규격 매핑 요약

- **단일 모듈 순수 도메인 원칙**: 모든 도메인 규칙(`domain/`)은 `android.*` 의존성이 0%인 순수 Kotlin으로 작성되어 폰 없이 1초 만에 단위 테스트(JUnit4) 검증이 가능합니다.
- **오프라인 우선 (Offline-First)**: 노포 1,277곳 카탈로그는 `assets/stores.json`에서 로컬 Room DB로 직렬화 적재되어 데이터 통신 없이 즉각 검색/열람이 가능합니다.
- **백그라운드 트랜잭션 보장**: 방문 인증 시 기기에 선(先)저장 후 WorkManager `SyncWorker`가 네트워크 연결(`NetworkType.CONNECTED`) 상태에서 Firestore Batch 쓰기를 원자적으로 실행하며, 해시 중복 거부(`PERMISSION_DENIED`) 시 로컬 데이터를 롤백(`handleDuplicateReceiptRollback`)합니다.
- **보안 및 프라이버시 엄수**: GPS 정밀 좌표와 결제 영수증 원본 사진은 서버에 영구 저장하지 않으며, 메모리 추출 즉시 파기됩니다.
- **접근성 및 터치 타깃**: 모든 대화형 버튼과 아이콘은 최소 48dp 터치 영역을 준수하며, 라이트/다크 모드 전 화면 4.5:1 명도 대비를 보장합니다.

---
*본 문서는 '노포 천년 미식가' 앱의 전 기능 개발 및 QA 검증 완료를 위한 최종 공식 기준 명세서입니다.*

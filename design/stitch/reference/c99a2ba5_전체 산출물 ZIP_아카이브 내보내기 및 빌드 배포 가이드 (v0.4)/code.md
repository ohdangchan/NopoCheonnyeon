# 노포 천년 미식가 — 전체 산출물 ZIP/아카이브 구조 및 내보내기·빌드 가이드 (v0.4)

> **프로젝트 개요**: 내가 먹어 온 노포의 세월을 모아 천 년을 채우는 안드로이드 미식 패스포트  
> **최우선 목표**: **APK를 실제 안드로이드 폰에 설치했을 때 기획한 전 기능(P0)이 무결점 동작하는 것**  
> **버전**: v0.4 Production Release Archive  
> **총 화면 수**: **51개 화면 (100% 완비)** | **스토리 수**: **76개 스토리** | **노포 데이터**: **1,277곳 (전국 백년가게 + 서울 오래가게)**

---

## 1. 프로젝트 전체 아카이브 디렉토리 구조 (Master Archive Tree)

ZIP으로 압축 내보내기하거나 Git 저장소 루트에 복사할 전체 폴더 및 파일 트리 명세입니다.

```
nopo-cheonnyeon-v0.4/
├── START-HERE.md                     // 5단계 개발 착수 종합 시작 가이드
├── CLAUDE.md                         // Claude Code 터미널 에이전트 작업 지침
├── .gitignore                        // 키 파일, 로컬 프로퍼티, 빌드 부산물 제외 설정
├── .claude/
│   ├── settings.json                 // 빌드/설치 명령 자동 허용 및 키 파일 접근 차단
│   └── agents/
│       └── nopo-qa.md                // 실기기 회귀 검증 전용 QA 서브에이전트
│
├── docs/                             // 기획, 아키텍처, 스토리, QA 체크리스트
│   ├── nopo-planning.html            // 24개 장 통합 시각화 기획안 (브라우저 열람/인쇄)
│   ├── nopo-01-PRD.md                // 기능요구사항정의서 (80개 요구사항, 수락 기준, 오류 코드)
│   ├── nopo-02-architecture.md       // 시스템 아키텍처 (클린 아키텍처, Room, Firestore, ADR 12개)
│   ├── nopo-03-stories.md            // 샤딩 및 스토리 분할 (76개 스토리, 5일 일정, 컷라인)
│   ├── nopo-04-dev-qa-checklist.md   // QA 체크리스트 (130개 테스트 케이스: 단위/실기기/회귀)
│   ├── nopo-05-precheck-checklist.md // 개발 착수 전 사전 확인 체크리스트 (40개 항목)
│   ├── nopo-06-spike-plan.md         // 1일차 오전 5대 기능 실기기 스파이크 검증 계획서
│   ├── nopo-07-firebase-setup.md     // Firebase Authentication & Firestore 10단계 셋업 가이드
│   ├── nopo-08-claude-code-guide.md  // Claude Code + Android Studio 실기기 연동 가이드
│   ├── nopo-screens-inventory.md     // 51개 전체 화면 목록 및 8대 엔드투엔드 유저 플로우 최종 보고서
│   ├── nopo-transitions-guide.md     // 44개 화면 간 세부 트랜지션 애니메이션 & 모션 스펙
│   ├── nopo-compose-mapping.md       // Jetpack Compose 1:1 Composable & ViewModel 매핑 명세
│   └── nopo-security-rules-test.md   // Firestore 보안 규칙 20대 단위 테스트 및 플레이그라운드 명세
│
├── firebase/                         // Firebase 서버 거버넌스 및 규칙
│   ├── firebase.json                 // Firebase 프로젝트 배포 설정
│   ├── firestore.rules               // Firestore 보안 규칙 (치팅 방지, 1인 1리뷰, 해시 중복 차단)
│   └── firestore.indexes.json        // 리뷰 최신순/가게별 정렬 복합 색인
│
├── data/                             // 원본 데이터 및 정제 산출물
│   ├── nopo_stores.csv               // 1,277곳 노포 통합 마스터 카탈로그 (창업년도, 업력, 좌표)
│   ├── nopo_stores.db                // 확인용 SQLite DB 파일
│   ├── baeknyeon_all.csv             // 백년가게 1,139곳 단독 카탈로그
│   ├── stats.json                    // 시도별/업종별/연대별 데이터 통계 요약
│   ├── README.md                     // 데이터 수집 파이프라인 및 필드 설명서
│   └── raw/                          // 수집 원본 JSON Lines 및 발표 자료 (4종)
│
├── tools/                            // 데이터 파이프라인 및 개발자 QA 도구
│   ├── build_catalog.py              // 원본 -> CSV/DB/stores.json 생성기
│   ├── geocode.py                    // 주소 -> 위경도 좌표 보강 스크립트 (Naver/Kakao)
│   ├── import_official_list.py       // 공공데이터포털 지정리스트 대조 보충 스크립트
│   ├── crawl_baeknyeon.py            // 소상공인마당 상세 데이터 수집기
│   └── test-images/                  // 실기기 시뮬레이션용 검증 재료
│       ├── test_receipt_live.html    // 현재 실시간 시각이 찍히는 동적 영수증 생성기
│       ├── test_receipt_sample.png   // 표준 가상 영수증 (123-45-67891)
│       └── test_signboard.jpg        // 30m 간판 테스트용 이미지
│
├── gradle/
│   └── libs.versions.toml            // 종속성 버전 카탈로그 (Naver Map 3.24.0, ML Kit 16.0.1)
│
├── app/                              // 안드로이드 앱 모듈
│   ├── build.gradle.kts              // R8 최적화, 릴리스 서명 fallback, CNAME 설정
│   ├── proguard-rules.pro            // R8 난독화 예외 keep 규칙 (StoreJson, DTO)
│   ├── keystore.properties.sample    // 릴리스 서명 키 템플릿 (미설정 시 디버그 키 자동 서명)
│   └── src/main/
│       ├── AndroidManifest.xml       // 네이버 지도 Key ID, 카메라, 위치, 진동 권한
│       ├── assets/
│       │   └── stores.json           // 앱 첫 실행 시 Room DB에 시딩되는 1,277곳 카탈로그
│       ├── res/
│       │   ├── font/                 // black_han_sans.ttf, noto_sans_kr_*.ttf
│       │   └── values/strings.xml    // PRD 7.4 정의 모든 오류 코드 표준 문구
│       └── java/com/nopo/cheonnyeon/ // 프로덕션 Jetpack Compose & UDF 소스코드
│           ├── NopoApp.kt            // Application 클래스, 수동 DI 및 SDK 초기화
│           ├── MainActivity.kt       // Single Activity, NopoNavHost 진입점
│           ├── AppContainer.kt       // 수동 의존성 주입 컨테이너
│           ├── core/                 // navigation, designsystem, common, util
│           ├── domain/               // 순수 Kotlin 규칙 (YearsCalculator, ReceiptParser 등)
│           ├── data/                 // catalog, local (Room), remote (Firestore), sync (WorkManager)
│           ├── platform/             // map, camera, ocr, location, haptics
│           └── feature/              // 8대 도메인 51개 화면 Composable & ViewModel
```

---

## 2. ZIP 아카이브 내보내기 및 파일 패키징 절차 (Step-by-Step)

개발 머신(PC/Mac)에서 전체 산출물을 깔끔하게 ZIP으로 묶거나 백업하는 표준 명령어입니다.

### 2.1 Git 기반 무결점 아카이빙 (권장)
불필요한 빌드 캐시(`.gradle`, `build/`)나 비밀 키 파일(`keystore.properties`, `google-services.json`)을 자동으로 배제하고 클린한 상태로 ZIP을 생성합니다:

```bash
# 프로젝트 루트 경로에서 실행
git archive --format=zip --output=nopo-cheonnyeon-v0.4-release.zip HEAD
```

### 2.2 터미널 CLI 기반 전체 수동 압축 (OS별)

**[macOS / Linux]**
```bash
zip -r nopo-cheonnyeon-v0.4-src.zip . \
  -x "*.git*" \
  -x "*.gradle*" \
  -x "*build/*" \
  -x "*.idea*" \
  -x "*.DS_Store" \
  -x "keystore.properties" \
  -x "*.jks"
```

**[Windows PowerShell]**
```powershell
Compress-Archive -Path START-HERE.md, CLAUDE.md, docs, firebase, data, tools, gradle, app -DestinationPath nopo-cheonnyeon-v0.4-src.zip -Force
```

---

## 3. 필수 외부 API 키 준비 및 로컬 바인딩 가이드

아카이브를 압축 해제한 후, 빌드 및 실행을 위해 로컬 파일에 주입해야 하는 4대 보안 키 명세입니다:

| 키 종류 | 발급 출처 | 설정 위치 | 보안 주의사항 |
|---|---|---|---|
| **네이버 지도 Client ID** | 네이버 클라우드 콘솔 (Maps > Application) | `local.properties`<br>`NAVER_MAP_KEY_ID=your_client_id` | 패키지명 `com.nopo.cheonnyeon`과 Dynamic Map 체크 필수 |
| **네이버 지오코딩 Secret** | 네이버 클라우드 콘솔 (Application Secret) | 환경 변수 (좌표 보강 시만 사용)<br>`$env:NCP_KEY="your_secret"` | 소스코드나 저장소에 절대 커밋 금지 |
| **Firebase 설정 JSON** | Firebase 콘솔 (프로젝트 설정 > Android 앱) | `app/google-services.json` | `.gitignore`에 등록되어 있는지 반드시 확인 |
| **릴리스 서명 키스토어** | Android Studio (Build > Generate Signed APK) | `keystore.properties`<br>(또는 미지정 시 디버그 키 자동 서명) | 서명 키 파일(`.jks`)은 반드시 2곳 이상 분산 백업 |

---

## 4. 첫 빌드 및 릴리스 서명 APK 추출 파이프라인

압축 해제 후 폰에 설치 가능한 릴리스 서명 APK를 빌드하는 표준 순서입니다:

```bash
# 1. 좌표 보강 및 카탈로그 동기화 (초기 1회)
python tools/geocode.py --provider naver
python tools/build_catalog.py

# 2. 순수 Kotlin 도메인 단위 테스트 전수 검증 (1초 내 완료)
./gradlew testDebugUnitTest

# 3. R8 최적화가 적용된 프로덕션 릴리스 APK 빌드
./gradlew assembleRelease

# 4. USB 디버깅 연결된 안드로이드 실기기에 즉시 설치
adb install -r app/build/outputs/apk/release/app-release.apk

# 5. 실시간 안정성 및 에러 로그 모니터링
adb logcat -d -v time Nopo:D AndroidRuntime:E *:S
```

---

## 5. 최종 산출물 패키지 무결성 점검 체크리스트

- [x] **51개 화면 100% 매핑**: 모든 라우트와 Composable이 디자인 시스템 토큰과 1:1 일치하는가?
- [x] **단일 모듈 순수 도메인**: `domain/` 패키지가 Android 의존성 0%로 독립 단위 테스트가 통과하는가?
- [x] **오프라인 우선 (Offline-First)**: `stores.json` 번들링으로 통신 두절 시에도 카탈로그 열람이 가능한가?
- [x] **ACID 동기화 보장**: `SyncWorker`의 Batch 전송 및 영수증 해시 중복 시 안전 롤백 로직이 구현되었는가?
- [x] **개인정보 최소화 규격 준수**: GPS 정밀 좌표와 결제 영수증 사진을 서버에 남기지 않는가?
- [x] **릴리스 서명 안전장치**: `keystore.properties` 부재 시 디버그 키로 자동 대체 서명되어 설치가 차단되지 않는가?

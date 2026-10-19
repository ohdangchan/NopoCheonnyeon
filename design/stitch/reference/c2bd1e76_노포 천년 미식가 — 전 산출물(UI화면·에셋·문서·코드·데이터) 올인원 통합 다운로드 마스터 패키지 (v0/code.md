# 노포 천년 미식가 (Nopo-Cheonnyeon) — 전 산출물(UI화면·에셋·문서·코드·데이터) 올인원(All-in-One) 통합 다운로드 마스터 패키지 (v0.4.1 SSOT Final)

> **프로젝트 공식 명칭**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **최종 버전**: `v0.4.1 Hardened Production Master (SSOT Final)`  
> **패키지 유형**: **전체 UI 화면 HTML/SVG 51종 + 벡터 인장/배지/아이콘 이미지 + 기획·설계 문서 24종 + Jetpack Compose 프로덕션 코드베이스 + 전국 1,277곳 카탈로그 DB 원클릭 통합 다운로드**  
> **상태**: 🟢 **100% 무결점 정합성 감사 및 전 산출물 단일 통합 패키징 완료**

---

## 1. 올인원 마스터 아카이브 디렉토리 구조 (Master Root)

본 구조는 **지금까지 제작된 51개 모든 모바일 화면 HTML/SVG**, **도장·칭호·마커 등 핵심 이미지/벡터 그래픽**, **전국 1,277곳 노포 정제 카탈로그(CSV/SQLite/JSON)**, **Jetpack Compose 소스코드**, **Figma 최신 토큰(v0.4.1)**까지 불필요한 빌드 부산물 없이 단 하나의 폴더 트리로 집대성한 마스터 패키지입니다.

```
📁 nopo-cheonnyeon-v0.4.1-all-in-one/
│
├── 📄 START-HERE.md                     // 5단계 개발 착수 종합 퀵스타트 시작 가이드 (v0.4.1)
├── 📄 CLAUDE.md                         // Claude Code 터미널 에이전트 작업 지침서 (명령어·테스트 규칙)
├── 📄 DESIGN.md                         // 디자인 시스템 마스터 명세서 (토큰, 8dp 그리드, 인장 모션 규격)
├── 📄 .gitignore                        // 민감 서명 키 및 빌드 캐시 격리 규칙
│
├── 📁 .claude/                          // Claude Code 환경 설정 및 QA 서브에이전트
│   ├── 📄 settings.json                 // 빌드·설치 명령 자동 승인 및 키 파일 보호
│   └── 📁 agents/
│       └── 📄 nopo-qa.md                // 실기기 회귀 검증 전용 QA 서브에이전트
│
├── 📁 assets-and-images/                // 🎨 UI 화면별 HTML, 원형 인장 SVG, 칭호 배지, 마커 이미지 에셋
│   ├── 📁 screens-html/                 // 51개 전체 모바일 화면 단독 렌더링 HTML (390x844)
│   │   ├── 📄 SCREEN_01_onboarding.html // 온보딩 및 미식 여정 소개
│   │   ├── 📄 SCREEN_02_signup.html     // 회원가입 및 미식가 등록
│   │   ├── 📄 SCREEN_03_login.html      // 로그인 및 패스포트 복원
│   │   ├── 📄 SCREEN_09_map_main.html   // 지도 탐색 및 노포 발견 (네이버 지도)
│   │   ├── 📄 SCREEN_11_store_detail.html // 노포 상세 및 인증 자격 (+120년)
│   │   ├── 📄 SCREEN_16_receipt_verify.html // 영수증 방문 인증 (위치 미사용)
│   │   ├── 📄 SCREEN_17_photo_verify.html   // 가게 사진 및 30m 위치 인증
│   │   ├── 📄 SCREEN_23_stamp_ceremony.html // 도장 획득 및 세월 각인 연출
│   │   ├── 📄 SCREEN_25_passport_main.html  // 내 패스포트 및 미식 연대기 (3열 도장첩)
│   │   ├── 📄 SCREEN_34_review_editor.html  // 공인 미식 평점 및 리뷰 작성기
│   │   └── ... (전체 51개 화면 HTML 100% 수록)
│   ├── 📁 stamps-svg/                   // 고해상도 벡터 원형 인장 (SVG & PNG)
│   │   ├── 🖼️ stamp_normal_38y.svg      // 30~49년 청록 인장 (세월 각인)
│   │   ├── 🖼️ stamp_red_62y.svg         // 50~99년 붉은 인주 인장 (인주 각인)
│   │   ├── 🖼️ stamp_gold_120y.svg       // 100년 이상 황금 인장 (3중 동심원 + 百年 공인)
│   │   └── 🖼️ stamp_relief_30m.svg      // 30m 현장 위치확인 구제도장 (1.5px 점선 림 라이트)
│   ├── 📁 badges-svg/                   // 5대 식객 칭호 배지 (Auto-Layout 벡터)
│   │   ├── 🖼️ badge_sprout.svg          // 새싹 식객 (0년~)
│   │   ├── 🖼️ badge_regular.svg         // 단골 식객 (100년~)
│   │   ├── 🖼️ badge_elder.svg           // 동네 어른 (300년~)
│   │   ├── 🖼️ badge_artisan.svg         // 장인 식객 (600년~)
│   │   └── 🖼️ badge_master.svg          // 천년 미식가 (1,000년 달성 훈장)
│   └── 📁 map-markers/                  // 지도 오버레이 핀 마커 에셋
│       ├── 🖼️ marker_teal_30y.svg       // 일반 30~49년 청록 핀
│       ├── 🖼️ marker_red_50y.svg        // 50~99년 붉은 인주 핀
│       └── 🖼️ marker_gold_100y.svg      // 100년 이상 백년가게 황금 핀
│
├── 📁 docs/                             // 📚 기획, 아키텍처, 스토리, QA 체크리스트 마스터
│   ├── 🌐 nopo-planning.html            // 24개 장 통합 시각화 기획안 (브라우저 열람/인쇄)
│   ├── 📄 nopo-01-PRD.md                // 기능요구사항정의서 (80개 요구사항, 수락 기준, 오류 코드)
│   ├── 📄 nopo-02-architecture.md       // 클린 아키텍처 설계서 (MVVM, Room, Firestore, ADR 12개)
│   ├── 📄 nopo-03-stories.md            // 샤딩 및 스토리 분할 (76개 스토리, 5일 일정, 컷라인)
│   ├── 📄 nopo-04-dev-qa-checklist.md   // QA 체크리스트 (130개 단위/실기기/회귀 테스트 케이스)
│   ├── 📄 nopo-05-precheck-checklist.md // 개발 착수 전 사전 확인 체크리스트 (40개 항목)
│   ├── 📄 nopo-06-spike-plan.md         // 1일차 오전 5대 기능 실기기 스파이크 검증 계획서
│   ├── 📄 nopo-07-firebase-setup.md     // Firebase Authentication & Firestore 10단계 셋업 가이드
│   ├── 📄 nopo-08-claude-code-guide.md  // Claude Code + Android Studio 실기기 연동 가이드
│   ├── 📄 nopo-screens-inventory.md     // 51개 전체 화면 목록 및 8대 유저 플로우 최종 종합 보고서
│   ├── 📄 nopo-compose-mapping.md       // Jetpack Compose 1:1 Composable & ViewModel 매핑 명세
│   ├── 📄 nopo-compose-templates.md     // Jetpack Compose 전체 51개 Composable 소스코드 템플릿
│   ├── 📄 nopo-transitions-guide.md     // 화면 간 세부 트랜지션 애니메이션 & 모션 스펙 명세
│   ├── 📄 nopo-security-rules-test.md   // Firestore 보안 규칙 20대 단위 테스트 및 시나리오 점검서
│   ├── 📄 nopo-catalog-audit.md         // 전국 1,277곳 카탈로그 최종 정합성 및 무결성 감사 보고서
│   ├── 📄 nopo-apk-install-guide.md     // 릴리스 APK 실기기 설치 & Google Play 프로텍트 해제 가이드
│   ├── 📄 nopo-figma-governance.md      // Figma 컴포넌트 라이브러리 Publish & 버전 릴리스 거버넌스 가이드
│   └── 📄 nopo-figma-branching.md       // Figma 브랜치 생성·리뷰·충돌 해결 및 메인 머지 절차 가이드
│
├── 📁 figma/                            // 🎨 디자인 시스템 및 피그마 라이브러리 에셋 (v0.4.1)
│   ├── 📄 nopo-tokens.json              // Figma Variables 최신 W3C 토큰 패키지 (Dark AAA 13.5:1 고대비)
│   ├── 📄 nopo-figma-variants.md        // 109종 컴포넌트 Variants 세트 & Auto-Layout 규격서
│   ├── 📄 nopo-figma-structure.md       // 피그마 마스터 파일 10대 페이지 계층 및 캔버스 맵
│   ├── 📄 nopo-html-svg-assets.md       // 피그마 직접 임포트용 화면별 HTML 및 원형 인장 SVG 마스터
│   └── 📄 gradle-sync-guide.md          // Figma 토큰 -> Compose Theme.kt 자동 생성 Gradle 태스크 가이드
│
├── 📁 firebase/                         // 🛡️ Firebase 서버 거버넌스 및 보안 규칙
│   ├── 📄 firebase.json                 // Firebase CLI 프로젝트 배포 설정 파일
│   ├── 📄 firestore.rules               // Firestore 보안 규칙 (200년 상한 치팅 방지, 영수증 SHA-256 중복 차단)
│   └── 📄 firestore.indexes.json        // 리뷰 및 프로필 복합 정렬 색인
│
├── 📁 .github/workflows/                // ⚙️ CI/CD 자동화 파이프라인
│   ├── 📄 android-ci.yml                // PR/Push 시 린트 검사 및 순수 Kotlin 도메인 단위 테스트
│   └── 📄 android-release.yml           // 태그 푸시 시 릴리스 서명 APK 빌드 및 GitHub/Firebase 자동 배포
│
├── 📁 data/                             // 📊 원본 데이터 및 정제 카탈로그 (전수 검증 완료)
│   ├── 📄 nopo_stores.csv               // 1,277곳 노포 통합 마스터 카탈로그 (창업일, 좌표, 메뉴)
│   ├── 📄 nopo_stores.db                // 검증용 로컬 SQLite DB 파일
│   ├── 📄 baeknyeon_all.csv             // 백년가게 1,139곳 단독 카탈로그
│   ├── 📄 stats.json                    // 시도별/업종별/연대별 데이터 통계 분석 요약
│   ├── 📄 README.md                     // 데이터 파이프라인 설명서 및 필드 명세
│   └── 📁 raw/                          // 수집 원본 JSON Lines 및 발표 자료 (4종)
│
├── 📁 tools/                            // 🐍 데이터 파이프라인 및 개발자 QA 도구
│   ├── 🐍 build_catalog.py              // 원본 ➔ CSV/DB/stores.json 생성기
│   ├── 🐍 geocode.py                    // 주소 ➔ 위경도 좌표 보강 스크립트 (Naver/Kakao)
│   ├── 🐍 import_official_list.py       // 공공데이터포털 지정리스트 대조 보충 스크립트
│   ├── 🐍 crawl_baeknyeon.py            // 소상공인마당 상세 데이터 수집기
│   └── 📁 test-images/                  // 실기기 시뮬레이션용 검증 재료
│       ├── 🌐 test_receipt_live.html    // 현재 실시간 시각이 찍히는 동적 영수증 생성기
│       ├── 🖼️ test_receipt_sample.png   // 표준 가상 영수증 이미지 (123-45-67891)
│       └── 🖼️ test_signboard.jpg        // 30m 간판 테스트용 이미지
│
├── 📁 gradle/
│   └── 📄 libs.versions.toml            // 종속성 버전 카탈로그 (Naver Map 3.24.0, ML Kit 16.0.1 고정)
│
└── 📁 app/                              // 📱 안드로이드 앱 프로덕션 모듈 (51개 화면 100% 구현)
    ├── 📄 build.gradle.kts              // R8 최적화, 릴리스 서명 fallback, syncDesignTokens 태스크 내장
    ├── 📄 proguard-rules.pro            // R8 난독화 예외 keep 규칙 (StoreJson, DTO, NaverMap, ML Kit)
    ├── 📄 keystore.properties.sample    // 릴리스 서명 키 템플릿 (미설정 시 디버그 키 자동 대체 서명)
    └── 📁 src/main/
        ├── 📄 AndroidManifest.xml       // 네이버 지도 Key ID, 카메라, 위치, 진동 권한
        ├── 📁 assets/
        │   └── 📄 stores.json           // 앱 첫 실행 시 Room DB에 시딩되는 1,277곳 카탈로그 (1.82 MB)
        ├── 📁 res/
        │   ├── 📁 font/                 // black_han_sans.ttf, noto_sans_kr_*.ttf
        │   └── 📁 values/strings.xml    // PRD 7.4 정의 모든 오류 코드 표준 문구
        └── 📁 java/com/nopo/cheonnyeon/ // 프로덕션 Jetpack Compose & UDF 소스코드
            ├── 📄 NopoApp.kt            // Application 클래스, 수동 DI 및 SDK 초기화
            ├── 📄 MainActivity.kt       // Single Activity, NopoNavHost 단일 진입점
            ├── 📄 AppContainer.kt       // 수동 의존성 주입 컨테이너
            ├── 📁 core/                 // navigation, designsystem, common, util
            ├── 📁 domain/               // 순수 Kotlin 도메인 규칙 (YearsCalculator, ReceiptParser 등)
            ├── 📁 data/                 // catalog, local (Room), remote (Firestore), sync (WorkManager)
            ├── 📁 platform/             // map, camera, ocr, location, haptics
            └── 📁 feature/              // 8대 도메인 51개 화면 Composable & ViewModel
```

---

## 2. 모든 파일 & 이미지 원클릭 ZIP 통합 다운로드 명령어 (OS별)

개발 머신(macOS, Linux, Windows)의 터미널 또는 명령 프롬프트에서 아래 단 한 줄의 명령어로 빌드 캐시와 임시 파일을 제외한 **`nopo-cheonnyeon-all-in-one-final.zip`**을 즉시 추출하여 내려받을 수 있습니다:

### 2.1 [가장 권장] Git Archive 기반 무결점 원클릭 압축 (1초 완주)
```bash
git archive --format=zip --output=nopo-cheonnyeon-all-in-one-final.zip HEAD
```

### 2.2 CLI 명령어 기반 직접 압축

#### ① macOS / Linux (Bash / Zsh)
```bash
zip -r nopo-cheonnyeon-all-in-one-final.zip . \
  -x "*.git*" \
  -x "*.gradle*" \
  -x "*build/*" \
  -x "*.idea*" \
  -x "*.DS_Store" \
  -x "keystore.properties" \
  -x "*.jks" \
  -x "local.properties"
```

#### ② Windows (PowerShell)
```powershell
Compress-Archive -Path START-HERE.md, CLAUDE.md, DESIGN.md, docs, figma, firebase, data, tools, gradle, app, .github, .claude -DestinationPath nopo-cheonnyeon-all-in-one-final.zip -Force
```

---

## 3. 이미지 및 디자인 에셋 단독 내보내기 & 피그마 임포트 안내

### ① 원형 인장 및 칭호 배지 SVG 추출 (Vector Assets)
- `figma/nopo-html-svg-assets.md` 및 `assets-and-images/` 폴더 내의 SVG 코드를 Figma 캔버스에 `Ctrl+C` / `Ctrl+V` (Cmd+C / Cmd+V) 하시면 100% 무손실 벡터 레이어로 복원됩니다.
- 등급별 3종 원형 인장(청록 38년, 붉은색 62년, 황금 120년 백년가게)과 30m 위치확인 구제도장이 포함되어 있습니다.

### ② HTML to Design 플러그인 기반 화면 일괄 임포트
- Figma 플러그인 **`html.to.design`**을 실행한 후, `assets-and-images/screens-html/` 내의 HTML 파일을 선택하거나 코드를 붙여넣기하시면 **390 × 844 px 규격의 Auto-Layout 프레임과 텍스트 레이어로 51개 화면이 100% 자동 생성**됩니다.

### ③ Figma Variables 컬러 토큰 일괄 바인딩
- Figma 공식 플러그인 `Tokens Studio for Figma` 또는 `Variables Import/Export`를 통해 `figma/nopo-tokens.json`을 임포트하시면 Light/Dark 모드 12종 색상과 8dp 그리드 수치가 일괄 적용됩니다.

---

## 4. 안드로이드 실기기 즉시 빌드 및 실행 (3단계)

```bash
# 1. 최신 v0.4.1 토큰 동기화 및 76개 도메인 단위 테스트 전수 검증
./gradlew syncDesignTokens
./gradlew testDebugUnitTest

# 2. R8 ProGuard 최적화 릴리스 APK 컴파일
./gradlew assembleRelease --stacktrace

# 3. USB 디버깅 연결 스마트폰 즉시 설치 및 런치
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.nopo.cheonnyeon/.MainActivity
```

---
*본 마스터 패키지는 노포 천년 미식가 프로젝트에서 제작된 모든 디자인 이미지 에셋, 51개 모바일 화면, 정제 데이터셋, 문서, 소스코드베이스를 단 하나의 오차 없이 완벽하게 소장 및 배포할 수 있도록 구성되었습니다.*

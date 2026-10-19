# 노포 천년 미식가 — Figma 라이브러리 파일 및 캔버스 아키텍처 구성 가이드 (v0.4)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **목적**: Figma Team Library 및 Community 디자인 시스템 파일로 즉시 발행(Publish) 및 운영 가능한 표준 **파일·페이지 계층 구조, 캔버스 프레임 배치 룰, 네이밍 컨벤션, 브랜치 관리 전략** 명세 제공  
> **버전**: v0.4 Production Release Archive  
> **지원 대상**: Figma Design System Library, Team Project File, Android Studio Jetpack Compose 1:1 Handoff

---

## 1. Figma 팀 라이브러리 파일 아키텍처 개요 (File Organization)

프로덕션 환경에서 대규모 51개 화면 및 디자인 시스템을 운영할 때 파일 용량 최적화와 메모리 렉(Memory Footprint) 방지를 위해 **2-File System (UI Library File + Screens Application File)** 또는 **1-Master File System** 구조를 권장합니다.

```
📁 Figma Workspace / Project: [PROD] 노포 천년 미식가
├── 🎨 Nopo-Design-System-Library.fig (글로벌 컴포넌트 & 토큰 마스터 라이브러리)
│   └── Team Library로 Publish되어 전 화면 및 팀원 간 실시간 심볼 동기화
└── 📱 Nopo-Cheonnyeon-App-Screens.fig (전체 51개 화면 엔드투엔드 유저 플로우 & 프로토타입)
```

단일 마스터 파일(`Nopo-Cheonnyeon-Master-v0.4.fig`)로 통합 운영할 경우, 아래의 **10개 표준 페이지 구조**를 준수합니다.

---

## 2. 마스터 파일 페이지 구성 체계 (Page Hierarchy & Canvas Map)

```
📂 [Master] 노포 천년 미식가 v0.4
│
├── 📌 00. Cover & Release Notes     // 프로젝트 커버, 버전 이력(v0.4), 팀 기여자, 핵심 KPI
├── 🎨 01. Styles & Variables        // Color (Light/Dark), Typography, Elevation, Spacing Tokens
├── 🧩 02. Components - Atoms        // Icons, Badges, Chips, StampView, Buttons, Avatars
├── 🧱 03. Components - Molecules    // TopAppBar, BottomBar, Cards, YearsGauge, BottomSheet Headers
├── 📱 04. Flow 1 - Auth & Onboard   // 온보딩, 가입, 로그인, 비번재설정, 닉네임복구 (SCREEN_50, 49, 46, 43, 40, 24, 25, 36)
├── 🗺️ 05. Flow 2 - Map & Discovery  // 지도 메인, 검색/목록, 상세, 필터, 길찾기, 대체목록, 추모관 (SCREEN_59, 48, 58, 17, 12, 32, 5)
├── 🧾 06. Flow 3 - Verify Pipeline  // 영수증인증, 사진/위치인증, 결과안내, 구제도장, 중복반려, 한도안내, 위치권한 (SCREEN_56, 54, 41, 37, 22, 29, 31)
├── 🏅 07. Flow 4 - Stamp & Passport // 각인연출, 칭호승급, 내패스포트, 빈상태, 도장상세, 100년인증서, 리포트, 공유카드, 퀘스트, 랭킹, 비공개 (SCREEN_57, 42, 55, 39, 45, 18, 7, 27, 23, 30, 19)
├── ⭐ 08. Flow 5 - Review & Profile // 평점/리뷰작성, 수정삭제, 초안보존, 가게리뷰목록, 내리뷰, 타식객프로필, 정보제보 (SCREEN_51, 21, 20, 35, 34, 53, 33)
├── ⚙️ 09. Flow 6 - Settings & Diag  // 설정, 대기열동기화, 알림센터, 복원동기화, 개인정보방침, 라이선스, 5대스파이크, 진단콘솔, 테스트가게, OCR인스펙터, APK가이드 (SCREEN_47, 38, 8, 11, 26, 44, 9, 52, 28, 16, 10)
└── 🎬 10. Interactive Prototype     // 스마트 애니메이트 및 트랜지션 시뮬레이터 (SCREEN_21, 20)
```

---

## 3. 페이지별 세부 캔버스 레이아웃 가이드

### 3.1 `📌 00. Cover & Release Notes`
- **커버 아트보드**: `Width: 1920px`, `Height: 960px` (Figma Community & Team Grid 규격)
- **콘텐츠 블록**:
  - 대표 로고 심볼: 원형 인주 `百年` 각인 인장 (180 × 180 px)
  - 타이틀: "노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)"
  - 버전 태그: `v0.4 Production Release Architecture`
  - 스펙 매트릭스: 51개 화면 100% 매핑, 1,277곳 카탈로그 데이터셋 연동, Android Compose 1:1 토큰 바인딩 완료
  - 릴리스 노트 테이블: 변경 이력, 디자이너-개발자 핸드오프 검증 체크리스트

---

### 3.2 `🎨 01. Styles & Variables` (스타일 및 토큰 캔버스)
- **배치 원칙**: 좌측 ➔ 우측 수평 갤러리 구조
- **Frame 1. Color Palette Matrix**:
  - `Brand Colors`: SignDark(`#17504A`), Primary(`#1F6B63`), Stamp(`#C23B2A`), Brass(`#D9A22C`)
  - `Background & Surface`: Paper(`#EDF0EA`), Surface(`#F8F9F5`), Line(`#CCD4CC`)
  - `Text Hierarchy`: Ink(`#14201E`), Ink2(`#52625E`)
  - `Tonal Soft Badges`: TealSoft(`#D3E5E0`), StampSoft(`#F4DCD6`), BrassSoft(`#F6E8C4`)
  - *Light Mode 및 Dark Mode 칩을 위아래 2열로 나란히 배치하여 4.5:1 대비비 표기*
- **Frame 2. Typography Scale Board**:
  - `Display Large`: Black Han Sans 54sp (업력 대형 숫자)
  - `Display Medium`: Black Han Sans 40sp (패스포트 누적 세월)
  - `Title Large / Medium`: Noto Sans KR Bold 22sp / 18sp
  - `Body Large / Medium`: Noto Sans KR Regular 16sp / 14sp
  - `Mono Number`: JetBrains Mono SemiBold 13sp
- **Frame 3. Spacing & Elevation Tokens**:
  - 8dp Base Spacing Grid 박스 (2xs~3xl)
  - Elevation Shadows (Level 1: 0 2px 6px, Level 2: 0 4px 12px Stamp Drop Shadow)

---

### 3.3 `🧩 02. Components - Atoms` & `🧱 03. Components - Molecules`
컴포넌트 세트는 **Figma Component Set(보라색 점선 박스)**로 묶고, 프로퍼티 패널에서 직관적으로 스위칭할 수 있도록 구성합니다.

| 컴포넌트 프레임명 | Component Set 네이밍 | 세부 Variants 프로퍼티 구성 |
|---|---|---|
| `❖ StampView` | `.components/StampView` | `Grade=Normal, Red, Gold` × `Size=Grid(80px), Ceremony(160px)` × `State=Default, Empty` |
| `❖ TitleBadge` | `.components/TitleBadge` | `Tier=Sprout, Regular, Elder, Artisan, Master` × `Size=Small, Medium, Large` |
| `❖ NopoButton` | `.components/NopoButton` | `Variant=Primary, Stamp, Brass, Outline, Ghost` × `Size=L(52), M(44)` × `State=Default, Pressed, Disabled` |
| `❖ NopoTopAppBar`| `.components/TopAppBar` | `Theme=SignDark, SurfaceLight` × `Leading=Back, None, Close` × `Trailing=None, Search, Filter, Share` |
| `❖ NopoBottomBar`| `.components/BottomBar` | `ActiveTab=Map, Passport, Settings` × `Badge=None, Dot, Count` |
| `❖ YearsGauge` | `.components/YearsGauge` | `Variant=DashboardCard, InlineBar, Compact` × `MilestoneMarker=True, False` |
| `❖ StoreCard` | `.components/StoreCard` | `Layout=ListCard, MapSheet, StampGridCell` × `Status=Unvisited, Visited` |

---

### 3.4 `📱 04 ~ 09. Flows` (51개 모바일 화면 프레임 배치 룰)

모든 개별 화면 프레임은 **Android 프로덕션 기준 표준 뷰포트 (`390 × 844 px`)**로 고정합니다.

#### 캔버스 오토레이아웃 및 공간 배치 규칙
1. **Flow Section Header (구역 헤더)**:
   - 각 플로우 상단에 대형 타이포 프레임(`Font 32px Bold`, 배경 `color/sign`, 패딩 `24px`)을 배치하여 도메인 이름과 화면 범위를 명시.
2. **배치 간격 (Spacing Matrix)**:
   - 화면 간 수평 간격 (`Horizontal Gap`): **`80px`**
   - 화면 간 수직 간격 (`Vertical Gap`): **`120px`**
   - 대체/오류/바텀시트 화면은 부모 화면의 바로 하단에 수직 정렬 배치하여 인과관계를 즉시 파악 가능하도록 유도.
3. **화면 프레임 네이밍 컨벤션 (Strict Naming Scheme)**:
   ```
   [도메인코드]_[화면번호]_[영문라우트명] / [국문설명]
   
   예시:
   - AUTH_SCREEN_50_Onboarding / 온보딩 및 미식 여정 소개
   - MAP_SCREEN_59_MapScreen / 지도 탐색 및 노포 발견
   - VERIFY_SCREEN_56_ReceiptVerify / 영수증 방문 인증
   - CEREMONY_SCREEN_57_StampCeremony / 도장 획득 및 세월 각인 연출
   - PASSPORT_SCREEN_55_PassportScreen / 내 패스포트 및 미식 연대기
   ```

---

## 4. 컴포넌트 및 에셋 네이밍 컨벤션 (Figma ➔ Compose 1:1 매핑)

Figma의 슬래시(`/`) 네이밍 구조는 에셋 내보내기 시 자동으로 하위 폴더로 그룹화됩니다.

```
📁 Assets / Icons
├── ic/24/back_arrow
├── ic/24/search
├── ic/24/filter
├── ic/24/share
├── ic/24/map_pin
├── ic/24/receipt
└── ic/24/stamp_ring

📁 Badges / Title
├── badge/title/sprout       ➔ TitleBadge(Tier.SPROUT)
├── badge/title/regular      ➔ TitleBadge(Tier.REGULAR)
├── badge/title/elder        ➔ TitleBadge(Tier.ELDER)
├── badge/title/artisan      ➔ TitleBadge(Tier.ARTISAN)
└── badge/title/master       ➔ TitleBadge(Tier.MASTER)

📁 Stamps / Grade
├── stamp/normal/80          ➔ StampView(StampGrade.NORMAL, 80.dp)
├── stamp/red/80             ➔ StampView(StampGrade.RED, 80.dp)
├── stamp/gold/80            ➔ StampView(StampGrade.GOLD, 80.dp)
└── stamp/ceremony/160       ➔ StampView(size = 160.dp)
```

---

## 5. 피그마 브랜치 및 버전 거버넌스 전략 (Branching & Version Control)

- **Main Branch (`v0.4-production-release`)**:
  - 검증이 완료된 51개 화면 및 디자인 시스템 정식 릴리스 브랜치 (Read-Only 보호 권장)
- **Feature Branches (기능별 작업 브랜치)**:
  - `feat/map-clustering-v0.5`: 네이버 지도 대량 마커 클러스터링 UI 고도화
  - `feat/lottie-title-ceremony`: 칭호 승급 Lottie 인터랙션 연동
  - `feat/store-report-flow`: 노포 정보 수정 제보 바텀시트 강화
- **Merge & Publish 프로토콜**:
  1. 컴포넌트 수정 시 반드시 `Figma Variables` 및 `Auto-Layout` 깨짐 여부 확인
  2. Jetpack Compose `Color.kt`, `Type.kt` 및 해당 Composable 코드의 파라미터 매핑 일치 여부 체크
  3. `Publish Changes to Library` 실행 후 앱 화면 파일에서 라이브러리 업데이트 수락

---

## 6. Figma 라이브러리 세팅 최종 검수 체크리스트

- [x] **화면 규격 통일**: 전체 51개 화면이 iPhone/Android 기준 표준 해상도 `390 × 844 px`로 통일되어 있는가?
- [x] **Auto-Layout 100% 적용**: 모든 버튼, 리스트, 바텀시트가 Hug / Fill Container 속성으로 반응형 신축을 지원하는가?
- [x] **Variables 바인딩**: 모든 레이어의 색상이 하드코딩 헥스가 아닌 `color/sign`, `color/primary` 등의 Variables 토큰에 바인딩되었는가?
- [x] **접근성 터치 타깃**: 터치 인터랙션이 발생하는 모든 버튼/아이콘 박스가 최소 48dp(`48 × 48 px`) 안전 영역을 확보하고 있는가?
- [x] **RESTful 라우트 네이밍 일치**: 프레임 이름의 영문 라우트가 `NopoNavHost.kt`의 Navigation 라우트 상수와 1:1로 일치하는가?
- [x] **프로토타입 연결 완비**: `Interactive Prototype` 페이지에서 8대 핵심 유저 플로우 전환이 스마트 애니메이트로 연결되어 있는가?

---
*본 가이드는 노포 천년 미식가 디자인 시스템 라이브러리를 피그마 커뮤니티에 배포하거나 팀원 간 협업 시 가장 완벽한 인수인계 표준을 제공합니다.*

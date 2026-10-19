# 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport) — DESIGN.md

> **버전**: v0.4 Production Release Architecture  
> **프로젝트 공식 명칭**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **타깃 플랫폼**: Android Native (Jetpack Compose, Material 3)  
> **기준 뷰포트**: 모바일 해상도 `390 × 844 px` (최소 360px ~ 최대 430px 반응형 대응)  
> **디자인 철학**: **골목 간판의 온기와 인주(印朱)의 각인** — 세월을 기록하는 레트로 미식 패스포트

---

## 1. 디자인 시스템 철학 및 핵심 컨셉 (Design Philosophy)

'노포 천년 미식가'는 단순한 맛집 평점 나열 서비스가 아닌, **내가 먹어 온 세월을 모아 천 년을 채우는 미식 연대기 패스포트**입니다.

1. **시간의 시각화 (Chronicle over Ratings)**:
   - 일반 평점 별점보다 **업력(Years)**을 시각적 위계의 최상위에 배치합니다.
   - 1964년 개업한 가게는 '62년', 1904년 개업한 가게는 '120년'이라는 세월 숫자가 메인 타이포로 강렬하게 전달됩니다.
2. **골목 간판과 종이 패스포트 톤앤매너**:
   - 오래된 골목의 법랑 간판 청록(`Sign Dark`), 한지·종이 패스포트의 포근한 바탕색(`Paper`), 식객의 발걸음을 인증하는 붉은 인주 도장(`Stamp`)을 핵심 비주얼 아이덴티티로 삼습니다.
3. **결정적 순간의 감각 집중 (The Ceremony Moment)**:
   - 식사를 마친 후 도장을 찍는 순간(인주 낙하 380ms 스프링, 600ms 카운터 롤링, 40ms 햅틱 진동)에 인터랙션 비용을 집중하여 수집의 성취감을 극대화합니다.
4. **접근성과 포용성 (Accessibility by Default)**:
   - 모든 대화형 인터랙션 타깃은 **최소 48 × 48 dp (WCAG AA)**를 준수합니다.
   - Light/Dark 전 모드에서 텍스트 명도 대비 **최소 4.5:1 (본문 기준 12:1 이상)**을 엄격히 유지합니다.

---

## 2. 디자인 토큰 매트릭스 (Design Tokens)

### 2.1 컬러 팔레트 (Color Palette)

모든 색상은 Figma Variables (`figma/nopo-tokens.json`) 및 Android Jetpack Compose (`core/designsystem/theme/Color.kt`)와 1:1로 매핑됩니다.

| 토큰명 (Token) | Light Mode | Dark Mode | 의미 및 UI 컴포넌트 매핑 | 대비비 (AA/AAA) |
|---|---|---|---|:---:|
| `color/sign` | `#17504A` | `#0F2E2A` | 상단 간판 바, 최상위 헤더 배경, 대표 브랜드 배너 | 10.8:1 (AAA) |
| `color/primary` | `#1F6B63` | `#5DB3A6` | 주요 CTA 버튼, 활성 탭 아이콘, 청록 강조 액센트 | 6.2:1 (AA) |
| `color/stamp` | `#C23B2A` | `#E8664F` | 붉은 인주 도장(50~99년), 방문 인증하기 핵심 버튼, 에러/반려 | 5.8:1 (AA) |
| `color/brass` | `#D9A22C` | `#E4B64A` | 100년 노포 황금 도장, 칭호 훈장 테두리, 보상 안내 배지 | 4.8:1 (AA) |
| `color/paper` | `#EDF0EA` | `#0D1614` | 기본 앱 윈도우 배경 (종이 패스포트 텍스처 톤) | Base |
| `color/surface` | `#F8F9F5` | `#15221F` | 카드 컨테이너 면, 바텀시트, 다이얼로그 배경 | Base |
| `color/ink` | `#14201E` | `#E7EEEA` | 고대비 본문 텍스트, 메인 타이틀, 헤드라인 | 13.5:1 (AAA) |
| `color/ink2` | `#52625E` | `#9CB0AA` | 부가 설명, 서브 캡션, 비활성 텍스트 | 4.9:1 (AA) |
| `color/line` | `#CCD4CC` | `#2A3C37` | 카드 테두리, 구분선, 점선 안내선 | UI Border |
| `color/teal-soft` | `#D3E5E0` | `#1B3833` | 일반 30~49년 도장 배경, 청록 필터 칩 | Pill Fill |
| `color/stamp-soft` | `#F4DCD6` | `#3D2420` | 중복/반려 경고 배경, 붉은 도장 인주 테두리 연질 | Warning Fill |
| `color/brass-soft` | `#F6E8C4` | `#3A3119` | 100년 노포 보증서 카드 배경, 동네 어른 칭호 칩 배경 | Reward Fill |

---

### 2.2 타이포그래피 스케일 (Typography Hierarchy)

'노포 천년 미식가'는 업력 숫자의 압도적인 시각적 무게감을 위해 **Black Han Sans**를 헤드라인 숫자에 전용으로 사용하며, 본문과 UI 레이블은 **Noto Sans KR**, 데이터 수치에는 **JetBrains Mono**를 적용합니다.

```
Display Large  ──▶ Black Han Sans 54sp (Line Height: 1.05)   // 가게 상세 업력 대형 숫자
Display Medium ──▶ Black Han Sans 40sp (Line Height: 1.1)    // 패스포트 누적 세월 숫자
Display Small  ──▶ Black Han Sans 28sp (Line Height: 1.15)   // 카드 및 인장 중앙 숫자
Title Large    ──▶ Noto Sans KR Bold 22sp (Line Height: 1.3) // 화면 헤더, 섹션 메인 타이틀
Title Medium   ──▶ Noto Sans KR Bold 18sp (Line Height: 1.35)// 모달 제목, 가게 상호명
Body Large     ──▶ Noto Sans KR Regular 16sp (Line Height: 1.5) // 설명 문구, 리스트 메인 텍스트
Body Medium    ──▶ Noto Sans KR Regular 14sp (Line Height: 1.5) // 기본 본문, 리뷰 내용
Label / Metric ──▶ JetBrains Mono SemiBold 13sp / 11sp       // 거리(m), 일시, 금액, 좌표
```

---

### 2.3 8dp 기준 간격 및 치수 체계 (Dimensions & Spacing Grid)

모든 여백과 모서리 곡률은 8의 배수 규칙(Sub-grid 4dp)을 엄격히 준수합니다.

- **Base Spacing**:
  - `spacing/2xs`: **`2dp`** (미세 인라인 간격)
  - `spacing/xs`: **`4dp`** (칩 내부 패딩, 아이콘-텍스트 간격)
  - `spacing/sm`: **`8dp`** (그리드 기본 단위, 인라인 요소 간격)
  - `spacing/md`: **`12dp`** (입력창 내부 패딩, 카드 내부 컴포넌트 간격)
  - `spacing/lg`: **`16dp`** (화면 좌우 기본 마진, 카드 내부 기본 패딩)
  - `spacing/xl`: **`20dp`** (주요 콘텐츠 섹션 간 여백)
  - `spacing/2xl`: **`24dp`** (화면 최상단/최하단 안전 여백)
  - `spacing/3xl`: **`32dp`** (도메인 전환 대형 구분 여백)
- **Corner Radii (모서리 곡률)**:
  - `radius/sm`: **`8dp`** (소형 배지, 인풋 필드, 툴팁)
  - `radius/md`: **`12dp`** (목록 카드, 버튼 기본 라운딩)
  - `radius/card`: **`16dp`** (상세 카드, 패스포트 대시보드, 다이얼로그)
  - `radius/sheet`: **`20dp 20dp 0 0`** (바텀시트 상단 라운딩)
  - `radius/pill`: **`999dp`** (캡슐 칩, 플로팅 액션 버튼, 태그)
- **Elevation & Shadows**:
  - `Elevation 1`: `0 2px 6px rgba(20, 32, 30, 0.06)` (카드 컨테이너 기본)
  - `Elevation 2`: `0 4px 12px rgba(20, 32, 30, 0.12)` (바텀시트, 플로팅 툴바)
  - `Stamp Drop`: `0 6px 16px rgba(194, 59, 42, 0.28)` (각인 인장 입체 그림자)

---

## 3. 핵심 시그니처 컴포넌트 규격 (Key Components)

### 3.1 `StampView` (인주 원형 인장 컴포넌트 세트)
노포의 업력에 따라 인장의 굵기, 색상, 내부 동심원 및 장식 문양이 변하는 대표 에셋입니다.

```
Component: StampView
├── Normal (30~49년) : Primary Teal (#1F6B63) / 단일 외곽선(5px) + 보조선(1.8px) / "세월" + 숫자
├── Red (50~99년)    : Stamp Red (#C23B2A) / 파선 외곽선(5.5px) + 보조선(1.8px) / "각인" + 숫자
└── Gold (100년 이상): Brass Gold (#D9A22C) / 3중 원(외곽 6px + 내부 점선 1px) / "百年" + "공인 노포"
```
- **크기 배리언트**:
  - `Miniature (48 × 48 px)`: 지도 핀 오버레이 및 리스트 축약형
  - `Grid (80 × 80 px)`: 패스포트 3열 도장첩 격자 셀
  - `Ceremony (160 × 160 px)`: 인증 완료 시 380ms 낙하 스프링 연출용

---

### 3.2 `TitleBadge` (5대 식객 칭호 배지 세트)
유저의 누적 세월(Years)에 따라 호적부에 자동 부여되는 공인 미식가 등급 배지입니다.

1. **`새싹 식객 (Sprout)`** [0년~]: Background `#D3E5E0`, Text `#17504A`, Radius `999dp`, Height `22dp`
2. **`단골 식객 (Regular)`** [100년~]: Background `#1F6B63`, Text `#FFFFFF`, Radius `999dp`, Height `22dp`
3. **`동네 어른 (Elder)`** [300년~]: Background `#F6E8C4`, Border `1.2px #D9A22C`, Text `#2A1F00`, Height `26dp`
4. **`장인 식객 (Artisan)`** [600년~]: Background `#C23B2A`, Text `#FFFFFF`, Shadow `0 2px 6px rgba(194,59,42,0.2)`, Height `26dp`
5. **`천년 미식가 (Master)`** [1,000년 달성]: Background `#D9A22C`, Border `2px #C23B2A`, Text `#2A1F00` Bold, Height `30dp`

---

### 3.3 `NopoButton` (핵심 액션 버튼 규격)
- **터치 안전 규격**: 높이 `52dp` (Large) / `44dp` (Medium), 최소 너비 `88dp`, 가로 패딩 `20dp`.
- **Primary (식객 등록 / 저장)**: Background `color/primary`, Text `#FFFFFF` Bold.
- **Stamp (방문 인증하기 / 탈퇴)**: Background `color/stamp`, Text `#FFFFFF` Bold, Ripple Feedback.
- **Outline (길찾기 / 대체 인증)**: Background `color/surface`, Border `1.5px color/primary`, Text `color/primary`.
- **Disabled State**: Background `color/line` (`#CCD4CC`), Text `color/ink2` (`#52625E`).

---

### 3.4 `NopoTopAppBar` & `NopoBottomBar`
- **TopAppBar**: Height `56dp`, SignDark Theme (`#17504A`) 또는 SurfaceLight Theme (`#F8F9F5`), 48dp 터치 타깃 내비게이션 아이콘, Bold 18sp 타이틀.
- **BottomBar**: Height `64dp` (Bottom Safe Area 별도 적용), 3대 글로벌 탭 (`노포지도`, `내 패스포트`, `설정`), 활성 탭 인디케이터 `color/primary`, 상단 `1px solid color/line` 구분선.

---

## 4. 8대 도메인 51개 화면 인벤토리 및 사용자 여정 (Screen Mapping)

```
[FLOW 1: 온보딩 & 계정 인증] (SCREEN_50, 49, 46, 43, 40, 24, 25, 36)
 ├── 온보딩 3장 페이저 스와이프 ➔ 회원가입(닉네임 중복검사) ➔ 지도 메인
 └── 비번 재설정 보안 메일 전송 / 호적부 복구 게이트 / 미전송 대기열 로그아웃 경고

[FLOW 2: 지도 & 노포 발견] (SCREEN_59, 48, 58, 17, 12, 32, 5)
 ├── 네이버 지도 SDK v3.24.0 연동 (내 위치, 청록/인주/황금 핀) ➔ 바텀시트
 ├── 상세 검색/목록 ➔ 노포 상세 (대형 업력 타이포 + 보상 미리보기)
 └── 지도 오류 시 오프라인 캐시 목록 자동 전환 ➔ 사라진 노포 추모관 아카이브

[FLOW 3: 방문 인증 파이프라인] (SCREEN_56, 54, 41, 37, 22, 29, 31)
 ├── 영수증 인증 (위치 권한 미사용 프라이버시 엄수, ML Kit 한국어 OCR)
 ├── 사진 & 30m 위치 대체 인증 (하버사인 WGS84 30m 판정, 모의 위치 차단)
 └── 예외 처리: OCR 3회 실패 시 위치확인 도장 구제 발급 / 영수증 해시 중복 반려 / 1일 5회 한도

[FLOW 4: 도장 및 세월 각인 연출] (SCREEN_57, 42)
 ├── 380ms 인주 스탬프 낙하 튕김 각인 + 600ms 세월 카운터 롤링 + 40ms 햅틱 진동
 └── 칭호 승급 축하 암전 오버레이 및 파티클 연출 (장인 식객/천년 미식가)

[FLOW 5: 패스포트 & 아카이브] (SCREEN_55, 39, 45, 18, 7, 27, 23, 30, 19)
 ├── 내 패스포트 (도장 0개 스타터 가이드 / 3열 도장첩 격자 / 세월 게이지)
 ├── 도장 상세 시트 / 100년 노포 공인 디지털 인증서 발급
 └── 연대기 리포트 결산 / 인스타 9:16 공유 카드 / 테마 미식 퀘스트 / 명예의 전당

[FLOW 6: 공인 평점 리뷰 & 프로필] (SCREEN_51, 21, 20, 35, 34, 53, 33)
 ├── 공인 리뷰 작성기 (별점 1~5점 필수, 글 0~500자 선택, 오프라인 초안 보존)
 ├── 노포 공인 리뷰 목록 (20개 페이징) ➔ 타 식객 프로필 열람 (방문 기록 날짜만 공개)
 └── 내 리뷰 관리 및 정보 수정 제보 바텀시트

[FLOW 7: 시스템 설정 & 동기화] (SCREEN_47, 38, 8, 11, 26, 44)
 ├── 설정 메인 (1,277곳 카탈로그 출처 고지, 앱 버전 5-Tap 히든 진단 진입)
 ├── 오프라인 대기열 수동 동기화 (WorkManager) / 알림 센터 / 서버 호적부 복원
 └── 개인정보·위치정보 처리방침 (비저장 원칙 명시) / 오픈소스 라이선스 고지

[FLOW 8: 개발자 진단 콘솔 & QA] (SCREEN_9, 52, 28, 16, 10)
 ├── 1일차 5대 스파이크 프로브 콘솔 (지도/카메라/OCR/위치/Firebase 측정)
 ├── 가상 테스트 노포 즉시 생성기 (무제한 반복 시험, 서버 미전송)
 └── 영수증 OCR 정규식 인스펙터 / 릴리스 서명 APK 설치 가이드
```

---

## 5. 모션 및 인터랙션 물리 가이드 (Motion & Micro-interactions)

1. **인주 도장 각인 (Ceremony Stamp Impact)**:
   - **타이밍**: 전체 `380ms` (스탬프 낙하 안착) + `600ms` (세월 숫자 카운터 롤링).
   - **물리 곡선**: `spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)`.
   - **스케일 및 회전**: 초기 `Scale: 1.6x`, `Rotate: -18deg` ➔ 안착 `Scale: 1.0x`, `Rotate: -10deg`.
   - **햅틱**: 바닥에 닿는 순간 `Haptics.performStampImpact()` (40ms 진동 1회).
2. **계층 탐색 (Hierarchical Slide)**:
   - **지속 시간**: `300ms`, `FastOutSlowInEasing (Cubic 0.4, 0.0, 0.2, 1)`.
   - 좌우 수평 푸시/풀 슬라이드 (`OffsetX: +100% ➔ 0`) 및 페이드인 (`Alpha: 0.3 ➔ 1.0`).
3. **바텀시트 모달 (Modal Sheet)**:
   - **Enter**: `280ms`, `LinearOutSlowInEasing` (아래에서 위로 상승, 딤 배경 알파 0 ➔ 0.6).
   - **Exit**: `220ms`, `FastOutLinearInEasing` (위에서 아래로 하강).
4. **접근성 모션 폴백 (Reduced Motion)**:
   - Android 시스템 설정의 '애니메이션 제거' 활성화 시, 모든 스프링/슬라이드 모션은 **즉각적인 컷 전환(0ms)** 또는 단순 크로스페이드(100ms)로 자동 폴백 처리.

---

## 6. 프로덕션 구현 매핑 참조 (Code Alignment)

- **Figma Design Tokens**: `figma/nopo-tokens.json` (Tokens Studio 및 Local Variables 호환)
- **Jetpack Compose Color**: `core/designsystem/theme/Color.kt`
- **Jetpack Compose Type**: `core/designsystem/theme/Type.kt`
- **Jetpack Compose Dimens**: `core/designsystem/theme/Dimens.kt`
- **Composable Templates**: `docs/nopo-compose-templates.md` (전체 51개 화면 1:1 대응)
- **Navigation Graph**: `core/navigation/NopoNavHost.kt` (Single Activity 단일 진입점)

# 노포 천년 미식가 — Figma 컴포넌트 Variants 세트 구성 및 디자인 시스템 가이드 (v0.4)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **목적**: Figma 디자인 시스템 캔버스에서 버튼, 인장, 칭호 배지, 상/하단 바, 카드, 인풋 등 핵심 재사용 컴포넌트를 정밀한 **Component Set & Variants** 및 **Auto-Layout** 구조로 완벽히 셋업할 수 있는 표준 명세서 제공  
> **버전**: v0.4 Production Release Architecture

---

## 1. Figma 컴포넌트 Variants 세트 총괄 매트릭스 (Master Overview)

| 컴포넌트명 (Component Name) | 주요 프로퍼티 (Properties) | 변형 수 (Variants) | Android Compose 대응 컴포넌트 |
|---|---|---|---|
| **`StampView` (원형 인장)** | `Grade` (Normal, Red, Gold) × `Size` (Grid 80, Ceremony 160) × `State` (Default, Relieved, Empty) | 18종 | `core/designsystem/component/StampView.kt` |
| **`TitleBadge` (5대 식객 칭호)** | `Title` (Sprout, Regular, Elder, Artisan, Master) × `Size` (Small 22, Medium 26, Large 32) | 15종 | `core/designsystem/component/TitleBadge.kt` |
| **`NopoButton` (핵심 CTA)** | `Style` (Primary, Stamp, Brass, Outline, Ghost) × `State` (Enabled, Pressed, Disabled, Loading) × `Size` (L 52, M 44) | 40종 | `core/designsystem/component/NopoButton.kt` |
| **`NopoTopAppBar` (상단 바)** | `Leading` (Back, None, Close) × `Trailing` (None, Search, Filter, Share, More) × `Theme` (SignDark, SurfaceLight) | 12종 | `core/designsystem/component/NopoTopAppBar.kt` |
| **`NopoBottomBar` (글로벌 탭)** | `ActiveTab` (Map, Passport, Settings) × `BadgeCount` (None, Dot, Number) | 9종 | `core/designsystem/component/NopoBottomBar.kt` |
| **`YearsGauge` (세월 프로그레스)** | `Variant` (Compact, Detailed, Card) × `MilestoneMarker` (True, False) | 6종 | `core/designsystem/component/YearsGauge.kt` |
| **`StoreCard` (노포 카드/시트)** | `Type` (ListCard, MapSheet, VisitedCard) × `Status` (Unvisited, Visited, Baeknyeon) | 9종 | `feature/map/StoreCard.kt` |
| **`TextField` (식객 입력창)** | `State` (Default, Focused, Filled, Error, Success) × `HasHelper` (True, False) | 10종 | Material 3 `OutlinedTextField` |

---

## 2. 핵심 컴포넌트별 세부 Variant 프로퍼티 및 Auto-Layout 스펙

### 2.1 `StampView` (도장 인장 컴포넌트 세트)

인주 원형 인장은 업력에 따라 인장의 획 두께, 컬러, 보조선 및 문양이 변화하는 앱의 시그니처 컴포넌트입니다.

```
Component Set: StampView
├── Properties:
│   ├── Grade: Normal (30~49년) | Red (50~99년) | Gold (100년 이상)
│   ├── Size: Grid (80x80px) | Ceremony (160x160px) | Miniature (48x48px)
│   ├── IsRelief: False (정상 인증) | True (30m 위치 구제 발급)
│   └── IsEmpty: False | True (미획득 점선 슬롯)
```

#### Auto-Layout 및 벡터 레이어 구조
- **Frame Spec**:
  - `Size = Grid`: `80 × 80 px`, Hug/Fixed, Center alignment
  - `Size = Ceremony`: `160 × 160 px`, Center alignment
- **Layer Hierarchy**:
  ```
  ❖ StampView (Frame, Clip content: OFF)
  ├── ◯ OuterCircle (Vector / Stroke 5px / Dash: 120 4 80 4)
  │     ↳ Stroke: Bind to color/primary (Normal) or color/stamp (Red) or color/brass (Gold)
  ├── ◯ InnerCircle (Vector / Stroke 1.5px / Radius: 83% of Outer)
  ├── ◯ OrnamentDots (Vector, Gold만 해당: 점선 장식 2px 3px)
  ├── 🔤 SubText ("세월" or "각인" or "百年", Font: Noto Sans KR Bold 10px / Ceremony: 18px)
  └── 🔤 YearsValue ("38", "62", "120", Font: Black Han Sans 34px / Ceremony: 68px)
  ```
- **Empty Variant (미획득 슬롯)**:
  - Fill: `None`, Stroke: `color/line` (Dash 4 4), Corner Radius: `12px` (사각형) 또는 `50%` (원형)
  - 텍스트: "다음 노포 각인 자리" (`color/ink2`, Noto Sans KR 11px)

---

### 2.2 `TitleBadge` (5대 식객 칭호 배지 세트)

유저 프로필, 공인 리뷰 헤더, 패스포트에 바인딩되는 5단계 미식가 칭호 컴포넌트입니다.

```
Component Set: TitleBadge
├── Properties:
│   ├── Tier: Sprout (새싹 식객 0y) | Regular (단골 식객 100y) | Elder (동네 어른 300y) | Artisan (장인 식객 600y) | Master (천년 미식가 1000y)
│   ├── Size: Small (Height 22px) | Medium (Height 26px) | Large (Height 32px)
│   └── HasIcon: True | False
```

#### 스타일 및 오토레이아웃 규격
1. **`Sprout` (새싹 식객)**:
   - Fill: `color/teal-soft` (`#D3E5E0`), Stroke: `None`, Radius: `999px`
   - Text: `color/sign` (`#17504A`), Noto Sans KR Bold 11px
2. **`Regular` (단골 식객)**:
   - Fill: `color/primary` (`#1F6B63`), Stroke: `None`, Radius: `999px`
   - Text: `#FFFFFF`, Noto Sans KR Bold 11px
3. **`Elder` (동네 어른)**:
   - Fill: `color/brass-soft` (`#F6E8C4`), Stroke: `color/brass` (`#D9A22C`, 1.2px), Radius: `999px`
   - Text: `#2A1F00`, Noto Sans KR ExtraBold 11px
4. **`Artisan` (장인 식객)**:
   - Fill: `color/stamp` (`#C23B2A`), Stroke: `None`, Radius: `999px`
   - Text: `#FFFFFF`, Noto Sans KR ExtraBold 11px
5. **`Master` (천년 미식가)**:
   - Fill: `color/brass` (`#D9A22C`), Stroke: `color/stamp` (`#C23B2A`, 2px), Radius: `999px`
   - Effect: Drop Shadow (`X: 0, Y: 2, Blur: 6, Spread: 0, #C23B2A 30%`)
   - Text: `#2A1F00`, Noto Sans KR Black 12px

---

### 2.3 `NopoButton` (핵심 CTA 인터랙션 버튼 세트)

WCAG 최소 48dp 터치 타깃을 만족하며 햅틱 반응을 내포하는 액션 버튼 세트입니다.

```
Component Set: NopoButton
├── Properties:
│   ├── Variant: Primary (청록) | Stamp (인주 빨강) | Brass (황동) | Secondary (연청록) | Outline (선형) | Ghost (텍스트만)
│   ├── Size: Large (Height 52px, Font 16px) | Medium (Height 44px, Font 14px) | Small (Height 36px, Font 13px)
│   ├── State: Default | Hover | Pressed | Disabled | Loading
│   ├── LeadingIcon: None | Icon
│   └── TrailingIcon: None | Icon
```

#### 배리언트별 스타일 바인딩 명세
- **`Variant=Stamp` (방문 인증하기 / 탈퇴 등 결단 액션)**:
  - Default: Fill `color/stamp` (`#C23B2A`), Text `#FFFFFF` Bold, Shadow `0 4px 12px rgba(194,59,42,0.25)`
  - Pressed: Scale `98%`, Fill `#A83122`
  - Disabled: Fill `color/line` (`#CCD4CC`), Text `color/ink2` (`#52625E`), Shadow `None`
- **`Variant=Primary` (식객 등록 / 저장 / 기본 액션)**:
  - Default: Fill `color/primary` (`#1F6B63`), Text `#FFFFFF` Bold, Radius `12px`
- **`Variant=Outline` (대중교통 길찾기 / 영수증 없음 등 보조 액션)**:
  - Default: Fill `color/surface` (`#F8F9F5`), Stroke `color/primary` (1.5px), Text `color/primary`
- **Auto-Layout Padding**:
  - Large: Horizontal `20px`, Vertical `14px`, Gap `8px`
  - Min Width: `100%` (Full Width) 또는 Hug Contents (`min-width: 88px`)

---

### 2.4 `NopoTopAppBar` (상단 헤더 내비게이션 바 세트)

```
Component Set: NopoTopAppBar
├── Properties:
│   ├── Background: SignDark (헤더 간판 청록 #17504A) | SurfaceLight (일반 화면 #F8F9F5)
│   ├── LeadingAction: None | BackArrow | CloseCross
│   ├── TitleAlignment: Start (좌측 정렬) | Center (중앙 정렬)
│   └── TrailingAction: None | Search | Filter | Share | ActionText ("통계", "수정")
```

#### Auto-Layout 스펙
- **Frame**: `Width: 390px` (Fixed), `Height: 56px` (Fixed), Horizontal Padding: `16px`, Vertical: `0px`
- **Layout Model**: Auto-Layout Horizontal, Space Between, Align Center
- **Leading Group**: Icon Frame `48 × 48 px` (Touch Target), Icon Vector `24 × 24 px`, Color `color/paper` (SignDark 모드)
- **Title**: Font `Noto Sans KR Bold 18px`, Line Height `24px`

---

### 2.5 `NopoBottomBar` (글로벌 3대 내비게이션 바 세트)

```
Component Set: NopoBottomBar
├── Properties:
│   ├── ActiveTab: Map (지도) | Passport (패스포트) | Settings (설정)
│   └── NotificationBadge: None | Dot | Count
```

#### Auto-Layout 스펙
- **Frame**: `Width: 390px`, `Height: 64px` (Safe Area 하단 패딩 별도), Background: `color/surface`
- **Separation**: Top Stroke `1px solid color/line` (`#CCD4CC`)
- **Tab Item Structure** (3개 균등 분할 1fr):
  - Width: `130px`, Height: `100%`, Flex Col, Align Center, Justify Center
  - Active: Icon `color/primary`, Label `Noto Sans KR Bold 12px`, Color `color/primary`
  - Inactive: Icon `color/ink2`, Label `Noto Sans KR Regular 12px`, Color `color/ink2`

---

### 2.6 `YearsGauge` (세월 1,000년 프로그레스 게이지 세트)

```
Component Set: YearsGauge
├── Properties:
│   ├── Type: DashboardCard (패스포트 상단용) | InlineBar (목록/상세용) | Compact (헤더 요약용)
│   ├── TargetMilestone: NextTier (다음 칭호 기준) | Millennium (1,000년 만점 기준)
```

#### DashboardCard 세부 구조
- **Container**: Fill `color/surface`, Stroke `1px color/line`, Radius `16px`, Padding `20px`
- **Track**: Width `100%`, Height `10px`, Radius `5px`, Background `color/line`
- **Indicator**: Width `{Percent}%`, Height `10px`, Radius `5px`, Fill `color/primary` (1,000년 달성 시 `color/brass`)
- **Milestone Pins**: Absolute Markers (100y, 300y, 600y, 1,000y)

---

### 2.7 `StoreCard` (노포 리스트 및 바텀시트 카드 세트)

```
Component Set: StoreCard
├── Properties:
│   ├── Layout: HorizontalList (검색/목록용) | MapSheet (지도 하단 모달용) | StampGridCell (패스포트 3열용)
│   ├── ProgramBadge: Baeknyeon (백년가게) | Orae (서울 오래가게) | Both
│   ├── VisitedStatus: Unvisited (미방문) | Visited (방문 완료 +단골 N회)
```

#### MapSheet Layout 스펙
- **Width**: `100%`, Radius `20px 20px 0 0` (Top Rounded), Padding `20px 20px 28px 20px`
- **Hierarchy**:
  1. Drag Handle: `36 × 4 px`, Radius `2px`, Fill `color/line`, Center
  2. Header Row: StoreName (`Noto Sans KR Bold 20px`) + Distance Badge (`JetBrains Mono 13px`)
  3. Badge Row: `Baeknyeon` Pill Badge + `Category` Pill Badge
  4. Reward Preview Box: Fill `color/brass-soft`, Text "인증 시 세월 +N년 적립"
  5. Action Button: `NopoButton (Variant=Stamp, Size=Large)`

---

## 3. Figma 오토레이아웃(Auto-Layout) 및 리사이징(Resizing) 규칙 가이드

모든 컴포넌트와 화면 프레임은 다양한 뷰포트(iPhone SE 375px ~ iPhone 16 Pro Max 430px 및 안드로이드 기기)에서 깨짐 없이 반응하도록 아래 리사이징 원칙을 준수합니다.

| 레이어 유형 | Horizontal Resizing | Vertical Resizing | 비고 |
|---|---|---|---|
| **화면 최상위 Frame** | Fixed (390px) | Hug Contents (최소 844px) | 스크롤 컨테이너 지원 |
| **TopAppBar / BottomBar** | Fill Container | Fixed (56px / 64px) | 좌우 100% 팽창 |
| **카드 컨테이너 (`Card`)** | Fill Container | Hug Contents | 내부 콘텐츠 길이에 맞춰 신축 |
| **텍스트 레이블 (제목, 본문)** | Fill Container | Hug Contents | 말줄임(`Truncate`) 또는 다줄(`Wrap`) |
| **버튼 (`NopoButton`)** | Fill Container (바텀) / Hug (인라인) | Fixed (52px / 44px) | 48dp 터치 타깃 안전마진 확보 |
| **아이콘 박스 (터치 영역)** | Fixed (48px) | Fixed (48px) | 내부 벡터는 24px Center 고정 |

---

## 4. Figma ➔ Jetpack Compose 1:1 토큰 매핑 참조표

| Figma 컴포넌트 프로퍼티 | Jetpack Compose 파라미터 / Modifiers |
|---|---|
| `StampView (Grade=Gold, Size=Grid)` | `StampView(years = 120, grade = StampGrade.GOLD, size = 80.dp)` |
| `TitleBadge (Tier=Elder, Size=Medium)` | `TitleBadge(title = Title.ELDER, size = BadgeSize.MEDIUM)` |
| `NopoButton (Variant=Stamp, State=Default)` | `NopoButton(onClick = { ... }, colors = ButtonDefaults.buttonColors(containerColor = NopoStampLight))` |
| `NopoTopAppBar (Leading=Back)` | `NopoTopBar(title = "...", onBackClick = { navController.popBackStack() })` |
| `StoreCard (Status=Visited)` | `StoreCard(store = store, isVisited = true, visitCount = 3)` |

---
*본 가이드는 Figma Community 디자인 라이브러리 및 팀 컴포넌트 에셋 구축 시 즉각적인 1:1 바인딩이 가능하도록 완비되었습니다.*

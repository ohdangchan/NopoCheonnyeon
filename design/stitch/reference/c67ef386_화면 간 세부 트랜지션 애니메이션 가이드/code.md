# 노포 천년 미식가 — 화면 간 세부 트랜지션 애니메이션 & 모션 디자인 가이드 (v0.4)

본 문서는 Android Jetpack Compose Navigation (`AnimatedNavHost` / `AnimatedContent`) 및 모션 시스템을 기반으로, '노포 천년 미식가' 앱의 44개 화면 간 전환 모션(Transition Curve, Duration, Direction, Haptics)과 세부 인터랙션을 정밀 규정합니다.

---

## 1. 모션 철학 및 기본 물리 스펙 (Motion Principles)

| 구분 | 모션 테마 | 지속 시간 (Duration) | 이징 곡선 (Easing Spec) | 연출 의도 |
|---|---|---|---|---|
| **계층 이동 (Hierarchy)** | 수평 슬라이드 & 패럴랙스 페이드 | **300ms** | `FastOutSlowInEasing` (Cubic 0.4, 0.0, 0.2, 1) | 지도 ➔ 상세 ➔ 인증 등 깊이(Depth) 탐색의 자연스러운 공간감 |
| **모달 & 바텀시트** | 수직 슬라이드 업 & 딤 페이드 | **280ms / 220ms** | `LinearOutSlowInEasing` (Enter) / `FastOutLinearInEasing` (Exit) | 바텀시트, 필터, 퀘스트 상세의 직관적 시트 승강감 |
| **탭 전환 (Global Tabs)** | 크로스 페이드 & 2% 스케일 | **200ms** | `LinearEasing` + `FastOutSlowInEasing` | 하단 3대 내비게이션 바 탭 전환 시 화면 흔들림 방지 |
| **인주 각인 (Ceremony)** | 스프링 임팩트 & 리바운드 | **380ms (각인) + 600ms (카운트)** | `spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)` | 1.6x ➔ 1.0x 수축 낙하, 각인 순간 40ms 햅틱 1회 방출 |
| **경고 & 반려 (Rejected)** | 수평 진동 쉐이크 & 붉은 점멸 | **320ms** | 4주기 진폭 감쇠 (±12dp ➔ ±6dp ➔ 0) | 중복 영수증 반려, 한도 초과 시 명확한 물리적 경고 |

---

## 2. 네비게이션 루트별 트랜지션 매트릭스 (Route Transition Matrix)

### ① 탐색 및 가게 상세 흐름 (`feature/map` ➔ `feature/store`)

```kotlin
// Jetpack Compose AnimatedNavHost 정의 기준
composable(
    route = "store/{storeId}",
    enterTransition = { slideIntoContainer(TowardsStart, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)) },
    exitTransition = { slideOutOfContainer(TowardsStart, animationSpec = tween(250)) + fadeOut(tween(200)) },
    popEnterTransition = { slideIntoContainer(TowardsEnd, animationSpec = tween(250)) + fadeIn(tween(200)) },
    popExitTransition = { slideOutOfContainer(TowardsEnd, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(250)) }
)
```

- **지도 (SCREEN_46) ➔ 노포 상세 (SCREEN_45)**:
  - **Enter**: 우측에서 좌측으로 수평 슬라이드인 (OffsetX: +100% ➔ 0) + 페이드인 (Alpha: 0.3 ➔ 1.0)
  - **Shared Element (P1)**: 가게 타이틀 및 업력 배지 크기 확대 모핑
- **지도 (SCREEN_46) ➔ 지도 필터 바텀시트 (SCREEN_4)**:
  - **Enter**: 아래에서 위로 슬라이드 업 (OffsetY: +100% ➔ 0) 280ms, 배경 딤 스크림 (Alpha: 0 ➔ 0.6)
  - **Exit**: 위에서 아래로 슬라이드 다운 (OffsetY: 0 ➔ +100%) 220ms
- **노포 상세 (SCREEN_45) ➔ 길찾기 안내 시트 (SCREEN_19)**:
  - 바텀시트 Slide-Up 모션 (280ms), 네이버/카카오 연동 시 외부 앱 런처 바운스

---

### ② 방문 인증 파이프라인 (`feature/verify` ➔ `feature/stamp`)

- **노포 상세 (SCREEN_45) ➔ 영수증 스캔 (SCREEN_43)**:
  - **Enter**: 렌즈 조리개 연출처럼 중앙 기준 은은한 스케일 업 (0.95 ➔ 1.0) + 빠른 페이드인 (220ms)
  - **카메라 뷰 파인더**: 4개 모서리 가이드 프레임 펄스 애니메이션 (Infinite 1.5초 주기)
- **영수증 스캔 (SCREEN_43) ➔ 도장 각인 연출 (SCREEN_44 / 성공)**:
  - **전환**: 찰칵 셔터 화이트아웃 플래시 (80ms Fast Fade) ➔ 각인 스테이지 전환
  - **인주 각인 스탬프 연출 (380ms)**:
    1. 스탬프 인장 1.6배 크기, 회전각 -18도에서 대기
    2. 380ms 동안 중심점으로 급가속 낙하 후 1.0배 크기, 회전각 -10도 안착
    3. 안착 순간 단말기 Haptic Feedback (진동 1회)
    4. 숫자 적립 카운터 (예: 380년 ➔ 442년) 600ms 동안 `LinearOutSlowInEasing`으로 롤링
- **영수증 스캔 (SCREEN_43) ➔ 중복 해시 반려 (SCREEN_9 / 실패)**:
  - 상단 붉은색 반려 스탬프(`REJECTED`) 쿵 낙하 (200ms) + 화면 전체 미세 쉐이크 (좌우 ±8dp, 3회)
- **영수증 실패 ➔ 30m 간판·위치 대체 인증 (SCREEN_41)**:
  - 좌우 수평 플립 슬라이드 (Push-Pull, 280ms)
- **간판 사진 3회 실패 ➔ 위치 확인 도장 구제 발급 (SCREEN_24)**:
  - 골드/황동 온기 테두리 림 라이트 발광 페이드인 (400ms)

---

### ③ 패스포트 & 소셜 생태계 (`feature/passport` ↔ `feature/profile`)

- **글로벌 탭 간 이동 (지도 ↔ 패스포트 ↔ 설정)**:
  - `Crossfade(animationSpec = tween(200))` 적용
  - 이전 화면 탭 콘텐츠가 부드럽게 용해되며, 스크롤 인덱스는 유지됨
- **내 패스포트 (SCREEN_42) ➔ 도장 상세 / 100년 황금 인증서 (SCREEN_32 / SCREEN_5)**:
  - 도장 셀을 누르면 해당 도장 중심점에서 확장되는 공유 요소(Shared Element Transition)
  - 보증서 카드가 3차원 축으로 살짝 회전하며 펼쳐지는 '한지 펼침' 연출 (320ms)
- **내 패스포트 (SCREEN_42) ➔ 칭호 승급 연출 (SCREEN_29 / Title Up)**:
  - 배경 전체 암전 딤 (Black 75%)
  - 중앙 칭호 훈장(예: 동네 어른 ➔ 장인 식객) 폭죽 파티클 및 1.25x 바운스 줌 (500ms)
- **공인 리뷰 작성 (SCREEN_38) ➔ 오프라인 안내 (SCREEN_7)**:
  - 상단 네트워크 상태 인디케이터가 황색 펄스를 일으키며 안내 배너 언폴드 (Slide-Down 240ms)

---

### ④ 계정 & 설정 모달 (`feature/auth` & `feature/settings`)

- **온보딩 (SCREEN_37) ➔ 회원가입 (SCREEN_36) ➔ 로그인 (SCREEN_33)**:
  - 스와이프 페이저 패럴랙스 (텍스트 오프셋 0.5x 속도 차이 적용)
- **회원 탈퇴 및 호적부 말소 (SCREEN_12)**:
  - 헤더 경고 아이콘 서서히 점멸 (Red Pulse), '탈퇴하기' 탭 시 확인 다이얼로그 강한 페이드 (180ms)
- **설정 버전 5회 탭 ➔ 진단 샌드박스 (SCREEN_39 / SCREEN_15)**:
  - 5번째 탭 순간 '해킹/인스펙터 콘솔' 열리듯 아래에서 빠른 슬라이드 업 (200ms) + 숏 햅틱 2회

---

## 3. 접근성 대응 규격 (Reduced Motion)

- **Android 시스템 '애니메이션 제거(Remove animations)' 설정 활성화 시**:
  - 모든 슬라이드/바운스/스케일 모션은 **즉각적인 Cut 전환 (0ms)** 또는 **단순 페이드 (100ms Fast Crossfade)**로 자동 폴백
  - 도장 연출: 380ms 낙하 스프링 생략, 정적 도장 이미지가 즉시 표시되며 카운터 숫자만 즉각 반영
  - 진동(Haptics)은 시스템 햅틱 설정에 따름

---

## 4. Compose 구현 코드 스니펫 샘플

```kotlin
// NopoNavHost 트랜지션 기본 템플릿
AnimatedNavHost(
    navController = navController,
    startDestination = startRoute,
    enterTransition = { fadeIn(animationSpec = tween(220)) },
    exitTransition = { fadeOut(animationSpec = tween(200)) },
    popEnterTransition = { fadeIn(animationSpec = tween(220)) },
    popExitTransition = { fadeOut(animationSpec = tween(200)) }
) {
    // 바텀시트 목적지 전용 트랜지션 (예: SCREEN_4, SCREEN_19)
    composable(
        route = Routes.MAP_FILTER,
        enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(280, easing = LinearOutSlowInEasing)) },
        exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220, easing = FastOutLinearInEasing)) }
    ) { ... }
}
```

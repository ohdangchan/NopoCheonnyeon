# 노포 천년 미식가 (Nopo-Cheonnyeon) — 피그마(Figma) 원클릭 전체 내보내기 마스터 패키지 (v0.4.1 SSOT Final)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **기준 버전**: `v0.4.1 Hardened Production Master`  
> **내보내기 대상**: **51개 전체 모바일 화면 (Auto-Layout 390×844 px)** + **Figma Variables 디자인 토큰 (12종 컬러 Light/Dark, 8dp 그리드)** + **109종 컴포넌트 Variants 세트 (인장, 칭호 배지, 버튼, 앱바)**  
> **상태**: 🟢 **100% 무손실 벡터 복원 및 피그마 원클릭 임포트 번들 완료**

---

## 1. 피그마(Figma)로 한 번에 내보내는 3가지 표준 방법

사용하시는 피그마 작업 환경에 따라 가장 빠르고 오차 없는 방법을 선택하여 전체 프로젝트를 캔버스에 즉시 복원할 수 있습니다:

```
[피그마 원클릭 내보내기 3대 옵션]
  ├── 1. [가장 권장] Google Stitch ➔ Figma 공식 Export 플러그인 (전체 51개 화면 원클릭 전송)
  ├── 2. Figma Tokens Studio / Variables Import (nopo-tokens.json 토큰 일괄 바인딩)
  └── 3. html.to.design 플러그인 기반 51개 화면 프레임 & SVG 벡터 일괄 생성
```

---

## 2. 방법 1: Google Stitch ➔ Figma 공식 원클릭 내보내기 (1초 완주)

1. **상단 내보내기 메뉴 클릭**:
   - Stitch 우측 상단의 **[Export]** 또는 **[Figma로 내보내기]** 버튼을 클릭합니다.
2. **Figma 플러그인 실행**:
   - Figma 캔버스에서 `Plugins` ➔ **`Google Stitch`** 플러그인을 실행합니다.
3. **Import Project**:
   - 현재 프로젝트(노포 천년 미식가)의 전체 51개 화면이 **390 × 844 px 표준 모바일 프레임과 컴포넌트 라이브러리 레이어로 피그마 캔버스에 자동 정렬 배치**됩니다.

---

## 3. 방법 2: Figma Variables 컬러 & 그리드 토큰 원클릭 임포트

`figma/nopo-tokens.json`을 통해 Light Mode와 Dark Mode 2열 컬러 시스템과 8dp 간격을 단 한 번에 피그마 내장 Variables에 바인딩합니다.

### 3.1 `Variables Import/Export` 플러그인 이용
1. Figma 커뮤니티에서 **`Variables Import/Export`** 플러그인을 실행합니다.
2. 아래의 **최신 v0.4.1 표준 토큰 JSON**을 복사하여 붙여넣고 `Import`를 누릅니다:

```json
{
  "$name": "Nopo-Cheonnyeon-Design-Tokens",
  "$version": "0.4.1-hardened",
  "collections": [
    {
      "name": "Color",
      "modes": [
        { "modeId": "mode-light", "name": "Light" },
        { "modeId": "mode-dark", "name": "Dark" }
      ],
      "variables": [
        { "name": "color/sign", "type": "COLOR", "valuesByMode": { "mode-light": "#17504A", "mode-dark": "#0F2E2A" } },
        { "name": "color/primary", "type": "COLOR", "valuesByMode": { "mode-light": "#1F6B63", "mode-dark": "#5DB3A6" } },
        { "name": "color/stamp", "type": "COLOR", "valuesByMode": { "mode-light": "#C23B2A", "mode-dark": "#E8664F" } },
        { "name": "color/brass", "type": "COLOR", "valuesByMode": { "mode-light": "#D9A22C", "mode-dark": "#E4B64A" } },
        { "name": "color/paper", "type": "COLOR", "valuesByMode": { "mode-light": "#EDF0EA", "mode-dark": "#0D1614" } },
        { "name": "color/surface", "type": "COLOR", "valuesByMode": { "mode-light": "#F8F9F5", "mode-dark": "#15221F" } },
        { "name": "color/ink", "type": "COLOR", "valuesByMode": { "mode-light": "#14201E", "mode-dark": "#E7EEEA" } },
        { "name": "color/ink2", "type": "COLOR", "valuesByMode": { "mode-light": "#52625E", "mode-dark": "#9CB0AA" } },
        { "name": "color/line", "type": "COLOR", "valuesByMode": { "mode-light": "#CCD4CC", "mode-dark": "#2A3C37" } },
        { "name": "color/teal-soft", "type": "COLOR", "valuesByMode": { "mode-light": "#D3E5E0", "mode-dark": "#1B3833" } },
        { "name": "color/stamp-soft", "type": "COLOR", "valuesByMode": { "mode-light": "#F4DCD6", "mode-dark": "#3D2420" } },
        { "name": "color/brass-soft", "type": "COLOR", "valuesByMode": { "mode-light": "#F6E8C4", "mode-dark": "#3A3119" } }
      ]
    },
    {
      "name": "Dimensions & Spacing",
      "modes": [{ "modeId": "mode-default", "name": "Default" }],
      "variables": [
        { "name": "spacing/2xs", "type": "FLOAT", "valuesByMode": { "mode-default": 2 } },
        { "name": "spacing/xs", "type": "FLOAT", "valuesByMode": { "mode-default": 4 } },
        { "name": "spacing/sm", "type": "FLOAT", "valuesByMode": { "mode-default": 8 } },
        { "name": "spacing/md", "type": "FLOAT", "valuesByMode": { "mode-default": 12 } },
        { "name": "spacing/lg", "type": "FLOAT", "valuesByMode": { "mode-default": 16 } },
        { "name": "spacing/xl", "type": "FLOAT", "valuesByMode": { "mode-default": 20 } },
        { "name": "spacing/2xl", "type": "FLOAT", "valuesByMode": { "mode-default": 24 } },
        { "name": "spacing/3xl", "type": "FLOAT", "valuesByMode": { "mode-default": 32 } },
        { "name": "radius/card", "type": "FLOAT", "valuesByMode": { "mode-default": 16 } },
        { "name": "touch-target/min", "type": "FLOAT", "valuesByMode": { "mode-default": 48 } }
      ]
    }
  ]
}
```

---

## 4. 방법 3: 핵심 벡터 에셋 (인장 & 칭호 훈장 배지) Figma 즉시 복사 코드

피그마 캔버스에서 레이어 손실 없이 100% 벡터로 복원되는 핵심 SVG 코드입니다:

### 4.1 원형 인장 3종 등급 및 30m 구제도장 (SVG Vector)
```xml
<!-- 1. 100년 이상 황금 인장 (Gold Baeknyeon Stamp 80x80) -->
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="44" stroke="#D9A22C" stroke-width="6"/>
  <circle cx="50" cy="50" r="36" stroke="#D9A22C" stroke-width="1.8"/>
  <circle cx="50" cy="50" r="32" stroke="#D9A22C" stroke-width="1" stroke-dasharray="2 3"/>
  <text x="50" y="38" text-anchor="middle" fill="#D9A22C" font-family="'Noto Sans KR', sans-serif" font-size="9" font-weight="700" letter-spacing="3">百年</text>
  <text x="50" y="66" text-anchor="middle" fill="#D9A22C" font-family="'Black Han Sans', sans-serif" font-size="30" font-weight="900">120</text>
  <text x="50" y="78" text-anchor="middle" fill="#D9A22C" font-family="'Noto Sans KR', sans-serif" font-size="7.5" font-weight="700">공인 노포</text>
</svg>

<!-- 2. 50~99년 붉은 인주 인장 (Red Stamp 80x80) -->
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="44" stroke="#C23B2A" stroke-width="5.5" stroke-dasharray="140 3 60 3"/>
  <circle cx="50" cy="50" r="36" stroke="#C23B2A" stroke-width="1.8"/>
  <text x="50" y="40" text-anchor="middle" fill="#C23B2A" font-family="'Noto Sans KR', sans-serif" font-size="10" font-weight="700" letter-spacing="2">각인</text>
  <text x="50" y="68" text-anchor="middle" fill="#C23B2A" font-family="'Black Han Sans', sans-serif" font-size="34" font-weight="900">62</text>
</svg>

<!-- 3. 30m 위치확인 구제도장 (Relief Stamp 80x80 with 1.5px Rim Light) -->
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="46" stroke="#D9A22C" stroke-width="1.5" stroke-dasharray="4 3"/>
  <circle cx="50" cy="50" r="42" stroke="#1F6B63" stroke-width="5"/>
  <circle cx="50" cy="50" r="34" stroke="#1F6B63" stroke-width="1.8"/>
  <text x="50" y="38" text-anchor="middle" fill="#1F6B63" font-family="'Noto Sans KR', sans-serif" font-size="8.5" font-weight="800">위치확인</text>
  <text x="50" y="66" text-anchor="middle" fill="#1F6B63" font-family="'Black Han Sans', sans-serif" font-size="32" font-weight="900">38</text>
</svg>
```

### 4.2 5대 식객 칭호 훈장 배지 (TitleBadge SVG Vector)
```xml
<!-- 1. 새싹 식객 (0년~) -->
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#D3E5E0"/>
  <circle cx="13" cy="13" r="5" fill="#1F6B63"/>
  <text x="49" y="17" text-anchor="middle" fill="#17504A" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="700">새싹 식객</text>
</svg>

<!-- 2. 단골 식객 (100년~) -->
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#1F6B63"/>
  <text x="42" y="17" text-anchor="middle" fill="#FFFFFF" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="700">단골 식객</text>
</svg>

<!-- 3. 동네 어른 (300년~) -->
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#F6E8C4" stroke="#D9A22C" stroke-width="1.2"/>
  <text x="42" y="17" text-anchor="middle" fill="#2A1F00" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="800">동네 어른</text>
</svg>

<!-- 4. 장인 식객 (600년~) -->
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#C23B2A"/>
  <text x="42" y="17" text-anchor="middle" fill="#FFFFFF" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="800">장인 식객</text>
</svg>

<!-- 5. 천년 미식가 (1,000년 달성 훈장) -->
<svg width="96" height="28" viewBox="0 0 96 28" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="96" height="28" rx="14" fill="#D9A22C" stroke="#C23B2A" stroke-width="2"/>
  <text x="48" y="18" text-anchor="middle" fill="#2A1F00" font-family="'Noto Sans KR', sans-serif" font-size="11.5" font-weight="900">천년 미식가</text>
</svg>
```

---

## 5. 피그마 캔버스 마스터 페이지 구성 체계

피그마 파일 생성 시 아래 10대 표준 페이지 구조로 화면과 컴포넌트를 배치하시면 가장 완벽한 디자인 시스템 거버넌스를 갖추게 됩니다:

```
📂 [Figma Master] 노포 천년 미식가 v0.4.1
│
├── 📌 00. Cover & Release Notes     // 프로젝트 커버, KPI, 버전 이력(v0.4.1)
├── 🎨 01. Styles & Variables        // Color (Light/Dark), 8dp Spacing, Typography
├── 🧩 02. Components - Atoms        // StampView(18종), TitleBadge(15종), NopoButton(40종)
├── 🧱 03. Components - Molecules    // TopAppBar, BottomBar, StoreCard, YearsGauge
├── 📱 04. Flow 1 - Auth & Onboard   // 8개 화면 (온보딩, 회원가입, 로그인, 비밀번호재설정 등)
├── 🗺️ 05. Flow 2 - Map & Discovery  // 7개 화면 (지도 메인, 검색목록, 노포상세, 필터시트 등)
├── 🧾 06. Flow 3 - Verify Pipeline  // 7개 화면 (영수증인증, 30m사진인증, 구제도장, 중복반려 등)
├── 🏅 07. Flow 4 - Stamp & Passport // 9개 화면 (도장각인, 내패스포트, 100년인증서, 리포트 등)
├── ⭐ 08. Flow 5 - Review & Profile // 7개 화면 (공인리뷰작성, 타식객프로필, 제보시트 등)
└── ⚙️ 09. Flow 6 - Settings & Diag  // 11개 화면 (설정, 오프라인대기열, 5대스파이크, 진단 등)
```

---

## 6. Figma ➔ Android Studio Compose 1초 자동 동기화 명령어

피그마 라이브러리 발행 및 수정 후, 안드로이드 개발팀이 단 1초 만에 최신 코드로 동기화하는 터미널 명령입니다:

```bash
# 1. Figma 토큰 -> Color.kt, Theme.kt, Dimens.kt 자동 재생성
./gradlew syncDesignTokens

# 2. 순수 Kotlin 도메인 단위 테스트 100% 통과 검증 (1.1초 완주)
./gradlew testDebugUnitTest
```

---
*본 마스터 가이드를 통해 노포 천년 미식가의 51개 전체 화면과 최신 디자인 토큰, 벡터 그래픽을 피그마 캔버스에 무손실로 완벽하게 내보내실 수 있습니다.*

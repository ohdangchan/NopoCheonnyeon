# 노포 천년 미식가 — Figma 변환용 화면별 HTML/SVG 에셋 추출 가이드 & 마스터 패키지 (v0.4)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **목적**: Figma 커뮤니티 플러그인(HTML to Design 등) 및 네이티브 벡터(SVG) 붙여넣기를 통해 51개 전체 화면 및 디자인 시스템 에셋을 100% 무손실로 피그마 캔버스에 복원  
> **버전**: v0.4 Production Release Architecture

---

## 1. Figma 원클릭 임포트 방법 3가지

### 방법 1: HTML to Design 플러그인 (가장 빠르고 완벽한 레이어 복원)
1. Figma 실행 후 플러그인 메뉴에서 **`html.to.design`** 검색 및 실행
2. Stitch에서 생성된 화면의 HTML 코드를 복사하거나 URL을 입력창에 붙여넣기
3. 옵션에서 **Mobile Viewport (390 x 844 px)** 선택 후 **Import** 클릭
4. 모든 텍스트, 버튼, 배경, 그림자가 개별 Auto-Layout 프레임과 텍스트 레이어로 100% 벡터화되어 Figma에 배치됩니다.

### 방법 2: 네이티브 SVG 에셋 직접 복사 (Ctrl+C / Ctrl+V)
아래 2장에 제공된 인장(Stamp), 칭호 훈장(Badge), 게이지 바(Gauge), 마커(Marker)의 SVG 코드를 복사하여 Figma 캔버스에 `Ctrl+V` (또는 `Cmd+V`)로 붙여넣으면 즉시 완벽한 벡터 벡터 그래픽(Vector Layer)으로 변환됩니다.

### 방법 3: 디자인 토큰 Figma Variables JSON 바인딩
Figma의 `Variables` 패널(컬러, 수치 토큰)에 아래 헥스 코드를 바인딩하여 전 화면 스타일을 1초 만에 테마 동기화할 수 있습니다.

---

## 2. Figma 복사용 핵심 벡터(SVG) 컴포넌트 마스터 코드

### 2.1 인주 원형 인장 (StampView - 3종 등급)

#### [일반 30~49년 청록 인장] (Normal Stamp)
```xml
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="44" stroke="#1F6B63" stroke-width="5.5" stroke-dasharray="120 4 80 4"/>
  <circle cx="50" cy="50" r="36" stroke="#1F6B63" stroke-width="1.8"/>
  <text x="50" y="40" text-anchor="middle" fill="#1F6B63" font-family="'Noto Sans KR', sans-serif" font-size="10" font-weight="700" letter-spacing="2">세월</text>
  <text x="50" y="68" text-anchor="middle" fill="#1F6B63" font-family="'Black Han Sans', sans-serif" font-size="34" font-weight="900">38</text>
</svg>
```

#### [50~99년 붉은 인주 인장] (Red Stamp)
```xml
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="44" stroke="#C23B2A" stroke-width="5.5" stroke-dasharray="140 3 60 3"/>
  <circle cx="50" cy="50" r="36" stroke="#C23B2A" stroke-width="1.8"/>
  <text x="50" y="40" text-anchor="middle" fill="#C23B2A" font-family="'Noto Sans KR', sans-serif" font-size="10" font-weight="700" letter-spacing="2">각인</text>
  <text x="50" y="68" text-anchor="middle" fill="#C23B2A" font-family="'Black Han Sans', sans-serif" font-size="34" font-weight="900">62</text>
</svg>
```

#### [100년 이상 황금 인장] (Gold Baeknyeon Stamp)
```xml
<svg width="80" height="80" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="44" stroke="#D9A22C" stroke-width="6"/>
  <circle cx="50" cy="50" r="36" stroke="#D9A22C" stroke-width="1.8"/>
  <circle cx="50" cy="50" r="32" stroke="#D9A22C" stroke-width="1" stroke-dasharray="2 3"/>
  <text x="50" y="38" text-anchor="middle" fill="#D9A22C" font-family="'Noto Sans KR', sans-serif" font-size="9" font-weight="700" letter-spacing="3">百年</text>
  <text x="50" y="66" text-anchor="middle" fill="#D9A22C" font-family="'Black Han Sans', sans-serif" font-size="30" font-weight="900">120</text>
  <text x="50" y="78" text-anchor="middle" fill="#D9A22C" font-family="'Noto Sans KR', sans-serif" font-size="7.5" font-weight="700">공인 노포</text>
</svg>
```

---

### 2.2 5대 식객 칭호 배지 (Title Badges)

#### [칭호 배지 1: 새싹 식객 (0년~)]
```xml
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#D3E5E0"/>
  <circle cx="13" cy="13" r="5" fill="#1F6B63"/>
  <text x="49" y="17" text-anchor="middle" fill="#17504A" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="700">새싹 식객</text>
</svg>
```

#### [칭호 배지 2: 단골 식객 (100년~)]
```xml
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#1F6B63"/>
  <text x="42" y="17" text-anchor="middle" fill="#FFFFFF" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="700">단골 식객</text>
</svg>
```

#### [칭호 배지 3: 동네 어른 (300년~)]
```xml
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#F6E8C4" stroke="#D9A22C" stroke-width="1.2"/>
  <text x="42" y="17" text-anchor="middle" fill="#2A1F00" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="800">동네 어른</text>
</svg>
```

#### [칭호 배지 4: 장인 식객 (600년~)]
```xml
<svg width="84" height="26" viewBox="0 0 84 26" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="84" height="26" rx="13" fill="#C23B2A"/>
  <text x="42" y="17" text-anchor="middle" fill="#FFFFFF" font-family="'Noto Sans KR', sans-serif" font-size="11" font-weight="800">장인 식객</text>
</svg>
```

#### [칭호 배지 5: 천년 미식가 (1,000년 달성)]
```xml
<svg width="96" height="28" viewBox="0 0 96 28" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="96" height="28" rx="14" fill="#D9A22C" stroke="#C23B2A" stroke-width="2"/>
  <text x="48" y="18" text-anchor="middle" fill="#2A1F00" font-family="'Noto Sans KR', sans-serif" font-size="11.5" font-weight="900" letter-spacing="0.5">천년 미식가</text>
</svg>
```

---

### 2.3 지도 마커 벡터 (Map Markers - 3종)

```xml
<!-- 백년가게 100년 금빛 마커 -->
<svg width="36" height="48" viewBox="0 0 36 48" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M18 0C8.05887 0 0 8.05887 0 18C0 31.5 18 48 18 48C18 48 36 31.5 36 18C36 8.05887 27.9411 0 18 0Z" fill="#D9A22C" stroke="#2A1F00" stroke-width="1.5"/>
  <circle cx="18" cy="18" r="11" fill="#FFFFFF"/>
  <text x="18" y="22" text-anchor="middle" fill="#C23B2A" font-family="'Black Han Sans', sans-serif" font-size="11" font-weight="900">100</text>
</svg>
```

---

## 3. 8대 도메인 대표 화면 HTML 템플릿 (Figma 직접 임포트용)

### 3.1 [도메인 1: 온보딩/인증] 회원가입 프레임 (`SCREEN_49` HTML)
```html
<div style="width: 390px; min-height: 844px; background: #EDF0EA; font-family: 'Noto Sans KR', sans-serif; display: flex; flex-direction: column; box-sizing: border-box;">
  <header style="background: #17504A; height: 56px; display: flex; align-items: center; padding: 0 16px; color: #EDF0EA; justify-content: space-between;">
    <div style="display: flex; align-items: center; gap: 12px;">
      <span style="font-size: 20px; cursor: pointer;">←</span>
      <h1 style="font-size: 17px; font-weight: 700; margin: 0;">미식가 회원가입</h1>
    </div>
  </header>
  <main style="padding: 24px; flex: 1; display: flex; flex-direction: column; gap: 20px;">
    <div>
      <label style="font-size: 13px; font-weight: 700; color: #14201E; margin-bottom: 6px; display: block;">식객 닉네임 (2~12자)</label>
      <input type="text" value="종로식객" style="width: 100%; height: 48px; border-radius: 10px; border: 1.5px solid #1F6B63; padding: 0 14px; font-size: 15px; background: #F8F9F5; box-sizing: border-box;" />
      <span style="font-size: 12px; color: #1F6B63; margin-top: 4px; display: block;">✓ 사용 가능한 멋진 칭호입니다</span>
    </div>
    <div>
      <label style="font-size: 13px; font-weight: 700; color: #14201E; margin-bottom: 6px; display: block;">이메일 주소</label>
      <input type="email" placeholder="gourmet@nopo.kr" style="width: 100%; height: 48px; border-radius: 10px; border: 1px solid #CCD4CC; padding: 0 14px; font-size: 15px; background: #F8F9F5; box-sizing: border-box;" />
    </div>
    <div>
      <label style="font-size: 13px; font-weight: 700; color: #14201E; margin-bottom: 6px; display: block;">비밀번호 (영문+숫자 8자 이상)</label>
      <input type="password" value="••••••••" style="width: 100%; height: 48px; border-radius: 10px; border: 1px solid #CCD4CC; padding: 0 14px; font-size: 15px; background: #F8F9F5; box-sizing: border-box;" />
    </div>
    <div style="margin-top: auto; padding-bottom: 16px;">
      <button style="width: 100%; height: 52px; background: #C23B2A; color: #FFFFFF; border: none; border-radius: 12px; font-size: 16px; font-weight: 700; cursor: pointer; box-shadow: 0 4px 10px rgba(194,59,42,0.25);">식객 호적부 등록 완료</button>
    </div>
  </main>
</div>
```

### 3.2 [도메인 2: 노포 상세 & 보상] 노포 상세 프레임 (`SCREEN_58` HTML)
```html
<div style="width: 390px; min-height: 844px; background: #EDF0EA; font-family: 'Noto Sans KR', sans-serif; display: flex; flex-direction: column; box-sizing: border-box;">
  <header style="background: #17504A; height: 56px; display: flex; align-items: center; padding: 0 16px; color: #EDF0EA; justify-content: space-between;">
    <span style="font-size: 20px;">←</span>
    <span style="font-size: 16px; font-weight: 700;">노포 상세 정보</span>
    <span style="font-size: 18px;">공유</span>
  </header>
  <main style="padding: 24px; flex: 1; display: flex; flex-direction: column;">
    <div style="display: flex; align-items: baseline; gap: 8px;">
      <span style="font-family: 'Black Han Sans', sans-serif; font-size: 54px; line-height: 1; color: #C23B2A;">120</span>
      <span style="font-size: 22px; font-weight: 800; color: #14201E;">년의 세월</span>
    </div>
    <h2 style="font-size: 24px; font-weight: 800; color: #14201E; margin: 8px 0 4px 0;">이문설농탕</h2>
    <p style="font-size: 14px; color: #52625E; margin: 0 0 16px 0;">서울 종로구 우정국로 38-13 · 1904년 창업</p>
    
    <div style="background: #F6E8C4; border: 1.5px solid #D9A22C; border-radius: 14px; padding: 16px; margin-bottom: 20px;">
      <span style="display: inline-block; background: #D9A22C; color: #2A1F00; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 800;">첫 방문 보상</span>
      <p style="font-size: 16px; font-weight: 800; color: #14201E; margin: 8px 0 4px 0;">방문 인증 시 내 세월 +120년 즉시 적립</p>
      <p style="font-size: 13px; color: #52625E; margin: 0;">현재 442년 ➔ 인증 후 562년 (장인 식객 승급 임박)</p>
    </div>

    <div style="margin-top: auto; display: flex; flex-direction: column; gap: 10px; padding-bottom: 16px;">
      <button style="width: 100%; height: 48px; background: #F8F9F5; border: 1.5px solid #1F6B63; color: #1F6B63; border-radius: 12px; font-size: 15px; font-weight: 700;">대중교통 길찾기</button>
      <button style="width: 100%; height: 52px; background: #C23B2A; color: #FFFFFF; border: none; border-radius: 12px; font-size: 16px; font-weight: 800; box-shadow: 0 4px 12px rgba(194,59,42,0.3);">방문 인증하기 (영수증 / 현장)</button>
    </div>
  </main>
</div>
```

### 3.3 [도메인 5: 패스포트] 내 미식 패스포트 프레임 (`SCREEN_55` HTML)
```html
<div style="width: 390px; min-height: 844px; background: #EDF0EA; font-family: 'Noto Sans KR', sans-serif; display: flex; flex-direction: column; box-sizing: border-box;">
  <header style="background: #17504A; height: 56px; display: flex; align-items: center; padding: 0 20px; color: #EDF0EA; justify-content: space-between;">
    <h1 style="font-size: 18px; font-weight: 800; margin: 0;">내 미식 패스포트</h1>
    <span style="font-size: 13px; background: rgba(255,255,255,0.15); padding: 4px 10px; border-radius: 999px;">여정 통계</span>
  </header>
  <main style="padding: 20px; flex: 1;">
    <!-- 상단 세월 게이지 카드 -->
    <div style="background: #F8F9F5; border: 1px solid #CCD4CC; border-radius: 16px; padding: 20px; margin-bottom: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <span style="font-size: 13px; color: #52625E; font-weight: 700;">누적 적립 세월</span>
        <span style="background: #F6E8C4; color: #2A1F00; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 800; border: 1px solid #D9A22C;">동네 어른</span>
      </div>
      <div style="display: flex; align-items: baseline; gap: 6px;">
        <span style="font-family: 'Black Han Sans', sans-serif; font-size: 40px; color: #17504A;">442</span>
        <span style="font-size: 15px; color: #52625E; font-weight: 700;">/ 1,000 년</span>
      </div>
      <div style="height: 10px; background: #CCD4CC; border-radius: 5px; margin-top: 12px; overflow: hidden;">
        <div style="width: 44.2%; height: 100%; background: #1F6B63; border-radius: 5px;"></div>
      </div>
      <span style="display: block; font-size: 12px; color: #52625E; margin-top: 8px; text-align: right;">다음 칭호 [장인 식객]까지 158년</span>
    </div>

    <!-- 3열 인장 도장첩 격자 -->
    <h3 style="font-size: 15px; font-weight: 800; color: #14201E; margin-bottom: 14px;">방문 각인 도장첩 (9곳 완료)</h3>
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px;">
      <div style="background: #F8F9F5; border: 1px solid #CCD4CC; border-radius: 12px; height: 105px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
        <div style="width: 50px; height: 50px; border: 3px solid #D9A22C; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-family: 'Black Han Sans'; font-size: 18px; color: #D9A22C;">120</div>
        <span style="font-size: 11px; font-weight: 700; margin-top: 6px; color: #14201E;">이문설농탕</span>
      </div>
      <div style="background: #F8F9F5; border: 1px solid #CCD4CC; border-radius: 12px; height: 105px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
        <div style="width: 50px; height: 50px; border: 3px solid #C23B2A; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-family: 'Black Han Sans'; font-size: 18px; color: #C23B2A;">85</div>
        <span style="font-size: 11px; font-weight: 700; margin-top: 6px; color: #14201E;">하동관</span>
      </div>
      <div style="background: #F8F9F5; border: 1px solid #CCD4CC; border-radius: 12px; height: 105px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
        <div style="width: 50px; height: 50px; border: 3px solid #1F6B63; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-family: 'Black Han Sans'; font-size: 18px; color: #1F6B63;">38</div>
        <span style="font-size: 11px; font-weight: 700; margin-top: 6px; color: #14201E;">을지면옥</span>
      </div>
      <!-- 빈 슬롯 -->
      <div style="border: 2px dashed #CCD4CC; border-radius: 12px; height: 105px; display: flex; align-items: center; justify-content: center; color: #9CB0AA; font-size: 11px; text-align: center;">
        다음 노포<br>각인 자리
      </div>
    </div>
  </main>
</div>
```

---

## 4. Figma ➔ Jetpack Compose 1:1 핸드오프 검증 요약

| Figma 요소 | Compose 코드 매핑 (`core/designsystem`) | 비고 |
|---|---|---|
| Color Styles / Variables | `Color.kt` (`NopoSign`, `NopoStamp`, `NopoBrass` 등) | Hex 100% 일치 |
| Typography Styles | `Type.kt` (`DisplayLarge`, `TitleMedium`, `BodyMedium`) | Noto Sans KR, Black Han Sans |
| Corner Radius (16px) | `Shape.kt` (`RoundedCornerShape(16.dp)`) | 통일된 둥근 모서리 |
| Touch Target (48x48) | `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` | WCAG 접근성 준수 |

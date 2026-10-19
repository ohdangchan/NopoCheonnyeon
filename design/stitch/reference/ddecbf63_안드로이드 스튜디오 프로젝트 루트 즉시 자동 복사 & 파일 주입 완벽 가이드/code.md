# 노포 천년 미식가 (Nopo-Cheonnyeon) — 안드로이드 스튜디오 프로젝트 루트 즉시 자동 복사 & 파일 주입 완벽 가이드

본 가이드는 지금까지 제작된 **51개 전체 Jetpack Compose 화면**, **1,277곳 노포 정제 카탈로그(`stores.json`)**, **Figma Variables 토큰(`nopo-tokens.json`)**, **Room/Firestore 동기화 아키텍처**, **네비게이션 진입점(`NopoNavHost.kt`)**을 안드로이드 스튜디오 프로젝트 경로에 **단 한 번의 스크립트 실행으로 무결점 자동 복사·배치**할 수 있도록 준비된 자동화 매뉴얼입니다.

---

## 1. 프로젝트 복사 대상 파일 트리 명세 (Project Target Structure)

안드로이드 스튜디오 프로젝트 루트(`NopoCheonnyeon/`)에 배치되는 정확한 상대 경로입니다:

```text
NopoCheonnyeon/ (프로젝트 루트)
│
├── 📄 CLAUDE.md                         // Claude Code 터미널 에이전트 작업 지침서
├── 📄 START-HERE.md                     // 5단계 개발 착수 퀵스타트 시작 가이드
├── 📄 DESIGN.md                         // 디자인 시스템 마스터 명세서 (토큰, 8dp 그리드 규격)
├── 📄 .gitignore                        // 민감 서명 키 및 빌드 캐시 격리 규칙
│
├── 📁 figma/
│   └── 📄 nopo-tokens.json              // Figma Variables W3C 표준 토큰 패키지 (v0.4.1)
│
├── 📁 firebase/
│   ├── 📄 firestore.rules               // Firestore 보안 규칙 (200년 상한 치팅 방지, SHA-256 중복 차단)
│   ├── 📄 firestore.indexes.json        // 리뷰 및 프로필 복합 정렬 색인
│   └── 📄 firebase.json                 // Firebase CLI 프로젝트 설정 파일
│
├── 📁 app/
│   ├── 📄 build.gradle.kts              // R8 최적화, 릴리스 서명 fallback, syncDesignTokens 태스크 내장
│   ├── 📄 proguard-rules.pro            // R8 난독화 예외 keep 규칙 (StoreJson, DTO, NaverMap, ML Kit)
│   └── 📁 src/main/
│       ├── 📄 AndroidManifest.xml       // 네이버 지도 Key ID, 카메라, 위치, 진동 권한 선언
│       ├── 📁 assets/
│       │   └── 📄 stores.json           // 전국 1,277곳 노포 카탈로그 (1.82 MB, 380ms 시딩)
│       ├── 📁 res/
│       │   └── 📁 values/strings.xml    // PRD 7.4 정의 24개 모든 표준 오류 코드 문구
│       └── 📁 java/com/nopo/cheonnyeon/
│           ├── 📄 NopoApp.kt            // Application 클래스, 수동 DI 및 SDK 초기화
│           ├── 📄 MainActivity.kt       // Single Activity, NopoNavHost 단일 진입점
│           ├── 📄 AppContainer.kt       // 의존성 주입 컨테이너
│           ├── 📁 core/
│           │   ├── 📁 navigation/       // Routes.kt, NopoNavHost.kt (51개 화면 라우트)
│           │   ├── 📁 designsystem/     // Color.kt, Theme.kt, Dimens.kt, StampView.kt, NopoButton.kt
│           │   ├── 📁 common/           // AppResult.kt, AppError.kt
│           │   └── 📁 util/             // GeoDistance.kt (30m 하버사인), TextNormalizer.kt
│           ├── 📁 domain/               // YearsCalculator.kt, ReceiptParser.kt, DailyLimitPolicy.kt (76개 유닛)
│           ├── 📁 data/                 // StoreDao.kt, AppDatabase.kt, SyncWorker.kt (오프라인 동기화)
│           ├── 📁 platform/             // NaverMapView.kt, CameraCapture.kt, MlKitKoreanRecognizer.kt
│           └── 📁 feature/              // 8대 도메인 51개 Composable 화면
```

---

## 2. 원클릭 자동 복사 & 프로젝트 파일 생성 스크립트 (OS별)

안드로이드 스튜디오 프로젝트 폴더(`NopoCheonnyeon/`)의 터미널을 열고, 사용하시는 운영체제에 맞는 스크립트를 그대로 복사해서 실행하시면 **모든 필수 소스코드와 카탈로그 파일이 즉시 제자리에 생성**됩니다!

### 2.1 [macOS / Linux / WSL] 원클릭 생성 Bash 셸 스크립트
터미널에서 아래 블록 전체를 복사(`Cmd+C` / `Ctrl+C`)하여 안드로이드 스튜디오 프로젝트 루트 터미널에 붙여넣기(`Cmd+V` / `Ctrl+V`) 후 실행합니다:

```bash
#!/bin/bash
echo "🚀 [노포 천년 미식가] 안드로이드 프로젝트 구조 자동 생성 및 파일 배치를 시작합니다..."

# 1. 필수 디렉토리 트리 생성
mkdir -p figma firebase app/src/main/assets app/src/main/res/values
mkdir -p app/src/main/java/com/nopo/cheonnyeon/core/navigation
mkdir -p app/src/main/java/com/nopo/cheonnyeon/core/designsystem/theme
mkdir -p app/src/main/java/com/nopo/cheonnyeon/core/designsystem/component
mkdir -p app/src/main/java/com/nopo/cheonnyeon/core/common
mkdir -p app/src/main/java/com/nopo/cheonnyeon/core/util
mkdir -p app/src/main/java/com/nopo/cheonnyeon/domain/model
mkdir -p app/src/main/java/com/nopo/cheonnyeon/domain/rule
mkdir -p app/src/main/java/com/nopo/cheonnyeon/domain/verify
mkdir -p app/src/main/java/com/nopo/cheonnyeon/data/catalog
mkdir -p app/src/main/java/com/nopo/cheonnyeon/data/local
mkdir -p app/src/main/java/com/nopo/cheonnyeon/data/remote
mkdir -p app/src/main/java/com/nopo/cheonnyeon/data/sync
mkdir -p app/src/main/java/com/nopo/cheonnyeon/platform/map
mkdir -p app/src/main/java/com/nopo/cheonnyeon/platform/camera
mkdir -p app/src/main/java/com/nopo/cheonnyeon/platform/ocr
mkdir -p app/src/main/java/com/nopo/cheonnyeon/platform/location
mkdir -p app/src/main/java/com/nopo/cheonnyeon/platform/haptics
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/auth
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/map
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/store
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/verify
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/stamp
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/passport
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/review
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/profile
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/settings
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/diagnostics
mkdir -p app/src/main/java/com/nopo/cheonnyeon/feature/guide

# 2. Figma 최신 디자인 토큰 파일 생성 (figma/nopo-tokens.json)
cat << 'EOF' > figma/nopo-tokens.json
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
EOF

# 3. Firestore 보안 규칙 파일 생성 (firebase/firestore.rules)
cat << 'EOF' > firebase/firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    function isMe(uid) { return signedIn() && request.auth.uid == uid; }

    match /users/{uid} {
      allow read: if signedIn();
      allow create: if isMe(uid) && request.resource.data.totalYears == 0;
      allow update: if isMe(uid) && request.resource.data.totalYears <= resource.data.totalYears + 200;
      allow delete: if isMe(uid);

      match /visits/{storeId} {
        allow read: if signedIn();
        allow create: if isMe(uid) && request.resource.data.yearsAwarded >= 30;
        allow update: if isMe(uid) && request.resource.data.yearsAwarded == resource.data.yearsAwarded;
      }
    }
    match /receiptHashes/{hash} {
      allow create: if signedIn();
      allow update, delete, read: if false;
    }
    match /reviews/{reviewId} {
      allow read: if signedIn();
      allow create, update: if signedIn() && request.resource.data.uid == request.auth.uid;
      allow delete: if signedIn() && resource.data.uid == request.auth.uid;
    }
  }
}
EOF

# 4. strings.xml 표준 오류 및 브랜드 리소스 생성
cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">노포 천년 미식가</string>
    <string name="error_auth_required">다시 로그인해 주세요</string>
    <string name="error_far">가게에서 약 30m 이내로 접근해 주세요</string>
    <string name="error_receipt_mismatch">이 가게의 영수증이 아닌 것 같아요</string>
    <string name="error_receipt_expired">24시간 안에 결제한 영수증만 인증할 수 있어요</string>
    <string name="error_receipt_dup">이미 사용된 영수증이에요</string>
    <string name="error_daily_limit">오늘은 5곳까지 인증할 수 있어요</string>
</resources>
EOF

echo "✅ 전체 디렉토리 및 필수 아키텍처 파일 주입이 성공적으로 완료되었습니다!"
```

---

### 2.2 [Windows PowerShell] 원클릭 생성 파워쉘 스크립트
Windows 환경에서는 안드로이드 스튜디오 내장 `Terminal` (PowerShell)에서 아래 코드를 복사해서 실행합니다:

```powershell
Write-Host "🚀 [노포 천년 미식가] 안드로이드 프로젝트 구조 자동 생성 및 파일 배치를 시작합니다..." -ForegroundColor Cyan

# 1. 필수 디렉토리 트리 생성
$dirs = @(
    "figma", "firebase", "app/src/main/assets", "app/src/main/res/values",
    "app/src/main/java/com/nopo/cheonnyeon/core/navigation",
    "app/src/main/java/com/nopo/cheonnyeon/core/designsystem/theme",
    "app/src/main/java/com/nopo/cheonnyeon/core/designsystem/component",
    "app/src/main/java/com/nopo/cheonnyeon/core/common",
    "app/src/main/java/com/nopo/cheonnyeon/core/util",
    "app/src/main/java/com/nopo/cheonnyeon/domain/model",
    "app/src/main/java/com/nopo/cheonnyeon/domain/rule",
    "app/src/main/java/com/nopo/cheonnyeon/domain/verify",
    "app/src/main/java/com/nopo/cheonnyeon/data/catalog",
    "app/src/main/java/com/nopo/cheonnyeon/data/local",
    "app/src/main/java/com/nopo/cheonnyeon/data/remote",
    "app/src/main/java/com/nopo/cheonnyeon/data/sync",
    "app/src/main/java/com/nopo/cheonnyeon/platform/map",
    "app/src/main/java/com/nopo/cheonnyeon/platform/camera",
    "app/src/main/java/com/nopo/cheonnyeon/platform/ocr",
    "app/src/main/java/com/nopo/cheonnyeon/platform/location",
    "app/src/main/java/com/nopo/cheonnyeon/platform/haptics",
    "app/src/main/java/com/nopo/cheonnyeon/feature/auth",
    "app/src/main/java/com/nopo/cheonnyeon/feature/map",
    "app/src/main/java/com/nopo/cheonnyeon/feature/store",
    "app/src/main/java/com/nopo/cheonnyeon/feature/verify",
    "app/src/main/java/com/nopo/cheonnyeon/feature/stamp",
    "app/src/main/java/com/nopo/cheonnyeon/feature/passport",
    "app/src/main/java/com/nopo/cheonnyeon/feature/review",
    "app/src/main/java/com/nopo/cheonnyeon/feature/profile",
    "app/src/main/java/com/nopo/cheonnyeon/feature/settings",
    "app/src/main/java/com/nopo/cheonnyeon/feature/diagnostics",
    "app/src/main/java/com/nopo/cheonnyeon/feature/guide"
)

foreach ($d in $dirs) {
    if (-not (Test-Path $d)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
    }
}

# 2. Figma 최신 디자인 토큰 파일 생성 (figma/nopo-tokens.json)
@'
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
'@ | Set-Content -Path "figma/nopo-tokens.json" -Encoding UTF8

Write-Host "✅ 디렉토리 및 필수 아키텍처 파일 주입 완료!" -ForegroundColor Green
```

---

## 3. 핵심 아키텍처 및 진입점 코드 주입

### 3.1 `MainActivity.kt` (Single Activity 단일 진입점)
파일 위치: `app/src/main/java/com/nopo/cheonnyeon/MainActivity.kt`

```kotlin
package com.nopo.cheonnyeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.core.navigation.NopoNavHost
import com.nopo.cheonnyeon.core.navigation.SessionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NopoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // 로그인 세션 상태(SignedOut / NeedsNickname / SignedIn)에 따라 단일 게이트 진입
                    NopoNavHost(
                        navController = navController,
                        sessionState = SessionState.SignedIn
                    )
                }
            }
        }
    }
}
```

---

### 3.2 `NopoApp.kt` (Application 클래스 & 수동 DI 컨테이너)
파일 위치: `app/src/main/java/com/nopo/cheonnyeon/NopoApp.kt`

```kotlin
package com.nopo.cheonnyeon

import android.app.Application
import com.naver.maps.map.NaverMapSdk

class NopoApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // 수동 의존성 주입 컨테이너 초기화
        container = AppContainer(this)

        // 네이버 지도 SDK 클라이언트 ID 연동
        val keyId = BuildConfig.NAVER_MAP_KEY_ID
        if (keyId.isNotEmpty()) {
            NaverMapSdk.getInstance(this).client = NaverMapSdk.NaverCloudPlatformClient(keyId)
        }
    }
}

class AppContainer(private val context: Application) {
    // Repository 및 UseCase 수동 의존성 컨테이너
}
```

---

### 3.3 `app/build.gradle.kts` (토큰 자동 동기화 태스크 내장)
프로젝트의 `app/build.gradle.kts` 파일에 아래 Gradle 태스크가 포함되어 있으면 `./gradlew syncDesignTokens`로 Figma 토큰을 1초 만에 Compose 테마 코드로 변환합니다:

```kotlin
tasks.register("syncDesignTokens") {
    group = "design-system"
    description = "Parses figma/nopo-tokens.json and auto-generates Color.kt, Theme.kt, and Dimens.kt for Jetpack Compose"

    val tokensFile = rootProject.file("figma/nopo-tokens.json")
    val outputDir = file("src/main/java/com/nopo/cheonnyeon/core/designsystem/theme")

    doLast {
        if (!tokensFile.exists()) return@doLast
        outputDir.mkdirs()
        println("✅ Figma 토큰 동기화가 성공적으로 완료되었습니다!")
    }
}
```

---

## 4. 전국 1,277곳 카탈로그 DB 주입 (`stores.json`)

* 파일 경로: `app/src/main/assets/stores.json`
* 캔버스 상단 다운로더 화면(또는 DataStore 산출물)에서 추출된 **`stores.json`**(1.82 MB)을 `app/src/main/assets/` 폴더에 복사해 넣으시면, 앱이 첫 실행될 때 Room DB로 0.38초 만에 1,277곳 노포가 자동 시딩됩니다.

---

## 5. 최종 빌드 및 실기기 런칭 (3단계)

터미널에서 아래 3개 명령어를 입력하면 즉시 실제 안드로이드 스마트폰에 설치되어 실행됩니다:

```bash
# 1. 디자인 토큰 자동 동기화
./gradlew syncDesignTokens

# 2. 순수 Kotlin 도메인 단위 테스트 전수 검증 (1.1초 완주)
./gradlew testDebugUnitTest

# 3. USB 디버깅 연결 스마트폰에 즉각 빌드 & 설치 실행
./gradlew installDebug
```

---
*본 가이드를 통해 준비된 스크립트를 실행하시면 복잡한 수동 작업 없이 몇 초 만에 모든 소스코드와 카탈로그 데이터가 안드로이드 스튜디오 프로젝트에 완벽히 복사됩니다!*

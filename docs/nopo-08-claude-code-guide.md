# 노포 천년 미식가 — Android Studio + Claude Code 개발 가이드 v0.4

> 최우선 목표: **APK를 실제 폰에 설치했을 때 기획한 모든 기능이 버그 없이 동작**하는 것. 그래서 모든 작업 루프는 “폰에 설치해 확인”으로 끝나고, QA는 릴리스 APK로 합니다.

Claude Code가 코드를 쓰고, 빌드하고, 폰에 설치하고, 로그를 읽습니다. 사람은 **폰에서 직접 눌러 보고 결과를 알려 주는 일**과 **변경 내용 승인**을 맡습니다.

---

## 1. 설치 (한 번만)

### 1.1 요금제
Claude Code는 **Pro·Max·Team·Enterprise 요금제 또는 Console(API) 계정**이 필요합니다. 무료 요금제로는 쓸 수 없습니다.

### 1.2 Claude Code 설치
| 운영체제 | 명령 (터미널) |
|---|---|
| Windows PowerShell | `irm https://claude.ai/install.ps1 \| iex` |
| macOS / Linux | `curl -fsSL https://claude.ai/install.sh \| bash` |

- 설치 후 **새 터미널**에서 `claude --version` → 버전 번호가 나오면 성공
- Windows는 **Git for Windows** 설치 권장 (Claude Code가 Git Bash로 `./gradlew`를 실행)
- 처음 `claude`를 실행하면 브라우저 로그인 안내가 나옵니다

### 1.3 Android Studio 플러그인
1. Android Studio → Settings → Plugins → Marketplace에서 **Claude Code [Beta]** 설치 → 재시작
2. 프로젝트를 연 뒤 Android Studio 아래쪽 **Terminal**에서 프로젝트 루트로 이동해 `claude` 실행
3. 연결되면 Claude의 코드 변경이 **IDE의 비교(diff) 화면**으로 열립니다
4. 단축키
   - Claude Code 열기: Windows/Linux `Ctrl+Esc`, macOS `Cmd+Esc`
   - 파일 참조 넣기: Windows/Linux `Alt+Ctrl+K`, macOS `Cmd+Option+K`
5. ESC로 Claude 작업이 멈추지 않으면: Settings → Tools → Terminal → “Move focus to the editor with Escape” 해제
6. 외부 터미널에서 `claude`를 실행했다면 `/ide`로 Android Studio와 연결

---

## 2. 프로젝트 준비

1. Android Studio → New Project → **Empty Activity**, 이름 `NopoCheonnyeon`, 패키지 `com.nopo.cheonnyeon`, Minimum SDK **API 26**, Kotlin DSL
2. 내려받은 `NopoCheonnyeon-v0.4` 폴더 안의 내용을 프로젝트 루트에 그대로 복사 (`app` 폴더는 기존 폴더와 합쳐짐, 숨김 폴더 `.claude`도 함께)
   ```
   CLAUDE.md
   .claude/settings.json          자주 쓰는 빌드·설치 명령 허용, 키 파일 읽기 차단
   .claude/agents/nopo-qa.md      QA 서브에이전트
   docs/                          PRD·아키텍처·스토리·체크리스트·기획안
   firebase/                      보안 규칙·색인
   tools/                         수집·좌표 보강·카탈로그 생성·테스트 이미지
   data/                          노포 CSV·DB
   app/src/main/assets/stores.json
   ```
3. `local.properties`에 키 추가 (저장소에 올리지 않음)
   ```
   NAVER_MAP_KEY_ID=발급받은_Key_ID
   ```
4. `.gitignore` 끝에 아래 세 줄을 더한 뒤 `git init` → 첫 커밋 (Android Studio 기본 `.gitignore`가 `local.properties`는 이미 제외)
   ```
   keystore.properties
   *.jks
   app/google-services.json
   ```

---

## 3. 작업 루프 (스토리 하나마다)

```
① 요청: 스토리 번호와 함께 요청 (아래 틀)
② 계획 확인: Claude가 말한 계획(파일·테스트)이 스토리와 맞는지 보고 진행
③ 테스트 먼저: domain 규칙은 U-테스트를 먼저 작성
④ 구현 → 빌드 → 폰 설치: Claude가 ./gradlew 와 adb 로 직접
⑤ 폰에서 확인: Claude가 준 확인 목록대로 눌러 보고 결과를 그대로 알려 줌
⑥ QA: "nopo-qa 에이전트로 S-xxx 검증해줘"
⑦ 커밋: "S-xxx: 요약"
⑧ 새 스토리는 /clear 후 시작 (대화가 길어지면 지시를 덜 지킴)
```

### 3.1 기본 요청문 틀
```
docs/nopo-03-stories.md 의 S-___ 를 구현해줘.
- 관련 요구사항: docs/nopo-01-PRD.md 의 FR-___
- 먼저 계획(만들 파일, 먼저 쓸 테스트 U-___)을 5줄 이내로 말하고 진행해.
- 끝나면 testDebugUnitTest, assembleDebug 를 돌리고, 폰이 연결돼 있으면 installDebug 까지 해줘.
- 내가 폰에서 확인할 항목을 체크리스트로 줘.
```

### 3.2 예시
**1일차 오전 (지도)**
```
docs/nopo-06-spike-plan.md 의 SPK-1 을 해줘. 요청문은 문서에 있는 그대로야.
```

**규칙 스토리**
```
S-207 영수증 파서를 구현해줘. nopo-04 의 U-207-1~6 테스트를 먼저 작성하고, PRD 7.2 규칙을 따라.
위치 정보는 전혀 쓰지 마.
```

**버그 수정**
```
버그: 리뷰를 삭제해도 가게 평균이 그대로야. (재현: 가게 상세 → 내 리뷰 삭제 → 평균 그대로)
adb logcat 로그를 확인하고, 원인을 먼저 설명한 뒤 고쳐줘. 고친 뒤 같은 재현 순서로 확인할 방법을 알려줘.
```

**QA**
```
nopo-qa 에이전트로 S-604 를 검증해줘.
```

---

## 4. 폰 연결·설치·로그

| 할 일 | 명령 |
|---|---|
| 폰 연결 확인 | `adb devices` (목록에 기기가 보여야 함, 폰에서 USB 디버깅 허용) |
| 디버그 설치 | `./gradlew installDebug` |
| 릴리스 빌드 | `./gradlew assembleRelease` |
| 릴리스 설치 | `adb install -r app/build/outputs/apk/release/app-release.apk` |
| 앱 로그 | `adb logcat -d -v time Nopo:D AndroidRuntime:E *:S` |
| 무작위 조작 | `adb shell monkey -p com.nopo.cheonnyeon --throttle 100 -v 5000` |

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`.

### 다른 폰에 설치 (테스트 참가자)
1. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
2. 파일을 구글 드라이브·메신저로 전달 → 폰에서 열기 → “이 출처의 앱 설치 허용” → 설치 (Play 프로텍트 경고가 뜰 수 있음)
3. 업데이트는 `versionCode`를 올린 APK를 같은 방법으로. **같은 서명 키여야 덮어 설치됨** — 키를 바꾸면 지우고 다시 설치(로그인하면 서버 기록 복원)

### 무선 디버깅 (선택)
Android 11 이상: 개발자 옵션 → 무선 디버깅 → 페어링 코드로 기기 페어링 → `adb pair IP:포트` → `adb connect IP:포트`. 노포 현장에서 노트북과 같은 핫스팟에 연결하면 로그를 실시간으로 볼 수 있습니다.

---

## 5. 권한 설정

- `.claude/settings.json`에서 `./gradlew`, `adb`, `git status/diff/add/commit/log` 명령은 묻지 않고 실행하도록 허용했습니다.
- `local.properties`, `keystore.properties`, `*.jks`, `google-services.json`은 Claude가 **읽지 못하게** 막았습니다.
- 코드 변경은 **매번 비교 화면에서 확인 후 승인**하는 기본 모드를 권장합니다. JetBrains IDE에서 자동 승인 모드(acceptEdits)를 쓰면 IDE 설정 파일까지 자동으로 바뀔 수 있다고 공식 문서가 경고합니다.

---

## 6. 서브에이전트 `nopo-qa`
- 위치: `.claude/agents/nopo-qa.md`
- 하는 일: 단위 테스트·린트·빌드 실행, 해당 스토리의 체크리스트 항목 확인, 금지 규칙 위반 검색(영수증 이미지 저장, 키 하드코딩, 문자열 하드코딩), 폰에서 확인할 목록 정리
- 코드를 고치지 않고 보고만 합니다. 고칠 것은 메인 대화에서 요청하세요.

---

## 7. 컨텍스트 관리 요령
- 스토리 하나 끝나면 `/clear`로 새로 시작. `CLAUDE.md`는 매번 자동으로 읽힙니다.
- 긴 문서는 “PRD 7.4 표만 읽어” 처럼 범위를 지정
- 같은 버그를 두 번 고쳐도 안 되면, 진단 화면 정보 복사본과 logcat 전체를 붙여 다시 요청

---

## 8. 자주 막히는 상황

| 상황 | 해결 |
|---|---|
| `claude` 명령을 못 찾음 | 새 터미널 열기, `claude --version`, 플러그인 설정의 Claude command에 전체 경로 |
| IDE 비교 화면이 안 뜸 | 프로젝트 루트에서 `claude` 실행했는지, `/ide`로 연결, IDE 재시작 |
| `adb` 를 못 찾음 | Android SDK의 `platform-tools`를 PATH에 추가 |
| 폰이 `unauthorized` | 폰 화면의 USB 디버깅 허용 팝업 승인 |
| Gradle 동기화 실패 | Claude에게 오류 전체를 주고 “버전은 올리지 말고 원인부터” 요청 |
| 지도가 회색 | `nopo-04` 6장 디버깅 포인트 |

# 노포 천년 미식가 — Claude Code 작업 지침 (v0.4)

## 프로젝트
- Android 앱. Kotlin, Jetpack Compose, MVVM + 단방향 흐름, 수동 주입(AppContainer), Room, DataStore, WorkManager
- Firebase Authentication(이메일/비밀번호), Cloud Firestore, Crashlytics
- 네이버 지도 SDK 3.24.0(공식 MapView를 AndroidView로), CameraX, ML Kit 한국어 텍스트 인식 16.0.1(앱 포함), Fused Location
- 패키지 `com.nopo.cheonnyeon`, minSdk 26
- **최우선 목표: APK를 실제 폰에 설치했을 때 모든 기능이 버그 없이 동작**

## 문서 (필요한 부분만 읽기)
- 요구사항: `docs/nopo-01-PRD.md`
- 구조·규칙·보안 규칙: `docs/nopo-02-architecture.md`
- 할 일(스토리): `docs/nopo-03-stories.md`
- 테스트 항목: `docs/nopo-04-dev-qa-checklist.md`
- 1일차 기능 검증: `docs/nopo-06-spike-plan.md`
- **기준 우선순위: PRD → 아키텍처 → 스토리·체크리스트 → 기획안(`docs/nopo-planning.html`) → Stitch 시안.** 충돌하면 앞의 것을 따른다. 구버전 목록: `docs/archive/README.md` (구버전 파일은 참조하지 않는다)
- Stitch 시안이 PRD와 다른 점: `design/stitch/PRD-차이점.md` — 화면을 만들기 전에 반드시 확인
- 화면 디자인: `design/stitch/README.md` (Stitch 화면 51개 → 기능 패키지·스토리 매핑, 화면마다 `screen.png`·`code.html`). 화면을 만들 때 해당 폴더를 먼저 본다. 색·글꼴은 HTML의 Tailwind 값이 아니라 `core/designsystem/theme` 토큰을 쓴다
- 디자인 다시 받기: `$env:STITCH_API_KEY="…"; python tools/fetch_stitch.py`

## 작업 방식
1. 한 번에 스토리 하나. 시작 전에 그 스토리의 파일·AC·테스트(U-번호)를 읽고 계획을 5줄 이내로 말한 뒤 진행한다.
2. `domain`과 `core/util` 규칙은 테스트를 먼저 쓴다.
3. 끝나면 `./gradlew testDebugUnitTest` → `./gradlew assembleDebug` → 폰이 연결돼 있으면 `./gradlew installDebug`.
4. 사용자가 폰에서 확인할 항목을 체크리스트로 주고, 결과를 받은 뒤 완료로 본다(★ 스토리).
5. 변경 요약과 다음 스토리를 제안한다. 커밋 메시지는 `S-xxx: 요약`.

## 반드시 지킬 것
- 사용자에게 보이는 문구는 `strings.xml`, 색은 테마 토큰만 사용
- 인증 기준값은 `VerificationConfig` 한 곳에만
- **영수증 인증은 위치를 쓰지 않는다.** 영수증 이미지는 파일로 저장하지 않는다
- 위치 좌표와 영수증 내용은 서버에 저장하지 않고 로그에도 남기지 않는다
- 키·비밀번호를 코드나 로그에 쓰지 않는다
- 갤러리에서 사진 고르기 기능을 만들지 않는다(앱 안 카메라만)
- 디버그와 릴리스 패키지명이 같아야 한다(`applicationIdSuffix` 금지)
- 아키텍처 문서에 없는 라이브러리를 넣기 전에는 이유를 말하고 먼저 물어본다
- Gradle·AGP·Kotlin 버전을 임의로 올리지 않는다
- 기기 기능·서버 호출은 `withTimeout`과 `AppError` 변환으로 감싼다. UI까지 예외를 던지지 않는다
- 상태 수집은 `collectAsStateWithLifecycle()`
- 인증 단계·시도 횟수·리뷰 초안은 `SavedStateHandle`에 저장

## 명령
- 빌드: `./gradlew assembleDebug`, 릴리스: `./gradlew assembleRelease`
- 테스트: `./gradlew testDebugUnitTest`, 린트: `./gradlew lintDebug`
- 설치: `./gradlew installDebug`, 릴리스 설치: `adb install -r app/build/outputs/apk/release/app-release.apk`
- 로그: `adb logcat -d -v time Nopo:D AndroidRuntime:E *:S`
- 무작위 조작: `adb shell monkey -p com.nopo.cheonnyeon --throttle 100 -v 5000`
- Windows PowerShell에서는 `.\gradlew.bat`

## 완료 정의
- 스토리 AC 통과, 해당 U 테스트 통과, 빌드 경고 증가 없음, ★ 스토리는 실기기 확인
- 스토리를 마치면 `nopo-qa` 서브에이전트로 검증

## 데이터
- 노포 카탈로그: `app/src/main/assets/stores.json` (생성: `python tools/build_catalog.py`)
- 좌표 보강: `python tools/geocode.py --provider naver|kakao` (주소 없는 가게는 kakao 이름 검색, `COORD_FROM_NAME` 표시)
- 공식 지정리스트 보충: `python tools/import_official_list.py --csv 파일.csv` → `python tools/build_catalog.py`
- 원본: `data/nopo_stores.csv`, 설명: `data/README.md`

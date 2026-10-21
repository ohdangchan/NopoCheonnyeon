# 노포 천년 미식가 — 앱 내려받기

<p align="center"><img src="docs/media/download_qr.png" width="260" alt="앱 내려받기 QR"></p>

- **QR을 폰 카메라로 찍거나** 아래 링크를 폰에서 열면 APK가 바로 내려받아집니다(안드로이드 8.0 이상, arm64).
  - https://github.com/ohdangchan/NopoCheonnyeon/releases/latest/download/nopo-cheonnyeon.apk
- 설치할 때 "출처를 알 수 없는 앱 설치"를 허용해 주세요(브라우저 또는 파일 앱 기준). 설치 후 위치·카메라 권한을 허용하면 모든 기능을 쓸 수 있습니다.
- 시연용 예시 리뷰·사진이 들어 있으며(설정 > 앱 정보 5번 탭 > 개발자 도구에서 끌 수 있음), 사진 출처는 앱의 오픈소스 라이선스 화면에 있습니다.
- 시연 시나리오: `docs/demo-guide.md`, 개발 인수인계: `docs/dev-status.md`.

---

# 노포 천년 미식가

전국 백년가게·서울 오래가게 1,277곳을 지도에서 찾아 **영수증(위치 미사용)** 또는 **가게 앞 30m 사진**으로 방문을 인증하고, 도장과 "세월(업력 합계)"을 모아 칭호를 올리는 안드로이드 앱입니다. 평점·리뷰, 유저 프로필, 가게별 재방문율을 지원합니다.

- 패키지 `com.nopo.cheonnyeon`, Kotlin · Jetpack Compose · MVVM, Firebase(Auth·Firestore·Crashlytics), 네이버 지도 SDK, CameraX, ML Kit 한국어 OCR
- **이어서 개발하는 사람·에이전트는 `docs/dev-status.md`를 먼저 읽으세요.** 문서 색인은 `docs/README.md`, 작업 규칙은 `CLAUDE.md`.
- 시작 안내(키 준비·데이터): `START-HERE.md`

## 빌드
```
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug   # Windows PowerShell
.\gradlew.bat assembleRelease                             # 서명 키(keystore.properties) 필요, 없으면 디버그 키로 서명
```
저장소에 없는 비밀 파일: `local.properties`(네이버 키), `app/google-services.json`, `keystore.properties`와 `.jks`. 자세한 내용은 `docs/dev-status.md` 3장.

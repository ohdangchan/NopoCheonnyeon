# 노포 천년 미식가 — 1일차 오전 기능 검증(스파이크) 계획 v0.4

> 최우선 목표는 **APK를 실제 폰에 설치했을 때 기획한 모든 기능이 버그 없이 동작**하는 것입니다. 이 오전 검증이 그 첫 관문입니다.

**목표**: 본 개발 전에, 빈 앱 하나에 **지도·카메라·한국어 글자 인식·위치** 네 가지만 넣고 **실제 폰**에서 도는지 확인한다. 여기서 막히는 것은 나중에 고치기 가장 비싼 문제들이다.

**결과물**: 설치 가능한 APK 1개, 진단 화면(최종 앱에 그대로 남김), `docs/spike-report.md`

**소요**: 약 4시간 30분 (09:00~13:30). 계정·서버를 처음부터 쓰므로 Firebase 확인(SPK-5)도 필수입니다.

---

## 0. 전날까지 준비 (막히면 오전이 통째로 날아감)

| 준비 | 확인 |
|---|---|
| Claude 유료 요금제(Pro 이상) 또는 Console 계정 — 무료 요금제는 Claude Code를 쓸 수 없음 | ☐ |
| Claude Code 설치 (`claude --version`), Android Studio 최신 안정판, JetBrains 플러그인 “Claude Code [Beta]” | ☐ |
| 폰: 개발자 옵션, USB 디버깅 켜기, PC와 USB 연결 후 `adb devices`에 표시 | ☐ |
| 네이버 클라우드: Maps 애플리케이션 등록 — **Dynamic Map 선택**, Android 패키지명 `com.nopo.cheonnyeon` 정확히 입력, Key ID 확보 | ☐ |
| Firebase 프로젝트와 `google-services.json`, 이메일 로그인 켜기, 보안 규칙 게시 (`nopo-07` 참고, 필수) | ☐ |
| 테스트 재료: 실제 영수증 1장, 한글 간판 사진 또는 인쇄물, `tools/test-images/`의 간판 이미지와 현재 시각이 찍히는 영수증(`test_receipt_live.html`을 PC 브라우저로 열기) | ☐ |

---

## 1. 일정

| 시간 | 작업 | 통과 기준 |
|---|---|---|
| 09:00–09:30 | SPK-0 프로젝트 생성, Claude Code 연결, 진단 화면 뼈대 | 빈 진단 화면이 폰에 설치됨 |
| 09:30–10:15 | SPK-1 지도 | 2-1 항목 |
| 10:15–11:00 | SPK-2 카메라 | 2-2 항목 |
| 11:00–11:45 | SPK-3 한국어 글자 인식 | 2-3 항목 |
| 11:45–12:30 | SPK-4 위치 | 2-4 항목 |
| 12:30–13:00 | SPK-5 Firebase 가입·로그인·쓰기·읽기 | 2-5 항목 |
| 13:00–13:30 | SPK-6 릴리스 APK 빌드·설치, 보고서 작성 | 릴리스 APK에서도 다섯 탭 모두 동작 |

---

## 2. 항목별 구현 범위·Claude Code 요청문·통과 기준

### SPK-0 프로젝트와 진단 화면 뼈대
Android Studio > New Project > **Empty Activity (Compose)**, 이름 `NopoCheonnyeon`, 패키지 `com.nopo.cheonnyeon`, Minimum SDK **API 26**, 빌드 언어 Kotlin DSL. 프로젝트 루트에 `CLAUDE.md`, `.claude/`, `docs/`, `tools/`, `firebase/`, `data/`를 복사한 뒤 Android Studio 터미널에서 `claude` 실행.

> **Claude Code 요청문**
> docs/nopo-06-spike-plan.md의 SPK-0을 해줘. feature/diagnostics 패키지에 DiagnosticsScreen을 만들고 상단 탭 4개(지도, 카메라, 글자 인식, 위치)를 둬. 탭 내용은 아직 “준비 중” 문구만. 화면 맨 위에는 기기 모델, Android 버전, 앱 버전을 표시해. MainActivity는 지금은 DiagnosticsScreen만 보여줘. 끝나면 ./gradlew assembleDebug 로 빌드하고 ./gradlew installDebug 로 연결된 폰에 설치해.

- [ ] 빌드 성공, 폰에 설치·실행됨
- [ ] 화면 회전 시 크래시 없음

### SPK-1 지도
> **Claude Code 요청문**
> SPK-1 지도 탭을 구현해줘. 네이버 지도 SDK 3.24.0을 settings.gradle.kts에 저장소 https://repository.map.naver.com/archive/maven 추가 후 의존성으로 넣고, AndroidManifest의 meta-data 이름 com.naver.maps.map.NCP_KEY_ID 값은 local.properties의 NAVER_MAP_KEY_ID를 manifestPlaceholders로 주입해. 지도는 공식 MapView를 AndroidView로 감싸고 생명주기를 빠짐없이 전달해(onCreate, onStart, onResume, onPause, onStop, onDestroy, onSaveInstanceState, onLowMemory — onCreate를 빠뜨리면 빈 화면). AGP 9 이상이면 org.jetbrains.kotlin.android 플러그인은 넣지 마. NaverMapSdk의 인증 실패 리스너를 등록해서 오류 코드를 화면에 표시해. 서울시청(37.5663, 126.9779)에 마커 1개, 그리고 “마커 1,000개” 버튼을 누르면 서울 주변에 무작위 마커 1,000개를 찍어 성능을 볼 수 있게 해줘. 끝나면 빌드하고 폰에 설치해.

- [ ] 지도 타일이 5초 안에 보인다
- [ ] 인증 오류 표시 없음 (401이면 패키지명, 429면 Dynamic Map 미선택, 800이면 Key ID 미지정)
- [ ] 확대·축소·이동이 부드럽다
- [ ] 마커 1,000개에서 끊김이 심하지 않다 (심하면 본 개발에서 클러스터링 필수로 표시)
- [ ] 다른 앱 다녀오기·화면 회전 후에도 지도가 정상
- [ ] 비행기 모드에서 앱이 죽지 않는다

### SPK-2 카메라
> **Claude Code 요청문**
> SPK-2 카메라 탭을 구현해줘. CameraX로 미리보기와 촬영 버튼 두 개를 만들고, 권한 요청·거부·“다시 묻지 않음”(설정 이동 버튼)을 처리해. ① “메모리로 촬영”은 OnImageCapturedCallback으로 ImageProxy를 받아 회전값과 해상도만 표시하고 파일을 만들지 말고 close() 해(본 개발의 영수증 경로). ② “파일로 촬영”은 앱 내부 저장소(filesDir/diag/)에 JPEG로 저장하고 썸네일과 촬영~저장 시간을 표시해(본 개발의 인증샷 경로). 회전 정보(EXIF)를 반영해 썸네일이 똑바로 보이게 해줘. 카메라는 화면 생명주기에 바인딩하고 화면을 떠나면 해제해.

- [ ] 미리보기가 1초 안에 뜬다
- [ ] 메모리 촬영 후 `files/`·`cache/`에 새 파일이 생기지 않는다 (Device Explorer)
- [ ] 파일 촬영 사진이 똑바로(세로/가로) 저장된다
- [ ] 권한 거부 → 안내, 다시 묻지 않음 → 설정 이동
- [ ] 앱 전환·전화 수신 후 돌아와도 미리보기 복구

### SPK-3 한국어 글자 인식
> **Claude Code 요청문**
> SPK-3 글자 인식 탭을 구현해줘. com.google.mlkit:text-recognition-korean:16.0.1(앱 포함 방식)을 넣고, 카메라로 찍은 사진을 긴 변 1,600px로 줄여 인식한 뒤 결과 텍스트 전체, 줄 수, 처리 시간(ms)을 표시해. 인식기는 ViewModel이 사라질 때 close() 해줘. 아래에 tools/test-images의 샘플 영수증 텍스트에서 전화번호와 결제일시를 정규식으로 찾아 보여주는 간단한 확인 영역도 넣어줘.

- [ ] 실제 영수증: 상호·전화번호·결제일시가 텍스트에 나온다
- [ ] 간판(한글): 가게 이름이 나온다
- [ ] 처리 시간 3초 이내 (중급 기기)
- [ ] 비행기 모드에서도 인식된다 (앱 포함 모델이므로)
- [ ] 연속 10회 인식해도 메모리 경고·크래시 없음

### SPK-4 위치
> **Claude Code 요청문**
> SPK-4 위치 탭을 구현해줘. 정밀·대략 위치 권한을 함께 요청하고, 사용자가 대략적 위치만 허용한 경우를 구분해서 표시해. 기기 위치 설정이 꺼져 있으면 켜기 대화상자를 띄워. “측정” 버튼을 누르면 Fused Location으로 1초 간격 업데이트를 최대 15초 받아서 가장 정확한 값을 고르고, 위도·경도·정확도(m)·첫 값까지 걸린 시간·모의 위치 여부(API 31 이상 isMock, 미만 isFromMockProvider)를 표시해. 목표 지점 위도·경도를 입력하면 거리(m)와 “30m 이내 & 정확도 30m 이하” 판정을 보여줘. 측정이 끝나면 위치 업데이트를 반드시 해제해.

- [ ] 실외에서 정확도 값 기록 (3회): ____ m / ____ m / ____ m
- [ ] 실내(가게 안)에서 정확도 값 기록 (3회): ____ m / ____ m / ____ m
- [ ] **실제 노포 문 앞**에서 가게 좌표까지 거리와 정확도: ____ m / ±____ m → 30m 판정 통과? ☐
- [ ] 대략적 위치만 허용 → “정확한 위치 필요” 표시
- [ ] 개발자 옵션 모의 위치 앱 → 모의 위치 감지
- [ ] 15초 후 업데이트가 멈춘다 (배터리)

### SPK-5 Firebase (필수)
> **Claude Code 요청문**
> SPK-5를 해줘. google-services.json을 app/에 두었어. Firebase BoM으로 Auth와 Firestore를 넣고, 진단 화면 다섯 번째 탭에 테스트 이메일·비밀번호로 가입/로그인 버튼, Firestore에 diagnostics/{uid} 문서를 쓰고 다시 읽는 버튼, 걸린 시간 표시를 만들어줘. 보안 규칙은 firebase/firestore.rules(진단 문서 diagnostics/{uid} 규칙 포함)를 콘솔에 붙여 넣었어.

- [ ] 가입·로그인 성공
- [ ] 쓰기·읽기 왕복 2초 이내
- [ ] 비행기 모드에서 쓰기 → 복귀 후 서버 반영

### SPK-6 릴리스 APK
> **Claude Code 요청문**
> 릴리스 서명 설정을 keystore.properties에서 읽도록 app/build.gradle.kts를 고쳐줘. 파일이 없으면 릴리스 빌드를 디버그 키로 서명해서(서명 없는 APK는 폰에 설치되지 않음) 바로 설치할 수 있게 해. R8을 켠 상태로 ./gradlew assembleRelease 하고, 생성된 APK를 adb install -r 로 폰에 설치해서 다섯 탭(지도·카메라·글자 인식·위치·Firebase)을 다시 확인할 수 있게 해줘.

- [ ] 릴리스 APK에서도 지도·카메라·글자 인식·위치·Firebase 모두 동작 (R8 문제 없음)
- [ ] APK 크기: ____ MB

---

## 3. 통과하지 못했을 때

| 증상 | 먼저 볼 것 | 대안 |
|---|---|---|
| 지도 회색·인증 오류 | NCP 콘솔 패키지명, Dynamic Map 선택, Key ID 주입, 저장소 URL | 그래도 안 되면 카카오맵 SDK v2로 같은 테스트(키 해시 등록 필요) |
| 카메라 검은 화면 | 권한, 생명주기 바인딩, 다른 앱의 카메라 점유 | `ImageCapture` 해상도 낮추기 |
| 한국어가 깨지거나 빈 결과 | 한국어 모델 의존성(라틴 모델이 아닌지), 이미지 회전 | 사진을 더 가까이, 밝게 |
| 위치 정확도가 늘 30m 초과 | 실내 여부, 기기 위치 정확도 설정, 15초 기다리기 | 결과를 기록하고 `PHOTO_MAX_DISTANCE_M`·`PHOTO_MAX_ACCURACY_M` 조정 검토 |
| 릴리스에서만 실패 | R8 keep 규칙 | 해당 SDK 문서의 keep 규칙 추가 |

**게이트**: SPK-1~SPK-5(기기 기능 네 가지 + Firebase)가 모두 통과해야 본 개발(스토리 S-101 이후)로 넘어간다. 실패 항목은 보고서에 원인과 결정을 적는다.

---

## 4. `docs/spike-report.md` 양식
```
# 1일차 기능 검증 보고서 (YYYY-MM-DD)
기기 A: 모델 / Android 버전 / Play 서비스 버전
기기 B: 모델 / Android 버전 / Play 서비스 버전

| 항목 | 기기 A | 기기 B | 메모 |
| SPK-1 지도 | 통과/실패 | | 마커 1,000개 체감: |
| SPK-2 카메라 | | | |
| SPK-3 글자 인식 | | | 처리 시간: ms |
| SPK-4 위치 | | | 실외 ±m / 실내 ±m / 노포 앞 거리 m |
| SPK-5 Firebase | | | |
| SPK-6 릴리스 APK | | | 크기 MB |

결정: (예) 30m 기준 유지 / 정확도 상한 조정 / 클러스터링 필수
```

# 보안 전수 조사와 조치 (2026-10-28)

범위: Firebase·Google Cloud 설정, Firestore 규칙, 앱 구성(매니페스트·저장·로그), 배포 경로(GitHub 릴리스·QR). 코드를 줄 단위로 모두 심사한 것은 아니고, 아래 항목을 점검했다.

## 1. 한 일
| 항목 | 내용 |
|---|---|
| AI 하루 한도 | `AiQuota`: 기기당 하루 60회. 넘으면 서버에 요청 없이 `E_AI_LIMIT` (2.0.3부터) |
| Firebase AI Logic | **인증된 사용자 모드 적용** — 로그인 정보가 없는 요청은 차단 |
| Firestore 규칙 | `receiptHashes`·`reports`·`diagnostics`에 필드 목록·길이 제한을 걸어 배포(콘솔 게시 2026-10-28 18:36). 규칙 시험 30개 통과(`tools/rules-test`) |
| API 키 | 웹용 `Browser key` 삭제(30일 안에는 콘솔에서 복구 가능). `Android key`에 앱 제한(패키지 `com.nopo.cheonnyeon` + 릴리스 서명 SHA-1 `6A:23:F2:12:D2:44:7A:8B:64:37:B9:10:46:57:32:6C:65:0D:6D:ED`) 추가. 반영까지 최대 5분 |
| 이메일 열거 보호 | 이미 켜져 있음(Authentication → 설정 → 사용자 작업) |
| 가입 속도 | 시간당 100(기본) |
| 저장소 | `redesign/v1`·`master`·`baseline/v0.2.0` 브랜치에 강제 푸시·삭제 금지 보호 설정 |
| QR | QR(`releases/latest/download/nopo-cheonnyeon.apk`)이 v2.0.0부터 404였다(파일 이름이 `nopo-passport-*.apk`로 바뀜). 릴리스마다 `nopo-cheonnyeon.apk` 이름의 사본을 함께 올려 고침 |

## 2. 문제 없음으로 확인
- 비밀 파일(`google-services.json`·키스토어·`.env`)은 git에 없고 과거 기록에도 없음. 코드에 키 문자열 없음.
- 매니페스트: 내보내는 컴포넌트는 `MainActivity`(런처)뿐, `FileProvider`는 비공개, 자동 백업은 DB·설정 저장소·설정 파일 제외.
- 로그: 위치·영수증·이메일·토큰을 남기지 않음. 사진 서버 응답 로그와 진단은 디버그 빌드 한정.
- 로그인 토큰은 Firebase Auth 라이브러리가 앱 전용 저장소에 보관한다. 앱이 비밀번호를 저장하지 않음. 계정 삭제는 비밀번호 재인증 필요.
- 릴리스 APK에는 네이버 Client Secret을 넣지 않음(빈 문자열). 오래된 릴리스 `v0.1.0-demo` APK에서도 `AIza` 형식 키는 Firebase Android 키 하나뿐(현재 APK와 같은 키)이었다.
- Realtime Database·Storage는 만들어져 있지 않음. 로그인 방법은 이메일·구글뿐(익명 없음).

## 3. 남은 위험·결정 사항
1. **설정 → 버전 5번 누르면 나오는 개발자 메뉴가 릴리스에도 있다.** 진단 화면, 시연 데이터 켜기, 내 위치에 테스트 가게 만들기·삭제, 시연 여권 도장. 모두 이 기기 안에서만 동작하고 서버에는 시연용 방문(`DEMO`)이 올라가지 않는다(규칙이 `RECEIPT|PHOTO|LOCATION_ONLY`만 허용). 시연에 쓰고 있으므로 그대로 두었다. 공개 배포 전에 `BuildConfig.DEBUG`로 막을지 결정할 것.
2. **방문·세월 값은 앱이 계산해 서버에 올린다(클라이언트 신뢰).** 규칙은 한 번에 올릴 수 있는 양(세월 ≤ 200, 방문 +1)만 제한한다. 직접 API를 호출하면 거짓 기록을 만들 수 있다. 서버 검증(Cloud Functions)은 Blaze 요금제가 필요해 하지 않았다. 랭킹이 중요해지면 도입.
3. **App Check는 켜지 않았다.** APK 직접 배포라 Play Integrity가 통과하지 않는다. Play 스토어(내부 테스트 포함)로 옮길 때 켠다.
4. 결제 계정 없음(Spark). Blaze로 올리기 전에 예산 알림·Gemini 할당량 상한·App Check를 먼저 설정한다.
5. 공개 저장소다. 키·시크릿이 올라가지 않도록 `.gitignore`(키스토어·`google-services.json`·`local.properties`)를 유지한다.

## 4. 배포 무결성(변조 확인)
- 서명 인증서 SHA-256: `de164afa0ba6587f5faf574887fb082fc1877686d745e8eb976d8a033fab5f84` (2.0.0 ~ 2.0.3 모두 같음). SHA-1은 `6A:23:F2:12:D2:44:7A:8B:64:37:B9:10:46:57:32:6C:65:0D:6D:ED`.
- 릴리스 노트에 APK의 SHA-256을 적는다. 확인: `certutil -hashfile 파일.apk SHA256`(Windows) 또는 `sha256sum 파일.apk`.
- 인증서 지문 확인: `apksigner verify --print-certs 파일.apk`. 지문이 위와 다르면 설치하지 않는다.
- 안드로이드는 같은 패키지를 다른 서명으로 덮어 설치하지 못하므로, 서명이 같은 APK만 업데이트로 설치된다.
- QR 주소는 최신 릴리스의 `nopo-cheonnyeon.apk`를 가리키므로, 릴리스마다 이 이름의 사본을 올린다.
- GitHub 계정에는 2단계 인증을 켠다(계정 설정이라 사용자가 확인).

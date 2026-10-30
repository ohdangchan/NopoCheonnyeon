# 노포 패스포트 — 개발 현황 (버전2)

> 최종 갱신 2026-10-30(**2.0.3 보안 강화 릴리스**, 5장 "2.0.2~2.0.3 세션 인수인계"가 가장 최신. 2.0.1 점검은 `v2-14-qa-2.0.1.md`). 브랜치 `redesign/v1`(`master`·`baseline/v0.2.0`·태그 v0.2.0은 건드리지 않음). 이 문서가 최신 인수인계이며, 옛 인수인계 상세는 `docs/dev-status.md`에 보관되어 있습니다.

## 1. 한눈에 보기
- 모든 시안 화면(1차 6장 + 2차 28장)을 앱으로 복제했고, 사용자 점검(총 3차 24건 + 최종 6건)을 반영해 푸시했습니다.
- 검증: 단위 테스트 통과, 린트 통과, 릴리스(R8) 빌드 통과(이전 확인), Galaxy S9+에서 주요 화면 점검. 디버그 콜드 스타트 약 3.4초(목표 2.5초, 릴리스 미측정).
- 릴리스: v2.0.0(2026-10-23) → 2.0.1 정리 → 2.0.2 온보딩 수정 → **v2.0.3 보안 강화(최신, 코드 6)**. 2.0.1(앱 `versionName` 2.0.1, 코드 4). 단위 테스트 331개 통과·린트 오류 0. 이번 환경에는 폰이 없어 실기기 확인은 하지 못했습니다.
- 화면 이미지·흐름도: `docs/v2/screens`, `docs/v2/screens_overview`(`python tools/make_overview.py`, `python tools/make_flow.py`). 평가자료: `docs/eval/`. 피그마: https://www.figma.com/design/IP2EihmKnZEYYoGUKrYeYG

## 2. 영역별 최종 상태
| 영역 | 최종 상태 |
|---|---|
| 로그인 | 이메일 + **구글만**(카카오·네이버·휴대폰 가입·게스트 둘러보기 제외). 로그인 성공 뒤 "로그인 중…" 유지, 프로필 확인 5초 제한 |
| 처음 실행 | 로딩(창 배경, 겹침 글자 정리) → 온보딩 3단계(시작 → 지역·음식 취향 → 준비 완료) → 로그인 |
| 홈 | 오늘의 AI 추천(취향 반영), 오른쪽 위 알림 벨, 내 주변 노포 칩 → 탐색 칩 연동 |
| 탐색 | "N년 전통" 표기 통일, 검색창 연두색 AI 원 버튼, 상단 돋보기 삭제 |
| 지도 | 네이버 지도, 상단 돋보기 삭제·검색창 AI 버튼, 테스트 가게 제외, 8초 안에 지도가 안 뜨면 목록으로 대체 |
| 가게 상세 | 저장하기 복원, 번역(소개글 먼저·리뷰 4개씩 동시), 길찾기 |
| 방문 인증 | 영수증 정보 확인 → 확정, GPS 인증(실제 지도·마커·큰 오류 팝업·10초 안내), 가게 사진 인증(큰 카메라·연두 스캔선), AI 안내 1.8초 제한·"다시 찍기" |
| AI 룰렛 | 머리글 유지, 완전한 원 룰렛, 큰 사진 릴, 분석 안내 팝업, 첫 응답 5초 이내(AI 4초 대기 후 기본 이유), 결과는 AI 이유 → 추천 노포 + 2곳, 최대 30곳 비중복 |
| AI 도우미 | 후보 축소·3문장 답변, "생각 중" 큰 글자 + 돌아가는 원 |
| 알림 | 항목을 누르면 가게 / 컬렉션 / 홈 / 마이로 이동 |
| 컬렉션 | 여권 펼침면 기울임(4°), 시연 패스포트 737년·후기 28개 |
| 마이·설정 | 설정 통계 4개가 버튼(각 화면으로 이동), 권한 관리, 도움말 센터(이메일 문의), 제목·닉네임 로고풍 굵은 글꼴, 사용 데이터 수집 기본 켜짐·마케팅 기본 꺼짐 |

## 3. 남은 일 (사용자 확인·결정)
1. **구글 로그인**: Firebase 콘솔에서 Google 사용 설정 + SHA 지문 + 새 `google-services.json` (`google-login-setup.md`)
2. **릴리스 서명 키 SHA** 등록
   - 리뷰 사진 앨범 권한: `CLAUDE.md`("저장소 권한 없음")와 현재 매니페스트(`READ_MEDIA_IMAGES`)가 다름 — 규칙을 고칠지 권한을 뺄지 결정
3. **고객센터 이메일** 운영 주소로 교체(지금은 `wow.sillyer@gmail.com` 임시)
4. **서울관광재단** 비짓서울 사진·이야기 글: 서면 허가 대기
5. **폴드8 지도 탭**: 실기기 미확인(방어 코드만 적용)
6. 실기기 영수증 실촬영·오프라인·두 번째 기기, 48dp·TalkBack·큰 글자

## 4. 명령 (Windows)
빌드 `.\gradlew.bat assembleDebug`, 테스트 `.\gradlew.bat testDebugUnitTest`, 설치 `.\gradlew.bat installDebug`, 릴리스 `.\gradlew.bat assembleRelease`. 이 PC(낮은 램)에서는 `/c/gt/b.sh <작업>`으로 한 번에 하나씩 실행.

## 5. 2.0.2~2.0.3 세션 인수인계 (2026-10-28~30) — 가장 최신
**한 일(푸시됨, 최신 커밋 `69e3595`, 릴리스 v2.0.3, 코드 6):**
- 2.0.2: 온보딩 2·3단계 글자 겹침 수정, "이전" 버튼, 추천 문구 변경.
- 2.0.3: `AiQuota`(기기당 하루 60회, UTC 날짜 기준, 초과 시 `E_AI_LIMIT`), Firestore 규칙 강화(`receiptHashes`, `reports`, `diagnostics`의 필드·길이 제한), 규칙 시험 30개(`tools/rules-test`).
- 콘솔(이미 적용): Firebase AI Logic 인증된 사용자 모드, GCP Android key 앱 제한(패키지+SHA-1), 웹용 Browser key 삭제(30일 복구 가능), Firestore 규칙 게시.
- GitHub: redesign/v1, master, baseline/v0.2.0 강제 푸시·삭제 금지. 릴리스마다 `nopo-cheonnyeon.apk` 사본을 함께 올려야 QR이 열린다(v2.0.0~2.0.1은 404였음).
- 과금: Spark 요금제, 결제 계정 없음 -> 청구 불가. 위험은 무료 한도 소진뿐. 상세 `v2-15-cost-guard.md`, `v2-16-security-audit.md`.

**미확인(사용자만 가능, 가장 먼저):** 2.0.3을 폰에 설치해 로그인 + AI(룰렛·챗봇) 1회 사용. 막히면: AI는 Firebase 콘솔 > AI Logic > 설정에서 인증된 사용자 모드 끄기, 로그인·Firestore는 GCP 콘솔 > 사용자 인증 정보 > Android key 앱 제한 해제. 이 PC에서는 adb가 PATH에 없었다(`%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`).

**사용자 결정 대기(묻지 않고 바꾸지 말 것):**
1. 개발자 메뉴(설정 > 버전 5번 탭)가 릴리스에도 있음 - 공개 배포 전 BuildConfig.DEBUG로 막을지.
2. 방문·세월 값은 앱이 계산(클라이언트 신뢰). 서버 검증은 Blaze + Cloud Functions 필요.
3. App Check는 Play 스토어로 옮길 때 켠다(APK 직접 배포에서 켜면 AI가 막힘).
4. Blaze 전환 전에 예산 알림·Gemini 할당량·App Check 먼저.
5. GitHub 2단계 인증 확인, 옛 릴리스 v0.1.0-demo 공개 유지 여부, 데모 시절 Gemini 키 폐기 여부.
6. 3장의 기존 항목(구글 로그인 설정, 고객센터 이메일, 비짓서울 서면 허가, 앨범 권한 규칙 불일치)도 그대로 남아 있다.

**이 PC 함정:**
- 빌드: `bash /c/gt/b.sh <작업>`(한 번에 하나, 로그 `/c/gt/build.log`, 릴리스 약 9분). 서명 SHA-256 `de164afa...ab5f84`(2.0.0~2.0.3 동일).
- 규칙 시험은 `tools/rules-test`에서 JAVA_HOME=/d/android/android_studio/jbr, TEMP/TMP=C:\gt, JAVA_TOOL_OPTIONS=`-Djava.io.tmpdir=C:\gt -Djdk.net.unixdomain.tmpdir=C:\gt`를 걸고 `firebase emulators:exec --config ../../firebase/firebase.json --project nopo-rules-test --only firestore "node rules.test.js"`. 없으면 Netty "Invalid argument: connect"로 실패.
- 콘솔 작업은 사용자가 로그인해 둔 내장 브라우저로 한다. 비밀번호·재인증은 사용자가 직접 입력. `google-services.json` 읽기는 권한이 막혀 있으니 우회하지 않는다. 브라우저 폭이 좁으면 resize_window 1300x900 후 desktop 프리셋으로 복원. 규칙 편집기는 CodeMirror 6(가상화)이라 전체 복사는 스크롤로 모은다.
- 한글 든 파일을 `python -c ... > 파일`이나 bash heredoc 파이썬으로 쓰면 깨진다 -> Edit/Write 도구 사용.
- 릴리스 절차: 버전 올림 -> 테스트 -> `b.sh assembleRelease` -> `apksigner verify --print-certs` -> 커밋·푸시 -> `gh release create vX.Y.Z`에 `nopo-passport-X.Y.Z.apk`와 `nopo-cheonnyeon.apk` 두 이름으로 업로드, 노트에 APK SHA-256.

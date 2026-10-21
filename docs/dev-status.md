# 개발 현황·인수인계 (이어서 개발하는 에이전트용)

> 최종 갱신: 2026-10-21 (맨 위 "★ 최신 인수인계" 참고; 아래 본문 일부는 2026-10-20 기준이라 낡았을 수 있음). **이 문서를 먼저 읽고**, 그다음 `CLAUDE.md` → `docs/nopo-01-PRD.md` 순서로 읽는다.
> 목표: **오늘 오후 해커톤에서 공기계에 설치해 심사위원에게 시연**. 기획된 기능 전부 구현 + QA 체크리스트 통과 문서(`docs/qa-report.md`).
> 이 문서 6장 "코드 점검에서 찾은 차이·미구현"은 이번 재독에서 새로 확인한 것이다. 문서와 코드가 다르면 **코드가 현재 사실**이고, 기준 우선순위는 `CLAUDE.md`의 PRD → 아키텍처 → 스토리 순이다.

## ★ 최신 인수인계 (2026-10-21) — 가장 먼저 읽을 것

**QA 검수 문서:** `docs/qa-final-2026-10-21.md`(기능별 검수표·보안 점검·결함·미확인 항목·시연 직전 체크리스트).

**배포(2026-10-21):** 저장소를 **공개**로 전환했고 릴리스 `v0.1.0-demo`에 APK(`nopo-cheonnyeon.apk`, 87MB, Gemini 키 포함 — 사용자가 노출을 감수하기로 함)를 올렸다. QR(`docs/media/download_qr.png`, 앱 공유 카드의 QR도 같은 주소)은 `https://github.com/ohdangchan/NopoCheonnyeon/releases/latest/download/nopo-cheonnyeon.apk`를 가리킨다. 새 APK를 배포하려면 `gh release create vX.Y.Z --repo ohdangchan/NopoCheonnyeon app-release.apk#nopo-cheonnyeon.apk`(latest 릴리스가 바뀌면 QR 주소는 그대로 유효). **시연 뒤에는 Gemini 키 폐기·교체를 권장.**

**상태:** P0·P1 요구사항과 이번 APK 범위의 기능은 모두 구현·푸시됨(최신 커밋은 `git log` 참고). 단위 테스트 약 275개·lint 오류 0. 남은 것은 대부분 **사용자만 할 수 있는 실기기 확인**과 공개 배포 전 정리다. 상세 변경 이력은 아래 10장 10-6~10-18.

**한 줄 요약(최근 세션):** 시연용 리뷰·사진·패스포트, 명예의 전당, 위치 시연 가게(시험용 가게), 공유 카드+QR, 칭호 11단계(100년 단위)+도감, 도장·승급 연출, 리뷰 사진 3장(카메라+앨범)·가게당 5개 리뷰, Gemini AI(메뉴 룰렛·리뷰 분석), Google Places 대표 사진.

### 시연 시나리오 (2026-10-21 정리)
- **단일 출처:** `docs/demo-guide.md` 3·3-1장 + `tools/test-images/demo_scenarios.html`(`python tools/gen_test_receipt.py`로 생성, 지난 그림은 자동 삭제). 영수증·간판·메뉴판은 모두 `노포천년테스트식당` 기준이며 이 가게는 설정 5탭 > 개발자 도구 "테스트 가게 만들기 (현재 위치)"로 **지금 서 있는 자리**에 만든다(위치 30m 시험 가능). 이전 문서의 `test_receipt_today/live` 이름은 폐기됨(`test_receipt_a/b/c`로 대체).

### 기기·계정
- 폰 ① Galaxy Z Fold SM-F971N(Android 17, adb `R5KL806MA4W`, 사용자 본인 폰 — 조작 중 알림이 뜨면 자동 조작 중단) ② **Galaxy S9+ SM-G965N(Android 10, adb `212340b20e017ece`, 1080×2220) — 시험 전용 새 폰.** 둘 다 디버그/릴리스 APK가 같은 릴리스 키로 서명돼 덮어 설치(`adb install -r`)된다.
- S9+에는 시험 계정(닉네임 `s9t5017`)이 로그인돼 있다. 자격 증명은 이 PC의 에이전트 작업 폴더에만 있고 저장소·문서에 없다. 사용자 본인 계정(`nptNew1`)으로 바꾸려면 설정에서 로그아웃 후 로그인.
- 폰 자동 조작은 `uiautomator dump`(텍스트)만 쓴다(스크린샷은 위치 노출 위험 — 연출 화면 등 안전한 화면에서만 예외). 스크립트는 임시 폴더라 저장소에 없음: 텍스트 탭/스크롤은 `adb shell uiautomator dump` + `input tap` 조합으로 쉽게 다시 만든다(한글 입력은 adb로 불가 → 영문·숫자만).

### 빌드·키 (중요)
- Gemini 키는 사용자 환경변수 `GEMINI_API_KEY`. gradle은 `providers.environmentVariable`로 읽어 `BuildConfig.GEMINI_API_KEY`에 넣는다(없으면 AI 기능이 꺼지고 안내만). 셸에 변수가 안 보이면: `export GEMINI_API_KEY="$(powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('GEMINI_API_KEY','User')" | tr -d '\r\n')"` 후 `./gradlew.bat ...`. Google Places 키는 `GOOGLE_PLACES_KEY`(`tools/fetch_google_places.py`, 사진은 `.gitignore`라 새로 받아야 함). **키를 코드·문서·로그·채팅에 쓰지 말 것.**
- 릴리스 빌드(`assembleRelease`, lint 포함 5~12분)는 메모리를 많이 써서 PC가 불안정할 때 주의(백그라운드 실행 후 완료 알림 대기). APK 약 85MB(arm64).
- Firestore 규칙은 `firebase/firestore.rules` → `cd firebase && firebase deploy --only firestore:rules --project nopocheonnyeon`(배포됨, 사용자 승인 하에 진행). 규칙 시험 `tools/rules-test/`는 에뮬레이터 jar를 내려받지 못해 **아직 한 번도 실행하지 못함** — 다운로드가 되는 PC에서 `cd firebase && firebase emulators:exec --only firestore "node ../tools/rules-test/rules.test.js"` (18개 시나리오, 실패하면 규칙 또는 시험을 고칠 것).

### 사용자가 직접 해야 하는 일 (막힘)
1. 영수증 실촬영·인증 성공/실패/24시간 경과/타인 영수증, 인증 직후 "평점·리뷰 남기기" 자동 열림, 리뷰 사진 카메라·앨범 3장, 한 가게 리뷰 여러 개(5개 한도), 새 서버 규칙으로 **실제 가게 리뷰 저장이 되는지**(규칙을 강화해 두고 아직 실기기 저장을 못 봄 → 거부되면 `firestore.rules`의 `validReview()`부터 의심).
2. 공개 배포 전: **Gemini 키 제한**(앱이 키를 품고 있음 — Generative Language API로 제한·예산 알림, 가능하면 Firebase AI Logic/서버 경유), Google Places 사진 약관(저장·재배포 제한), 공유 카드 QR 주소(`platform/share/ShareConfig.DOWNLOAD_URL`, 지금은 비공개 저장소 릴리스 페이지라 남들은 못 엶), 서명 키 백업 2곳, 노출된 Stitch/네이버 키 폐기, 시연 후 예시 데이터 끄기(설정 앱 정보 5탭 > 개발자 도구).
3. 구형 기기(S9+)에서 큰 글씨·다크 모드·Monkey(개인 폰이 아니면 `adb shell monkey -p com.nopo.cheonnyeon --throttle 100 -v 5000`).

### 알려진 한계·다음 후보 (우선순위 순)
1. QA 에이전트 지적 중 미해결: 화면 회전 시 선택한 리뷰 사진 유실(용량 때문에 Bundle 미사용 → ViewModel로 옮기기), 리뷰 목록이 사진 base64 전체를 읽음(썸네일 필드 도입), `reviewCount`·`totalYears` 위조 가능(Cloud Functions 필요).
2. 룰렛·분석은 키가 있는 빌드에서만 동작. 모델은 `GeminiClient.DEFAULT_MODEL`(`gemini-3.1-flash-lite`) 한 곳. 응답 지연·한도 초과 시 `E_AI_FAILED`.
3. Stitch 시안 보강(지도 레이어·북마크, 패스포트 타임라인, 화면별 @Preview 확대), 화면별 상태 분리, 큰 글씨·TalkBack 점검, 성능 측정(콜드 스타트 2.5초 등).
4. 범위 밖(PRD P2): QR 인증, 서버 판정, 사장님 상생, 추모관 등(`nopo-12-unimplemented.md`).

### 작업 규칙 (사용자 지시 요약)
- 모든 응답·진행 안내는 **한국어**. 사용자에게 묻지 말고 개발을 이어가되, 키·비밀번호는 노출하지 않는다. 컴퓨터가 꺼질 수 있어 **큰 단계마다 커밋·푸시**(커밋 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`). 스토리 끝나면 `nopo-qa` 서브에이전트로 검증. 한도(주간 99%/세션 95%)에 닿으면 푸시·이 문서 갱신 후 대기.
- 도구 함정: Git-Bash heredoc에 한글/따옴표가 섞이면 깨진다 → 스크립트는 파일로 만들어 실행. `adb` 경로 인자에 `MSYS_NO_PATHCONV=1`. 다중 디스플레이(폴드)에서 `screencap`은 PNG 앞에 경고 텍스트가 붙으니 `\x89PNG` 위치부터 잘라 저장.

## 0. 사용자 지시 모음 (시간순, 그대로 따를 것)
**2026-10-20 이전 세션에서 받은 지시 (이 문서 이전 판에서 옮김)**
- 모든 것은 사전 승인됨. 사용자에게 묻지 말고 기획안(PRD)에 적힌 모든 것을 개발하고 QA까지 진행한다.
- 사용자가 직접 해야 하는 시험(실제 영수증 촬영, 실제 가게 앞 30m 인증)은 "통과했다고 가정"하고 개발을 계속하며, **마지막 QA 문서에 수동 시험 항목으로 넣는다.**
- 사진 없는 가게는 글자 카드로 대신한다. 이후 개선안: 웹에서 사진을 내려받아 앱에 포함 또는 구글 Places API (1,277곳×3장 ≈ $20, 약관 확인 필요).
- 글자 크기는 알아서 (본문 ≥14sp, 짧은 라벨 ≥12sp).
- 한도 도달 시: 세션 95% 또는 주간 99%가 되면 그때까지의 모든 것을 커밋·푸시하고 이 문서를 최신으로 고쳐 다른 에이전트가 바로 이어받게 한다. 세션 한도가 풀리면 주간 99%까지 계속 진행.
- 모든 채팅·진행 안내는 **한국어** (자동 메모리 `korean-only-replies`에도 저장됨).

**이번 세션(2026-10-20, 모델을 Sonnet 5.5로 바꾼 직후)에서 받은 지시**
1. "이전 대화 기억해?" → 대화 자체는 기억하지 못하고 저장소·메모리·커밋으로 아는 것만 안다고 답함.
2. "이 폴더의 모든 문서와 코드를 하나도 빠짐없이 읽고 분석해서, 지금까지의 작업 기록과 내가 내린 지시까지 정리해 다른 에이전트가 이어서 진행할 수 있도록 철저히 준비해줘." → 이 문서가 그 결과.
3. "지금까지 개발한 모든 문서들 https://github.com/ohdangchan 여기에 푸시 커밋 싱크 해줘." → 원격 `origin = https://github.com/ohdangchan/NopoCheonnyeon.git` (이미 연결돼 있음). 비밀 파일은 `.gitignore`로 제외된 상태이며 커밋에 포함하지 않았다.
4. (2차 지시) "다른 에이전트가 할 거니까 너는 인수인계 준비를 더 철저히. **Stitch 화면 파일들까지 생성된 모든 파일을 반드시 다 읽고** 완벽히 준비. `docs/spike-report.md` 등 필요한 문서를 모두 생성. QA 목록을 다시 확인해 **실기기 테스트가 필요한 모든 부분을 전부 정리해 다시 만들 것**." → 아래 "읽은 범위"와 신규 문서(`spike-report`, `nopo-09`~`nopo-11`, `apk-install-guide`, `privacy-notice`, `docs/README`)가 그 결과. 이 세션에서는 **기능 코드를 수정하지 않았다**(다른 에이전트가 이어서 구현).

### 이번 인수인계에서 읽은 범위 (2026-10-20)
- 전부 읽음: `CLAUDE.md`, `START-HERE.md`, `docs/*.md` 전부, `design/stitch/PRD-차이점.md`·`README.md`, `data/README.md`, `.claude/*`, `firebase/firestore.rules`, Gradle 파일·`libs.versions.toml`·매니페스트·`rules.keep`, `strings.xml` 342개, 앱 코드 전체(도메인·유틸·데이터·플랫폼·기능 화면·디자인 시스템·진단 탭), 테스트 함수 목록.
- 텍스트 추출 후 전부 읽음: Stitch 화면 51개 `code.html`(보이는 문구), `docs/nopo-planning.html`(하반기 장 위주 정독, 상단 장은 PRD와 같은 내용). 화면 이미지는 `receipt_verify`·`map` 두 장만 직접 확인.
- **훑기만 함(내용 신뢰 안 함)**: `design/stitch/reference/` 43개 폴더(검증되지 않은 "완료" 주장 문서, `DESIGN.md`만 코드와 대조해 유효 확인), `data/raw/*.jsonl`·CSV·SQLite·`stats.json`(수치만), `tools/*.py` 본문(머리 설명만), PNG 이미지 대부분(`screen.png` 51장 중 2장), `design/stitch/screens/*/meta.json`.


## 1. 프로젝트 요약
- 앱: 노포 천년 미식가 (`com.nopo.cheonnyeon`). 전국 백년가게·서울 오래가게 1,277곳을 지도에서 찾아 **영수증(위치 미사용) 또는 가게 30m 이내 사진**으로 방문 인증 → 도장 → "세월(업력 합계)" 적립 → 칭호. 평점·리뷰·유저 프로필.
- 스택: Kotlin 2.2.10, Jetpack Compose(BOM 2026.02.01), AGP 9.4.1, Gradle 9.6.0, minSdk 26 / target·compile 37, 단일 모듈 `:app`. MVVM + 단방향 흐름, 수동 DI(`app/AppContainer.kt`), Room 2.8.5(DB v2), DataStore, WorkManager, Coil 3, 네이버 지도 SDK 3.24.0, CameraX 1.6.2, ML Kit 한국어 OCR 16.0.1(앱 포함), Fused Location 21.4.0, Firebase BoM 34.19.0(Auth·Firestore·Crashlytics). **버전을 임의로 올리지 않는다** (`gradle/libs.versions.toml`).
- 서버: Firebase 프로젝트 `nopocheonnyeon` (계정 songpa24@iceu.kr), Firestore `(default)` **asia-northeast3**, 규칙·색인은 `firebase/`.
- 카탈로그: `app/src/main/assets/stores.json` v26101921 — 1,277곳, 좌표 1,181곳(92.5%), 공식 사진 86곳, 음식점 834곳. 생성: `python tools/build_catalog.py`.
- 저장소 규모: 추적 파일 약 455개. 앱 코드 `app/src/main/java/com/nopo/cheonnyeon` 약 8,300줄(테스트 포함), 단위 테스트 19개 파일/145케이스, 문자열 리소스 342개.

## 2. 문서 지도 (무엇을 어디서 읽나)
| 파일 | 내용 | 상태 |
|---|---|---|
| `CLAUDE.md` | 작업 규칙(필독): 스토리 단위, 테스트 먼저, 금지 사항, 명령 | 최신 (v0.4) |
| `START-HERE.md` | 패키지 안내·키 준비·데이터 현황 | 2026-10-18 기준, 좌표 "0"은 낡음(지금 1,181) |
| `docs/nopo-01-PRD.md` | 요구사항 80개, 규칙값 `VerificationConfig`, 오류 코드(7.4), 결정 D-01~D-14 | 기준 문서 |
| `docs/nopo-02-architecture.md` | 구조·보안 규칙·Gradle·ADR | 일부 낡음 (6장 참고) |
| `docs/nopo-03-stories.md` | 스토리 76개·일정·컷라인 | 체크 표시 안 됨 (진행도는 이 문서 4장) |
| `docs/nopo-04-dev-qa-checklist.md` | U/I/M/S/R 항목, 디버깅 포인트 | 체크 표시 안 됨 (결과는 `qa-report.md`) |
| `docs/nopo-05-precheck-checklist.md` | 사용자가 직접 할 일 40개 | 참고 |
| `docs/nopo-06-spike-plan.md` | 1일차 기능 검증 계획 | 결과는 `spike-report.md` |
| `docs/nopo-07-firebase-setup.md` | 서버 설정. 0장에 실제 연결 정보·nam5 함정 | 최신 |
| `docs/nopo-08-claude-code-guide.md` | Claude Code 작업 루프 | 참고 |
| `docs/qa-report.md` | 2026-10-20 QA 결과(PASS/수동/보류) | 최신 |
| `docs/demo-guide.md` | 해커톤 시연 절차·문제 대처·발표 수치 | 최신 |
| `docs/nopo-planning.html` | 기획 전체 24장 한 장 문서 | PRD와 다르면 PRD 우선 |
| `docs/archive/` | 구버전 보관, **참조 금지** | — |
| `docs/README.md` | docs 색인(신규) | 최신 |
| `docs/spike-report.md` | 1일차 기능 검증 결과(소급, 수치 재측정 필요) | 신규 |
| `docs/nopo-09-real-device-qa.md` | **실기기 QA 전체 목록과 상태(기준)** | 신규 |
| `docs/nopo-10-stitch-implementation-map.md` | Stitch 51화면 ↔ 구현 현황 | 신규 |
| `docs/nopo-11-backlog.md` | 결함·누락·데이터·문서 정리 백로그 | 신규 |
| `docs/apk-install-guide.md` | APK 설치 안내(S-1006) | 신규 |
| `docs/privacy-notice.md` | 개인정보 안내 초안(운영자 정보 비움) | 신규 |
| `design/stitch/README.md`, `PRD-차이점.md` | Stitch 51화면 매핑과 PRD와 다른 점. **화면을 만들기 전에 반드시 확인** | 최신 |
| `design/stitch/reference/` | 검증 안 된 "완료" 주장 문서 모음. **스크립트 실행 금지**(규칙·strings.xml을 덮어씀) | 위험 |
| `data/README.md` | 데이터 열 설명·수집 방법·한계 | 수치는 수집 시점(좌표 0)이라 낡음 |
| `.claude/settings.json`, `.claude/agents/nopo-qa.md` | 명령 허용/키 파일 읽기 차단, QA 서브에이전트 | 최신 |

## 3. 비밀 파일 (저장소에 없음, 이 PC에만 있음)
| 파일 | 용도 | 없으면 |
|---|---|---|
| 환경변수 `GEMINI_API_KEY` / `GOOGLE_PLACES_KEY` | AI 기능(BuildConfig 주입) / 대표 사진 받기 | AI 기능 꺼짐 / 사진 못 받음 |
| `local.properties` | `NAVER_MAP_KEY_ID`, `NAVER_MAP_CLIENT_SECRET` (`local.properties.sample` 참고) | 지도 인증 실패 |
| `app/google-services.json` | Firebase 설정 (`firebase apps:sdkconfig ANDROID 1:401283218421:android:1cf2a80d59a68bf853bebc -o app/google-services.json`) | 빌드 실패 |
| `keystore.properties` + `C:/Users/HB_OPERATION_NICK/AndroidStudioProjects/jks/nopo_release_key` | 릴리스 서명 (샘플: `app/keystore.properties.sample`) | 릴리스가 디버그 키로 서명됨 |
- `.stitch-key`, `STITCH_API_KEY`, `NCP_KEY*`, `KAKAO_REST_KEY`는 환경 변수/무시 파일로만. 채팅에 노출된 Stitch 키·네이버 IAM 키는 사용자가 폐기해야 한다. `.claude/settings.json`이 위 비밀 파일 읽기를 막는다.

## 4. 진행 상태

### 4-1. 스토리 (커밋 메시지 `S-xxx: 요약`, 26개 커밋, 전부 2026-10-19~20)
완료: E0 진단(SPK-0~6, S-001~006) · E1 기반(S-101~106) · E2 도메인 규칙 전부(S-201~212) · E3(S-301~307) · E4(S-401~408 모두, P1 포함) · E5(S-501~506) · E6(S-601~609) · E7(S-701~704) · E8(S-801~805, P1 포함) · E9(S-901~903) · S-1002(릴리스 서명·R8).
미완/부분: S-008 보고서(`docs/spike-report.md` 소급 작성됨, **측정 수치는 미기록 → 재측정 필요**), S-308(규칙 플레이그라운드 I-1~I-10 점검 기록 없음 — 배포는 됨), **S-705 칭호 승급 연출(P1, 텍스트 한 줄만)**, S-1001(공식 지정리스트 CSV 미반영, 좌표 없는 96곳 미보강), S-1003~S-1006(실기기 QA 일부만, Monkey 미완주, 최종 배포 안내 미작성).

### 4-2. 커밋 이력 (최신순 요약)
`d37ce99` Crashlytics(릴리스만)·R8 `-dontoptimize`·릴리스 실기기 확인 → `f7e651d` 오프라인 배너·확인 중 도장·업력 확인 중·인증샷 저장·설정 계정 정보 → `9c0b497` 닉네임 변경/탈퇴·리뷰 신고·시도별 클러스터 원·디버그 가상 방문 → `0b5e39d` 유저 프로필·내 리뷰 탭 → `219802e` dev-status 첫 판 → `40fe500` 서명 값 trim → `8a4b0cc` 온보딩·설정 → `49d289b` 리뷰·서버 복원·로그아웃 정리 → `69f1098` 패스포트·도장 연출 → `0d2df2b` 방문 기록·동기화·인증 화면·테스트 가게 → `2b02768` 지도·상세 → `dea13fb` 가입/로그인 → `49988ec` 사진 서버 Accept 헤더 → `95874b8` 공식 사진 → `2579e3c` 좌표 1,181곳 → `15c44c6` Room·카탈로그 → `e20d2e8` 기반·도메인 규칙 → 진단 탭들(`893454c`,`bd7a3c7`,`778710b`) → `22af130` 진단 뼈대 → `57e9c5b` 초기 설정.

### 4-3. 이번 세션의 미커밋 작업 (이 인수인계 커밋에 포함)
- `DiagnosticsSampleOcr.kt`(신규) + `DiagnosticsOcrTab.kt` 한 줄 연결 + `app/src/debug/assets/test_receipt.png`·`test_sign.png`(신규): **디버그 전용** — 카메라 없이 샘플 영수증·간판 이미지를 실제 ML Kit로 읽어 파서·판정을 돌려 보는 버튼 2개. 문구가 하드코딩(한글)이지만 디버그 전용이라 허용된 예외로 둔다. 릴리스에는 자산이 없고 `BuildConfig.DEBUG` 가드가 있다.
- `.idea/gradle.xml`(Android Studio 자동 변경)·`.idea/vcs.xml`(신규): IDE 설정, 기능 영향 없음.

### 4-4. PRD 기능 구현 현황 (요구사항 ID 기준)
| 영역 | 상태 | 비고 |
|---|---|---|
| SPK/TST 진단·테스트 가게 | 완료 | 설정 > 앱 정보 5탭. 진단 탭: 지도·카메라·OCR·위치·Firebase·사진·디자인 7개 |
| AUTH-01~10 | 완료 | 가입(닉네임 중복 실시간)·로그인·재설정·로그아웃 경고·게이트·복구·닉네임 변경·탈퇴. 탈퇴·신고의 실기기 시험은 수동 |
| MAP-01~07 | 완료 | 줌<9에서 시·도별 개수 원, 이상은 화면 영역 마커 최대 250개, 검색어 있으면 목록 고정 |
| STORE-01~05 | 완료 | 사진 페이저(없으면 글자 카드), 보상 박스, 네이버 지도 길찾기(앱 → 웹 대체), 전화 걸기(P1)도 구현 |
| STORE-06 정보 오류 신고 | **미구현 (P1)** | 리뷰 신고(FR-REV-08)만 있음 |
| VER-01~16 | 대부분 완료 | **VER-03 영수증 가이드 틀 오버레이 미구현**. 갤러리 선택 없음(준수) |
| STP-01~05, 07 | 완료 | STP-06 승급 연출은 P1 미완 |
| PAS-01~04 | 완료 | PAS-05 공유 카드는 P2 |
| REV-01~08 | 완료 | 편집기는 별도 화면이 아니라 `AlertDialog` (6장) |
| PRF-01~06 | 완료 | PRF-07 비공개 설정 P1 미구현 |
| DATA-01~04 | 완료 | 좌표 없는 96곳은 영수증 인증만 가능(`E_NO_COORD`) |
| SYS-01~04 | 완료 | 설정 > 라이선스는 접이식 텍스트, 개인정보 안내 텍스트 |

## 5. 코드 지도 (`app/src/main/java/com/nopo/cheonnyeon`)
- `NopoApp.kt` — StrictMode(디버그), Crashlytics 수집은 `!DEBUG`에서만, Coil `ImageLoader`에 `Accept: image/*` 헤더(사진 서버 sbiz24 500 회피), 시작 시 `container.syncPending()`.
- `app/AppContainer.kt` — 수동 DI. 카탈로그 적재(백그라운드) → `catalog.invalidate()`. 로그인되면 `visits.restoreFromServer()` + 동기화. `signOut()`이 방문·대기열·`filesDir/proofs/` 삭제. `sessionState`는 `Eagerly`로 `Loading` 시작.
- `core/navigation/NopoNavHost.kt`, `Routes.kt` — 세션 상태로 그래프 통째 전환(`popUpTo(0)`). 경로: splash·onboarding·login·sign_up·reset·nickname·home·`store/{id}`·`verify/{id}`·`profile/{uid}`·diagnostics. 하단 탭(지도·패스포트·설정)은 `feature/main/MainScreen.kt`가 상태로 처리(별도 nav 경로 아님). 로그인 화면에는 디버그 빌드에서만 진단 진입 버튼.
- `core/common` — `AppError`(PRD 7.4 코드 25개, 문구는 `strings.xml`의 `err_<소문자코드>`), `AppResult`, `Clock`/`SeoulZone`, `DispatcherProvider`, `factoryOf`.
- `core/util` — `GeoDistance`(하버사인), `TextNormalizer`, `Similarity`(`names`=전체 편집거리, `partial`=간판 속 부분 일치, Damerau-Levenshtein), `BusinessNumber`(체크섬), `PhoneNumber`.
- `domain/rule` — `VerificationConfig`(**기준값 단일 위치**, 아래 표), `YearsCalculator`(바닥 30, 미래 창업연도는 `require` 예외), `StampGradePolicy`, `TitlePolicy`, `DailyLimitPolicy`(서울 자정·테스트 가게 제외), `ReviewRules`, `AuthValidation`.
- `domain/verify` — `ReceiptParser`(전화·사업자번호·결제일시·금액·상호), `ReceiptMatcher`(가게 확인 → 날짜), `ReceiptHasher`(SHA-256, 분 단위), `PhotoMatcher`, `VerifyReceiptUseCase`(object, 순수 함수).
- `domain/catalog/StoreFilter.kt` — 필터·검색·정렬·영역. `domain/model` — Store, Title, StampGrade, VerifyMethod, SessionState, VisitRef.
- `data/local` — Room(`stores`, `my_visits`, `pending_ops`; **DB v2, `fallbackToDestructiveMigration`**), `Daos.kt`, `SettingsStore`(DataStore: 온보딩·카탈로그 버전·음식점 필터). `data/catalog` — `CatalogSeeder`(버전이 같으면 건너뜀, 한 트랜잭션 교체), `CatalogRepository`(메모리 캐시 + `changes` 카운터 + 테스트 가게 생성 `TEST-NOPO`).
- `data/visit` — `VisitRepository`(기기 먼저 기록 → `pending_ops` → Firestore 배치, 서울 0시로 자른 시각 저장, 복원, 중복 영수증 되돌림), `SyncWorker`/`SyncScheduler`(`enqueueUniqueWork("sync", APPEND_OR_REPLACE)`, 네트워크 제약, 지수 백오프 30초).
- `data/remote` — `AuthRepository`(세션 Flow, 가입·복구·변경·탈퇴, 15초 타임아웃), `ReviewRepository`(집계 쿼리 평균·개수, 20개 페이징, 저장/삭제/신고), `ProfileRepository`.
- `platform` — `camera/CameraCapture`(메모리 촬영 `captureInMemory`는 파일 안 만듦, `captureToFile`), `location/FusedLocationAdapter`(`bestFix` 15초·정확도 ≤10m 조기 종료), `map/NaverMapView`(생명주기 8개 전달), `map/ReverseGeocoder`(진단용), `network/ConnectivityMonitor`, `ocr/MlKitKoreanRecognizer`, `permission/PermissionState`(ON_RESUME 재확인).
- `feature` — `auth`(온보딩·로그인·가입·재설정·닉네임), `main`(탭·오프라인 배너·로그아웃 경고·**테스트 가게 버튼**·**디버그 가상 방문 버튼**), `map`(MapScreen/ViewModel/StoreSheet/목록), `store`(상세·리뷰 섹션·사진), `verify`(VerifyViewModel/Screen: RECEIPT→LOCATE→PHOTO→DONE, 도장 연출 포함), `passport`(도장 격자·상세 다이얼로그·내 리뷰), `profile`, `settings`, `diagnostics`(7탭).
- `core/designsystem` — 토큰(Color/Type/Dimens/NopoTheme), 컴포넌트(StampView·YearsGauge·TitleBadge·StarRating·NopoButton·EmptyState·OfflineBanner), `DesignCatalogScreen`(진단 "디자인" 탭).
- 테스트: `app/src/test` 19개 파일 — 순수 도메인·유틸·검증만. ViewModel·Repository·Room·Compose UI 테스트는 **없음**(`androidTest`는 템플릿 예제뿐).

`VerificationConfig` 값: 영수증 24시간·미래 허용 10분·상호 유사도 ≥0.70(전화 일치 OR), 사진 거리 ≤30m·정확도 ≤30m·글자 유사도 ≥0.60·글자 3회 실패 후 위치 확인 도장 허용, 위치 타임아웃 15초(조기 종료 10m, 간격 1초), 하루 5곳.

## 6. 코드 점검에서 찾은 차이·미구현 (이번 재독 결과, 우선순위 순)
> 전체 목록·완료 조건은 **`docs/nopo-11-backlog.md`**(B-01~B-24, D-01~, C-01~). 아래는 핵심 요약이며 B-02 이후 항목(중복 알림 미수집, 위치 켜기 대화상자, 진단 정보 복사·테스트 기록 삭제 누락, 프로필 단순화)은 backlog에만 자세히 있다.
0. **(최우선) 서버가 중복 영수증을 거부해도 사용자에게 알리지 않는다** — `VisitRepository.duplicates` 흐름을 수집하는 코드가 없다(B-02). "확인 중" 도장이 조용히 사라진다.
1. **중복 영수증 판정 로직이 아키텍처 5.3·U-306-1과 다름** — `VisitRepository.sync()`는 `PERMISSION_DENIED` + `receiptHash != null`이면 곧바로 "중복 영수증"으로 보고 기기 기록을 지운다. 문서는 되돌리기 전에 `users/{uid}/visits/{storeId}`를 서버에서 한 번 읽어, 이미 있으면 성공으로 처리하라고 한다(응답만 못 받은 재전송, 또는 다른 이유의 거부를 오판하지 않기 위해). **미구현.** 시연 영향은 낮지만 데이터 손실 위험이 있어 먼저 고칠 가치가 있다. 재방문(`firstVisit=false`)에 영수증이 거부되면 `visitDao.delete`는 안 하고 알림만 보낸다(의도된 동작으로 보임).
2. **자동 테스트 공백** — nopo-04의 U-VM-*, U-301/302/306(Room·Repository·동기화) 항목에 해당하는 테스트가 없다. `testDebugUnitTest` 145개는 전부 도메인·유틸이다.
3. **`MapViewModel.totalYears`가 테스트 방문까지 합산** (`visits.sumOf { it.yearsAwarded }`). 패스포트·상세는 `TEST-` 제외인데 지도 상단 "내 세월" 알약만 테스트 가게 방문을 포함해 값이 어긋난다. `PassportViewModel`처럼 `storeId.startsWith(Store.TEST_PREFIX)`를 거를 것.
4. **리뷰 저장·삭제가 트랜잭션이 아니라 배치** (`ReviewRepository.save/delete`). 문서(아키텍처 5.4)는 트랜잭션. 배치는 오프라인에서 큐에 쌓였다가 나중에 거부될 수 있으나, 현재는 `withTimeout 12s`와 `UNAVAILABLE → E_REVIEW_OFFLINE` 매핑으로 대응한다. 타임아웃 시에도 `E_REVIEW_OFFLINE`으로 변환된다(`CancellationException` 분기). 오프라인 실기기 시험(M-49)은 아직 안 했다.
5. **리뷰 편집기·도장 연출이 문서 구조와 다름** — `ReviewEditorScreen`/`StampCeremonyScreen`/`StampDetailSheet`는 별도 화면이 아니라 각각 `StoreReviewsSection`의 `AlertDialog`, `VerifyScreen.ResultSection`, `PassportScreen.StampDetail`(AlertDialog). 기능은 있으나 Stitch 시안보다 단순하다.
6. **미구현 UI**: 영수증 촬영 가이드 틀(FR-VER-03), 칭호 승급 연출(S-705), 정보 오류 신고(FR-STORE-06), 방문 기록 비공개(FR-PRF-07), 개인정보 처리방침 별도 화면(현재 설정 화면 텍스트), 라이선스 목록 자동 생성(현재 고정 문자열 + `assets/licenses/*.txt` 2개), `Haptics` 클래스(진동은 `VerifyScreen`에 직접 구현), `Lottie`는 쓰지 않음.
7. **문서가 낡은 부분(고칠 것)**: ① 아키텍처 6.1은 `fallbackToDestructiveMigration`을 쓰지 말라고 하나 실제로는 사용 중(출시 전 정식 마이그레이션 필요, DB v2). ② 아키텍처 6.3 규칙 예시는 users 업데이트에서 `nickname == resource.data.nickname`을 요구하지만 **실제 `firebase/firestore.rules`는 닉네임 변경을 허용**하도록 바뀌어 있다(규칙 파일이 정본). ③ 아키텍처 7.3의 `abiFilters`는 `arm64-v8a, armeabi-v7a`이나 실제는 `arm64-v8a`만(D-15). ④ 아키텍처 R8 설명은 최적화 켬이나 실제는 `-dontoptimize`(Crashlytics 충돌). ⑤ 스토리/체크리스트 체크박스 미표시. ⑥ `START-HERE`/`data/README` 좌표 수치. ⑦ PRD에 D-15(arm64만)·`-dontoptimize` 결정 미반영. ⑧ (해결) `docs/spike-report.md` 작성됨.
8. **세션 게이트의 오프라인 동작**: `AuthRepository.hasProfile()`은 확인 실패(오프라인·타임아웃)면 "프로필 있음"으로 간주해 `SignedIn`으로 들어간다(오프라인에서 앱이 막히지 않게 한 의도). 프로필 문서가 실제로 없는 계정이 오프라인 상태로 진입할 수 있음.
9. **하드코딩 문자열 예외**: `MainScreen`의 `DebugVisitButton`("가상 방문 기록…", "기록할 가게가 없어요"), `DiagnosticsSampleOcr`는 한글 리터럴(디버그 전용). `CatalogRepository.createTestStore`의 테스트 가게 이름·소개도 리터럴(진단용). 사용자 화면 문구 규칙 위반은 이 디버그/진단 범위 밖에서는 찾지 못했다(`nopo-qa`로 재검증 권장).
10. **`CLAUDE.md` 규칙 점검 결과**: `applicationIdSuffix` 없음 ✔, 갤러리 선택 없음 ✔, 영수증 이미지는 `captureInMemory`로 파일 없이 처리 ✔(인증샷만 `filesDir/proofs/`에 JPEG 85, 기기 안에만), 위치는 사진 인증에서만 사용 ✔, 좌표·영수증 내용은 서버/로그에 없음 ✔(`ReverseGeocoder`는 좌표를 저장·로그하지 않음), 키는 `local.properties`→`BuildConfig`/매니페스트 플레이스홀더 ✔(다만 `NAVER_MAP_CLIENT_SECRET`이 `BuildConfig`로 APK에 들어감 — 알려진 위험).

## 7. 알려진 문제·결정 (유지할 것)
- **R8**: 최적화를 켜면 Crashlytics와 충돌해 릴리스가 시작 시 `IllegalAccessError`로 종료 → `app/src/main/keepRules/rules.keep`의 `-dontoptimize`를 지우지 말 것. 릴리스는 반드시 폰에 설치해 확인. (`app/build.gradle.kts`의 `optimization { enable = true; packageScope = … }`는 AGP 9 방식.)
- **Monkey**: 개인 폰에서 돌리면 앱 밖(런처)으로 나가 다른 앱이 열린다. 쓰지 말 것. 전용 기기/에뮬레이터 필요(이 PC 메모리 부족으로 에뮬레이터 불가). 1차 4,329회·2차 3,647회에서 중단, 앱 크래시 없음.
- 디버그와 릴리스는 서명이 달라 폰에서 서로 덮어쓸 수 없다(`adb uninstall com.nopo.cheonnyeon` 후 설치).
- 사진 서버(sbiz24.kr)는 `Accept` 헤더 없는 요청에 500 → `NopoApp.newImageLoader`가 헤더를 붙임.
- 리뷰 쓰기는 방문 문서가 서버에 있어야 허용(규칙) → 테스트 가게는 서버로 안 보내므로 리뷰 불가(의도). 평점 버튼은 `my_visits.synced`일 때만 활성(`E_VISIT_SYNCING`).
- 가상 방문(디버그 버튼)은 서버에 실제 방문 문서를 만든다(시험 계정으로만).
- D-15: 릴리스 APK는 arm64-v8a만 (147MB → 약 48MB).
- Firestore 데이터베이스를 만들기 전에 `firebase deploy --only firestore`를 하지 말 것(기본 nam5 미국으로 생성됨, 바꿀 수 없음). 배포: `cd firebase && firebase deploy --only firestore:indexes,firestore:rules --project nopocheonnyeon`.
- Git-Bash에서 한글 curl은 인코딩이 깨져 네이버 지오코딩이 0건. 파일 여러 개를 heredoc으로 한 번에 만들면 파싱 오류 → Write 도구나 파이썬 파일 사용.
- `design/stitch/reference/` 안의 "즉시 자동 복사" 스크립트 문서는 실행 금지.

## 8. 명령 (Windows)
- 단위 테스트 `.\gradlew.bat testDebugUnitTest` · 린트 `lintDebug` · 디버그 설치 `installDebug` (폰 R5KL806MA4W, Galaxy Z Fold SM-F971N, Android 17 API 37)
- 릴리스 `.\gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk`; 설치 `adb install -r …` (adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`)
- 로그 `adb logcat -d -v time Nopo:D AndroidRuntime:E *:S`
- 카탈로그 재생성 `python tools/build_catalog.py`; 좌표 `python tools/geocode.py --provider naver|kakao`; 사진 주소 `python tools/fetch_photos.py`; 공식 지정리스트 `python tools/import_official_list.py --csv 파일.csv`; Stitch 재수집 `$env:STITCH_API_KEY="…"; python tools/fetch_stitch.py`
- 시험 재료: `tools/test-images/test_receipt_live.html`(현재 시각 영수증), `test_receipt.png`(날짜 고정 → 24시간 뒤 만료 확인용), `test_sign.png`

## 9. 남은 일 (우선순위 순)
1. **사용자 수동 QA** (**`docs/nopo-09-real-device-qa.md`**, 이전 목록은 `qa-report.md` 5장): 영수증 실촬영, 실제 가게 앞 30m 인증, 오프라인 전송("확인 중" 도장 → 복귀 후 확정), 릴리스 APK에서 영수증·사진·리뷰 재확인, 구형 기기·큰 글씨·다크 모드, 회원 탈퇴·리뷰 신고(두 번째 계정), 길찾기 앱 실행, 서명 키 백업 2곳.
2. backlog B-01·B-02(중복 영수증 서버 확인·알림)·B-03(지도 세월 합계) 수정 + 해당 테스트 추가 → 6장 2번(Repository/ViewModel 테스트).
3. 영수증 가이드 틀(FR-VER-03), 칭호 승급 연출(S-705), 정보 오류 신고(FR-STORE-06), 필요 시 리뷰 편집기·도장 연출을 시안에 맞춰 보강(`design/stitch/PRD-차이점.md` 먼저).
4. 사진 확대: 공식 사진 86곳(7%) 외에는 글자 카드. 웹 사진 내려받기 또는 구글 Places API(≈$20, 약관 확인).
5. 좌표 없는 96곳 보강(카카오 키 필요) → `geocode.py --provider kakao` → `build_catalog.py` → 앱 재적재(카탈로그 version 변경됨).
6. 출시 전: Room 정식 마이그레이션, 네이버 Client Secret 한도 설정/교체, 공식 지정리스트 반영(S-1001), 문서 정리(스토리/체크리스트 체크, 7장의 낡은 문서 수정, PRD 결정 반영), `nopo-qa` 에이전트로 최종 검증.
7. 작업 방식 재확인: 한 번에 스토리 하나, 계획 5줄, 도메인은 테스트 먼저, `testDebugUnitTest` → `assembleDebug` → `installDebug`, 커밋 `S-xxx: 요약`, 새 라이브러리·버전 변경은 먼저 사용자에게 물을 것(단, 0장의 "사전 승인" 범위 안의 개발은 묻지 않고 진행).

## 10. 진행 로그 (2026-10-20, 사용자 "묻지 말고 모든 개발·테스트 계속" 지시 이후)
사용자 결정: P1·시안 수준 보강을 **전부 진행**, 사진 출처는 **사용자가 구글 API 키를 줄 때까지 보류**(글자 카드 유지), **재방문율 기능 추가**(FR-STORE-07). 이 기간 커밋: `6f050bc` `c82bb47` `180c657` 이후 `B-09·B-10·B-11·B-25`, `B-13·B-14`, 본 로그 시점 마지막 커밋은 `git log` 참고.
| 항목 | 상태 |
|---|---|
| B-01·B-02 중복 영수증(서버 방문 확인 후 판정, 알림 다이얼로그) | 구현, 실기기 미확인 |
| B-03 지도 세월에서 테스트 방문 제외 | 구현 |
| B-05 인증 화면 위치 켜기 대화상자 | 구현, 실기기 미확인 |
| B-06 진단 정보(Play 서비스·권한·마지막 측정) + 복사 | 구현 |
| B-07 테스트 가게·기록 삭제(개발자 도구) | 구현 |
| B-08 프로필(가입일·게이지·탭·등급 도장·첫 방문일·단골·20개씩) | 구현 |
| B-09 영수증 가이드 틀 / B-10 가게 정보 오류 제보 / B-11 칭호 승급 연출 / B-25 연타 방지 | 구현 |
| B-12 방문 기록 공개 범위(설정) + 규칙 | 구현, **규칙 배포 대기** |
| F-01 재방문율(`storeStats` 카운터, 상세 평점 영역에 표시) | 구현, **규칙 배포 대기** |
| B-13 Room 마이그레이션 체계 / B-14 릴리스에서 Client Secret 제거 | 구현(릴리스 APK dex에서 secret 검색 0건) |
| 시안 수준: 결과 화면 인증 전→후·게이지·칭호, 하루 한도 화면 오늘 인증 목록 | 구현 |
| 단위 테스트 | 152개 통과(RevisitStats 7개 추가) |
| 폰 설치 | 릴리스 APK(사용자 키)를 `adb install -r`로 덮어 설치, 시작 후 `AndroidRuntime` 오류 0. **화면 조작 시험은 하지 않음** — `nopo-09`의 해당 항목은 모두 "미실시" 유지 |

**막힌 것(사용자 필요):** ① **Firebase CLI 로그인 만료** → `firebase login --reauth` 후 `cd firebase && firebase deploy --only firestore:rules --project nopocheonnyeon` (storeStats·공개 범위 규칙). 배포 전에는 재방문율이 0명, 공개 범위 저장은 오류 문구. 앱 자체는 정상(집계 쓰기는 실패해도 방문 기록에 영향 없게 분리). ② 구글 Places 키(사진). ③ 영수증 실촬영 등 수동 QA(`nopo-09`).
**남은 개발 후보:** B-04(ViewModel·Repository·Room 테스트, Firestore 포트 분리 필요), B-15~B-24, 시안 보강(동기화 복원 진행 화면, 로그아웃 경고 대기 목록, 내 리뷰 수정·삭제 버튼, 개인정보 처리방침 화면, 라이선스 목록), D-01~D-04 데이터 보강.

### 10-2. 이어진 진행 (같은 날)
- B-04 일부: `SyncRejectionPolicy`(+테스트 4개) → 단위 테스트 156개. 로그아웃 경고에 올라가지 않은 가게 목록, 로그인 직후 "서버에서 기록을 불러오는 중" 배너(`AppContainer.restoring`), 설정의 개인정보 처리방침 전체 보기·라이선스 목록, 내 리뷰 "수정·삭제하러 가기".
- lint 오류 0건(Compose `LocalContext` 리소스 호출 수정). 쓰지 않는 문자열 제거, `err_*`는 `getIdentifier`로 읽으므로 `tools:ignore="UnusedResources"`.
- 이 PC에서 `./gradlew.bat lintDebug testDebugUnitTest assembleDebug`가 통과하는 상태에서 커밋.

### 10-3. nopo-qa 점검 반영
`nopo-qa` 에이전트(정적 읽기·빌드)가 높음 3건을 지적해 수정했다(Q-01~Q-03, `nopo-11` 참고). 규칙 보강분도 **Firebase 로그인 재인증 후 배포**해야 적용된다. 한글 리터럴은 디버그 전용 버튼·진단 샘플·@Preview에만 남아 있다(허용). 에이전트가 제안한 폰 확인 목록은 `nopo-09`의 O-01·R-09·V-06·V-10 등과 겹친다.

### 10-4. Firebase 규칙 배포 완료
`firebase login`(songpa24@iceu.kr) 재인증 후 `firebase deploy --only firestore:rules,firestore:indexes --project nopocheonnyeon`이 성공했다(DB 위치 asia-northeast3 확인). 이제 `nopo-09` 13장의 ★ 항목(N-01 재방문율, N-03 공개 범위)을 실기기로 시험할 수 있다. 앞의 "막힌 것 ①"은 해소됨. 인증 코드는 채팅에 노출됐으나 1회용이라 이미 사용돼 무효다.

### 10-5. 테스트 라이브러리·가게 이야기·계측 시험 (사용자 승인 요청 사항)
- **새 라이브러리(승인됨):** Robolectric 4.17(4.14는 JDK 25 클래스를 못 읽음), `androidx.test:core-ktx`, `androidx.room:room-testing`. `testOptions.unitTests.isIncludeAndroidResources=true`, Robolectric 시험은 `@Config(sdk=[34], application=android.app.Application::class)`(NopoApp이 Firebase를 초기화하므로 제외). ViewModel은 MockK로 `AppContainer`를 모킹하고, 위치·OCR·시계 확인은 지연 생성/주입으로 분리.
- **시험 현황:** 단위 220개(도메인·유틸 + Room·저장소·ViewModel 5종), 폰 계측 6개. `app/src/test/.../testutil/TestSupport.kt`에 `MainDispatcherRule`·`FixedClock`.
- **가게 이야기:** `nopo-11` 마지막 절 참고. DB v3(`MIGRATION_2_3`), 카탈로그 v26102013.
- **서명:** 디버그 빌드도 `keystore.properties`가 있으면 릴리스 키로 서명(폰의 릴리스 앱 위에 덮어 설치 가능).

### 10-6. 시연용 예시 데이터 (사용자 요청 2026-10-20)
- 서울 가게 270곳에 예시 리뷰 3개씩, 송파구 12곳은 예시 리뷰 10개 + 예시 사진 3장(총 리뷰 894개, 사진 36장 약 2MB). 생성: `python tools/gen_demo_data.py`(가게 ID로 난수 고정, 메뉴·업력·지역을 문장에 반영, 조사 자동) → `assets/demo_reviews.json`, `assets/demo/<가게ID>_<1~3>.jpg`, `data/demo_photos.json` → `python tools/build_catalog.py`가 `photoUrls`에 `file:///android_asset/demo/…`로 합침(공식 사진이 있으면 앞에 둠).
- 앱: `DemoReviews`(assets 읽기) → `ReviewsViewModel`이 서버 평점 요약에 합산, 목록에서는 실제 리뷰 뒤에 "시연용 예시 리뷰 · 실제 이용자 글이 아닙니다" 표시, 신고·프로필 이동 없음. 설정 > 앱 정보 5탭 > 개발자 도구의 "시연용 예시 리뷰 표시" 스위치(기본 켜짐, `SettingsStore.demoData`). 사진 속에는 "시연용 예시 이미지" 문구가 박혀 있음.
- **실사용 배포 전에는 스위치를 끄거나 `demo_reviews.json`·`assets/demo`·`data/demo_photos.json`을 지우고 카탈로그를 다시 만들 것.** 서버(Firestore)에는 아무것도 쓰지 않는다.
- 폰 확인: 오금버드나무집 상세 사진 "1 / 3", 리뷰 10개와 표시 확인. 단위 시험 2개 추가.

### 10-7. 기획서 대비 추가 구현 (2026-10-20)
- FR-VER-11 설정 스위치(설정 > 방문 인증), FR-MAP-02 금빛 테두리 핀(`feature/map/PinIcons.kt`), PAS-05 패스포트 공유 카드(`platform/share/PassportShareCard.kt`, FileProvider `${applicationId}.fileprovider`, 캐시 `share/passport.png`), 지역 컬렉션(`domain/rule/RegionProgress.kt` + 패스포트 "지역" 탭, 단위 시험 3개).
- **실기기 확인 대기**(폰 화면이 꺼져 있어 자동 확인 못 함): 설정 스위치 동작, 지도에서 100년 이상 가게 핀의 금빛 테두리, 패스포트 "지역" 탭, "패스포트 카드 공유"(공유 시트·카드 이미지).
- 남은 미개발은 `nopo-12-unimplemented.md`(Analytics 이벤트, QR 인증, 서버 판정 등).

### 10-8. 명예의 전당·시연용 패스포트 (사용자 요청 2026-10-20)
- 시연용 리뷰를 가게당 5개 늘림: 서울 8개, 송파구 15개(총 2,244개). 시연용 식객 120명(`users`: 닉네임·세월 35~1,474년·탐방 수·리뷰 수)을 `assets/demo_reviews.json`에 함께 생성(`tools/gen_demo_data.py`). 리뷰어의 칭호·단골 횟수는 그 식객의 세월에서 정해짐.
- **명예의 전당**(`feature/passport/HallOfFameScreen.kt`, 경로 `hall`, 패스포트 화면의 "식객 명예의 전당 보기"): 내 순위 카드(세월·순위·상위 %·천년 도달률), 탭 3개(누적 세월 순 / 리뷰왕 / 인기 노포), 3위 시상대, 4~20위 목록. 순위 계산 `domain/rule/HallOfFameRule.kt`(단위 시험 3개). 서버 집계가 없어서 **시연용 예시 식객과 견준 순위**이며 화면에 "시연용 예시 순위" 표시. 예시 데이터를 끄면 "아직 집계된 순위가 없어요". Stitch 시안의 승급 기준 700년은 PRD대로 600년으로 표기.
- **시연용 패스포트**(설정 > 앱 정보 5탭 > 개발자 도구): "시연용 패스포트 채우기(637년)" = 서울 가게 12곳(송파구 3곳 포함) 도장을 `method=DEMO`, `synced=true`로 기기 DB에만 넣음(서버·대기열에 안 올라감, 세월 합 637년 → 장인 식객). 내 리뷰 8개는 패스포트 "내 리뷰" 탭과 해당 가게 상세에 "시연용 예시 리뷰" 배지로 보임. "지우기"로 제거. 시연용 도장이 있는 가게를 실제로 인증하면 그 도장은 실제 첫 방문으로 바뀜(`VisitRepository.record`). 로그아웃 시 사용자 데이터와 함께 삭제됨.
- 시연 순서 제안: 새 계정 가입 → 설정 5탭 → 채우기 → 패스포트(637년·장인) → 지역 탭 → 명예의 전당 → 송파구 가게 상세(사진 3장·리뷰 15개) → 실제 인증으로 도장 하나 더.

### 10-9. 시연용 예시 노포(현재 위치)·영수증·재방문율 (2026-10-20)
- **시연용 노포:** 설정 5탭 > 개발자 도구 > "테스트 가게 만들기 (현재 위치)" = `노포천년테스트식당`(메뉴 설렁탕·수육·공기밥, 영업시간, 이야기, 예시 사진 3장 `assets/demo/TEST-NOPO_n.jpg`). 예시 리뷰 12개(평균 4.3)와 예시 재방문율(방문 12명 중 4명, 33%)이 상세에 나온다. 시험용 가게라 리뷰 작성은 막고(`ReviewEligibility.TEST_STORE`, 서버 호출 없음) 세월·하루 한도에도 들어가지 않는다는 문구를 보여 준다.
- **영수증:** `python tools/gen_test_receipt.py` → `tools/test-images/test_receipt_today.png`(현재 시각, 24시간 안에 사용). 모니터 등 다른 화면에 띄우고 앱 카메라로 찍는다. 같은 영수증은 중복(`E_RECEIPT_DUP`)이므로 다시 시험할 땐 다시 생성. `test_receipt_live.html`(현재 시각이 매번 찍힘)도 사용 가능.
- **재방문율(F-01):** 가게 상세 "평점과 리뷰" 영역에 "재방문율 NN% · 방문 N명 중 N명이 다시 갔어요"(방문자 5명 미만이면 인원수만). 서버 집계(`storeStats`)에 시연용 예시 리뷰어(단골 2회 이상=재방문자)를 더해 보여 준다(`mergeDemoStats`).
- **발견·수정한 결함:** 금빛 테두리 핀(`PinIcons`)의 몸통 경로 방향이 달라 겹치는 부분이 비는 문제를 Robolectric(NATIVE 그래픽) 시험으로 찾아 고침. 시험용 가게 상세의 "세월 +46년" 문구·"소상공인마당 출처" 표기도 바로잡음.

### 10-10. 시연 가게 시나리오 (2026-10-20)
- 시험용 가게(`TEST-NOPO`): 인증 전에는 "방문 인증한 사람만 평점을 남길 수 있어요", 인증 뒤에는 리뷰를 쓸 수 있다. 내 리뷰는 `SettingsStore.testReview`(DataStore, 키 `test_review_<가게 ID>`)에 이 기기에만 저장되고 평균·개수에 합쳐진다. "테스트 가게·기록 삭제"가 함께 지운다. 단위 시험 `ReviewsViewModelTest`에 2개.
- `tools/gen_test_receipt.py`: 영수증 3종(성공·50시간 전·다른 가게)과 메뉴판, 시나리오 페이지 `demo_scenarios.html`. 시나리오 표는 `docs/demo-guide.md` 끝.
- 미확인: 새 영수증 이미지들을 ML Kit으로 읽는 것(카메라 촬영 필요). 폰에서 시나리오 2~4는 사용자가 촬영해 확인.

### 10-11. Google 지도 대표 사진·가입 동의·패스포트 통계 (2026-10-20)
- **대표 사진:** `python tools/fetch_google_places.py`(키는 환경변수 `GOOGLE_PLACES_KEY`, 코드·로그에 없음) — 공식 사진이 없는 1,191곳 중 860곳에 Google Places(New) 사진 1장씩(폭 560px WebP, 약 26MB → `app/src/main/assets/places/<가게ID>.webp`). 이름 유사도 0.9 이상 + 시·군·구 일치일 때만 채택(0.75~0.89 구간은 오매칭이 많아 39곳 제외). 못 찾은 약 330곳은 글자 카드. 기록 `data/raw/google_places.jsonl`, 지도 `data/places_photos.json` → `build_catalog.py`가 공식 → Google → 시연용 순으로 최대 3장. 사진 위에 "사진: Google 지도" 표시. 현재 사진이 있는 가게 946/1,277곳. **사진 파일은 `.gitignore`(약관상 저장·재배포 제한) — 새로 받으려면 스크립트 재실행. 공개 배포 전 Google 지도 플랫폼 약관 재확인.** APK 79MB(arm64).
- 가입 화면: 개인정보 처리방침 동의 체크박스(필수)·전체 보기. 패스포트: 도장·방문 지역·가장 오래된 노포 통계 카드(`PassportStats`, 단위 시험 2개).
- 폰 확인: 설치까지만 했고 새 사진 표시는 미확인(폰을 사용 중이라 UI 자동 확인 중단).

### 10-12. 지도 사용성 개선 (2026-10-20)
- 하단 시트의 사진·상호를 눌러도 가게 상세로 이동(기존 "자세히 보기" 유지).
- "내 위치" 버튼은 누를 때마다 지도를 내 위치로 이동(`MapUiState.recenter`). 빠른 위치(`FusedLocationAdapter.quickFix`: 1분 이내 값 또는 저전력 3초)로 먼저 표시한 뒤 `bestFix`(최대 8초, 30m 이하면 종료)로 다듬는다. 방문 인증은 기존 정확도 기준 그대로.
- 폰 확인: Google 사진 표시("1 / 3", "사진: Google 지도")·패스포트 통계 확인됨. 지도 이동·시트 탭은 사용자가 육안 확인 필요.
- `StoreSheet` @Preview(라이트·다크·1.5배 글씨) 추가. 남은 @Preview는 ViewModel에 묶인 화면이라 상태 분리가 필요.
- **Firestore 호출 시험(가짜 Firestore, mockk+`Tasks.forResult/forException`):** `ReviewRepositoryTest`(오프라인·규칙 거부·네트워크 오류 변환, 신고/제보 필드, 비로그인, 잘못된 별점은 서버 미호출)·`VisitSyncTest`(전송 성공, 오프라인 재시도, 거부 후 서버 확인으로 성공/영수증 중복 되돌림/확인 불가 재시도, 영수증 없는 기록 거부, 집계 전송 실패 시 대기열 보존).
- **화면별 @Preview(라이트·다크·1.5배 글씨):** 지도 목록·시트, 가게 상세(보상 박스·이용 정보), 리뷰 카드, 프로필 카드, 명예의 전당(내 카드·시상대·행), 칭호 승급 대화상자. ViewModel에 묶인 최상위 화면(로그인·인증·설정 등)은 상태 분리가 필요해 제외.

### 10-13. 칭호 11단계·도장 연출·사진 리뷰·공유 QR (2026-10-20)
- **칭호 100년 단위(11단계):** 첫술 식객 0 → 골목 단골 100 → 국밥 순례자 200 → 시장통 어르신 300 → 가마솥 감별사 400 → 노포 길잡이 500 → 맛의 장인 600 → 세월 지킴이 700 → 미식 도인 800 → 전설의 식객 900 → 천년 미식가 1,000. `Title` enum 이름(SPROUT/REGULAR/ELDER/ARTISAN/MASTER)은 서버 저장값이라 유지하고 새 단계(WANDERER·CONNOISSEUR·PIONEER·KEEPER·SAGE·LEGEND)만 추가. 라벨은 `Title.labelRes()` 한 곳. PRD 6.2·FR-STP-04, 시험 갱신.
- **도장 연출(`StampCeremony`):** 위에서 크게 내려와 쿵 찍힘(회전·눌림) → 충격 고리·인주 튀김·화면 흔들림·진동 1회, 세월 숫자 카운트업, 백년(금빛)은 반짝이는 별. 시스템 애니메이션을 끄면 정적.
- **시험용 가게 리뷰 화면 결함 수정:** 인증 화면에서 돌아와도 `ReviewsViewModel`(상세 화면 back stack에 살아 있음)이 예전 자격(NOT_VISITED)을 들고 있었다. 내 방문 기록(생성·확인됨)이 바뀌면 다시 불러오도록 고침(회귀 시험 추가).
- **사진 리뷰(전 가게):** 리뷰 편집기에 "사진 찍어 올리기"(앱 카메라만, 갤러리 없음, 파일 저장 없음). `ReviewPhotoCodec`이 긴 변 720px WebP ≤110KB로 줄여 base64 문자열을 리뷰 문서의 `photo` 필드에 저장(Firebase Storage는 Blaze 요금제·새 라이브러리가 필요해 쓰지 않음). Firestore 규칙에 `photo` 길이 ≤170,000 검증을 추가해 **배포함**(`firebase deploy --only firestore:rules`). 시험용 가게 리뷰 사진은 DataStore. 리뷰 카드·내 리뷰·프로필 카드에 사진 표시, 누르면 크게 보기.
- **송파구 시연용 사진 리뷰:** 송파구 12곳과 시험용 가게의 예시 리뷰 앞 5개씩에 음식 그림 사진(`assets/demo/R_<ID>_<1~5>.jpg`, "시연용 예시 이미지" 문구 포함) — `tools/gen_demo_data.py`.
- **"시연용 예시 리뷰" 배지 문구 제거**(리뷰 카드·내 리뷰 탭, 시험용 가게 안내문). 예시 데이터 스위치와 명예의 전당 "시연용 예시 순위" 표기는 유지.
- **공유 카드 QR:** `zxing-core 3.5.3`(QR 만들기 전용, 새 의존성) 추가. 카드 하단에 앱 내려받기 QR + 안내, 공유 메시지에 링크. 주소는 `platform/share/ShareConfig.DOWNLOAD_URL` 한 곳(기본: GitHub 릴리스 페이지, 저장소가 비공개라 지금은 권한 있는 사람만 열림 → 배포 링크가 정해지면 교체). 카드 1080×1560.

### 10-14. 인증 후 리뷰 유도·사진 3장(앨범 허용)·실제 음식 사진·칭호 도감·승급 연출 (2026-10-20)
- **인증 후 리뷰 화면이 안 뜨던 문제:** 리뷰 섹션이 상세 화면 맨 아래라 눈에 안 띄었다. 인증 결과 화면에 "평점·리뷰 남기기" 버튼 추가 → 가게 상세로 돌아가 리뷰 쪽으로 스크롤하고 편집기를 자동으로 연다(`AppContainer.pendingReviewStoreId`, 자격이 OK가 될 때까지 기다림).
- **리뷰 사진 최대 3장, 앨범 허용:** 카메라 또는 시스템 사진 선택기(`PickMultipleVisualMedia`, 저장소 권한 없음). 줄여서 문자열로만 보관, 파일 저장 안 함. 모델 `ReviewItem.photos`, Firestore 필드 `photos`(예전 `photo`는 읽기 호환), 규칙 `photos is list && size <= 3` 배포. 요청의 "파일로 저장"은 Firebase Storage가 Blaze 요금제라 하지 않고 문서에 직접 저장(3장 ≈ 450KB < 1MB). CLAUDE.md·PRD FR-REV-09에 갤러리 예외 반영.
- **시연용 사진 교체:** `tools/fetch_demo_food_photos.py`가 Wikimedia Commons(CC0/CC BY/CC BY-SA)에서 가게 메뉴별 음식 사진 104장을 받아 송파구 12곳·시험용 가게 리뷰 5개(1·2·1·3·1장)와 대표 사진에 사용. 작가·원본은 `data/demo_photo_credits.json`, 설정 > 라이선스 화면에 출처 안내. 사진관·건설 가게는 마땅한 무료 사진이 적어 결혼식·실내 사진으로 대체(품질 낮음).
- **칭호 도감:** 패스포트의 칭호 배지를 누르면 11개 칭호·필요 세월·달성 여부를 보고, 칭호를 누르면 승급 연출을 미리 본다(실제 인증 없이 배지 색 변화·연출 확인).
- **칭호 승급 연출 강화:** 전체 화면 무대, 회전 빛줄기·빛무리, 배지가 뒤집혀 튀어나옴, 색종이 낙하, 폭죽(칭호가 높을수록 많이), 이름 타이핑, "이전 → 새 칭호", 축하 진동. 폰에서 확인(미리보기).

### 10-15. 두 번째 기기(구형) 설치 (2026-10-21)
- 새 폰 Galaxy S9+ (SM-G965N, Android 10, arm64, 화면 1080×2220)에 디버그 APK 설치(`adb install -r`, 릴리스 키 서명). 계정은 시연용 테스트 계정을 새로 만들어 사용(자격 증명은 저장소·문서에 없음, 사용자가 자기 계정으로 로그인해도 됨).
- 확인: 앱 실행·카탈로그 적재, 가입(개인정보 동의 체크박스 필수 동작), 시연용 패스포트(도장 12개), 시험용 가게 생성(위치 권한 허용 후), 시험용 가게 대표 사진 3장·사진 달린 리뷰 표시.
- 미확인(직접 조작 필요): 영수증 촬영 인증, 리뷰 앨범/카메라 사진 올리기, 인증 직후 리뷰 편집기 자동 열림.

### 10-16. 리뷰 여러 개·도장 후 축하 연출·연출 영상 (2026-10-21)
- **리뷰를 계속 쌓기:** 한 가게에 여러 번 쓸 수 있다. 문서 ID `가게_내uid_시각`(예전 `가게_내uid`도 수정 가능), `ReviewRepository.mine()`은 내 리뷰 목록, `save(reviewId=null)`은 새로 쌓고 ID가 있으면 수정, `delete(reviewId)`는 하나만 삭제. 화면: "리뷰 남기기"는 항상 새 리뷰, 내 리뷰 카드에 "내 리뷰 수정". 시험용 가게도 `SettingsStore.testReviews`(JSON 목록, 예전 한 개 기록은 읽을 때 이전). Firestore 규칙(ID 정규식) 배포함. 시험 추가.
- **도장 연출 순서:** 도장이 찍히면(진동) 곧바로 폭죽·색종이(`CelebrationOverlay`)가 터지고, 칭호가 올랐으면 2.8초 뒤 승급 연출이 뜬다(전에는 결과 화면이 열리자마자 가려졌다).
- **개발자 도구 "연출 미리보기"(일반/붉은/금빛+승급):** 인증 없이 연출 확인·영상 녹화용.
- **연출 영상:** `docs/media/stamp_ceremony_demo.mp4`(S9+ 화면 녹화, 상태바 제거, 금빛 도장 → 폭죽 → 칭호 승급). 앱에 영상 파일을 붙이려면 Media3 같은 재생 라이브러리가 필요하지만, 연출은 이미 코드(Compose)라 영상은 발표 자료용을 권장.

### 10-17. QA 지적 반영 (2026-10-21)
- **리뷰 폭탄 방지:** 새 리뷰 문서 ID를 `가게_uid_슬롯(1~5)`로 바꿔 한 사람이 한 가게에 남길 수 있는 리뷰를 최대 5개(+예전 형식 1개)로 제한. 슬롯이 차면 `E_REVIEW_LIMIT`(PRD 7.4·FR-REV-10, 오류 코드 26개). 
- **서버 규칙 강화(배포함):** 리뷰 필드 화이트리스트, `photos` 원소 문자열·크기 ≤170,000, 닉네임이 users 문서와 일치, `updatedAt == request.time`, `storeId` 형식 검사. `reviewCount`·`totalYears` 상한은 Cloud Functions 없이는 한계가 있어 미해결(참고용 값).
- **규칙 시험 스크립트:** `tools/rules-test/rules.test.js`(에뮬레이터용, 18개 시나리오). 이 PC에서는 에뮬레이터 jar 다운로드가 실패해 **아직 실행하지 못함** → 실행 전까지 규칙의 동작은 컴파일·배포 확인뿐.
- 사진 디코딩을 메인 스레드 밖으로, API 26~27 앨범 사진 샘플링·EXIF 회전, 시험용 가게 리뷰 최대 10개, 색 토큰(`Color(0xFF050B0A)` 제거), 아키텍처 문서에 zxing-core 추가.
- 남은 지적(보통·낮음): 사진 선택 상태가 화면 회전 때 사라짐(용량 때문에 Bundle 미사용), 목록에서 사진 전체를 읽음(썸네일 필드 필요), QR 주소(배포 링크 확정 필요), `reviewCount` 위조 가능.

### 10-18. AI 기능 (Gemini, 2026-10-21)
- **키:** 빌드하는 PC의 환경변수 `GEMINI_API_KEY` → `BuildConfig.GEMINI_API_KEY`(저장소·문서·로그에 없음, 비어 있으면 AI 기능이 꺼지고 안내만 표시). 빌드 전 셸에 환경변수가 안 보이면 `export GEMINI_API_KEY=$(powershell -c "[Environment]::GetEnvironmentVariable('GEMINI_API_KEY','User')")` 후 `./gradlew.bat ...`. **앱에 들어간 키는 APK에서 추출될 수 있으니** GCP 콘솔에서 Generative Language API로만 제한하고(가능하면 Android 앱 패키지+SHA-1 제한) 예산 알림을 걸 것. 공개 배포 전에는 Firebase AI Logic/서버 경유로 바꾸는 것이 안전.
- **모델:** `GeminiClient.DEFAULT_MODEL = gemini-3.1-flash-lite`(응답 약 3초, JSON 응답 모드). 키로 사용 가능한 모델 목록 확인 완료.
- **① AI 메뉴 룰렛**(패스포트 > "AI 메뉴 룰렛", `feature/ai/MenuRouletteScreen`): 내 리뷰(서버·시험용 가게·시연용)와 가게 메뉴·음식 사진(최대 6장, base64 WebP)을 Gemini에 보내 메뉴 3개(이유·주재료·구성 성분)를 받고, 카탈로그에서 그 메뉴가 있는 노포를 연결. 다시 돌리면 이전 메뉴를 제외 목록으로 알려 주고(최근 9라운드 기억) 중복은 클라이언트에서도 거름, 최대 10번(= 30개) 후 "처음부터 다시". 규칙 `domain/rule/MenuRoulette`.
- **② 가게 리뷰 AI 분석**(가게 상세 리뷰 영역, 리뷰 3개 이상): 한 줄 요약·특장점·추천 메뉴·아쉬운 점·어울리는 사람. 같은 리뷰 묶음은 캐시.
- 처음 사용 전 전송 동의 대화상자(`SettingsStore.aiConsent`), 개인정보 안내(앱·`privacy-notice.md`) 갱신, 오류 코드 `E_AI_FAILED`(27개째).
- 시험: `MenuRouletteTest`(5), `AiRepositoryTest`(가짜 전송 9: 파싱·중복 재요청·오류 변환·캐시). 폰(S9+)에서 룰렛 3회(곰탕→순대국→닭볶음탕, 중복 없음)·가게 분석 동작 확인.

## 2026-10-21 Gemini 키를 앱에서 제거 (Firebase AI Logic)
- Cloud Functions 프록시안은 Blaze 요금제가 필요해 **버리고**, Firebase AI Logic(Gemini Developer API, Spark 무료)으로 교체했다. 앱에는 키가 없다.
- `GeminiClient`가 `firebase-ai` SDK로 호출하고, 응답 스키마는 프롬프트 끝에 붙인다(JSON 모드). 모델은 `GeminiClient.DEFAULT_MODEL`.
- 콘솔 쪽: Firebase 콘솔 > AI Logic 시작(완료). **App Check 적용(Enforce)이 켜져 있으면 "App Check token is invalid"로 실패**한다 → App Check > API에서 Firebase AI Logic(Generative Language)을 "적용 안 함"으로 두거나, App Check(Play Integrity/디버그 제공자)를 앱에 붙여야 한다. 사이드로드 APK는 Play Integrity를 통과하지 못한다.
- 2026-10-21 확인: App Check를 "적용 안 함"으로 바꾼 뒤(반영에 약 10분) 디버그·릴리스 모두 폰에서 AI 응답 확인. 기본 모델이 붐비면(503 high demand) 25초 제한 후 예비 모델 `gemini-2.5-flash-lite`로 한 번 더 시도한다. APK 안에 Gemini 키 없음(내 키 문자열·AIza 형태 0건 확인). 릴리스 `v0.1.0-demo`의 APK를 키 없는 빌드로 교체함.
- 남은 일: 기존 Gemini 키 폐기(콘솔), **2026-11-02부터 AI Logic App Check 적용이 의무**라 그 뒤에는 이 방식이 막힌다 → 그때는 App Check 제공자 또는 서버 프록시(Blaze) 필요, 시연 뒤 App Check 다시 켜기.
- (이전 메모) 배포 후: 새 릴리스 APK로 교체, 기존 릴리스(v0.1.0-demo)와 기존 Gemini 키 폐기.

## 2026-10-21 앱 아이콘
- 청록 간판 바탕 + 붉은 도장("노포") 적응형 아이콘. 생성: `python tools/gen_app_icon.py`(나눔명조 ExtraBold 필요) → `mipmap-*/ic_launcher*.webp`, `mipmap-anydpi/ic_launcher*.xml`(바탕·전경·모노크롬). 미리보기 `docs/media/app_icon_preview.png`. 폰 홈 화면 확인, 공개 릴리스 APK도 교체함.

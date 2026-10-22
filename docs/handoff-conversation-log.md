# 인수인계 — 사용자와의 대화 기록 정리 (2026-10-19 ~ 2026-10-22)

이 문서는 사용자(이하 "사용자")와 Claude Code 사이에 오간 **모든 요청·결정·제약**을 시간순으로 정리한 것이다. 다른 에이전트나 사람이 이 문서만 읽어도 "무엇을 왜 했고, 지금 어디까지 왔고, 무엇을 하면 안 되는지"를 알 수 있게 쓴다.
함께 읽을 문서: `docs/dev-status.md`(코드·진행 로그, 맨 위가 최신), `docs/qa-final-2026-10-21.md`(검수 결과), `CLAUDE.md`(작업 규칙), `docs/nopo-01-PRD.md`(요구사항).
대화 원문(`.jsonl`)은 이 PC의 `C:\Users\HB_OPERATION_NICK\.claude\projects\C--Users-HB-OPERATION-NICK-AndroidStudioProjects-NopoCheonnyeon\` 아래에 있고 **저장소에는 올리지 않는다**(용량·개인 정보 때문). 원문을 이어서 쓰려면 프로젝트 폴더에서 `claude --continue` 또는 `claude --resume`.

## 0. 사용자와 일하는 방법 (가장 중요한 약속)
1. **모든 답변은 한국어.** (자동 메모리 `korean-only-replies`)
2. 사용자는 프로젝트 폴더 `C:\Users\HB_OPERATION_NICK\AndroidStudioProjects\NopoCheonnyeon`에서 안드로이드 앱 **노포 천년 미식가**(`com.nopo.cheonnyeon`)를 만든다. 해커톤 심사위원 시연(공기계 설치)이 목표였다.
3. 개발은 **묻지 말고 계속 진행**하라는 포괄 승인이 있다. 단 **사용자 판단이 필요한 것(키 폐기, 요금제, 공개 범위 등)은 제일 나중으로 미루고 나머지를 전부 끝낸다**(2026-10-21 지시).
4. **자주 커밋·푸시**한다(PC가 꺼질 수 있음). 커밋 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
5. **비밀을 쓰지 않는다.** 키·비밀번호를 코드·문서·로그·채팅에 쓰지 않는다. `local.properties`, `keystore.properties`, `*.jks`, `app/google-services.json`은 읽지 않는다.
6. 위험하거나 되돌리기 어려운 일(공개 업로드, 삭제, 요금제 변경)은 확인 후에 한다. 자동 승인 분류기가 막은 일을 우회하지 않는다.
7. 사용자가 직접 해야 하는 시험(영수증 촬영, 실제 가게 앞 30m)은 "통과했다고 가정"하지 말고 **"미확인(사용자)"로 따로 적는다**(QA 문서 방식).

## 1. 시간순 요청과 결과
### 1-1. 2026-10-19~20 (기반 구축 — 이전 세션, 요약)
- PRD v0.4 기준 전체 앱 구현: 로그인 게이트, 지도, 가게 상세, 영수증 인증(위치 미사용), 사진·위치 인증(30m), 도장·세월·칭호, 패스포트, 리뷰·프로필, 오프라인 대기열, 시연용 데이터. 스토리 단위로 `S-xxx: 요약` 커밋.
- 자세한 로그: `docs/dev-status.md` 10-1~10-12장.

### 1-2. 2026-10-20~21 (이번 대화에서 받은 요청, 순서대로, 모두 완료)
| 순서 | 사용자 요청(요약) | 결과 |
|---|---|---|
| 1 | 지도 하단 시트의 가게 사진·상호를 눌러도 상세로 | `StoreSheet`의 사진+이름 영역 클릭 → 상세 |
| 2 | 다른 가게를 보다 "내 위치" 누르면 내 위치로 재중심, 위치를 1~2초 안에 | `MapViewModel.recenter`, `FusedLocationAdapter.quickFix`(마지막 위치 우선) |
| 3 | 화면별 미리보기와 Firestore 호출 시험 | @Preview 다수, 호출 시험 |
| 4 | 도장 애니메이션, 칭호 100년 단위(11단계), 공유 카드에 QR, "시연용 예시 리뷰" 문구 삭제, 테스트 가게 리뷰 화면 점검, 사진 리뷰, 송파 5곳 사진 더미 리뷰, Gemini 활용 아이디어 | `Title` 11단계, `StampCeremony`, `PassportShareCard`+`QrCode`, 리뷰 사진 |
| 5 | 리뷰를 또 쓰면 이전 리뷰가 사라짐, 도장 후 연출 순서, 연출을 동영상으로 | 리뷰 슬롯 1~5, 연출 순서 정리, `docs/media/stamp_ceremony_demo.mp4` |
| 6 | 영수증 인증 후 리뷰 화면이 안 뜸 / 리뷰 사진은 앨범에서 최대 3장(파일 저장 금지) / 시연 사진이 구림 → 실제 음식 사진 / 칭호 배지 색 변화·도감 / 칭호 연출을 더 화려하게 | 리뷰 사진 예외 규칙(CLAUDE.md), 음식 사진 자산+출처 기록, `TitleDexDialog`, `TitleUpDialog` |
| 7 | 새 폰(Galaxy S9+) 설치 후 개발 계속 | S9+(adb `212340b20e017ece`)가 시험 전용 폰 |
| 8 | AI 메뉴 룰렛(메뉴 3개, 최대 30개), 가게 리뷰 AI 분석 — 환경변수 `GEMINI_API_KEY` 사용 | 당시에는 키를 BuildConfig로 주입(이후 10장에서 제거) |
| 9 | 10분마다 커밋·푸시, 컴퓨터가 꺼지기 전까지 묻지 말고 개발 | 자동 저장 커밋(메모리가 부족해 한때 중단됨) |
| 10 | 시연 2시간 전: 영수증 레이저·형광펜 스캔 연출 + AI 실패 팝업 + 테스트 영수증, AI 번역, 지도 위 AI 챗봇(말로 찾기·천년 경로·오늘의 코스, 기록 보존) | `ReceiptScanView`, `AiRepository.receiptHint`, `StoreTranslator`, `ChatScreen`/`ChatStore` |
| 11 | 시나리오가 뒤죽박죽 → 전부 지우고 다시, 시험용 가게 복원 | `docs/demo-guide.md`, `tools/test-images/demo_scenarios.html` |
| 12 | 단골 충성도 AI 점수, 서울 가게마다 리뷰 +10 | `LoyaltyScore`(AI 보정 ±12), 시연 리뷰 4,956개 |
| 13 | 리뷰 남기기→방문 인증 후 작성, 간판 인증에도 스캔 연출+AI 안내, 검색창과 AI 도우미 합치기, 내 사진 리뷰 10개+룰렛 사진 릴 | 구현 완료 |
| 14 | 예시 질문이 보이게 | 챗 화면 예시 3개 |
| 15 | 최종 릴리스 APK + QR 다운로드, 저장소 공개 허용 → "키 노출돼도 되니 올려줘" | 릴리스 `v0.1.0-demo`, QR(`docs/media/download_qr.png`) |
| 16 | QA 검수 문서 | `docs/qa-final-2026-10-21.md` |

### 1-3. 2026-10-21 저녁~10-22 (이 문서 직전 세션)
| 순서 | 사용자 요청/결정 | 결과 |
|---|---|---|
| 17 | "APK에서 API 키를 빼도 같은 기능이 유지되게?" → 설명 후 "A(Cloud Functions)로 진행" | Cloud Functions 프록시 코드를 만들었으나 **Blaze 요금제가 필요**해 중단 |
| 18 | "Blaze로 못 바꾸면 Firebase AI Logic으로?" → "Gemini Developer API로 시작했다" | `firebase-ai`로 교체. 앱에는 키가 없다. 프록시 코드는 삭제 |
| 19 | App Check 때문에 호출 실패(`App Check token is invalid`) → 사용자가 콘솔에서 AI Logic 적용을 **해제** | 약 10분 뒤 정상. **시연 뒤 다시 켜야 하고, 2026-11-02부터 적용이 의무(공식 문서)** |
| 20 | 모델이 붐벼 응답이 40초 넘게 멈춤 | 25초 제한 + 예비 모델 1회 재시도(예비는 `gemini-3.5-flash-lite`, 2.5는 신규 사용자에게 막힘), 추론 최소화 |
| 21 | 앱 아이콘 제작 | `tools/gen_app_icon.py` → 청록 간판+붉은 도장("노포") 적응형 아이콘 |
| 22 | 중간 평가 기획안 문서 5종(참고 양식 = `ex_copy/`의 4개 파일 + Google 문서 URL) | `중간 평가 기획안/`에 PDF·발표자료 HTML·제작기 HTML·docx 2개, 생성 스크립트 `tools/mid_eval/` |
| 23 | 문서 후에 하던 코드·QA 마무리 | 번역 묶음 동시 요청, 리뷰 사진 화면 회전 유지, 릴리스에서 오류 문구가 사라지는 버그(keep.xml), QA 문서 10장 |
| 24 | 램·디스크 부족 → `app/build`와 Gradle 옛 캐시 3개 삭제 허용 | 릴리스 재빌드 성공(7분), 공개 릴리스 교체 |
| 25 | 대화 이어받는 법 문의, 인수인계 요청 | 이 문서와 메모리 `project-state-2026-10-22` |

## 2. 사용자가 이미 내린 결정 (되돌리지 말 것)
- 저장소 **공개** 전환 허용(`https://github.com/ohdangchan/NopoCheonnyeon`). 공개 이후에는 비밀이 커밋에 들어가면 안 된다.
- 이전 공개 APK에 들어 있던 Gemini 키 노출은 **데모 한정으로 감수**했고, 이후 키를 APK에서 뺐다. **기존 키 폐기는 아직 사용자가 하지 않았다.**
- 영수증 인증은 **위치를 쓰지 않는다**. 영수증 이미지는 저장하지 않는다.
- 방문 인증은 앱 안 카메라만(갤러리 금지). **리뷰 사진만 예외**(카메라 또는 시스템 사진 선택기, 최대 3장, 줄여서 문자열로만).
- 칭호는 세월 100년 단위 11단계, 도장 등급 30~49 일반 / 50~99 붉은 / 100+ 금빛.
- 시연 모드(검사 건너뛰기)는 만들지 않고 "테스트 가게"로 실제 로직을 시험한다.
- 디버그·릴리스 패키지명 동일(`applicationIdSuffix` 금지). Gradle/AGP/Kotlin 버전을 임의로 올리지 않는다.
- 아키텍처 문서에 없는 라이브러리는 이유를 말하고 먼저 묻는다(`firebase-ai`는 사용자가 방식을 정하면서 승인).
- 앱 아이콘, 중간 평가 문서 양식은 사용자가 요청한 대로(청록·인주·놋쇠 팔레트).

## 3. 지금 사용자 판단을 기다리는 것 (건드리지 말고 사용자에게 물을 것)
1. **기존 Gemini 키 폐기**(Google AI Studio/Cloud 콘솔). 새 APK는 이 키를 쓰지 않으므로 지워도 앱에 영향 없음.
2. **시연 뒤 Firebase 콘솔 → 보안 → App Check → APIs에서 AI Logic 적용을 다시 켜기.** 지금은 꺼져 있어 다른 앱이 이 프로젝트의 무료 AI 할당량을 쓸 수 있다.
3. **2026-11-02 이후 AI 호출 방식:** (가) Cloud Functions 프록시 — Blaze 요금제 필요, (나) App Check 제공자 — 공개 APK(사이드로드)가 Play Integrity를 통과하는지는 검증하지 못함.
4. Google Places 사진(앱 자산 약 26MB) 약관 확인, 서명 키 백업 2곳, 노출된 Stitch/네이버 키 폐기, 시연 후 예시 데이터 끄기.

## 4. 사용자만 할 수 있는 확인 (QA 문서의 "미확인(사용자)")
영수증·간판 촬영 연출(레이저→형광펜)과 AI 촬영 도우미 팝업 화면, 강화된 서버 규칙으로 **실제 가게 리뷰 저장**(가장 중요), 리뷰 사진 카메라·앨범 업로드, 다른 폰에서 QR 스캔 설치, 큰 글씨·Monkey·오프라인·두 번째 계정. 예비 모델의 실제 응답은 기본 모델이 정상이라 한 번도 시험되지 않았다.

## 5. 현재 상태 (2026-10-22 기준)
- 코드: 단위 테스트 **291개 + 추가분 통과**, lint 오류 0(경고 25). 최신 릴리스 APK = 폰(S9+)에 설치된 앱 = 공개 릴리스 `v0.1.0-demo`(88,027,991바이트, 키 없음).
- AI: Firebase AI Logic 경유. 챗봇·룰렛(사진 6장)·번역·리뷰 분석·충성도를 릴리스 앱에서 폰으로 확인(`qa-final` 10장). 촬영 도우미(이미지)는 미확인.
- 문서: `중간 평가 기획안/` 5개 파일(생성 스크립트 `tools/mid_eval/`). 양식 참고 원본 `ex_copy/`는 저장소에 올리지 않는다(.gitignore).
- 알려진 한계: 리뷰 목록이 사진 base64를 전부 읽음(썸네일 필드 필요), `reviewCount`·`totalYears` 위조 가능(서버 함수 필요), 서버 규칙 에뮬레이터 시험을 한 번도 못 돌림(`tools/rules-test/`, 이 PC에서 jar 다운로드 실패), 번역은 가게에 따라 25초 이상.

## 6. 이 PC에서 일할 때의 함정
- **램 7.4GB·디스크 여유가 작다.** 기본 `assembleRelease`는 멈춘다. 이렇게 빌드한다: `./gradlew.bat assembleRelease --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process --console=plain`(약 7분). 빌드 전 Android Studio를 닫으면 램 1.6GB가 확보된다. 시스템이 메모리 부족으로 백그라운드 명령을 죽였다면 **사용자가 말하기 전에 다시 시작하지 말 것.**
- **Gradle이 `Unable to establish loopback connection`으로 죽으면**(Java 25가 임시 폴더에서 내부 연결을 못 엶): `mkdir C:\gt` 후 `export JAVA_HOME=/d/android/android_studio/jbr TEMP='C:\gt' TMP='C:\gt' JAVA_TOOL_OPTIONS='-Djava.io.tmpdir=C:\gt'` 를 걸고 실행한다. `JAVA_HOME`이 비어 있으면 Android Studio 안의 `D:\android\android_studio\jbr`를 쓴다(2026-10-22 확인).
- Git-Bash heredoc에 한글·따옴표가 섞이면 깨진다 → 스크립트는 Write 도구로 파일을 만들어 실행. 파이썬에서 문자열 안의 `\n`이 실제 줄바꿈으로 들어가는 일이 반복됐다 → 소스를 고칠 땐 Edit 도구를 쓰거나 `chr(92)`로 역슬래시를 만든다.
- `adb`: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, 경로 인자에 `MSYS_NO_PATHCONV=1`. 폰이 바탕화면이면 앱이 아닌 다른 앱(날씨 등)을 건드리지 않도록 먼저 앱이 앞에 있는지 `dumpsys window | grep mCurrentFocus`로 확인.
- 폰 화면 조작 좌표는 S9+ 1080×2220 기준(`tools/device_translate_check.sh`에 예).
- Firebase CLI 로그인은 만료돼 있다(`firebase login --reauth` 필요). 규칙 배포: `cd firebase && firebase deploy --only firestore:rules --project nopocheonnyeon`.
- Gemini 키는 환경변수였으나 **이제 필요 없다**(앱에서 제거). 빌드 환경에 키 변수가 남아 있어도 앱은 무시한다.

## 7. 새 에이전트 시작 체크리스트
1. `CLAUDE.md` → 이 문서 → `docs/dev-status.md` 맨 위 → `docs/qa-final-2026-10-21.md` 순서로 읽는다.
2. `git status`, `git log --oneline | head`로 상태 확인. 작업 트리가 깨끗해야 한다.
3. 사용자에게 묻기 전에 3장의 "대기 중인 결정"을 다시 확인하고, 그 외에는 질문 없이 개발·시험을 이어간다.
4. 큰 단계마다 단위 테스트(`./gradlew.bat testDebugUnitTest`) → 커밋 → 푸시.
5. 답변은 한국어.

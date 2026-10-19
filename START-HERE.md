# 노포 천년 미식가 v0.4 — 여기서 시작

**최우선 목표: APK를 실제 폰에 설치했을 때 기획한 모든 기능이 버그 없이 동작하는 것.**
Google Play 등록은 다음 버전입니다.

## 들어 있는 것 (모두 최신판 v0.4, 같은 내용은 한 번만)
| 구분 | 파일 | 내용 |
|---|---|---|
| 기획안 | `docs/nopo-planning.html` | 지금까지의 기획 전체를 담은 한 장짜리 문서 (24개 장, 브라우저로 열기·인쇄 가능) |
| 개발 문서 | `docs/nopo-01-PRD.md` | 요구사항 80개, 인증 규칙·오류 코드, 데이터·서버 구조, 결정 기록 14개 |
| | `docs/nopo-02-architecture.md` | 기술 스택, 패키지 구조, Room·Firestore 설계, 보안 규칙, Gradle, 결정 기록(ADR) 12개 |
| | `docs/nopo-03-stories.md` | 스토리 76개, 5일 일정(개발 4일 + QA 1일), 컷라인 |
| | `docs/nopo-04-dev-qa-checklist.md` | 테스트 130개 (단위·통합·실기기·안정성·회귀), 디버깅 포인트 |
| | `docs/nopo-05-precheck-checklist.md` | 개발 전에 직접 할 일 40개 |
| | `docs/nopo-06-spike-plan.md` | 1일차 오전 기능 검증 5종 (지도·카메라·한국어 OCR·위치·Firebase) |
| | `docs/nopo-07-firebase-setup.md` | 서버·DB(Firebase) 설정 10단계 |
| | `docs/nopo-08-claude-code-guide.md` | Android Studio + Claude Code 설치·작업 루프·폰 설치 |
| Claude Code 설정 | `CLAUDE.md`, `.claude/settings.json`, `.claude/agents/nopo-qa.md` | 작업 지침, 명령 허용·키 파일 차단, QA 서브에이전트 (`.claude`는 숨김 폴더) |
| 서버 설정 | `firebase/firestore.rules`, `firestore.indexes.json`, `firebase.json` | 보안 규칙, 색인, 배포 설정 |
| 앱 데이터 | `app/src/main/assets/stores.json` | 앱이 첫 실행에 적재하는 노포 카탈로그 |
| 노포 데이터 | `data/nopo_stores.csv` | 통합 카탈로그 (엑셀에서 바로 열림, 백년가게만 보려면 `programs` 열로 거르기) |
| | `data/nopo_stores.db` | 같은 내용의 SQLite DB |
| | `data/stats.json`, `data/README.md` | 수치와 열 설명·수집 방법·한계 |
| | `data/raw/` (4개) | 수집 원본, 가게당 한 줄로 정리: 백년가게 목록·상세, 오래가게, 선정 회차 |
| 도구 | `tools/build_catalog.py` | 원본 → CSV·DB·stores.json 생성 |
| | `tools/geocode.py` | 주소 → 좌표 보강 (네이버·카카오 키 필요) |
| | `tools/import_official_list.py` | 공공데이터포털 지정리스트 CSV로 빠진 백년가게 보충 |
| | `tools/crawl_baeknyeon.py` | 소상공인마당 상세 재수집 |
| | `tools/test-images/` (3개) | 테스트 영수증(현재 시각 HTML·PNG), 테스트 간판 |

## 준비할 키 (모두 저장소에 올리지 않음)
| 키 | 어디서 | 어디에 넣나 |
|---|---|---|
| 네이버 지도 Client ID | 네이버 클라우드 콘솔 → Maps 애플리케이션 등록(Dynamic Map·Geocoding 선택, 패키지명 `com.nopo.cheonnyeon`) | `local.properties`에 `NAVER_MAP_KEY_ID=…` |
| 같은 앱의 Client ID·Client Secret | 위와 같은 화면 | 좌표 보강 때만 환경 변수 `NCP_KEY_ID`(Client ID), `NCP_KEY`(Client Secret) |
| (선택) 카카오 REST API 키 | 카카오 디벨로퍼스 → 내 애플리케이션 | 환경 변수 `KAKAO_REST_KEY` (주소 없는 오래가게 좌표 찾기) |
| `google-services.json` | Firebase 콘솔 → Android 앱 등록 (`docs/nopo-07-firebase-setup.md`) | `app/` 폴더 |
| 릴리스 서명 키 `.jks` | Android Studio → Build → Generate Signed App Bundle/APK → 새 키 | `keystore.properties`에 경로·비밀번호, 백업 2곳 |

## 순서
1. `docs/nopo-05-precheck-checklist.md`의 A·B: Claude 유료 요금제(Pro 이상 또는 Console 계정), Claude Code 설치, 위 키 준비
2. Android Studio → New Project → Empty Activity, 패키지 `com.nopo.cheonnyeon`, Minimum SDK API 26 → 이 폴더 내용을 프로젝트 루트에 복사 (숨김 폴더 `.claude` 포함)
3. `.gitignore`에 `keystore.properties`, `*.jks`, `app/google-services.json` 추가 후 첫 커밋
4. 좌표 보강 (1일차 전 권장, 늦어도 1일차 오후 S-301 전). Python 3.10 이상이 필요하고, 프로젝트 루트의 PowerShell에서:
   ```
   pip install requests
   $env:NCP_KEY_ID="Client ID"; $env:NCP_KEY="Client Secret"
   python tools/geocode.py --provider naver
   $env:KAKAO_REST_KEY="카카오 REST 키"; python tools/geocode.py --provider kakao
   python tools/build_catalog.py
   ```
   (macOS·Linux 터미널은 `export NCP_KEY_ID=…` 형식. Claude Code에 “START-HERE 4단계를 해줘”라고 맡겨도 됩니다.)
5. (선택) 공식 지정리스트로 빠진 백년가게 보충: https://www.data.go.kr/data/15132695/fileData.do 에서 CSV 내려받기 → `python tools/import_official_list.py --csv 받은파일.csv` → `python tools/build_catalog.py`
6. Android Studio 터미널에서 `claude` 실행 → `docs/nopo-06-spike-plan.md의 SPK-0을 해줘`
7. 1일차 오전 SPK-1~5(지도·카메라·한국어 OCR·위치·Firebase)가 실기기에서 통과하면 `docs/nopo-03-stories.md`의 S-101부터 한 스토리씩

## 데이터 현황 (2026-10-18 22:52 기준)
| 항목 | 값 |
|---|---|
| 백년가게 전체 / 음식점업 | 1,139 / 751 |
| 백년가게 상세 수집 | 1,139 |
| 백년가게 창업일 확보 | 1,139 (업력 30년 미만으로 나오는 207곳은 앱에서 30년) |
| 서울 오래가게 (선정 발표 기준 수집 / 앱 포함) | 173 / 151 |
| 앱 카탈로그 | 1,277 |
| 좌표 | 0 → 컴퓨터에서 `python tools/geocode.py --provider naver` (주소 없는 오래가게는 `--provider kakao`) 후 `python tools/build_catalog.py` |

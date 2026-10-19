# 개발 현황·인수인계 (이어서 개발하는 에이전트용)

> 최종 갱신: 2026-10-20. 이 문서를 먼저 읽고, 그다음 `CLAUDE.md` → `docs/nopo-01-PRD.md` 순서로 읽는다.
> 목표: **오늘 오후 해커톤에서 공기계에 설치해 심사위원에게 시연**. 기획된 기능 전부 구현 + QA 체크리스트 통과 문서(`docs/qa-report.md`).

## 0. 사용자 지시 요약 (2026-10-20)
- 모든 것은 사전 승인됨. 사용자에게 묻지 말고 기획안(PRD)에 적힌 모든 것을 개발하고 QA까지 진행한다.
- 사용자가 직접 해야 하는 시험(실제 영수증 촬영, 실제 가게 앞 30m 인증)은 "통과했다고 가정"하고 개발을 계속하며, **마지막 QA 문서에 수동 시험 항목으로 넣는다.**
- 사진 없는 가게는 글자 카드로 대신한다. 이후 개선안: 웹에서 사진을 내려받아 앱에 포함 또는 구글 Places API (1,277곳×3장 ≈ $20, 약관 확인 필요).
- 글자 크기는 알아서 (본문 ≥14sp, 짧은 라벨 ≥12sp).
- 한도 도달 시: 세션 95% 또는 주간 99%가 되면 그때까지의 모든 것을 커밋·푸시하고 이 문서를 최신으로 고쳐 다른 에이전트가 바로 이어받게 한다. 세션 한도가 풀리면 주간 99%까지 계속 진행.
- 모든 채팅·진행 안내는 한국어.

## 1. 프로젝트 요약
- 앱: 노포 천년 미식가 (`com.nopo.cheonnyeon`), Kotlin 2.2.10 + Jetpack Compose, AGP 9.4.1, Gradle 9.6.0, minSdk 26 / target 37, 단일 모듈.
- 구조: `core/`(designsystem·common·util·navigation) `domain/`(model·rule·verify·catalog) `data/`(local Room·DataStore, catalog, remote Firebase, visit) `platform/`(camera·ocr·location·map·permission) `feature/`(auth·map·store·verify·passport·settings·main·diagnostics).
- 수동 DI: `app/AppContainer.kt`. 내비게이션: `core/navigation/NopoNavHost.kt` (세션 상태로 로그인 게이트).
- 서버: Firebase 프로젝트 `nopocheonnyeon` (계정 songpa24@iceu.kr), Auth(이메일) + Firestore `(default)` asia-northeast3. 규칙·색인: `firebase/` (배포: `cd firebase && firebase deploy --only firestore:indexes,firestore:rules --project nopocheonnyeon`).
- 지도: 네이버 지도 SDK 3.24.0 + Reverse Geocoding. 주소→좌표 변환: 네이버 Geocoding (REST, 한글을 **UTF-8**로 보낼 것. Windows Git-Bash curl은 인코딩이 깨져 0건이 나옴).
- 카탈로그: `app/src/main/assets/stores.json` (생성: `python tools/build_catalog.py`). 가게 1,277곳, 좌표 1,181곳, 공식 사진 86곳.

## 2. 이 PC에서만 있는 비밀 파일 (저장소에 없음)
| 파일 | 용도 | 없으면 |
|---|---|---|
| `local.properties` | 네이버 지도 Key ID / Client Secret (`local.properties.sample` 참고) | 지도 인증 실패 |
| `app/google-services.json` | Firebase 설정 (Firebase 콘솔에서 다시 받기) | 빌드 실패 |
| `keystore.properties` (프로젝트 루트) + 키 파일 | 릴리스 서명 (`storeFile=C:/Users/HB_OPERATION_NICK/AndroidStudioProjects/jks/nopo_release_key`) | 릴리스가 디버그 키로 서명됨 |

## 3. 명령 (Windows)
- 단위 테스트: `.\gradlew.bat testDebugUnitTest` (현재 130개 이상 통과)
- 디버그 설치: `.\gradlew.bat installDebug` (폰 R5KL806MA4W, USB)
- 릴리스 빌드: `.\gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (arm64-v8a만, 약 47MB)
- 디버그와 릴리스는 서명이 달라 폰에서 서로 덮어쓸 수 없다 (한쪽을 지우고 설치).
- Git-Bash에서 파일 여러 개를 heredoc으로 한꺼번에 만들면 파싱 오류가 난다 → Write 도구나 `python scratch.py` 파일 방식 사용.

## 4. 완료된 스토리
E0 진단(SPK-0~6) · E1 기반(S-101~106) · E2 도메인 규칙 전부(테스트 포함) · E3(S-301~307) · E4(S-401·402·403~407) · E5(S-501~505) · E6(S-601~609: 영수증·사진·위치 인증, 3회 실패 시 위치 확인 도장) · E7(S-701~703) · E8(S-801·802) · S-506 리뷰 요약 · S-901/902 테스트 가게 · S-903 설정.
세부는 `git log` 참고 (커밋 메시지 `S-xxx: 요약`).

## 5. 남은 일 (우선순위 순)
1. S-804 유저 프로필 + S-803 리뷰→프로필 이동 + S-704 내 리뷰 탭
2. 지도 줌아웃 시 지역별 개수 원 (FR-MAP-04 클러스터)
3. P1: S-408 닉네임 변경·탈퇴, S-705 칭호 승급 연출, S-805 리뷰 신고
4. S-1001 카탈로그 최종 점검 (좌표 없는 96곳은 영수증 인증만 가능, `E_NO_COORD`)
5. S-1004 안정성 (Monkey 5,000회, 회전·백그라운드·권한 철회)
6. S-1003 QA 문서 `docs/qa-report.md` (자동 검증 결과 + 사용자 수동 시험 항목)
7. 시연 준비: 릴리스 APK 설치 안내 `docs/demo-guide.md`

## 6. 알려진 문제·결정
- 사진 서버(sbiz24.kr)는 `Accept` 헤더 없는 요청에 500 → `NopoApp.newImageLoader`가 헤더를 붙임.
- Room은 개발 중이라 `fallbackToDestructiveMigration` 사용 (`AppDatabase.kt`). 출시 전 정식 마이그레이션으로 교체할 것 (스키마를 바꿀 때마다 버전 올리기).
- 네이버 Client Secret이 APK에 들어간다 → 공개 배포 전 콘솔에서 사용 한도 설정 또는 교체.
- 채팅에 노출된 Stitch 키·네이버 IAM 키는 사용자가 폐기해야 함.
- 리뷰 쓰기는 방문 문서가 서버에 있어야 허용됨(규칙). 테스트 가게는 서버로 보내지 않으므로 리뷰 불가(의도).
- D-15: 릴리스 APK는 arm64-v8a만 (x86 제외로 147MB → 약 47MB).

## 7. 사용자가 직접 해야 하는 QA (최종 문서에 포함)
- 실제 영수증 촬영 인증 (테스트 영수증: `tools/test-images/test_receipt_live.html`)
- 실제 노포 앞 30m 이내 사진·위치 인증
- 서명 키(`jks`) 백업 2곳 이상

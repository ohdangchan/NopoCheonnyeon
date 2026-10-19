# Stitch 시안 ↔ PRD 차이점 (구현 시 실수 방지)

> **원칙: 기준은 PRD(`docs/nopo-01-PRD.md`)와 아키텍처 문서다. Stitch 화면은 레이아웃·분위기 참고용이다.**
> 화면을 만들기 전에 이 표에서 해당 화면을 먼저 찾는다. 여기 적힌 요소는 그대로 옮기지 않는다.
> 작성: 2026-10-19, Stitch 화면 51개 전체(문구·스크린샷)를 PRD v0.4와 대조.

## 0. 모든 화면 공통

| Stitch | 실제 구현 | 근거 |
|---|---|---|
| HTML의 Tailwind 색(`#00524b` 등 자동 생성 M3 팔레트) | `core/designsystem/theme` 토큰만 사용 | PRD 10.1, CLAUDE.md |
| 화면 상단의 `S-xxx`, `FR-xxx`, `CODE:` 배지, "v2.4", "Build 42" 같은 개발 표기 | 사용자 화면에 넣지 않음 (진단 화면 제외) | — |
| "공인", "국가 표준", "보존위원회", "官印認證", "블록체인 해시", "AES-256", "위변조 방지 블록" | 쓰지 않음. 앱은 기관과 무관하며 공식 인증처럼 보이면 안 됨 | 사전점검 E, PRD 1.3 |
| 예시 숫자(442년, 이문설농탕 4.7점 48명 등)·가게 사진 | 실제 데이터로 채움. 예시 사진은 넣지 않음 | — |
| 하단 탭 이름 "지도 탐색 / 내 패스포트 / 설정" | 그대로 사용 가능 | 아키텍처 3장 |
| 칭호 기준 | 새싹 0 / 단골 100 / 동네 어른 300 / **장인 600** / 천년 1,000 | PRD 6.2 |
| 도장 등급 | 30~49 청록 / 50~99 붉은 / 100+ 금빛 | PRD 6.2 |

## 1. 금지 기능이 그려진 화면 (만들지 않는다)

| 화면 | Stitch에 있는 것 | 처리 |
|---|---|---|
| `verify/receipt_verify` | 오른쪽 아래 **갤러리 버튼** | 제거. 앱 안 카메라만 (CLAUDE.md, FR-VER-03) |
| `verify/receipt_verify` | 상단 "간판·위치 인증" 탭 전환 | 탭 대신 하단 "가게 사진으로 인증하기" 버튼 (FR-VER-08) |
| `review/review_editor` | 글 입력칸의 **사진 첨부·영수증 첨부 아이콘**, 키워드 칩 | 사진 첨부 없음 (PRD 1.3 비목표). 키워드 칩은 넣지 않음 |
| `review/review_edit` | "공인 방문 실물 사진 관리 / 사진 교체" | 제거 |
| `store/store_reviews` | 리뷰 **사진 2장**, **도움됨·댓글** 버튼, 해시태그, 5점~1점 분포 막대 | 사진·좋아요·댓글 없음 (PRD 1.3). 분포 막대도 PRD에 없음 → 평균·개수만 (FR-STORE-03) |
| `passport/my_reviews` | 리뷰 사진, "결제 일련번호 #9021-KR" | 사진·결제 정보 표시 안 함 |
| `store/report` | 사진 첨부, "10년 헤리티지 포인트", "24시간 현장 교차 검증" | P1. 만들 때는 사유 선택만 (FR-REV-08), 보상·약속 문구 없음 |
| `verify/verify_result` | "테스트 가게로 모의 인증" 링크 | 제거. 테스트 가게는 진단 화면에서만 (FR-SPK-02) |
| `store/directions` | 카카오맵 버튼, 대중교통 하차 안내, 골목 설명 | 네이버 지도 앱 → 없으면 웹 지도만 (FR-STORE-05). 안내 문구는 데이터가 없으므로 넣지 않음 |
| `passport/passport_empty` | "테이블 **QR 태그** 또는 영수증 **GPS** 인증", "은장 패스포트 커버" | QR은 확장 계획, 영수증은 위치를 쓰지 않음 (FR-VER-02). 문구 교체 |

## 2. 서버·화면에 남기지 않기로 한 정보를 보여 주는 화면

PRD 8.2: 위치 좌표·영수증 이미지·인증 시각(시:분)은 서버에 저장하지 않는다. FR-PRF-06: 프로필에 인증 방법·시각 비표시.

| 화면 | Stitch에 있는 것 | 처리 |
|---|---|---|
| `passport/stamp_detail` | 첫 방문 "12:34 KST", **영수증 해시**, "분산 아카이브 서버 영구 각인" | 날짜만. 해시 표시 안 함. 인증샷은 "내 기기에만 보관" 안내 유지 (FR-PAS-03) |
| `passport/gold_certificate` | "현장 GPS 지오태깅", "테이블 넘버 7번", 사진 해시, "1080x1350 HDR RAW", 인증서 번호 | 확인 필요 화면. 만들더라도 위 정보 전부 제외 |
| `passport/my_reviews` | "GPS 45분 잔류 확인" | 제거. 인증 방법만 (영수증/사진/위치 확인) |
| `verify/daily_limit` | 오늘 인증한 가게의 "인증 시각 11:42" | 기기 `my_visits`의 시각이라 내 화면에는 표시 가능. 단 서버로 보내지 않음 |
| `verify/receipt_duplicate` | 해시 원문 "02-733-****\|202610181340\|13000", "국세청 승인번호" | 해시·영수증 내용 표시 안 함. "이미 사용된 영수증이에요"(E_RECEIPT_DUP)와 사진 인증 버튼만 |
| `settings/account` | 내부 UID, 인증샷 저장 경로 `filesDir/stamps/` | 사용자 화면에 경로·UID 노출 안 함 |

## 3. 규칙·숫자가 PRD와 다른 화면

| 화면 | Stitch | PRD 기준 |
|---|---|---|
| `passport/hall_of_fame` | 승급 기준 "장인 식객 **700년**" | 600년 (PRD 6.2). 화면 자체가 PRD 외 |
| `settings/notifications` | 칭호 "**한양 도편수**", "단골 3회차 **추가 가산 세월**", "24시간 쿨타임" | 존재하지 않는 칭호. 재방문은 세월 없이 단골 횟수만 (FR-STP-05). 같은 가게 제한은 "서울 자정 기준 하루 1회" (FR-VER-14) |
| `profile/user_profile` | 게이지 눈금 "새싹(100년) 단골(300년)" | 라벨 오류. 새싹 0 / 단골 100 / 동네 어른 300 / 장인 600 |
| `settings/settings` | 방문 인증 "반경 **50m**" | 30m + 정확도 30m (FR-VER-09) |
| `map/store_list` | "시즌 한정 누적 세월 보너스 **1.5배**" | 보너스 없음. 세월 = 인증 당시 업력 (FR-STP-01) |
| `passport/collections` | 퀘스트 보상 "세월 보너스 **+30년**" | 보너스 없음. 화면 자체가 확장 계획 |
| `stamp/title_promotion` | "東네 어른" (오타), "보증 마크 각인" 특권 | "동네 어른". 특권 문구 없음. P1 (S-705) |
| `map/map_filter` | 업력 50·100년 필터, 평점순·단골순 정렬, 업종 4분류 | P0 필터는 음식점만/전체, 백년가게/오래가게, 안 가 본 곳 (FR-MAP-03). 정렬은 가까운 순/업력 순 (FR-MAP-07) |
| `map/map_filter`, `auth/onboarding` | 음식점 751곳, "전국 1,300여 개", "서울 백년오래가게" | 숫자는 카탈로그에서 계산해 표시 (현재 앱 1,277곳, 음식점 834곳). 제도 이름은 "백년가게", "오래가게" |
| `verify/verify_result` | 오류 코드 "E-VER-402" | PRD 7.4의 코드만 사용 (예: E_RECEIPT_MISMATCH) |
| `auth/login` | "자동 로그인 유지" 체크박스, "오류 5회 초과 시 일시 제한" | 로그인은 항상 유지 (FR-AUTH-03). 제한 문구는 Firebase가 막을 때만 AUTH_TOO_MANY |
| `settings/account` | "30일 내 1회만 변경 가능" | PRD에 없음 (S-408 P1에서 결정) |
| `diagnostics/apk_install_guide` | "안드로이드 **10** 이상", 가짜 SHA-256 지문 | minSdk 26 = 안드로이드 **8.0** 이상. 지문은 실제 서명 키에서 계산 |
| `diagnostics/ocr_inspector` | "Tesseract", 사업자번호 101-04-12345를 "VALID" 표시 | ML Kit만 사용. 이 번호는 체크섬이 **틀린 번호** (PRD 7.5) — 테스트 값은 123-45-67891 |
| `diagnostics/test_store` | `test_signboard.png`, "Tesseract-Lite", 주소 "인사동길 12" | 실제 파일은 `tools/test-images/test_sign.png`. 주소는 현재 위치 기준 (FR-TST-01) |
| `diagnostics/spike_console`, `system_diagnostics` | 측정값(42ms, 94%, ±7.4m, "5/5 PASSED") | 전부 예시. 실제 측정값을 표시 |

## 4. 사실처럼 보이는 가짜 정보 (그대로 쓰면 안 됨)

| 화면 | 내용 | 처리 |
|---|---|---|
| `settings/privacy_policy` | 가짜 사업자번호 104-86-99201, 주소 "인사동길 44 백년기념관 3F", 전화, "CISO", 근거 불명 법 조항·암호화 방식 | 처리방침은 실제 운영 정보로 새로 작성 (사전점검 E). 구조(3대 원칙 요약 → 조항별 접기)만 참고 |
| `store/closed_memorial` | 실존 가게의 폐업·화재 서술, 가상 인물 "김영호 님(52세)" 헌사 | 확장 계획(PRD 외). 만들 때도 가상 인물·확인 안 된 사실은 넣지 않음 |
| `diagnostics/ocr_inspector` | 실존 가게의 대표자 실명과 영수증 | 테스트 가게(노포천년테스트식당) 영수증만 사용 |
| `settings/licenses` | 라이브러리 버전(Compose 1.7.0, Room 2.6.1 등)·"18종" | 실제 의존성에서 생성. 글꼴 OFL(Black Han Sans, JetBrains Mono) 포함 |
| `store/report` | "누적 방문식객 8,492명", "BN-11029" | 실제 데이터 |

## 5. 우선순위 (PRD 4·5장 기준) — 이 순서로 만든다

- **P0 (이번 APK)**: auth 5개, map 5개, store_detail, store_reviews, directions(FR-STORE-05), verify 6개, stamp_ceremony, passport, passport_empty, stamp_detail, my_reviews, review 3개, user_profile, settings, licenses, privacy_policy(내용 새로 작성), logout_warning, sync_restore, offline_queue, diagnostics 4개
- **P1**: title_promotion, report, account(닉네임 변경), delete_account, privacy_settings
- **P2·확장(만들지 않음)**: share_card, collections, closed_memorial
- **PRD 외(만들지 않음, 필요하면 PRD부터 수정)**: chronicle_stats, hall_of_fame, notifications, gold_certificate
- **참고 문서 성격**: apk_install_guide → 앱 화면이 아니라 배포 안내문(S-1006)으로 작성

## 6. 참고 문서(`reference/`) 주의

`reference/` 안의 데스크톱 문서들은 **검증된 적 없는 "완료·통과" 주장**을 담고 있다 (테스트 76개 통과, APK 42.8MB, 좌표 97.4%, Monkey 크래시 0 등). 실제가 아니다.
또한 존재하지 않는 파일·명령(`syncDesignTokens`, `figma/`, `.github/`, `docs/nopo-compose-templates.md`, `data/baeknyeon_all.csv`)을 전제로 한다.

- ⛔ **「안드로이드 스튜디오 프로젝트 루트 즉시 자동 복사」 문서의 스크립트를 실행하지 않는다.** `firebase/firestore.rules`를 리뷰 자격 검사 등이 빠진 약한 규칙으로, `strings.xml`을 일부 문구만 있는 파일로 **덮어쓴다**.
- Compose 템플릿 51개는 문구·색 하드코딩, 다크 모드 미대응, 미포함 의존성(material-icons) 사용, 동작 없음 → 화면 구성 아이디어로만 참고.
- Firestore 규칙 테스트 문서의 Jest 코드는 그대로 돌지 않는다 (`.document()` 오타, `createdAt`을 `new Date()`로 넣어 규칙의 `request.time` 검사에 걸림).
- `76404327_노포천년미식가_기획안_v0.4.html`은 **구버전** 기획안이다. 최신은 `docs/nopo-planning.html`.

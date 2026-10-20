# Stitch 화면 51개 ↔ 구현 현황 (2026-10-20)

`design/stitch/screens/*/*/code.html`(텍스트)와 대표 `screen.png`(영수증 인증·지도)를 직접 읽고, 실제 코드(`app/src/main/java/com/nopo/cheonnyeon`)와 대조했다. 기준은 PRD이며 Stitch는 참고용이다(`design/stitch/PRD-차이점.md` 필독 — 시안에 있는 갤러리 버튼·사진 첨부·공인/블록체인 문구·가짜 사업자번호 등은 **쓰지 않는다**).
**범례**: ✔ 구현 / ◐ 일부(기능은 있으나 시안보다 단순) / ✖ 없음 / ⛔ 만들지 않기로 함(PRD 외·P2·금지).

## 공통 디자인 규칙 (시안 → 코드)
- 색·글꼴은 `core/designsystem/theme` 토큰만 사용(시안의 Tailwind 색 금지). 토큰은 PRD 10.1·`reference/DESIGN.md`와 일치: sign #17504A / primary #1F6B63 / stamp #C23B2A / brass #D9A22C / paper #EDF0EA / surface #F8F9F5 / ink #14201E (다크 값 별도, `values-night`, `Color.kt`).
- 글꼴: 숫자·제목 Black Han Sans, 본문은 **시스템 한글 글꼴**(Noto Sans KR 미포함, APK 크기), 수치 JetBrains Mono(가변). 간격 8dp 격자, 모서리 8/12/16/20dp, 터치 48dp.
- 시안의 한자 장식(百年·契印·官印)은 도장 컴포넌트(`StampView`)의 GOLD "百年"만 사용. 나머지 장식 문구("공인", "호적부", "보존위원회" 등)는 쓰지 않는다.
- 시안 하단 탭: 지도 탐색/내 패스포트/설정 — 구현됨(`MainScreen`, 아이콘 없음).
- 시안의 숫자(442년, 이문설농탕 4.7점 등)는 예시. 지도 시안의 핀(등급별 색 + 말풍선 "120년 이문설농탕")은 구현이 `MarkerIcons.BLACK` + 틴트 색(방문=붉은, 100년+=금빛, 그 외 청록) + 줌 ≥14에서 캡션.

## auth
| 시안 | 상태 | 구현 위치 / 시안과의 차이 |
|---|---|---|
| login | ✔ | `AuthScreens.LoginScreen`. 시안의 "자동 로그인 유지" 체크박스 없음(항상 유지, PRD). "5회 초과 일시 제한" 문구는 Firebase가 막을 때만 `AUTH_TOO_MANY` |
| sign_up | ◐ | `SignUpScreen`. 닉네임 실시간 중복 안내·비번 검증 있음. **약관·개인정보 동의 체크박스 없음**(개인정보 한 줄 안내만, `auth_privacy_note`). "보존위원회 공인 규격" 문구 쓰지 않음 |
| reset_password | ◐ | `ResetPasswordScreen`. 같은 안내 문구 정책 준수. 시안의 "help@nopo.kr" 문의 메일은 가짜(쓰지 않음) |
| nickname_setup | ✔ | `NicknameSetupScreen`(+ 다른 계정으로 로그인=로그아웃) |
| onboarding | ◐ | `OnboardingScreen` 3장 텍스트 페이저(건너뛰기·다음·시작). 시안의 패스포트 일러스트·가짜 데이터 카드 없음 |

## map
| 시안 | 상태 | 비고 |
|---|---|---|
| map | ◐ | `MapScreen`: 검색창, 필터 칩 4개, 내 세월 알약, 내 위치 버튼, 하단 시트(`StoreSheet`). 시안의 상단 지도 아이콘·필터 설정 버튼·레이어 버튼·북마크 버튼 없음 |
| store_list | ✔ | `StoreListScreen`(가까운 순/업력 순, 방문 표시). 시안의 "종로 시즌 1.5배 보너스" 배너는 **쓰지 않음**(보너스 없음) |
| map_fallback_list | ◐ | 목록 모드 + 지도 인증 오류 빨간 배너. **자동 전환 없음**(B-18). "마지막 동기화" 표시 없음 |
| map_filter | ⛔/◐ | 별도 설정 시트 없음. P0 필터는 칩으로 충분(PRD 5.2). 업력 50·100년 필터·평점순·최다 방문순은 PRD 외 |
| location_permission | ✖ | 전용 화면 없음. 대신 인증 화면의 `E_LOC_DENIED`/`E_LOC_APPROX_ONLY` 문구 + 설정 열기 버튼. 시안의 "±2,000m" 정밀도 표·"배경 위치 0%" 문구는 근거가 불명확해 쓰지 않음 |

## store
| 시안 | 상태 | 비고 |
|---|---|---|
| store_detail | ✔ | `StoreDetailScreen`: 사진 페이저, 큰 업력, 배지, 보상 박스, 영업·전화·이용정보·메뉴·업종, 길찾기·전화·방문 인증. 시안의 "종로 길손" 같은 가짜 칭호, 메뉴 가격·메뉴 설명은 데이터에 없어 제외 |
| store_reviews | ◐ | `StoreReviewsSection`: 평균·개수, 20개 더보기, 신고. 시안의 5점~1점 분포 막대·도움됨·댓글·사진·해시태그·정렬은 **PRD 비목표라 제외** |
| directions | ⛔/◐ | 별도 화면 없음, 상세의 "길찾기" 버튼이 네이버 지도 앱(`nmap://route/walk`) → 웹 지도 대체 호출. 시안의 대중교통 하차 안내·카카오맵·주소 복사는 데이터가 없어 제외 |
| report | ◐ | **리뷰 신고만**(사유 4종 다이얼로그). 정보 오류 신고(FR-STORE-06)는 없음(B-10). 시안의 사진 첨부·"10년 포인트" 보상 문구는 쓰지 않음 |
| closed_memorial | ⛔ | 확장 계획(가상 인물·미확인 사실 금지) |

## verify
| 시안 | 상태 | 비고 |
|---|---|---|
| receipt_verify | ◐ | `VerifyScreen` RECEIPT 단계: 카메라 미리보기 + 촬영 버튼 + "영수증이 없어요" 버튼. **가이드 틀·실시간 필드 오버레이 없음(B-09)**, 갤러리 버튼 **의도적으로 없음**, 탭 전환 대신 하단 버튼 |
| photo_verify | ◐ | LOCATE(위치 측정)→PHOTO(촬영) 단계. 측정 중 최선 정확도 표시, 실패 횟수 표시. 시안의 실시간 간판 탐지 오버레이·일치도 표시 없음 |
| location_only_fallback | ◐ | 3회 실패+30m 안이면 PHOTO 단계에 "위치 확인 도장 받기" 버튼과 안내문. 시안의 시도 로그·거리 상세 없음 |
| verify_result | ◐ | 실패는 같은 화면에 오류 문구(코드별 `err_*`)와 다음 행동 버튼. 성공은 도장 연출(`ResultSection`). "테스트 가게로 모의 인증" 링크·오류 코드 "E-VER-402"는 **쓰지 않음** |
| receipt_duplicate | ✖ | **알림 자체가 없음(B-02)**. 시안의 해시 원문 표시는 금지 |
| daily_limit | ◐ | 인증 화면 진입 시 `E_DAILY_LIMIT`/`E_ALREADY_TODAY` 문구 + 돌아가기. 오늘 인증한 5곳 목록·인증 시각 없음 |

## stamp
| 시안 | 상태 | 비고 |
|---|---|---|
| stamp_ceremony | ◐ | 인증 결과 화면 안에서 연출(2.2→1.0 380ms, 숫자 600ms, 진동 40ms). 시안의 인증 전/후 세월 비교·게이지·"평점 남기기" 버튼은 없음(B-16) |
| title_promotion | ✖ | P1 (B-11). "東네 어른" 오타·특권 문구 쓰지 말 것 |

## passport
| 시안 | 상태 | 비고 |
|---|---|---|
| passport | ◐ | `PassportScreen`: 칭호 배지, 세월 게이지(눈금), 도장/내 리뷰 칩, 3열 격자(빈 칸 포함). 시안의 방문 노포·평균 업력·남긴 기록 통계, 타임라인, "500년 특전 잠금"은 없음 |
| passport_empty | ◐ | `EmptyState`(흐린 도장). 시안의 추천 노포 카드·"QR/GPS" 문구는 **쓰지 않음**(영수증은 위치 미사용) |
| stamp_detail | ◐ | 다이얼로그: 도장, 인증샷, 첫 방문일(날짜), 업력, 인증 방법, 단골 횟수. 해시·시각(시:분)·가짜 "분산 서버" 문구 제외, 내 평점 미표시 |
| my_reviews | ◐ | "내 리뷰" 칩 → 목록(가게명·별점·글). **수정/삭제 버튼 없음**(가게 상세로 이동해 수정). 통계 카드·정렬 없음 |
| chronicle_stats / hall_of_fame / gold_certificate | ⛔ | PRD 외 (`PRD-차이점.md`: 승급 기준 700년 오류, "블록 검증" 문구 등) |
| collections / share_card | ⛔ | 확장/P2 |

## profile · review
| 시안 | 상태 | 비고 |
|---|---|---|
| user_profile | ◐ | `ProfileScreen`: 닉네임·칭호 배지·세월/방문/리뷰 수, 다녀온 노포, 남긴 평점(한 화면 스크롤). 게이지·가입일·탭·등급·첫 방문일·단골 횟수·더보기 없음(B-08). 시안의 게이지 눈금 라벨 오류(새싹 100) 주의 |
| review_editor / review_edit / review_offline | ◐ | 별도 화면 없이 `AlertDialog`: 별점 입력(48dp), 글(500자·10줄), 저장/삭제, 오류(`E_REVIEW_OFFLINE`) 표시, 초안 `rememberSaveable`. 시안의 키워드 칩·사진 첨부·"사진 교체"는 **금지**. 오프라인 안내 상세(초안 스냅샷 시각 등)는 없음 |

## settings
| 시안 | 상태 | 비고 |
|---|---|---|
| settings | ✔ | `SettingsScreen`: 계정(닉네임·이메일), 앱 정보(버전, 카탈로그, 5탭 개발자 도구), 데이터 출처, 개인정보, 라이선스, 계정(닉네임 변경·탈퇴), 로그아웃. 시안의 "반경 50m" 오류 주의(30m) |
| account | ◐ | 닉네임 변경 다이얼로그(중복 확인). "30일 내 1회" 제한은 PRD에 없음(미적용). UID·저장 경로·용량 표시 제외 |
| delete_account | ◐ | 비밀번호 다이얼로그 + 경고문. 시안의 소멸 자산 요약·동의 체크는 없음 |
| logout_warning | ◐ | 대기 개수 경고 다이얼로그. 시안의 대기 기록 목록·"지금 동기화" 버튼 없음 |
| licenses | ◐ | 접이식 고정 텍스트. 시안의 "18종" 가짜 버전은 쓰지 말 것 |
| privacy_policy | ✖ | 설정에 요약 문장만. 시안의 가짜 사업자번호·주소·CISO·"AES-256"은 **절대 사용 금지**. 초안: `docs/privacy-notice.md` |
| privacy_settings | ✖ | P1 (B-12) |
| notifications | ⛔ | PRD 외 ("한양 도편수" 등 없는 칭호) |
| offline_queue | ◐ | 상단 오프라인 배너(`OfflineBanner`, 대기 개수 표시). 별도 화면 없음 |
| sync_restore | ✖ | 로그인 시 조용히 복원(`restoreFromServer`). 진행 화면 없음 |

## diagnostics
| 시안 | 상태 | 비고 |
|---|---|---|
| spike_console / system_diagnostics / test_store / ocr_inspector | ◐ | 진단 7탭(지도·카메라·글자 인식·위치·Firebase·사진·디자인) + 설정의 테스트 가게 버튼. 시안의 수치·"Tesseract"·"5/5 PASSED"는 가짜. 정보 복사·테스트 기록 삭제 없음(B-06·B-07) |
| apk_install_guide | → 문서 | `docs/apk-install-guide.md`로 작성(앱 화면 아님). 시안의 "안드로이드 10 이상"은 오류(minSdk 26 = Android 8.0), 가짜 SHA-256 지문은 실제 서명 키에서 계산할 것 |

## 요약
- **P0 화면은 모두 기능적으로 존재**하나 다수가 시안보다 단순(◐). 완전 ✖인 P0 흐름 관련 항목: receipt_duplicate 알림(B-02, 기능 결함), sync_restore 진행 표시, privacy_policy 화면.
- 시안 상세 재현(그래픽·통계·일러스트)은 **시연 품질을 위한 선택 작업**이며 기능 요구가 아니다. 우선순위는 `nopo-11-backlog.md`의 P0/P1 결함이 먼저.
- `design/stitch/reference/`(43개 폴더)는 검증되지 않은 "완료" 주장 문서라 구현 근거로 쓰지 않는다. 그중 `DESIGN.md`만 토큰·컴포넌트 규격 참고용으로 유효(코드와 일치 확인).

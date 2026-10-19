# Stitch 디자인 원본

Stitch 프로젝트 `3905731688785616669`에서 `tools/fetch_stitch.py`로 받은 원본. 직접 고치지 말고 다시 받는다.

> ⚠️ **기준은 PRD다. 화면을 만들기 전에 [`PRD-차이점.md`](PRD-차이점.md)에서 해당 화면을 먼저 확인한다.**

- 색·글꼴·간격의 기준은 `reference/*DESIGN.md*`와 PRD 10.1이다. 화면 HTML 안의 Tailwind 색(Stitch 자동 생성 M3 팔레트)은 레이아웃 참고용이며 토큰으로 옮기지 않는다.
- 각 폴더: `screen.png`(스크린샷), `code.html`(Stitch HTML), `meta.json`.
- 우선순위는 PRD 4장·5장 기준. `확장`은 기획안의 확장 계획, `PRD 외`·`확인 필요`는 Stitch에서 추가로 그린 화면이라 PRD·스토리에 없거나 범위가 불분명하다. 구현 전에 범위를 정한다.

## 앱 화면

| 기능 패키지 | 화면 | 제목 | 스토리 | 우선순위 | 폴더 |
|---|---|---|---|---|---|
| `feature/auth` | `login` | 로그인 및 패스포트 복원 | S-404 | P0 | [screens/auth/login](screens/auth/login/screen.png) |
| `feature/auth` | `nickname_setup` | 식객 칭호 설정 및 호적부 복구 | S-406 | P0 | [screens/auth/nickname_setup](screens/auth/nickname_setup/screen.png) |
| `feature/auth` | `onboarding` | 온보딩 및 미식 여정 소개 | S-402 | P0 | [screens/auth/onboarding](screens/auth/onboarding/screen.png) |
| `feature/auth` | `reset_password` | 비밀번호 재설정 및 복구 안내 | S-405 | P0 | [screens/auth/reset_password](screens/auth/reset_password/screen.png) |
| `feature/auth` | `sign_up` | 회원가입 및 미식가 등록 | S-403 | P0 | [screens/auth/sign_up](screens/auth/sign_up/screen.png) |
| `feature/diagnostics` | `apk_install_guide` | 릴리스 서명 APK 설치 및 보안 안내 | S-1006 | 참고 | [screens/diagnostics/apk_install_guide](screens/diagnostics/apk_install_guide/screen.png) |
| `feature/diagnostics` | `ocr_inspector` | 테스트 영수증 검증기 및 OCR 인스펙터 | S-004 | P0 | [screens/diagnostics/ocr_inspector](screens/diagnostics/ocr_inspector/screen.png) |
| `feature/diagnostics` | `spike_console` | 1일차 스파이크 종합 진단 콘솔 | S-001 | P0 | [screens/diagnostics/spike_console](screens/diagnostics/spike_console/screen.png) |
| `feature/diagnostics` | `system_diagnostics` | 시스템 진단 및 테스트 가게 관리 | S-901 | P0 | [screens/diagnostics/system_diagnostics](screens/diagnostics/system_diagnostics/screen.png) |
| `feature/diagnostics` | `test_store` | 테스트 가게 생성 및 실기기 시뮬레이터 | S-902 | P0 | [screens/diagnostics/test_store](screens/diagnostics/test_store/screen.png) |
| `feature/map` | `location_permission` | 위치 권한 안내 및 대략적 위치 경고 | S-502 | P0 | [screens/map/location_permission](screens/map/location_permission/screen.png) |
| `feature/map` | `map` | 지도 탐색 및 노포 발견 | S-502 | P0 | [screens/map/map](screens/map/map/screen.png) |
| `feature/map` | `map_fallback_list` | 지도 오류 대체 목록 및 오프라인 탐색 | S-504 | P0 | [screens/map/map_fallback_list](screens/map/map_fallback_list/screen.png) |
| `feature/map` | `map_filter` | 노포 지도 필터 및 정렬 상세 설정 | S-502 | P0 | [screens/map/map_filter](screens/map/map_filter/screen.png) |
| `feature/map` | `store_list` | 노포 검색 및 목록 보기 | S-504 | P0 | [screens/map/store_list](screens/map/store_list/screen.png) |
| `feature/passport` | `chronicle_stats` | 식객 미식 여정 통계 및 연대기 결산 | - | PRD 외 | [screens/passport/chronicle_stats](screens/passport/chronicle_stats/screen.png) |
| `feature/passport` | `collections` | 지역 컬렉션 및 테마 미식 퀘스트 | - | 확장 | [screens/passport/collections](screens/passport/collections/screen.png) |
| `feature/passport` | `gold_certificate` | 100년 노포 황금 도장 상세 및 인증서 | S-703 | 확인 필요 | [screens/passport/gold_certificate](screens/passport/gold_certificate/screen.png) |
| `feature/passport` | `hall_of_fame` | 식객 명예의 전당 및 칭호 순위 | - | PRD 외 | [screens/passport/hall_of_fame](screens/passport/hall_of_fame/screen.png) |
| `feature/passport` | `my_reviews` | 내 패스포트 - 내 작성 리뷰 목록 | S-704 | P0 | [screens/passport/my_reviews](screens/passport/my_reviews/screen.png) |
| `feature/passport` | `passport` | 내 패스포트 및 미식 연대기 | S-702 | P0 | [screens/passport/passport](screens/passport/passport/screen.png) |
| `feature/passport` | `passport_empty` | 내 패스포트 (도장 0개 첫 시작) | S-702 | P0 | [screens/passport/passport_empty](screens/passport/passport_empty/screen.png) |
| `feature/passport` | `share_card` | 미식 패스포트 공유 카드 생성기 | - | P2 | [screens/passport/share_card](screens/passport/share_card/screen.png) |
| `feature/passport` | `stamp_detail` | 도장 상세 및 방문 각인 기록 | S-703 | P0 | [screens/passport/stamp_detail](screens/passport/stamp_detail/screen.png) |
| `feature/profile` | `user_profile` | 타 유저 미식가 공인 프로필 | S-804 | P0 | [screens/profile/user_profile](screens/profile/user_profile/screen.png) |
| `feature/review` | `review_edit` | 공인 리뷰 수정 및 삭제 에디터 | S-802 | P0 | [screens/review/review_edit](screens/review/review_edit/screen.png) |
| `feature/review` | `review_editor` | 공인 미식 평점 및 리뷰 작성 | S-802 | P0 | [screens/review/review_editor](screens/review/review_editor/screen.png) |
| `feature/review` | `review_offline` | 오프라인 리뷰 작성 및 초안 보존 안내 | S-802 | P0 | [screens/review/review_offline](screens/review/review_offline/screen.png) |
| `feature/settings` | `account` | 식객 닉네임 변경 및 계정 관리 | S-408 | P1 | [screens/settings/account](screens/settings/account/screen.png) |
| `feature/settings` | `delete_account` | 회원 탈퇴 및 호적부 영구 말소 | S-408 | P1 | [screens/settings/delete_account](screens/settings/delete_account/screen.png) |
| `feature/settings` | `licenses` | 오픈소스 라이선스 고지 | S-903 | P0 | [screens/settings/licenses](screens/settings/licenses/screen.png) |
| `feature/settings` | `logout_warning` | 식객 로그아웃 및 미전송 대기열 경고 | S-407 | P0 | [screens/settings/logout_warning](screens/settings/logout_warning/screen.png) |
| `feature/settings` | `notifications` | 식객 알림 및 세월 동기화 센터 | - | PRD 외 | [screens/settings/notifications](screens/settings/notifications/screen.png) |
| `feature/settings` | `offline_queue` | 오프라인 모드 및 대기열 동기화 | S-609 | P0 | [screens/settings/offline_queue](screens/settings/offline_queue/screen.png) |
| `feature/settings` | `privacy_policy` | 개인정보 및 위치정보 처리방침 고지 | S-903 | P0 | [screens/settings/privacy_policy](screens/settings/privacy_policy/screen.png) |
| `feature/settings` | `privacy_settings` | 방문 기록 비공개 및 프라이버시 설정 | - | P1 | [screens/settings/privacy_settings](screens/settings/privacy_settings/screen.png) |
| `feature/settings` | `settings` | 설정 및 데이터 출처 안내 | S-903 | P0 | [screens/settings/settings](screens/settings/settings/screen.png) |
| `feature/settings` | `sync_restore` | 서버 방문 기록 복원 및 동기화 | S-307 | P0 | [screens/settings/sync_restore](screens/settings/sync_restore/screen.png) |
| `feature/stamp` | `stamp_ceremony` | 도장 획득 및 세월 적립 연출 | S-701 | P0 | [screens/stamp/stamp_ceremony](screens/stamp/stamp_ceremony/screen.png) |
| `feature/stamp` | `title_promotion` | 칭호 승급 축하 연출 | S-705 | P1 | [screens/stamp/title_promotion](screens/stamp/title_promotion/screen.png) |
| `feature/store` | `closed_memorial` | 사라진 노포 추모관 및 맛의 기억 | - | 확장 | [screens/store/closed_memorial](screens/store/closed_memorial/screen.png) |
| `feature/store` | `directions` | 노포 길찾기 및 대중교통 안내 | S-505 | P0 | [screens/store/directions](screens/store/directions/screen.png) |
| `feature/store` | `report` | 노포 정보 수정 제보 및 리뷰 신고 | S-805 | P1 | [screens/store/report](screens/store/report/screen.png) |
| `feature/store` | `store_detail` | 노포 상세 및 인증 자격 | S-505 | P0 | [screens/store/store_detail](screens/store/store_detail/screen.png) |
| `feature/store` | `store_reviews` | 노포 공인 미식가 리뷰 목록 | S-506 | P0 | [screens/store/store_reviews](screens/store/store_reviews/screen.png) |
| `feature/verify` | `daily_limit` | 일일 인증 한도 및 단골 쿨타임 안내 | S-608 | P0 | [screens/verify/daily_limit](screens/verify/daily_limit/screen.png) |
| `feature/verify` | `location_only_fallback` | 위치 확인 도장 구제 및 대체 발급 | S-607 | P0 | [screens/verify/location_only_fallback](screens/verify/location_only_fallback/screen.png) |
| `feature/verify` | `photo_verify` | 가게 사진 및 위치 인증 | S-607 | P0 | [screens/verify/photo_verify](screens/verify/photo_verify/screen.png) |
| `feature/verify` | `receipt_duplicate` | 영수증 중복 해시 반려 안내 | S-608 | P0 | [screens/verify/receipt_duplicate](screens/verify/receipt_duplicate/screen.png) |
| `feature/verify` | `receipt_verify` | 영수증 방문 인증 화면 | S-604 | P0 | [screens/verify/receipt_verify](screens/verify/receipt_verify/screen.png) |
| `feature/verify` | `verify_result` | 방문 인증 결과 및 대체 인증 안내 | S-608 | P0 | [screens/verify/verify_result](screens/verify/verify_result/screen.png) |

## 참고 문서 (데스크톱 가이드·문서 사본)

> ⚠️ 검증된 적 없는 '완료·통과' 주장과 존재하지 않는 파일을 전제로 한 문서들이다. 스크립트를 실행하지 않는다. 자세한 내용은 `PRD-차이점.md` 6장.
> `76404327_노포천년미식가_기획안_v0.4.html`은 구버전이다 (최신: `docs/nopo-planning.html`).

- [Android Jetpack Compose 전체 51개 Composable 템플릿 코드 (v0.4)](reference/f4914400_Android Jetpack Compose 전체 51개 Composable 템플릿 코드 (v0.4))
- [Android Jetpack Compose 코드 구조 및 Composable 매핑 가이드](reference/3e7f29a0_Android Jetpack Compose 코드 구조 및 Composable 매핑 가이드)
- [Android Studio CI/CD 파이프라인(GitHub Actions) 빌드 검증 가이드 (v0.4)](reference/92c86cf8_Android Studio CI_CD 파이프라인(GitHub Actions) 빌드 검증 가이드 (v0.4))
- [CLAUDE.md](reference/76404327_CLAUDE.md)
- [DESIGN.md](reference/0ebd423c_DESIGN.md)
- [Figma Variables 토큰 JSON (v0.4)](reference/49255e75_Figma Variables 토큰 JSON (v0.4))
- [Figma 라이브러리 파일 구조 및 아키텍처 가이드 (v0.4)](reference/1405a7d1_Figma 라이브러리 파일 구조 및 아키텍처 가이드 (v0.4))
- [Figma 및 Compose 코드 내보내기 가이드 (v0.4)](reference/08fef1a3_Figma 및 Compose 코드 내보내기 가이드 (v0.4))
- [Figma 브랜치 병합 및 머지 절차 가이드 (v0.4)](reference/c61797b9_Figma 브랜치 병합 및 머지 절차 가이드 (v0.4))
- [Figma 최신 토큰 & 컴포넌트 동기화 가이드 (v0.4.1)](reference/4c95fd97_Figma 최신 토큰 & 컴포넌트 동기화 가이드 (v0.4.1))
- [Figma 컴포넌트 Variants 세트 구성 가이드 (v0.4)](reference/42c781bc_Figma 컴포넌트 Variants 세트 구성 가이드 (v0.4))
- [Figma 컴포넌트 라이브러리 Publish & 버전 릴리스 절차 가이드 (v0.4)](reference/8f496f73_Figma 컴포넌트 라이브러리 Publish & 버전 릴리스 절차 가이드 (v0.4))
- [Figma 컴포넌트 라이브러리 종합 검수 보고서 (v0.4)](reference/6ece680c_Figma 컴포넌트 라이브러리 종합 검수 보고서 (v0.4))
- [Figma 토큰 ➔ Compose 자동 동기화 Gradle 태스크 가이드 (v0.4)](reference/6595898b_Figma 토큰 ➔ Compose 자동 동기화 Gradle 태스크 가이드 (v0.4))
- [Firestore 보안 규칙 단위 테스트 및 플레이그라운드 점검 가이드](reference/675fadb7_Firestore 보안 규칙 단위 테스트 및 플레이그라운드 점검 가이드)
- [START-HERE.md](reference/76404327_START-HERE.md)
- [Three.js](reference/056a9848_Three.js)
- [nopo-01-PRD.md](reference/76404327_nopo-01-PRD.md)
- [nopo-02-architecture.md](reference/76404327_nopo-02-architecture.md)
- [nopo-03-stories.md](reference/76404327_nopo-03-stories.md)
- [노포 천년 미식가 — html.to.design 전체 51개 화면 피그마 일괄 임포트 마스터](reference/83a99e07_노포 천년 미식가 — html.to.design 전체 51개 화면 피그마 일괄 임포트 마스터)
- [노포 천년 미식가 — 전 산출물 정밀 디버깅 및 오류 프로세스 검증 보고서 (v0.4.1)](reference/38e3f292_노포 천년 미식가 — 전 산출물 정밀 디버깅 및 오류 프로세스 검증 보고서 (v0.4.1))
- [노포 천년 미식가 — 전 산출물 최종 단일 무결점 ZIP 아카이브 원클릭 다운로드 마스터 패키지 (v0.4.1 SSOT Release)](reference/e97d4167_노포 천년 미식가 — 전 산출물 최종 단일 무결점 ZIP 아카이브 원클릭 다운로드 마스터 패키지 (v0.4.)
- [노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (v0.4.1)](reference/58ddcd05_노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (v0.4.1))
- [노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (무결점 다운로드 보장)](reference/b8fb357f_노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (무결점 다운로드 보장))
- [노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (무결점 실시간 생성)](reference/8b3f9a2c_노포 천년 미식가 — 전 산출물 통합 원클릭 ZIP 다운로더 (무결점 실시간 생성))
- [노포 천년 미식가 — 전 산출물(UI화면·에셋·문서·코드·데이터) 올인원 통합 다운로드 마스터 패키지 (v0.4.1)](reference/c2bd1e76_노포 천년 미식가 — 전 산출물(UI화면·에셋·문서·코드·데이터) 올인원 통합 다운로드 마스터 패키지 (v0)
- [노포 천년 미식가 — 전체 51개 화면 목록 및 유저 플로우 최종 종합 정리 보고서 (v0.4)](reference/4d220d6f_노포 천년 미식가 — 전체 51개 화면 목록 및 유저 플로우 최종 종합 정리 보고서 (v0.4))
- [노포 천년 미식가 — 최종 버전(v0.4.1) 단일 통합 다운로드 마스터 패키지](reference/2f302139_노포 천년 미식가 — 최종 버전(v0.4.1) 단일 통합 다운로드 마스터 패키지)
- [노포 천년 미식가 — 최종 산출물 종합 검수 보고서 및 통합 다운로드 마스터 패키지 (v0.4)](reference/d815f76e_노포 천년 미식가 — 최종 산출물 종합 검수 보고서 및 통합 다운로드 마스터 패키지 (v0.4))
- [노포 천년 미식가 — 최종 통합 마스터 아카이브 & 원클릭 내보내기 패키지 (v0.4)](reference/ca0cd91c_노포 천년 미식가 — 최종 통합 마스터 아카이브 & 원클릭 내보내기 패키지 (v0.4))
- [노포 천년 미식가 — 피그마(Figma) 원클릭 전체 내보내기 마스터 패키지 (v0.4.1)](reference/5908ca4d_노포 천년 미식가 — 피그마(Figma) 원클릭 전체 내보내기 마스터 패키지 (v0.4.1))
- [노포천년미식가_기획안_v0.4.html](reference/76404327_노포천년미식가_기획안_v0.4.html)
- [안드로이드 스튜디오 프로젝트 루트 즉시 자동 복사 & 파일 주입 완벽 가이드](reference/ddecbf63_안드로이드 스튜디오 프로젝트 루트 즉시 자동 복사 & 파일 주입 완벽 가이드)
- [안드로이드 실기기 설치용 최신 릴리스 APK 빌드 & 배포 가이드 (v0.4)](reference/0b924271_안드로이드 실기기 설치용 최신 릴리스 APK 빌드 & 배포 가이드 (v0.4))
- [전체 산출물 ZIP/아카이브 내보내기 및 빌드 배포 가이드 (v0.4)](reference/c99a2ba5_전체 산출물 ZIP_아카이브 내보내기 및 빌드 배포 가이드 (v0.4))
- [최신 토큰(v0.4.1) 반영 릴리스 APK 재빌드 & 배포 가이드](reference/8c88b5fe_최신 토큰(v0.4.1) 반영 릴리스 APK 재빌드 & 배포 가이드)
- [카탈로그 1,277곳 데이터 최종 정합성 및 무결성 전수 감사 보고서 (v0.4.1)](reference/23c3f5d6_카탈로그 1,277곳 데이터 최종 정합성 및 무결성 전수 감사 보고서 (v0.4.1))
- [피그마 변환용 화면별 HTML/SVG 에셋 추출 가이드 & 마스터 패키지 (v0.4)](reference/96e1b322_피그마 변환용 화면별 HTML_SVG 에셋 추출 가이드 & 마스터 패키지 (v0.4))
- [화면 간 세부 트랜지션 애니메이션 가이드](reference/c67ef386_화면 간 세부 트랜지션 애니메이션 가이드)
- [화면 간 트랜지션 시연기 - 사진 인증 및 칭호 승급 추가](reference/8ad41371_화면 간 트랜지션 시연기 - 사진 인증 및 칭호 승급 추가)
- [화면 간 트랜지션 시연기 - 사진 인증 및 칭호 승급 추가](reference/a1e8c44e_화면 간 트랜지션 시연기 - 사진 인증 및 칭호 승급 추가)
- [화면 간 트랜지션 시연용 프로토타입 인터랙션](reference/278364ba_화면 간 트랜지션 시연용 프로토타입 인터랙션)

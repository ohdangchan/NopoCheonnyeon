# 노포 천년 미식가 — 직접 확인할 것 체크리스트 v0.4

개발 전에, 또는 개발 중에 **직접** 확인하거나 정해야 하는 항목입니다. 위에서부터 처리하세요.

## A. 꼭 먼저 (모르면 1일차가 멈춤)
- [ ] **Claude 요금제 확인**: Claude Code는 Pro·Max·Team·Enterprise 요금제 또는 Console(API) 계정이 필요합니다. 무료 요금제로는 쓸 수 없습니다
- [ ] Claude Code 설치 후 `claude --version` 확인, 첫 실행 시 로그인
- [ ] Android Studio 최신 안정판 설치, JetBrains Marketplace에서 “Claude Code [Beta]” 플러그인 설치 후 재시작
- [ ] Windows라면 Git for Windows 설치 (Claude Code가 Git Bash로 `./gradlew`를 실행)
- [ ] 테스트 폰 2대 (Android 12 이상 1대 필수, 구형·저사양 1대), 개발자 옵션·USB 디버깅, `adb devices` 확인
- [ ] 패키지명 확정: `com.nopo.cheonnyeon` (NCP·Firebase 등록 후 바꾸기 어려움)

## B. 계정과 키 (1일차 09:00 전)
- [ ] 네이버 클라우드 플랫폼 가입 → Maps → 애플리케이션 등록: **Dynamic Map**과 **Geocoding** 선택, Android 패키지명 입력 → Key ID·Key 확보
- [ ] 네이버 Maps 무료 이용량과 결제수단 등록 필요 여부를 요금 페이지에서 확인 (대표 계정만 무료 이용량 제공)
- [ ] (좌표 보강을 카카오로 할 경우) 카카오 디벨로퍼스 앱 등록 → REST API 키
- [ ] Firebase 프로젝트 생성, Android 앱 등록, `google-services.json` 받기 (`nopo-07`)
- [ ] Firebase Authentication에서 이메일/비밀번호 켜기, Firestore 서울 리전 생성, 규칙·색인 적용
- [ ] 키는 `local.properties`에만 저장 (`NAVER_MAP_KEY_ID=...`), 저장소에 올리지 않기
- [ ] 릴리스 서명 키(.jks) 만들기, 비밀번호 기록, **백업 2곳**

## C. 데이터
- [ ] 이 패키지의 `data/nopo_stores.csv`, `app/src/main/assets/stores.json` 확인 (가게 수·창업연도 채움 비율은 `data/README.md`)
- [ ] 상세 정보가 비어 있는 가게가 있으면 컴퓨터에서 `pip install requests beautifulsoup4` 후 `python tools/crawl_baeknyeon.py --only-missing` 실행으로 보충 (요청 간격 1초, robots.txt 준수, 결과는 `data/raw/`에 쌓이고 카탈로그 생성 때 자동으로 합쳐짐)
- [ ] `python tools/geocode.py --provider naver` (또는 `kakao`)로 좌표 채우기 → `python tools/build_catalog.py` 다시 실행. 주소가 없는 오래가게는 `--provider kakao`만 이름으로 찾을 수 있음(네이버로 먼저 돌린 뒤 카카오로 한 번 더 돌리면 빈 것만 채움). `COORD_FROM_NAME` 표시 가게는 지도에서 위치 확인
- [ ] (권장) 공식 지정리스트 CSV(https://www.data.go.kr/data/15132695/fileData.do, 1,511행)를 내려받아 `python tools/import_official_list.py --csv 파일.csv` → `python tools/build_catalog.py` (소상공인마당에 소개 페이지가 없는 백년가게 보충)
- [ ] 자주 테스트할 가게 5곳은 지도에서 좌표를 눈으로 확인
- [ ] 소개글은 수집 도구가 요약한 문장입니다. 출시 전에는 가게별로 직접 다시 쓰거나 출처를 표시

## D. 1일차 기능 검증 재료
- [ ] 실제 영수증 1장 이상(카드번호 가림), 한글 간판 사진·인쇄물
- [ ] 테스트 영수증: `tools/test-images/test_receipt_live.html`을 PC 브라우저로 열면 **현재 시각**이 찍힌 영수증이 나옴 → 화면을 폰으로 촬영. `test_receipt.png`는 날짜가 2026-10-17로 고정이라 24시간이 지나면 ‘기간 지남(E_RECEIPT_EXPIRED)’ 확인용으로 씀. 다시 시험할 때는 1분 뒤 새로고침(같은 분이면 중복 영수증)
- [ ] 테스트 간판: `tools/test-images/test_sign.png`를 다른 화면에 띄우거나 출력해 두기
- [ ] 근처 노포 1곳 (위치 정확도 현장 측정용)

## E. 법·개인정보 (APK 테스트 단계 기준)
- [ ] 테스트 참가자에게 수집 항목 안내: 이메일, 닉네임, 방문 기록(가게·날짜), 리뷰. 위치 좌표와 영수증 이미지는 서버에 저장하지 않음
- [ ] 위치 좌표를 서버에 저장하지 않는 설계 유지 확인 (저장하게 되면 위치정보 관련 신고·동의 절차를 출시 전에 검토)
- [ ] “백년가게”, “오래가게”는 공식 인증처럼 오해되지 않도록 출처 표기, 앱이 기관과 무관함을 설정 화면에 표기
- [ ] 오픈소스 라이선스 화면 (글꼴 OFL, 라이브러리 Apache 2.0 등)

## F. APK 배포 (스토어 출시는 다음 버전)
- [ ] 릴리스 APK 설치 방법 안내문: 파일 받기 → “출처를 알 수 없는 앱 설치” 허용 → 설치 (Play 프로텍트 경고가 뜰 수 있음)
- [ ] 테스트 참가자 기기 목록과 Android 버전 기록
- [ ] Crashlytics에서 테스트 기기 크래시가 보이는지 확인
- [ ] 업데이트 APK는 같은 서명 키로 (디버그 키 APK → 정식 키 APK로 바꿀 때는 지우고 재설치)
- [ ] Android 개발자 인증: 2026-09-30 인도네시아·싱가포르·태국부터, 2027년 전 세계 확대 예정. 한국 적용 시점을 확인하고, 필요하면 무료 제한 배포 계정(기기 20대) 등록. ADB 설치는 계속 허용

## G. 심사·발표
- [ ] 평가 기준표에서 UX/UI 가산점 항목 확인
- [ ] 테스트 가게로 인터넷 없이도 시연 가능한지 3회 연습, 백업 영상 녹화
- [ ] 발표에 실제 측정값 넣기: 영수증 인식률, 노포 앞 위치 정확도, Monkey 결과, 크래시 0
- [ ] 예상 질문 대비: 부정 인증 방지, 데이터 출처와 정확성, 개인정보, 가게에 주는 이득

## H. 개발 습관
- [ ] 스토리 하나 = 커밋 하나 (`S-xxx: 요약`)
- [ ] Claude Code에는 한 번에 스토리 하나만
- [ ] 변경 내용은 IDE 비교 화면에서 확인 후 승인
- [ ] 매일 저녁 릴리스 APK로 한 바퀴 (R-4)

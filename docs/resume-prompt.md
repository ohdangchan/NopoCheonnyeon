# 새 Claude(다른 계정·다른 창)에게 이어받게 하는 방법

## 1. 같은 PC에서 계정만 바꿔 로그인했을 때
대화 원문은 계정이 아니라 이 PC의 사용자 폴더에 있다: `C:\Users\HB_OPERATION_NICK\.claude\projects\C--Users-HB-OPERATION-NICK-AndroidStudioProjects-NopoCheonnyeon\` (대화 `.jsonl` + `memory\`).
1. 프로젝트 폴더에서 Claude를 열고 `/resume`(또는 터미널 `claude --resume 60beb805-8679-4463-8f77-bc74857997cd`)으로 이 대화를 고른다. 목록에 없으면 아래 2번.
2. 대화가 안 보이거나 이어받을 수 없으면 새 대화에서 아래 "붙여 넣을 말"을 그대로 쓴다. 기억 파일(`memory\`)은 같은 폴더에 있어 새 Claude가 자동으로 읽는다.

## 2. 붙여 넣을 말(새 대화 첫 메시지)
> 이 프로젝트(노포 천년 미식가, 안드로이드 앱)를 이어서 개발해 줘. 먼저 `git switch redesign/v1` 후, `CLAUDE.md` → `docs/handoff-conversation-log.md`(1-4장까지) → `docs/dev-status.md` 맨 위 → `docs/rollback.md` 순서로 읽고, 내가 지금까지 내린 결정과 약속을 요약해서 확인시켜 줘. 모든 답은 한국어로. 비밀(키·비밀번호·`keystore.properties`)은 읽거나 쓰지 마. `master`와 `baseline/v0.2.0`은 건드리지 마. 10분 자동 저장은 꺼져 있으니 큰 단계마다 직접 커밋·푸시해 줘. 다음 일은 새 UX 시안(5탭, 크림 종이 배경, 여권·스탬프) 적용이고, 앱 이름과 시작 순서는 내가 정해 줄게.

## 3. 반드시 알고 시작할 것(요약)
- 기준선: 태그 `v0.2.0`(GitHub 릴리스 APK, QR 주소 그대로). 새 개발은 `redesign/v1`에서만.
- 사용자 결정 대기: 기존 Gemini 키 폐기, 시연 뒤 App Check 다시 켜기, 11월 2일 이후 AI 호출 방식, 앱 이름("노포 패스포트"?), 새 디자인 시작 순서.
- 하지 않는 일: 약관을 어기는 크롤링, 비밀 노출, 사용자 허락 없는 공개·삭제·결제.
- 이 PC에서 빌드: `docs/handoff-conversation-log.md` 6장(임시 폴더 `C:\gt`, JDK `D:\android\android_studio\jbr`, 램이 작아 `--no-daemon --max-workers=1`).

## 4. 다른 PC로 옮길 때
`.jsonl` 두 개와 `memory\` 폴더를 같은 경로 구조(`.claude\projects\<프로젝트 폴더를 뜻하는 이름>\`)로 복사한다. 서명 키(`keystore.properties`, `*.jks`)와 `app/google-services.json`은 저장소에 없으니 안전하게 따로 옮긴다. 저장소 푸시 권한(협업자 추가)도 필요하다.

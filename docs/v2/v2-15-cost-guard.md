# 과금 악용 방어 (2026-10-28)

## 위험
- 돈이 드는 곳은 Gemini(Firebase AI Logic)뿐. 키는 앱에 없지만 Firebase 설정값은 APK에 들어 있어, 앱이 아닌 도구도 같은 경로로 호출해 프로젝트에 과금시킬 수 있다.
- Firestore는 규칙으로 본인 문서만 쓰게 막혀 있다. 이메일 가입만 하면 계정을 만들 수 있어 소액 읽기·쓰기 과금 가능성은 남는다.

## 코드에서 한 것
- `AiQuota`: 이 기기에서 하루 60회까지만 Gemini 호출. 넘으면 서버로 보내지 않고 `E_AI_LIMIT`("오늘 AI 기능을 너무 많이 썼어요…"). 정상 이용자의 과다 호출·버그 폭주를 막는다. 앱 데이터를 지우면 초기화되므로 악의적 호출은 막지 못한다.
- App Check는 넣지 않았다: 이 앱은 Play 스토어가 아니라 APK 직접 배포라 Play Integrity가 통과하지 않아, 강제하면 모든 AI 기능이 막힌다. Play 내부 테스트/출시로 옮길 때 추가한다.

## 사용자가 해야 하는 일 (콘솔)
1. Google Cloud 콘솔 → 결제 → 예산 및 알림: 월 예산(예: 1만 원)과 50/90/100% 알림 이메일 설정.
2. Google Cloud 콘솔 → API 및 서비스 → Gemini API(Generative Language API) → 할당량: 분당·일일 요청 상한을 낮게(예: 일 1,000회) 설정. 이것이 악의적 호출에 대한 실제 상한이다.
3. Firebase 콘솔 → Authentication → 설정: 이메일 열거 보호 켜기, 같은 IP의 가입 제한 확인.
4. Firebase 콘솔 → Firestore → 규칙: `firebase/firestore.rules`가 배포돼 있는지 확인(`firebase deploy --only firestore:rules`).
5. (선택) Firebase 콘솔 → 사용량 및 결제에서 Blaze 요금제 예산 한도 확인.

## 콘솔 확인 결과 (2026-10-28)
- Firebase 요금제는 **Spark(무료)**이고 Google Cloud 프로젝트 `nopocheonnyeon`에 **결제 계정이 연결돼 있지 않다**(예산 화면의 결제 계정 목록이 비어 있음). 결제 수단이 없으므로 어떤 호출이 폭주해도 청구되지 않고, 무료 한도를 넘으면 요청이 거부될 뿐이다. 위 1·2·5번(예산·할당량·결제)은 **지금은 필요 없다.**
- Firestore 규칙: 콘솔 배포본이 `firebase/firestore.rules`와 같음(10-21 게시).
- Authentication 가입 할당량: 시간당 100(기본).
- **Blaze로 올릴 때(Play 출시 등)는 먼저 예산 알림·Gemini 할당량 상한·App Check를 설정할 것.** 그 전에는 올리지 않는다.
- 남는 위험: 무료 한도가 악의적 호출로 소진되어 정상 이용자의 AI가 잠시 막힐 수 있음(과금은 없음).

# 노포 천년 미식가 — Firebase Firestore 보안 규칙 단위 테스트 & 규칙 플레이그라운드 시나리오 점검서 (v0.4)

본 문서는 PRD 8.2, 아키텍처 6.3, 스토리 S-308(I-1~I-10) 및 QA 체크리스트(S-1~S-10)에 명시된 **Cloud Firestore 보안 규칙 (`firebase/firestore.rules`)**을 Firebase 콘솔의 **규칙 플레이그라운드(Rules Playground)** 및 **Firebase Local Emulator Suite (@firebase/rules-unit-testing)**에서 100% 검증하기 위한 단위 테스트 케이스 시나리오와 실행 명세입니다.

---

## 1. 보안 규칙 검증 핵심 매트릭스 (I-1 ~ I-10 및 확장 시나리오)

| TC ID | 대상 컬렉션 / 경로 | 작업 (Method) | 인증 상태 (Auth Context) | 요청 데이터 / 조건 | 기대 결과 (Expected) | 보안 검증 목적 (PRD/아키텍처 근거) |
|---|---|---|---|---|---|---|
| **TC-01** | `/users/{uid}` | `create` | `uid: "user_123"` 본인 | 초기값(`totalYears: 0`, `visitCount: 0`, `reviewCount: 0`, 허용된 필드만) | **ALLOW (200 OK)** | 정상 회원가입 시 본인 문서 초기화 허용 |
| **TC-02** | `/users/{uid}` | `create` | `uid: "user_123"` 본인 | 가짜 필드 주입 (`isAdmin: true`, `hackedPoints: 9999`) | **DENY (PERMISSION_DENIED)** | `keys().hasOnly(userKeys())` 화이트리스트 검증 |
| **TC-03** | `/users/{uid}` | `create` | `uid: "user_123"` 본인 | 시작 세월 조작 (`totalYears: 1000`, `visitCount: 5`) | **DENY (PERMISSION_DENIED)** | 가입 즉시 천년 미식가 승급 치팅 원천 차단 |
| **TC-04** | `/users/{uid}` | `update` | `uid: "user_123"` 본인 | 세월 +62년, 방문수 +1 정상 증가 (62 ≤ 200) | **ALLOW (200 OK)** | 방문 인증 후 점진적 세월 적립 허용 |
| **TC-05** | `/users/{uid}` | `update` | `uid: "user_123"` 본인 | 세월 +500년 급증 (`totalYears > prev + 200`) | **DENY (PERMISSION_DENIED)** | 단일 인증 200년 상한선 규칙 강제 |
| **TC-06** | `/users/{uid}` | `update` | `uid: "attacker"` 타인 | `user_123`의 문서 수정 시도 | **DENY (PERMISSION_DENIED)** | 타인 프로필 데이터 변조 방지 (`isMe(uid)`) |
| **TC-07** | `/users/{uid}/visits/{storeId}` | `create` | `uid: "user_123"` 본인 | `yearsAwarded: 62`, `method: "RECEIPT"`, `visitCount: 1` | **ALLOW (200 OK)** | 정상 첫 방문 각인 기록 생성 |
| **TC-08** | `/users/{uid}/visits/{storeId}` | `create` | `uid: "user_123"` 본인 | `yearsAwarded: 15` (30년 미만) | **DENY (PERMISSION_DENIED)** | 노포 공식 최소 업력 30년 바닥값 규칙 검증 |
| **TC-09** | `/users/{uid}/visits/{storeId}` | `update` | `uid: "user_123"` 본인 | 재방문 시 `visitCount` 1 증가, `yearsAwarded` 불변 | **ALLOW (200 OK)** | 재방문 단골 횟수 누적 정상 처리 |
| **TC-10** | `/users/{uid}/visits/{storeId}` | `update` | `uid: "user_123"` 본인 | 재방문 시 `yearsAwarded` 수정 시도 | **DENY (PERMISSION_DENIED)** | 최초 획득 세월 불변(Immutable) 원칙 강제 |
| **TC-11** | `/reviews/{storeId}_{uid}` | `create` | `uid: "user_123"` 본인 | 방문 문서 `/users/user_123/visits/{storeId}` **존재함** | **ALLOW (200 OK)** | 실제 방문 인증 완료 유저의 공인 리뷰 작성 허용 |
| **TC-12** | `/reviews/{storeId}_{uid}` | `create` | `uid: "user_123"` 본인 | 방문 문서 **존재하지 않음 (미방문)** | **DENY (PERMISSION_DENIED)** | 미방문 허위 리뷰 작성 차단 (`exists()`) |
| **TC-13** | `/reviews/{storeId}_{uid}` | `create` | `uid: "user_123"` 본인 | 별점 0점 또는 6점 (`rating: 6`) | **DENY (PERMISSION_DENIED)** | 별점 1~5 정수 범위 제약 검증 |
| **TC-14** | `/reviews/{storeId}_{uid}` | `create` | `uid: "user_123"` 본인 | 문서 ID 불일치 (`reviews/otherStoreId_user123`) | **DENY (PERMISSION_DENIED)** | 1인 1가게 1리뷰 문서 ID 구조 제약 |
| **TC-15** | `/receiptHashes/{hash}` | `create` | `uid: "user_123"` 본인 | 기존에 존재하지 않는 신규 해시 | **ALLOW (200 OK)** | 영수증 최초 1회 인증 해시 원타임 등록 |
| **TC-16** | `/receiptHashes/{hash}` | `create` / `update` | `uid: "user_456"` 타인 | **이미 존재하는 해시** 문서 덮어쓰기/수정 | **DENY (PERMISSION_DENIED)** | 타인 영수증 재사용 중복 차단 (`E_RECEIPT_DUP`) |
| **TC-17** | `/receiptHashes/{hash}` | `read` | 로그인 / 익명 유저 | 해시 컬렉션 문서 열람 | **DENY (PERMISSION_DENIED)** | 타인 영수증 결제 정보 해시 유출 방지 |
| **TC-18** | `/nicknames/{name}` | `get` (단건 조회) | 비로그인 (익명) | 특정 닉네임 문서 단건 get (`/nicknames/gukbap`) | **ALLOW (200 OK)** | 가입 전 실시간 닉네임 중복 체크 허용 |
| **TC-19** | `/nicknames/{name}` | `list` (목록 쿼리) | 로그인 / 익명 유저 | 전체 닉네임 목록 쿼리 (`collection("nicknames")`) | **DENY (PERMISSION_DENIED)** | 회원 닉네임 대량 스크래핑/크롤링 차단 |
| **TC-20** | `/diagnostics/{uid}` | `read` / `write` | `uid: "user_123"` 본인 | 1일차 스파이크 진단 데이터 왕복 | **ALLOW (200 OK)** | SPK-5 진단 핑퐁 콘솔 허용 |

---

## 2. Firebase 콘솔 규칙 플레이그라운드 모의 시나리오 (단계별 실행 가이드)

Firebase 웹 콘솔(`Firestore Database > Rules > Rules Playground`)에서 별도 코드 작성 없이 즉시 시뮬레이션할 수 있는 주요 시나리오 입력값입니다.

### 시나리오 A: 가짜 필드를 주입하여 가입 시도 (TC-02)
- **작업 유형 (Simulation type)**: `create`
- **위치 (Location)**: `users/test_user_01`
- **인증 (Authentication)**: 인증됨 (Provider: `password`, UID: `test_user_01`)
- **문서 내용 (Document data)**:
  ```json
  {
    "nickname": "식객홍길동",
    "nicknameLower": "식객홍길동",
    "titleId": "TITLE_01",
    "totalYears": 0,
    "visitCount": 0,
    "reviewCount": 0,
    "isAdmin": true,
    "createdAt": "request.time"
  }
  ```
- **시뮬레이션 실행 결과**: 🔴 **PERMISSION_DENIED (거부됨)**
- **통과 사유**: `userKeys()` 화이트리스트에 없는 `isAdmin` 필드가 감지되어 즉시 차단됨.

---

### 시나리오 B: 미방문 상태에서 공인 리뷰 작성 시도 (TC-12)
- **작업 유형 (Simulation type)**: `create`
- **위치 (Location)**: `reviews/BN-1001_test_user_01`
- **인증 (Authentication)**: 인증됨 (UID: `test_user_01`)
- **문서 내용 (Document data)**:
  ```json
  {
    "storeId": "BN-1001",
    "storeName": "이문설농탕",
    "uid": "test_user_01",
    "nickname": "식객홍길동",
    "titleId": "TITLE_01",
    "rating": 5,
    "text": "정말 깊은 120년 국물 맛입니다.",
    "createdAt": "request.time"
  }
  ```
- **전제 조건**: Firestore 내에 `users/test_user_01/visits/BN-1001` 문서가 존재하지 않음.
- **시뮬레이션 실행 결과**: 🔴 **PERMISSION_DENIED (거부됨)**
- **통과 사유**: `exists(/databases/$(database)/documents/users/test_user_01/visits/BN-1001)` 조건이 `false`로 평가됨.

---

### 시나리오 C: 이미 사용된 영수증 해시 재사용 시도 (TC-16)
- **작업 유형 (Simulation type)**: `create`
- **위치 (Location)**: `receiptHashes/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
- **인증 (Authentication)**: 인증됨 (UID: `test_user_02`)
- **전제 조건**: 이미 해당 위치에 문서가 등록되어 있음 (이전 사용자가 생성 완료).
- **시뮬레이션 실행 결과**: 🔴 **PERMISSION_DENIED (거부됨)**
- **통과 사유**: `allow update: if false;` 및 이미 존재하는 문서에 대한 create 재시도는 수정(update)으로 간주되어 원천 거부됨. 앱의 `SyncWorker`는 이를 감지하여 `handleDuplicateReceiptRollback()`을 실행함.

---

## 3. Jest 기반 자동화 단위 테스트 스크립트 (`rules.test.ts`)

로컬 에뮬레이터(`firebase emulators:start --only firestore`) 환경에서 `npm test`로 1초 만에 회귀 검증할 수 있는 단위 테스트 코드 명세입니다.

```typescript
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";

let testEnv: RulesTestEnvironment;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "nopo-cheonnyeon-test",
    firestore: {
      rules: fs.readFileSync("firebase/firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

describe("노포 천년 미식가 - Firestore 보안 규칙 테스트", () => {

  const aliceUid = "alice_123";
  const bobUid = "bob_456";
  const storeId = "BN-0001";

  // ---------------------------------------------------------------------------
  // 1. 유저 프로필 (/users/{uid}) 검증
  // ---------------------------------------------------------------------------
  test("I-1: 정상 초기값으로 가입 시 성공", async () => {
    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const userRef = aliceDb.collection("users").document(aliceUid);

    await assertSucceeds(
      userRef.set({
        nickname: "국밥장인",
        nicknameLower: "국밥장인",
        titleId: "TITLE_01",
        totalYears: 0,
        visitCount: 0,
        reviewCount: 0,
        createdAt: new Date(),
        updatedAt: new Date(),
      })
    );
  });

  test("I-2: 가입 시 세월 치팅(totalYears > 0) 시도시 거절", async () => {
    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const userRef = aliceDb.collection("users").document(aliceUid);

    await assertFails(
      userRef.set({
        nickname: "해커식객",
        nicknameLower: "해커식객",
        titleId: "TITLE_05",
        totalYears: 1000, // 치팅 시도
        visitCount: 10,
        reviewCount: 0,
        createdAt: new Date(),
      })
    );
  });

  test("I-3: 단일 인증 세월 적립이 200년 이하인 경우 갱신 성공", async () => {
    // 사전에 관리자 권한으로 기존 유저 생성
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection("users").doc(aliceUid).set({
        nickname: "국밥장인",
        nicknameLower: "국밥장인",
        titleId: "TITLE_01",
        totalYears: 100,
        visitCount: 2,
        reviewCount: 1,
        createdAt: new Date(),
      });
    });

    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const userRef = aliceDb.collection("users").doc(aliceUid);

    // +62년 증가 (100 -> 162)
    await assertSucceeds(
      userRef.update({
        totalYears: 162,
        visitCount: 3,
        reviewCount: 1,
      })
    );
  });

  test("I-4: 단일 인증 세월 적립이 200년을 초과하여 급증할 경우 거절", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection("users").doc(aliceUid).set({
        nickname: "국밥장인",
        nicknameLower: "국밥장인",
        totalYears: 100,
        visitCount: 2,
        reviewCount: 0,
        createdAt: new Date(),
      });
    });

    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    // +300년 급증 시도 (100 -> 400)
    await assertFails(
      aliceDb.collection("users").doc(aliceUid).update({
        totalYears: 400,
        visitCount: 3,
      })
    );
  });

  // ---------------------------------------------------------------------------
  // 2. 공인 리뷰 (/reviews) 검증
  // ---------------------------------------------------------------------------
  test("I-5: 해당 가게 방문 인증 문서가 있을 때만 리뷰 작성 가능", async () => {
    // 1) 앨리스의 방문 기록 사전 등록
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore()
        .collection("users").doc(aliceUid)
        .collection("visits").doc(storeId)
        .set({
          storeId: storeId,
          yearsAwarded: 50,
          visitCount: 1,
        });
    });

    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const reviewRef = aliceDb.collection("reviews").doc(`${storeId}_${aliceUid}`);

    await assertSucceeds(
      reviewRef.set({
        storeId: storeId,
        uid: aliceUid,
        rating: 5,
        text: "정갈하고 깊은 맛!",
        createdAt: new Date(),
      })
    );
  });

  test("I-6: 미방문 가게에 리뷰 작성 시도시 거절", async () => {
    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const unvisitedStoreId = "BN-9999";
    const reviewRef = aliceDb.collection("reviews").doc(`${unvisitedStoreId}_${aliceUid}`);

    await assertFails(
      reviewRef.set({
        storeId: unvisitedStoreId,
        uid: aliceUid,
        rating: 1,
        text: "안 가봤지만 별점 테러",
        createdAt: new Date(),
      })
    );
  });

  // ---------------------------------------------------------------------------
  // 3. 영수증 원타임 해시 (/receiptHashes) 검증
  // ---------------------------------------------------------------------------
  test("I-7: 신규 영수증 해시 등록 성공, 기존 해시 덮어쓰기 거절", async () => {
    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    const receiptHash = "hash_sample_abc_123";
    const hashRef = aliceDb.collection("receiptHashes").doc(receiptHash);

    // 1) 앨리스의 첫 사용 -> 성공
    await assertSucceeds(
      hashRef.set({
        uid: aliceUid,
        storeId: storeId,
        createdAt: new Date(),
      })
    );

    // 2) 밥이 동일한 영수증 해시 재사용 시도 -> 거절 (PERMISSION_DENIED)
    const bobDb = testEnv.authenticatedContext(bobUid).firestore();
    await assertFails(
      bobDb.collection("receiptHashes").doc(receiptHash).set({
        uid: bobUid,
        storeId: storeId,
        createdAt: new Date(),
      })
    );
  });

  test("I-8: 영수증 해시는 누구도 읽을 수 없음 (개인정보 보호)", async () => {
    const receiptHash = "hash_sample_secret";
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection("receiptHashes").doc(receiptHash).set({
        uid: aliceUid,
      });
    });

    const aliceDb = testEnv.authenticatedContext(aliceUid).firestore();
    // 본인이 쓴 해시라도 직접 열람 불가
    await assertFails(aliceDb.collection("receiptHashes").doc(receiptHash).get());
  });

  // ---------------------------------------------------------------------------
  // 4. 닉네임 유일성 인덱스 (/nicknames) 검증
  // ---------------------------------------------------------------------------
  test("I-9: 닉네임 단건 get은 누구나 가능하나, list 쿼리는 차단", async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    
    // 단건 조회(중복 검사용) -> 성공
    await assertSucceeds(unauthDb.collection("nicknames").doc("gukbap").get());

    // 대량 스크래핑 쿼리 -> 거절
    await assertFails(unauthDb.collection("nicknames").get());
  });
});
```

---

## 4. 실기기 QA 및 심사 시연 체크리스트 연동

- [x] **Firebase Console Rules Simulator 통과**: 20개 테스트 벡터 전수 통과 확인.
- [x] **Room 롤백 핸들러 연동**: TC-16 발생 시 안드로이드 `SyncWorker`의 `PERMISSION_DENIED` 분기가 정상 작동하여 로컬 `my_visits`를 안전하게 롤백하고 `E_RECEIPT_DUP` 알림을 띄우는지 실기기 확인 (QA 시나리오 M-49).
- [x] **리뷰 작성 버튼 활성화 게이트**: 방문 인증 직후 서버 동기화가 끝나기 전(`E_VISIT_SYNCING`)에는 UI 상에서 리뷰 작성 버튼이 비활성화되어 TC-12 에러를 사전에 방지하는지 확인 (QA 시나리오 M-24).
- [x] **1일차 스파이크 진단 콘솔**: TC-20을 활용해 실기기에서 `diagnostics/{uid}` 왕복 핑을 100ms 이내로 측정 완료 (SPK-5 체크리스트).

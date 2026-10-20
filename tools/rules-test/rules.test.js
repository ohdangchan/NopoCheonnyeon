// Firestore 보안 규칙 시험 (리뷰 부분). 실행: cd firebase && firebase emulators:exec --only firestore "node ../tools/rules-test/rules.test.js"
// 에뮬레이터만 쓴다(실제 서버·데이터 건드리지 않음).
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, updateDoc, deleteDoc, serverTimestamp, deleteField } = require("firebase/firestore");
const fs = require("fs");
const path = require("path");

const STORE = "BN-100";
const ME = "alice";
let passed = 0, failed = 0;
async function check(name, p) {
  try { await p; passed++; console.log("PASS", name); } catch (e) { failed++; console.log("FAIL", name, String(e.message).split("\n")[0]); }
}

function review(extra = {}) {
  return {
    storeId: STORE, storeName: "가게", uid: ME, nickname: "앨리스", titleId: "SPROUT", rating: 5, text: "맛있어요", visitCount: 1,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...extra,
  };
}

(async () => {
  const env = await initializeTestEnvironment({
    projectId: "nopo-rules-test",
    firestore: { rules: fs.readFileSync(path.join(__dirname, "..", "..", "firebase", "firestore.rules"), "utf8") },
  });
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "users", ME), { nickname: "앨리스", nicknameLower: "앨리스", totalYears: 0, visitCount: 0, reviewCount: 0 });
    await setDoc(doc(db, "users", ME, "visits", STORE), { storeId: STORE, yearsAwarded: 40, visitCount: 1 });
  });
  const alice = env.authenticatedContext(ME).firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const rid = (slot) => `${STORE}_${ME}_${slot}`;

  await check("슬롯 1에 새 리뷰", assertSucceeds(setDoc(doc(alice, "reviews", rid(1)), review())));
  await check("같은 가게에 슬롯 2 리뷰도 가능", assertSucceeds(setDoc(doc(alice, "reviews", rid(2)), review({ rating: 3 }))));
  await check("슬롯 6은 거부(최대 5개)", assertFails(setDoc(doc(alice, "reviews", rid(6)), review())));
  await check("시각 형식 ID는 거부(무제한 방지)", assertFails(setDoc(doc(alice, "reviews", `${STORE}_${ME}_1730000000000`), review())));
  await check("예전 형식 ID(가게_uid)는 허용", assertSucceeds(setDoc(doc(alice, "reviews", `${STORE}_${ME}`), review())));
  await check("사진 3장까지 허용", assertSucceeds(setDoc(doc(alice, "reviews", rid(3)), review({ photos: ["a", "b", "c"] }))));
  await check("사진 4장은 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ photos: ["a", "b", "c", "d"] }))));
  await check("사진 원소가 문자열이 아니면 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ photos: [123] }))));
  await check("사진 한 장이 너무 크면 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ photos: ["x".repeat(170001)] }))));
  await check("모르는 필드는 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ admin: true }))));
  await check("남의 닉네임으로는 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ nickname: "누군가" }))));
  await check("별점 6은 거부", assertFails(setDoc(doc(alice, "reviews", rid(4)), review({ rating: 6 }))));
  await check("방문 인증이 없는 가게는 거부", assertFails(setDoc(doc(alice, "reviews", `OTHER_${ME}_1`), review({ storeId: "OTHER" }))));
  await check("남이 내 리뷰를 고칠 수 없다", assertFails(updateDoc(doc(bob, "reviews", rid(1)), { rating: 1, updatedAt: serverTimestamp() })));
  await check("남이 내 리뷰를 지울 수 없다", assertFails(deleteDoc(doc(bob, "reviews", rid(1)))));
  await check("내 리뷰 수정", assertSucceeds(updateDoc(doc(alice, "reviews", rid(1)), { rating: 4, text: "고침", photos: deleteField(), updatedAt: serverTimestamp() })));
  await check("내 리뷰 삭제", assertSucceeds(deleteDoc(doc(alice, "reviews", rid(2)))));
  await check("삭제로 빈 슬롯 2에 다시 쓰기", assertSucceeds(setDoc(doc(alice, "reviews", rid(2)), review())));

  await env.cleanup();
  console.log(`\n통과 ${passed}, 실패 ${failed}`);
  process.exit(failed ? 1 : 0);
})();

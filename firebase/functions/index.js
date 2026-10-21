// Gemini 프록시: 앱은 Gemini 키를 갖지 않고, 로그인한 사용자의 ID 토큰으로 이 함수를 부른다.
// 키는 Secret Manager(GEMINI_API_KEY)에만 있다. 모델·응답 형식은 서버에서 고정한다.
const functions = require('firebase-functions/v1');
const admin = require('firebase-admin');
admin.initializeApp();

const MODEL = 'gemini-3.1-flash-lite';
const DAILY_LIMIT = 300; // 사용자 1명이 하루에 부를 수 있는 횟수
const MAX_BODY = 4 * 1024 * 1024;

exports.gemini = functions
  .region('asia-northeast3')
  .runWith({ secrets: ['GEMINI_API_KEY'], timeoutSeconds: 60, memory: '256MB', maxInstances: 10 })
  .https.onRequest(async (req, res) => {
    if (req.method !== 'POST') return res.status(405).json({ error: 'method' });

    const m = /^Bearer (.+)$/.exec(req.get('Authorization') || '');
    if (!m) return res.status(401).json({ error: 'auth' });
    let uid;
    try {
      uid = (await admin.auth().verifyIdToken(m[1])).uid;
    } catch (_) {
      return res.status(401).json({ error: 'auth' });
    }

    const body = req.body;
    if (!body || !Array.isArray(body.contents) || body.contents.length === 0 ||
        JSON.stringify(body).length > MAX_BODY) {
      return res.status(400).json({ error: 'body' });
    }

    // 하루 호출 수 제한(서울 날짜 기준)
    const day = new Date(Date.now() + 9 * 3600 * 1000).toISOString().slice(0, 10);
    const ref = admin.firestore().collection('aiUsage').doc(`${uid}_${day}`);
    try {
      const ok = await admin.firestore().runTransaction(async (t) => {
        const n = (await t.get(ref)).data()?.count || 0;
        if (n >= DAILY_LIMIT) return false;
        t.set(ref, { count: n + 1, uid, day });
        return true;
      });
      if (!ok) return res.status(429).json({ error: 'limit' });
    } catch (_) {
      return res.status(500).json({ error: 'usage' });
    }

    // 허용하는 필드만 전달한다(모델·안전 설정 등은 앱이 정할 수 없다)
    const payload = { contents: body.contents, generationConfig: body.generationConfig };
    try {
      const r = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'x-goog-api-key': process.env.GEMINI_API_KEY },
          body: JSON.stringify(payload),
        });
      res.status(r.status).type('application/json').send(await r.text());
    } catch (_) {
      res.status(502).json({ error: 'upstream' });
    }
  });

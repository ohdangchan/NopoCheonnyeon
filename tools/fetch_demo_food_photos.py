#!/usr/bin/env python3
"""시연용 리뷰 사진: Wikimedia Commons에서 가게 메뉴에 맞는 무료 이용 사진을 받는다 (키 필요 없음).

    python tools/fetch_demo_food_photos.py

- 라이선스가 CC0·퍼블릭 도메인·CC BY·CC BY-SA 인 가로 사진만 쓴다(출처 표기 의무가 있는 것은 앱의 오픈소스 라이선스 화면에 안내).
- 결과: app/src/main/assets/demo/R_<가게ID>_<리뷰번호>_<사진번호>.jpg (긴 변 800px, JPEG),
        data/demo_review_photos.json (가게 -> 리뷰별 파일 목록), data/demo_photo_credits.json (파일별 작가·라이선스·원본 주소)
- 이 사진은 시연용 예시일 뿐 해당 가게의 실제 사진이 아니다. 이어서 `python tools/gen_demo_data.py` 가 리뷰에 붙인다.
"""
import io, json, os, re, sys, time, urllib.parse, urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "demo")
MANIFEST = os.path.join(ROOT, "data", "demo_review_photos.json")
CREDITS = os.path.join(ROOT, "data", "demo_photo_credits.json")
UA = "NopoCheonnyeonDemo/1.0 (hackathon demo; contact: github.com/ohdangchan/NopoCheonnyeon)"
API = "https://commons.wikimedia.org/w/api.php"

# 가게 -> 검색어(메뉴에 맞춰 고른다)
QUERIES = {
    "BN-20230021756": ["pork galbi grilled Korean", "dwaeji galbi", "Korean BBQ pork grill", "galmaegisal"],
    "BN-20230021552": ["galbitang", "gomtang beef soup", "Korean beef rib soup", "kalbi tang"],
    "BN-20240059612": ["mandu Korean dumplings", "Korean dumpling soup mandu guk", "naengmyeon", "gogi mandu"],
    "BN-HND004115": ["gamjatang pork bone stew", "gamja-tang", "sujebi", "Korean mandu dumplings"],
    "BN-HND002575": ["naengmyeon Korean cold noodles", "bibim naengmyeon", "Hamhung naengmyeon"],
    "BN-HND002446": ["chapssal tteok", "injeolmi", "red bean rice cake", "mochi Korean"],
    "BN-HND002322": ["naengmyeon Korean cold noodles", "Pyongyang naengmyeon", "mul naengmyeon"],
    "BN-HND002144": ["seolleongtang", "seolleongtang ox bone soup", "kimchi kkakdugi", "Korean rice and soup"],
    "BN-HND004733": ["wedding photography", "graduation photo", "studio portrait lighting", "child portrait smile"],
    "BN-20250295902": ["tteokbokki", "topokki cheese", "Korean tteokbokki dish"],
    "BN-20250277476": ["interior renovation", "office renovation interior", "operable wall partition", "room divider partition"],
    "OR-0c1dcb74": ["songpyeon", "tteok Korean rice cake", "injeolmi", "tteok pounding"],
    "TEST-NOPO": ["bossam Korean boiled pork", "suyuk boiled pork", "seolleongtang", "bap Korean rice bowl"],
}
OK_LICENSE = re.compile(r"^(cc0|public domain|pd|cc[ -]by(-sa)?[ -]?\d(\.\d)?)", re.I)
# 리뷰별 사진 장수(5개 리뷰): 1·2·1·3·1장
PER_REVIEW = [1, 2, 1, 3, 1]


def get(url, params=None, binary=False):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=40) as r:
                data = r.read()
                return data if binary else json.loads(data)
        except Exception as e:  # 속도 제한(429) 등은 잠시 쉬었다 다시
            time.sleep(3 * (attempt + 1))
            last = e
    raise last


def strip(html):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", html or "")).strip()


def search(q, limit=50):
    data = get(API, {
        "action": "query", "format": "json", "generator": "search", "gsrnamespace": 6, "gsrlimit": limit,
        "gsrsearch": q + " filetype:bitmap", "prop": "imageinfo", "iiprop": "url|size|mime|extmetadata", "iiurlwidth": 800,
    })
    out = []
    for p in (data.get("query") or {}).get("pages", {}).values():
        ii = (p.get("imageinfo") or [{}])[0]
        meta = ii.get("extmetadata") or {}
        lic = (meta.get("LicenseShortName") or {}).get("value", "")
        if ii.get("mime") != "image/jpeg" or not OK_LICENSE.match(lic.strip()):
            continue
        w, h = ii.get("width", 0), ii.get("height", 0)
        if w < 900 or h < 600 or w < h:       # 너무 작거나 세로 사진은 뺀다
            continue
        out.append({
            "title": p["title"], "thumb": ii.get("thumburl") or ii["url"], "page": ii.get("descriptionurl", ""),
            "author": strip((meta.get("Artist") or {}).get("value", ""))[:80], "license": lic.strip(),
        })
        time.sleep(0.0)
    return out


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):          # 예전 그림 사진 정리
        if f.startswith("R_"):
            os.remove(os.path.join(OUT, f))
    pools, used_global, credits, manifest = {}, set(), {}, {}
    for store, queries in QUERIES.items():
        cands = []
        for q in queries:
            if q not in pools:
                pools[q] = search(q)
                print(f"{q!r}: 후보 {len(pools[q])}장")
                time.sleep(1.0)
            cands += [c for c in pools[q] if c["title"] not in {x["title"] for x in cands}]
        words = [w for q in queries for w in q.lower().split() if len(w) > 3]
        cands.sort(key=lambda c: -sum(w in c["title"].lower() for w in words))
        need = sum(PER_REVIEW)
        # 다른 가게와 겹치지 않도록 아직 안 쓴 사진을 먼저
        fresh = [c for c in cands if c["title"] not in used_global] + [c for c in cands if c["title"] in used_global]
        picks = fresh[:need]
        while picks and len(picks) < need:
            picks.append(picks[len(picks) % len(picks)])
        rows, idx = [], 0
        for k, n in enumerate(PER_REVIEW, 1):
            files = []
            for j in range(1, n + 1):
                if idx >= len(picks):
                    break
                c = picks[idx]; idx += 1
                name = f"R_{store}_{k}_{j}.jpg"
                img = Image.open(io.BytesIO(get(c["thumb"], binary=True))).convert("RGB")
                img.thumbnail((800, 800))
                img.save(os.path.join(OUT, name), "JPEG", quality=74, optimize=True)
                used_global.add(c["title"])
                credits[name] = {"title": c["title"], "author": c["author"], "license": c["license"], "source": c["page"]}
                files.append("demo/" + name)
                time.sleep(0.5)
            if files:
                rows.append(files)
        manifest[store] = rows
        print(store, "->", [len(r) for r in rows])
    json.dump(manifest, open(MANIFEST, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(credits, open(CREDITS, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    size = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT) if f.startswith("R_"))
    print(f"사진 {len(credits)}장, {size // 1024}KB")


if __name__ == "__main__":
    main()

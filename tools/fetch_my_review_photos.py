#!/usr/bin/env python3
"""시연용 '내 리뷰' 10개에 붙일 음식 사진을 Wikimedia Commons(CC0/CC BY/CC BY-SA)에서 받는다.

    python tools/fetch_my_review_photos.py

- 가게 메뉴에 맞는 검색어로 받고, 이미 다른 시연 사진에 쓴 파일은 피한다.
- 결과: app/src/main/assets/demo/M_<가게ID>_<순번>.jpg, data/demo_my_photos.json(가게 -> 파일 목록), data/demo_photo_credits.json(출처 추가)
- 이어서 `python tools/gen_demo_data.py` 가 내 리뷰에 붙인다.
"""
import importlib.util, io, json, os, sys, time

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("ff", os.path.join(ROOT, "tools", "fetch_demo_food_photos.py"))
ff = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ff)

OUT = ff.OUT
MANIFEST = os.path.join(ROOT, "data", "demo_my_photos.json")

# (가게 ID, 검색어 목록, 사진 장수) — 내 리뷰 10개. 방문(도장)한 가게들의 메뉴에 맞춘다.
PLAN = [
    ("BN-HND002144", ["seolleongtang", "kkakdugi radish kimchi"], 3),
    ("BN-HND004115", ["gamjatang pork bone stew", "sujebi"], 2),
    ("BN-HND002575", ["bibim naengmyeon", "naengmyeon Korean cold noodles"], 2),
    ("BN-HND002177", ["makguksu buckwheat noodles", "bossam Korean boiled pork"], 3),
    ("OR-b3736906", ["samgyetang ginseng chicken soup", "Korean fried chicken"], 1),
    ("OR-74ddbd92", ["Pyongyang naengmyeon", "mul naengmyeon"], 2),
    ("OR-f0dc9b95", ["haejangguk hangover soup", "suyuk boiled pork"], 3),
    ("BN-HND003115", ["samgyeopsal Korean barbecue", "Korean pork belly grill"], 1),
    ("BN-HND003112", ["kalguksu noodle soup", "wang mandu dumplings"], 2),
    ("BN-HND003131", ["agujjim", "monkfish stew", "Korean spicy braised seafood stew", "haemul jjim"], 3),
]


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    credits = json.load(open(ff.CREDITS, encoding="utf-8"))
    used = {c["title"] for c in credits.values()}
    for f in os.listdir(OUT):
        if f.startswith("M_"):
            os.remove(os.path.join(OUT, f))
    manifest, pools = {}, {}
    for store, queries, n in PLAN:
        cands = []
        for q in queries:
            if q not in pools:
                pools[q] = ff.search(q)
                time.sleep(1.0)
            cands += [c for c in pools[q] if c["title"] not in {x["title"] for x in cands}]
        words = [w for q in queries for w in q.lower().split() if len(w) > 3]
        cands.sort(key=lambda c: -sum(w in c["title"].lower() for w in words))
        fresh = [c for c in cands if c["title"] not in used] or cands
        files = []
        for j, c in enumerate(fresh[:n], 1):
            name = f"M_{store}_{j}.jpg"
            img = Image.open(io.BytesIO(ff.get(c["thumb"], binary=True))).convert("RGB")
            img.thumbnail((800, 800))
            img.save(os.path.join(OUT, name), "JPEG", quality=74, optimize=True)
            used.add(c["title"])
            credits[name] = {"title": c["title"], "author": c["author"], "license": c["license"], "source": c["page"]}
            files.append("demo/" + name)
            time.sleep(0.5)
        manifest[store] = files
        print(store, len(files))
    json.dump(manifest, open(MANIFEST, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(credits, open(ff.CREDITS, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("완료", sum(len(v) for v in manifest.values()), "장")


if __name__ == "__main__":
    main()

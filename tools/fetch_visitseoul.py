#!/usr/bin/env python3
"""비짓서울(서울관광재단) 음식점 사진을 우리 서울 노포에 맞춰 내려받는다.

사용 허락: 서울관광재단에 전화로 확인(비짓서울 사진은 내려받아 활용 가능). 서면(이메일) 확인을 받으면 docs/에 기록할 것.
예의: 요청 사이 1초 쉬고, 식별 가능한 User-Agent를 쓰고, 이미 받은 페이지는 캐시를 쓴다. robots.txt가 막은 경로(/member, /admin, /search)는 건드리지 않는다.

단계(순서대로 실행):
  python tools/fetch_visitseoul.py list      # 한국어 사이트 음식점 목록 전체(약 160쪽) -> 캐시
  python tools/fetch_visitseoul.py match     # 우리 서울 가게와 이름·위치로 맞춰 보기 -> 캐시/matches.json
  python tools/fetch_visitseoul.py download  # 맞춘 가게의 사진을 줄여서 assets/visitseoul/ 에 저장 + 색인 JSON
"""
import argparse, io, json, math, re, sys, time, urllib.parse, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = "https://korean.visitseoul.net"
UA = {"User-Agent": "Mozilla/5.0 (compatible; NopoCheonnyeonDemo/1.0; student hackathon project; polite 1 req/s)"}
CACHE = ROOT / "tools" / "_vs_cache"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "visitseoul"
INDEX = ROOT / "app" / "src" / "main" / "assets" / "visitseoul_photos.json"
MAX_SIDE = 1080
MAX_DISTANCE_M = 300
DELAY = 1.0
_last = [0.0]


def fetch(url, binary=False):
    wait = DELAY - (time.time() - _last[0])
    if wait > 0:
        time.sleep(wait)
    p = urllib.parse.urlsplit(url)
    url = urllib.parse.urlunsplit((p.scheme, p.netloc, urllib.parse.quote(urllib.parse.unquote(p.path), safe="/"), p.query, ""))
    req = urllib.request.Request(url, headers=UA)
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=40) as r:
                data = r.read()
            _last[0] = time.time()
            return data if binary else data.decode("utf8", "replace")
        except Exception as e:  # noqa: BLE001
            _last[0] = time.time()
            if attempt == 2:
                raise
            time.sleep(3 * (attempt + 1))


def cached_page(key, url):
    f = CACHE / "pages" / (re.sub(r"[^0-9A-Za-z_.-]", "_", key) + ".html")
    if f.exists():
        return f.read_text(encoding="utf8")
    f.parent.mkdir(parents=True, exist_ok=True)
    html = fetch(url)
    f.write_text(html, encoding="utf8")
    return html


def norm(s):
    s = re.sub(r"\(.*?\)", "", s or "")
    s = re.sub(r"[^0-9A-Za-z가-힣]", "", s).lower()
    return re.sub(r"(본점|본관|별관|직영점|분점)$", "", s)


def haversine(a, b, c, d):
    r = 6371000
    p1, p2 = math.radians(a), math.radians(c)
    dp, dl = p2 - p1, math.radians(d - b)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def stage_list():
    first = cached_page("list_1", f"{BASE}/restaurants?curPage=1")
    last = max(int(x) for x in re.findall(r"curPage=(\d+)", first))
    cards = {}
    for n in range(1, last + 1):
        html = first if n == 1 else cached_page(f"list_{n}", f"{BASE}/restaurants?curPage={n}")
        for m in re.finditer(r'<a href="(/restaurants/[^"]+/(KOP\d+))"[^>]*>.*?<span class="title">([^<]+)</span>', html, re.S):
            cards[m.group(2)] = {"id": m.group(2), "href": urllib.parse.unquote(m.group(1)), "name": m.group(3).strip()}
        if n % 20 == 0:
            print(f"list {n}/{last}, cards {len(cards)}", flush=True)
    (CACHE).mkdir(parents=True, exist_ok=True)
    (CACHE / "cards.json").write_text(json.dumps(list(cards.values()), ensure_ascii=False, indent=1), encoding="utf8")
    print(f"done: {last} pages, {len(cards)} restaurants")


def load_stores():
    data = json.loads((ROOT / "app/src/main/assets/stores.json").read_text(encoding="utf8"))
    stores = data["stores"] if isinstance(data, dict) and "stores" in data else data
    return [s for s in stores if str(s.get("sido", "")).startswith("서울") or str(s.get("address", "")).startswith("서울")]


def parse_detail(html):
    lat = re.search(r"lat\s*=\s*'([\d.]+)", html)
    lng = re.search(r"lng\s*=\s*'([\d.]+)", html)
    photos = []
    for m in re.finditer(r'<a href="(/comm/getImage\?[^"]+)"[^>]*class="js-photo-viewer"', html):
        u = m.group(1).replace("&amp;", "&")
        if u not in photos:
            photos.append(u)
    og = re.search(r'property="og:image" content="([^"]+)"', html)
    # 상세 정보는 <dl><dt>라벨</dt><dd>값</dd></dl> 꼴이다(주소, 전화번호, 웹사이트 …)
    fields = {}
    for m in re.finditer(r"<dt>\s*([^<]+?)\s*</dt>\s*<dd>(.*?)</dd>", html, re.S):
        fields.setdefault(m.group(1), m.group(2))
    clean = lambda v: re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", (v or "").replace("&nbsp;", " "))).strip()
    website = ""
    for label, raw in fields.items():
        if any(k in label for k in ("웹사이트", "홈페이지", "Website", "URL")):
            href = re.search(r'href="(https?://[^"]+)"', raw)
            text = re.search(r"https?://[^\s<\"']+", raw)
            website = (href or text).group(1 if href else 0) if (href or text) else ""
            break
    return {
        "lat": float(lat.group(1)) if lat else None,
        "lng": float(lng.group(1)) if lng else None,
        "photos": photos,
        "og": og.group(1).replace("&amp;", "&") if og else None,
        "address": re.sub(r"^\d{5}\s*", "", clean(fields.get("주소"))),
        "phone": clean(fields.get("전화번호")),
        "website": website,
    }


def stage_match():
    cards = json.loads((CACHE / "cards.json").read_text(encoding="utf8"))
    stores = load_stores()
    by_name = {}
    for c in cards:
        by_name.setdefault(norm(c["name"]), []).append(c)
    matches, rejected, nocand = {}, [], 0
    for s in stores:
        key = norm(s["name"])
        cands = list(by_name.get(key, []))
        if not cands:
            nocand += 1
            continue
        for c in cands:
            html = cached_page(c["id"], BASE + c["href"])
            d = parse_detail(html)
            dist = None
            if d["lat"] and s.get("lat") and s.get("lng"):
                dist = haversine(float(s["lat"]), float(s["lng"]), d["lat"], d["lng"])
            addr_ok = False
            if d["address"] and s.get("address"):
                toks = [t for t in re.split(r"\s+", s["address"]) if t]
                road = [t for t in toks if re.search(r"(로|길)\d*$|(로|길)$", t)]
                addr_ok = bool(road) and all(r in d["address"] for r in road[:1]) and (len(toks) < 2 or toks[1] in d["address"] or True)
            ok = (dist is not None and dist <= MAX_DISTANCE_M) or (dist is None and addr_ok)
            rec = {"storeId": s["id"], "storeName": s["name"], "vsId": c["id"], "vsName": c["name"], "href": c["href"],
                   "distanceM": None if dist is None else round(dist), "addrOk": addr_ok, "photos": d["photos"], "og": d["og"],
                   "vsAddress": d["address"], "website": d["website"], "phone": d["phone"]}
            if ok and (d["photos"] or d["og"]):
                matches[s["id"]] = rec
                break
            rejected.append(rec)
    (CACHE / "matches.json").write_text(json.dumps(list(matches.values()), ensure_ascii=False, indent=1), encoding="utf8")
    (CACHE / "rejected.json").write_text(json.dumps(rejected, ensure_ascii=False, indent=1), encoding="utf8")
    total_photos = sum(len(m["photos"]) or 1 for m in matches.values())
    print(f"seoul stores {len(stores)}, name candidates {len(stores) - nocand}, matched {len(matches)}, rejected {len(rejected)}, photos {total_photos}")


def shrink(data):
    from PIL import Image  # pip install pillow
    im = Image.open(io.BytesIO(data)).convert("RGB")
    w, h = im.size
    if max(w, h) > MAX_SIDE:
        k = MAX_SIDE / max(w, h)
        im = im.resize((round(w * k), round(h * k)), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "JPEG", quality=80, optimize=True)
    return buf.getvalue()


def stage_download():
    matches = json.loads((CACHE / "matches.json").read_text(encoding="utf8"))
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    index = {}
    for i, m in enumerate(matches, 1):
        urls = m["photos"] or ([m["og"]] if m["og"] else [])
        files = []
        for k, u in enumerate(urls, 1):
            name = f"{m['storeId']}_{k}.jpg"
            f = OUT_DIR / name
            if not f.exists():
                try:
                    f.write_bytes(shrink(fetch(u if u.startswith("http") else BASE + u, binary=True)))
                except Exception as e:  # noqa: BLE001
                    print("skip", name, type(e).__name__)
                    continue
            files.append(name)
        if files:
            index[m["storeId"]] = {"files": files, "source": BASE + urllib.parse.quote(m["href"], safe="/"), "website": m.get("website", ""),
                                   "credit": "서울관광재단(비짓서울)"}
        if i % 10 == 0:
            print(f"download {i}/{len(matches)}", flush=True)
    INDEX.write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf8")
    size = sum(f.stat().st_size for f in OUT_DIR.glob("*.jpg")) / 1e6
    print(f"done: {len(index)} stores, {sum(len(v['files']) for v in index.values())} photos, {size:.1f} MB")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("stage", choices=["list", "match", "download"])
    a = ap.parse_args()
    {"list": stage_list, "match": stage_match, "download": stage_download}[a.stage]()

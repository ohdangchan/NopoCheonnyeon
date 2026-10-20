#!/usr/bin/env python3
"""공식 사진이 없는 가게의 대표 사진을 Google Places API (New)에서 한 장씩 받아 앱 자산으로 만든다.

    python tools/fetch_google_places.py [--limit N] [--ids ID,ID] [--width 640] [--sleep 0.15]

- 키: 환경변수 GOOGLE_PLACES_KEY (없으면 Windows 사용자 환경변수에서 읽는다). 키는 출력·저장하지 않는다.
- 가게마다 Text Search(이름+주소)로 후보를 찾고, 이름이 비슷하고 시·군·구가 같을 때만 채택한다.
- 사진은 첫 장(가로 사진 우선)을 폭 560px WebP로 줄여 `app/src/main/assets/places/<가게ID>.webp`에 저장한다.
- 결과: data/raw/google_places.jsonl (가게별 매칭 기록, 재실행하면 이어서), data/places_photos.json (가게 ID -> 자산 주소)
  -> `python tools/build_catalog.py`가 stores.json 의 photoUrls 에 공식 사진 다음으로 넣는다.
- 주의: Google 지도 플랫폼 약관은 사진의 저장·재배포를 제한하고 출처 표기를 요구한다. 사진 파일은 git 에 올리지 않고(.gitignore),
  앱은 사진 위에 "사진: Google 지도"를 표시한다. 공개 배포 전에는 약관을 다시 확인할 것.
"""
import argparse, difflib, io, json, os, re, sys, time, urllib.error, urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORES = os.path.join(ROOT, "app", "src", "main", "assets", "stores.json")
LOG = os.path.join(ROOT, "data", "raw", "google_places.jsonl")
OUT_MAP = os.path.join(ROOT, "data", "places_photos.json")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "places")
FIELDS = "places.id,places.displayName,places.formattedAddress,places.location,places.photos"


def api_key():
    k = os.environ.get("GOOGLE_PLACES_KEY")
    if k:
        return k
    import winreg
    with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment") as h:
        return winreg.QueryValueEx(h, "GOOGLE_PLACES_KEY")[0]


def norm(s):
    s = re.sub(r"\(.*?\)|주식회사|\(주\)|㈜|\s+|[^0-9A-Za-z가-힣]", "", s or "")
    return s.lower()


def similar(a, b):
    a, b = norm(a), norm(b)
    if not a or not b:
        return 0.0
    if len(a) >= 3 and (a in b or b in a):
        return 0.95
    return difflib.SequenceMatcher(None, a, b).ratio()


def search(key, store, loose=False):
    addr = re.split(r"[(,]", store.get("address") or "")[0].strip()
    if loose:   # 두 번째 시도: 주소 대신 시·군·구만, 위치 편향 없이
        addr = f"{store.get('sido', '')} {store.get('sigungu', '')}".strip()
    body = {"textQuery": f"{store['name']} {addr}".strip(), "languageCode": "ko", "regionCode": "KR", "maxResultCount": 5}
    if not loose and store.get("lat") is not None and store.get("lng") is not None:
        body["locationBias"] = {"circle": {"center": {"latitude": store["lat"], "longitude": store["lng"]}, "radius": 1500.0}}
    req = urllib.request.Request(
        "https://places.googleapis.com/v1/places:searchText", data=json.dumps(body).encode(), method="POST",
        headers={"Content-Type": "application/json", "X-Goog-Api-Key": key, "X-Goog-FieldMask": FIELDS})
    with urllib.request.urlopen(req, timeout=25) as r:
        return json.load(r).get("places", [])


def pick(store, places):
    sigungu = store.get("sigungu") or ""
    best, best_score = None, 0.0
    for p in places:
        if not p.get("photos"):
            continue
        score = similar(store["name"], p.get("displayName", {}).get("text", ""))
        addr = p.get("formattedAddress", "")
        same_area = (not sigungu) or (sigungu in addr)
        if score >= 0.9 and same_area or score >= 0.95:
            if score > best_score:
                best, best_score = p, score
    return best, best_score


def pick_photo(place):
    photos = place["photos"][:5]
    wide = [x for x in photos if x.get("widthPx", 0) >= x.get("heightPx", 1)]
    return (wide or photos)[0]


def download(key, photo_name, width):
    url = f"https://places.googleapis.com/v1/{photo_name}/media?maxWidthPx={width}"
    req = urllib.request.Request(url, headers={"X-Goog-Api-Key": key})
    with urllib.request.urlopen(req, timeout=40) as r:
        data = r.read()
    img = Image.open(io.BytesIO(data)).convert("RGB")
    if img.width > width:
        img = img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, "WEBP", quality=58, method=6)
    return buf.getvalue()


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--ids", default="")
    ap.add_argument("--width", type=int, default=560)
    ap.add_argument("--sleep", type=float, default=0.15)
    ap.add_argument("--retry-nomatch", action="store_true", help="매칭 실패한 가게를 주소 없이(시·군·구만) 다시 찾는다")
    a = ap.parse_args()
    key = api_key()
    stores = json.load(open(STORES, encoding="utf-8"))["stores"]
    want = set(filter(None, a.ids.split(",")))
    done = {}
    if os.path.exists(LOG):
        for line in open(LOG, encoding="utf-8"):
            r = json.loads(line)
            done[r["id"]] = r
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(LOG), exist_ok=True)
    todo = [s for s in stores if (not want or s["id"] in want)
            and not any(u.startswith("http") for u in s.get("photoUrls", []))   # 공식 사진이 없는 가게만
            and (s["id"] not in done or done[s["id"]]["status"] == "error" or (a.retry_nomatch and done[s["id"]]["status"] == "nomatch"))
            and not s["id"].startswith("TEST-")]
    if a.limit:
        todo = todo[: a.limit]
    print(f"대상 {len(todo)}곳 (이미 처리 {len(done)}곳)")
    with open(LOG, "a", encoding="utf-8") as log:
        for i, s in enumerate(todo, 1):
            rec = {"id": s["id"], "name": s["name"], "status": "nomatch"}
            try:
                place, score = pick(s, search(key, s, loose=a.retry_nomatch))
                if place:
                    photo = pick_photo(place)
                    data = download(key, photo["name"], a.width)
                    with open(os.path.join(OUT_DIR, s["id"] + ".webp"), "wb") as f:
                        f.write(data)
                    attr = (photo.get("authorAttributions") or [{}])[0].get("displayName", "")
                    rec.update(status="ok", placeId=place["id"], matched=place["displayName"]["text"], address=place.get("formattedAddress"),
                               score=round(score, 2), author=attr, bytes=len(data))
            except urllib.error.HTTPError as e:
                rec.update(status="error", http=e.code, detail=e.read().decode("utf-8", "ignore")[:200].replace(key, "***"))
                if e.code in (401, 403, 429):
                    log.write(json.dumps(rec, ensure_ascii=False) + "\n")
                    print("중단:", rec["http"], rec["detail"])
                    break
            except Exception as e:  # 네트워크 등 일시 오류는 다음 실행에서 다시 시도
                rec.update(status="error", detail=str(e)[:200])
            log.write(json.dumps(rec, ensure_ascii=False) + "\n")
            log.flush()
            if i % 25 == 0 or a.limit and i <= 20:
                print(f"{i}/{len(todo)} {rec['name']} -> {rec['status']} {rec.get('matched', '')} {rec.get('score', '')}")
            time.sleep(a.sleep)
    # 지도 만들기
    final = {}
    for line in open(LOG, encoding="utf-8"):
        r = json.loads(line)
        if r["status"] == "ok" and os.path.exists(os.path.join(OUT_DIR, r["id"] + ".webp")):
            final[r["id"]] = "file:///android_asset/places/" + r["id"] + ".webp"
    with open(OUT_MAP, "w", encoding="utf-8") as f:
        json.dump(final, f, ensure_ascii=False, indent=1)
    total = sum(os.path.getsize(os.path.join(OUT_DIR, x)) for x in os.listdir(OUT_DIR))
    print(f"사진 {len(final)}곳, {total // 1024}KB -> {OUT_MAP}")


if __name__ == "__main__":
    main()

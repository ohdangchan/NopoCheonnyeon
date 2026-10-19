#!/usr/bin/env python3
"""백년가게 공식 사진 주소 수집 (소상공인시장진흥공단 백년가게 상세 페이지).

각 가게의 source_url(상세 페이지)에서 대표 사진 주소만 뽑아 data/photo_cache.csv 에 쌓는다.
이미지 파일은 내려받지 않는다(주소만 저장). 다시 실행하면 빈 것만 요청한다.
robots.txt 는 /hdst/ 상세 페이지를 허용한다(2026-10 확인). 요청 사이에 --delay 초를 쉰다.
그 다음 python tools/build_catalog.py 를 다시 실행하면 stores.json 에 photoUrls 가 들어간다.
"""
import argparse, concurrent.futures as cf, csv, os, re, sys, time

import requests

UA = "Mozilla/5.0 (NopoCheonnyeon photo-url collector; contact: wow.sillyer@gmail.com)"
IMG = re.compile(r'<img[^>]+src="(https://www\.sbiz24\.or\.kr/api/cmmn/file/[0-9a-f-]+)"[^>]*class="autoplayImg"', re.I)


def photo_urls(page_url):
    r = requests.get(page_url, headers={"User-Agent": UA}, timeout=15)
    r.raise_for_status()
    urls = IMG.findall(r.text)
    # 옛 주소(sbiz24.or.kr)는 sbiz24.kr 로 넘어가므로 바로 최종 주소를 쓴다
    return [u.replace("www.sbiz24.or.kr", "www.sbiz24.kr") for u in dict.fromkeys(urls)]


def is_jpeg(url):
    try:
        r = requests.get(url, headers={"User-Agent": UA}, timeout=20, stream=True)
        ok = r.status_code == 200 and next(r.iter_content(2), b"") == bytes([0xFF, 0xD8])  # JPEG 시작 표시
        r.close()
        return ok
    except requests.RequestException:
        return False


def verify(cache_path):
    """모은 주소 중 실제로 사진이 내려오는 것만 남긴다 (깨진 링크 제거). 원본은 photo_cache_raw.csv 로 보관."""
    with open(cache_path, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    raw = cache_path.replace(".csv", "_raw.csv")
    if not os.path.exists(raw):
        os.replace(cache_path, raw)
        with open(raw, encoding="utf-8") as f:
            rows = list(csv.DictReader(f))
    urls = sorted({u for r in rows for u in r["photo_urls"].split("|") if u})
    with cf.ThreadPoolExecutor(6) as ex:
        good = {u for u, ok in zip(urls, ex.map(is_jpeg, urls)) if ok}
    with open(cache_path, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["store_id", "photo_urls"])
        w.writeheader()
        n = 0
        for r in rows:
            keep = [u for u in r["photo_urls"].split("|") if u in good]
            n += bool(keep)
            w.writerow({"store_id": r["store_id"], "photo_urls": "|".join(keep)})
    print(f"정상 사진 {len(good)}/{len(urls)}장, 사진 있는 가게 {n}곳 -> {cache_path}")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--delay", type=float, default=0.3)
    ap.add_argument("--verify", action="store_true", help="수집 대신 깨진 링크 제거")
    ap.add_argument("--limit", type=int, default=0, help="시험용: 앞에서 N곳만")
    a = ap.parse_args()
    cache_path = os.path.join(a.data, "photo_cache.csv")
    if a.verify:
        return verify(cache_path)
    done = set()
    if os.path.exists(cache_path):
        with open(cache_path, encoding="utf-8") as f:
            done = {r["store_id"] for r in csv.DictReader(f)}
    new = not os.path.exists(cache_path)
    ok = empty = err = n = 0
    with open(os.path.join(a.data, "nopo_stores.csv"), encoding="utf-8-sig") as f, \
            open(cache_path, "a", encoding="utf-8", newline="") as out:
        w = csv.DictWriter(out, fieldnames=["store_id", "photo_urls"])
        if new:
            w.writeheader()
        for row in csv.DictReader(f):
            sid, url = row["id"], row.get("source_url", "")
            if sid in done or "sbiz.or.kr/hdst" not in url:
                continue
            if a.limit and n >= a.limit:
                break
            n += 1
            try:
                urls = photo_urls(url)
            except Exception as e:
                err += 1
                print("error", sid, e)
                time.sleep(a.delay)
                continue
            w.writerow({"store_id": sid, "photo_urls": "|".join(urls)})
            out.flush()
            ok += bool(urls)
            empty += not urls
            time.sleep(a.delay)
    print(f"사진 있음 {ok}, 없음 {empty}, 오류 {err} -> {cache_path}")


if __name__ == "__main__":
    main()

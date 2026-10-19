#!/usr/bin/env python3
"""노포 카탈로그 좌표 보강 (주소 → 위도·경도). 개인 컴퓨터에서 실행.

네이버(NCP Maps Geocoding):  set NCP_KEY_ID=..., NCP_KEY=...   → --provider naver
카카오(Local 주소 검색):        set KAKAO_REST_KEY=...           → --provider kakao
결과는 data/geocode_cache.csv 에 쌓이고, 다시 실행하면 빈 것만 요청한다.
주소가 없는 가게(일부 오래가게)는 카카오 키워드 검색(시도·자치구 + 가게 이름)으로만 찾을 수 있다.
이렇게 찾은 가게는 카탈로그에 COORD_FROM_NAME 표시가 붙으니 지도에서 위치를 한 번 확인한다.
그 다음 python tools/build_catalog.py 를 다시 실행하면 stores.json 에 좌표가 들어간다.
"""
import argparse, csv, os, re, time

import requests

NAVER_URL = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode"
KAKAO_ADDR = "https://dapi.kakao.com/v2/local/search/address.json"
KAKAO_KEYWORD = "https://dapi.kakao.com/v2/local/search/keyword.json"


def clean(addr):
    addr = re.sub(r"\([^)]*\)", " ", addr or "")
    addr = re.sub(r"\s\d+층.*$", "", addr)
    addr = re.sub(r"[,]", " ", addr)
    return re.sub(r"\s+", " ", addr).strip()


def naver(q):
    r = requests.get(NAVER_URL, params={"query": q}, timeout=10, headers={
        "x-ncp-apigw-api-key-id": os.environ["NCP_KEY_ID"], "x-ncp-apigw-api-key": os.environ["NCP_KEY"],
        "Accept": "application/json"})
    r.raise_for_status()
    a = (r.json().get("addresses") or [])
    return (float(a[0]["y"]), float(a[0]["x"]), a[0].get("roadAddress", ""), "addr") if a else None


def kakao(q, name="", region=""):
    h = {"Authorization": "KakaoAK " + os.environ["KAKAO_REST_KEY"]}
    if q:
        r = requests.get(KAKAO_ADDR, params={"query": q, "size": 1}, headers=h, timeout=10)
        r.raise_for_status()
        d = r.json().get("documents") or []
        if d:
            return float(d[0]["y"]), float(d[0]["x"]), d[0].get("address_name", ""), "addr"
    if name:
        r = requests.get(KAKAO_KEYWORD, params={"query": f"{region} {name}".strip(), "size": 1}, headers=h, timeout=10)
        r.raise_for_status()
        d = r.json().get("documents") or []
        if d:
            road = d[0].get("road_address_name") or d[0].get("address_name", "")
            return float(d[0]["y"]), float(d[0]["x"]), road, "keyword"
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--provider", choices=["naver", "kakao"], default="naver")
    ap.add_argument("--delay", type=float, default=0.2)
    a = ap.parse_args()
    src = os.path.join(a.data, "nopo_stores.csv")
    cache_path = os.path.join(a.data, "geocode_cache.csv")
    done = {}
    if os.path.exists(cache_path):
        with open(cache_path, encoding="utf-8") as f:
            for row in csv.DictReader(f):
                done[row["store_id"]] = row
    new = not os.path.exists(cache_path)
    with open(src, encoding="utf-8-sig") as f, open(cache_path, "a", encoding="utf-8", newline="") as out:
        w = csv.DictWriter(out, fieldnames=["store_id", "lat", "lng", "provider", "query", "matched", "method"])
        if new:
            w.writeheader()
        ok = fail = 0
        for row in csv.DictReader(f):
            sid = row["id"]
            if sid in done:
                continue
            if not row.get("address") and a.provider != "kakao":
                continue   # 주소 없는 가게는 --provider kakao 의 이름 검색으로만 찾는다
            q = clean(row.get("address") or "")
            region = " ".join(x for x in (row.get("sido", ""), row.get("sigungu", "")) if x)
            try:
                res = naver(q) if a.provider == "naver" else kakao(q, row.get("name", ""), region)
            except Exception as e:
                print("error", sid, e)
                res = None
            if res:
                w.writerow({"store_id": sid, "lat": res[0], "lng": res[1], "provider": a.provider,
                            "query": q or f"{region} {row.get('name', '')}", "matched": res[2], "method": res[3]})
                ok += 1
            else:
                fail += 1
                print("no result:", sid, row.get("name"), q)
            out.flush()
            time.sleep(a.delay)
    print(f"geocoded {ok}, failed {fail} -> {cache_path}")


if __name__ == "__main__":
    main()

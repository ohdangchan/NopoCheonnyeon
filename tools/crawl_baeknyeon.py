#!/usr/bin/env python3
"""소상공인마당 백년가게 목록·상세 수집기 (개인 컴퓨터에서 실행).

- 목록:  https://www.sbiz.or.kr/hdst/main/ohndMarketListSmba.do?currentPage=N
- 상세:  https://www.sbiz.or.kr/hdst/main/ohndMarketDetailSmba.do?rcpnNo=<id>
- 요청 간격 기본 1초, robots.txt 에서 막힌 /mobile/ 경로는 쓰지 않는다.
- 받은 HTML 은 data/cache/sbiz/ 에 저장해 다시 실행하면 이어서 진행한다.
- 결과: data/raw/bn_list_local.jsonl, data/raw/bn_detail_local.jsonl (build_catalog.py 가 자동으로 합침)

설치: pip install requests beautifulsoup4
실행: python tools/crawl_baeknyeon.py              # 전체
      python tools/crawl_baeknyeon.py --only-missing  # 상세가 비어 있는 가게만
첫 실행 뒤 bn_detail_local.jsonl 5줄을 눈으로 확인하고, 값이 비면 PATTERNS 를 고친다.
"""
import argparse, glob, json, os, re, time

import requests
from bs4 import BeautifulSoup

BASE = "https://www.sbiz.or.kr"
LIST_URL = BASE + "/hdst/main/ohndMarketListSmba.do?currentPage={page}"
DETAIL_URL = BASE + "/hdst/main/ohndMarketDetailSmba.do?rcpnNo={rid}"
HEADERS = {"User-Agent": "Mozilla/5.0 (personal research; nopo prototype)"}
ID_RE = re.compile(r"fn_goPage\('([^']+)'\s*,\s*'rcpnNo'")
PAGES_RE = re.compile(r"(\d+)\s*/\s*(\d+)\s*페이지")
PATTERNS = {
    "founded": re.compile(r"창\s*업\s*일\s*[:：]?\s*(\d{4}[-.]\d{1,2}[-.]\d{1,2})"),
    "phone": re.compile(r"연\s*락\s*처\s*[:：]?\s*([0-9][0-9\-\s]{7,})"),
    "address": re.compile(r"주\s*소\s*[:：]?\s*([^\n]+)"),
    "category": re.compile(r"(?:주요\s*사업|업\s*종)\s*[:：]?\s*([^\n]+)"),
    "hours": re.compile(r"영업\s*시간\s*[:：]?\s*([^\n]+)"),
}
AMENITIES = ["주차장", "예약", "신용카드", "장애인 편의시설", "포장", "배달", "무선 인터넷", "놀이방"]


def fetch(session, url, cache_path, delay):
    if os.path.exists(cache_path):
        with open(cache_path, encoding="utf-8") as f:
            return f.read()
    for attempt in range(3):
        try:
            r = session.get(url, headers=HEADERS, timeout=20)
            if r.status_code == 200:
                r.encoding = r.apparent_encoding or "utf-8"
                with open(cache_path, "w", encoding="utf-8") as f:
                    f.write(r.text)
                time.sleep(delay)
                return r.text
        except requests.RequestException:
            pass
        time.sleep(delay * (attempt + 2))
    return ""


def parse_detail(html):
    soup = BeautifulSoup(html, "html.parser")
    for t in soup(["script", "style"]):
        t.decompose()
    text = soup.get_text("\n")
    text = re.sub(r"\n\s*\n+", "\n", text)
    out = {}
    for key, pat in PATTERNS.items():
        m = pat.search(text)
        out[key] = m.group(1).strip() if m else ""
    out["phone"] = re.sub(r"\D", "", out["phone"])
    out["amenities"] = [a for a in AMENITIES if a in text]
    links = [a.get("href", "") for a in soup.find_all("a")]
    out["homepage"] = next((h for h in links if h.startswith("http") and "sbiz.or.kr" not in h), "")
    # 소개글: '소개' 다음 가장 긴 문단
    paras = [p.get_text(" ", strip=True) for p in soup.find_all(["p", "div", "dd"])]
    paras = [p for p in paras if 40 <= len(p) <= 1500 and "Copyright" not in p]
    out["intro"] = max(paras, key=len) if paras else ""
    out["image_count"] = len([i for i in soup.find_all("img") if "upload" in (i.get("src") or "") or "file" in (i.get("src") or "").lower()])
    title = soup.find(["h3", "h2", "h4"])
    out["name"] = title.get_text(strip=True) if title else ""
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--delay", type=float, default=1.0)
    ap.add_argument("--only-missing", action="store_true")
    a = ap.parse_args()
    raw = os.path.join(a.data, "raw")
    cache = os.path.join(a.data, "cache", "sbiz")
    os.makedirs(raw, exist_ok=True)
    os.makedirs(cache, exist_ok=True)
    s = requests.Session()

    ids = []
    if a.only_missing:
        have = set()
        for p in glob.glob(os.path.join(raw, "bn_detail_*.jsonl")):
            for line in open(p, encoding="utf-8"):
                try:
                    d = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if d.get("founded"):
                    have.add(str(d.get("rcpn_no")))
        for p in glob.glob(os.path.join(raw, "bn_list_*.jsonl")):
            for line in open(p, encoding="utf-8"):
                try:
                    r = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if str(r.get("id")) not in have:
                    ids.append((str(r.get("id")), r.get("no"), r.get("list_page")))
    else:
        first = fetch(s, LIST_URL.format(page=1), os.path.join(cache, "list_1.html"), a.delay)
        m = PAGES_RE.search(BeautifulSoup(first, "html.parser").get_text(" "))
        total = int(m.group(2)) if m else 80
        with open(os.path.join(raw, "bn_list_local.jsonl"), "w", encoding="utf-8") as lf:
            for page in range(1, total + 1):
                html = first if page == 1 else fetch(s, LIST_URL.format(page=page), os.path.join(cache, f"list_{page}.html"), a.delay)
                page_ids = ID_RE.findall(html)
                if not page_ids:
                    break
                for rid in page_ids:
                    ids.append((rid, None, page))
                    lf.write(json.dumps({"id": rid, "list_page": page}, ensure_ascii=False) + "\n")
                print(f"list page {page}/{total}: {len(page_ids)}")

    out_path = os.path.join(raw, "bn_detail_local.jsonl")
    done = set()
    if os.path.exists(out_path):
        for line in open(out_path, encoding="utf-8"):
            try:
                done.add(json.loads(line)["rcpn_no"])
            except Exception:
                pass
    with open(out_path, "a", encoding="utf-8") as f:
        for i, (rid, no, page) in enumerate(ids, 1):
            if rid in done:
                continue
            url = DETAIL_URL.format(rid=rid)
            html = fetch(s, url, os.path.join(cache, f"detail_{rid}.html"), a.delay)
            rec = {"rcpn_no": rid, "list_no": no, "list_page": page, "detail_url": url, "error": ""}
            if html:
                rec.update(parse_detail(html))
            else:
                rec["error"] = "fetch failed"
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
            f.flush()
            if i % 20 == 0:
                print(f"detail {i}/{len(ids)}")
    print("done:", out_path)


if __name__ == "__main__":
    main()

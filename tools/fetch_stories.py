#!/usr/bin/env python3
"""백년가게 상세 페이지의 '소개글'(가게 이야기) 원문 수집 (소상공인마당 /hdst/ 상세 페이지).

각 가게의 source_url(상세 페이지)에서 '소개글' 아래 본문을 그대로 뽑아 data/raw/bn_story.jsonl 에 가게당 한 줄로 쌓는다.
다시 실행하면 이미 받은 가게는 건너뛴다. robots.txt 는 /hdst/ 상세 페이지를 허용한다(2026-10 확인, /mobile/ 만 금지).
요청 사이에 --delay 초를 쉰다. 그 다음 python tools/build_catalog.py 를 다시 실행하면 CSV·stores.json 에 story 가 들어간다.

    python tools/fetch_stories.py            # 전체 (약 6~8분)
    python tools/fetch_stories.py --limit 20 # 시험
출처 표기: 앱 가게 상세에 출처(소상공인마당 백년가게) 링크를 함께 보여 준다. 공개 배포 전에 이용 조건을 확인할 것.
"""
import argparse, csv, json, os, re, sys, time, html

import requests

UA = "Mozilla/5.0 (NopoCheonnyeon story collector; contact: nopo-app)"


def extract_story(page_html):
    """'소개글' 라벨 다음부터 '목록' 버튼 전까지의 글. 문단은 빈 줄로 나눈다."""
    t = re.sub(r"(?is)<(script|style).*?</\1>", "", page_html)
    t = re.sub(r"(?i)<br\s*/?>", "\n", t)
    t = re.sub(r"(?i)</(p|div|li|dd|dt|tr|h\d)>", "\n", t)
    t = re.sub(r"(?s)<[^>]+>", "", t)
    t = html.unescape(t).replace("\xa0", " ")
    lines = [ln.strip() for ln in t.split("\n")]
    # '소개글' 라벨 줄을 찾는다 (여러 번 나오면 마지막 것: 본문 직전)
    idx = [i for i, ln in enumerate(lines) if ln == "소개글"]
    if not idx:
        return ""
    body = []
    for ln in lines[idx[-1] + 1:]:
        if ln == "목록":
            break
        body.append(ln)
    # 연속된 빈 줄은 하나로
    text = "\n".join(body)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    ap.add_argument("--delay", type=float, default=0.3)
    ap.add_argument("--limit", type=int, default=0)
    a = ap.parse_args()
    out_path = os.path.join(a.data, "raw", "bn_story.jsonl")
    done = set()
    if os.path.exists(out_path):
        with open(out_path, encoding="utf-8") as f:
            for line in f:
                try:
                    done.add(json.loads(line)["id"])
                except Exception:
                    pass
    ok = empty = err = n = 0
    with open(os.path.join(a.data, "nopo_stores.csv"), encoding="utf-8-sig") as f, open(out_path, "a", encoding="utf-8") as out:
        for row in csv.DictReader(f):
            sid, url = row["id"], row.get("source_url", "")
            if sid in done or "sbiz.or.kr/hdst" not in url:
                continue
            if a.limit and n >= a.limit:
                break
            n += 1
            try:
                r = requests.get(url, headers={"User-Agent": UA}, timeout=20)
                r.raise_for_status()
                r.encoding = "utf-8"
                story = extract_story(r.text)
            except Exception as e:
                err += 1
                print("error", sid, e)
                time.sleep(a.delay)
                continue
            out.write(json.dumps({"id": sid, "story": story}, ensure_ascii=False) + "\n")
            out.flush()
            ok += bool(story)
            empty += not story
            if n % 100 == 0:
                print(f"{n}곳 진행 (글 있음 {ok}, 없음 {empty}, 오류 {err})", flush=True)
            time.sleep(a.delay)
    print(f"글 있음 {ok}, 없음 {empty}, 오류 {err} -> {out_path}")


if __name__ == "__main__":
    main()

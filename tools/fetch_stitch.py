"""Stitch 프로젝트의 모든 화면(HTML·스크린샷)을 design/stitch/ 로 내려받아 기능별로 정리한다.

사용법 (프로젝트 루트, PowerShell):
    $env:STITCH_API_KEY="..."; python tools/fetch_stitch.py
옵션:
    --project 3905731688785616669   Stitch 프로젝트 ID
    --out design/stitch              출력 폴더 (기존 내용은 지우고 다시 만든다)

화면 → 기능 매핑은 tools/stitch_screen_map.json (화면 ID 앞 8자리 → [feature, slug, 스토리, 우선순위]).
매핑에 없는 MOBILE 화면은 screens/_unmapped/ 로, DESKTOP·문서 항목은 reference/ 로 간다.
키는 환경 변수로만 받고 파일·로그에 남기지 않는다.
"""
import argparse
import json
import os
import re
import shutil
import sys
import urllib.request

ENDPOINT = "https://stitch.googleapis.com/mcp"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def call(key, name, args):
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": "tools/call",
                       "params": {"name": name, "arguments": args}}).encode()
    req = urllib.request.Request(ENDPOINT, data=body, headers={
        "X-Goog-Api-Key": key,
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    })
    with urllib.request.urlopen(req, timeout=120) as r:
        res = json.load(r)
    if "error" in res:
        sys.exit(f"Stitch 오류: {res['error'].get('message')}")
    return json.loads(res["result"]["content"][0]["text"])


def safe(s):
    return re.sub(r'[\\/:*?"<>|]+', "_", s).strip()[:60]


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--project", default="3905731688785616669")
    ap.add_argument("--out", default=os.path.join(ROOT, "design", "stitch"))
    a = ap.parse_args()
    key = os.environ.get("STITCH_API_KEY")
    if not key:
        sys.exit("환경 변수 STITCH_API_KEY 가 없습니다.")

    mapping = json.load(open(os.path.join(ROOT, "tools", "stitch_screen_map.json"), encoding="utf-8"))
    screens = call(key, "list_screens", {"projectId": a.project})["screens"]

    # 내려받는 폴더만 지운다. 직접 쓴 문서(PRD-차이점.md 등)는 남긴다
    for sub in ("screens", "reference"):
        if os.path.isdir(os.path.join(a.out, sub)):
            shutil.rmtree(os.path.join(a.out, sub))
    os.makedirs(a.out, exist_ok=True)

    rows, refs = [], []
    for s in screens:
        sid = s["name"].split("/")[-1]
        title = s.get("title") or sid
        device = s.get("deviceType")
        m = mapping.get(sid[:8])
        if m:
            feature, slug, story, prio = m
            rel = os.path.join("screens", feature, slug)
        elif device == "MOBILE":
            feature, slug, story, prio = "_unmapped", safe(title), "-", "미분류"
            rel = os.path.join("screens", "_unmapped", f"{sid[:8]}_{slug}")
        else:
            rel = os.path.join("reference", f"{sid[:8]}_{safe(title)}")
        d = os.path.join(a.out, rel)
        os.makedirs(d, exist_ok=True)

        html = s.get("htmlCode") or {}
        if html.get("downloadUrl"):
            ext = "md" if "markdown" in html.get("mimeType", "") else "html"
            urllib.request.urlretrieve(html["downloadUrl"], os.path.join(d, "code." + ext))
        shot = s.get("screenshot") or {}
        if shot.get("downloadUrl"):
            urllib.request.urlretrieve(shot["downloadUrl"] + "=w780", os.path.join(d, "screen.png"))
        json.dump({"id": sid, "title": title, "device": device, "width": s.get("width"),
                   "height": s.get("height")}, open(os.path.join(d, "meta.json"), "w", encoding="utf-8"),
                  ensure_ascii=False, indent=1)

        path = rel.replace(os.sep, "/")
        if rel.startswith("screens"):
            rows.append((feature, slug, title, story, prio, path))
        else:
            refs.append((title, path))
        print(f"{device or '-':8} {path}")

    rows.sort()
    lines = [
        "# Stitch 디자인 원본",
        "",
        f"Stitch 프로젝트 `{a.project}`에서 `tools/fetch_stitch.py`로 받은 원본. 직접 고치지 말고 다시 받는다.",
        "",
        "> ⚠️ **기준은 PRD다. 화면을 만들기 전에 [`PRD-차이점.md`](PRD-차이점.md)에서 해당 화면을 먼저 확인한다.**",
        "",
        "- 색·글꼴·간격의 기준은 `reference/*DESIGN.md*`와 PRD 10.1이다. 화면 HTML 안의 Tailwind 색(Stitch 자동 생성 M3 팔레트)은 레이아웃 참고용이며 토큰으로 옮기지 않는다.",
        "- 각 폴더: `screen.png`(스크린샷), `code.html`(Stitch HTML), `meta.json`.",
        "- 우선순위 `PRD 외`·`확인 필요`는 Stitch에서 추가로 그린 화면이라 PRD·스토리에 없거나 범위가 불분명하다. 구현 전에 범위를 정한다.",
        "",
        "## 앱 화면",
        "",
        "| 기능 패키지 | 화면 | 제목 | 스토리 | 우선순위 | 폴더 |",
        "|---|---|---|---|---|---|",
    ]
    lines += [f"| `feature/{f}` | `{sl}` | {t} | {st} | {p} | [{pa}]({pa}/screen.png) |" for f, sl, t, st, p, pa in rows]
    lines += ["", "## 참고 문서 (데스크톱 가이드·문서 사본)", "",
              "> ⚠️ 검증된 적 없는 '완료·통과' 주장과 존재하지 않는 파일을 전제로 한 문서들이다. 스크립트를 실행하지 않는다. 자세한 내용은 `PRD-차이점.md` 6장.",
              "> `76404327_노포천년미식가_기획안_v0.4.html`은 구버전이다 (최신: `docs/nopo-planning.html`).", ""]
    lines += [f"- [{t}]({pa})" for t, pa in sorted(refs)]
    open(os.path.join(a.out, "README.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print(f"\n화면 {len(rows)}개, 참고 {len(refs)}개 → {a.out}")


if __name__ == "__main__":
    main()

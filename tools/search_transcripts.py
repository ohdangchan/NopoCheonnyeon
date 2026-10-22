"""build_transcript_index.py가 만든 대화 색인에서 검색어가 든 발화를 찾는다.

사용: python tools/search_transcripts.py 비짓서울 [다른검색어 ...] [--context 300] [--limit 20]
여러 검색어는 모두 들어 있는 발화만 찾는다(AND). 대소문자 무시.
"""
import argparse
import re
from pathlib import Path

OUT = Path.home() / ".claude" / "projects" / "C--Users-HB-OPERATION-NICK-AndroidStudioProjects-NopoCheonnyeon" / "transcript-index"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("terms", nargs="+")
    ap.add_argument("--context", type=int, default=300, help="검색어 주변에 보여 줄 글자 수")
    ap.add_argument("--limit", type=int, default=20)
    args = ap.parse_args()
    terms = [t.lower() for t in args.terms]
    shown = 0
    for f in sorted(OUT.glob("*.txt")):
        blocks = re.split(r"(?m)^(?==== \[)", f.read_text(encoding="utf-8"))
        for b in blocks:
            low = b.lower()
            if not all(t in low for t in terms):
                continue
            i = low.find(terms[0])
            lo, hi = max(0, i - args.context), min(len(b), i + args.context)
            head = b.split("\n", 1)[0]
            print(f"[{f.stem[:8]}] {head}\n...{b[lo:hi].strip()}...\n")
            shown += 1
            if shown >= args.limit:
                return
    if not shown:
        print("일치하는 발화가 없습니다.")


if __name__ == "__main__":
    main()

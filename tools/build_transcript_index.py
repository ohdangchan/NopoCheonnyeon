"""이전 Claude Code 대화(.jsonl)에서 사용자·어시스턴트 글만 뽑아 검색용 텍스트로 만든다.

- 도구 출력, 이미지, 시스템 알림은 뺀다.
- 키·토큰·인증 코드처럼 보이는 문자열은 [REDACTED]로 가린다.
- 결과는 저장소 밖(~/.claude/projects/<프로젝트>/transcript-index/)에 둔다. 저장소에 올리지 말 것.

사용: python tools/build_transcript_index.py
검색: python tools/search_transcripts.py 검색어 [검색어 ...]
"""
import json
import os
import re
import sys
from pathlib import Path

PROJ = Path.home() / ".claude" / "projects" / "C--Users-HB-OPERATION-NICK-AndroidStudioProjects-NopoCheonnyeon"
OUT = PROJ / "transcript-index"

SECRET_PATTERNS = [
    re.compile(r"AIza[0-9A-Za-z_\-]{20,}"),
    re.compile(r"AQ\.[0-9A-Za-z_\-]{20,}"),
    re.compile(r"ncp_iam_[0-9A-Za-z_\-]{10,}"),
    re.compile(r"\b4/0A[0-9A-Za-z_\-]{30,}"),
    re.compile(r"(?i)(client\s*secret|secret|password|비밀번호|storePassword|keyPassword)\s*[:=]?\s*\S{8,}"),
    re.compile(r"\b[A-Za-z0-9_\-]{40,}\b"),
    re.compile(r"(?i)session=[0-9a-f\-]{20,}"),
    re.compile(r"code_challenge=[\w\-]+"),
    re.compile(r"attest=[\w\-]+"),
]


def redact(text: str) -> str:
    for p in SECRET_PATTERNS:
        text = p.sub("[REDACTED]", text)
    return text


def texts(message):
    c = message.get("content")
    if isinstance(c, str):
        yield c
    elif isinstance(c, list):
        for part in c:
            if isinstance(part, dict) and part.get("type") == "text":
                yield part.get("text", "")


def convert(src: Path, dst: Path):
    n = 0
    with src.open(encoding="utf-8", errors="ignore") as f, dst.open("w", encoding="utf-8") as o:
        for line in f:
            try:
                d = json.loads(line)
            except ValueError:
                continue
            role = d.get("type")
            if role not in ("user", "assistant") or d.get("isSidechain"):
                continue
            msg = d.get("message") or {}
            body = "\n".join(t for t in texts(msg) if t and t.strip())
            if not body.strip():
                continue
            if body.lstrip().startswith("<") and "system-reminder" in body[:60]:
                continue
            if "<task-notification>" in body[:40]:
                continue
            ts = (d.get("timestamp") or "")[:16].replace("T", " ")
            who = "사용자" if role == "user" else "Claude"
            o.write(f"=== [{ts}] {who}\n{redact(body.strip())}\n\n")
            n += 1
    return n


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for src in sorted(PROJ.glob("*.jsonl")):
        dst = OUT / (src.stem + ".txt")
        n = convert(src, dst)
        print(f"{src.name}: {n}개 발화 -> {dst.name} ({dst.stat().st_size // 1024}KB)")


if __name__ == "__main__":
    sys.exit(main())

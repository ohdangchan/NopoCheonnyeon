#!/usr/bin/env python3
"""tools/fetch_visitseoul.py 가 받은 비짓서울 사진을 stores.json 의 photoUrls 맨 앞에 넣는다.

- 비짓서울 사진은 개수 제한 없이 전부 맨 앞에, 그 뒤에 기존 사진(공식·Google·시연용)을 그대로 이어 붙인다.
- 여러 번 실행해도 같은 결과다(이미 들어 있는 비짓서울 사진은 빼고 다시 넣는다).
- 카탈로그 version 을 올려서 앱이 다음 실행 때 다시 적재하게 한다.
사용: python tools/apply_visitseoul.py
"""
import datetime, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PREFIX = "file:///android_asset/visitseoul/"


def main():
    index = json.loads((ASSETS / "visitseoul_photos.json").read_text(encoding="utf8"))
    path = ASSETS / "stores.json"
    cat = json.loads(path.read_text(encoding="utf8"))
    changed = added = 0
    for s in cat["stores"]:
        entry = index.get(s["id"])
        if not entry:
            continue
        vs = [PREFIX + f for f in entry["files"]]
        rest = [u for u in s.get("photoUrls", []) if not u.startswith(PREFIX)]
        new = vs + rest
        if new != s.get("photoUrls"):
            s["photoUrls"] = new
            changed += 1
        added += len(vs)
    cat["version"] = max(int(cat["version"]) + 1, int(datetime.datetime.now().strftime("%y%m%d%H")))
    cat["generatedAt"] = datetime.datetime.now().isoformat(timespec="seconds")
    path.write_text(json.dumps(cat, ensure_ascii=False), encoding="utf8")
    print(f"stores changed {changed}, visitseoul photos {added}, version {cat['version']}")


if __name__ == "__main__":
    sys.exit(main())

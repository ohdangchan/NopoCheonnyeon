"""새 화면 코드에 직접 적힌 한글 문구(따옴표 안에 $·역슬래시가 없는 것)를 res/values/strings_v2.xml 로 옮기고
코드는 stringResource(R.string.이름) 으로 바꾼다. 이미 옮긴 문구는 건너뛴다.
실행: python tools/extract_strings.py
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "java", "com", "nopo", "cheonnyeon", "feature")
XML = os.path.join(ROOT, "app", "src", "main", "res", "values", "strings_v2.xml")
FILES = {
    "home/HomeScreen.kt": "home", "explore/ExploreScreen.kt": "explore", "map/MapScreen.kt": "map",
    "passport/PassportScreen.kt": "col", "passport/PassportBook.kt": "book", "store/StoreDetailScreen.kt": "det",
    "my/MyScreen.kt": "my", "notifications/NotificationsScreen.kt": "notice",
    "verify/MockVerifyScreen.kt": "ver", "verify/GpsVerifyScreen.kt": "gps", "permission/PermissionScreens.kt": "perm",
    "auth/MockAuthScreens.kt": "auth2", "auth/OnboardingScreen.kt": "onb", "ai/NopoRouletteScreen.kt": "rou",
    "store/ReviewWriteScreen.kt": "rev", "help/HelpCenterScreen.kt": "help", "settings/MockSettingsScreen.kt": "set2",
}
CALLS = r"(?:MockText|GrayPill|LimePill|Pill|Text)"
PAT = re.compile(r"(" + CALLS + r')\("([^"$\\\n]*[가-힣][^"$\\\n]*)"')


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace('"', '\\"')


def main():
    entries = {}
    if os.path.exists(XML):
        for m in re.finditer(r'<string name="([^"]+)">(.*?)</string>', open(XML, encoding="utf-8").read()):
            entries[m.group(1)] = m.group(2)
    by_text = {v: k for k, v in entries.items()}
    counters = {}
    for rel, prefix in FILES.items():
        path = os.path.join(SRC, rel)
        s = open(path, encoding="utf-8").read()
        changed = [0]

        def sub(m):
            text = esc(m.group(2))
            name = by_text.get(text)
            if name is None:
                counters[prefix] = counters.get(prefix, 0) + 1
                name = f"{prefix}_{counters[prefix]:02d}"
                while name in entries:
                    counters[prefix] += 1
                    name = f"{prefix}_{counters[prefix]:02d}"
                entries[name] = text
                by_text[text] = name
            changed[0] += 1
            return f"{m.group(1)}(androidx.compose.ui.res.stringResource(com.nopo.cheonnyeon.R.string.{name})"

        new = PAT.sub(sub, s)
        if changed[0]:
            open(path, "w", encoding="utf-8").write(new)
            print(rel, changed[0])
    with open(XML, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<!-- 시안 복제 화면(v2)의 문구. 코드의 직접 문구를 tools/extract_strings.py 로 옮긴 것 -->\n<resources>\n')
        for k in sorted(entries):
            f.write(f'    <string name="{k}">{entries[k]}</string>\n')
        f.write("</resources>\n")


main()

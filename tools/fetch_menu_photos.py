"""대표 메뉴 사진: 위키미디어 공용(Wikimedia Commons)의 자유 이용 허락 사진을 받아 app/src/main/assets/menu 에 둔다.
허락(CC0 / CC BY / CC BY-SA)과 작성자를 credits.json 에 남기고, 앱의 오픈소스·출처 화면에서 보여 준다.
실행: python tools/fetch_menu_photos.py   (요청 사이 1초 간격, 사진당 한 번만 받는다)
"""
import io
import json
import os
import re
import time
import urllib.parse
import urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "menu")
UA = "NopoPassportDev/1.0 (wow.sillyer@gmail.com)"
API = "https://commons.wikimedia.org/w/api.php"

# 이름(앞 열쇠) → 공용 파일 이름
PHOTOS = {
    "gukbap": "Gugil Ttaro gukbap.jpg",
    "naengmyeon": "Mul-naengmyeon 3.jpg",
    "gogi": "Korean.cuisine-Samgyeopsal-01.jpg",
    "bulgogi": "Bulgogi 1.jpg",
    "tteokbokki": "Korean.snacks-Tteokbokki-08.jpg",
    "jjigae": "Kimchi jjigae - Kogi, Brighton 2023-11-15.jpg",
    "seolleongtang": "Seolleongtang.jpg",
    "jajang": "Jajangmyeon - Kogi Korean cuisine, Brighton 2024-03-12.jpg",
    "bibimbap": "Bibimbap 6.jpg",
    "gimbap": "Gimbap (pixabay).jpg",
    "kalguksu": "Korean.noodle-Kalguksu-01.jpg",
}


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def main():
    os.makedirs(OUT, exist_ok=True)
    credits = {}
    for key, fname in PHOTOS.items():
        q = urllib.parse.urlencode({
            "action": "query", "titles": "File:" + fname, "prop": "imageinfo",
            "iiprop": "url|extmetadata", "iiurlwidth": 640, "format": "json",
        })
        info = json.loads(get(API + "?" + q))
        page = next(iter(info["query"]["pages"].values()))
        ii = page["imageinfo"][0]
        meta = ii["extmetadata"]
        lic = meta.get("LicenseShortName", {}).get("value", "")
        artist = re.sub(r"<[^>]+>", "", meta.get("Artist", {}).get("value", "")).strip()
        time.sleep(1)
        img = Image.open(io.BytesIO(get(ii["thumburl"]))).convert("RGB")
        img.save(os.path.join(OUT, key + ".webp"), "WEBP", quality=78, method=6)
        credits[key] = {"title": fname, "license": lic, "artist": artist, "page": ii["descriptionurl"]}
        print(key, lic, artist[:40], img.size)
        time.sleep(1)
    with open(os.path.join(OUT, "credits.json"), "w", encoding="utf-8") as f:
        json.dump(credits, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()

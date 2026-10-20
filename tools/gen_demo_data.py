#!/usr/bin/env python3
"""시연용 예시 데이터 생성 (해커톤 시연 전용, 실제 사용자 데이터가 아니다).

- 서울 가게 전부: 예시 리뷰 3개씩, 서울 송파구 가게: 예시 리뷰 10개 + 예시 사진 3장
- 결과: app/src/main/assets/demo_reviews.json, app/src/main/assets/demo/<가게ID>_<1~3>.jpg, data/demo_photos.json
  (build_catalog.py 가 demo_photos.json 을 읽어 stores.json 의 photoUrls 에 넣는다)
- 같은 입력이면 같은 결과가 나오도록 가게 ID로 난수를 고정한다. 리뷰 글은 가게의 메뉴·업력·지역에서 만든 문장이다.
- 앱은 설정 > 개발자 도구의 "시연용 예시 데이터" 스위치로 켜고 끌 수 있고, 예시 리뷰·사진에는 "예시" 표시가 붙는다.

    python tools/gen_demo_data.py
"""
import hashlib, json, os, random, sys, textwrap

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORES = os.path.join(ROOT, "app", "src", "main", "assets", "stores.json")
OUT_REVIEWS = os.path.join(ROOT, "app", "src", "main", "assets", "demo_reviews.json")
OUT_IMG = os.path.join(ROOT, "app", "src", "main", "assets", "demo")
OUT_PHOTOS = os.path.join(ROOT, "data", "demo_photos.json")
FONT_TITLE = os.path.join(ROOT, "app", "src", "main", "res", "font", "black_han_sans.ttf")
FONT_BODY = r"C:\Windows\Fonts\NanumGothicBold.ttf"
FONT_LIGHT = r"C:\Windows\Fonts\NanumGothic.ttf"

NICKS = ["국밥장인", "골목탐험가", "을지로식객", "맛집수집가", "노포순례자", "동네어른", "미식가김씨", "새벽식객", "면치기왕", "밥심최고",
         "백년지기", "맛골목", "단골손님", "먹보하루", "별미사냥꾼", "가성비왕", "혼밥러", "퇴근식객", "주말탐방", "서울촌놈",
         "한상차림", "푸근한밥상", "찐맛집", "골목대장", "노포덕후", "숨은맛", "옛맛그대로", "시장통식객", "정직한맛", "할매손맛",
         "송파토박이", "잠실식객", "맛있는하루", "고수식객", "느긋한식사", "배부른곰", "한입만", "속풀이", "든든한점심", "저녁의낭만"]
TITLES = ["SPROUT", "SPROUT", "REGULAR", "REGULAR", "REGULAR", "ELDER", "ELDER", "ARTISAN", "MASTER"]

FOOD_POS = [
    "{menu_ga} 정말 맛있어요. {years}년을 이어 온 맛이 느껴져요.",
    "{name_eun} 기대 이상이었어요. 특히 {menu_eun} 꼭 드셔 보세요.",
    "{region}에서 오래 사랑받은 이유가 있네요. {menu} 추천합니다.",
    "{menu} 한 입 먹자마자 왜 노포인지 알겠더라고요. 또 올게요.",
    "양도 넉넉하고 간이 딱 좋아요. {menu_ga} 제일 좋았어요.",
    "가게 분위기가 옛날 그대로라 더 맛있게 느껴졌어요. {menu} 좋아요.",
    "점심시간에 갔는데 {menu} 덕분에 든든하게 먹었어요. 직원분들도 친절해요.",
    "부모님 모시고 갔는데 {menu_eul} 좋아하셨어요. 다음에 또 모시고 올게요.",
    "{years}년 된 가게답게 맛이 변함없어요. {menu} 강추!",
    "국물도 반찬도 정갈해요. {menu} 먹으러 일부러 찾아갈 만해요.",
    "처음 와 봤는데 {menu_ga} 인상 깊었어요. 재방문 의사 100%입니다.",
    "{menu}도 좋았고 가격도 부담 없어서 만족했어요.",
]
FOOD_MID = [
    "맛은 무난해요. 대표 메뉴인 {menu_eun} 괜찮았고 웨이팅이 조금 있었어요.",
    "{menu_eun} 좋았는데 피크 시간에는 많이 붐벼요. 한가한 때를 추천해요.",
    "전통의 맛은 느껴지지만 호불호는 있을 것 같아요. {menu_eun} 나쁘지 않았어요.",
    "분위기는 옛날 느낌 그대로예요. 맛은 평범했지만 {years}년의 역사가 인상 깊어요.",
]
FOOD_NEG = [
    "기대가 커서 그런지 조금 아쉬웠어요. 그래도 {years}년 노포라는 점은 인정해요.",
    "사람이 많아 오래 기다렸어요. 맛은 괜찮았습니다.",
]
SHOP_POS = [
    "{name_eun} {years}년 넘게 한자리를 지켜 온 곳이라 믿음이 가요. {menu} 잘 샀어요.",
    "{menu_eul} 샀는데 품질이 좋아요. 사장님이 친절하게 설명해 주셨어요.",
    "오래된 가게의 내공이 느껴져요. {region}에 오면 또 들를 거예요.",
    "{menu} 때문에 찾아갔는데 만족스러웠어요. 가격도 합리적이에요.",
    "정직하게 장사하시는 게 느껴져요. {menu} 추천드려요.",
    "선물용으로 {menu_eul} 샀는데 받는 분이 좋아하셨어요.",
]
SHOP_MID = [
    "{menu_eun} 무난했어요. 가게 분위기가 옛날 그대로라 구경하는 재미가 있었어요.",
    "{years}년의 세월이 느껴지는 가게예요. 물건은 평범했지만 정감 있어요.",
]
SHOP_NEG = ["기대했던 것보다는 평범했어요. 그래도 오래된 가게라는 점은 의미 있어요."]
SHORT_POS = ["맛있어요!", "또 갈래요.", "역시 노포.", "추천합니다.", "든든했어요.", "정말 좋아요.", "분위기 최고."]


def jong(word):
    c = word.rstrip()[-1:]
    return 0xAC00 <= ord(c) <= 0xD7A3 and (ord(c) - 0xAC00) % 28 != 0


def josa(word, with_jong, without):
    return word + (with_jong if jong(word) else without)


def rng_for(store_id, salt=""):
    return random.Random(int(hashlib.sha1((store_id + salt).encode()).hexdigest()[:12], 16))


def pick_rating(r):
    x = r.random()
    return 5 if x < 0.48 else 4 if x < 0.82 else 3 if x < 0.95 else 2


def build_reviews(store, count):
    r = rng_for(store["id"], "rev")
    years = max(30, 2026 - store["foundedYear"]) if store.get("foundedYear") else 30
    menus = store.get("menuItems") or []
    name = store["name"]
    for w in ("주식회사 ", "(주)", "㈜"):
        name = name.replace(w, "")
    region = store.get("sigungu") or store.get("sido") or "서울"
    food = store.get("categoryGroup") == "음식점"
    nicks = r.sample(NICKS, min(count, len(NICKS)))
    out = []
    used = set()
    for i in range(count):
        rating = pick_rating(r)
        pool = (FOOD_POS if rating >= 4 else FOOD_MID if rating == 3 else FOOD_NEG) if food else (SHOP_POS if rating >= 4 else SHOP_MID if rating == 3 else SHOP_NEG)
        menu = r.choice(menus) if menus else ("대표 메뉴" if food else "물건")
        text = ""
        if r.random() > 0.1:
            for _ in range(8):
                t = r.choice(pool).format(
                    menu=menu, years=years, name=name, region=region, menu_ga=josa(menu, "이", "가"), menu_eun=josa(menu, "은", "는"),
                    menu_eul=josa(menu, "을", "를"), name_eun=josa(name, "은", "는"),
                )
                if t not in used:
                    used.add(t)
                    text = t
                    break
            if not text:
                text = r.choice(SHORT_POS)
        out.append({
            "s": store["id"], "n": nicks[i % len(nicks)], "t": r.choice(TITLES), "r": rating, "x": text,
            "v": 1 if r.random() < 0.7 else r.randint(2, 4), "d": r.randint(1, 120),
        })
    out.sort(key=lambda x: x["d"])
    return out


# ---------- 예시 사진 ----------
W, H = 1200, 900
TEAL, TEAL_D, STAMP, BRASS, PAPER, SURF, INK = (31, 107, 99), (23, 80, 74), (194, 59, 42), (217, 162, 44), (237, 240, 234), (248, 249, 245), (20, 32, 30)


def font(path, size):
    return ImageFont.truetype(path, size)


def center(d, text, y, f, fill, w=W):
    box = d.textbbox((0, 0), text, font=f)
    d.text(((w - (box[2] - box[0])) / 2 - box[0], y), text, font=f, fill=fill)


def fit_font(path, text, max_w, start, minimum=40):
    size = start
    while size > minimum:
        f = font(path, size)
        b = ImageDraw.Draw(Image.new("RGB", (1, 1))).textbbox((0, 0), text, font=f)
        if b[2] - b[0] <= max_w:
            return f
        size -= 4
    return font(path, minimum)


def watermark(d):
    d.text((24, H - 44), "시연용 예시 이미지 · 실제 가게 사진이 아닙니다", font=font(FONT_LIGHT, 24), fill=(120, 130, 126))


def photo_sign(store, years, seed):
    """① 간판: 청록 판에 가게 이름"""
    img = Image.new("RGB", (W, H), TEAL_D)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((60, 60, W - 60, H - 60), 36, outline=BRASS, width=8)
    d.rounded_rectangle((110, 190, W - 110, H - 190), 28, fill=SURF)
    name = store["name"]
    for w in ("주식회사 ", "(주)", "㈜"):
        name = name.replace(w, "")
    f = fit_font(FONT_TITLE, name, W - 300, 150)
    center(d, name, 300, f, TEAL_D)
    center(d, f"{store['foundedYear']}년 개업 · {years}년째" if store.get("foundedYear") else f"{years}년 이상 이어 온 노포", 520, font(FONT_BODY, 48), STAMP)
    center(d, " ".join(x for x in (store.get("sido"), store.get("sigungu")) if x), 610, font(FONT_LIGHT, 40), INK)
    d.ellipse((W - 330, 90, W - 130, 290), outline=STAMP, width=8)
    d.text((W - 230, 190), "百年" if years >= 100 else "老鋪", font=font(FONT_TITLE, 70), fill=STAMP, anchor="mm")
    watermark(d)
    return img


def photo_menu(store, years, seed):
    """② 차림표: 종이 바탕에 대표 메뉴"""
    img = Image.new("RGB", (W, H), PAPER)
    d = ImageDraw.Draw(img)
    d.rectangle((50, 50, W - 50, H - 50), outline=TEAL, width=6)
    center(d, "차  림  표", 100, font(FONT_TITLE, 96), TEAL_D)
    d.line((250, 240, W - 250, 240), fill=BRASS, width=5)
    menus = (store.get("menuItems") or [])[:6]
    if not menus:
        menus = ["대표 메뉴", "오늘의 추천"] if store.get("categoryGroup") == "음식점" else ["대표 품목", "추천 품목"]
    y = 290
    for m in menus:
        f = fit_font(FONT_BODY, m, 620, 56, 30)
        d.text((220, y), m, font=f, fill=INK)
        d.text((W - 360, y + 6), "· · · · · ·", font=font(FONT_LIGHT, 40), fill=(150, 160, 156))
        y += 88
    center(d, store["name"].replace("주식회사 ", ""), H - 150, font(FONT_BODY, 40), STAMP)
    watermark(d)
    return img


def photo_story(store, years, seed):
    """③ 이야기: 인주 도장과 가게 소개 첫 문장"""
    r = random.Random(seed)
    img = Image.new("RGB", (W, H), (28, 52, 48))
    d = ImageDraw.Draw(img)
    d.ellipse((140, 150, 540, 550), outline=STAMP, width=14)
    d.ellipse((165, 175, 515, 525), outline=STAMP, width=4)
    center(d, "세월", 230, font(FONT_BODY, 44), STAMP, w=680)
    center(d, f"{years}년", 300, font(FONT_TITLE, 120), STAMP, w=680)
    story = (store.get("summary") or store.get("story") or f"{store['name']}은(는) 오랜 세월 한자리를 지켜 온 가게입니다.").split("\n")[0]
    lines = textwrap.wrap(story, 17)[:6]
    y = 150
    for ln in lines:
        d.text((620, y), ln, font=font(FONT_BODY, 40), fill=SURF)
        y += 66
    d.text((620, y + 30), "— " + store["name"].replace("주식회사 ", ""), font=font(FONT_TITLE, 44), fill=BRASS)
    addr = store.get("address") or ""
    if addr:
        d.text((90, H - 150), addr[:34], font=font(FONT_LIGHT, 34), fill=(190, 205, 200))
    watermark(d)
    return img


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    stores = json.load(open(STORES, encoding="utf-8"))["stores"]
    seoul = [s for s in stores if s.get("sido") == "서울"]
    reviews, photos = [], {}
    os.makedirs(OUT_IMG, exist_ok=True)
    for s in seoul:
        songpa = (s.get("sigungu") or "").startswith("송파")
        reviews += build_reviews(s, 10 if songpa else 3)
        if songpa:
            years = max(30, 2026 - s["foundedYear"]) if s.get("foundedYear") else 30
            urls = []
            for n, fn in enumerate((photo_sign, photo_menu, photo_story), 1):
                img = fn(s, years, int(hashlib.sha1(s["id"].encode()).hexdigest()[:8], 16) + n)
                name = f"{s['id']}_{n}.jpg"
                img.save(os.path.join(OUT_IMG, name), quality=82, optimize=True)
                urls.append("file:///android_asset/demo/" + name)
            photos[s["id"]] = urls
    with open(OUT_REVIEWS, "w", encoding="utf-8") as f:
        json.dump({"version": 1, "note": "시연용 예시 데이터(실제 사용자 리뷰 아님)", "reviews": reviews}, f, ensure_ascii=False, separators=(",", ":"))
    with open(OUT_PHOTOS, "w", encoding="utf-8") as f:
        json.dump(photos, f, ensure_ascii=False, indent=1)
    size = sum(os.path.getsize(os.path.join(OUT_IMG, x)) for x in os.listdir(OUT_IMG))
    print(f"서울 가게 {len(seoul)}곳, 리뷰 {len(reviews)}개, 송파구 {len(photos)}곳 사진 {len(photos) * 3}장({size // 1024}KB) -> {OUT_REVIEWS}")


if __name__ == "__main__":
    main()

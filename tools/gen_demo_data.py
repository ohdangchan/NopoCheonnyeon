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

ADJ = ["새벽", "골목", "든든한", "느긋한", "푸근한", "정직한", "옛날", "단골", "시장통", "숨은", "한입", "배부른", "퇴근길", "주말", "혼밥", "국물", "고소한", "얼큰한", "담백한", "넉넉한"]
NOUN = ["식객", "탐험가", "순례자", "미식가", "수집가", "대장", "장인", "손님", "여행자", "도깨비", "고수", "박사"]
MY_REVIEW_DAYS = (3, 9, 16, 24, 33, 41, 55, 70)
TITLE_STEPS = [(1000, "MASTER"), (900, "LEGEND"), (800, "SAGE"), (700, "KEEPER"), (600, "ARTISAN"), (500, "PIONEER"), (400, "CONNOISSEUR"),
               (300, "ELDER"), (200, "WANDERER"), (100, "REGULAR"), (0, "SPROUT")]
ME_TOTAL_YEARS = 637
USER_COUNT = 120


def title_for(years):
    return next(t for need, t in TITLE_STEPS if years >= need)


def make_users():
    r = random.Random(20261020)
    names = [a + n for a in ADJ for n in NOUN]
    r.shuffle(names)
    users = []
    for i in range(USER_COUNT):
        years = max(35, int(1480 * (0.955 ** i)) + r.randint(-9, 9))
        users.append({"n": names[i], "y": years, "c": max(1, round(years / r.uniform(38, 58)))})
    users.sort(key=lambda u: -u["y"])
    return users


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

# 리뷰를 가게당 10개씩 더 넣으면서 문장이 겹치지 않도록 더한 문장들
FOOD_POS += [
    "{menu_ga} 이 집의 자랑이라더니 정말이었어요. 국물까지 싹 비웠습니다.",
    "퇴근하고 들렀는데 {menu_eul} 시키자마자 나와서 좋았어요. 맛도 든든해요.",
    "{years}년 동안 사랑받은 데는 다 이유가 있어요. {menu_ga} 최고예요.",
    "친구가 추천해서 왔는데 {menu_eun} 기대 이상이었어요. 다음엔 가족과 올게요.",
    "{region}에서 한 끼 든든하게 먹고 싶을 때 여기가 정답이에요. {menu} 추천해요.",
    "사장님 손맛이 그대로예요. {menu} 먹고 나니 하루가 다 뿌듯했어요.",
    "혼자 와도 눈치 안 보이고 편해요. {menu_ga} 정말 괜찮았어요.",
    "{menu} 양이 푸짐해서 놀랐어요. 가격 대비 만족도가 높아요.",
    "비 오는 날 생각나서 갔는데 {menu_ga} 딱이었어요. 따뜻하게 잘 먹었어요.",
    "어릴 때 먹던 그 맛이에요. {menu_eul} 먹으니 추억이 떠올랐어요.",
    "간이 세지 않고 깔끔해요. {name_eun} 속이 편안한 노포예요.",
    "손님이 끊이지 않는 이유를 알겠어요. {menu_ga} 정말 맛있어요.",
    "외국인 친구를 데려갔는데 {menu_eul} 정말 좋아했어요. 한국의 맛이래요.",
    "직원분들이 친절하고 가게가 깨끗해요. {menu} 맛은 말할 것도 없고요.",
    "{menu} 하나만으로도 일부러 찾아올 가치가 있어요. 강력 추천합니다.",
    "주말에 가족끼리 갔는데 다들 만족했어요. {menu_ga} 특히 좋았어요.",
    "오랜만에 갔는데 변함없는 맛이라 반가웠어요. {menu} 여전히 맛있어요.",
    "{years}년의 내공이 느껴져요. {menu_eul} 한 숟갈 뜨는 순간 감탄했어요.",
    "점심 특선으로 먹었는데 {menu_ga} 훌륭했어요. 또 방문할게요.",
    "{name_eun} 동네 사람들이 다 아는 맛집이에요. {menu} 먹어 보면 알아요.",
    "반찬 하나하나가 정성스러워요. {menu_ga} 메인인데 곁들임도 최고예요.",
    "밤늦게까지 열어서 좋았어요. {menu_eul} 야식으로 먹었는데 든든했어요.",
    "{menu} 맛집으로 소문난 곳이라 갔는데 소문이 틀리지 않았어요.",
    "여행 중에 들렀는데 {region}에서 먹은 {menu_ga} 오래 기억에 남을 것 같아요.",
]
FOOD_MID += [
    "{menu_eun} 맛있었지만 자리가 좁아서 조금 불편했어요. 맛은 좋아요.",
    "전반적으로 괜찮았어요. 다만 주말 점심에는 줄이 길어서 각오하고 가세요.",
    "{menu_eun} 평범했지만 가격이 착해서 만족해요. 가성비 좋은 곳이에요.",
    "오래된 가게라 시설은 낡았어요. 그래도 {menu_eun} 정겨운 맛이에요.",
    "{menu} 맛은 좋은데 양이 조금 적게 느껴졌어요. 다른 메뉴도 먹어 볼게요.",
    "기대만큼은 아니었지만 나쁘지 않았어요. {years}년 역사가 인상적이에요.",
    "서비스가 빠르진 않았지만 {menu_eun} 맛있어서 용서됐어요.",
    "{menu_eun} 제 입맛에는 조금 짰어요. 그래도 단골이 많은 이유는 알겠어요.",
    "분위기는 정말 좋아요. 음식은 무난해서 다음엔 다른 메뉴를 시켜 보려고요.",
    "깔끔하진 않지만 정이 있어요. {menu_eun} 평범하게 맛있었어요.",
]
FOOD_NEG += [
    "웨이팅이 너무 길어서 지쳤어요. 맛은 괜찮았지만 다시 가기엔 고민돼요.",
    "{menu_eun} 제 취향은 아니었어요. 그래도 오래된 가게라는 점은 인정해요.",
    "주차가 불편하고 가게가 좁아요. 맛은 무난했어요.",
    "기대가 컸던 탓인지 평범했어요. 그래도 사장님은 친절하셨어요.",
    "가격이 조금 올라서 아쉬워요. 맛은 여전히 좋은 편이에요.",
]
SHOP_POS += [
    "{menu_eul} 구경하러 갔다가 필요한 것까지 샀어요. 오래된 가게의 정이 느껴져요.",
    "{years}년 된 가게라 믿고 샀어요. {menu} 만족합니다.",
    "사장님이 하나하나 알려 주셔서 좋았어요. {menu_eul} 잘 샀어요.",
    "{region}에 오면 꼭 들르는 곳이에요. {menu} 품질이 좋아요.",
    "가격이 정직하고 물건이 좋아요. {menu} 추천해요.",
    "오래 쓸 물건을 샀어요. {menu_ga} 튼튼하고 마음에 들어요.",
    "{name_eun} 믿고 맡길 수 있는 곳이에요. {menu} 만족스러웠어요.",
    "부모님 선물로 {menu_eul} 샀는데 아주 좋아하셨어요.",
    "동네에 이런 가게가 오래 남아 있어서 고마워요. {menu} 잘 쓰고 있어요.",
    "친절한 상담 덕분에 {menu_eul} 고르기 쉬웠어요. 또 갈게요.",
]
SHOP_MID += [
    "{menu_eun} 무난했어요. 가격은 합리적이고 설명은 조금 짧았어요.",
    "오래된 가게의 분위기가 좋아요. 물건은 기대보다는 평범했어요.",
    "{menu_eul} 샀어요. 나쁘지 않았지만 특별하진 않았어요.",
    "필요한 건 다 있었어요. 다만 매장이 좁아서 둘러보기엔 불편했어요.",
]
SHOP_NEG += [
    "원하던 물건이 없어서 아쉬웠어요. 그래도 사장님은 친절하셨어요.",
    "가격이 조금 비싼 편이에요. 품질은 괜찮았어요.",
    "오래된 가게 느낌은 좋은데 기대한 만큼은 아니었어요.",
]
SHORT_POS += ["가성비 최고!", "여기 맛집이에요.", "단골 될 듯해요.", "다음에 또 올게요.", "친절하고 맛있어요.", "믿고 먹는 곳.", "옛 맛 그대로.", "강추합니다!", "양 많고 맛있어요.", "역시 오래된 집은 달라요."]


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


def build_reviews(store, count, users):
    r = rng_for(store["id"], "rev")
    years = max(30, 2026 - store["foundedYear"]) if store.get("foundedYear") else 30
    menus = store.get("menuItems") or []
    name = store["name"]
    for w in ("주식회사 ", "(주)", "㈜"):
        name = name.replace(w, "")
    region = store.get("sigungu") or store.get("sido") or "서울"
    food = store.get("categoryGroup") == "음식점"
    picked = r.sample(users, count)
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
            "s": store["id"], "n": picked[i]["n"], "t": title_for(picked[i]["y"]), "r": rating, "x": text,
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


PLATE_BG = [(214, 196, 168), (60, 74, 70), (196, 170, 140), (88, 66, 52), (170, 188, 180)]


def photo_dish(store, menu, seed):
    """리뷰 사진 예시: 그릇과 김이 오르는 음식 그림 (실제 음식 사진이 아니다)"""
    r = random.Random(seed)
    w, h = 800, 600
    img = Image.new("RGB", (w, h), PLATE_BG[r.randrange(len(PLATE_BG))])
    d = ImageDraw.Draw(img)
    # 상 위의 나뭇결
    for y in range(0, h, 38):
        d.line((0, y, w, y + r.randint(-6, 6)), fill=tuple(max(0, c - 14) for c in img.getpixel((5, 5))), width=2)
    cx, cy = w // 2 + r.randint(-40, 40), h // 2 + 30
    d.ellipse((cx - 250, cy + 150, cx + 250, cy + 215), fill=(0, 0, 0))                        # 그림자
    d.ellipse((cx - 240, cy - 60, cx + 240, cy + 190), fill=(236, 230, 218), outline=(120, 108, 92), width=6)   # 그릇 바깥
    d.ellipse((cx - 205, cy - 50, cx + 205, cy + 60), fill=(r.choice([(200, 150, 90), (184, 120, 70), (222, 196, 140), (160, 70, 50)])))  # 국물·음식
    for _ in range(14):                                                                          # 고명
        x, y = cx + r.randint(-150, 150), cy + r.randint(-30, 30)
        d.ellipse((x - 14, y - 9, x + 14, y + 9), fill=r.choice([(86, 130, 70), (240, 232, 205), (150, 60, 40), (60, 50, 40)]))
    for k in (-1, 0, 1):                                                                         # 김
        x0 = cx + k * 70
        pts = [(x0 + 18 * (1 if i % 2 else -1), cy - 70 - i * 26) for i in range(6)]
        d.line(pts, fill=(250, 250, 246), width=7, joint="curve")
    d.ellipse((cx + 270, cy + 90, cx + 350, cy + 130), fill=(120, 90, 60))                      # 수저
    d.rounded_rectangle((cx + 310, cy + 105, cx + 430, cy + 118), 6, fill=(150, 150, 150))
    f = fit_font(FONT_BODY, menu, 520, 54, 28)
    d.rounded_rectangle((24, 24, 24 + int(d.textlength(menu, font=f)) + 40, 24 + 78), 20, fill=(24, 48, 44))
    d.text((44, 38), menu, font=f, fill=(250, 244, 230))
    d.text((24, h - 44), "시연용 예시 이미지 · 실제 음식 사진이 아닙니다", font=font(FONT_LIGHT, 22), fill=(40, 40, 40))
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


MY_TEXTS = [
    "{menu} 한 그릇에 {years}년의 세월이 담겨 있더라고요. 도장 받고 나오는 길이 뿌듯했어요.",
    "{name}에서 먹은 {menu}, 노포의 맛이 뭔지 알겠어요. 다음에 또 올게요.",
    "{region}에 오면 꼭 들르는 곳이에요. {menu} 추천합니다.",
    "{menu}은 변함없이 맛있었어요. 사장님 인심도 넉넉했어요.",
    "처음 와 봤는데 {menu} 덕분에 하루가 든든했어요.",
    "오래된 가게 특유의 분위기가 좋아요. {menu} 잘 먹었습니다.",
    "역시 {years}년 가게예요. {menu}에서 깊은 맛이 났어요.",
    "단골 하고 싶은 집이에요. {menu} 꼭 드셔 보세요.",
]


MY_PHOTO_TEXTS = [
    "{menu} 사진 남겨요. 국물이 진하고 {years}년 가게다운 깊은 맛이었어요.",
    "{name}에 또 왔어요. {menu} 정말 푸짐하게 나와서 사진부터 찍었어요.",
    "{menu} 비주얼 보세요. 맛도 비주얼만큼 훌륭했어요!",
    "오늘의 한 끼는 {menu}. 노포에서 먹으니 더 맛있어요.",
    "{region}에서 이 집 {menu} 꼭 드셔 보세요. 사진으로 다 담기지 않는 맛이에요.",
    "친구랑 {menu} 나눠 먹었어요. 양도 넉넉하고 정말 만족스러워요.",
    "{menu} 먹고 힘이 났어요. {years}년을 지켜 온 집은 역시 다르네요.",
    "사진 찍느라 식을 뻔했어요. {menu}는 따뜻할 때 먹어야 제맛!",
    "{name}의 {menu}, 제 취향 저격이에요. 또 올게요.",
    "{menu} 한 상 차림이 정갈해요. 사장님 솜씨가 그대로예요.",
]


def years_of(store):
    return max(30, 2026 - store["foundedYear"]) if store.get("foundedYear") else 30


def build_me(seoul):
    """내 패스포트 시연용: 서울 가게 중 세월 합이 정확히 637년이 되는 조합과 내 리뷰."""
    r = random.Random(637)
    pool = [s for s in seoul if s.get("foundedYear") and s.get("lat") is not None and s.get("categoryGroup") == "음식점"]
    songpa = [s for s in pool if (s.get("sigungu") or "").startswith("송파")]
    other = [s for s in pool if s not in songpa]
    for _ in range(200000):
        pick = r.sample(songpa, 3) + r.sample(other, r.randint(6, 9))
        if sum(years_of(s) for s in pick) == ME_TOTAL_YEARS:
            break
    else:
        raise SystemExit("637년 조합을 못 찾음")
    visits, mine = [], []
    days = sorted(r.sample(range(2, 400), len(pick)))
    for i, s in enumerate(pick):
        visits.append({"s": s["id"], "y": years_of(s), "d": days[i], "c": 1 if i % 3 else 2})
    for i, s in enumerate(pick[:8]):
        name = s["name"].replace("주식회사 ", "")
        menu = (s.get("menuItems") or ["대표 메뉴"])[0]
        mine.append({
            "s": s["id"], "n": name, "r": 5 if i % 3 else 4, "d": MY_REVIEW_DAYS[i],
            "x": MY_TEXTS[i].format(menu=menu, years=years_of(s), name=name, region=s.get("sigungu") or "서울"),
        })
    # 사진이 붙은 내 리뷰 10개(AI 룰렛이 이 사진들을 분석하는 것처럼 보여 주기 위함): data/demo_my_photos.json
    path = os.path.join(ROOT, "data", "demo_my_photos.json")
    photos = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    by_id = {s["id"]: s for s in pick}
    for i, (sid, files) in enumerate(photos.items()):
        s = by_id.get(sid)
        if not s or not files:
            continue
        name = s["name"].replace("주식회사 ", "")
        menus = s.get("menuItems") or ["대표 메뉴"]
        menu = menus[1] if len(menus) > 1 else menus[0]
        row = {
            "s": sid, "n": name, "r": 5 if i % 4 else 4, "d": 1 + i * 5,
            "x": MY_PHOTO_TEXTS[i % len(MY_PHOTO_TEXTS)].format(menu=menu, years=years_of(s), name=name, region=s.get("sigungu") or "서울"),
        }
        for key, f in zip(("p", "p2", "p3"), files):
            row[key] = f
        mine.append(row)
    return {"totalYears": ME_TOTAL_YEARS, "visits": visits, "reviews": mine}


def flat_photos(store_id):
    """받아 둔 사진 파일(자산 상대 경로)을 리뷰 순서대로 중복 없이 펼친다"""
    path = os.path.join(ROOT, "data", "demo_review_photos.json")
    rows = json.load(open(path, encoding="utf-8")).get(store_id, []) if os.path.exists(path) else []
    out = []
    for r in rows:
        for f in r:
            if f not in out:
                out.append(f)
    return out


def add_review_photos(store, batch, count=5):
    """송파구 가게(와 시험용 가게): 리뷰 앞쪽 [count]개에 사진을 붙인다 (리뷰 필드 "p","p2","p3" = 자산 경로, 1~3장).
    사진은 tools/fetch_demo_food_photos.py 가 받아 둔 무료 이용 음식 사진(data/demo_review_photos.json). 없으면 그림으로 대신한다."""
    manifest_path = os.path.join(ROOT, "data", "demo_review_photos.json")
    manifest = json.load(open(manifest_path, encoding="utf-8")) if os.path.exists(manifest_path) else {}
    rows = manifest.get(store["id"])
    menus = store.get("menuItems") or ["대표 메뉴"]
    for k, rv in enumerate(batch[:count]):
        if rows and k < len(rows):
            for key, path in zip(("p", "p2", "p3"), rows[k]):
                rv[key] = path
            continue
        menu = menus[k % len(menus)]
        name = f"R_{store['id']}_{k + 1}.jpg"
        photo_dish(store, menu, int(hashlib.sha1((store['id'] + str(k)).encode()).hexdigest()[:8], 16)).save(
            os.path.join(OUT_IMG, name), quality=72, optimize=True)
        rv["p"] = "demo/" + name


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    stores = json.load(open(STORES, encoding="utf-8"))["stores"]
    seoul = [s for s in stores if s.get("sido") == "서울"]
    users = make_users()
    reviews, photos = [], {}
    os.makedirs(OUT_IMG, exist_ok=True)
    for s in seoul:
        songpa = (s.get("sigungu") or "").startswith("송파")
        batch = build_reviews(s, 25 if songpa else 18, users)
        reviews += batch
        if songpa:
            add_review_photos(s, batch)
            years = years_of(s)
            urls = []
            food = flat_photos(s["id"])
            if food:      # 받아 둔 무료 음식 사진을 대표 사진으로도 쓴다
                urls = ["file:///android_asset/" + f for f in food[:3]]
            else:
                for n, fn in enumerate((photo_sign, photo_menu, photo_story), 1):
                    img = fn(s, years, int(hashlib.sha1(s["id"].encode()).hexdigest()[:8], 16) + n)
                    name = f"{s['id']}_{n}.jpg"
                    img.save(os.path.join(OUT_IMG, name), quality=82, optimize=True)
                    urls.append("file:///android_asset/demo/" + name)
            photos[s["id"]] = urls
    test_store = {"id": "TEST-NOPO", "name": "노포천년테스트식당", "foundedYear": 1980, "sido": "서울", "sigungu": "우리 동네", "address": "내 현재 위치",
                  "categoryGroup": "음식점", "menuItems": ["설렁탕", "수육", "공기밥"],
                  "summary": "1980년 문을 연 뒤 한 솥 국물로 사십 년 넘게 손님을 맞아 온 시연용 노포예요."}
    batch = build_reviews(test_store, 12, users)
    reviews += batch
    add_review_photos(test_store, batch)
    food = flat_photos("TEST-NOPO")
    for n, fn in enumerate((photo_sign, photo_menu, photo_story), 1):
        if len(food) >= n:
            Image.open(os.path.join(ROOT, "app", "src", "main", "assets", food[n - 1])).convert("RGB").save(os.path.join(OUT_IMG, f"TEST-NOPO_{n}.jpg"), quality=80, optimize=True)
        else:
            fn(test_store, 46, n).save(os.path.join(OUT_IMG, f"TEST-NOPO_{n}.jpg"), quality=82, optimize=True)
    me = build_me(seoul)
    # 명예의 전당의 탐방 수·리뷰 수는 실제 생성된 리뷰 수와 맞춘다
    counts = {}
    for rv in reviews:
        counts[rv["n"]] = counts.get(rv["n"], 0) + 1
    for u in users:
        u["rc"] = counts.get(u["n"], 0)
    with open(OUT_REVIEWS, "w", encoding="utf-8") as f:
        json.dump({"version": 2, "note": "시연용 예시 데이터(실제 사용자 리뷰 아님)", "users": users, "reviews": reviews, "me": me}, f, ensure_ascii=False, separators=(",", ":"))
    with open(OUT_PHOTOS, "w", encoding="utf-8") as f:
        json.dump(photos, f, ensure_ascii=False, indent=1)
    size = sum(os.path.getsize(os.path.join(OUT_IMG, x)) for x in os.listdir(OUT_IMG))
    print(f"서울 가게 {len(seoul)}곳, 리뷰 {len(reviews)}개, 사용자 {len(users)}명, 내 도장 {len(me['visits'])}개(세월 {sum(v['y'] for v in me['visits'])}년), 내 리뷰 {len(me['reviews'])}개, 송파구 {len(photos)}곳 사진 {len(photos) * 3}장({size // 1024}KB)")


if __name__ == "__main__":
    main()

"""디자인 시안(design/redesign/1~6.webp)에서 앱에 쓸 그림 조각을 잘라 res/drawable-nodpi 에 넣는다.

시안의 폰 화면 영역(760x1618)을 먼저 잘라 낸 뒤, 조각 좌표는 그 화면 기준이다.
정적인 장식(제목 글자, 우표, 여권 그림, 하단 탭 줄 등)만 잘라 쓰고, 가게 이름·거리·사진처럼 바뀌는 부분은 앱에서 그린다.
실행: python tools/crop_mockups.py
"""
import os
import sys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "design", "redesign")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
SCREEN = (90, 30, 850, 1648)  # 760 x 1618


def screen(n):
    return Image.open(os.path.join(SRC, f"{n}.webp")).convert("RGB").crop(SCREEN)


def erase(im, box, pad=3):
    """box 안을 가장자리 색을 섞어 지운다(바탕이 매끈한 곳 전용)."""
    x0, y0, x1, y1 = box
    a = np.asarray(im).astype(np.float32)
    h, w = y1 - y0, x1 - x0
    top = a[max(y0 - pad, 0):y0, x0:x1].mean(axis=0) if y0 - pad >= 0 else a[y0:y0 + 1, x0:x1].mean(axis=0)
    bot = a[y1:y1 + pad, x0:x1].mean(axis=0)
    left = a[y0:y1, max(x0 - pad, 0):x0].mean(axis=1)
    right = a[y0:y1, x1:x1 + pad].mean(axis=1)
    ty = np.linspace(0, 1, h)[:, None, None]
    tx = np.linspace(0, 1, w)[None, :, None]
    v = top[None, :, :] * (1 - ty) + bot[None, :, :] * ty
    hz = left[:, None, :] * (1 - tx) + right[:, None, :] * tx
    fill = (v + hz) / 2
    a[y0:y1, x0:x1] = fill
    return Image.fromarray(a.clip(0, 255).astype(np.uint8))


def save(im, name, box, quality=92):
    c = im.crop(box)
    path = os.path.join(OUT, f"mk_{name}.webp")
    c.save(path, "WEBP", quality=quality, method=6)
    print(f"{name:22s} {c.size[0]:4d}x{c.size[1]:<4d} {os.path.getsize(path)//1024:4d}KB")
    return c


def bg(im, box):
    a = np.asarray(im.crop(box)).reshape(-1, 3)
    return tuple(int(v) for v in np.median(a, axis=0))


def nav_strip(im, rx=0, ry=95):
    """하단 탭 줄: 둥근 화면 모서리(타원 rx x ry) 밖의 검은 부분은 바탕색으로 덮는다."""
    if not rx:
        return im
    color = bg(im, (300, 1522, 360, 1532))
    px = im.load()
    h = im.height
    for y in range(h - ry, h):
        for x in range(0, 66):
            if ((x - rx) / rx) ** 2 + ((y - (h - ry)) / ry) ** 2 > 1:
                px[x, y] = color
                px[im.width - 1 - x, y] = color
    return im


os.makedirs(OUT, exist_ok=True)
s = {n: screen(n) for n in range(1, 7)}

# --- 공통: 로고 + 제목 + 부제 (탐색·지도·컬렉션·마이 머리글) ---
save(s[4], "hdr_logo", (20, 84, 452, 206))
save(s[4], "ic_search", (604, 90, 658, 142))
save(s[4], "ic_bell", (682, 88, 736, 144))

# --- 하단 탭 줄(화면마다 선택된 탭 모양이 달라 그대로 쓴다) ---
for n, name in ((3, "home"), (4, "explore"), (5, "map"), (6, "collection"), (2, "my")):
    im = s[n].copy()
    nav_strip(im, rx=175)
    if n == 3:  # 시안 3은 옆 테두리와 홈 표시줄이 같이 찍혀 있어 지운다
        c = bg(im, (300, 1522, 360, 1532))
        px = im.load()
        for y in range(1520, im.height):
            for x in list(range(0, 20)) + list(range(736, 760)):
                px[x, y] = c
        im = erase(im, (258, 1594, 500, 1612), pad=2)
    save(im, f"nav_{name}", (0, 1520, 760, {3: 1604, 6: 1607}.get(n, 1618)))

# --- 홈 ---
save(s[3], "home_header", (0, 90, 760, 206))
save(s[3], "home_hero", (0, 205, 760, 578))
save(s[3], "home_chips", (20, 1298, 745, 1374))
banner = erase(s[3], (112, 1404, 400, 1486))
save(banner, "home_banner", (28, 1388, 734, 1500))
save(s[3], "home_title_hot", (24, 850, 300, 900))
save(s[3], "home_more", (630, 855, 740, 900))
save(s[3], "home_title_near", (24, 1240, 270, 1292))
save(s[3], "home_ic_target", (690, 1250, 735, 1292))
save(s[3], "home_ic_pin", (460, 1250, 495, 1290))

# --- 탐색 ---
save(s[4], "explore_title", (20, 225, 400, 336))
save(s[4], "explore_ic_search", (46, 364, 92, 402))
save(s[4], "explore_ic_filter", (664, 362, 726, 405))
save(s[4], "deco_explore", (430, 150, 760, 310))

# --- 지도 ---
save(s[5], "deco_map", (430, 140, 760, 280))
save(s[5], "map_ic_search", (44, 280, 80, 316))
save(s[5], "map_ic_filter", (668, 270, 736, 330))
save(s[5], "map_ic_locate", (660, 900, 740, 980))
save(s[5], "map_title_near", (18, 1108, 270, 1160))
save(s[5], "map_more", (650, 1112, 745, 1160))

# --- 컬렉션 ---
save(s[6], "col_title", (0, 232, 470, 345))
save(s[6], "col_note", (490, 290, 760, 410))
book = s[6].copy()
book = erase(book, (418, 458, 746, 884), pad=4)
save(book, "col_book", (0, 380, 760, 940))
save(s[6], "col_ic_plane", (480, 470, 540, 515))
save(s[6], "col_ic_seal", (645, 478, 692, 520))
save(s[6], "col_ic_flag", (30, 966, 82, 1020))
save(s[6], "col_title_visited", (14, 1110, 310, 1170))
save(s[6], "col_all", (605, 1118, 750, 1166))
save(s[6], "deco_collection", (430, 150, 760, 310))

# --- 마이 ---
save(s[2], "my_avatar", (28, 238, 204, 412))
save(s[2], "my_name", (222, 270, 470, 330))
save(s[2], "my_tagline", (226, 338, 474, 402))
save(s[2], "my_edit", (556, 348, 746, 410))
save(s[2], "deco_my", (430, 150, 760, 310))
jr = erase(s[2].copy(), (190, 515, 345, 605))
jr = erase(jr, (392, 515, 530, 605))
save(jr, "my_journey", (12, 436, 748, 642))
save(s[2], "my_card_saved", (12, 655, 374, 862))
save(s[2], "my_card_history", (385, 655, 748, 862))
save(s[2], "my_card_alarm", (12, 870, 374, 1078))
save(s[2], "my_card_prefs", (385, 870, 748, 1078))
save(s[2], "my_card_account", (12, 1086, 374, 1292))
save(s[2], "my_card_support", (385, 1086, 748, 1292))
save(s[2], "my_title_recent", (14, 1298, 260, 1348))
save(s[2], "my_all", (640, 1300, 748, 1346))

# --- 가게 상세 ---
card = erase(s[1].copy(), (186, 668, 520, 850))
save(card, "det_card", (14, 640, 746, 872))
save(s[1], "det_passport", (24, 650, 172, 838))
save(s[1], "det_title_menu", (14, 892, 160, 936))
save(s[1], "det_menu_all", (590, 900, 745, 930))
save(s[1], "det_title_intro", (14, 1240, 140, 1296))
save(s[1], "det_more", (660, 1250, 745, 1285))
save(s[1], "det_deco_intro", (430, 1270, 760, 1396))
info = erase(s[1].copy(), (28, 1405, 740, 1490))
save(s[1], "det_ic_clock", (36, 1414, 70, 1448))
save(s[1], "det_ic_pin", (270, 1416, 302, 1450))
save(s[1], "det_ic_phone", (552, 1416, 586, 1448))
save(s[1], "det_ic_save", (40, 1550, 78, 1590))
save(s[1], "det_ic_go", (222, 1550, 272, 1590))
save(s[1], "det_ic_stamp", (494, 1544, 540, 1596))
print("done")

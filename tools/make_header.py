"""머리글 그림을 새 동그란 로고로 바꾼다.
- mk_logo_round.webp : design/brand/app-icon-source.webp 에서 도장(동그란 로고)만 오려 낸 것
- mk_hdr_tab.webp    : 탭 머리글(mk_hdr_logo)의 왼쪽 네모 도장과 부제를 지우고 동그란 로고를 넣은 것
- mk_hdr_home.webp   : 홈 머리글(mk_home_header)의 오른쪽 v3.0 도장과 부제를 지우고 동그란 로고를 넣은 것
부제는 앱이 글자로 그린다(문구 변경이 쉽게).
실행: python tools/make_header.py
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
SRC = os.path.join(ROOT, "design", "brand", "app-icon-source.webp")
CIRCLE = (103, 94, 1150, 1134)


def erase(im, box, pad=6):
    """box 를 네 변 바깥의 색을 섞어 메운다. 그림 가장자리라 바깥이 없는 변은 반대쪽 변 색을 쓴다."""
    x0, y0, x1, y1 = box
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    H, W = a.shape[:2]
    h, w = y1 - y0, x1 - x0
    top = a[max(0, y0 - pad):y0, x0:x1].mean(axis=0) if y0 - pad >= 0 else None
    bot = a[y1:min(H, y1 + pad), x0:x1].mean(axis=0) if y1 < H else None
    left = a[y0:y1, max(0, x0 - pad):x0].mean(axis=1) if x0 - pad >= 0 else None
    right = a[y0:y1, x1:min(W, x1 + pad)].mean(axis=1) if x1 < W else None
    ty = np.linspace(0, 1, h)[:, None, None]
    tx = np.linspace(0, 1, w)[None, :, None]
    parts = []
    if top is not None or bot is not None:
        t_ = top if top is not None else bot
        b_ = bot if bot is not None else top
        parts.append(t_[None] * (1 - ty) + b_[None] * ty)
    if left is not None or right is not None:
        l_ = left if left is not None else right
        r_ = right if right is not None else left
        parts.append(l_[:, None] * (1 - tx) + r_[:, None] * tx)
    a[y0:y1, x0:x1] = sum(parts) / len(parts)
    return Image.fromarray(a.clip(0, 255).astype(np.uint8))


def flat(im, box, patch):
    """box 를 patch(깨끗한 바탕 한 곳)의 가운데 색으로 칠한다. 종이 바탕이라 한 색이면 티가 나지 않는다."""
    a = np.asarray(im.convert("RGB")).copy()
    px0, py0, px1, py1 = patch
    col = np.median(a[py0:py1, px0:px1].reshape(-1, 3), axis=0).astype(np.uint8)
    x0, y0, x1, y1 = box
    w = x1 - x0
    m = np.clip((x1 - np.arange(x0, x1)) / 36.0, 0, 1)[None, :, None]   # 오른쪽 가장자리는 서서히 섞는다
    a[y0:y1, x0:x1] = (a[y0:y1, x0:x1] * (1 - m) + col * m).astype(np.uint8)
    return Image.fromarray(a)


def round_logo(d):
    src = Image.open(SRC).convert("RGB")
    x0, y0, x1, y1 = CIRCLE
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2 + 4
    c = src.crop((int(cx - r), int(cy - r), int(cx + r), int(cy + r))).convert("RGBA")
    m = Image.new("L", c.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, c.size[0] - 1, c.size[1] - 1), fill=255)
    c.putalpha(m.filter(ImageFilter.GaussianBlur(1)))
    return c.resize((d, d), Image.LANCZOS)


def paste(im, logo, x, y):
    im = im.convert("RGBA")
    im.alpha_composite(logo, (x, y))
    return im.convert("RGB")


def main():
    big = round_logo(256)
    big.save(os.path.join(RES, "mk_logo_round.webp"), "WEBP", quality=95, method=6)

    tab = Image.open(os.path.join(RES, "mk_hdr_logo.webp")).convert("RGB")   # 432x122
    tab = erase(tab, (0, 0, 128, 122), pad=2)
    tab = flat(tab, (136, 82, 432, 122), (300, 100, 420, 118))
    tab = paste(tab, round_logo(100), 10, 10)
    tab.save(os.path.join(RES, "mk_hdr_tab.webp"), "WEBP", quality=94, method=6)

    home = Image.open(os.path.join(RES, "mk_home_header.webp")).convert("RGB")  # 760x116
    home = flat(home, (350, 0, 470, 116), (200, 100, 340, 112))
    home = flat(home, (30, 66, 400, 116), (200, 100, 340, 112))
    home = paste(home, round_logo(66), 358, 2)
    home.save(os.path.join(RES, "mk_hdr_home.webp"), "WEBP", quality=94, method=6)
    Image.new("RGB", (1380, 360), "white")
    t = Image.new("RGB", (1000, 250), (252, 247, 235))
    t.paste(tab, (0, 0)); t.paste(home.resize((560, 85)), (440, 0)); t.save("C:/gt/hdr_prev.png")


main()

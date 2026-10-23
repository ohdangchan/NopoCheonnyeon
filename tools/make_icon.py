"""design/brand/app-icon-source.webp 에서 안드로이드 앱 아이콘(적응형 전경·배경·흑백, 옛 방식 둥근/네모)을 만든다.
실행: python tools/make_icon.py
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "design", "brand", "app-icon-source.webp")
RES = os.path.join(ROOT, "app", "src", "main", "res")
TILE = (30, 24, 1224, 1216)          # 둥근 네모 아이콘 영역
CIRCLE = (103, 94, 1150, 1134)       # 도장 바깥 고리(검은 선) 영역 x0,y0,x1,y1
CREAM = (250, 243, 222)
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

src = Image.open(SRC).convert("RGB")


def circle_stamp(diameter_ratio, canvas):
    """도장 원만 잘라 투명 바탕 정사각 캔버스(canvas px)의 가운데에 놓는다. 지름은 캔버스의 diameter_ratio."""
    x0, y0, x1, y1 = CIRCLE
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    r = max(x1 - x0, y1 - y0) / 2 + 6
    box = (int(cx - r), int(cy - r), int(cx + r), int(cy + r))
    crop = src.crop(box).convert("RGBA")
    mask = Image.new("L", crop.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, crop.size[0] - 1, crop.size[1] - 1), fill=255)
    crop.putalpha(mask.filter(ImageFilter.GaussianBlur(1)))
    d = int(canvas * diameter_ratio)
    crop = crop.resize((d, d), Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    out.paste(crop, ((canvas - d) // 2, (canvas - d) // 2), crop)
    return out


def legacy_tile(size, round_=False):
    t = src.crop(TILE).convert("RGBA")
    t = t.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    if round_:
        d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * 0.22), fill=255)
    t.putalpha(mask.resize((size, size), Image.LANCZOS))
    return t


def mono(stamp):
    a = np.asarray(stamp.convert("RGBA")).astype(int)
    lum = a[:, :, :3].sum(axis=2) / 3
    alpha = np.clip((200 - lum) * 2.4, 0, 255) * (a[:, :, 3] / 255)
    out = np.zeros_like(a)
    out[:, :, 3] = alpha.astype(np.uint8)
    return Image.fromarray(out.astype(np.uint8), "RGBA")


for dens, px in SIZES.items():
    d = os.path.join(RES, f"mipmap-{dens}")
    os.makedirs(d, exist_ok=True)
    fg_size = int(px * 108 / 48)
    # 적응형 안전 영역은 지름 66%. 시스템 마스크에 안 잘리게 도장 지름을 캔버스의 64% 로 둔다.
    stamp = circle_stamp(0.64, fg_size)
    stamp.save(os.path.join(d, "ic_launcher_foreground.webp"), "WEBP", quality=95, method=6)
    Image.new("RGB", (fg_size, fg_size), CREAM).save(os.path.join(d, "ic_launcher_background.webp"), "WEBP", quality=95)
    mono(circle_stamp(0.64, fg_size)).save(os.path.join(d, "ic_launcher_monochrome.webp"), "WEBP", quality=95, lossless=True)
    legacy_tile(px).save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=95)
    legacy_tile(px, round_=True).save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=95)
    print(dens, px, fg_size)

# 스토어용 512 (참고)
legacy_tile(512).save(os.path.join(ROOT, "design", "brand", "play-store-icon-512.png"))
print("done")

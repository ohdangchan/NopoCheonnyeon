"""디자인 시안 2차(design/redesign2/NN-*.webp)용 공통 도구: 폰 화면(744x1616)을 폭 760 으로 맞춘다."""
import glob
import os
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "design", "redesign2")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
BOX = (97, 33, 841, 1649)  # 744 x 1616
W = 760


def screen(n):
    f = glob.glob(os.path.join(SRC, f"{n:02d}-*.webp"))[0]
    im = Image.open(f).convert("RGB").crop(BOX)
    im = im.resize((W, round(im.height * W / im.width)), Image.LANCZOS)
    a = np.asarray(im).copy()
    a[:, :4] = a[:, 4:5]  # 폰 테두리가 가장자리에 남긴 어두운 줄을 안쪽 색으로 덮는다
    a[:, -4:] = a[:, -5:-4]
    return Image.fromarray(a)


def grid(n, y0=0, y1=None, path=None, step=50):
    im = screen(n)
    y1 = y1 or im.height
    c = im.crop((0, y0, W, y1)).copy()
    d = ImageDraw.Draw(c)
    for y in range((y0 // step) * step, y1, step):
        if y < y0:
            continue
        col = (255, 0, 0) if y % 100 == 0 else (255, 160, 160)
        d.line([(0, y - y0), (W, y - y0)], fill=col, width=1)
        if y % 100 == 0:
            d.text((2, y - y0 + 1), str(y), fill=(255, 0, 0))
    for x in range(0, W, 50):
        col = (0, 0, 255) if x % 100 == 0 else (160, 160, 255)
        d.line([(x, 0), (x, y1 - y0)], fill=col, width=1)
        if x % 100 == 0:
            d.text((x + 2, 2), str(x), fill=(0, 0, 255))
    c.save(path or f"C:/gt/g{n}_{y0}.png")


def save(im, name, box, quality=92):
    c = im.crop(box)
    p = os.path.join(OUT, f"mk2_{name}.webp")
    c.save(p, "WEBP", quality=quality, method=6)
    print(f"{name:24s} {c.size[0]:4d}x{c.size[1]:<4d} {os.path.getsize(p)//1024:4d}KB")
    return c

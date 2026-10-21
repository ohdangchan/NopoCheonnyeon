"""앱 아이콘 생성: 청록 간판 바탕 + 붉은 도장("노포"). 실행: python tools/gen_app_icon.py
결과: app/src/main/res/mipmap-*/ic_launcher{,_round,_foreground,_background,_monochrome}.webp, mipmap-anydpi/ic_launcher*.xml
글꼴은 나눔명조 ExtraBold(Windows 글꼴 폴더)를 쓴다."""
import math, os, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

RES = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
FONT = 'C:/Windows/Fonts/NanumMyeongjoExtraBold.ttf'
SIGN, SIGN_DARK = (0x1F, 0x6B, 0x63), (0x0F, 0x2E, 0x2A)
STAMP, BRASS, PAPER = (0xC2, 0x3B, 0x2A), (0xD9, 0xA2, 0x2C), (0xF8, 0xF9, 0xF5)
S = 1728  # 108dp 기준 그림 크기의 4배(축소할 때 부드럽게)

def background():
    im = Image.new('RGB', (S, S), SIGN)
    px = im.load()
    for y in range(S):
        for x in range(S):
            t = (x * 0.35 + y * 0.65) / S
            px[x, y] = tuple(int(SIGN[i] + (SIGN_DARK[i] - SIGN[i]) * t) for i in range(3))
    d = ImageDraw.Draw(im, 'RGBA')
    # 간판 판자 느낌의 옅은 가로 결
    for k in range(1, 9):
        y = int(S * k / 9)
        d.line([(0, y), (S, y)], fill=(255, 255, 255, 14), width=4)
    return im

def seal_mask(size):
    """도장 모양(둥근 네모) 마스크"""
    m = Image.new('L', (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.16), fill=255)
    return m

def glyph_layer(size, color):
    """도장 안 "노/포" 두 글자(투명 바탕, 글자만 color)"""
    f = ImageFont.truetype(FONT, int(size * 0.36))
    lay = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(lay)
    for ch, cy in (('노', 0.30), ('포', 0.70)):
        d.text((size / 2, size * cy), ch, font=f, fill=255, anchor='mm')
    return lay

def foreground(mono=False):
    """투명 바탕 위의 도장. 안전 영역(가운데 약 61%) 안에 들어가게 도장은 전체의 44%"""
    size = int(S * 0.52)
    seal = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    mask = seal_mask(size)
    glyph = glyph_layer(size, None)
    if mono:
        a = Image.new('L', (size, size), 0)
        a.paste(mask, (0, 0))
        knock = ImageDraw.Draw(a)
        a = Image.composite(Image.new('L', (size, size), 0), a, glyph)  # 글자 부분은 비움
        base = Image.new('RGBA', (size, size), (0, 0, 0, 255)); base.putalpha(a)
        seal = base
    else:
        body = Image.new('RGBA', (size, size), STAMP + (255,))
        seal.paste(body, (0, 0), mask)
        d = ImageDraw.Draw(seal)
        # 안쪽 테두리(놋쇠색)
        inset = int(size * 0.055)
        d.rounded_rectangle([inset, inset, size - inset - 1, size - inset - 1], radius=int(size * 0.12), outline=BRASS + (255,), width=max(4, int(size * 0.022)))
        # 글자(종이색)
        paper = Image.new('RGBA', (size, size), PAPER + (255,))
        seal.paste(paper, (0, 0), glyph)
        # 도장 잉크가 고르지 않게 닳은 느낌
        rnd = random.Random(7)
        wear = Image.new('L', (size, size), 0)
        wd = ImageDraw.Draw(wear)
        for _ in range(90):
            x, y, r = rnd.randrange(size), rnd.randrange(size), rnd.randrange(2, 7)
            wd.ellipse([x - r, y - r, x + r, y + r], fill=rnd.randrange(25, 70))
        wear = wear.filter(ImageFilter.GaussianBlur(1.2))
        under = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        under.paste(Image.new('RGBA', (size, size), SIGN + (255,)), (0, 0), Image.composite(wear, Image.new('L', (size, size), 0), mask))
        seal = Image.alpha_composite(seal, under)
        # 그림자
    seal = seal.rotate(-6, resample=Image.BICUBIC, expand=True)
    lay = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    if not mono:
        sh = Image.new('RGBA', seal.size, (0, 0, 0, 0))
        sh.putalpha(seal.getchannel('A').point(lambda v: int(v * 0.35)))
        sh = sh.filter(ImageFilter.GaussianBlur(S * 0.012))
        lay.paste(sh, ((S - seal.width) // 2, (S - seal.height) // 2 + int(S * 0.014)), sh)
    lay.alpha_composite(seal, ((S - seal.width) // 2, (S - seal.height) // 2))
    return lay

def save(im, dp_px, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.resize((dp_px, dp_px), Image.LANCZOS).save(path, 'WEBP', quality=95, method=6)

DENS = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
bg, fg, mono = background(), foreground(), foreground(mono=True)
# 모노크롬(테마 아이콘)용 검은 도장 → 안드로이드가 색을 입힌다
for d, k in DENS.items():
    p = os.path.join(RES, f'mipmap-{d}')
    save(bg, int(108 * k), os.path.join(p, 'ic_launcher_background.webp'))
    save(fg, int(108 * k), os.path.join(p, 'ic_launcher_foreground.webp'))
    save(mono, int(108 * k), os.path.join(p, 'ic_launcher_monochrome.webp'))
    # 옛 기기용(48dp): 바탕+도장을 합치고 둥근 네모/원 모양으로 자른다
    full = bg.convert('RGBA'); full.alpha_composite(fg)
    n = int(48 * k)
    base = full.crop((int(S * 0.08), int(S * 0.08), int(S * 0.92), int(S * 0.92))).resize((n, n), Image.LANCZOS)
    for name, shape in (('ic_launcher', 'round_rect'), ('ic_launcher_round', 'circle')):
        m = Image.new('L', (n * 4, n * 4), 0); md = ImageDraw.Draw(m)
        if shape == 'circle': md.ellipse([0, 0, n * 4 - 1, n * 4 - 1], fill=255)
        else: md.rounded_rectangle([0, 0, n * 4 - 1, n * 4 - 1], radius=int(n * 4 * 0.22), fill=255)
        m = m.resize((n, n), Image.LANCZOS)
        out = Image.new('RGBA', (n, n), (0, 0, 0, 0)); out.paste(base, (0, 0), m)
        out.save(os.path.join(p, name + '.webp'), 'WEBP', quality=95, method=6)

xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
'''
for n in ('ic_launcher', 'ic_launcher_round'):
    open(os.path.join(RES, 'mipmap-anydpi', n + '.xml'), 'w', encoding='utf-8', newline='').write(xml)
# 미리보기(저장소에 남김)
prev = Image.new('RGBA', (1100, 540), (237, 240, 234, 255))
full = bg.convert('RGBA'); full.alpha_composite(fg)
sq = full.resize((480, 480), Image.LANCZOS)
mm = Image.new('L', (480 * 2, 480 * 2), 0); ImageDraw.Draw(mm).rounded_rectangle([0, 0, 959, 959], radius=210, fill=255)
prev.paste(sq, (30, 30), mm.resize((480, 480), Image.LANCZOS))
rm = Image.new('L', (480 * 2, 480 * 2), 0); ImageDraw.Draw(rm).ellipse([0, 0, 959, 959], fill=255)
sm = full.resize((480, 480), Image.LANCZOS); prev.paste(sm, (570, 30), rm.resize((480, 480), Image.LANCZOS))
os.makedirs(os.path.join(RES, '..', '..', '..', '..', 'docs', 'media'), exist_ok=True)
prev.convert('RGB').save(os.path.join(RES, '..', '..', '..', '..', 'docs', 'media', 'app_icon_preview.png'))
print('ok')

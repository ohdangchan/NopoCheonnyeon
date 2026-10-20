#!/usr/bin/env python3
"""시연·시험용 가상 영수증·간판·메뉴판 만들기. 다른 화면(모니터)에 띄우고 앱 카메라로 찍는다.

    python tools/gen_test_receipt.py   ->  tools/test-images/ 아래에
        test_receipt_today.png    현재 시각 영수증 (24시간 안에 사용)          -> 영수증 인증 성공
        test_receipt_expired.png   50시간 전 영수증                              -> E_RECEIPT_EXPIRED
        test_receipt_other.png     다른 가게(행복분식) 영수증, 현재 시각           -> E_RECEIPT_MISMATCH
        test_menuboard.png         메뉴판(가게 이름 포함)                         -> 가게 사진 인증(간판·메뉴판)
        demo_scenarios.html        위 그림을 시나리오별로 크게 보여 주는 시연용 페이지

가게는 설정 개발자 도구의 "테스트 가게 만들기 (현재 위치)"로 만든 `노포천년테스트식당`.
같은 영수증을 두 번 쓰면 중복 영수증으로 막히므로(E_RECEIPT_DUP) 다시 시험할 때는 이 스크립트를 다시 실행한다.
"""
import datetime, os, random

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "tools", "test-images")
BOLD = r"C:\Windows\Fonts\NanumGothicBold.ttf"
REG = r"C:\Windows\Fonts\NanumGothic.ttf"
TITLE = os.path.join(ROOT, "app", "src", "main", "res", "font", "black_han_sans.ttf")
W, H = 720, 1280
INK = (17, 17, 17)


def font(path, size):
    return ImageFont.truetype(path, size)


def receipt(path, shop, biz, phone, addr, when, items, seed):
    r = random.Random(seed)
    no = f"{r.randint(1, 9999):04d}-{r.randint(1, 9999):04d}"
    approval = str(r.randint(10000000, 99999999))
    total = sum(p for _, p in items)
    img = Image.new("RGB", (W, H), (251, 251, 247))
    d = ImageDraw.Draw(img)

    def center(text, y, f):
        b = d.textbbox((0, 0), text, font=f)
        d.text(((W - (b[2] - b[0])) / 2 - b[0], y), text, font=f, fill=INK)

    def right(text, y, f):
        b = d.textbbox((0, 0), text, font=f)
        d.text((W - 60 - (b[2] - b[0]), y), text, font=f, fill=INK)

    def line(y):
        d.line((60, y, W - 60, y), fill=(60, 60, 60), width=2)

    center(shop, 55, font(BOLD, 52))
    d.text((60, 130), "사업자번호: " + biz, font=font(REG, 30), fill=INK)
    d.text((60, 174), "대표: 홍길동   TEL: " + phone, font=font(REG, 30), fill=INK)
    d.text((60, 218), addr, font=font(REG, 30), fill=INK)
    line(268)
    d.text((60, 300), "판매일시: " + when.strftime("%Y-%m-%d %H:%M"), font=font(REG, 32), fill=INK)
    d.text((60, 344), "영수증번호: " + no, font=font(REG, 32), fill=INK)
    line(396)
    y = 428
    for name, price in items:
        d.text((60, y), name, font=font(REG, 32), fill=INK)
        right(f"{price:,}", y, font(REG, 32))
        y += 46
    line(568)
    d.text((60, 600), "합계", font=font(BOLD, 44), fill=INK)
    right(f"{total:,}", 600, font(BOLD, 44))
    d.text((60, 680), "카드번호: 1234-56**-****-7890", font=font(REG, 28), fill=INK)
    d.text((60, 722), f"승인번호: {approval}   일시불", font=font(REG, 28), fill=INK)
    d.text((60, 764), "가맹점번호: 00012345", font=font(REG, 28), fill=INK)
    center("이용해 주셔서 감사합니다", 830, font(REG, 32))
    center("※ 노포 천년 미식가 테스트용 가상 영수증", 880, font(REG, 26))
    img.save(path)


def menuboard(path):
    img = Image.new("RGB", (W, H), (36, 56, 52))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((40, 40, W - 40, H - 40), 28, outline=(217, 162, 44), width=8)

    def center(text, y, f, fill):
        b = d.textbbox((0, 0), text, font=f)
        d.text(((W - (b[2] - b[0])) / 2 - b[0], y), text, font=f, fill=fill)

    center("노포천년테스트식당", 110, font(TITLE, 76), (248, 249, 245))
    center("메  뉴  판", 230, font(BOLD, 50), (217, 162, 44))
    d.line((120, 310, W - 120, 310), fill=(217, 162, 44), width=4)
    y = 360
    for name, price in [("설렁탕", "12,000"), ("수육(소)", "28,000"), ("수육(대)", "38,000"), ("도가니탕", "15,000"), ("공기밥", "1,000")]:
        d.text((130, y), name, font=font(BOLD, 48), fill=(248, 249, 245))
        b = d.textbbox((0, 0), price, font=font(BOLD, 48))
        d.text((W - 130 - (b[2] - b[0]), y), price, font=font(BOLD, 48), fill=(248, 249, 245))
        y += 100
    center("1980년부터 한 솥 국물", H - 190, font(REG, 36), (190, 205, 200))
    center("※ 테스트용 가상 메뉴판", H - 130, font(REG, 26), (150, 165, 160))
    img.save(path)


def scenarios_html(path, stamp):
    items = [
        ("① 영수증 잘 찍힘 → 바로 인증", "test_receipt_today.png", "영수증 인증 → 도장 → 리뷰 작성"),
        ("② 영수증 24시간 지남", "test_receipt_expired.png", "E_RECEIPT_EXPIRED 안내 → 가게 사진 인증으로 이어가기"),
        ("③ 다른 가게 영수증", "test_receipt_other.png", "E_RECEIPT_MISMATCH 안내"),
        ("④ 메뉴판(가게 사진 인증용)", "test_menuboard.png", "위치 확인 → 가게 사진 인증(간판·메뉴판)"),
        ("④ 간판(가게 사진 인증용)", "test_sign.png", "위치 확인 → 가게 사진 인증(간판·메뉴판)"),
    ]
    cards = "".join(
        f'<section><h2>{t}</h2><p>{d}</p><img src="{f}" alt="{t}"></section>' for t, f, d in items
    )
    open(path, "w", encoding="utf-8").write(f"""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>노포 천년 미식가 시연 시나리오 ({stamp})</title>
<style>
body{{margin:0;background:#1d2b28;color:#eef;font-family:'Noto Sans KR','Malgun Gothic',sans-serif}}
nav{{position:sticky;top:0;background:#12201d;padding:10px 16px;display:flex;gap:8px;flex-wrap:wrap}}
nav button{{background:#2f6f66;color:#fff;border:0;border-radius:8px;padding:10px 14px;font-size:16px;cursor:pointer}}
section{{display:none;text-align:center;padding:12px}}
section.on{{display:block}}
img{{max-height:86vh;max-width:100%;border:6px solid #fff;border-radius:6px}}
h2{{margin:6px 0}} p{{margin:0 0 10px;color:#bcd}}
</style></head><body>
<nav>{''.join(f'<button onclick="show({i})">{t.split(" ")[0]}</button>' for i, (t, _, _) in enumerate(items))}</nav>
{cards}
<script>
const s=[...document.querySelectorAll('section')];
function show(i){{s.forEach((e,j)=>e.classList.toggle('on',i===j));}}
show(0);
</script></body></html>
""")


now = datetime.datetime.now()
os.makedirs(OUT, exist_ok=True)
seed = now.strftime("%Y%m%d%H%M%S")
receipt(os.path.join(OUT, "test_receipt_today.png"), "노포천년테스트식당", "123-45-67891", "02-000-0000", "서울특별시 중구 세종대로 110",
        now, [("설렁탕 x2", 24000), ("수육(소) x1", 28000), ("공기밥 x1", 1000)], seed + "a")
receipt(os.path.join(OUT, "test_receipt_expired.png"), "노포천년테스트식당", "123-45-67891", "02-000-0000", "서울특별시 중구 세종대로 110",
        now - datetime.timedelta(hours=50), [("설렁탕 x1", 12000), ("도가니탕 x1", 15000)], seed + "b")
receipt(os.path.join(OUT, "test_receipt_other.png"), "행복분식", "222-33-44556", "02-111-2222", "서울특별시 종로구 종로 1",
        now, [("김밥 x2", 7000), ("떡볶이 x1", 5000), ("어묵 x1", 3000)], seed + "c")
menuboard(os.path.join(OUT, "test_menuboard.png"))
scenarios_html(os.path.join(OUT, "demo_scenarios.html"), now.strftime("%Y-%m-%d %H:%M"))
print("생성:", now.strftime("%Y-%m-%d %H:%M"), "->", OUT)

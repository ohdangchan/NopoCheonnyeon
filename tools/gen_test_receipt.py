#!/usr/bin/env python3
"""시연·시험용 가상 영수증(현재 시각) 만들기. 24시간 안에 다른 화면(모니터)에 띄우고 앱 카메라로 찍는다.

    python tools/gen_test_receipt.py        ->  tools/test-images/test_receipt_today.png

가게는 진단 화면/설정 개발자 도구의 "테스트 가게 만들기 (현재 위치)"로 만든 `노포천년테스트식당`.
같은 영수증을 두 번 쓰면 중복 영수증으로 막히므로(E_RECEIPT_DUP) 다시 시험할 때는 이 스크립트를 다시 실행한다.
"""
import datetime, os, random

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "tools", "test-images", "test_receipt_today.png")
BOLD = r"C:\Windows\Fonts\NanumGothicBold.ttf"
REG = r"C:\Windows\Fonts\NanumGothic.ttf"

now = datetime.datetime.now()
r = random.Random(now.strftime("%Y%m%d%H%M%S"))
no = f"{r.randint(1, 9999):04d}-{r.randint(1, 9999):04d}"
approval = str(r.randint(10000000, 99999999))
items = [("설렁탕 x2", 24000), ("수육(소) x1", 28000), ("공기밥 x1", 1000)]
total = sum(p for _, p in items)

W, H = 720, 1280
img = Image.new("RGB", (W, H), (251, 251, 247))
d = ImageDraw.Draw(img)
f = lambda path, size: ImageFont.truetype(path, size)


def center(text, y, font):
    b = d.textbbox((0, 0), text, font=font)
    d.text(((W - (b[2] - b[0])) / 2 - b[0], y), text, font=font, fill=(17, 17, 17))


def line(y):
    d.line((60, y, W - 60, y), fill=(60, 60, 60), width=2)


center("노포천년테스트식당", 55, f(BOLD, 52))
d.text((60, 130), "사업자번호: 123-45-67891", font=f(REG, 30), fill=(17, 17, 17))
d.text((60, 174), "대표: 홍길동   TEL: 02-000-0000", font=f(REG, 30), fill=(17, 17, 17))
d.text((60, 218), "서울특별시 중구 세종대로 110", font=f(REG, 30), fill=(17, 17, 17))
line(268)
d.text((60, 300), "판매일시: " + now.strftime("%Y-%m-%d %H:%M"), font=f(REG, 32), fill=(17, 17, 17))
d.text((60, 344), "영수증번호: " + no, font=f(REG, 32), fill=(17, 17, 17))
line(396)
y = 428
for name, price in items:
    d.text((60, y), name, font=f(REG, 32), fill=(17, 17, 17))
    t = f"{price:,}"
    b = d.textbbox((0, 0), t, font=f(REG, 32))
    d.text((W - 60 - (b[2] - b[0]), y), t, font=f(REG, 32), fill=(17, 17, 17))
    y += 46
line(568)
d.text((60, 600), "합계", font=f(BOLD, 44), fill=(17, 17, 17))
t = f"{total:,}"
b = d.textbbox((0, 0), t, font=f(BOLD, 44))
d.text((W - 60 - (b[2] - b[0]), 600), t, font=f(BOLD, 44), fill=(17, 17, 17))
d.text((60, 680), "카드번호: 1234-56**-****-7890", font=f(REG, 28), fill=(17, 17, 17))
d.text((60, 722), f"승인번호: {approval}   일시불", font=f(REG, 28), fill=(17, 17, 17))
d.text((60, 764), "가맹점번호: 00012345", font=f(REG, 28), fill=(17, 17, 17))
center("이용해 주셔서 감사합니다", 830, f(REG, 32))
center("※ 노포 천년 미식가 테스트용 가상 영수증", 880, f(REG, 26))
img.save(OUT)
print(OUT, now.strftime("%Y-%m-%d %H:%M"))

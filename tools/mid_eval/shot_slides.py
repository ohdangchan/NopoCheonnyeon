"""발표자료 HTML을 슬라이드별로 캡처해 확인용 이미지를 만든다. 실행: python tools/mid_eval/shot_slides.py [html경로]"""
import glob
import os
import sys

from playwright.sync_api import sync_playwright
from PIL import Image

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
cands = sorted(glob.glob(os.path.join(root, '중간 평가 기획안', '발표*.html')))
f = sys.argv[1] if len(sys.argv) > 1 else cands[0]
out = os.path.join(os.environ.get('TEMP', '.'), 'slides')
os.makedirs(out, exist_ok=True)
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width': 1280, 'height': 720})
    errs = []
    pg.on('pageerror', lambda e: errs.append(str(e)))
    pg.goto('file:///' + f.replace(chr(92), '/'))
    pg.wait_for_timeout(800)
    n = pg.evaluate('document.querySelectorAll(".pg").length')
    print('pages', n)
    for i in range(n):
        pg.wait_for_timeout(1800)
        pg.screenshot(path=os.path.join(out, 's%02d.png' % (i + 1)))
        pg.keyboard.press('ArrowDown')
    print('errors', errs)
    b.close()
# 4장씩 묶어서 확인용 시트
for k in range(0, n, 6):
    ims = [Image.open(os.path.join(out, 's%02d.png' % (j + 1))).resize((640, 360)) for j in range(k, min(k + 6, n))]
    sheet = Image.new('RGB', (1280, 360 * ((len(ims) + 1) // 2)), 'white')
    for idx, im in enumerate(ims):
        sheet.paste(im, ((idx % 2) * 640, (idx // 2) * 360))
    sheet.save(os.path.join(out, 'sheet%d.png' % (k // 6 + 1)))

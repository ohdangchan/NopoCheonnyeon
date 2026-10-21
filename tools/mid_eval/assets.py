"""중간 평가 기획안 문서 5종이 함께 쓰는 이미지·사실 자료.
실행: python tools/mid_eval/assets.py  → 임시 폴더(OUT)에 축소 이미지를 만든다.
각 생성 스크립트(build_*.py)가 이 모듈을 불러 쓴다."""
import base64
import io
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
STITCH = os.path.join(ROOT, 'design', 'stitch', 'screens')
SHOTS = os.path.join(os.environ.get('TEMP', '.'), 'shots')
OUT_DIR = os.path.join(ROOT, '중간 평가 기획안')


def _open(path):
    im = Image.open(path)
    return im.convert('RGB')


def stitch(name, top=None, width=360, quality=80):
    """Stitch 시안 한 장(상단 top 픽셀만 잘라서) → (PIL 이미지)"""
    im = _open(os.path.join(STITCH, name, 'screen.png'))
    if top:
        im = im.crop((0, 0, im.width, min(top, im.height)))
    h = int(im.height * width / im.width)
    return im.resize((width, h), Image.LANCZOS)


def shot(name, width=360):
    im = _open(os.path.join(SHOTS, name + '.png'))
    h = int(im.height * width / im.width)
    return im.resize((width, h), Image.LANCZOS)


def logo(width=200):
    im = Image.open(os.path.join(ROOT, 'docs', 'media', 'app_icon_preview.png')).convert('RGB')
    im = im.crop((30, 30, 510, 510))
    return im.resize((width, width), Image.LANCZOS)


def qr(width=240):
    im = Image.open(os.path.join(ROOT, 'docs', 'media', 'download_qr.png')).convert('RGB')
    return im.resize((width, width), Image.LANCZOS)


def jpeg_bytes(im, quality=80):
    b = io.BytesIO()
    im.save(b, 'JPEG', quality=quality, optimize=True)
    return b.getvalue()


def png_bytes(im):
    b = io.BytesIO()
    im.save(b, 'PNG', optimize=True)
    return b.getvalue()


def data_uri(im, quality=80):
    return 'data:image/jpeg;base64,' + base64.b64encode(jpeg_bytes(im, quality)).decode()


# 문서에 쓰는 화면 목록: (키, 보여줄 이름, 불러오는 방법)
SCREENS = {
    'login': ('로그인', lambda w=360: stitch('auth/login', 1500, w)),
    'map': ('지도 탐색', lambda w=360: stitch('map/map', 1700, w)),
    'filter': ('지도 필터', lambda w=360: stitch('map/map_filter', 1700, w)),
    'store': ('가게 상세', lambda w=360: stitch('store/store_detail', 1900, w)),
    'receipt': ('영수증 인증', lambda w=360: stitch('verify/receipt_verify', 1700, w)),
    'photo': ('가게 사진 인증', lambda w=360: stitch('verify/photo_verify', 1700, w)),
    'stamp': ('도장 연출', lambda w=360: stitch('stamp/stamp_ceremony', 1700, w)),
    'title': ('칭호 승급', lambda w=360: stitch('stamp/title_promotion', 1700, w)),
    'passport': ('내 패스포트', lambda w=360: stitch('passport/passport', 1800, w)),
    'share': ('공유 카드', lambda w=360: stitch('passport/share_card', 1700, w)),
    'review': ('리뷰 작성', lambda w=360: stitch('review/review_editor', 1700, w)),
    'profile': ('유저 프로필', lambda w=360: stitch('profile/user_profile', 1700, w)),
    'test_store': ('테스트 가게', lambda w=360: stitch('diagnostics/test_store', 1700, w)),
}

# 실제 앱 캡처(Galaxy S9+)
DEVICE = {
    'dev_map': ('실제 앱: 지도 탐색', lambda w=360: shot('map', w)),
    'dev_passport': ('실제 앱: 내 패스포트', lambda w=360: shot('passport', w)),
    'dev_chat': ('실제 앱: AI 노포 도우미', lambda w=360: shot('chat', w)),
}

if __name__ == '__main__':
    os.makedirs(OUT_DIR, exist_ok=True)
    for k, (label, fn) in {**SCREENS, **DEVICE}.items():
        im = fn()
        print(k, label, im.size)

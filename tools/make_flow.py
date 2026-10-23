"""화면 캡처를 화살표로 이어 한 장짜리 화면 흐름도(docs/v2/screens_overview/flow.png)를 만든다.
실행: python tools/make_flow.py   (먼저 python tools/make_overview.py 와 같은 캡처 폴더가 필요)
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "docs", "v2", "screens")
OUT = os.path.join(ROOT, "docs", "v2", "screens_overview")
FB = "C:/Windows/Fonts/NanumGothicBold.ttf"

S = 1.64   # 고해상도: 화면 한 장을 540x1110 으로 그린다
TW, TH = int(330 * S), int(678 * S)
CELL_W, CELL_H = 520, 1000
PAD = int(120 * S)
NAVY, LIME, GRAY = (6, 26, 49), (189, 246, 66), (110, 118, 130)

# id: (파일, 라벨, 행, 열)
NODES = {
    "splash": ("15_splash", "시작 화면", 0, 0), "onb1": ("16_onboarding_1", "설치 후 첫 화면", 0, 1),
    "onb2": ("16b_onboarding_2", "취향 고르기", 0, 2), "onb3": ("16d_onboarding_3", "준비 완료", 0, 3),
    "login": ("17_login", "로그인", 0, 4), "signup": ("18_signup", "회원가입", 0, 5),
    "nick": ("20_nickname_setup", "닉네임 정하기", 0, 6), "reset": ("19_reset_password", "비밀번호 재설정", 0, 7),

    "home": ("01_home", "홈", 1, 0), "explore": ("02_explore", "탐색", 1, 1), "map": ("03_map", "지도", 1, 2),
    "coll": ("04_collection", "컬렉션", 1, 3), "my": ("05_my", "마이페이지", 1, 4), "noti": ("06_notifications", "알림", 1, 5),

    "detail": ("08_store_detail", "가게 상세", 2, 0), "vintro": ("12_verify_intro", "인증 안내", 2, 1),
    "vcam": ("13_verify_camera", "영수증 촬영", 2, 2), "vconf": ("21_verify_confirm", "정보 확인", 2, 3),
    "vdone": ("22_verify_done", "인증 완료", 2, 4), "review": ("25_review_write", "후기 작성", 2, 5),

    "call": ("50_collection_all", "방문한 노포", 3, 0), "crev": ("51_collection_reviews", "내가 쓴 리뷰", 3, 1),
    "ctitle": ("52_collection_titles", "칭호 도감", 3, 2), "chall": ("24_hall_of_fame", "미식가 랭킹", 3, 3),
    "cprof": ("54_collection_profile", "내 프로필", 3, 4), "roul": ("23_roulette", "AI 노포 룰렛", 3, 5),
    "chat": ("26_ai_chat", "AI 노포 도우미", 3, 6),

    "pedit": ("30_my_profile_edit", "프로필 편집", 4, 0), "saved": ("32_my_saved", "저장한 노포", 4, 1),
    "visits": ("33_my_visits", "방문 기록", 4, 2), "notiset": ("34_notify_settings", "알림 설정", 4, 3),
    "appset": ("35_app_settings", "앱 환경 설정", 4, 4), "acct": ("36_account_menu", "설정 메뉴", 4, 5),
    "perm": ("42_permission_manage", "권한 관리", 4, 6), "help": ("43_help_center", "도움말 센터", 4, 7),
}

FLOWS = [
    ("1. 처음 실행에서 로그인까지", ["splash", "onb1", "onb2", "onb3", "login", "signup", "nick", "reset"],
     [("splash", "onb1", ""), ("onb1", "onb2", "시작하기"), ("onb2", "onb3", "취향 선택"), ("onb3", "login", "시작하기"),
      ("login", "signup", "회원가입"), ("signup", "nick", "가입"), ("login", "reset", "비밀번호 찾기")]),
    ("2. 가게 고르기에서 방문 인증·후기까지", ["home", "explore", "map", "detail", "vintro", "vcam", "vconf", "vdone", "review"],
     [("home", "explore", "탭 이동"), ("explore", "map", "탭 이동"), ("map", "detail", "가게 선택"), ("detail", "vintro", "방문 인증"),
      ("vintro", "vcam", "영수증 촬영"), ("vcam", "vconf", "촬영"), ("vconf", "vdone", "확정"), ("vdone", "review", "후기")]),
    ("3. 컬렉션과 AI 기능", ["coll", "call", "crev", "ctitle", "chall", "cprof", "roul", "chat"],
     [("coll", "call", "전체 보기"), ("call", "crev", "칩"), ("crev", "ctitle", "칩"), ("ctitle", "chall", "칩"), ("chall", "cprof", "칩"),
      ("coll", "roul", "AI 룰렛"), ("coll", "chat", "AI 도우미")]),
    ("4. 마이페이지와 설정", ["my", "noti", "pedit", "saved", "visits", "notiset", "appset", "acct", "perm", "help"],
     [("my", "noti", "알림"), ("my", "pedit", "프로필 편집"), ("my", "saved", "저장한 노포"), ("my", "visits", "방문 기록"),
      ("my", "notiset", "알림 설정"), ("my", "appset", "앱 환경 설정"), ("my", "acct", "계정 관리"),
      ("acct", "perm", "위치 권한"), ("acct", "help", "도움말")]),
]


def font(sz):
    return ImageFont.truetype(FB, int(sz * S))


def tag(d, cx, cy, label, sz=30):
    f = font(sz)
    w = d.textlength(label, font=f)
    d.rounded_rectangle((cx - w / 2 - 14, cy - 26, cx + w / 2 + 14, cy + 26), 16, fill=LIME, outline=NAVY, width=2)
    d.text((cx - w / 2, cy - 21), label, font=f, fill=NAVY)


def head_arrow(d, x, y, dx, dy, s=22):
    if dx:
        d.polygon([(x, y), (x - dx * s * 1.6, y - s), (x - dx * s * 1.6, y + s)], fill=NAVY)
    else:
        d.polygon([(x, y), (x - s, y - dy * s * 1.6), (x + s, y - dy * s * 1.6)], fill=NAVY)


def render(title, ids, edges, path):
    GAPX = int(200 * S)
    W = PAD * 2 + len(ids) * TW + (len(ids) - 1) * GAPX
    arcs = sum(1 for a, b, _ in edges if abs(ids.index(a) - ids.index(b)) > 1)
    TOP = int((260 + arcs * 70) * S)
    H = TOP + TH + int(180 * S)
    im = Image.new("RGB", (W, H), (251, 246, 235))
    d = ImageDraw.Draw(im)
    d.text((PAD, int(60 * S)), "노포 패스포트 — " + title, font=font(70), fill=NAVY)
    xs = {n: PAD + i * (TW + GAPX) for i, n in enumerate(ids)}
    y0 = TOP
    labels = []
    k = 0
    for a, b, lab in edges:
        ia, ib = ids.index(a), ids.index(b)
        if abs(ia - ib) == 1:
            ya = y0 + TH // 2
            if ib > ia:
                x0, x1 = xs[a] + TW + 8, xs[b] - 8
                d.line([(x0, ya), (x1, ya)], fill=NAVY, width=int(8 * S))
                head_arrow(d, x1, ya, 1, 0)
            else:
                x0, x1 = xs[a] - 8, xs[b] + TW + 8
                d.line([(x0, ya), (x1, ya)], fill=NAVY, width=int(8 * S))
                head_arrow(d, x1, ya, -1, 0)
            if lab:
                labels.append(((x0 + x1) / 2, ya - int(70 * S), lab))
        else:
            ybus = y0 - int((50 + k * 70) * S)
            k += 1
            sx = xs[a] + TW // 2 + (40 if ib > ia else -40)
            ex = xs[b] + TW // 2
            d.line([(sx, y0 - 8), (sx, ybus), (ex, ybus), (ex, y0 - 8)], fill=NAVY, width=int(8 * S), joint="curve")
            head_arrow(d, ex, y0 - 8, 0, 1)
            if lab:
                labels.append(((sx + ex) / 2, ybus, lab))
    for n in ids:
        fn, label, _, _ = NODES[n]
        shot = Image.open(os.path.join(SRC, fn + ".png")).convert("RGB").resize((TW, TH), Image.LANCZOS)
        x = xs[n]
        d.rounded_rectangle((x - 8, y0 - 8, x + TW + 8, y0 + TH + 8), 40, fill=(60, 66, 78))
        im.paste(shot, (x, y0))
        f2 = font(38)
        w = d.textlength(label, font=f2)
        d.text((x + (TW - w) / 2, y0 + TH + 30), label, font=f2, fill=NAVY)
    for cx, cy, lab in labels:
        tag(d, cx, cy, lab)
    im.save(path)
    print(path, im.size)


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (title, ids, edges) in enumerate(FLOWS, 1):
        render(title, ids, edges, os.path.join(OUT, f"flow_{i}.png"))


main()

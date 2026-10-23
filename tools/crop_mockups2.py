"""시안 2차(design/redesign2) 조각 자르기 → res/drawable-nodpi/mk2_*.webp. 실행: python tools/crop_mockups2.py"""
import os
import sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mock2_lib as m
from mock2_lib import screen, save

CREAM = (251, 246, 235)


def fix_corners(im, r=70):
    """폰 테두리의 둥근 모서리(검은색)를 안쪽 색으로 덮는다."""
    a = np.asarray(im).astype(int).copy()
    h, w = a.shape[:2]
    for (cx, cy, sx, sy) in ((0, 0, 1, 1), (w - 1, 0, -1, 1), (0, h - 1, 1, -1), (w - 1, h - 1, -1, -1)):
        for dy in range(r):
            for dx in range(r):
                x, y = cx + sx * dx, cy + sy * dy
                if 0 <= x < w and 0 <= y < h and a[y, x].sum() < 200:
                    # 안쪽으로 밀어 가며 밝은 픽셀을 찾는다
                    k = 1
                    while k < 120:
                        xx, yy = x + sx * k, y + sy * k
                        if 0 <= xx < w and 0 <= yy < h and a[yy, xx].sum() >= 200:
                            a[y, x] = a[yy, xx]
                            break
                        k += 1
    return Image.fromarray(a.astype(np.uint8))


def erase(im, box, color=None, pad=None):
    """box 를 지운다. color 를 주면 한 가지 색, 안 주면 네 변의 색을 섞어 매끈하게 메운다."""
    x0, y0, x1, y1 = box
    if color is not None:
        px = im.load()
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = color
        return im
    pad = pad or 4
    a = np.asarray(im).astype(np.float32)
    h, w = y1 - y0, x1 - x0
    top = a[y0 - pad:y0, x0:x1].mean(axis=0)
    bot = a[y1:y1 + pad, x0:x1].mean(axis=0)
    left = a[y0:y1, x0 - pad:x0].mean(axis=1)
    right = a[y0:y1, x1:x1 + pad].mean(axis=1)
    ty = np.linspace(0, 1, h)[:, None, None]
    tx = np.linspace(0, 1, w)[None, :, None]
    v = top[None, :, :] * (1 - ty) + bot[None, :, :] * ty
    hz = left[:, None, :] * (1 - tx) + right[:, None, :] * tx
    a[y0:y1, x0:x1] = (v + hz) / 2
    return Image.fromarray(a.clip(0, 255).astype(np.uint8))


def erase_flat(im, box, pad=6, feather=22):
    """box 를 왼쪽 가장자리 색(가로로 같은 색, 세로로 섞음)으로 메우고, 위·아래·오른쪽 가장자리는 서서히 섞어 경계가 안 보이게 한다."""
    x0, y0, x1, y1 = box
    a = np.asarray(im).astype(np.float32)
    h, w = y1 - y0, x1 - x0
    left = a[y0:y1, x0 - pad:x0].mean(axis=1)
    fill = np.repeat(left[:, None, :], w, axis=1)
    ty = np.minimum(np.arange(h)[:, None], np.arange(h)[::-1][:, None]) / feather
    tx = (np.arange(w)[::-1][None, :]) / feather
    m = np.clip(np.minimum(ty, tx), 0, 1)[:, :, None]
    a[y0:y1, x0:x1] = a[y0:y1, x0:x1] * (1 - m) + fill * m
    return Image.fromarray(a.clip(0, 255).astype(np.uint8))


def build():
    # ---- 3: 온보딩 ----
    s = screen(3)
    hero = s.crop((0, 80, 760, 1100))
    hp = hero.load()
    save(hero, "ob_hero", (0, 0, 760, 850))  # y 80..930
    for name, box in (("ob_ic1", (50, 960, 152, 1064)), ("ob_ic2", (50, 1110, 152, 1214)), ("ob_ic3", (50, 1260, 152, 1364)),
                      ("ob_ill1", (535, 950, 725, 1095)), ("ob_ill2", (550, 1095, 725, 1230)), ("ob_ill3", (580, 1255, 725, 1400))):
        save(s, name, box)

    # ---- 2: 로그인 ----
    s = fix_corners(screen(2))
    save(s, "lg_hero", (0, 80, 760, 765))
    e1 = erase(s.copy(), (125, 790, 650, 840))
    save(e1, "lg_in1", (0, 765, 760, 865))
    e2 = erase(s.copy(), (125, 910, 650, 962))
    e2 = erase(e2, (655, 915, 715, 960))
    save(e2, "lg_in2", (0, 887, 760, 987))
    save(s, "lg_eye", (660, 915, 706, 957))
    save(s, "lg_ic_google", (590, 1280, 640, 1330))
    save(s, "lg_bottom", (0, 1495, 760, 1651))

    # ---- 1: 회원가입 ----
    s = fix_corners(screen(1))
    save(s, "su_hero", (0, 85, 760, 598))
    for i, (name, box, er) in enumerate((
        ("su_in_nick", (0, 605, 760, 688), [(110, 628, 640, 672), (645, 628, 715, 672)]),
        ("su_in_mail", (0, 728, 760, 814), [(110, 750, 480, 795)]),
        ("su_in_pw", (0, 855, 760, 938), [(110, 876, 650, 922), (655, 876, 715, 922)]),
        ("su_in_pw2", (0, 978, 760, 1061), [(110, 1000, 650, 1045), (655, 1000, 715, 1045)]),
    )):
        t = s.copy()
        for b in er:
            t = erase(t, b)
        save(t, name, box)
    save(s, "su_phone_chip", (548, 748, 712, 798))
    save(s, "su_eye", (660, 880, 706, 920))


def build_verify():
    # 공통 머리글(로고+부제)
    s9 = fix_corners(screen(9))
    save(s9, "v_hdr", (20, 88, 480, 205))
    # 9: 영수증 인증 첫 화면
    save(s9, "v9_hero", (0, 205, 760, 665))
    save(s9, "v9_steps", (22, 650, 745, 892))
    save(s9, "v9_tips", (20, 1105, 745, 1298))
    save(s9, "v9_ic_cam", (258, 912, 310, 965))
    # 10: 카메라 화면 장식(코너 틀)과 셔터
    s10 = fix_corners(screen(10))
    save(s10, "v10_hdr", (110, 90, 600, 235))
    # 8: 정보 확인 행 라벨(아이콘+글자+AI 칩)
    s8 = fix_corners(screen(8))
    for i, (y0, y1) in enumerate(((795, 865), (895, 960), (990, 1060), (1085, 1190), (1195, 1300))):
        save(s8, f"v8_row{i+1}", (40, y0, {0: 312, 1: 312, 2: 312, 3: 283, 4: 318}[i], y1))
    save(s8, "v8_warn", (30, 1318, 735, 1475))
    save(s8, "v8_ic_cam", (88, 1520, 140, 1572))
    # 4: 실패 화면
    s4 = fix_corners(screen(4))
    save(s4, "v4_hero", (0, 205, 760, 625))
    save(s4, "v4_reasons", (25, 722, 740, 1118))
    save(s4, "v4_ic_retry", (258, 1155, 310, 1210))
    save(s4, "v4_ic_cam", (98, 1472, 140, 1512))
    save(s4, "v4_ic_head", (470, 1470, 510, 1515))
    save(s4, "v4_tip", (25, 1335, 740, 1510))
    # 5: 위치 확인 화면
    s5 = fix_corners(screen(5))
    save(s5, "v5_steps", (20, 395, 745, 535))
    save(s5, "v5_map", (0, 545, 760, 925))
    save(s5, "v5_ic_receipt", (48, 1100, 100, 1170))
    save(s5, "v5_safe", (22, 1325, 742, 1505))
    # 11: 방문 인증 완료
    s11 = fix_corners(screen(11))
    save(s11, "v11_title", (60, 205, 700, 410))
    save(s11, "v11_ic_cal", (316, 988, 350, 1022))
    save(s11, "v11_ic_pin", (318, 1028, 350, 1062))
    save(s11, "v11_ic_star", (270, 1155, 335, 1230))
    save(s11, "v11_ic_badge", (492, 1160, 575, 1250))
    save(s11, "v11_ic_map", (100, 1580, 150, 1630))
    save(s11, "v11_ic_next", (445, 1580, 495, 1630))


def build_roulette():
    s14 = fix_corners(screen(14))
    save(s14, "r_title", (20, 225, 500, 380))
    save(s14, "r_note", (500, 250, 745, 400))
    save(s14, "r_note2", (10, 960, 150, 1100))
    save(s14, "r_note3", (590, 940, 745, 1060))
    wheel = s14.crop((95, 490, 705, 1100)).convert("RGBA")
    mask = Image.new("L", wheel.size, 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((6, 24, 606, 624), fill=255)
    d.rectangle((268, 0, 342, 80), fill=255)  # 위쪽 바늘
    wheel.putalpha(mask)
    wp = os.path.join(m.OUT, "mk2_r_wheel.webp")
    wheel.save(wp, "WEBP", quality=92, method=6)
    dice = s14.crop((262, 1102, 322, 1160)).convert("RGBA")
    da = np.asarray(dice).copy()
    dist = np.abs(da[:, :, :3].astype(int) - np.array([214, 244, 74])).sum(axis=2)
    da[:, :, 3] = np.clip((dist - 40) * 6, 0, 255).astype(np.uint8)  # 연두 배경은 투명하게
    Image.fromarray(da).save(os.path.join(m.OUT, "mk2_r_dice.webp"), "WEBP", quality=92, method=6)
    save(s14, "r_chip_tab", (45, 1203, 372, 1250))
    f = erase(s14.copy(), (45, 455, 725, 500))
    f = erase(f, (418, 410, 535, 445))
    save(f, "r_filter", (20, 395, 740, 512))
    s6 = fix_corners(screen(6))
    save(s6, "r6_title", (0, 205, 760, 490))
    save(s6, "r6_char", (590, 1165, 742, 1348))
    save(s6, "r6_ic_fork", (368, 232, 392, 256))
    save(s6, "r6_deco", (500, 960, 740, 1130))


def build_rank():
    s13 = fix_corners(screen(13))
    save(s13, "k_title", (0, 205, 760, 405))
    save(s13, "k_ic_store", (460, 1043, 494, 1076))
    save(s13, "k_ic_ticket", (580, 1043, 614, 1078))
    save(s13, "k_crown", (262, 215, 322, 262))


def build_account():
    s12 = fix_corners(screen(12))
    save(s12, "a_title", (20, 225, 430, 330))
    c = erase(s12.copy(), (30, 356, 175, 500))
    c = erase(c, (190, 372, 520, 490))
    c = erase(c, (30, 505, 745, 580))
    save(c, "a_profile", (12, 338, 748, 600))
    l = s12.copy()
    l = erase(l, (600, 1050, 690, 1095))   # 한국어
    l = erase(l, (600, 1150, 690, 1195))   # 허용됨
    save(l, "a_list", (12, 608, 748, 1305))
    save(s12, "a_logout", (12, 1325, 748, 1415))
    save(s12, "a_delete", (12, 1432, 748, 1522))


def build_perm():
    import glob
    # 15 카메라 / 16 알림 / 17 꺼짐 안내 / 20 위치: 머리글부터 아래 버튼까지 한 장으로 자른다
    for n, name, y1 in ((15, "p_cam", 1472), (16, "p_notif", 1482), (17, "p_off", 1508), (20, "p_loc", 1510)):
        s = fix_corners(screen(n))
        save(s, name, (0, 88, 760, y1))
    # 19 처음 권한 안내(전체 화면, 테두리 없음)
    f = glob.glob(os.path.join(m.SRC, "19-*.webp"))[0]
    im = Image.open(f).convert("RGB")
    im = im.resize((760, round(im.height * 760 / im.width)), Image.LANCZOS)
    save(im, "p_intro", (0, 62, 760, 1350))
    # 18 권한 관리
    s = fix_corners(screen(18))
    save(s, "pm_head", (0, 88, 760, 556))
    for i in range(6):
        y0 = int(558 + i * 139.6)
        r = s.copy()
        col = tuple(int(v) for v in np.median(np.asarray(r.crop((470, y0 + 6, 480, y0 + 12))).reshape(-1, 3), axis=0))
        r = erase(r, (492, y0 + 12, 722, y0 + 118), color=col)
        save(r, f"pm_row{i+1}", (12, y0, 748, y0 + 130))
    save(s, "pm_info", (12, 1410, 748, 1508))
    # 21 GPS 방문 인증
    s = fix_corners(screen(21))
    save(s, "g_head", (0, 205, 760, 365))
    save(s, "g_map", (15, 378, 748, 822))
    save(s, "g_steps", (30, 1170, 735, 1352))
    save(s, "g_info", (15, 1405, 748, 1508))
    save(s, "g_ic_pin", (289, 863, 343, 917))


def paper_at(im, box):
    a = np.asarray(im.crop(box)).reshape(-1, 3)
    return tuple(int(v) for v in np.median(a, axis=0))


def patch_badge(im, box, shift):
    """사진 위 선택 표시(동그라미)를 왼쪽 옆 조각으로 덮는다."""
    x0, y0, x1, y1 = box
    src = im.crop((x0 - shift, y0, x1 - shift, y1))
    im.paste(src, (x0, y0))
    return im


def build_new():
    # 27 로딩 화면: 전체 화면 그림(스피너·글자는 지우고 앱이 직접 그린다)
    s = fix_corners(screen(27))
    s = erase(s, (430, 1462, 530, 1530), pad=6)
    s = erase(s, (320, 1545, 620, 1585), pad=6)
    save(s, "n_splash", (0, 72, 760, 1651))
    # 26 설치 후 첫 화면(눌리는 곳만 앱에서 얹는다)
    s = fix_corners(screen(26))
    save(s, "n_intro1", (0, 84, 760, 1651))
    # 25 / 24 취향 선택: 머리글 그림은 글자를 지우고 앱이 글자를 그린다
    for n, name, x1 in ((25, "n_pref2_head", 500), (24, "n_pref3_head", 530)):
        s = fix_corners(screen(n))
        s = erase_flat(s, (30, 228, x1, 570), pad=6)
        save(s, name, (0, 84, 760, 590))
    s = fix_corners(screen(25))
    foot = erase(s.copy(), (270, 1548, 480, 1610), pad=6)
    save(foot, "n_pref_foot", (0, 1500, 760, 1651))
    regs = ["jongno", "euljiro", "mapo", "seongsu", "busan"]
    for i, r in enumerate(regs):
        x = 28 + int(i * 145.0)
        t = s.copy()
        t = patch_badge(t, (x + 80, 674, x + 126, 720), 52)
        save(t, f"n_reg_{r}", (x, 672, x + 128, 812))
    foods = ["naengmyeon", "gukbap", "bunsik", "gogi", "kalguksu", "jeontongju", "haemul", "jeon"]
    for i, f in enumerate(foods):
        col, row = i % 4, i // 4
        x = [30, 214, 393, 570][col]
        y = 1048 if row == 0 else 1235
        w = 164
        t = s.copy()
        t = patch_badge(t, (x + w - 50, y + 2, x + w - 4, y + 48), 56)
        save(t, f"n_food_{f}", (x, y, x + w, y + 98))
    # 28 로그인
    s = fix_corners(screen(28))
    save(s, "n_login_head", (0, 84, 760, 885))
    for name, box, er in (("n_login_in1", (0, 870, 760, 962), (120, 895, 700, 945)), ("n_login_in2", (0, 968, 760, 1062), (120, 990, 640, 1040))):
        t = s.copy()
        t = erase(t, er, color=paper_at(s, (er[0] + 200, er[1] - 2, er[0] + 210, er[1])) if False else (251, 247, 237))
        save(t, name, box)
    save(s, "n_login_user", (265, 1428, 312, 1475))
    save(erase(s.copy(), (285, 1548, 480, 1594), pad=6), "n_login_foot", (0, 1490, 760, 1651))
    # 22 후기 작성
    s = fix_corners(screen(22))
    save(erase(s.copy(), (18, 280, 545, 340), pad=6), "n_rev_head", (0, 84, 760, 430))
    save(s, "n_rev_banner", (20, 1332, 740, 1462))
    save(s, "n_rev_ic_person", (552, 588, 584, 628))
    # 23 도움말 센터
    s = fix_corners(screen(23))
    t = erase(s.copy(), (95, 448, 560, 490), color=(251, 247, 237))
    save(t, "n_help_head", (0, 84, 760, 524))
    for i, (x, y) in enumerate(((36, 534), (402, 534), (36, 651), (402, 651), (36, 768), (402, 768))):
        save(s, f"n_help_ic{i+1}", (x, y, x + 78, y + 78))


build_new()
build_perm()
build_account()
build_rank()
build_roulette()
build_verify()

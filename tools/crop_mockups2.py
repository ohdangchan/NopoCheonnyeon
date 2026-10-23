"""시안 2차(design/redesign2) 조각 자르기 → res/drawable-nodpi/mk2_*.webp. 실행: python tools/crop_mockups2.py"""
import os
import sys
import numpy as np
from PIL import Image

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


def erase(im, box, color=None):
    """box 안을 색 하나로 지운다(색을 안 주면 박스 바로 위 가장자리 가운데 색)."""
    x0, y0, x1, y1 = box
    px = im.load()
    c = color or px[(x0 + x1) // 2, max(0, y0 - 2)]
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = c
    return im


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
    save(s, "lg_ic_kakao", (357, 1280, 407, 1330))
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
    save(s, "su_ic_kakao", (340, 1513, 424, 1597))
    save(s, "su_ic_naver", (548, 1511, 632, 1595))
    save(s, "su_naver_n", (572, 1534, 610, 1568))


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


build_verify()

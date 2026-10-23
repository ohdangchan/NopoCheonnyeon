"""docs/v2/screens 의 화면 캡처를 구역별로 묶어 심사용 한 장짜리 개요 이미지(docs/v2/screens_overview/)로 만든다.
실행: python tools/make_overview.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "docs", "v2", "screens")
OUT = os.path.join(ROOT, "docs", "v2", "screens_overview")
FONT = "C:/Windows/Fonts/NanumGothicBold.ttf"
FONT_R = "C:/Windows/Fonts/NanumGothic.ttf"

SECTIONS = [
    ("1. 처음 실행과 로그인", [
        ("15_splash", "시작 화면"), ("16_onboarding_1", "설치 후 첫 화면"), ("16b_onboarding_2", "취향 고르기 (2/3)"),
        ("16d_onboarding_3", "준비 완료 (3/3)"), ("17_login", "로그인"), ("18_signup", "회원가입"),
        ("19_reset_password", "비밀번호 재설정"), ("20_nickname_setup", "닉네임 정하기")]),
    ("2. 메인 탭", [
        ("01_home", "홈"), ("02_explore", "탐색"), ("07_explore_search", "검색"), ("03_map", "지도"),
        ("04_collection", "컬렉션 (여권 1/3)"), ("04b_collection_page2", "컬렉션 (여권 2/3)"), ("04c_collection_page3", "컬렉션 (여권 3/3)"),
        ("05_my", "마이페이지"), ("06_notifications", "알림")]),
    ("3. 가게 상세와 방문 인증", [
        ("08_store_detail", "가게 상세"), ("09_store_detail_menu", "가게 상세 (메뉴·정보)"), ("12_verify_intro", "인증 안내"),
        ("13_verify_camera", "영수증 촬영"), ("21_verify_confirm", "영수증 정보 확인"), ("22_verify_done", "방문 인증 완료"),
        ("25_review_write", "방문 후기 작성"), ("60_review_write_validation", "후기 (별점 선택 전)")]),
    ("4. AI 룰렛·도우미와 랭킹", [
        ("23_roulette", "AI 노포 룰렛"), ("56_ai_consent_dialog", "AI 분석 안내(동의)"), ("26_ai_chat", "AI 노포 도우미"),
        ("24_hall_of_fame", "미식가 랭킹 (전국)"), ("58_ranking_seoul", "미식가 랭킹 (서울)"), ("59_ranking_friends", "미식가 랭킹 (친구)")]),
    ("5. 컬렉션 상세", [
        ("50_collection_all", "방문한 노포"), ("51_collection_reviews", "내가 쓴 리뷰"), ("52_collection_titles", "칭호 도감"),
        ("53_collection_hall", "명예의 전당"), ("54_collection_profile", "내 프로필")]),
    ("6. 마이페이지와 설정", [
        ("30_my_profile_edit", "프로필 편집"), ("32_my_saved", "저장한 노포"), ("33_my_visits", "방문 기록"),
        ("34_notify_settings", "알림 설정"), ("35_app_settings", "앱 환경 설정"), ("36_account_menu", "설정 메뉴"),
        ("37_profile_manage", "개인정보·계정"), ("38_linked_dialog", "연결된 계정"), ("41_language_dialog", "언어 설정"),
        ("42_permission_manage", "권한 관리"), ("43_help_center", "도움말 센터")]),
]

TW, TH = 540, 1110          # 화면 한 장 크기
GAP, PAD, COLS = 36, 80, 9


def font(size, bold=True):
    return ImageFont.truetype(FONT if bold else FONT_R, size)


def sheet(title, sections, path):
    rows = []
    for name, items in sections:
        n = (len(items) + COLS - 1) // COLS
        rows.append((name, items, n))
    width = PAD * 2 + COLS * TW + (COLS - 1) * GAP
    height = PAD + 150 + sum(100 + r[2] * (TH + 90 + GAP) for r in rows) + PAD
    im = Image.new("RGB", (width, height), (251, 246, 235))
    d = ImageDraw.Draw(im)
    d.text((PAD, PAD - 10), title, font=font(86), fill=(6, 26, 49))
    y = PAD + 150
    for name, items, nrows in rows:
        d.rounded_rectangle((PAD, y, PAD + 14, y + 64), 6, fill=(189, 246, 66))
        d.text((PAD + 34, y - 2), name, font=font(58), fill=(6, 26, 49))
        y += 100
        for i, (fn, label) in enumerate(items):
            r, c = divmod(i, COLS)
            x = PAD + c * (TW + GAP)
            yy = y + r * (TH + 90 + GAP)
            f = os.path.join(SRC, fn + ".png")
            if not os.path.exists(f):
                continue
            shot = Image.open(f).convert("RGB").resize((TW, TH), Image.LANCZOS)
            d.rounded_rectangle((x - 6, yy - 6, x + TW + 6, yy + TH + 6), 28, fill=(214, 208, 190))
            im.paste(shot, (x, yy))
            d.text((x, yy + TH + 18), label, font=font(38), fill=(6, 26, 49))
        y += nrows * (TH + 90 + GAP)
    im.save(path)
    print(path, im.size)


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet("노포 패스포트 — 전체 화면", SECTIONS, os.path.join(OUT, "00_all_screens.png"))
    for i, sec in enumerate(SECTIONS, 1):
        sheet("노포 패스포트 — " + sec[0], [sec], os.path.join(OUT, f"{i:02d}_{sec[0].split('. ')[1].replace(' ', '_').replace('·', '_')}.png"))


main()

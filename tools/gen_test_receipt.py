#!/usr/bin/env python3
"""시연·시험용 가상 영수증·간판·메뉴판과 시연 시나리오 페이지 만들기. 다른 화면(모니터)에 띄우고 앱 카메라로 찍는다.

    python tools/gen_test_receipt.py   ->  tools/test-images/ 아래에 (모두 노포천년테스트식당 기준, 만든 시각부터 24시간 안에 사용)
        test_receipt_a.png / _b.png / _c.png   정상 영수증 3장(서로 다른 내용 → 인증을 여러 번 시연할 수 있음)
        test_receipt_cut_top.png               위쪽이 잘린 영수증      -> 가게 이름 안 보임 → AI 촬영 도우미
        test_receipt_blur.png                  흐린 영수증             -> 글자 인식 실패 → AI 촬영 도우미
        test_receipt_dark.png                  어두운 영수증           -> 글자 인식 실패 → AI 촬영 도우미
        test_receipt_expired.png               50시간 전 영수증        -> E_RECEIPT_EXPIRED
        test_receipt_other.png                 다른 가게(행복분식) 영수증 -> E_RECEIPT_MISMATCH + AI 촬영 도우미
        test_menuboard.png / test_sign.png     메뉴판·간판            -> 가게 사진 인증(영수증이 안 될 때)
        demo_scenarios.html                    시나리오별 단계·기대 결과·보여 줄 그림(모니터에 띄우는 시연용 페이지)

가게는 설정 개발자 도구의 "테스트 가게 만들기 (현재 위치)"로 만든 `노포천년테스트식당`(지금 서 있는 자리에 생김 → 위치 30m 시험 가능).
시연 직전에 이 스크립트를 다시 실행해 영수증 시각을 새로 만든다.
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


SCENARIOS = [
    {
        "id": "1", "group": "영수증·인증", "title": "인증 전에는 리뷰를 쓸 수 없다",
        "steps": ["지도 탐색 > 목록 > 맨 위 노포천년테스트식당(0m) 열기", "아래로 내려 \"평점과 리뷰\" 확인"],
        "expect": "\"방문 인증한 사람만 평점을 남길 수 있어요\" 안내만 보이고 작성 버튼이 없다. 예시 리뷰 12개·재방문율 33%는 보인다.",
        "images": [],
    },
    {
        "id": "2", "group": "영수증·인증", "title": "영수증이 잘 찍혀 바로 인증 (스캔 연출 → 도장 → 폭죽 → 리뷰)",
        "steps": ["방문 인증하기 > 영수증 촬영 화면", "아래 \"정상 영수증 A\"를 모니터에 크게 띄우고 틀 안에 맞춰 촬영하기",
                  "빨간 레이저가 영수증을 위아래로 훑고 → 읽은 글자에 형광펜이 칠해지는 것을 보여 준다", "도장이 쾅 찍힘(진동) → 폭죽·색종이 → \"평점·리뷰 남기기\" 누르기",
                  "리뷰 편집기에서 별점·글 + 사진(카메라/앨범 최대 3장) 저장"],
        "expect": "스캔 연출 → 도장(46년 아님: 시험용 가게라 세월·하루 한도에는 안 들어감) → 폭죽 → 리뷰 작성. 내 리뷰는 이 기기에만 저장되고 평균에 합쳐진다.",
        "images": [("정상 영수증 A", "test_receipt_a.png")],
    },
    {
        "id": "3", "group": "영수증·인증", "title": "영수증 위쪽이 잘렸을 때 → AI 촬영 도우미 → 다시 찍어 성공",
        "steps": ["방문 인증하기 > \"위쪽이 잘린 영수증\" 촬영", "형광펜 연출 뒤 오류 문구와 함께 AI 촬영 도우미 팝업이 뜬다(처음이면 전송 동의 창이 먼저)",
                  "팝업의 \"다시 찍기\" 누르기 → 카메라로 복귀", "\"정상 영수증 B\"를 촬영해 인증 성공"],
        "expect": "AI가 \"가게 이름이 잘렸어요, 영수증 위쪽까지 담아 주세요\" 같은 한두 문장을 알려 준다. 다시 찍으면 성공.",
        "images": [("위쪽 잘림", "test_receipt_cut_top.png"), ("정상 영수증 B", "test_receipt_b.png")],
    },
    {
        "id": "4", "group": "영수증·인증", "title": "영수증이 흐리거나 어두울 때 → AI 촬영 도우미",
        "steps": ["방문 인증하기 > \"흐린 영수증\" 촬영 → 팝업 확인", "다시 찍기 > \"어두운 영수증\" 촬영 → 팝업 확인"],
        "expect": "흐림은 \"초점·평평하게\", 어두움은 \"밝은 곳에서\" 같은 원인별 안내가 나온다.",
        "images": [("흐린 영수증", "test_receipt_blur.png"), ("어두운 영수증", "test_receipt_dark.png")],
    },
    {
        "id": "5", "group": "영수증·인증", "title": "다른 가게 영수증 / 24시간 지난 영수증",
        "steps": ["\"다른 가게 영수증(행복분식)\" 촬영 → \"영수증의 가게 이름이 달라요\" + AI 안내", "\"24시간 지난 영수증\" 촬영 → \"영수증이 24시간이 지났어요\""],
        "expect": "각각 E_RECEIPT_MISMATCH, E_RECEIPT_EXPIRED 안내. 기록은 남지 않는다.",
        "images": [("다른 가게(행복분식)", "test_receipt_other.png"), ("24시간 지남", "test_receipt_expired.png")],
    },
    {
        "id": "6", "group": "영수증·인증", "title": "영수증이 없거나 계속 안 될 때 → 가게 사진 + 위치(30m)로 인증",
        "steps": ["영수증 화면에서 \"영수증이 없어요\" > 내 위치 확인(테스트 가게는 지금 서 있는 자리라 거리 0m)", "간판 또는 메뉴판 촬영(아래 그림을 모니터에 띄움)",
                  "글자 확인이 3번 실패하면(일부러 엉뚱한 곳 촬영) \"위치 확인 도장\" 선택지가 나온다(설정 스위치로 끌 수 있음)"],
        "expect": "도장(인증 방법: 가게 사진 또는 위치 확인) → 같은 폭죽 연출 → 리뷰 작성.",
        "images": [("메뉴판", "test_menuboard.png"), ("간판", "test_sign.png")],
    },
    {
        "id": "7", "group": "AI 기능", "title": "AI 번역 (외국인 친구에게 보여 주기)",
        "steps": ["아무 가게 상세 > 소개글 아래 \"AI 번역\" > English / 日本語 / 简体中文 …", "\"직접 입력\"으로 Thai 같은 언어도 시도", "\"원문 보기\"로 되돌리기"],
        "expect": "소개글·한 줄 소개·리뷰가 선택한 언어로 바뀌고, 메뉴 이름은 원어가 괄호로 붙는다.",
        "images": [],
    },
    {
        "id": "8", "group": "AI 기능", "title": "AI 노포 도우미 (지도 맨 위)",
        "steps": ["지도 탭 맨 위 \"AI 노포 도우미\" 열기", "칩 3개를 차례로: 말로 찾기(비 오는 날 종로…) / 천년 경로(남은 세월 채우는 코스) / 오늘의 노포 코스(지금 위치·영업시간)",
                  "추천 카드를 눌러 가게 상세로 이동", "앱을 껐다 켜서 다시 열면 이전 대화가 그대로", "\"기록 지우기\""],
        "expect": "후보 가게 안에서만 추천(지어낸 가게 없음), 대화는 이 기기에만 저장.",
        "images": [],
    },
    {
        "id": "9", "group": "AI 기능", "title": "AI 메뉴 룰렛 · 가게 리뷰 AI 분석",
        "steps": ["내 패스포트 > \"AI 메뉴 룰렛\" > 룰렛 돌리기(분석 후 메뉴 3개 + 이유·주재료)", "\"마음에 안 들어요, 다시 돌리기\"를 여러 번(최대 10번 = 서로 다른 메뉴 30개)",
                  "리뷰가 많은 가게(송파구 가게·테스트 가게) 상세 > \"AI로 리뷰 분석하기\""],
        "expect": "룰렛은 앞서 나온 메뉴가 다시 안 나오고 \"본 메뉴 n/30\"이 올라간다. 리뷰 분석은 특장점·추천 메뉴·아쉬운 점·어울리는 사람을 보여 준다.",
        "images": [],
    },
    {
        "id": "10", "group": "연출·패스포트", "title": "칭호 11단계·도감·승급 연출, 공유 카드, 명예의 전당",
        "steps": ["내 패스포트 > 칭호 배지 누르기 > 칭호 도감 > 아무 칭호를 눌러 승급 연출 미리보기", "설정 5탭 > 개발자 도구 > \"연출 미리보기: 금빛 도장 + 칭호 승급\"(도장 → 폭죽 → 승급)",
                  "패스포트 > \"패스포트 카드 공유\"(QR 포함) / \"식객 명예의 전당 보기\""],
        "expect": "100년마다 바뀌는 11개 칭호와 배지 색, 화려한 승급 연출, 공유 카드(QR), 시연용 순위.",
        "images": [],
    },
]


def scenarios_html(path, stamp):
    import html as H, json
    data = json.dumps(SCENARIOS, ensure_ascii=False)
    groups = []
    for sc in SCENARIOS:
        if sc["group"] not in groups:
            groups.append(sc["group"])
    open(path, "w", encoding="utf-8").write("""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>노포 천년 미식가 시연 시나리오 (""" + stamp + """)</title>
<style>
*{box-sizing:border-box}
body{margin:0;background:#14221f;color:#eef3f2;font-family:'Noto Sans KR','Malgun Gothic',sans-serif;display:flex;height:100vh}
aside{width:300px;background:#0e1a18;overflow:auto;padding:12px}
aside h3{margin:14px 6px 6px;color:#9bc;font-size:13px}
aside button{display:block;width:100%;text-align:left;background:#1c332f;color:#fff;border:0;border-radius:8px;padding:10px 12px;margin:4px 0;font-size:15px;cursor:pointer}
aside button.on{background:#2f7f73}
main{flex:1;overflow:auto;padding:18px 24px;display:flex;gap:24px}
.info{width:380px;flex:none}
.info h1{font-size:22px;margin:0 0 10px}
.info ol{padding-left:20px;line-height:1.6}
.expect{background:#1c332f;border-left:4px solid #e0b64a;padding:10px 12px;border-radius:6px;margin-top:14px;line-height:1.5}
.stage{flex:1;text-align:center}
.stage .tabs button{background:#2f7f73;color:#fff;border:0;border-radius:8px;padding:8px 14px;margin:0 4px 10px;font-size:15px;cursor:pointer}
.stage .tabs button.on{background:#e0b64a;color:#111}
.stage img{max-height:88vh;max-width:100%;border:6px solid #fff;border-radius:6px}
.noimg{color:#9bc;margin-top:30vh;font-size:18px}
small{color:#9bc}
</style></head><body>
<aside id="nav"></aside>
<main><div class="info"><h1 id="t"></h1><ol id="s"></ol><div class="expect" id="e"></div><p><small>생성 """ + stamp + """ · 영수증은 이 시각부터 24시간 안에만 유효</small></p></div>
<div class="stage"><div class="tabs" id="tabs"></div><div id="img"></div></div></main>
<script>
const S=""" + data + """;
const nav=document.getElementById('nav');let cur=0,sub=0;
let lastGroup='';
S.forEach((sc,i)=>{if(sc.group!==lastGroup){nav.insertAdjacentHTML('beforeend','<h3>'+sc.group+'</h3>');lastGroup=sc.group}
 const b=document.createElement('button');b.textContent=sc.id+'. '+sc.title;b.onclick=()=>{cur=i;sub=0;render()};b.id='b'+i;nav.appendChild(b)});
function render(){const sc=S[cur];
 S.forEach((_,i)=>document.getElementById('b'+i).classList.toggle('on',i===cur));
 document.getElementById('t').textContent=sc.id+'. '+sc.title;
 document.getElementById('s').innerHTML=sc.steps.map(x=>'<li>'+x+'</li>').join('');
 document.getElementById('e').innerHTML='<b>기대 결과</b><br>'+sc.expect;
 const tabs=document.getElementById('tabs');tabs.innerHTML='';
 sc.images.forEach((im,j)=>{const b=document.createElement('button');b.textContent=im[0];b.className=j===sub?'on':'';b.onclick=()=>{sub=j;render()};tabs.appendChild(b)});
 document.getElementById('img').innerHTML=sc.images.length?'<img src="'+sc.images[sub][1]+'">':'<div class="noimg">이 시나리오는 그림 없이 앱에서 직접 진행해요</div>';}
document.addEventListener('keydown',e=>{if(e.key==='ArrowDown'){cur=Math.min(S.length-1,cur+1);sub=0;render()}if(e.key==='ArrowUp'){cur=Math.max(0,cur-1);sub=0;render()}});
render();
</script></body></html>
""")


now = datetime.datetime.now()
os.makedirs(OUT, exist_ok=True)
for old in os.listdir(OUT):          # 지난 시나리오의 낡은 그림·페이지를 모두 지운다 (test_sign.png는 유지)
    if old != "test_sign.png" and (old.startswith("test_") or old == "demo_scenarios.html"):
        os.remove(os.path.join(OUT, old))
seed = now.strftime("%Y%m%d%H%M%S")
SHOP = ("노포천년테스트식당", "123-45-67891", "02-000-0000", "서울특별시 중구 세종대로 110")
receipt(os.path.join(OUT, "test_receipt_a.png"), *SHOP, now, [("설렁탕 x2", 24000), ("수육(소) x1", 28000), ("공기밥 x1", 1000)], seed + "a")
receipt(os.path.join(OUT, "test_receipt_b.png"), *SHOP, now, [("도가니탕 x1", 15000), ("설렁탕 x1", 12000), ("깍두기 추가 x1", 2000)], seed + "b")
receipt(os.path.join(OUT, "test_receipt_c.png"), *SHOP, now, [("수육(대) x1", 38000), ("공기밥 x3", 3000)], seed + "c")
receipt(os.path.join(OUT, "test_receipt_expired.png"), *SHOP, now - datetime.timedelta(hours=50), [("설렁탕 x1", 12000), ("도가니탕 x1", 15000)], seed + "d")
receipt(os.path.join(OUT, "test_receipt_other.png"), "행복분식", "222-33-44556", "02-111-2222", "서울특별시 종로구 종로 1",
        now, [("김밥 x2", 7000), ("떡볶이 x1", 5000), ("어묵 x1", 3000)], seed + "e")
# AI 촬영 도우미 시연용: 같은 영수증을 일부러 망가뜨린다(위쪽 잘림·흐림·어두움). 정상 영수증과 내용이 달라 인증에 쓰이지 않는다.
from PIL import ImageEnhance, ImageFilter
broken = Image.new("RGB", (1, 1))
receipt(os.path.join(OUT, "_tmp_broken.png"), *SHOP, now, [("순대국 x1", 9000), ("소주 x1", 5000)], seed + "f")
base = Image.open(os.path.join(OUT, "_tmp_broken.png")).convert("RGB")
w, h = base.size
base.crop((0, int(h * 0.30), w, h)).save(os.path.join(OUT, "test_receipt_cut_top.png"))
base.filter(ImageFilter.GaussianBlur(7)).save(os.path.join(OUT, "test_receipt_blur.png"))
ImageEnhance.Brightness(base).enhance(0.18).save(os.path.join(OUT, "test_receipt_dark.png"))
os.remove(os.path.join(OUT, "_tmp_broken.png"))
menuboard(os.path.join(OUT, "test_menuboard.png"))
scenarios_html(os.path.join(OUT, "demo_scenarios.html"), now.strftime("%Y-%m-%d %H:%M"))
print("생성:", now.strftime("%Y-%m-%d %H:%M"), "->", OUT)

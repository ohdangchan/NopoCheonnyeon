"""중간 평가 기획안 ① 서비스 기획서 PDF (형식: 참고 문서 '주도 앱 서비스 기획서.pdf' — A4, 번호 장, 표, 그림, 자문 요청)
실행: python tools/mid_eval/build_pdf.py
HTML을 만든 뒤 Playwright(Chromium)로 PDF를 뽑는다. 숫자는 app/src/main/assets/stores.json 과 PRD에서 읽는다."""
import collections
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import assets  # noqa: E402

OUT = os.path.join(assets.OUT_DIR, '노포천년 서비스 기획서.pdf')

stores = json.load(open(os.path.join(assets.ROOT, 'app', 'src', 'main', 'assets', 'stores.json'), encoding='utf-8'))['stores']
N = len(stores)
FOOD = sum(1 for s in stores if s.get('isFood'))
COORD = sum(1 for s in stores if s.get('lat'))
STORY = sum(1 for s in stores if s.get('story'))
BN = sum(1 for s in stores if 'BAEKNYEON' in s['programs'])
OR = sum(1 for s in stores if 'ORAE' in s['programs'])
ages = [2026 - s['foundedYear'] for s in stores if s.get('foundedYear')]
AVG = sum(ages) / len(ages)
OVER100 = sum(1 for a in ages if a >= 100)
dec = collections.Counter()
for s in stores:
    y = s.get('foundedYear')
    if y and y >= 1950:
        dec[y // 10 * 10] += 1
    elif y:
        dec[0] += 1
    else:
        dec[-1] += 1
decade_rows = ''.join(
    f'<tr><td>{"1950년 이전" if k == 0 else (str(k) + "년대")}</td><td class="num">{dec[k]:,}</td><td><div class="bar" style="width:{dec[k] / max(dec.values()) * 100:.0f}%"></div></td></tr>'
    for k in sorted(k for k in dec if k > 0 or k == 0) if k != -1
)
UNKNOWN = dec[-1]


def fig(key, caption, w=230):
    label, fn = assets.SCREENS[key]
    return f'<figure><img src="{assets.data_uri(fn(420), 82)}" style="width:{w}px"><figcaption><b>{label}</b><br>{caption}</figcaption></figure>'


def dev(key, caption, w=230):
    label, fn = assets.DEVICE[key]
    return f'<figure><img src="{assets.data_uri(fn(420), 82)}" style="width:{w}px"><figcaption><b>{label}</b><br>{caption}</figcaption></figure>'


HTML = f'''<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>노포천년 서비스 기획 보고서</title>
<style>
@page {{ size: A4; margin: 18mm 16mm 20mm 16mm; }}
*{{box-sizing:border-box}}
body{{font-family:"Malgun Gothic","Noto Sans KR",sans-serif;color:#1d2420;font-size:10.4pt;line-height:1.62;margin:0;word-break:keep-all}}
.brand{{font-size:20pt;font-weight:800;color:#17504A;display:flex;justify-content:space-between;align-items:baseline;border-bottom:2px solid #1F6B63;padding-bottom:10px}}
.brand small{{font-size:7.5pt;letter-spacing:.18em;color:#52625e;font-weight:600}}
h1{{font-size:22pt;margin:22px 0 6px;letter-spacing:-.01em}}
.meta{{font-size:8.8pt;color:#52625e;margin-bottom:16px}}
.lead{{border-left:4px solid #C23B2A;padding:12px 18px;margin:16px 0 20px;background:#fbf4f2}}
.lead b.t{{color:#C23B2A;font-size:11.5pt;display:block;margin-bottom:6px}}
h2{{font-size:14.5pt;margin:30px 0 10px;padding-top:16px;border-top:2px solid #14201e;break-after:avoid}}
h3{{font-size:11.6pt;margin:20px 0 6px;break-after:avoid}}
p{{margin:6px 0}}
table{{width:100%;border-collapse:separate;border-spacing:0;border:1px solid #d6ddd6;border-radius:8px;margin:10px 0 14px;font-size:9.2pt;overflow:hidden;break-inside:auto}}
th{{background:#f4f7f4;text-align:left;padding:7px 10px;border-bottom:1px solid #d6ddd6;font-size:8.8pt;color:#33403b}}
td{{padding:7px 10px;border-bottom:1px solid #e8eee8;vertical-align:top}}
tr:last-child td{{border-bottom:0}}
tr{{break-inside:avoid}}
td.num{{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}}
.bar{{height:9px;background:#1F6B63;border-radius:4px}}
.note{{border-left:4px solid #1F6B63;background:#eef5f3;padding:10px 16px;margin:12px 0;font-size:9.6pt}}
.note b.t{{color:#17504A;display:block;margin-bottom:3px}}
.small{{font-size:8.4pt;color:#5b6a65}}
ol,ul{{margin:6px 0 6px 20px;padding:0}}
li{{margin:2px 0}}
.figs{{display:flex;gap:14px;justify-content:center;flex-wrap:nowrap;margin:12px 0;break-inside:avoid}}
figure{{margin:0;text-align:center;font-size:8.2pt;color:#52625e;flex:0 0 auto}}
figure img{{border:1px solid #cdd6cf;border-radius:10px;display:block;margin:0 auto 6px}}
code{{font-family:Consolas,monospace;background:#eef2ee;padding:0 4px;border-radius:3px;font-size:.92em}}
.pb{{break-before:page}}
.sw{{display:inline-block;width:11px;height:11px;border-radius:3px;vertical-align:-1px;margin-right:4px;border:1px solid #0002}}
</style></head><body>

<div class="brand"><span>노포천년 ·</span><small>PRODUCT PLANNING REVIEW / 2026.10.21</small></div>
<h1>노포천년 서비스 기획 보고서</h1>
<div class="meta">최초 작성 2026.10.17 · 개정 2026.10.21 · 목적: 문제·범위·검증 방법·운영 가능성 검토 (중간 평가용)</div>

<div class="lead"><b class="t">내가 먹어 온 노포의 세월을 모아 천 년을 채운다.</b>
노포 천년 미식가는 전국 백년가게·서울 오래가게를 지도에서 찾아 가고, 영수증으로 방문을 인증하면 그 가게의 업력만큼 "세월"이 패스포트에 쌓이는 안드로이드 앱이다. 합계가 1,000년이 되면 "천년 미식가" 칭호를 받는다. 이번 중간 평가본은 기획안(v0.4)을 실제 앱으로 구현해 폰에 설치·실행한 결과(릴리스 APK, 자동 테스트 291개, 실기기 확인)를 기준으로 개정했다.</div>

<p>본 문서는 중간 평가를 위한 기획 보고서다. 화면은 Stitch 시안과 실제 앱 화면을 함께 실었으며, 모든 기능이 완성되었다는 뜻이 아니다. 실기기에서 아직 사용자가 직접 확인하지 못한 항목은 QA 문서(<code>docs/qa-final-2026-10-21.md</code>)에 "미확인"으로 분리해 두었고, 이 보고서의 11·12장에서 다시 밝힌다.</p>

<h2>01. 서비스 정의와 해결할 문제</h2>
<p><b>노포천년은 가 본 노포를 별점이 아니라 "세월"로 기록하고, 그 기록을 다시 보며 다음에 갈 노포를 고르게 돕는 미식 패스포트 서비스다.</b> 1964년에 문을 연 설렁탕집에 가면 62년이, 1990년에 문을 연 분식집에 가면 36년이 내 패스포트에 쌓인다. 맛집 앱이 "어디가 맛있나"에 답한다면 이 앱은 "나는 지금까지 얼마나 오래된 맛을 먹어 봤나"에 답한다.</p>
<p>현재 문제는 세 가지 가설로 정리한다. 첫째, 노포 정보는 정부·지자체 사이트에 흩어져 있고 지도에서 "근처의 노포"만 모아 보기 어렵다. 둘째, 다녀온 기록이 메모·사진·별점으로 흩어져 "내가 어디를 다녀왔는지" 한눈에 보이지 않는다. 셋째, 가 보지 않고도 쓸 수 있는 리뷰가 많아 평가를 믿기 어렵다. 이 문제들의 빈도와 중요도는 인터뷰와 베타에서 확인해야 하며 시장 전체의 확정 사실로 제시하지 않는다.</p>
<p>본 서비스는 맛 평가 서비스가 아니다. 글만으로 어느 가게가 더 맛있다고 판정하지 않고, 위생·영업 상태를 보증하지 않는다. 지도·영업시간 정보는 공식 자료의 수집 시점 값이며 달라질 수 있다.</p>

<h2>02. 기대 효과와 검증 가설</h2>
<table><tr><th style="width:17%">가설</th><th style="width:42%">사용자 가치·측정 방법</th><th>검증 후 결정</th></tr>
<tr><td><b>H01 · 세월 수집의 재미</b></td><td>가게마다 쌓이는 업력(년)과 11단계 칭호가 "더 가 보고 싶다"는 동기가 되는지. 첫 도장까지 걸린 시간과 이후 7일 안 재방문 여부를 관찰한다.</td><td>가설 목표: 시범 사용자 10명 중 6명이 두 번째 가게까지 인증. 미달이면 칭호 간격(100년)과 연출을 조정.</td></tr>
<tr><td><b>H02 · 영수증 인증의 신뢰</b></td><td>위치를 쓰지 않는 영수증 인증이 실제로 통과하는지. 인증 성공률, 실패 사유(E_RECEIPT_*), 사진 인증으로 넘어간 비율을 기록한다.</td><td>성공률이 낮으면 인식 규칙(상호 유사도 0.70, 전화번호)과 안내 문구를 고친다.</td></tr>
<tr><td><b>H03 · 인증된 평점의 가치</b></td><td>방문 인증을 한 사람만 리뷰를 남기게 하면 리뷰를 더 믿는지. 가게 상세에서 리뷰 목록과 프로필을 보는 비율을 본다.</td><td>리뷰 열람이 낮으면 AI 리뷰 요약·단골 충성도 카드 노출을 키운다.</td></tr>
<tr><td><b>H04 · AI 도우미의 도움</b></td><td>"비 오는 날 혼자 먹기 좋은 국밥집"처럼 말로 묻는 검색, 메뉴 룰렛, 후기 요약이 실제로 가게 선택에 쓰이는지. 질문 수와 추천 가게 탭 비율을 본다.</td><td>탭 비율이 낮으면 후보 선정 규칙과 프롬프트를 조정한다.</td></tr>
<tr><td><b>H05 · 운영 지속성</b></td><td>서버 비용 없이 유지 가능한지. Firestore 읽기·쓰기 수, AI 호출 수, 앱 배포 방식(직접 설치 APK)의 부담을 측정한다.</td><td>무료 한도를 넘으면 캐시·호출 제한을 강화하고 서버 프록시를 검토한다.</td></tr></table>
<p class="small">위 목표는 조사 설계를 위한 초안이며 달성 실적이나 시장 대표 표본이 아니다. 사전 인터뷰 몇 명의 반응은 "검증 완료"가 아니라 "초기 관찰"로 기록한다. 표본이 작거나 기간이 짧으면 "검증 미완료"로 남긴다.</p>

<h2 class="pb">03. 아이템 선정의 데이터 근거와 시장 검증</h2>
<h3>3.1 왜 노포인가 — 앱에 담은 공식 자료</h3>
<p>기획의 출발점은 "내가 먹어 온 오래된 맛을 모아 보고 싶다"는 제작자의 개인적 동기이며, 시장 수요가 확인된 아이디어로 포장하지 않는다. 다만 노포 지정 제도는 공식 데이터가 있어 앱의 카탈로그로 바로 쓸 수 있다. 아래는 이번 앱에 넣은 카탈로그의 집계다(<code>app/src/main/assets/stores.json</code>, 2026.10.21 기준).</p>
<table><tr><th>구분</th><th>가게 수</th><th>설명</th></tr>
<tr><td><b>전체 카탈로그</b></td><td class="num">{N:,}곳</td><td>백년가게 {BN:,} + 서울 오래가게 {OR:,} (두 지정에 모두 해당하는 12곳 중복 포함)</td></tr>
<tr><td><b>음식점업</b></td><td class="num">{FOOD:,}곳</td><td>지도 기본 필터는 음식점만 표시</td></tr>
<tr><td><b>좌표 확보</b></td><td class="num">{COORD:,}곳</td><td>주소 좌표 변환 + 이름 검색 보강. 좌표가 없는 가게는 지도에서 빼고 목록·검색에서 "위치 준비 중"으로 표시</td></tr>
<tr><td><b>가게 이야기(소개글) 있음</b></td><td class="num">{STORY:,}곳</td><td>소상공인마당 상세 페이지 '소개글'. 출처를 함께 표시</td></tr>
<tr><td><b>평균 업력</b></td><td class="num">{AVG:.1f}년</td><td>창업연도를 아는 {len(ages):,}곳의 평균. 업력 100년 이상은 {OVER100}곳</td></tr></table>
<table><tr><th style="width:24%">창업 연대</th><th style="width:14%">가게 수</th><th>분포</th></tr>{decade_rows}
<tr><td>창업연도 미상</td><td class="num">{UNKNOWN}</td><td class="small">업력 30년(공식 지정 최소 기준)으로 계산하고 "업력 확인 중" 표시</td></tr></table>
<p><b>기획 해석:</b> 1980~90년대 창업이 가장 많아 업력 30~45년대 가게가 대부분이다. 그래서 한 곳을 가면 대략 40년이 쌓이고, 1,000년을 채우려면 서로 다른 노포 25곳 안팎이 필요하다. "천 년"이 너무 멀지도 쉽지도 않은 목표가 되도록 칭호를 100년 단위 11단계로 나눴다.</p>
<p class="small">분석 한계: 소상공인마당의 창업일은 현재 사업자 기준이라 2000년 이후 날짜는 대를 이어 다시 등록한 날짜일 수 있다. 소개글에 더 이른 연도가 있으면 그 연도를 썼으나 모든 가게를 검증하지는 않았다. 폐업 여부는 수집 시점 이후 달라질 수 있다.</p>

<h3>3.2 유사 서비스 비교</h3>
<p>유사 서비스는 가격·기능을 이번 개정에서 새로 조사하지 않았다. 아래는 비교 출발점이 되는 서비스 유형과 노포천년에서 확인할 차이다.</p>
<table><tr><th style="width:22%">비교 대상(유형)</th><th style="width:36%">참고할 접근</th><th>노포천년에서 검증할 차이</th></tr>
<tr><td>지도 앱의 장소 리뷰</td><td>방문하지 않아도 쓸 수 있는 리뷰와 평점, 풍부한 장소 정보</td><td>"인증한 방문자만" 쓰는 리뷰가 더 믿을 만한가, 그리고 지도에서 노포만 모아 보는 가치가 있는가</td></tr>
<tr><td>맛집 추천·큐레이션 앱</td><td>에디터·사용자 추천, 인기순 노출</td><td>인기보다 "업력"을 기준으로 한 탐색과 수집형 보상이 다른 동기를 만드는가</td></tr>
<tr><td>지자체 스탬프 투어·여행 도장 앱</td><td>장소별 스탬프, 위치 기반 인증</td><td>위치 없이 영수증으로 인증하는 방식(개인정보 최소)이 인증 부담을 낮추는가</td></tr>
<tr><td>메모·기록 앱</td><td>자유로운 기록, 사진·글 보관</td><td>"세월 합계"와 칭호 같은 누적 지표가 기록 지속에 도움이 되는가</td></tr></table>
<p class="small">비교는 기능 참고용이며 경쟁 우위·효과·국내 적정 가격이 입증된 것은 아니다. 국내 대체재에는 기존 메모 앱·지도 즐겨찾기·사진첩도 포함한다.</p>
<ol><li>노포를 자주 찾는 성인 5~8명을 대상으로 방문 기록 방식, 중단 이유, 다시 보고 싶은 정보를 인터뷰한다.</li>
<li>실제 사용 과정(영수증 촬영, 도장, 리뷰)을 관찰하고 막히는 지점 한 가지를 먼저 고친다.</li>
<li>동일한 기능 설명으로 일회성 방문과 재방문 반응을 조사하되, 설문 응답과 실제 인증은 구분한다.</li>
<li>동의·중도 철회·수집 최소화를 적용하고 소규모 결과를 전체 시장으로 일반화하지 않는다.</li></ol>

<h2>04. 사용자·권한·개발 범위</h2>
<div class="note"><b class="t">범위 판단 기준</b>이번 버전은 "안드로이드 폰에 APK를 설치했을 때 기획한 기능이 실제로 돈다"를 최우선으로 한다. 그래서 스토어 출시·iOS·웹·푸시·결제는 뺐고, 대신 인증·지도·패스포트·리뷰·AI 도우미를 끝까지 구현했다.</div>
<table><tr><th style="width:16%">구분</th><th style="width:44%">범위</th><th>제외·조건</th></tr>
<tr><td><b>필수 보유</b></td><td>이메일 가입·로그인·닉네임, 지도(마커·필터·검색·목록), 가게 상세, 영수증·사진·위치 인증, 도장·세월·칭호, 패스포트, 평점·리뷰(사진 최대 3장), 유저 프로필, 오프라인 대기열, 다크 모드</td><td>로그인 전에는 가입·로그인 화면만 접근. 영수증은 위치를 쓰지 않고 이미지를 저장하지 않음</td></tr>
<tr><td><b>확장 · AI</b></td><td>AI 노포 도우미(말로 찾기·천년 경로·오늘의 코스), 메뉴 추천 룰렛(30개), 후기 분석, 단골 충성도 점수, 번역, 영수증 촬영 도우미</td><td>처음 사용 전 전송 동의. AI는 후보 가게 목록 안에서만 추천하고 실패하면 직접 입력으로 넘어감</td></tr>
<tr><td><b>확장 · 공유</b></td><td>패스포트 공유 카드(QR 포함), 칭호 도감, 식객 명예의 전당</td><td>공유 카드에는 닉네임·칭호·세월만. 위치·영수증 없음</td></tr>
<tr><td><b>후속(제외)</b></td><td>Google Play 등록, iOS·웹, 푸시·결제·쿠폰, 서버 판정(Cloud Functions), 팔로우·댓글</td><td>서버 프록시는 AI 호출 보안 강화 때 재검토</td></tr></table>
<table><tr><th style="width:16%">주체</th><th style="width:42%">허용</th><th>제한</th></tr>
<tr><td><b>본인</b></td><td>내 방문 기록·리뷰 작성·수정·삭제, 닉네임 변경, 탈퇴</td><td>다른 사용자의 기록은 쓸 수 없음, 방문 기록 공개 범위(전체·횟수만·비공개) 설정 가능</td></tr>
<tr><td><b>다른 로그인 사용자</b></td><td>리뷰 목록과 프로필(칭호·세월·다녀온 노포·남긴 평점) 열람</td><td>영수증 해시·위치·인증 시각(시:분)은 볼 수 없음</td></tr>
<tr><td><b>운영자(개발자)</b></td><td>카탈로그 갱신, 서버 규칙 배포, 신고 검토</td><td>사용자 비밀번호·영수증 이미지에 접근하지 않음</td></tr>
<tr><td><b>서버(Firestore 규칙)</b></td><td>인증·소유권·필드 형식 검사, 방문 문서가 있을 때만 리뷰 쓰기</td><td>화면에서 버튼을 숨기는 것으로 권한을 대신하지 않음</td></tr></table>
<p>현재 핵심 판단은 "클라이언트가 판정하고 서버 규칙으로 막는다"이다. 서버 판정(Cloud Functions)은 Blaze 요금제가 필요해 이번 버전에서 제외했으며, 따라서 위조 영수증을 완전히 막지는 못한다. 24시간·영수증 해시 중복·하루 5곳 제한으로 피해를 제한한다.</p>

<h2 class="pb">05. 사용자 흐름과 화면 역할</h2>
<ol><li><b>첫 사용:</b> 앱 설치 → 온보딩 3장 → 이메일·닉네임 가입 → 지도에서 근처 노포 확인 → 가게 상세 → 영수증 촬영 → 도장 연출 → 세월 +N년.</li>
<li><b>탐색:</b> 지도(마커·필터·검색) ↔ 목록(가까운 순·업력 순). 검색창에서 "비 오는 날 혼자 먹기 좋은 국밥집"처럼 문장으로 물으면 AI 도우미가 열린다.</li>
<li><b>인증:</b> 영수증이 먼저. 인식 실패나 영수증이 없으면 "가게 사진으로 인증"(30m 이내, 간판·메뉴판 촬영). 3번 실패해도 30m 조건을 통과하면 "위치 확인 도장".</li>
<li><b>기록:</b> 패스포트(세월 게이지·도장 격자·내 리뷰), 도장 상세, 칭호 도감, 공유 카드.</li>
<li><b>리뷰:</b> "리뷰 남기기"를 누르면 오늘 인증하지 않은 경우 방문 인증 화면으로 이동하고, 성공하면 리뷰 편집기가 열린다. 별점만, 글, 사진 3장까지 가능.</li></ol>
<div class="figs">{fig("map", "마커와 하단 시트로 근처 노포 탐색", 215)}{fig("store", "가게 이야기와 세월 +N년 보상 먼저 노출", 215)}{fig("receipt", "영수증 스캔 중 인식한 글자 표시", 215)}</div>
<p class="small">그림은 Stitch 디자인 시안이다. 실제 앱 화면은 8장에 따로 싣는다.</p>
<p>대시보드 대신 패스포트가 요약·근거·재열람을 맡는다. 도장 격자는 빈 칸을 "다음에 갈 노포 자리"로 남겨 다음 행동을 유도한다. 지도와 목록은 같은 필터 상태를 공유하고, 지도 인증 오류나 오프라인에서는 목록으로 전환해 쓸 수 있다.</p>

<h2>06. 핵심 기능의 입력·상태·예외 규칙</h2>
<h3>6.1 방문 인증</h3>
<p><b>영수증 인증(위치 미사용).</b> 앱 안 카메라로만 촬영하고 기기 안 한국어 글자 인식으로 상호·전화번호·사업자번호·결제일시·금액을 읽는다. 판정은 <b>(전화번호 일치) 또는 (상호 유사도 0.70 이상, 별칭 포함)</b> 그리고 <b>결제일시 24시간 이내</b>다. 같은 영수증은 해시로 한 번만 쓸 수 있다. 영수증 이미지는 판정 직후 메모리에서 지우며 기기·서버 어디에도 남기지 않는다.</p>
<p><b>사진·위치 인증(대체).</b> 정밀 위치를 최대 15초 측정해 <b>거리 30m 이하이고 위치 정확도도 30m 이하</b>일 때 촬영 단계로 간다. 모의 위치는 거부한다. 가게 이름·전화번호가 보이는 간판·메뉴판·출입문 사진을 글자 인식해 가게명(유사도 0.60 이상) 또는 전화번호가 있으면 통과한다.</p>
<table><tr><th style="width:26%">상태·오류</th><th style="width:40%">사용자 문구(요약)</th><th>다음 행동</th></tr>
<tr><td>E_RECEIPT_UNREADABLE</td><td>영수증을 읽지 못했어요. 평평하게 펴고 밝은 곳에서 다시</td><td>재촬영 / 가게 사진 인증</td></tr>
<tr><td>E_RECEIPT_MISMATCH · EXPIRED</td><td>이 가게의 영수증이 아닌 것 같아요 · 24시간 안에 결제한 영수증만</td><td>재촬영 / 가게 사진 인증</td></tr>
<tr><td>E_FAR · E_ACCURACY</td><td>가게에서 약 N m 떨어져 있어요 · 위치 정확도가 낮아요</td><td>재측정</td></tr>
<tr><td>E_DAILY_LIMIT · ALREADY_TODAY</td><td>오늘은 5곳까지 · 오늘 이미 다녀온 가게</td><td>안내</td></tr>
<tr><td>E_NETWORK</td><td>인터넷이 연결되면 자동으로 저장할게요</td><td>대기열 → 자동 전송("확인 중" 도장)</td></tr>
<tr><td>E_AI_FAILED</td><td>AI 분석을 가져오지 못했어요. 잠시 뒤 다시</td><td>다시 시도</td></tr></table>
<p class="small">전체 오류 코드 27종과 문구는 PRD 7.4장에 있다. 인증 기준값은 <code>VerificationConfig</code> 한 곳에서만 정의한다.</p>

<h3>6.2 세월·칭호·도장</h3>
<p>업력은 <code>방문한 해 − 창업연도</code>이고 창업연도를 모르면 30년이다. 처음 인증한 가게만 인증 당시 업력을 세월로 적립하며 이후 바뀌지 않는다. 재방문은 단골 횟수만 올린다. 도장은 업력 30~49 일반(청록), 50~99 붉은, 100 이상 금빛이다. 칭호는 세월 100년마다 한 단계 올라 11단계(첫술 식객 → 골목 단골 → 국밥 순례자 → 시장통 어르신 → 가마솥 감별사 → 노포 길잡이 → 맛의 장인 → 세월 지킴이 → 미식 도인 → 전설의 식객 → 천년 미식가)다.</p>
<h3>6.3 평점·리뷰</h3>
<p>별점 1~5는 필수, 글(0~500자)과 사진(최대 3장)은 선택이다. 작성 자격은 그 가게를 방문 인증한 사용자이며 서버 규칙이 방문 문서 없는 리뷰를 거부한다. 한 가게에 최대 5개까지 남길 수 있다. 저장·삭제는 온라인에서만 하며 오프라인이면 쓰던 글을 그대로 두고 연결되면 저장 버튼을 다시 켠다. 사진은 긴 변 720px WebP로 줄여 문자열로만 보관하고 파일로 남기지 않는다.</p>
<h3>6.4 AI 도우미</h3>
<table><tr><th style="width:20%">기능</th><th style="width:44%">입력 → 출력</th><th>안전장치</th></tr>
<tr><td><b>AI 노포 도우미</b></td><td>사용자 질문 + 지금 시각·내 세월·후보 가게 → 답변 + 추천 가게 카드</td><td>후보 목록에 없는 가게 ID는 버린다. 대화 기록은 기기에만 저장</td></tr>
<tr><td><b>메뉴 룰렛</b></td><td>내 리뷰·방문 가게 메뉴·음식 사진 → 메뉴 3개(이유·재료) × 최대 10번 = 서로 다른 메뉴 30개</td><td>이전 라운드 메뉴를 제외 목록으로 전달, 처음 사용 전 동의</td></tr>
<tr><td><b>후기 분석</b></td><td>다른 이용자 리뷰 3개 이상 → 한 줄 요약·특장점·추천 메뉴·아쉬운 점</td><td>같은 리뷰는 캐시, 리뷰 없으면 숨김</td></tr>
<tr><td><b>단골 충성도</b></td><td>재방문 통계 → 0~100점과 근거</td><td>앱이 기준 점수를 계산하고 AI 보정은 ±12점으로 고정</td></tr>
<tr><td><b>촬영 도우미</b></td><td>인식 실패한 영수증·간판 + 실패 사유 → 원인과 다시 찍는 방법 한 문장</td><td>실패해도 인증 흐름은 막지 않음</td></tr></table>

<h2 class="pb">07. 확장과 안전 관리</h2>
<p>AI는 상시 자율 운영자가 아니라 "고르고 설명하는" 보조다. 금액·날짜·거리·세월처럼 틀리면 안 되는 값은 앱이 계산하고, 모델은 후보를 고르거나 문장을 만든다. 실패하면 오류 문구를 보여 주고 직접 탐색으로 넘어가게 한다.</p>
<table><tr><th style="width:22%">사건</th><th>상태와 처리</th></tr>
<tr><td><b>리뷰 사진 올리기</b></td><td>방문 인증과 달리 카메라 또는 시스템 사진 선택기(저장소 권한 없음). 줄여서 문자열로만 보관, 파일 저장 없음</td></tr>
<tr><td><b>리뷰 폭탄·필드 변조</b></td><td>서버 규칙이 한 가게 5개 슬롯, 필드 화이트리스트, 사진 크기, 닉네임 일치, 서버 시각을 검사</td></tr>
<tr><td><b>AI 전송</b></td><td>처음 사용 전 동의 화면. 영수증·사진·후기 요약이 외부 AI로 가는 항목을 안내. 동의 철회 가능</td></tr>
<tr><td><b>신고</b></td><td>리뷰·가게 정보 오류 신고(<code>reports</code>)는 구현, 처리 절차는 운영 준비 후 도입</td></tr>
<tr><td><b>키·비밀</b></td><td>Gemini 키는 앱(APK)에 넣지 않고 Firebase AI Logic을 통해 호출. 서명 키·서비스 키는 저장소에 올리지 않음</td></tr></table>
<div class="note"><b class="t">솔직한 한계</b>공개 APK에 한때 Gemini 키가 포함되어 이후 폐기·교체 절차를 밟았다(2026.10.21). Firebase AI Logic의 App Check 적용은 2026.11.02부터 의무가 되므로 이후에는 서버 프록시 또는 App Check 제공자가 필요하다. 이 항목은 시연 이후 과제로 남겨 두었다.</div>

<h2>08. 디자인 초안과 상태 설계</h2>
<p>디자인 시스템은 "골목 간판과 인주"다. 오래된 가게 간판의 청록 법랑, 종이 영수증의 회녹색 바탕, 도장의 붉은 인주를 기본 색으로 삼았다. 붉은색은 도장과 핵심 버튼에만 쓴다.</p>
<p><span class="sw" style="background:#17504A"></span>간판 청록 #17504A &nbsp; <span class="sw" style="background:#1F6B63"></span>포인트 청록 #1F6B63 &nbsp; <span class="sw" style="background:#C23B2A"></span>인주 빨강 #C23B2A &nbsp; <span class="sw" style="background:#D9A22C"></span>놋쇠 #D9A22C &nbsp; <span class="sw" style="background:#EDF0EA"></span>종이 #EDF0EA &nbsp; <span class="sw" style="background:#F8F9F5"></span>카드 #F8F9F5</p>
<p>글꼴은 제목·숫자 Black Han Sans, 본문 Noto Sans KR, 모서리 16dp, 간격 8dp 단위, 터치 영역 48dp 이상, 대비 4.5:1 이상이다. 도장 연출은 380ms 안에 찍히고 숫자가 600ms 동안 올라가며 진동 1회, "애니메이션 끄기" 설정이면 정적 화면을 쓴다.</p>
<div class="figs">{fig("stamp", "도장이 찍히는 순간(380ms)과 세월 카운트", 205)}{fig("passport", "세월 게이지·도장 격자·칭호", 205)}{fig("review", "별점·글·사진 리뷰 작성", 205)}</div>
<p>화면 상태는 "로딩·빈 자료·실패·저장 중·이탈"을 구분한다. 빈 자료는 거짓 숫자를 만들지 않고, 실패는 오류 없는 빈 화면으로 보이지 않게 재시도 경로를 둔다. 아래는 실제 Galaxy S9+에 설치한 릴리스 앱의 화면이다.</p>
<div class="figs">{dev("dev_map", "근처 노포 마커와 필터, 내 세월 표시", 205)}{dev("dev_passport", "세월 668/1,000년, 도장 13개", 205)}{dev("dev_chat", "말로 찾는 오늘의 노포 코스", 205)}</div>

<h2 class="pb">09. 데이터 모델과 API·권한 설계</h2>
<p>구현 전 설계 초안이며 기존 앱의 스키마를 그대로 옮긴 것이 아니다. 저장 모델에는 안정적 ID와 생성·수정 시각을 둔다. 위치 좌표와 영수증 이미지는 서버에 저장하지 않는다.</p>
<table><tr><th style="width:22%">자료</th><th style="width:44%">주요 필드·관계</th><th>주의사항</th></tr>
<tr><td><b>users/{{uid}}</b></td><td>nickname, nicknameLower, titleId, totalYears, visitCount, reviewCount, visitsVisibility</td><td>본인만 쓰기, 로그인 사용자는 읽기. 닉네임은 별도 문서로 중복 방지</td></tr>
<tr><td><b>users/{{uid}}/visits/{{storeId}}</b></td><td>yearsAwarded, grade, method, photoVerified, firstVisitedAt, lastVisitedAt, visitCount</td><td>yearsAwarded 변경 불가. 방문일은 서울 기준 그날 0시로 잘라 저장</td></tr>
<tr><td><b>reviews/{{storeId}}_{{uid}}_{{1..5}}</b></td><td>rating, text, photos(≤3), nickname, titleId, updatedAt</td><td>방문 문서가 있을 때만 쓰기, 필드 화이트리스트, 사진 원소 ≤170,000자</td></tr>
<tr><td><b>receiptHashes/{{hash}}</b></td><td>uid, storeId, createdAt</td><td>읽기 불가, 생성만. 같은 영수증 재사용 차단</td></tr>
<tr><td><b>storeStats/{{storeId}}</b></td><td>visitors, revisitors 카운터</td><td>재방문율을 컬렉션 그룹 쿼리 없이 계산</td></tr>
<tr><td><b>기기 Room·DataStore</b></td><td>stores(카탈로그), my_visits(내 방문), pending_ops(전송 대기), 설정·AI 동의</td><td>테스트 가게 기록은 서버로 보내지 않음</td></tr></table>
<table><tr><th style="width:30%">외부 연동</th><th>내용</th></tr>
<tr><td>네이버 지도 SDK 3.24.0</td><td>공식 MapView를 AndroidView로 사용. 앱 패키지명 등록 필요</td></tr>
<tr><td>ML Kit 한국어 글자 인식</td><td>앱에 포함(오프라인 동작), 영수증·간판 인식</td></tr>
<tr><td>Firebase Auth · Firestore · Crashlytics</td><td>이메일 로그인, 사용자 데이터·리뷰, 릴리스 크래시 수집</td></tr>
<tr><td>Gemini (Firebase AI Logic)</td><td>AI 도우미·룰렛·분석·번역·촬영 도우미. 25초 제한 후 예비 모델로 한 번 재시도</td></tr></table>

<h2>10. 기술 구조·시험 배포·복구</h2>
<p>앱은 Kotlin·Jetpack Compose의 MVVM + 단방향 흐름이며 수동 주입(<code>AppContainer</code>)을 쓴다. Hilt·비공식 지도 래퍼는 설정 오류를 줄이려고 뺐다. 화면은 상태만 그리고, 규칙 판단은 <code>domain</code> 계층 함수, 기기 기능(위치·카메라·글자 인식)은 포트로 감싼다. 모든 기기·서버 호출에는 제한 시간과 오류 변환을 둔다.</p>
<table><tr><th style="width:22%">준비 영역</th><th>요구사항·완료 증거</th></tr>
<tr><td><b>환경·권한</b></td><td>개발/시험 Firebase 프로젝트 구분, 서명 키는 이 PC에만 보관(저장소 제외), 공개 저장소에는 키·서비스 파일 없음</td></tr>
<tr><td><b>빌드</b></td><td>릴리스 APK: R8 축소·난독화, arm64만 포함(약 88MB), 릴리스 키 서명. 디버그와 릴리스 패키지명 동일</td></tr>
<tr><td><b>배포</b></td><td>GitHub 공개 릴리스 <code>v0.1.0-demo</code> + 다운로드 QR(앱 공유 카드·README). 다음 버전에서 Play 등록</td></tr>
<tr><td><b>영속성</b></td><td>앱을 닫아도 WorkManager가 대기열 전송, 재설치 후 로그인하면 서버 기록으로 패스포트 복원</td></tr>
<tr><td><b>비용·감시</b></td><td>Crashlytics 크래시 수집, Firestore 사용량·AI 호출량 점검</td></tr>
<tr><td><b>장애·복구</b></td><td>규칙 배포 이력 보관, 카탈로그는 앱에 포함되어 서버 장애에도 지도·목록 사용 가능, AI 실패 시 직접 탐색으로 대체</td></tr></table>
<p>시험은 <b>단위 테스트 291개 통과·실패 0</b>, 린트 오류 0, 릴리스 APK 설치·실행 확인, 실기기(Galaxy S9+, Z Fold)에서 주요 흐름을 확인했다. 서버 보안 규칙 에뮬레이터 시험(시나리오 18개)은 작성만 하고 이 PC에서 실행하지 못했다.</p>

<h2 class="pb">11. 가격 정책과 운영비 산정 방법</h2>
<p>현재 앱은 무료 APK로 배포하며 결제 기능이 없다. 따라서 "가격"은 정하지 않았고, 아래는 운영비가 어디에서 생기는지를 정리한 것이다. 요금·무료 한도는 실제 배포 전에 공식 가격표로 다시 확인해야 하며 이번 보고서는 금액을 단정하지 않는다.</p>
<table><tr><th style="width:22%">단계</th><th style="width:40%">측정할 원가 변수</th><th>제한·대응</th></tr>
<tr><td><b>인증·기록</b></td><td>Firestore 읽기·쓰기 수, 방문·리뷰 문서 크기, 대기열 재전송 횟수</td><td>카탈로그는 앱에 포함해 서버 읽기를 줄임, 리뷰 목록은 20개씩 페이지</td></tr>
<tr><td><b>리뷰 사진</b></td><td>리뷰당 사진 3장 × 최대 170KB 문자열, 목록에서 읽는 총량</td><td>720px WebP로 축소, 후속으로 썸네일·Storage 분리 검토</td></tr>
<tr><td><b>AI 호출</b></td><td>질문 수, 이미지 첨부(룰렛 최대 6장), 모델 지연·재시도</td><td>동의 후 호출, 25초 제한·예비 모델, 사용자당 일일 호출 상한(프록시 도입 시)</td></tr>
<tr><td><b>배포</b></td><td>GitHub 릴리스 용량(약 88MB), 다운로드 횟수</td><td>Play 등록 시 정책·수수료 별도 확인</td></tr></table>
<p><b>월 현금 원가 = 인증(Auth) + DB 읽기·쓰기 + AI 호출 + 파일·배포 + 기타 고정비.</b> 원가는 사용자 수만이 아니라 사용자당 읽기·쓰기·AI 호출 횟수로 산정한다. 지금은 Firebase 무료 요금제(Spark) 범위에서 운영해 현금 지출이 없고, 한도를 넘으면 캐시·호출 제한을 먼저 강화한다.</p>
<p class="small">수익화(구독·광고)는 이번 범위가 아니다. 반복 가치와 원가를 확인한 뒤 판단하며 앱 기능이 적다는 이유로 범위를 늘려 구독을 정당화하지 않는다. 공식 참고: Firebase · Cloud Firestore · Firebase AI Logic 요금 문서.</p>

<h2>12. 개발 순서와 검증 완료 기준</h2>
<table><tr><th style="width:14%">단계</th><th style="width:44%">산출물</th><th>다음 단계 조건</th></tr>
<tr><td><b>1 기능 검증</b></td><td>빈 앱에 지도·카메라·한국어 글자 인식·위치·Firebase 진단 화면</td><td>실기기 통과 후 본 개발(이 화면은 진단 화면으로 앱에 남김)</td></tr>
<tr><td><b>2 핵심 규칙</b></td><td>업력·세월·칭호 규칙, 영수증 필드 추출·판정, 사진 판정, 오류 코드(테스트 먼저)</td><td>경계값 단위 테스트 통과</td></tr>
<tr><td><b>3 화면·연결</b></td><td>로그인 게이트, 지도, 가게 상세, 인증, 도장, 패스포트, 리뷰, 프로필</td><td>실기기에서 SC-1~SC-8 확인</td></tr>
<tr><td><b>4 서버·보안</b></td><td>Firestore 규칙·색인 배포, 대기열 동기화, 재설치 복원</td><td>규칙으로 방문 없는 리뷰 거부 확인</td></tr>
<tr><td><b>5 AI·연출</b></td><td>AI 도우미·룰렛·분석·충성도, 도장·칭호 연출, 공유 카드</td><td>AI 실패 시 직접 탐색으로 대체되는지 확인</td></tr>
<tr><td><b>6 릴리스·QA</b></td><td>릴리스 APK 서명·설치, QR 배포, QA 문서</td><td>미확인 항목을 사용자가 직접 통과</td></tr></table>
<p>일정은 범위와 외부 준비가 확정된 뒤 추정한다. AI 코딩 속도나 시안 제작 시간을 출시 일정으로 간주하지 않는다. 이 프로젝트는 2026.10.19~21 사이에 106개의 커밋으로 개발했다.</p>
<table><tr><th style="width:12%">시험</th><th>입력·상황 → 기대 결과</th></tr>
<tr><td>T01</td><td>같은 영수증을 두 번 인증 → 두 번째는 E_RECEIPT_DUP로 거부</td></tr>
<tr><td>T02</td><td>24시간이 지난 영수증 → E_RECEIPT_EXPIRED, 가게 사진 인증 안내</td></tr>
<tr><td>T03</td><td>가게에서 35m, 정확도 10m → E_FAR와 현재 거리 표시. 정확도 80m → E_ACCURACY</td></tr>
<tr><td>T04</td><td>글자 확인 3번 실패 + 30m 이내 → "위치 확인 도장" 선택지</td></tr>
<tr><td>T05</td><td>세월 99 → 100 경계 → 칭호 승급 연출, 재방문은 단골 횟수만 증가</td></tr>
<tr><td>T06</td><td>방문 인증 없이 리뷰 쓰기 시도 → 서버 규칙 거부, 화면은 인증 화면으로 안내</td></tr>
<tr><td>T07</td><td>비행기 모드에서 인증 → 대기열 저장, 복귀 시 자동 전송</td></tr>
<tr><td>T08</td><td>AI 호출 실패(키 없음·시간 초과) → "AI 분석을 가져오지 못했어요", 기능은 계속 사용</td></tr>
<tr><td>T09</td><td>다른 계정 로그인 → 이전 계정 기록이 보이지 않음, 재설치 후 서버에서 복원</td></tr></table>
<p><b>기능 시험 기록:</b> 시험ID / 빌드·리비전 / 기기·환경 / 입력 / 기대 / 실제 / 통과 여부 / 증거 / 결함·재시험. <b>가설 검증 기록:</b> 가설ID / 사전 기준 / 참여 인원·기간 / 측정 결과 / 외부 조건 / 미확인 사항 / 다음 결정. 기능 시험 통과와 사용자 가치 입증은 별도로 보고한다.</p>

<h2>13. 평가위원께 요청드리는 자문</h2>
<ol><li>"내가 먹은 노포의 세월을 모은다"는 수집형 접근이 실제 사용자에게 충분한 동기가 되는가?</li>
<li>영수증 중심 인증(위치 미사용)과 사진·위치 대체 인증의 조합이 부정 인증을 줄이면서 사용 부담을 낮추는가?</li>
<li>인증한 방문자만 리뷰를 쓰게 한 정책이 리뷰 수 부족이라는 초기 문제를 키우지 않는가?</li>
<li>AI 도우미 기능 중 먼저 다듬어야 할 것은 무엇이며, 데이터 전송 동의·표시는 충분한가?</li>
<li>공개 APK에서 AI 호출을 안전하게 유지하려면 App Check·서버 프록시 중 어떤 순서가 적절한가?</li>
<li>인터뷰·소규모 베타의 표본·기간·지표는 적절한가? 건강 효과가 아닌 사용 가치를 어떻게 검증할 것인가?</li>
<li>스토어(Google Play) 출시 전에 반드시 정리해야 할 정책·개인정보 항목은 무엇인가?</li></ol>
<p>자문 후 범위·인증 기준·AI 호출 구조를 확정하고, 화면·데이터·규칙을 정리한 뒤 작은 기능 단위로 개선한다.</p>

<h3>참고 문서와 개정 이력</h3>
<p>구성 참고: 제공된 서비스 기획 보고서 양식(주도 앱 서비스 기획서). 내부 근거: <code>docs/nopo-01-PRD.md</code>(요구사항), <code>nopo-02-architecture.md</code>(구조·규칙), <code>nopo-03-stories.md</code>(스토리), <code>nopo-04-dev-qa-checklist.md</code>(시험), <code>nopo-planning.html</code>(기획안 v0.4), <code>design/stitch</code>(Stitch 시안 51화면), <code>docs/qa-final-2026-10-21.md</code>(최종 QA), <code>docs/dev-status.md</code>(인수인계).</p>
<p>개정: 기획안 v0.4(2026.10.17)의 서비스 정의·범위를 유지하고, 구현 결과(AI 도우미, 사진 리뷰, 칭호 11단계, 공유 카드, 릴리스 APK·QR 배포)와 검증 상태, 한계(서버 규칙 시험 미실행, 공개 키 노출 이력, App Check 의무화)를 반영했다.</p>
</body></html>'''

os.makedirs(assets.OUT_DIR, exist_ok=True)
html_path = os.path.join(os.environ.get('TEMP', '.'), 'nopo_plan_report.html')
open(html_path, 'w', encoding='utf-8').write(HTML)

from playwright.sync_api import sync_playwright  # noqa: E402

with sync_playwright() as p:
    b = p.chromium.launch()
    page = b.new_page()
    page.goto('file:///' + html_path.replace('\\', '/'))
    page.wait_for_timeout(500)
    page.pdf(
        path=OUT, format='A4', print_background=True, display_header_footer=True,
        margin={'top': '18mm', 'bottom': '20mm', 'left': '16mm', 'right': '16mm'},
        header_template='<div style="font-size:8px;width:100%;padding:0 16mm;display:flex;justify-content:space-between;color:#444"><span class="date"></span><span>노포천년 서비스 기획 보고서</span><span></span></div>',
        footer_template='<div style="font-size:8px;width:100%;padding:0 16mm;display:flex;justify-content:space-between;color:#444"><span>https://github.com/ohdangchan/NopoCheonnyeon</span><span><span class="pageNumber"></span>/<span class="totalPages"></span></span></div>',
    )
    b.close()
print('ok', OUT, os.path.getsize(OUT))

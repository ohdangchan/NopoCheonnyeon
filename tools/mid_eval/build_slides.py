"""중간 평가 기획안 ② 발표자료 HTML (형식: 참고 문서 '발표자료_권도형.html' — 페이지 넘김 슬라이드)
참고 파일의 머리(CSS)·스크립트를 그대로 쓰고, 본문만 노포천년 내용으로 바꾼다.
실행: python tools/mid_eval/build_slides.py"""
import base64
import glob
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import assets  # noqa: E402

REF = sorted(glob.glob(os.path.join(assets.ROOT, 'ex_copy', '*.html')), key=os.path.getsize)[0]
OUT = os.path.join(assets.OUT_DIR, '발표자료_노포천년.html')

src = open(REF, encoding='utf-8').read()
head = src[:src.index('<body')]
script = src[src.index('<script'):]

# 색을 노포천년 팔레트(청록·인주·놋쇠)로 바꾼다. 구조·크기·움직임은 그대로.
for a, b in [
    ('<title>일주일 AI 네이티브 앱 배포 · 페이지 발표</title>', '<title>노포 천년 미식가 · 중간 평가 발표</title>'),
    ('--accent:#bf4800;', '--accent:#C23B2A;'),
    ('--g1:#ff9f0a;--g2:#ff375f;--g3:#8e5cff;', '--g1:#D9A22C;--g2:#C23B2A;--g3:#1F6B63;'),
    ('color:#ff9f0a;letter-spacing:.02em', 'color:#D9A22C;letter-spacing:.02em'),
    ('rgba(255,138,30,.55),rgba(255,138,30,0)', 'rgba(217,162,44,.55),rgba(217,162,44,0)'),
    ('rgba(142,92,255,.55),rgba(142,92,255,0)', 'rgba(31,107,99,.65),rgba(31,107,99,0)'),
    ('rgba(255,55,95,.45),rgba(255,55,95,0)', 'rgba(194,59,42,.5),rgba(194,59,42,0)'),
    ('linear-gradient(135deg,#ff9f0a,#ff5e3a)', 'linear-gradient(135deg,#D9A22C,#C23B2A)'),
    ('linear-gradient(135deg,#0a84ff,#5e5ce6)', 'linear-gradient(135deg,#1F6B63,#17504A)'),
    ('#0071e3', '#1F6B63'), ('#e6f0ff', '#d3e5e0'), ('#0066cc', '#17504A'),
    ('#ff8a1f', '#C23B2A'), ('#fff1e0', '#f6e8c4'), ('#c4560b', '#9a3a1d'),
]:
    head = head.replace(a, b)
head += '\n<style>.shotrow{display:flex;gap:18px;justify-content:center;align-items:flex-start}.shotrow img{height:min(44vh,420px);width:auto;border-radius:18px;border:1px solid var(--line);box-shadow:0 10px 30px rgba(0,0,0,.12)}.shotrow figure{margin:0;text-align:center;font-size:14px;color:var(--sub)}.shotrow figcaption{margin-top:8px}.qrbox img{display:block;width:128px;height:128px}</style>\n'


def img(im, quality=82):
    return assets.data_uri(im, quality)


def shot(key, cap, h=None):
    label, fn = assets.SCREENS[key] if key in assets.SCREENS else assets.DEVICE[key]
    return f'<figure><img src="{img(fn(380))}" alt="{label}"><figcaption>{cap}</figcaption></figure>'


def dev(key, cap):
    label, fn = assets.DEVICE[key]
    return f'<figure><img src="{img(fn(380))}" alt="{label}"><figcaption>{cap}</figcaption></figure>'


qr_img = img(assets.qr(256), 90)


def divider(n, title, sub):
    return f'''<section class="pg dark divider" id="s{n}">
  <div class="orb o1"></div><div class="orb o2"></div>
  <div class="center"><div class="dv-num grad">{n:02d}</div>
    <h2 class="dv-title">{title}</h2>
    <p class="dv-sub">{sub}</p></div>
</section>
'''


MAIN = f'''<body>
<nav class="nav"><span class="brand">노포 천년 미식가 × AI 네이티브</span><a href="#s1" data-s="s1">01</a><a href="#s2" data-s="s2">02</a><a href="#s3" data-s="s3">03</a><a href="#s4" data-s="s4">04</a><a href="#s5" data-s="s5">05</a><a href="#s6" data-s="s6">06</a><div class="pg-bar"></div></nav>
<main class="stage">
<section class="pg dark hero">
  <div class="orb o1"></div><div class="orb o2"></div><div class="orb o3"></div>
  <div class="center">
    <div class="eyebrow">AI 활용 서비스 개발 · 1인 프로젝트 · 중간 평가</div>
    <h1 class="h-title">3일 안에<br><span class="grad">노포 천년 미식가</span>를<br>폰에 설치할 수 있을까?</h1>
    <p class="h-sub">내가 먹은 노포의 세월을 모아 천 년을 채우는 안드로이드 앱 — 기획부터 릴리스 APK와 QR 배포까지 만든 기록</p>
    <div class="h-chips"><span>Kotlin</span><span>Jetpack Compose</span><span>Firebase</span><span>네이버 지도</span><span>ML Kit</span><span>Gemini</span><span>Claude Code</span></div>
  </div>
  <div class="hint">→ 키, 스크롤, 스와이프로 다음 페이지 <i>›</i></div>
</section>
<section class="pg sec" id="toc"><div class="wrap">
  <div class="kicker"><i>목차</i></div>
  <h2>발표 순서</h2>
  <div class="tocgrid"><a class="tocitem" href="#s1"><span class="grad">01</span><b>프로젝트 목표 · 기획 배경</b><small>3일, 실기기 APK, 빠른 검증</small></a><a class="tocitem" href="#s2"><span class="grad">02</span><b>프로젝트명 · 한 줄 소개</b><small>노포 천년 미식가 / AI 노포 도우미</small></a><a class="tocitem" href="#s3"><span class="grad">03</span><b>개발 배경 · 타겟 사용자</b><small>풀려는 문제와 사용자</small></a><a class="tocitem" href="#s4"><span class="grad">04</span><b>핵심 기능 · AI 활용 전략</b><small>서비스 플로우, 인증 규칙, AI 처리 구조</small></a><a class="tocitem" href="#s5"><span class="grad">05</span><b>시스템 아키텍처 · 기술 스택</b><small>앱 · Firebase · 지도 · AI 연동</small></a><a class="tocitem" href="#s6"><span class="grad">06</span><b>트러블슈팅 · 주요 성과</b><small>인증 판정 · 릴리스 빌드 · AI 키 · 바이브 코딩</small></a></div>
</div></section>

{divider(1, "프로젝트 목표 · 기획 배경", "3일, 실기기 APK, 빠른 검증")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>01</i> 프로젝트 목표 · 기획 배경</div>
  <h2>"3일 안에, 폰에 설치해 모든 기능이 도는 앱을 만들 수 있을까?"</h2>
  <p class="lead">데모 영상이 아니라 설치되는 APK. 그러려면 기능과 안정성이 실제 기기에서 검증되어야 한다.</p>
  <div class="body">
    <div class="row">
      <div class="col card b"><span class="tag">목표 1</span><h3>실제로 가서 쓰는 앱</h3><p>노포 앞에서 영수증을 찍고 도장을 받는 순간까지, 폰 하나로 끝나는 경험을 만든다.</p></div>
      <div class="col card b"><span class="tag">목표 2</span><h3>내가 쓰고 싶은 앱</h3><p>내가 가 본 오래된 가게를 한눈에 모아 보고 싶다는 개인적 불편에서 출발한다.</p></div>
      <div class="col card b"><span class="tag">목표 3</span><h3>만들고 → 설치하고 → 고친다</h3><p>실기기 QA와 자동 테스트로 결함을 먼저 잡고, 사용자 반응으로 다음 범위를 정한다.</p></div>
      <div class="card g qrcard"><span class="tag g">배포 완료</span><div class="qrbox"><img src="{qr_img}" alt="APK 다운로드 QR"></div><p>GitHub 릴리스 · APK</p></div>
    </div>
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 150" role="img" aria-label="기획, 개발, 실기기 QA, 배포, 반응 확인 후 개선 또는 보류로 이어지는 흐름">
        <defs><marker id="m1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mkb" d="M0,0L10,5L0,10z"/></marker></defs>
        <rect class="nb" x="0" y="40" width="170" height="64"/><text x="85" y="78" text-anchor="middle">기획 v0.4</text>
        <rect class="nb" x="230" y="40" width="170" height="64"/><text x="315" y="78" text-anchor="middle">빠른 개발</text>
        <rect class="nb" x="460" y="40" width="170" height="64"/><text x="545" y="78" text-anchor="middle">실기기 QA</text>
        <rect class="nb" x="690" y="40" width="170" height="64"/><text x="775" y="78" text-anchor="middle">APK · QR 배포</text>
        <rect class="ng" x="940" y="2" width="196" height="58"/><text class="gl" x="1038" y="37" text-anchor="middle">반응 보고 개선</text>
        <rect class="nw" x="940" y="88" width="196" height="58"/><text class="wl" x="1038" y="123" text-anchor="middle">보류 · 폐기</text>
        <path class="ab" d="M170 72H226" marker-end="url(#m1)"/><path class="ab" d="M400 72H456" marker-end="url(#m1)"/><path class="ab" d="M630 72H686" marker-end="url(#m1)"/>
        <path class="ab" d="M860 64L936 34" marker-end="url(#m1)"/><path class="ab" d="M860 80L936 114" marker-end="url(#m1)"/>
      </svg>
    </div>
  </div></div></section>

{divider(2, "프로젝트명 · 한 줄 소개", "노포 천년 미식가 / AI 노포 도우미")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>02</i> 프로젝트명 · 한 줄 소개</div>
  <h2>하나의 앱, 두 개의 축: "세월을 모은다"와 "말로 찾는다"</h2>
  <div class="body">
    <div class="row">
      <div class="col card b" style="padding:28px">
        <span class="tag">수집형 미식 패스포트</span>
        <h3 style="font-size:34px">노포 천년 미식가</h3>
        <p style="font-size:21px;color:var(--ink);margin-top:6px">가 본 노포의 업력(년)을 세월로 모아 1,000년을 채우는 안드로이드 앱</p>
        <ul style="margin-top:14px">
          <li>영수증 한 장으로 방문 인증 → 도장 → 세월 적립</li>
          <li>세월 100년마다 한 단계, 11단계 칭호</li>
          <li>인증한 사람만 남기는 평점·사진 리뷰</li>
        </ul>
      </div>
      <div class="col card g" style="padding:28px">
        <span class="tag g">AI 도우미</span>
        <h3 style="font-size:34px">AI 노포 도우미</h3>
        <p style="font-size:21px;color:var(--ink);margin-top:6px">검색창에 말로 물으면 후보 노포 안에서 골라 주는 대화형 탐색</p>
        <ul style="margin-top:14px">
          <li>"비 오는 날 혼자 먹기 좋은 국밥집" 말로 찾기</li>
          <li>1,000년까지 남은 세월을 채우는 천년 경로</li>
          <li>메뉴 룰렛 · 후기 요약 · 단골 충성도 점수</li>
        </ul>
      </div>
    </div>
    <div class="note">두 축 모두 <b>"공식 노포 데이터 1,277곳"</b> 위에서 돈다. 별점 대신 업력을 점수로 쓰고, 평가는 <b>다녀온 사람만</b> 남긴다.</div>
  </div></div></section>

{divider(3, "개발 배경 · 타겟 사용자", "풀려는 문제와 사용자")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>03</i> 개발 배경 · 필요성</div>
  <h2>노포는 많은데, "내가 어디를 다녀왔는지"는 남지 않는다</h2>
  <div class="body">
    <table>
      <thead><tr><th style="width:30%">문제</th><th style="width:38%">앱이 맡는 일</th><th>결과</th></tr></thead>
      <tbody>
        <tr><td><b>정보가 흩어져 있다</b><br>백년가게·오래가게 정보가 정부·지자체 자료로 따로 있다</td><td>공식 자료를 모아 앱에 넣고 지도·검색·필터로 제공</td><td>근처 노포를 지도에서 바로 확인</td></tr>
        <tr><td><b>기록이 남지 않는다</b><br>다녀온 가게가 메모·사진첩에 흩어진다</td><td>영수증 인증 → 도장·세월을 패스포트에 쌓음</td><td>세월 게이지와 도장 격자로 한눈에</td></tr>
        <tr><td><b>리뷰를 믿기 어렵다</b><br>가 보지 않고도 쓸 수 있는 평가가 많다</td><td>방문 인증한 사용자만 리뷰·평점 작성(서버 규칙으로도 차단)</td><td>"다녀온 사람의 평가"만 노출</td></tr>
      </tbody>
    </table>
    <div class="row">
      <div class="col card b"><div class="big" style="font-size:40px">1,277곳</div><div class="num">앱에 담은 백년가게·오래가게 카탈로그</div></div>
      <div class="col card b"><div class="big" style="font-size:40px">3번</div><div class="num">지도 핀 → 상세 → 인증, 도장까지 누르는 횟수</div></div>
      <div class="col card b"><div class="big" style="font-size:40px">0건</div><div class="num">서버에 저장하는 영수증 이미지·위치 좌표</div></div>
    </div>
  </div></div></section>
<section class="pg sec alt"><div class="wrap"><div class="kicker"><i>03</i> 타겟 사용자</div>
  <h2>주말에 노포를 찾아다니는 사람을 첫 사용자로</h2>
  <div class="body">
    <div class="row">
      <div class="col card b"><span class="tag">사용자 A · 첫 버전</span><h3>노포 탐방러</h3><p>새로운 노포를 찾아다니며 가 본 곳을 모으고 자랑하고 싶다. 도장·칭호·공유 카드가 보상이다.</p></div>
      <div class="col card b"><span class="tag">사용자 B · 다음 버전</span><h3>단골 기록러</h3><p>오래 다닌 가게가 있어 나만의 단골집을 남기고 싶다. 재방문 횟수와 충성도가 의미다.</p></div>
      <div class="col card b"><span class="tag">사용자 C · 이후</span><h3>타지·외국인 방문객</h3><p>믿을 수 있는 노포 목록을 찾는다. 공식 지정 표시와 번역(AI)이 필요하다.</p></div>
    </div>
    <div class="note"><b>공통 요구</b> — 가깝고 믿을 수 있는 노포를 찾고 · 다녀온 기록이 남고 · 말로 물어도 답해 주길 원한다</div>
  </div></div></section>

{divider(4, "핵심 기능 · AI 활용 전략", "서비스 플로우, 인증 규칙, AI 처리 구조")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>04</i> 핵심 기능 · 서비스 플로우</div>
  <h2>탐색 → 인증 → 도장 → 패스포트, 그리고 AI가 돕는 지점</h2>
  <div class="body">
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 440" role="img" aria-label="노포천년 서비스 플로우">
        <defs>
          <marker id="m2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mk" d="M0,0L10,5L0,10z"/></marker>
        </defs>
        <text class="z" x="0" y="18">기본 흐름 · 지도에서 도장과 세월까지</text>
        <rect class="n" x="0" y="34" width="196" height="96"/><text x="16" y="66">① 탐색</text><text class="s" x="16" y="92">지도 · 필터 · 검색</text><text class="s" x="16" y="114">가게 상세</text>
        <rect class="nb" x="235" y="34" width="196" height="96"/><text x="251" y="66">② 인증</text><text class="s" x="251" y="92">영수증(위치 미사용)</text><text class="s" x="251" y="114">안 되면 30m 사진</text>
        <rect class="n" x="470" y="34" width="196" height="96"/><text x="486" y="66">③ 도장</text><text class="s" x="486" y="92">연출 380ms · 진동</text><text class="s" x="486" y="114">세월 +N년</text>
        <rect class="n" x="705" y="34" width="196" height="96"/><text x="721" y="66">④ 패스포트</text><text class="s" x="721" y="92">세월 게이지·도장 격자</text><text class="s" x="721" y="114">칭호 11단계</text>
        <rect class="ng" x="940" y="34" width="196" height="96"/><text x="956" y="66">⑤ 리뷰·공유</text><text class="s" x="956" y="92">인증자만 평점·사진</text><text class="s" x="956" y="114">공유 카드 QR</text>
        <path class="a" d="M196 82H231" marker-end="url(#m2)"/><path class="a" d="M431 82H466" marker-end="url(#m2)"/><path class="a" d="M666 82H701" marker-end="url(#m2)"/><path class="a" d="M901 82H936" marker-end="url(#m2)"/>

        <text class="z" x="0" y="196">AI가 더해지는 지점 · ① 검색창, ② 촬영 실패, ④ 패스포트, 가게 상세에서 판단을 돕는다</text>
        <rect class="nb" x="0" y="212" width="360" height="110"/><text x="18" y="246">AI 노포 도우미</text><text class="s" x="18" y="274">질문 + 시각 + 남은 세월 + 후보 가게</text><text class="s" x="18" y="298">→ 답변과 추천 가게 카드</text>
        <rect class="nb" x="388" y="212" width="360" height="110"/><text x="406" y="246">메뉴 룰렛 · 후기 요약</text><text class="s" x="406" y="274">내 리뷰·음식 사진·타인 리뷰</text><text class="s" x="406" y="298">→ 메뉴 30개 · 특장점 · 충성도 점수</text>
        <rect class="nb" x="776" y="212" width="360" height="110"/><text x="794" y="246">촬영 도우미</text><text class="s" x="794" y="274">인식 실패한 영수증·간판 + 사유</text><text class="s" x="794" y="298">→ 원인과 다시 찍는 방법 한 문장</text>

        <rect class="nw" x="0" y="350" width="1136" height="80"/>
        <text class="wl" x="20" y="384">원칙</text>
        <text x="80" y="384" style="font-weight:500">AI는 후보를 고르고 설명하는 곳에만 쓴다. 날짜·거리·세월 같은 숫자는 앱이 계산한다.</text>
        <text class="s" x="80" y="410">실패하면 오류 문구를 보여 주고 직접 탐색·직접 인증으로 자연스럽게 넘어간다.</text>
      </svg>
    </div>
  </div></div></section>
<section class="pg sec alt"><div class="wrap"><div class="kicker"><i>04</i> 방문 인증 규칙</div>
  <h2>영수증은 위치 없이, 안 되면 30m 안에서 가게 사진으로</h2>
  <div class="body">
    <table>
      <thead><tr><th style="width:22%">경로</th><th style="width:40%">판정 규칙</th><th>개인정보 처리</th></tr></thead>
      <tbody>
        <tr><td><b>① 영수증 인증</b></td><td>(전화번호 일치) 또는 (상호 유사도 ≥ 0.70) 그리고 결제일시 24시간 이내 · 같은 영수증 해시 1회</td><td>기기 안 글자 인식, 이미지는 판정 후 즉시 폐기, 위치 미사용</td></tr>
        <tr><td><b>② 사진·위치 인증</b></td><td>거리 ≤ 30m 그리고 정확도 ≤ 30m → 간판·메뉴판 글자에서 가게명(≥ 0.60) 또는 전화번호</td><td>사진은 기기에만 저장, 좌표는 서버에 안 보냄, 모의 위치 거부</td></tr>
        <tr><td><b>③ 위치 확인 도장</b></td><td>글자 확인 3번 실패해도 30m 조건 통과 시 별도 표시로 기록</td><td>한자·붓글씨 간판에서 막히지 않게</td></tr>
        <tr><td><b>한도</b></td><td>하루 5곳 · 같은 가게 하루 1회 (서울 자정 기준)</td><td>몰아 찍기 완화</td></tr>
      </tbody>
    </table>
    <div class="row">
      <div class="col note"><b>정확도도 30m.</b> 정확도 ±80m인 위치로 "25m"라고 판정하면 실제로는 100m 밖일 수 있어, 거리와 정확도를 함께 본다.</div>
      <div class="col note"><b>기준값은 한 곳에.</b> 인증 상수는 <code>VerificationConfig</code>에만 있고 현장 시험 후 여기만 고친다.</div>
    </div>
  </div></div></section>
<section class="pg sec"><div class="wrap"><div class="kicker"><i>04</i> AI 도입 전략</div>
  <h2>기능마다 입력과 안전장치를 다르게 설계했다</h2>
  <div class="body">
    <table>
      <thead><tr><th style="width:17%">기능</th><th style="width:30%">기법</th><th style="width:20%">모델</th><th>핵심 설계</th></tr></thead>
      <tbody>
        <tr><td><b>노포 도우미</b></td><td>후보 압축 + 구조화 출력(JSON 스키마)</td><td>Gemini flash-lite → 예비 모델</td><td>앱이 후보를 먼저 골라 넘기고, 목록에 없는 가게 ID는 버림</td></tr>
        <tr><td><b>메뉴 룰렛</b></td><td>멀티모달(사진) + 제외 목록</td><td>Gemini flash-lite</td><td>이전 9라운드 메뉴를 제외, 최대 10번 = 서로 다른 메뉴 30개</td></tr>
        <tr><td><b>후기 분석·충성도</b></td><td>통계 기반 프롬프트</td><td>Gemini flash-lite</td><td>앱이 기준 점수를 계산하고 AI 보정은 ±12점으로 고정</td></tr>
        <tr><td><b>촬영 도우미</b></td><td>멀티모달 + 실패 사유</td><td>Gemini flash-lite</td><td>이미지 첨부, 한 문장 안내, 실패해도 인증은 막지 않음</td></tr>
      </tbody>
    </table>
    <div class="row">
      <div class="col note"><b>처음 사용 전 동의.</b> 영수증 글자·사진·후기가 외부 AI로 전송됨을 알리고, 동의 후에만 호출한다.</div>
      <div class="col note"><b>모델 이름은 한 곳.</b> 모델 상수를 한 군데에 두고, 응답이 느리면 25초 뒤 예비 모델로 한 번 더 시도한다.</div>
    </div>
  </div></div></section>

{divider(5, "시스템 아키텍처 · 기술 스택", "앱 · Firebase · 지도 · AI 연동")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>05</i> 시스템 아키텍처</div>
  <h2>서버 코드 없이 앱이 공식 SDK로 직접 연결하는 구조</h2>
  <p class="lead">3일 안에 설치되는 앱을 만들려고 별도 서버를 두지 않았다. 대신 Firestore 보안 규칙과 앱 안 판정으로 운영한다.</p>
  <div class="body">
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 380" role="img" aria-label="노포천년 아키텍처">
        <defs><marker id="m5" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mk" d="M0,0L10,5L0,10z"/></marker></defs>
        <rect class="zone" x="0" y="20" width="360" height="350"/><text class="z" x="16" y="44">FRONTEND · Android 앱 (Kotlin)</text>
        <rect class="nb" x="24" y="62" width="312" height="60"/><text x="40" y="98">feature · Compose 화면</text>
        <rect class="nb" x="24" y="140" width="312" height="60"/><text x="40" y="176">ViewModel · 상태(StateFlow)</text>
        <rect class="nb" x="24" y="218" width="312" height="60"/><text x="40" y="254">domain 규칙 · data 저장소</text>
        <rect class="n" x="24" y="296" width="312" height="56"/><text x="40" y="322">기기 DB · 대기열</text><text class="s" x="40" y="342">Room · DataStore · WorkManager</text>

        <rect class="zone" x="460" y="20" width="320" height="350"/><text class="z" x="476" y="44">BACKEND · Firebase</text>
        <rect class="n" x="484" y="62" width="272" height="64"/><text x="500" y="90">Firebase Auth</text><text class="s" x="500" y="112">이메일 · 비밀번호 + 닉네임</text>
        <rect class="n" x="484" y="144" width="272" height="64"/><text x="500" y="172">Cloud Firestore</text><text class="s" x="500" y="194">방문·리뷰·통계 + 보안 규칙</text>
        <rect class="n" x="484" y="226" width="272" height="64"/><text x="500" y="254">Crashlytics</text><text class="s" x="500" y="276">릴리스 크래시 수집</text>

        <rect class="zone" x="880" y="20" width="256" height="350"/><text class="z" x="896" y="44">외부 SDK · AI</text>
        <rect class="nw" x="904" y="62" width="208" height="90"/><text x="920" y="94">Gemini</text><text class="s" x="920" y="118">Firebase AI Logic</text><text class="s" x="920" y="138">키는 앱에 없음</text>
        <rect class="n" x="904" y="166" width="208" height="90"/><text x="920" y="198">네이버 지도 SDK</text><text class="s" x="920" y="222">MapView · 마커 · 필터</text>
        <rect class="n" x="904" y="270" width="208" height="84"/><text x="920" y="300">ML Kit · CameraX</text><text class="s" x="920" y="324">한국어 글자 인식(앱 포함)</text>

        <path class="a" d="M360 172H480" marker-end="url(#m5)"/><path class="a" d="M780 172H900" marker-end="url(#m5)"/>
        <text class="s" x="540" y="346">카탈로그 1,277곳은 앱에 포함, 서버에는 사용자 데이터만</text>
      </svg>
    </div>
  </div></div></section>
<section class="pg sec alt"><div class="wrap"><div class="kicker"><i>05</i> 기술 스택</div>
  <h2>사용한 언어 · 프레임워크 · LLM · DB</h2>
  <div class="body">
    <div class="row">
      <div class="col card b"><span class="tag">앱</span><ul><li>Kotlin 2.2 · Jetpack Compose · Material 3</li><li>MVVM + 단방향 흐름, 수동 주입</li><li>Navigation Compose · Coil</li><li>CameraX · Fused Location</li></ul></div>
      <div class="col card b"><span class="tag">데이터 · 백엔드</span><ul><li>Firebase Auth · Cloud Firestore</li><li>Room · DataStore · WorkManager</li><li>Firestore 보안 규칙(필드 화이트리스트)</li><li>별도 서버 없음</li></ul></div>
      <div class="col card w"><span class="tag w">AI · 인식</span><ul><li>Gemini (Firebase AI Logic)</li><li>ML Kit 한국어 글자 인식</li><li>JSON 스키마 응답 · 25초 제한</li><li>ZXing(QR 생성)</li></ul></div>
    </div>
    <div class="row">
      <div class="col card g"><span class="tag g">품질</span><ul><li>단위 테스트 291개 통과</li><li>린트 오류 0</li><li>실기기 2대(S9+·Z Fold) 확인</li></ul></div>
      <div class="col card g"><span class="tag g">배포</span><ul><li>릴리스 서명 APK · R8 · arm64</li><li>GitHub 공개 릴리스 + QR</li><li>수동 설치(알 수 없는 앱 허용)</li></ul></div>
      <div class="col card"><span class="tag h">개발 도구</span><ul><li>Claude Code · Android Studio</li><li>Stitch 시안 51화면 → Compose</li><li>QA 서브에이전트 · 문서 우선순위</li></ul></div>
    </div>
  </div></div></section>

{divider(6, "트러블슈팅 · 주요 성과", "인증 판정 · 릴리스 빌드 · AI 키 · 바이브 코딩")}
<section class="pg sec"><div class="wrap"><div class="kicker"><i>06</i> 트러블슈팅 ① 영수증 인증 판정</div>
  <h2>영수증 양식이 제각각이어도 "패턴"으로 읽는다</h2>
  <div class="body">
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 150" role="img" aria-label="촬영, 글자 인식, 필드 추출, 판정, 도장 순서">
        <defs><marker id="p1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mkb" d="M0,0L10,5L0,10z"/></marker></defs>
        <rect class="n" x="0" y="20" width="200" height="84"/><text x="16" y="52">① 앱 안 카메라</text><text class="s" x="16" y="78">갤러리 선택 없음</text>
        <rect class="nb" x="240" y="20" width="220" height="84"/><text x="256" y="52">② ML Kit 글자 인식</text><text class="s" x="256" y="78">기기 안 · 오프라인</text>
        <rect class="nb" x="500" y="20" width="200" height="84"/><text x="516" y="52">③ 필드 추출</text><text class="s" x="516" y="78">전화·사업자·일시·금액</text>
        <rect class="nw" x="740" y="20" width="170" height="84"/><text x="756" y="52">④ 판정</text><text class="s" x="756" y="78">전화 또는 상호 0.70</text>
        <rect class="ng" x="950" y="20" width="186" height="84"/><text x="966" y="52">⑤ 도장</text><text class="s" x="966" y="78">이미지는 즉시 폐기</text>
        <path class="ab" d="M200 62H236" marker-end="url(#p1)"/><path class="ab" d="M460 62H496" marker-end="url(#p1)"/><path class="ab" d="M700 62H736" marker-end="url(#p1)"/><path class="ab" d="M910 62H946" marker-end="url(#p1)"/>
        <text class="s" x="0" y="146">실패하면 → 가게 사진(30m) 인증 / AI 촬영 도우미가 원인 안내</text>
      </svg>
    </div>
    <table>
      <thead><tr><th style="width:26%">문제</th><th style="width:40%">해결</th><th>왜</th></tr></thead>
      <tbody>
        <tr><td><b>양식이 단말기마다 다름</b></td><td>자리가 아니라 패턴(전화·날짜 형식, 사업자번호 체크섬)으로 추출. 요일 괄호·두 자리 연도도 인식</td><td>위치로 찾으면 양식이 바뀔 때마다 깨진다</td></tr>
        <tr><td><b>법인명과 간판 이름이 다름</b></td><td>전화 일치 <b>또는</b> 상호 유사도 0.70, 카탈로그 별칭과도 비교</td><td>정상 방문이 이름 차이로 막히지 않게</td></tr>
        <tr><td><b>흐리거나 영수증이 없음</b></td><td>첫 화면부터 "가게 사진으로 인증" 버튼, 실패 시 AI가 원인 안내</td><td>한 번 탭으로 다른 경로로 전환</td></tr>
        <tr><td><b>남의 영수증·재사용</b></td><td>24시간 · 해시 중복 차단 · 하루 5곳</td><td>완전 차단은 불가해 피해를 제한</td></tr>
      </tbody>
    </table>
  </div></div></section>
<section class="pg sec alt"><div class="wrap"><div class="kicker"><i>06</i> 트러블슈팅 ② 릴리스에서만 나는 문제</div>
  <h2>디버그에서는 되는데 릴리스 APK에서 깨지는 일들</h2>
  <div class="body">
    <table>
      <thead><tr><th style="width:24%">증상</th><th style="width:40%">원인과 해결</th><th>배운 점</th></tr></thead>
      <tbody>
        <tr><td><b>릴리스 앱이 시작하자마자 종료</b></td><td>R8 최적화가 Crashlytics와 충돌(IllegalAccessError). 최적화는 끄고 축소·난독화만 사용(<code>-dontoptimize</code>)</td><td>QA는 항상 릴리스 서명 APK로</td></tr>
        <tr><td><b>내 위치가 늦게 잡힘</b></td><td>마지막 위치를 먼저 쓰는 빠른 측정(quickFix)과 "내 위치" 버튼에 지도 재중심 추가</td><td>실기기에서만 느껴지는 지연</td></tr>
        <tr><td><b>리뷰를 쓰면 이전 리뷰가 사라짐</b></td><td>문서 ID를 <code>가게_사용자_슬롯(1~5)</code>으로 바꿔 한 가게에 여러 리뷰</td><td>데이터 모델을 요구사항 변화에 맞춤</td></tr>
        <tr><td><b>도장 → 연출 순서가 어색함</b></td><td>도장 찍힘 → 충격 고리 → 폭죽 → 칭호 승급 순서로 연출을 정리</td><td>보상 순간에 연출 비용을 집중</td></tr>
      </tbody>
    </table>
    <div class="shotrow">{dev("dev_map", "실제 앱(릴리스) — 지도 탐색")}{dev("dev_passport", "실제 앱(릴리스) — 내 패스포트")}{dev("dev_chat", "실제 앱(릴리스) — AI 도우미")}</div>
  </div></div></section>
<section class="pg sec"><div class="wrap"><div class="kicker"><i>06</i> 트러블슈팅 ③ AI 호출 보안·안정성</div>
  <h2>공개 APK에 키를 넣지 않고도 AI가 그대로 동작하게</h2>
  <div class="body">
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 180" role="img" aria-label="앱에서 직접 호출하던 방식을 Firebase AI Logic으로 바꾸고 App Check 설정과 예비 모델을 더한 순서">
        <defs><marker id="w1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mkb" d="M0,0L10,5L0,10z"/></marker></defs>
        <rect class="nw" x="0" y="10" width="200" height="90"/><text x="16" y="42">① 처음</text><text class="s" x="16" y="66">앱에 Gemini 키</text><text class="s" x="16" y="86">공개 APK에 포함</text>
        <rect class="nb" x="236" y="10" width="220" height="90"/><text x="252" y="42">② 키 제거</text><text class="s" x="252" y="66">Firebase AI Logic</text><text class="s" x="252" y="86">키는 Firebase가 보관</text>
        <rect class="n" x="492" y="10" width="200" height="90"/><text x="508" y="42">③ 호출 실패</text><text class="s" x="508" y="66">App Check 토큰 무효</text><text class="s" x="508" y="86">콘솔에서 적용 해제</text>
        <rect class="nw" x="728" y="10" width="190" height="90"/><text x="744" y="42">④ 모델 붐빔</text><text class="s" x="744" y="66">25초 제한 +</text><text class="s" x="744" y="86">예비 모델 재시도</text>
        <rect class="ng" x="954" y="10" width="182" height="90"/><text x="970" y="42">⑤ 릴리스 교체</text><text class="s" x="970" y="66">키 없는 APK 확인</text><text class="s" x="970" y="86">기존 키 폐기</text>
        <path class="ab" d="M200 55H232" marker-end="url(#w1)"/><path class="ab" d="M456 55H488" marker-end="url(#w1)"/><path class="ab" d="M692 55H724" marker-end="url(#w1)"/><path class="ab" d="M918 55H950" marker-end="url(#w1)"/>
        <text class="s" x="0" y="146">남은 과제: 2026.11.02부터 AI Logic App Check 적용 의무 → 서버 프록시 또는 App Check 제공자 필요</text>
      </svg>
    </div>
    <table>
      <thead><tr><th style="width:24%">문제</th><th>해결</th></tr></thead>
      <tbody>
        <tr><td><b>키가 APK 안에 있음</b></td><td>호출을 Firebase AI Logic으로 옮기고 키·BuildConfig 제거. APK 안에서 키 문자열 0건 확인</td></tr>
        <tr><td><b>"App Check token is invalid"</b></td><td>콘솔 APIs 탭에서 AI Logic 적용 해제 후 폰에서 응답 확인(반영에 약 10분)</td></tr>
        <tr><td><b>모델이 붐벼 응답이 40초 넘게 멈춤</b></td><td>요청 제한 25초 + 예비 모델로 한 번 더 시도, 단위 테스트로 고정</td></tr>
      </tbody>
    </table>
  </div></div></section>
<section class="pg sec alt"><div class="wrap"><div class="kicker"><i>06</i> 바이브 코딩 ① 설정 구조</div>
  <h2>AI에게 "알아서 해 줘" 대신 문서 · 규칙 · 역할을 줬다</h2>
  <p class="lead">3일 안에 설치되는 품질을 내려면 AI가 매번 같은 기준으로 일하게 만들어야 했다.</p>
  <div class="body">
    <div class="svgwrap">
      <svg class="d" viewBox="0 0 1136 300" role="img" aria-label="기획안에서 PRD와 아키텍처를 거쳐 규칙과 QA 에이전트로 이어지는 구조">
        <defs><marker id="v1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path class="mk" d="M0,0L10,5L0,10z"/></marker></defs>
        <rect class="n" x="0" y="100" width="180" height="100"/><text x="16" y="134">기획안 v0.4</text><text class="s" x="16" y="160">컨셉 · 규칙 · 화면</text><text class="s" x="16" y="182">Stitch 시안 51개</text>
        <rect class="nb" x="220" y="100" width="200" height="100"/><text x="236" y="134">PRD · 아키텍처</text><text class="s" x="236" y="160">요구사항 ID · 수락 기준</text><text class="s" x="236" y="182">충돌하면 앞 문서 우선</text>
        <rect class="nb" x="480" y="6" width="300" height="84"/><text x="496" y="38">CLAUDE.md · 지켜야 할 규칙</text><text class="s" x="496" y="64">키·영수증·위치 금지, 문구는 strings.xml</text>
        <rect class="nb" x="480" y="108" width="300" height="84"/><text x="496" y="140">스토리 단위 개발</text><text class="s" x="496" y="166">한 번에 하나 · 테스트 먼저</text>
        <rect class="nb" x="480" y="210" width="300" height="84"/><text x="496" y="242">nopo-qa 에이전트</text><text class="s" x="496" y="268">테스트 · 린트 · 금지 규칙 점검</text>
        <rect class="ng" x="840" y="60" width="296" height="80"/><text x="856" y="92">코드</text><text class="s" x="856" y="118">feature / data / domain</text>
        <rect class="ng" x="840" y="160" width="296" height="80"/><text x="856" y="192">자동 · 실기기 검증</text><text class="s" x="856" y="218">291 테스트 · 릴리스 설치</text>
        <path class="a" d="M180 150H216" marker-end="url(#v1)"/><path class="a" d="M420 130L476 60" marker-end="url(#v1)"/><path class="a" d="M420 150H476" marker-end="url(#v1)"/><path class="a" d="M420 170L476 250" marker-end="url(#v1)"/><path class="a" d="M780 60L836 90" marker-end="url(#v1)"/><path class="a" d="M780 250L836 200" marker-end="url(#v1)"/>
      </svg>
    </div>
    <div class="row">
      <div class="col note"><b>문서가 먼저.</b> 판단이 애매하면 코드보다 PRD·아키텍처를 본다. 기준 우선순위는 PRD → 아키텍처 → 스토리 → 기획안 → 시안이다.</div>
      <div class="col note"><b>작게, 검증하며.</b> 스토리 하나씩 구현하고 테스트·빌드·실기기 설치로 끝낸다. 새 라이브러리는 이유를 말하고 먼저 묻는다.</div>
    </div>
  </div></div></section>
<section class="pg sec"><div class="wrap"><div class="kicker"><i>06</i> 주요 성과</div>
  <h2>기획한 기능을 구현해 릴리스 APK로 배포했다</h2>
  <div class="body">
    <div class="row">
      <div class="col card b"><div class="big" style="font-size:30px;white-space:nowrap;line-height:52px">10.19 → 10.21</div><div class="num">개발 시작 → 공개 릴리스·QR 배포</div></div>
      <div class="col card b"><div class="big">106</div><div class="num">커밋</div></div>
      <div class="col card b"><div class="big">291</div><div class="num">단위 테스트 통과 (실패 0)</div></div>
      <div class="col card b"><div class="big">1,277</div><div class="num">앱에 담은 노포 카탈로그</div></div>
    </div>
    <div class="row">
      <div class="col card"><h3>실제로 설치한다</h3><p>릴리스 서명 APK를 실기기에 설치하고 QR로 내려받게 했다. 디버그에서만 되는 기능은 실패로 본다.</p></div>
      <div class="col card"><h3>모르는 것은 미확인으로</h3><p>카메라 촬영·실제 장소·서버 규칙 에뮬레이터 시험은 "미확인"으로 분리해 QA 문서에 남겼다.</p></div>
      <div class="col card"><h3>AI는 고르고, 앱이 계산한다</h3><p>세월·거리·날짜는 앱이, 후보 선택·설명은 AI가. 실패해도 앱은 계속 쓸 수 있다.</p></div>
    </div>
  </div></div></section>
<section class="pg dark finale">
  <div class="orb o1"></div><div class="orb o3"></div>
  <div class="center"><div class="eyebrow">Q&amp;A</div>
    <h1 class="f-title grad">감사합니다</h1></div>
</section>
</main>
<div class="ctrl"><button class="prev" aria-label="이전 페이지">‹</button><span class="cnt">1 / 1</span><button class="next" aria-label="다음 페이지">›</button><button class="full" aria-label="전체 화면 켜기·끄기">전체 화면</button></div>
'''

os.makedirs(assets.OUT_DIR, exist_ok=True)
open(OUT, 'w', encoding='utf-8').write(head + MAIN + script)
print('ok', os.path.getsize(OUT))

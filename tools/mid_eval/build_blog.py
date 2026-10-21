"""중간 평가 기획안 ③ 제작기 HTML (형식: 참고 문서 '송파SeSAC 도서 대여 시스템은 이렇게 만들어졌습니다.html'
— 도서관 대출카드 모양의 장(章) 구성, 기록 + '암묵지 책갈피' 카드, 도식, 참고 자료)
참고 파일의 CSS를 그대로 쓰고 본문만 노포천년 내용으로 바꾼다. 실행: python tools/mid_eval/build_blog.py"""
import glob
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import assets  # noqa: E402

REF = sorted(glob.glob(os.path.join(assets.ROOT, 'ex_copy', '*.html')), key=os.path.getsize)[1]
OUT = os.path.join(assets.OUT_DIR, '노포 천년 미식가는 이렇게 만들어졌습니다.html')

src = open(REF, encoding='utf-8').read()
head = src[:src.index('<div class="wrap">')]
head = head.replace('<title>송파SeSAC 도서 대여 시스템 제작기</title>', '<title>노포 천년 미식가 제작기</title>')
# 이미지가 들어 있던 머리 쪽 data URI는 없다(본문에만 있음). 색 변수만 노포천년 팔레트로.
head = head.replace('--sprout:#178F55; --sprout-soft:#E2F3EA; --sky:#1C6FD1; --sky-soft:#E3EEFB;', '--sprout:#1F6B63; --sprout-soft:#D3E5E0; --sky:#17504A; --sky-soft:#E3EFEC;')
head = head.replace('--stamp:#C9372C; --stamp-soft:#FBE7E4; --amber:#A86A00; --amber-soft:#FBF0D9;', '--stamp:#C23B2A; --stamp-soft:#F4DCD6; --amber:#A86A00; --amber-soft:#F6E8C4;')
head = head.replace('"Gowun Batang","Nanum Myeongjo",Georgia,serif', '"Gowun Batang","Nanum Myeongjo","NanumMyeongjo",Georgia,serif')


def img(im, q=80):
    return assets.data_uri(im, q)


def fig(key, cap, w=300):
    pool = assets.SCREENS if key in assets.SCREENS else assets.DEVICE
    label, fn = pool[key]
    return f'<figure><img class="shot" src="{img(fn(w))}" alt="{label}"><figcaption>{cap}</figcaption></figure>'


def tacit(title, body, why):
    return f'''<div class="tacit"><div class="lbl">암묵지 책갈피</div><h3>{title}</h3><p>{body}</p><p class="why">{why}</p></div>'''


def head_of(callno, num, title, sub, anchor):
    return f'''<section class="ch" id="{anchor}">
  <div class="ch-head">
    <div class="callno">{callno}<b>{num}</b></div>
    <div><h2>{title}</h2><p class="sub">{sub}</p></div>
  </div>
  <div class="stack">'''


END = '  </div>\n</section>\n'

logo = img(assets.logo(112), 90)

BODY = f'''<div class="wrap">

<header class="hero">
  <div class="stack">
    <div class="appmark"><img src="{logo}" alt="노포천년 앱 아이콘"><span class="eyebrow">AI 코딩 도구로 만든 안드로이드 앱 제작 사례</span></div>
    <h1>노포 천년 미식가는 이렇게 만들어졌습니다</h1>
    <p class="lede">기획부터 디자인, 데이터 수집, 개발, 실기기 검증, 릴리스 APK 배포까지 3일 동안의 기록입니다. 기록에 적힌 <b>"무엇을 했는가"</b>보다, 적혀 있지 않은 <b>"왜 그렇게 했는가"</b>, 즉 만든 사람 머릿속에만 있던 판단(암묵지)을 함께 꺼내서 설명합니다.</p>
    <div class="tools">
      <span class="chip"><b>Stitch</b><i>UI 시안 51화면</i></span>
      <span class="chip"><b>Python 수집 스크립트</b><i>노포 데이터</i></span>
      <span class="chip"><b>Claude Code</b><i>개발</i></span>
      <span class="chip"><b>Android Studio</b><i>빌드·설치</i></span>
      <span class="chip"><b>Firebase</b><i>로그인 · DB · AI</i></span>
      <span class="chip"><b>GitHub</b><i>릴리스 · QR 배포</i></span>
    </div>
    <div class="links">
      <a href="https://github.com/ohdangchan/NopoCheonnyeon" target="_blank" rel="noopener">소스 저장소 ↗</a>
      <a href="https://github.com/ohdangchan/NopoCheonnyeon/releases/latest/download/nopo-cheonnyeon.apk" target="_blank" rel="noopener">APK 내려받기 ↗</a>
    </div>
  </div>
  <div class="hero-phones">
    {fig("dev_map", "지도 탐색", 260).replace('<figure>', '').replace('</figure>', '').replace('<figcaption>지도 탐색</figcaption>', '')}
    {fig("dev_passport", "패스포트", 260).replace('<figure>', '').replace('</figure>', '').replace('<figcaption>패스포트</figcaption>', '')}
    {fig("dev_chat", "AI 도우미", 260).replace('<figure>', '').replace('</figure>', '').replace('<figcaption>AI 도우미</figcaption>', '')}
  </div>
</header>

<nav class="toc" aria-label="목차"><div class="row">
  <a href="#map"><span>00</span>전체 지도</a>
  <a href="#plan"><span>01</span>기획</a>
  <a href="#design"><span>02</span>디자인</a>
  <a href="#data"><span>03</span>데이터 수집</a>
  <a href="#dev"><span>04</span>개발</a>
  <a href="#test"><span>05</span>테스트</a>
  <a href="#rule"><span>06</span>룰 추가</a>
  <a href="#bug"><span>07</span>버그 수정</a>
  <a href="#deploy"><span>08</span>배포</a>
  <a href="#phone"><span>09</span>실기기</a>
  <a href="#polish"><span>10</span>다듬기</a>
  <a href="#icon"><span>11</span>앱 아이콘</a>
  <a href="#summary"><span>✓</span>정리</a>
  <a href="#refs"><span>REF</span>참고 자료</a>
</div></nav>

<!-- 00 MAP -->
{head_of("MAP", "00", "전체 지도: 한 장으로 보는 제작 과정", "기록 문서의 순서를 그대로 따라갑니다. 단계마다 쓴 도구와, 그 단계에서 사람이 맡은 일을 함께 표시했습니다.", "map")}
    <div class="diagram">
      <div class="dt">제작 흐름 · 위: 사용한 도구 / 아래: 사람이 직접 한 판단</div>
      <svg viewBox="0 0 1000 300" role="img" aria-label="기획, 디자인, 데이터 수집, 개발, 테스트와 수정, 배포로 이어지는 제작 흐름">
        <defs>
          <marker id="a1" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" class="ah"/></marker>
          <marker id="a1s" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" class="ah-stamp"/></marker>
        </defs>
        <g font-size="13">
          <text x="80" y="34" text-anchor="middle" class="t-muted">기획안·PRD</text><text x="245" y="34" text-anchor="middle" class="t-muted">Stitch</text><text x="410" y="34" text-anchor="middle" class="t-muted">Python 스크립트</text><text x="575" y="34" text-anchor="middle" class="t-muted">Claude Code</text><text x="740" y="34" text-anchor="middle" class="t-muted">S9+ · Z Fold</text><text x="905" y="34" text-anchor="middle" class="t-muted">GitHub · QR</text>
        </g>
        <g>
          <rect x="20" y="50" width="120" height="70" rx="8" class="box-ink"/><rect x="185" y="50" width="120" height="70" rx="8" class="box-ink"/><rect x="350" y="50" width="120" height="70" rx="8" class="box-ink"/><rect x="515" y="50" width="120" height="70" rx="8" class="box-sky"/><rect x="680" y="50" width="120" height="70" rx="8" class="box-stamp"/><rect x="845" y="50" width="120" height="70" rx="8" class="box-sprout"/>
        </g>
        <g font-size="17" font-weight="700" text-anchor="middle">
          <text x="80" y="82">기획</text><text x="245" y="82">디자인</text><text x="410" y="82">데이터 수집</text><text x="575" y="82">개발</text><text x="740" y="82">테스트·수정</text><text x="905" y="82">배포</text>
        </g>
        <g font-size="12" text-anchor="middle" class="t-muted">
          <text x="80" y="104" class="t-muted">무엇을 만들까</text><text x="245" y="104" class="t-muted">어떻게 보일까</text><text x="410" y="104" class="t-muted">무엇을 채울까</text><text x="575" y="104" class="t-muted">AI가 코드 작성</text><text x="740" y="104" class="t-muted">직접 눌러보기</text><text x="905" y="104" class="t-muted">누구나 설치</text>
        </g>
        <g class="ln" marker-end="url(#a1)"><path d="M140 85H181"/><path d="M305 85H346"/><path d="M470 85H511"/><path d="M635 85H676"/><path d="M800 85H841"/></g>
        <path d="M740 120 C740 165 575 165 575 124" class="ln-stamp dash" marker-end="url(#a1s)"/>
        <text x="657" y="170" text-anchor="middle" font-size="12" class="t-stamp">버그·룰 추가 → 다시 개발 (여러 번 반복)</text>
        <line x1="20" y1="195" x2="965" y2="195" class="ln dash"/>
        <text x="20" y="218" font-size="12" font-weight="700" class="t-sprout">사람의 몫</text>
        <g font-size="12.5" text-anchor="middle">
          <text x="80" y="246">규칙을 숫자로</text><text x="80" y="264">정하기</text>
          <text x="245" y="246">시안을 고르되 PRD가</text><text x="245" y="264">우선임을 정하기</text>
          <text x="410" y="246">출처와 한계를</text><text x="410" y="264">표시하기</text>
          <text x="575" y="246">문서·규칙을 주고</text><text x="575" y="264">스토리 하나씩 맡기기</text>
          <text x="740" y="246">막혀야 할 곳까지</text><text x="740" y="264">실기기에서 확인</text>
          <text x="905" y="246">키 노출·권한을</text><text x="905" y="264">판단하고 승인</text>
        </g>
      </svg>
    </div>
    <div class="col stack">
      <p>이 프로젝트의 코드는 Claude Code가 대부분 작성했습니다. 그런데 완성까지 가는 길에는 AI가 대신 내려줄 수 없는 결정이 계속 나옵니다. "영수증에 위치를 써도 되나?", "30m는 어떻게 판정하지?", "이 시안과 PRD가 다르면 무엇이 우선인가?" 같은 것들입니다. 각 장에는 <b>기록</b>(실제로 한 일)과 <b>암묵지 책갈피</b>(그 일 뒤에 숨은 판단)가 함께 나옵니다.</p>
      <div class="analogy"><span class="k">비유</span><p>AI는 손이 아주 빠른 신입 개발자이고, 만든 사람은 팀장입니다. 신입은 시키면 무엇이든 빨리 해내지만, 무엇을 시킬지, 결과가 괜찮은지는 팀장이 판단해야 합니다. 이 프로젝트의 암묵지는 대부분 <b>"일 잘 시키는 팀장의 습관"</b>입니다.</p></div>
    </div>
{END}
<!-- 01 PLAN -->
{head_of("PLAN", "01", "기획: 만들고 싶은 것을 글로 적고, 바뀌면 문서를 고친다", "출발점은 한 줄이었습니다. 내가 먹은 노포의 세월을 모아 천 년을 채우자. 이 한 줄이 PRD, 아키텍처, 스토리, 시험 항목으로 풀렸습니다.", "plan")}
    <div class="wide2">
      <figure><img class="shot" src="{img(assets.SCREENS["map"][1](400))}" alt="지도 탐색 시안"><figcaption>시안 속 핵심 장면: 지도에서 노포를 찾고, 세월 +120년 보상을 먼저 본다.</figcaption></figure>
      <div class="prompt">
        <div class="bar"><span>기획 메모 (요약)</span><span>docs/nopo-planning.html</span></div>
<pre># 노포 천년 미식가
Android 앱 · 가 본 노포의 업력(년)을 세월로 모은다

## 규칙
- 방문 인증 1회 = 그 가게의 <span class="hl">업력(올해 − 창업연도)</span>만큼 세월
- 같은 가게는 처음 한 번만 적립, 재방문은 단골 횟수만
- 세월 <span class="hl">100년마다 칭호 한 단계</span> (11단계, 1,000년 = 천년 미식가)

## 인증
- <span class="hl">영수증은 위치를 쓰지 않는다</span>
- 영수증이 안 되면 가게 <span class="hl">30m 안</span>에서 간판·메뉴판 사진

## 평점
- 그 가게를 <span class="hl">방문 인증한 사람만</span> 평점·리뷰</pre>
      </div>
    </div>
    <div class="tacit-grid">
      {tacit("기획서가 곧 AI에게 줄 첫 번째 지시서다", "이 메모는 사람끼리 보는 기획서이면서, 나중에 Claude Code에게 주는 <b>작업 기준</b>이 됩니다. 그래서 PRD → 아키텍처 → 스토리 → 기획안 → 시안 순으로 우선순위를 정하고, 충돌하면 앞의 것을 따른다고 못 박았습니다.", "왜 중요한가: 같은 내용이 문서마다 조금씩 다르면 AI가 임의로 고릅니다. 우선순위를 정해 두면 판단이 일관됩니다.")}
      {tacit("규칙은 숫자로 정한다", "\"가까이에서\", \"최근 영수증\"은 AI가 대신 정해줄 수 없습니다. <b>30m, 24시간, 유사도 0.70, 하루 5곳</b>처럼 숫자가 있어야 \"인증 성공/실패\"를 기계가 판정합니다. 숫자는 <code>VerificationConfig</code> 한 곳에만 둡니다.", "왜 중요한가: 숫자가 비어 있으면 AI가 임의로 채우고, 현장 시험 뒤 바꾸려 해도 코드 곳곳을 찾아야 합니다.")}
      {tacit("기능은 \"왜\"에서 나온다", "\"위치 인증을 넣어줘\" 대신 <b>\"영수증으로 방문을 확인하면 위치 정보가 필요 없다\"</b>고 이유부터 적었습니다. 그 결과 영수증 인증은 위치 권한이 없어도 동작하고, 좌표를 서버에 저장하지 않는 구조가 자연스럽게 나왔습니다.", "왜 중요한가: 이유를 함께 주면 AI가 개인정보 처리 같은 관련 세부 사항까지 챙깁니다.")}
      {tacit("보상은 한 순간에 몰아준다", "별점 대신 \"세월\"을 점수로 쓰고, 가장 두근거리는 순간인 <b>식후 도장</b>에 연출 비용을 모았습니다. 도장 380ms, 숫자 600ms 카운트, 진동 1회처럼 연출도 숫자로 정했습니다.", "왜 중요한가: 연출은 취향이 아니라 설계입니다. 숫자로 적으면 구현과 검수가 쉬워집니다.")}
    </div>
    <div class="diagram">
      <div class="dt">기획에 숨어 있는 "방문과 리뷰의 상태" · 이 그림이 머릿속에 있었기 때문에 리뷰 버튼이 인증 화면으로 이어집니다</div>
      <svg viewBox="0 0 1000 200" role="img" aria-label="미방문, 오늘 인증, 리뷰 작성 가능으로 이어지는 상태">
        <defs><marker id="b1" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" class="ah"/></marker></defs>
        <rect x="20" y="60" width="200" height="80" rx="8" class="box"/><text x="120" y="96" text-anchor="middle" font-size="16" font-weight="700">미방문</text><text x="120" y="120" text-anchor="middle" font-size="12" class="t-muted">리뷰 버튼 → 인증 화면</text>
        <rect x="300" y="60" width="200" height="80" rx="8" class="box-sky"/><text x="400" y="96" text-anchor="middle" font-size="16" font-weight="700">오늘 인증 성공</text><text x="400" y="120" text-anchor="middle" font-size="12" class="t-muted">도장 연출 → 세월 적립</text>
        <rect x="580" y="60" width="200" height="80" rx="8" class="box-sprout"/><text x="680" y="96" text-anchor="middle" font-size="16" font-weight="700">리뷰 편집기</text><text x="680" y="120" text-anchor="middle" font-size="12" class="t-muted">별점 · 글 · 사진 3장</text>
        <rect x="810" y="60" width="170" height="80" rx="8" class="box-stamp"/><text x="895" y="96" text-anchor="middle" font-size="16" font-weight="700">서버 규칙</text><text x="895" y="120" text-anchor="middle" font-size="12" class="t-muted">방문 문서 없으면 거부</text>
        <g class="ln" marker-end="url(#b1)"><path d="M220 100H296"/><path d="M500 100H576"/><path d="M780 100H806"/></g>
      </svg>
    </div>
{END}
<!-- 02 DESIGN -->
{head_of("DSGN", "02", "디자인: Stitch 시안 51화면을 만들고, 어디까지 따를지 정한다", "골목 간판의 청록, 영수증의 회녹색 종이, 도장의 붉은 인주. 색과 글꼴을 먼저 정하고 화면을 만들었습니다.", "design")}
    <div class="phones four">
      {fig("map", "① 지도 탐색", 300)}
      {fig("store", "② 가게 상세", 300)}
      {fig("receipt", "③ 영수증 인증", 300)}
      {fig("stamp", "④ 도장 연출", 300)}
    </div>
    <div class="phones four">
      {fig("passport", "⑤ 내 패스포트", 300)}
      {fig("review", "⑥ 리뷰 작성", 300)}
      {fig("title", "⑦ 칭호 승급", 300)}
      {fig("share", "⑧ 공유 카드", 300)}
    </div>
    <div class="tacit-grid">
      {tacit("시안은 참고, 기준은 문서다", "Stitch가 만든 화면에는 PRD에 없는 요소(예: 다른 서비스의 기능)가 섞일 수 있습니다. 그래서 <code>design/stitch/PRD-차이점.md</code>를 만들어 <b>\"무엇을 따르고 무엇을 버릴지\"</b>를 화면마다 적고, 화면을 만들기 전에 반드시 읽게 했습니다.", "왜 중요한가: 시안을 그대로 구현하면 범위가 불어납니다. 기준을 문서로 남겨야 AI가 같은 판단을 합니다.")}
      {tacit("색은 값이 아니라 토큰으로", "시안 HTML의 색 값을 복사하지 않고 <code>sign</code>, <code>primary</code>, <code>stamp</code>, <code>brass</code> 같은 <b>이름 있는 토큰</b>으로 옮겼습니다. 다크 모드는 같은 이름의 어두운 색을 한 벌 더 둡니다.", "왜 중요한가: 색을 바꿀 때 한 곳만 고치면 되고, 라이트·다크 모두 대비 4.5:1을 확인하기 쉽습니다.")}
    </div>
    <div class="diagram">
      <div class="dt">디자인 시스템 · 골목 간판과 인주</div>
      <svg viewBox="0 0 1000 120" role="img" aria-label="색 토큰 여섯 가지">
        <g font-size="13" text-anchor="middle">
          <rect x="20" y="20" width="140" height="56" rx="8" fill="#17504A"/><text x="90" y="104">간판 청록 #17504A</text>
          <rect x="185" y="20" width="140" height="56" rx="8" fill="#1F6B63"/><text x="255" y="104">포인트 청록 #1F6B63</text>
          <rect x="350" y="20" width="140" height="56" rx="8" fill="#C23B2A"/><text x="420" y="104">인주 빨강 #C23B2A</text>
          <rect x="515" y="20" width="140" height="56" rx="8" fill="#D9A22C"/><text x="585" y="104">놋쇠 노랑 #D9A22C</text>
          <rect x="680" y="20" width="140" height="56" rx="8" fill="#EDF0EA" stroke="#ccd4cc"/><text x="750" y="104">종이 바탕 #EDF0EA</text>
          <rect x="845" y="20" width="140" height="56" rx="8" fill="#F8F9F5" stroke="#ccd4cc"/><text x="915" y="104">카드 면 #F8F9F5</text>
        </g>
      </svg>
    </div>
{END}
<!-- 03 DATA -->
{head_of("DATA", "03", "데이터 수집: 공식 노포 1,277곳을 앱 안에 넣기", "소상공인마당의 백년가게와 서울시 오래가게를 모아 카탈로그(stores.json)로 만들었습니다.", "data")}
    <div class="wide2">
      <div class="prompt">
        <div class="bar"><span>수집 파이프라인 (요약)</span><span>tools/*.py</span></div>
<pre>data/raw/*.jsonl            <span class="hl">수집 원본</span>
  └ tools/build_catalog.py  → data/nopo_stores.csv
                            → app/src/main/assets/<span class="hl">stores.json</span>
tools/geocode.py            주소 → 좌표 (네이버/카카오)
tools/import_official_list.py  공식 CSV와 대조해 빠진 가게 보충
tools/fetch_stories.py      가게 이야기(소개글) 수집

전체 1,277곳 · 음식점 834 · 좌표 1,181 · 이야기 1,138</pre>
      </div>
      <div class="stack">
        <div class="tacit"><div class="lbl">암묵지 책갈피</div><h3>모르는 값은 모른다고 표시한다</h3><p>창업연도를 모르는 40곳은 임의로 채우지 않고 <b>업력 30년(공식 지정 최소 기준)</b>으로 계산하며 화면에 "업력 확인 중"이라고 보여 줍니다. 좌표가 없는 가게는 지도에서 빼고 "위치 준비 중"으로 목록에만 남깁니다.</p><p class="why">왜 중요한가: 데이터를 그럴듯하게 메우면 세월 합계가 틀어지고, 틀린 기록은 되돌리기 어렵습니다.</p></div>
      </div>
    </div>
    <div class="tacit-grid">
      {tacit("출처를 정하고 한계를 적는다", "카탈로그 항목마다 <code>sourceUrl</code>을 두고, 설정 화면에 카탈로그 버전·가게 수·출처를 보여 줍니다. 소상공인마당의 창업일이 \"현재 사업자 기준\"이라는 한계도 문서에 적었습니다.", "왜 중요한가: 공공 데이터는 시간이 지나면 달라집니다. 한계를 적어 두면 나중에 숫자를 의심할 근거가 생깁니다.")}
      {tacit("서버가 아니라 앱 안에 둔다", "카탈로그는 앱에 포함해 첫 실행 때 기기 DB에 넣습니다. 서버에는 사용자가 만든 데이터만 저장합니다.", "왜 중요한가: 무료 요금제의 읽기 한도를 지키고, 비행기 모드에서도 지도와 목록이 동작합니다.")}
    </div>
{END}
<!-- 04 DEV -->
{head_of("DEV", "04", "개발: 문서와 규칙을 주고, 스토리 하나씩 맡긴다", "Claude Code에게 \"앱 만들어줘\"라고 하지 않았습니다. CLAUDE.md의 규칙, 스토리 목록, 시험 항목을 주고 한 번에 하나씩 구현·검증하게 했습니다.", "dev")}
    <div class="wide2">
      <div class="prompt">
        <div class="bar"><span>CLAUDE.md 중 "반드시 지킬 것" (요약)</span><span>Claude Code</span></div>
<pre>- 사용자에게 보이는 문구는 strings.xml, 색은 테마 토큰만
- 인증 기준값은 <span class="hl">VerificationConfig 한 곳</span>에만
- <span class="hl">영수증 인증은 위치를 쓰지 않는다.</span> 영수증 이미지는 저장하지 않는다
- 위치 좌표와 영수증 내용은 서버에 저장·로그 금지
- 키·비밀번호를 코드나 로그에 쓰지 않는다
- 기기 기능·서버 호출은 withTimeout과 AppError 변환으로 감싼다
- 새 라이브러리는 <span class="hl">이유를 말하고 먼저 묻는다</span>
- 스토리를 마치면 nopo-qa 서브에이전트로 검증</pre>
      </div>
      <div class="diagram">
        <div class="dt">앱 구조 · 화면은 그리기만, 규칙은 domain에</div>
        <svg viewBox="0 0 440 300" role="img" aria-label="feature, domain, data, platform 계층">
          <rect x="20" y="20" width="400" height="52" rx="8" class="box-sky"/><text x="36" y="52" font-size="15" font-weight="700">feature · Compose 화면 + ViewModel</text>
          <rect x="20" y="88" width="400" height="52" rx="8" class="box-stamp"/><text x="36" y="120" font-size="15" font-weight="700">domain · 업력·칭호·영수증·사진 판정 규칙 (테스트 먼저)</text>
          <rect x="20" y="156" width="400" height="52" rx="8" class="box-sprout"/><text x="36" y="188" font-size="15" font-weight="700">data · Room · Firestore · AI · 대기열 동기화</text>
          <rect x="20" y="224" width="400" height="52" rx="8" class="box"/><text x="36" y="256" font-size="15" font-weight="700">platform · 카메라 · 위치 · 글자 인식 · 공유 카드</text>
        </svg>
      </div>
    </div>
    <div class="tacit-grid">
      {tacit("규칙은 파일로 남겨야 매번 지켜진다", "대화 중에 한 번 말한 규칙은 다음 세션에서 잊힙니다. 그래서 <code>CLAUDE.md</code>에 \"반드시 지킬 것\"을 적고, 작업 방식(한 번에 스토리 하나, 계획 5줄, 테스트 후 빌드·설치)까지 순서로 못 박았습니다.", "왜 중요한가: 규칙이 코드와 같은 저장소에 있으면 사람도 AI도 같은 기준으로 일합니다.")}
      {tacit("검증은 만든 쪽과 다른 쪽이 한다", "구현이 끝나면 <code>nopo-qa</code> 에이전트가 테스트·린트·금지 규칙(위치 사용, 키 노출 등)을 점검해 보고합니다. 쓴 사람이 스스로 검사하면 놓치기 쉽습니다.", "왜 중요한가: 정적 점검에서 \"리뷰 폭탄\"과 \"필드 변조\"가 높음으로 나왔고 모두 수정·재배포했습니다.")}
      {tacit("모르면 묻고, 추측으로 채우지 않는다", "새 라이브러리, 버전 업그레이드, 사용자 판단이 필요한 값은 임의로 정하지 않고 먼저 물었습니다. 사용자가 \"묻지 말고 계속\"이라고 한 때에도 키 노출 같은 위험한 결정은 확인 뒤에 진행했습니다.", "왜 중요한가: 권한이 있어도 되돌릴 수 없는 일은 한 번 더 확인해야 합니다.")}
    </div>
{END}
<!-- 05 TEST -->
{head_of("TEST", "05", "테스트: 되는 길과 막혀야 하는 길을 모두 걸어본다", "단위 테스트 291개가 통과해도 끝이 아닙니다. 사람이 폰에서 같은 흐름을 처음부터 끝까지 눌러봤습니다.", "test")}
    <div class="phones three" style="max-width:760px">
      {fig("dev_map", "① 릴리스 앱: 지도 탐색", 320)}
      {fig("dev_passport", "② 릴리스 앱: 패스포트 (세월 668년)", 320)}
      {fig("dev_chat", "③ 릴리스 앱: AI 도우미 응답", 320)}
    </div>
    <div class="diagram">
      <div class="dt">"막혀야 하는 길" 시험표 · 정상 흐름보다 이쪽이 더 중요합니다</div>
      <svg viewBox="0 0 1000 250" role="img" aria-label="막혀야 하는 시험 6가지">
        <g font-size="14">
          <rect x="20" y="20" width="300" height="60" rx="8" class="box-stamp"/><text x="36" y="48" font-weight="700">같은 영수증 두 번</text><text x="36" y="68" class="t-muted" font-size="12">→ E_RECEIPT_DUP로 거부</text>
          <rect x="350" y="20" width="300" height="60" rx="8" class="box-stamp"/><text x="366" y="48" font-weight="700">24시간 지난 영수증</text><text x="366" y="68" class="t-muted" font-size="12">→ E_RECEIPT_EXPIRED, 사진 인증 안내</text>
          <rect x="680" y="20" width="300" height="60" rx="8" class="box-stamp"/><text x="696" y="48" font-weight="700">하루 6번째 가게</text><text x="696" y="68" class="t-muted" font-size="12">→ E_DAILY_LIMIT</text>
          <rect x="20" y="100" width="300" height="60" rx="8" class="box-stamp"/><text x="36" y="128" font-weight="700">35m 떨어진 가게</text><text x="36" y="148" class="t-muted" font-size="12">→ E_FAR + 현재 거리</text>
          <rect x="350" y="100" width="300" height="60" rx="8" class="box-stamp"/><text x="366" y="128" font-weight="700">방문 없이 리뷰 쓰기</text><text x="366" y="148" class="t-muted" font-size="12">→ 서버 규칙 거부, 인증 화면 안내</text>
          <rect x="680" y="100" width="300" height="60" rx="8" class="box-stamp"/><text x="696" y="128" font-weight="700">AI 키 없음·시간 초과</text><text x="696" y="148" class="t-muted" font-size="12">→ E_AI_FAILED, 기능은 계속 사용</text>
        </g>
        <text x="20" y="200" font-size="13" class="t-muted">서버 규칙 에뮬레이터 시험(시나리오 18개)은 작성했으나 이 PC에서 실행하지 못해 "미확인(환경)"으로 기록했습니다.</text>
      </svg>
    </div>
    <div class="tacit">
      <div class="lbl">암묵지 책갈피</div>
      <h3>"확인했다"와 "확인하지 못했다"를 분리해서 적는다</h3>
      <p>QA 문서의 상태는 네 가지입니다. <b>PASS</b>(기기에서 확인), <b>PASS(자동)</b>(자동 시험), <b>미확인(사용자)</b>(카메라·실제 장소처럼 사람이 해야 함), <b>미확인(환경)</b>(이 PC에서 못 돌림). 보지 못한 것을 본 것처럼 쓰지 않았습니다.</p>
      <p class="why">강의 질문: 여러분의 서비스에서 "AI가 확인했다"는 보고 중, 실제로 사람이 눈으로 본 것은 몇 개인가요?</p>
    </div>
{END}
<!-- 06 RULE -->
{head_of("RULE", "06", "룰 추가: 써보니까 보이는 규칙", "처음 기획에 없던 규칙이 실기기에서 하나씩 나왔습니다.", "rule")}
    <div class="prompt">
      <div class="bar"><span>추가한 룰</span><span>Claude Code</span></div>
<pre>- 리뷰를 쓰고 또 쓰면 <span class="hl">이전 리뷰가 사라진다</span> → 한 가게에 여러 개(최대 5개) 남기게
- "리뷰 남기기"를 누르면 오늘 인증하지 않았을 때 <span class="hl">방문 인증 화면으로</span> 보내고, 성공하면 리뷰 편집기
- 간판·메뉴판 인증에도 영수증과 같은 <span class="hl">스캔 연출(레이저 → 형광펜)</span>과 실패 원인 안내
- 글자 확인이 3번 실패해도 30m 안이면 <span class="hl">위치 확인 도장</span></pre>
    </div>
    <div class="diagram">
      <div class="dt">"리뷰 남기기"를 눌렀을 때 · 앱이 판단하는 순서</div>
      <svg viewBox="0 0 1000 220" role="img" aria-label="리뷰 남기기 판단 순서">
        <defs><marker id="c1" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" class="ah"/></marker></defs>
        <rect x="20" y="80" width="190" height="60" rx="8" class="box-ink"/><text x="115" y="116" text-anchor="middle" font-size="15" font-weight="700">리뷰 남기기 탭</text>
        <rect x="270" y="80" width="200" height="60" rx="8" class="box-sky"/><text x="370" y="108" text-anchor="middle" font-size="14" font-weight="700">오늘 인증했나?</text><text x="370" y="128" text-anchor="middle" font-size="11" class="t-muted">시연용 도장은 제외</text>
        <rect x="560" y="20" width="200" height="56" rx="8" class="box-sprout"/><text x="660" y="54" text-anchor="middle" font-size="14" font-weight="700">예 → 편집기 바로 열기</text>
        <rect x="560" y="140" width="200" height="56" rx="8" class="box-stamp"/><text x="660" y="168" text-anchor="middle" font-size="14" font-weight="700">아니오 → 인증 화면</text><text x="660" y="186" text-anchor="middle" font-size="11" class="t-muted">성공하면 편집기 자동 열림</text>
        <g class="ln" marker-end="url(#c1)"><path d="M210 110H266"/><path d="M470 100L556 56"/><path d="M470 120L556 164"/></g>
      </svg>
    </div>
    <div class="tacit-grid">
      {tacit("처음부터 완벽한 기획서는 없다", "\"리뷰를 또 쓰면 이전 리뷰가 사라지는\" 문제는 폰에서 두 번 써 보기 전에는 보이지 않았습니다. 문서 ID를 <code>가게_사용자_슬롯</code>으로 바꾸고 서버 규칙·시험·PRD(FR-REV-10)까지 함께 고쳤습니다.", "왜 중요한가: 규칙이 바뀌면 코드만이 아니라 문서와 서버 규칙도 같이 고쳐야 다음 작업이 어긋나지 않습니다.")}
      {tacit("요청한 흐름을 그대로 옮기지 말고 상태로 그린다", "\"리뷰는 인증 뒤에\"라는 요청을 <b>미방문 → 오늘 인증 → 편집기</b> 상태 그림으로 먼저 그린 뒤 구현했습니다. 시연용 도장은 인증으로 치지 않는다는 예외도 이 그림에서 나왔습니다.", "왜 중요한가: 상태로 그리면 \"이 경우는?\"이라는 질문이 구현 전에 나옵니다.")}
    </div>
{END}
<!-- 07 BUG -->
{head_of("BUG", "07", "버그 수정: 에러 원문을 그대로 주고, 원인을 측정한다", "디버그에서는 되는데 릴리스에서만 깨지는 일, 폰에서만 느껴지는 지연이 가장 늦게 발견됐습니다.", "bug")}
    <div class="diagram">
      <div class="dt">버그 리포트 네 칸 · 어디서 / 무엇을 / 무엇은 됐고 / 무엇이 문제인가</div>
      <svg viewBox="0 0 1000 160" role="img" aria-label="버그 리포트 네 칸">
        <g font-size="14">
          <rect x="20" y="30" width="225" height="90" rx="8" class="box-ink"/><text x="36" y="58" font-weight="700">① 어디서</text><text x="36" y="82" class="t-muted" font-size="12">릴리스 APK, S9+, 앱 시작</text>
          <rect x="262" y="30" width="225" height="90" rx="8" class="box-ink"/><text x="278" y="58" font-weight="700">② 무엇을 했나</text><text x="278" y="82" class="t-muted" font-size="12">앱 실행 직후</text>
          <rect x="504" y="30" width="225" height="90" rx="8" class="box-sprout"/><text x="520" y="58" font-weight="700">③ 무엇은 됐나</text><text x="520" y="82" class="t-muted" font-size="12">디버그 빌드는 정상</text>
          <rect x="746" y="30" width="234" height="90" rx="8" class="box-stamp"/><text x="762" y="58" font-weight="700">④ 문제 + 에러 원문</text><text x="762" y="82" class="t-muted" font-size="12">IllegalAccessError … 그대로</text>
        </g>
      </svg>
    </div>
    <div class="prompt">
      <div class="bar"><span>실제로 고친 문제들</span><span>docs/dev-status.md</span></div>
<pre>1. <span class="hl">릴리스 앱이 시작하자마자 종료</span>
   원인: R8 최적화 ↔ Crashlytics 충돌(IllegalAccessError) → -dontoptimize
2. 내 위치가 늦게 잡힘 → 마지막 위치를 먼저 쓰는 quickFix, 내 위치 버튼에 지도 재중심
3. 리뷰를 또 쓰면 이전 리뷰가 사라짐 → 리뷰 슬롯 1~5
4. <span class="hl">Gemini 키가 공개 APK에 포함</span> → Firebase AI Logic으로 이전, 기존 키 폐기
5. "App Check token is invalid" → 콘솔 적용 해제 후 확인(약 10분 반영)
6. 모델이 붐벼 응답이 40초 넘게 멈춤 → 25초 제한 + 예비 모델</pre>
    </div>
    <div class="tacit-grid">
      {tacit("증상이 아니라 로그를 본다", "\"AI가 안 돼요\"만으로는 원인을 알 수 없었습니다. 실패 시 <b>예외 종류와 짧은 메시지</b>(요청 내용·키는 제외)를 로그에 남기자 App Check 토큰, 모델 붐빔이 각각 다른 원인임을 바로 알았습니다.", "왜 중요한가: 같은 화면 문구(\"가져오지 못했어요\") 뒤에 원인이 여러 개 있을 수 있습니다.")}
      {tacit("가설이 틀리면 바로 버린다", "릴리스에서만 멈춘다고 R8 규칙을 의심해 보호 규칙을 넣어 봤지만, 로그는 모델 붐빔이었습니다. 효과가 입증되지 않은 규칙은 다시 빼고 다시 빌드했습니다.", "왜 중요한가: 입증되지 않은 설정은 나중에 아무도 이유를 모르는 짐이 됩니다.")}
    </div>
{END}
<!-- 08 DEPLOY -->
{head_of("DEPLOY", "08", "배포: 릴리스 APK와 QR, 그리고 공개 저장소", "스토어 등록 없이도 누구나 폰에 설치할 수 있게 GitHub 릴리스에 APK를 올리고 QR로 내려받게 했습니다.", "deploy")}
    <div class="wide2">
      <figure><img class="shot" src="{img(assets.qr(360), 90)}" alt="APK 다운로드 QR" style="max-width:260px;margin:auto"><figcaption>QR을 폰 카메라로 비추면 릴리스 APK 내려받기 주소가 열립니다. 앱의 패스포트 공유 카드에도 같은 QR이 들어갑니다.</figcaption></figure>
      <div class="prompt">
        <div class="bar"><span>배포 순서</span><span>PowerShell · gh</span></div>
<pre>1. 단위 테스트 + 린트
2. <span class="hl">assembleRelease</span> (R8, 릴리스 키 서명, arm64)
3. 폰에 덮어 설치: adb install -r app-release.apk
4. GitHub 릴리스 v0.1.0-demo 에 nopo-cheonnyeon.apk 업로드
5. 올린 파일과 빌드 산출물의 <span class="hl">크기·해시가 같은지</span> 확인
6. README · 공유 카드에 다운로드 QR</pre>
      </div>
    </div>
    <div class="tacit-grid">
      {tacit("공개하는 순간 비밀도 공개된다", "공개 저장소와 공개 APK에는 키·비밀번호가 없어야 합니다. 저장소 이력에 키 문자열이 없는지 먼저 확인했고, 사용자가 \"키가 노출돼도 된다\"고 승인한 데모용 빌드에만 AI 키를 넣었다가 <b>나중에 Firebase AI Logic으로 키 자체를 빼서</b> 릴리스를 교체했습니다.", "왜 중요한가: 승인받은 위험도 줄일 수 있으면 줄입니다. 기존 키는 폐기해야 합니다.")}
      {tacit("서명 키는 같은 키로 계속", "서명 키가 바뀌면 폰에서 덮어 설치가 안 되어 앱을 지우고 다시 깔아야 합니다. 그래서 키를 백업하고 디버그 빌드도 같은 키로 서명했습니다.", "왜 중요한가: 지우고 다시 설치하면 사용자의 로컬 기록(인증샷 등)이 사라집니다.")}
    </div>
{END}
<!-- 09 PHONE -->
{head_of("PHONE", "09", "실기기: 서로 다른 폰, 서로 다른 계정으로", "Galaxy S9+(Android 10, 구형)와 Z Fold(Android 17)에서 시험했습니다. 개발 PC에서 되는 것과 폰에서 되는 것은 다릅니다.", "phone")}
    <div class="phones three" style="max-width:760px">
      {fig("login", "① 로그인 시안", 300)}
      {fig("photo", "② 가게 사진 인증 시안", 300)}
      {fig("test_store", "③ 테스트 가게 시안 (진단 화면)", 300)}
    </div>
    <div class="tacit-grid">
      {tacit("진단 화면과 테스트 가게를 남긴다", "설정에서 앱 정보를 5번 누르면 진단 화면이 열리고, <b>현재 위치에 \"노포천년테스트식당\"</b>을 만들어 실제 인증 로직 그대로 시험할 수 있습니다. 시연 모드(검사 건너뛰기)는 실제 버그를 숨기기 때문에 만들지 않았습니다.", "왜 중요한가: 우회 로직 대신 실제 로직을 시험할 도구를 만들어야 현장에서 같은 버그를 봅니다.")}
      {tacit("카메라와 장소는 자동화할 수 없다", "화면 조작은 adb로 자동화했지만 영수증 촬영과 실제 가게 앞 30m 시험은 사람이 해야 합니다. 그래서 체크리스트(V-01~V-08)로 사용자에게 넘기고 \"미확인(사용자)\"로 표시했습니다.", "왜 중요한가: 자동화할 수 없는 것을 명시하면 마지막 점검이 빠지지 않습니다.")}
    </div>
{END}
<!-- 10 POLISH -->
{head_of("POLISH", "10", "다듬기: AI 기능과 연출을 더하고, 하나씩 확인한다", "AI 노포 도우미, 메뉴 룰렛, 후기 분석, 단골 충성도, 번역, 촬영 도우미를 순서대로 붙였습니다.", "polish")}
    <div class="phones three" style="max-width:760px">
      {fig("title", "① 칭호 승급 연출 시안", 300)}
      {fig("profile", "② 유저 프로필 시안", 300)}
      {fig("dev_chat", "③ 실제 앱: 오늘의 노포 코스", 300)}
    </div>
    <div class="tacit-grid">
      {tacit("AI는 후보 안에서만 고르게 한다", "챗봇은 앱이 먼저 후보 가게를 압축해 넘기고, 답에 후보 목록에 없는 가게 ID가 있으면 버립니다. 세월·거리·시각은 앱이 계산해 전달합니다.", "왜 중요한가: 모델이 없는 가게를 만들어 내는 일(환각)을 구조로 막습니다.")}
      {tacit("한 번에 하나, 바꾸면 확인", "기능을 한꺼번에 붙이지 않고 하나씩 붙인 뒤 폰에서 확인하고 커밋했습니다. 10분마다 커밋·푸시해 PC가 꺼져도 작업이 남게 했습니다.", "왜 중요한가: 작은 단위로 확인해야 문제가 생겼을 때 원인이 좁혀집니다.")}
    </div>
{END}
<!-- 11 ICON -->
{head_of("ICON", "11", "앱 아이콘: 생성 스크립트로 만들어 다시 만들 수 있게", "청록 간판 바탕에 붉은 도장(\"노포\")을 얹었습니다. 손으로 그리지 않고 스크립트로 만들어 크기별 이미지를 한 번에 뽑습니다.", "icon")}
    <div class="wide2">
      <figure><img class="shot" src="{img(assets.logo(420), 90)}" alt="노포천년 앱 아이콘" style="max-width:260px;margin:auto"><figcaption>적응형 아이콘(바탕 + 도장 + 단색 버전), 옛 기기용 일반 아이콘도 함께 생성</figcaption></figure>
      <div class="prompt">
        <div class="bar"><span>tools/gen_app_icon.py (요약)</span><span>Python · Pillow</span></div>
<pre>바탕: 청록 그라데이션 + 옅은 가로 결(간판 판자)
전경: 붉은 둥근 네모 도장 + 놋쇠 테두리 + "노 / 포"
      나눔명조 ExtraBold, 6도 기울임, 잉크 닳은 잔점
출력: mipmap-mdpi~xxxhdpi (바탕·전경·단색)
      mipmap-anydpi/ic_launcher*.xml</pre>
      </div>
    </div>
    <div class="tacit">
      <div class="lbl">암묵지 책갈피</div>
      <h3>이미지도 "코드"로 만들면 고치기 쉽다</h3>
      <p>글자 크기를 줄이고 도장을 키우는 수정을 숫자 두 개로 끝냈고, 한 번 실행하면 모든 해상도가 같이 갱신됩니다. 폰 홈 화면에서 새 아이콘을 확인하고 릴리스 APK도 다시 올렸습니다.</p>
      <p class="why">왜 중요한가: 디자인 파일이 흩어지면 나중에 같은 모양을 다시 만들 수 없습니다.</p>
    </div>
{END}
<!-- SUMMARY -->
<section class="ch" id="summary">
  <div class="ch-head">
    <div class="callno">END<b>✓</b></div>
    <div><h2>정리: 가져갈 암묵지 열 가지</h2><p class="sub">코드를 대신 써 주는 시대에도 결과를 가른 것은 이런 습관들이었습니다.</p></div>
  </div>
  <div class="stack">
    <div class="sum">
      <div><span class="n">01 · 기획</span><h3>기획서를 지시서로 쓴다</h3><p>한 번 잘 정리한 문서를 모든 단계에서 재사용하고, 충돌하면 어느 문서가 우선인지 정합니다.</p></div>
      <div><span class="n">02 · 기획</span><h3>규칙은 숫자로</h3><p>30m, 24시간, 0.70, 하루 5곳. AI가 정할 수 없는 값은 사람이 정합니다.</p></div>
      <div><span class="n">03 · 디자인</span><h3>시안은 참고, 기준은 문서</h3><p>차이점 문서로 따를 것과 버릴 것을 화면마다 정합니다.</p></div>
      <div><span class="n">04 · 디자인</span><h3>값이 아니라 토큰</h3><p>색을 이름으로 옮겨 라이트·다크를 함께 관리합니다.</p></div>
      <div><span class="n">05 · 데이터</span><h3>모르면 모른다고</h3><p>창업연도 미상은 30년, 좌표 없음은 "위치 준비 중"으로 표시합니다.</p></div>
      <div><span class="n">06 · 개발</span><h3>규칙을 파일로, 스토리 하나씩</h3><p>CLAUDE.md와 시험 항목으로 매번 같은 기준을 주고 한 번에 하나만 맡깁니다.</p></div>
      <div><span class="n">07 · 테스트</span><h3>막혀야 할 길을 시도한다</h3><p>재사용 영수증, 한도 초과, 방문 없는 리뷰를 일부러 해봅니다.</p></div>
      <div><span class="n">08 · 버그</span><h3>네 칸 리포트 + 원문 로그</h3><p>어디서·무엇을·무엇은 됐고·무엇이 문제. 에러는 원문 그대로, 원인은 측정으로.</p></div>
      <div><span class="n">09 · 배포</span><h3>실제 기기, 실제 릴리스로</h3><p>디버그에서만 되는 기능은 실패로 보고, 공개 전에 비밀을 점검합니다.</p></div>
      <div><span class="n">10 · 보고</span><h3>본 것과 못 본 것을 나눈다</h3><p>PASS와 "미확인"을 구분해 적어 마지막 사람의 점검이 빠지지 않게 합니다.</p></div>
    </div>
    <div class="diagram">
      <div class="dt">이 프로젝트의 역할 분담</div>
      <svg viewBox="0 0 1000 200" role="img" aria-label="사람과 AI의 역할 분담">
        <rect x="20" y="30" width="460" height="140" rx="10" class="box-sprout"/><text x="40" y="62" font-size="16" font-weight="700" class="t-sprout">사람</text>
        <g font-size="14"><text x="40" y="92">· 무엇을 만들지, 규칙의 숫자</text><text x="40" y="116">· 위험한 결정(키 노출, 공개)의 승인</text><text x="40" y="140">· 카메라·실제 장소 확인, 최종 판정</text></g>
        <rect x="520" y="30" width="460" height="140" rx="10" class="box-sky"/><text x="540" y="62" font-size="16" font-weight="700" class="t-sky">AI (Claude Code · Gemini)</text>
        <g font-size="14"><text x="540" y="92">· 코드·테스트·문서 작성, 반복 작업</text><text x="540" y="116">· 후보 고르기·설명(앱 AI 기능)</text><text x="540" y="140">· 빌드·설치·로그 확인과 보고</text></g>
      </svg>
    </div>
  </div>
</section>

<!-- REFERENCES -->
<section class="ch" id="refs">
  <div class="ch-head">
    <div class="callno">REF<b>↗</b></div>
    <div><h2>참고 자료</h2><p class="sub">이 문서의 내용을 뒷받침하는 공식 문서와 널리 읽히는 글입니다. 이번 개발 중 직접 확인한 문서를 먼저 적었습니다.</p></div>
  </div>
  <div class="stack">
    <div class="legend">
      <span><span class="badge off">공식 문서</span> 도구·표준을 만든 곳의 문서</span>
      <span><span class="badge exp">전문가</span> 분야에서 널리 인용되는 글</span>
      <span><span class="badge first">직접 확인</span> 이번 개발 중 읽고 적용</span>
    </div>
    <div class="refs">
      <article class="ref">
        <div class="meta"><span class="badge off">공식 문서</span><span class="badge first">직접 확인</span><span>Firebase</span></div>
        <h4><a href="https://firebase.google.com/docs/ai-logic/app-check" target="_blank" rel="noopener">Firebase AI Logic · App Check <span class="ext">↗</span></a></h4>
        <p>App Check가 Gemini 호출을 보호하는 방식, 적용 설정 위치(Security → App Check → APIs), 디버그 제공자와 제한 사용 토큰을 설명합니다. 2026-11-02부터 적용이 의무가 된다는 안내도 이 문서에 있습니다.</p>
        <div class="tie">연결: <a href="#bug">07 버그 수정</a>의 "App Check token is invalid"</div>
      </article>
      <article class="ref">
        <div class="meta"><span class="badge off">공식 문서</span><span class="badge first">직접 확인</span><span>Firebase</span></div>
        <h4><a href="https://firebase.google.com/docs/app-check/android/debug-provider" target="_blank" rel="noopener">Android 디버그 제공자 <span class="ext">↗</span></a></h4>
        <p>개발 중인 기기에서 App Check를 통과하는 방법: 디버그 제공자를 설치하고 로그의 토큰을 콘솔에 등록합니다. 릴리스에는 쓰지 않습니다.</p>
        <div class="tie">연결: 시연 이후 App Check를 붙일 때의 개발용 경로</div>
      </article>
      <article class="ref">
        <div class="meta"><span class="badge off">공식 문서</span><span>Google Cloud</span></div>
        <h4><a href="https://cloud.google.com/discover/what-is-vibe-coding" target="_blank" rel="noopener">What is vibe coding? <span class="ext">↗</span></a></h4>
        <p>AI 결과를 그대로 믿는 방식과, 사람이 검토·테스트해 책임지는 "책임 있는 AI 보조 개발"을 구분합니다.</p>
        <div class="tie">연결: <a href="#map">00 전체 지도</a>와 <a href="#test">05 테스트</a></div>
      </article>
      <article class="ref">
        <div class="meta"><span class="badge off">공식 문서</span><span>AWS</span></div>
        <h4><a href="https://aws.amazon.com/what-is/sdlc/" target="_blank" rel="noopener">What is SDLC? <span class="ext">↗</span></a></h4>
        <p>계획 → 설계 → 구현 → 테스트 → 배포 → 유지보수의 소프트웨어 개발 생명주기를 설명합니다.</p>
        <div class="tie">연결: 장 구성의 기준</div>
      </article>
      <article class="ref">
        <div class="meta"><span class="badge exp">전문가</span><span>Simon Willison · 2025.03</span></div>
        <h4><a href="https://simonwillison.net/2025/Mar/19/vibe-coding/" target="_blank" rel="noopener">Not all AI-assisted programming is vibe coding <span class="ext">↗</span></a></h4>
        <p>AI가 쓴 코드라도 검토·테스트하고 설명할 수 있다면 평범한 소프트웨어 개발이라고 구분합니다.</p>
        <div class="tie">연결: <a href="#dev">04 개발</a>의 검증 원칙</div>
      </article>
    </div>
    <p class="col" style="font-size:.88rem;color:var(--muted)">링크의 내용은 2026년 10월 기준으로 확인했습니다. 외부 서비스의 요금·한도·정책은 바뀔 수 있으니 적용 전에 공식 페이지를 다시 확인하세요.</p>
  </div>
</section>

<footer>
  <p>원본 기록: 노포 천년 미식가 저장소의 문서(PRD·아키텍처·QA·인수인계)와 작업 이력 · 화면은 Stitch 시안과 Galaxy S9+ 실제 화면입니다.</p>
  <p style="margin-top:6px"><a href="https://github.com/ohdangchan/NopoCheonnyeon" target="_blank" rel="noopener">github.com/ohdangchan/NopoCheonnyeon</a></p>
</footer>

</div>
'''

tail = src[src.index('</body>'):] if '</body>' in src else '</body></html>'
# 참고 파일 끝(닫는 태그와 스크립트)이 있으면 그대로, 없으면 닫는다
os.makedirs(assets.OUT_DIR, exist_ok=True)
open(OUT, 'w', encoding='utf-8').write(head + BODY + '\n' + (tail if tail.strip().startswith('</body>') else '</body></html>'))
print('ok', os.path.getsize(OUT))

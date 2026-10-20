#!/usr/bin/env python3
"""노포 천년 미식가 카탈로그 생성기.

data/raw/*.jsonl (수집 원본) → data/nopo_stores.csv (백년가게만 보려면 programs 열로 거르기),
data/nopo_stores.db (확인용 SQLite), app/src/main/assets/stores.json (앱용), data/stats.json

사용: python3 tools/build_catalog.py --raw data/raw --out data --assets app/src/main/assets
좌표는 tools/geocode.py 가 data/geocode_cache.csv 에 채우면 자동으로 합쳐진다.
"""
import argparse, csv, glob, hashlib, json, os, re, sqlite3, datetime

THIS_YEAR = datetime.date.today().year
SIDO_ALIASES = {
    "서울특별시": "서울", "서울시": "서울", "서울": "서울", "부산광역시": "부산", "부산": "부산",
    "대구광역시": "대구", "대구시": "대구", "대구": "대구", "부산시": "부산", "인천시": "인천", "대전시": "대전", "울산시": "울산", "인천광역시": "인천", "인천": "인천", "광주광역시": "광주", "광주": "광주",
    "대전광역시": "대전", "대전": "대전", "울산광역시": "울산", "울산": "울산", "세종특별자치시": "세종", "세종": "세종",
    "경기도": "경기", "경기": "경기", "강원도": "강원", "강원특별자치도": "강원", "강원": "강원",
    "충청북도": "충북", "충북": "충북", "충청남도": "충남", "충남": "충남",
    "전라북도": "전북", "전북특별자치도": "전북", "전북": "전북", "전라남도": "전남", "전남": "전남",
    "경상북도": "경북", "경북": "경북", "경상남도": "경남", "경남": "경남", "제주특별자치도": "제주", "제주도": "제주", "제주": "제주",
}
CATEGORY_GROUP = {"음식점업": "음식점", "도소매업": "도소매", "서비스업": "서비스", "제조업": "제조", "숙박업": "숙박"}


def read_jsonl(paths):
    rows = []
    for p in sorted(paths):
        with open(p, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    rows.append(json.loads(line))
                except json.JSONDecodeError:
                    pass
    return rows


def digits(s):
    return re.sub(r"\D", "", s or "")


def first_phone(s):
    """'02-1, 02-2'처럼 여러 번호가 붙어 있으면 첫 번호만 쓴다 (숫자만)."""
    ok = lambda d: (d.startswith("0") and 9 <= len(d) <= 12) or bool(re.fullmatch(r"1[5-8]\d{6}", d))
    for part in re.split(r"[,/;]|\s+(?=0)", s or ""):
        d = digits(part)
        if ok(d):
            return d
    d = digits(s)
    return d if ok(d) else ""   # 0으로 시작하지 않는 등 형식이 틀린 번호는 비운다(영수증 대조 오류 방지)


FOOD_WORDS = ("음식", "식당", "한식", "중식", "중화", "일식", "양식", "분식", "요리", "보양식", "레스토랑", "카페", "다방",
              "베이커리", "제과", "빵", "국밥", "탕", "찌개", "구이", "갈비", "곱창", "냉면", "국수", "짬뽕", "짜장", "떡볶이",
              "백숙", "게장", "횟집", "해장국", "닭", "삼겹살", "돌솥밥", "한정식", "짜글", "순대", "족발", "보쌈", "수제비",
              "만두", "김밥", "치킨", "통닭", "바베큐", "추어", "설렁", "곰탕", "막국수", "전문점")
SHOP_WORDS = ("도매", "소매", "판매", "유통", "상회", "젓갈", "건어물", "화훼", "플라워", "참기름", "방앗간", "수산", "청과", "슈퍼", "상사")
MAKE_WORDS = ("제조", "공방", "공예", "양조")
SERV_WORDS = ("관광", "사진", "이용", "미용", "세탁", "수선", "학원", "서비스", "목욕", "인쇄", "수리")


# 규칙으로 못 고른 가게는 소개글을 읽고 직접 정했다 (2026-10-18 점검)
MANUAL_CATEGORY = {"오동동 아구할매집": "음식점업", "창익집": "음식점업", "한길문고 나운점": "도소매업",
                   "한보내장건설": "건설업", "소랑": "제조업", "통도요": "제조업",
                   "안성서점(행복한책가게)": "도소매업", "농업회사법인주식회사한씨떡집": "제조업"}


def infer_category(raw, name, menu):
    """목록·상세 모두 업종이 비어 있을 때 상세의 업종 설명과 가게명·메뉴로 추정한다."""
    if (name or "").strip() in MANUAL_CATEGORY:
        return MANUAL_CATEGORY[name.strip()]
    t = f"{raw or ''} {name or ''}"
    if any(w in t for w in SHOP_WORDS) or re.search(r"꽃(?!게)", t):   # 꽃게장은 음식
        return "도소매업"
    if any(w in t for w in MAKE_WORDS):
        return "제조업"
    if any(w in t for w in SERV_WORDS):
        return "서비스업"
    if any(w in f"{t} {' '.join(menu or [])}" for w in FOOD_WORDS):
        return "음식점업"
    return ""


ADDRESS_FIXES = {"부산광역시 북부 ": "부산광역시 북구 ", "경남 통영 서호 ": "경남 통영시 서호동 ", "서울 동대무 ": "서울 동대문구 "}


def core_name(n):
    """회사 표기·괄호·공백을 뺀 이름 (같은 가게 대조용)."""
    return re.sub(r"[\s\W_]+", "", re.sub(r"\([^)]*\)", "", CORP.sub("", n or "")))


def orae_summary(o, oy):
    """오래가게는 소개 원문이 없어 선정 연도·업종·창업연도·대표 품목으로 한 줄을 만든다 (조사 메모는 쓰지 않음)."""
    parts = []
    if o.get("selection_year"):
        parts.append(f"{o['selection_year']}년 서울시 오래가게 선정")
    if o.get("category"):
        parts.append(str(o["category"]))
    if oy:
        parts.append(f"{oy}년 창업")
    items = [str(m) for m in (o.get("main_items") or []) if m][:3]
    if items:
        parts.append("대표 " + ", ".join(items))
    return " · ".join(parts)


CORP = re.compile(r"(주식회사|유한회사|농업회사법인|영농조합법인|\(주\)|㈜)")


def name_aliases(primary, *others):
    """영수증·간판 글자와 맞춰 볼 별칭: 목록/상세 이름 차이, 회사 표기·괄호를 뺀 이름."""
    norm = lambda x: re.sub(r"[\s\W_]+", "", x or "")
    seen, out = {norm(primary)}, []
    for c in [o for o in others if o] + [primary]:
        for v in (c, CORP.sub("", c), re.sub(r"\([^)]*\)", "", c), re.sub(r"\([^)]*\)", "", CORP.sub("", c))):
            v = re.sub(r"\s+", " ", v).strip()
            n = norm(v)
            if len(n) >= 2 and n not in seen:
                seen.add(n)
                out.append(v)
    return out


def split_region(address):
    toks = (address or "").replace(",", " ").split()
    sido = SIDO_ALIASES.get(toks[0], toks[0]) if toks else ""
    sigungu = ""
    if len(toks) > 1:
        sigungu = toks[1]
        # '청주시 흥덕구' 처럼 시+구 인 경우 함께 기록
        if len(toks) > 2 and toks[1].endswith("시") and toks[2].endswith("구"):
            sigungu = toks[1] + " " + toks[2]
    return sido, sigungu


def founded_from_date(s):
    m = re.search(r"(1[89]\d{2}|20[0-2]\d)[-./년\s]", (s or "") + " ")
    return int(m.group(1)) if m else None


def hints_from_text(text):
    """소개글에서 창업 연도·업력·n대 단서를 찾는다 (참고용)."""
    text = text or ""
    year = None
    m = re.search(r"(1[89]\d{2}|20[0-2]\d)\s*년[^.。]{0,25}?(창업|개업|개점|문을\s*[열연]|시작|출발|설립|오픈|간판을\s*[달단]|터를\s*잡)", text)
    if not m:
        m = re.search(r"(1[89]\d{2}|20[0-2]\d)\s*년부터[^.。]{0,15}?(운영|영업|장사)", text)
    if m:
        year = int(m.group(1))
    yrs = None
    m2 = re.search(r"(\d{2,3})\s*년\s*(전통|역사|간|째|이상|넘)", text)
    if m2:
        yrs = int(m2.group(1))
    gen = None
    m3 = re.search(r"(\d)\s*대\s*(째|에\s*걸쳐|물림|승계|가업|를\s*이어|대표)", text)
    if m3:
        gen = int(m3.group(1))
    return year, yrs, gen


def detail_score(d):
    """같은 가게의 상세 기록이 여러 개면 창업일이 있고 오류가 없는 기록을 고른다."""
    err = d.get("error") or ""
    fetch_failed = ("429" in err) or ("not fetched" in err) or ("fail" in err.lower() and not d.get("founded"))
    return (1 if d.get("founded") else 0) + (0 if fetch_failed else 1)


def merge_orae(orae):
    """오래가게 원본 줄(발표 자료 + 보정 줄)을 가게 하나당 한 레코드로 합친다."""
    seen = {}
    for o in orae:
        key = ((o.get("name") or "").strip(), (o.get("district") or "").strip())
        if not key[0]:
            continue
        same_name = [k for k in seen if k[0].replace(" ", "") == key[0].replace(" ", "")]
        if not key[1] and len(same_name) > 1:
            same_name.sort(key=lambda k: 0 if not k[1] else 1)   # 자치구 없는 줄은 자치구 없는 가게에 먼저 붙인다
        if same_name and (not key[1] or not same_name[0][1]):
            y_new, y_old = o.get("selection_year"), seen[same_name[0]].get("selection_year")
            if y_new and y_old and y_new != y_old:
                same_name = []   # 이름이 같아도 선정 연도가 다르면 다른 가게 (예: 2017 만나분식 / 2021 강남 만나분식)
        if same_name and (not key[1] or not same_name[0][1] or same_name[0][1] == key[1]):
            key = same_name[0] if same_name[0][1] else key
            if key not in seen:
                seen[key] = seen.pop(same_name[0])
        prev = seen.get(key, {})
        merged = dict(prev)
        for k, v in o.items():
            if v not in (None, "", []):
                merged[k] = v
        seen[key] = merged
    return seen


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", default="data/raw")
    ap.add_argument("--out", default="data")
    ap.add_argument("--assets", default="app/src/main/assets")
    ap.add_argument("--version", type=int, default=int(datetime.datetime.now().strftime("%y%m%d%H")))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    os.makedirs(a.assets, exist_ok=True)

    lists = read_jsonl(glob.glob(os.path.join(a.raw, "bn_list_*.jsonl")))
    details = read_jsonl(glob.glob(os.path.join(a.raw, "bn_detail_*.jsonl")))
    orae = read_jsonl(glob.glob(os.path.join(a.raw, "orae_stores.jsonl")))

    by_id = {}
    for r in lists:
        rid = str(r.get("id") or "").strip()
        if rid:
            by_id[rid] = r
    score = detail_score

    det = {}
    for d in details:
        rid = str(d.get("rcpn_no") or "").strip()
        if not rid:
            continue
        if rid not in det or score(d) > score(det[rid]):
            det[rid] = d

    geo = {}
    gpath = os.path.join(a.out, "geocode_cache.csv")
    if os.path.exists(gpath):
        with open(gpath, encoding="utf-8") as f:
            for row in csv.DictReader(f):
                try:
                    geo[row["store_id"]] = (float(row["lat"]), float(row["lng"]),
                                            (row.get("matched") or "").strip(), (row.get("method") or "addr").strip())
                except (KeyError, ValueError):
                    pass

    stories = {}   # tools/fetch_stories.py 가 채운 가게 이야기(소개글 원문)
    for r in read_jsonl([os.path.join(a.raw, "bn_story.jsonl")] if os.path.exists(os.path.join(a.raw, "bn_story.jsonl")) else []):
        if r.get("id") and (r.get("story") or "").strip():
            stories[r["id"]] = r["story"].strip()
    photos = {}
    ppath = os.path.join(a.out, "photo_cache.csv")   # tools/fetch_photos.py 가 채운다
    if os.path.exists(ppath):
        with open(ppath, encoding="utf-8") as f:
            for row in csv.DictReader(f):
                photos[row["store_id"]] = [u for u in row["photo_urls"].split("|") if u]

    stores = []
    for rid, l in by_id.items():
        d = det.get(rid, {})
        name = (d.get("name") or l.get("name") or "").strip()
        KNOWN = {"음식점업", "도소매업", "서비스업", "제조업", "숙박업", "건설업", "농업", "축산업"}
        dc, lc = (d.get("category") or "").strip(), (l.get("category") or "").strip()
        category = dc if dc in KNOWN else (lc if lc in KNOWN else (lc or dc))
        cat_note = ""
        if category not in KNOWN:
            guess = infer_category(dc if dc not in ("-", "") else "", name, d.get("menu_items") if isinstance(d.get("menu_items"), list) else [])
            if guess:
                cat_note = f"업종 추정(원문: {dc or lc or '없음'})"
                category = guess
        address = (d.get("address") or l.get("address") or "").strip()
        for wrong, right in ADDRESS_FIXES.items():   # 원본 주소의 오타 (2026-10-18 점검)
            address = address.replace(wrong, right)
        phone = first_phone(d.get("phone") or l.get("phone"))
        if not phone.strip("0"):
            phone = first_phone(l.get("phone")) if first_phone(l.get("phone")).strip("0") else ""
        founded_raw = (d.get("founded") or "").strip()
        founded_year = founded_from_date(founded_raw)
        intro = (d.get("intro") or "").strip()
        snippet = (l.get("intro_snippet") or "").strip()
        hy, hyrs, hgen = hints_from_text(intro or snippet)
        founded_source = "SBIZ_DETAIL" if founded_year else ""
        # 상세 페이지 창업일은 현재 사업자 기준일 수 있다. 소개글에 더 이른 창업연도가 있으면 그것을 쓴다.
        if hy and 1850 <= hy <= THIS_YEAR and (not founded_year or hy < founded_year):
            founded_year, founded_source = hy, ("INTRO_TEXT" if not founded_year else "INTRO_TEXT_EARLIER")
        # 그래도 30년 미만이면 소개글의 “50년 넘게”, “35년간” 같은 업력으로 보정 (보수적으로 올해 기준)
        if founded_year and THIS_YEAR - founded_year < 30 and hyrs and 30 <= hyrs <= 80:
            founded_year, founded_source = THIS_YEAR - hyrs, "INTRO_YEARS"
        sido, sigungu = split_region(address)
        sid = "BN-" + rid
        lat, lng = geo.get(sid, (None, None, "", ""))[:2]
        g = d.get("generation")
        if isinstance(g, str):
            g = int(re.sub(r"\D", "", g)) if re.search(r"\d", g) else None
        gen = g if isinstance(g, int) and 1 <= g <= 9 else hgen
        menu = d.get("menu_items") if isinstance(d.get("menu_items"), list) else []
        stores.append({
            "id": sid,
            "rcpn_no": rid,
            "list_no": l.get("no"),
            "name": name,
            "aliases": "|".join(name_aliases(name, l.get("name"), d.get("name"))),
            "programs": "BAEKNYEON",
            "category_raw": category,
            "category_group": CATEGORY_GROUP.get(category, "기타" if category else ""),
            "is_food": 1 if category == "음식점업" else 0,
            "sido": sido,
            "sigungu": sigungu,
            "address": address,
            "phone": phone,
            "founded_date": founded_raw,
            "founded_year": founded_year or "",
            "founded_source": founded_source,
            "years_in_text": hyrs or "",
            "years_now": (THIS_YEAR - founded_year) if founded_year else "",
            "generation": gen or "",
            "menu_items": "|".join(m for m in menu if m),
            "hours": (d.get("hours") or "").strip(),
            "amenities": "|".join(d.get("amenities") or []) if isinstance(d.get("amenities"), list) else "",
            "homepage": (d.get("homepage") or "").strip(),
            "sns": (d.get("sns") or "").strip(),
            "intro": intro,
            "intro_snippet": snippet,
            "image_count": int(re.sub(r"\D", "", str(d.get("image_count")))) if re.search(r"\d", str(d.get("image_count", ""))) else "",
            "detail_collected": 1 if d and score(d) >= 1 and (d.get("founded") or d.get("intro")) else 0,
            "lat": lat if lat is not None else "",
            "lng": lng if lng is not None else "",
            "source_url": "https://www.sbiz.or.kr/hdst/main/ohndMarketDetailSmba.do?rcpnNo=" + rid,
            "data_flags": "|".join(x for x in ("" if d else "DETAIL_MISSING", "CATEGORY_INFERRED" if cat_note else "") if x),
            "data_note": cat_note,
        })

    # 공식 지정리스트(공공데이터포털 CSV, tools/import_official_list.py로 만든 파일)가 있으면 빠진 백년가게를 더한다
    off_path = os.path.join(a.raw, "bn_official_list.jsonl")
    if os.path.exists(off_path):
        official = read_jsonl([off_path])
        listed = {r.get("matched_id") for r in official if r.get("matched_id")}
        for st_ in stores:
            if st_["id"].startswith("BN-") and st_["id"] not in listed:
                st_["data_flags"] = "|".join([f for f in st_["data_flags"].split("|") if f] + ["NOT_IN_OFFICIAL"])
        for r in official:
            if r.get("matched_id") or not (r.get("name") or "").strip():
                continue
            name = r["name"].strip()
            address = (r.get("address") or "").strip()
            for wrong, right in ADDRESS_FIXES.items():
                address = address.replace(wrong, right)
            sido, sigungu = split_region(address)
            sid = "BN-OFF-" + hashlib.sha1(f"{name}|{address}".encode()).hexdigest()[:8]
            cat = infer_category("", name, [])
            lat, lng = geo.get(sid, (None, None, "", ""))[:2]
            stores.append({
                "id": sid, "rcpn_no": "", "list_no": "", "name": name,
                "aliases": "|".join(name_aliases(name)), "programs": "BAEKNYEON",
                "category_raw": cat, "category_group": CATEGORY_GROUP.get(cat, "기타"),
                "is_food": 1 if cat == "음식점업" else 0, "sido": sido, "sigungu": sigungu,
                "address": address, "phone": first_phone(r.get("phone")), "founded_date": "", "founded_year": "",
                "founded_source": "", "years_in_text": "", "years_now": "", "generation": "", "menu_items": "",
                "hours": "", "amenities": "", "homepage": "", "sns": "", "intro": "", "intro_snippet": "",
                "image_count": "", "detail_collected": 0,
                "lat": lat if lat is not None else "", "lng": lng if lng is not None else "",
                "source_url": "https://www.data.go.kr/data/15132695/fileData.do",
                "data_flags": "|".join(["OFFICIAL_ONLY"] + (["CATEGORY_INFERRED"] if cat else [])),
                "data_note": "공공데이터포털 지정리스트에만 있음: 상세 미수집, 창업연도 미상(앱은 업력 30년)",
            })

    # 오래가게 병합 (수집된 경우)
    seen = merge_orae(orae)
    for (nm, dist), o in seen.items():
        if o.get("closed") in (True, "true"):
            continue
        nm = (o.get("name_fixed") or nm).strip()   # 출처로 확인한 정식 한글 상호
        sid = "OR-" + hashlib.sha1(f"{nm}|{dist}".encode()).hexdigest()[:8]
        note = (o.get("note") or "").strip()
        if o.get("name_en"):
            note = (note + f" (영문: {o.get('name_en')})").strip()
        fl = []
        if "미검증" in note:
            fl.append("NAME_UNVERIFIED")
        if "폐업" in note:
            fl.append("MAYBE_CLOSED")
        if "상충" in note:
            fl.append("YEAR_CONFLICT")
        if "추정" in note and "배치" in note:
            fl.append("BATCH_INFERRED")
        flags = "|".join(fl)
        clean_note = "" if any(k in note for k in ("미검증", "폐업", "상충", "추정", "영문")) else note
        verified = (o.get("verified") or "").strip()
        if verified:   # 한글 기사·자료로 상호와 선정 회차를 확인한 경우
            fl = [f for f in fl if f not in ("NAME_UNVERIFIED", "BATCH_INFERRED")]
            flags, note, clean_note = "|".join(fl), verified, ""
        match = next((s for s in stores if s["id"].startswith("BN-") and s["sido"] == "서울" and core_name(s["name"]) == core_name(nm)), None)
        if match:
            match["programs"] = "BAEKNYEON|ORAE"
            if not match["founded_year"]:
                oy2 = o.get("opening_year")
                if isinstance(oy2, int):
                    match["founded_year"], match["founded_source"] = oy2, "SEOUL_RELEASE"
            continue
        oy = o.get("opening_year")
        if isinstance(oy, str):
            oy = int(re.sub(r"\D", "", oy)[:4]) if re.search(r"\d{4}", oy) else None
        if not (isinstance(oy, int) and 1850 <= oy <= THIS_YEAR):
            oy = None
        og = o.get("generation")
        if isinstance(og, str):
            og = int(re.sub(r"\D", "", og)) if re.search(r"\d", og) else None
        lat, lng, g_addr, g_method = geo.get(sid, (None, None, "", ""))
        address = (o.get("address") or "").strip()
        if not address and lat is not None and g_method == "keyword":
            # 주소가 없어 가게 이름으로 찾은 좌표: 지도에서 위치를 눈으로 확인할 것
            address = g_addr
            flags = "|".join([f for f in flags.split("|") if f] + ["COORD_FROM_NAME"])
        stores.append({
            "id": sid, "rcpn_no": "", "list_no": "", "name": nm, "aliases": "|".join(name_aliases(nm, o.get("name"))), "programs": "ORAE",
            "category_raw": o.get("category") or "", "category_group": "음식점" if o.get("is_food") in (True, "true", 1) else "기타",
            "is_food": 1 if o.get("is_food") in (True, "true", 1) else 0, "sido": "서울", "sigungu": dist,
            "address": address, "phone": first_phone(o.get("phone")), "founded_date": "",
            "founded_year": oy or "", "founded_source": "SEOUL_RELEASE" if oy else "",
            "years_in_text": o.get("years_stated") or "", "years_now": (THIS_YEAR - oy) if oy else "",
            "generation": og or "", "menu_items": "|".join(str(m) for m in (o.get("main_items") or []) if m),
            "hours": "", "amenities": "", "homepage": "", "sns": "", "intro": orae_summary(o, oy),
            "intro_snippet": "", "image_count": "", "detail_collected": 1 if oy else 0,
            "lat": lat if lat is not None else "", "lng": lng if lng is not None else "",
            "source_url": (o.get("source_urls") or [""])[0],
            "data_flags": flags,
            "data_note": note,
        })

    # 같은 가게가 두 번 지정된 경우(같은 이름 + 같은 주소): 창업연도가 이른 쪽을 남기고 나머지는 DUPLICATE
    def _addr_key(a):
        return re.sub(r"\([^)]*\)|[\s,.\-]", "", a or "")[:18]
    groups = {}
    for st_ in stores:
        if st_["id"].startswith("BN-") and st_["address"]:
            groups.setdefault((st_["name"].replace(" ", ""), _addr_key(st_["address"])), []).append(st_)
    for g in groups.values():
        if len(g) > 1:
            g.sort(key=lambda x: (x["founded_year"] if isinstance(x["founded_year"], int) else 9999))
            for dup in g[1:]:
                dup["data_flags"] = "|".join([f for f in dup["data_flags"].split("|") if f] + ["DUPLICATE"])
                dup["data_note"] = (dup["data_note"] + f" 같은 가게의 중복 지정(앱에는 {g[0]['id']}만 넣음)").strip()

    # 여러 가게에 똑같이 붙은 홈페이지 주소는 페이지 공통 링크로 보고 비운다
    hp = {}
    for st_ in stores:
        if st_["homepage"]:
            hp[st_["homepage"]] = hp.get(st_["homepage"], 0) + 1
    for st_ in stores:
        if st_["homepage"] and hp.get(st_["homepage"], 0) > 1:
            st_["homepage"] = ""

    stores.sort(key=lambda s: (s["programs"], -(s["list_no"] or 0) if isinstance(s["list_no"], int) else 0, s["name"]))
    for st_ in stores:
        st_["story"] = stories.get(st_["id"], "")   # 가게 이야기(소개글 원문). 오래가게 등은 비어 있다
    cols = list(stores[0].keys())
    with open(os.path.join(a.out, "nopo_stores.csv"), "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols)
        w.writeheader()
        w.writerows(stores)

    dbp = os.path.join(a.out, "nopo_stores.db")
    if os.path.exists(dbp):
        os.remove(dbp)
    con = sqlite3.connect(dbp)
    con.execute("CREATE TABLE stores (" + ", ".join(f'"{c}" TEXT' for c in cols) + ", PRIMARY KEY(id))")
    con.executemany("INSERT INTO stores VALUES (" + ",".join("?" * len(cols)) + ")",
                    [[None if s[c] == "" else s[c] for c in cols] for s in stores])
    con.execute("CREATE INDEX idx_sido ON stores(sido)")
    con.execute("CREATE INDEX idx_food ON stores(is_food)")
    con.commit()
    con.close()

    app_stores = []
    for s in stores:
        if any(f in s.get("data_flags", "") for f in ("NAME_UNVERIFIED", "MAYBE_CLOSED", "DUPLICATE")):
            continue
        fy = s["founded_year"] if isinstance(s["founded_year"], int) else None
        app_stores.append({
            "id": s["id"], "name": s["name"], "aliases": [x for x in s["aliases"].split("|") if x],
            "programs": s["programs"].split("|"), "categoryGroup": s["category_group"], "isFood": bool(s["is_food"]),
            "sido": s["sido"], "sigungu": s["sigungu"], "address": s["address"], "phone": s["phone"] or None,
            "foundedYear": fy, "foundedDate": s["founded_date"] or None,
            "lat": s["lat"] if s["lat"] != "" else None, "lng": s["lng"] if s["lng"] != "" else None,
            "menuItems": [m for m in s["menu_items"].split("|") if m],
            "hours": s["hours"] or None, "amenities": [m for m in s["amenities"].split("|") if m],
            "summary": (s["intro"] or s["intro_snippet"]) or None,
            "story": s["story"] or None,
            "generation": s["generation"] if isinstance(s["generation"], int) else None,
            "sourceUrl": s["source_url"] or None,
            "photoUrls": photos.get(s["id"], []),
        })
    with open(os.path.join(a.assets, "stores.json"), "w", encoding="utf-8") as f:
        json.dump({"version": a.version, "generatedAt": datetime.datetime.now().isoformat(timespec="seconds"),
                   "count": len(app_stores), "stores": app_stores}, f, ensure_ascii=False)

    def count(key, rows):
        out = {}
        for r in rows:
            out[r[key]] = out.get(r[key], 0) + 1
        return dict(sorted(out.items(), key=lambda kv: -kv[1]))

    bn = [s for s in stores if "BAEKNYEON" in s["programs"]]
    fy = [s["founded_year"] for s in bn if isinstance(s["founded_year"], int)]
    decades = {}
    for y in fy:
        d = (y // 10) * 10
        decades[d] = decades.get(d, 0) + 1
    stats = {
        "generated_at": datetime.datetime.now().isoformat(timespec="seconds"),
        "baeknyeon_total": len(bn),
        "baeknyeon_official_only": sum(1 for s in bn if s["id"].startswith("BN-OFF-")),
        "baeknyeon_not_in_official": sum(1 for s in bn if "NOT_IN_OFFICIAL" in s["data_flags"]),
        "baeknyeon_detail_collected": sum(1 for s in bn if s["detail_collected"]),
        "baeknyeon_food": sum(1 for s in bn if s["is_food"]),
        "baeknyeon_founded_known": len(fy),
        "baeknyeon_under30": sum(1 for s in bn if isinstance(s["founded_year"], int) and THIS_YEAR - s["founded_year"] < 30),
        "baeknyeon_founded_from_detail": sum(1 for s in bn if s["founded_source"] == "SBIZ_DETAIL"),
        "baeknyeon_founded_from_text": sum(1 for s in bn if s["founded_source"] in ("INTRO_TEXT", "INTRO_TEXT_EARLIER", "INTRO_YEARS")),
        "orae_total": sum(1 for s in stores if "ORAE" in s["programs"]),
        "orae_listed": len([1 for o in seen.values() if o.get("closed") not in (True, "true")]),
        "orae_matched_baeknyeon": sum(1 for s in stores if s["programs"] == "BAEKNYEON|ORAE"),
        "orae_founded_known": sum(1 for s in stores if "ORAE" in s["programs"] and isinstance(s["founded_year"], int)),
        "catalog_total": len(stores),
        "app_catalog_total": len(app_stores),
        "orae_in_app": sum(1 for s in app_stores if "ORAE" in s["programs"]),
        "orae_flagged": sum(1 for s in stores if s.get("data_flags", "") and s["id"].startswith("OR-") and any(f in s["data_flags"] for f in ("NAME_UNVERIFIED", "MAYBE_CLOSED"))),
        "catalog_food": sum(1 for s in stores if s["is_food"]),
        "with_coords": sum(1 for s in stores if s["lat"] != ""),
        "by_category": count("category_raw", bn),
        "by_sido": count("sido", bn),
        "food_by_sido": count("sido", [s for s in bn if s["is_food"]]),
        "founded_by_decade": dict(sorted(decades.items())),
        "oldest": sorted(({"name": s["name"], "sido": s["sido"], "year": s["founded_year"]} for s in bn if isinstance(s["founded_year"], int)), key=lambda x: x["year"])[:10],
    }
    with open(os.path.join(a.out, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=1)
    print(json.dumps({k: v for k, v in stats.items() if not isinstance(v, (dict, list))}, ensure_ascii=False))


if __name__ == "__main__":
    main()

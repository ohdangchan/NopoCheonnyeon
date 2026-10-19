#!/usr/bin/env python3
"""공공데이터포털 '전국 백년가게 지정리스트' CSV를 카탈로그에 합칠 준비를 한다. 개인 컴퓨터에서 실행.

소상공인마당 목록에는 소개 페이지가 있는 백년가게만 나온다(수집 1,139곳).
공식 지정리스트(공공데이터포털 15132695, 2026-08-20 기준 1,511행)에는 그 밖의 가게도 있으므로,
CSV를 내려받아 이 도구로 대조하면 빠진 가게가 data/raw/bn_official_list.jsonl 로 저장되고,
python tools/build_catalog.py 를 다시 실행하면 카탈로그에 들어간다(이름·주소·전화만, 창업연도 미상 → 앱은 업력 30년).

    1) https://www.data.go.kr/data/15132695/fileData.do 에서 CSV 내려받기
    2) python tools/import_official_list.py --csv 내려받은파일.csv
    3) python tools/build_catalog.py
"""
import argparse, csv, json, os, re

CORP = re.compile(r"(주식회사|유한회사|농업회사법인|영농조합법인|\(주\)|㈜)")


def core_name(n):
    return re.sub(r"[\s\W_]+", "", re.sub(r"\([^)]*\)", "", CORP.sub("", n or "")))


def digits(s):
    return re.sub(r"\D", "", s or "")


def addr_key(a):
    """시도 표기 차이를 줄이고 도로명+번호까지만 비교."""
    a = re.sub(r"\([^)]*\)", " ", a or "")
    a = re.sub(r"(특별시|광역시|특별자치시|특별자치도)", "", a)
    a = a.replace("경기도", "경기").replace("강원도", "강원").replace("충청북도", "충북").replace("충청남도", "충남")
    a = a.replace("전라북도", "전북").replace("전라남도", "전남").replace("경상북도", "경북").replace("경상남도", "경남")
    m = re.search(r"([가-힣0-9]+(로|길))\s*([0-9-]+)", a)
    return (m.group(1) + m.group(3)) if m else re.sub(r"\s+", "", a)[:16]


def read_rows(path):
    for enc in ("utf-8-sig", "cp949", "euc-kr"):
        try:
            with open(path, encoding=enc, newline="") as f:
                rows = list(csv.reader(f))
            break
        except UnicodeDecodeError:
            continue
    else:
        raise SystemExit("CSV 인코딩을 읽지 못했습니다 (UTF-8 또는 CP949)")
    head = [h.strip() for h in rows[0]]
    col = lambda *keys: next((i for i, h in enumerate(head) if any(k in h for k in keys)), None)
    i_no, i_name, i_addr, i_tel = col("연번", "번호"), col("업체명", "상호", "가게"), col("주소"), col("연락처", "전화")
    if i_name is None or i_addr is None:
        raise SystemExit(f"업체명·주소 열을 찾지 못했습니다: {head}")
    for r in rows[1:]:
        if not r or not any(x.strip() for x in r):
            continue
        g = lambda i: (r[i].strip() if i is not None and i < len(r) else "")
        yield {"official_no": g(i_no), "name": g(i_name), "address": g(i_addr), "phone": g(i_tel)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", required=True, help="공공데이터포털에서 내려받은 지정리스트 CSV")
    ap.add_argument("--data", default="data")
    a = ap.parse_args()
    with open(os.path.join(a.data, "nopo_stores.csv"), encoding="utf-8-sig") as f:
        bn = [r for r in csv.DictReader(f) if r["id"].startswith("BN-") and not r["id"].startswith("BN-OFF-")]
    by_phone = {digits(r["phone"]): r["id"] for r in bn if len(digits(r["phone"])) >= 9}
    by_name_addr = {(core_name(r["name"]), addr_key(r["address"])): r["id"] for r in bn}
    by_name_sigungu = {}
    for r in bn:
        by_name_sigungu.setdefault((core_name(r["name"]), r["sigungu"]), []).append(r["id"])
    out, matched = [], 0
    for row in read_rows(a.csv):
        cn, ak, ph = core_name(row["name"]), addr_key(row["address"]), digits(row["phone"])
        mid = by_name_addr.get((cn, ak)) or (by_phone.get(ph) if len(ph) >= 9 else None)
        if not mid:
            cands = [ids for (n, sg), ids in by_name_sigungu.items() if n == cn and sg and sg in row["address"]]
            mid = cands[0][0] if len(cands) == 1 and len(cands[0]) == 1 else None
        row["matched_id"] = mid
        matched += bool(mid)
        out.append(row)
    path = os.path.join(a.data, "raw", "bn_official_list.jsonl")
    with open(path, "w", encoding="utf-8") as f:
        for r in out:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    print(f"공식 목록 {len(out)}행: 기존 카탈로그와 맞춘 {matched}행, 새로 넣을 {len(out) - matched}행 -> {path}")
    print("이제 python tools/build_catalog.py 를 실행하세요.")


if __name__ == "__main__":
    main()

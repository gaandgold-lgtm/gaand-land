"""
부족한 정보 보완
- 세움터(seum.go.kr): 건축물대장 → 사용승인일, 구조, 면적
- 집폼: 단지 추가 정보
"""
from playwright.sync_api import sync_playwright, Page
import re


# ── 세움터 ──────────────────────────────────────────────
def fetch_seum(address: str, seum_id: str, seum_pw: str, headless: bool = False) -> dict:
    """세움터에서 건축물대장 정보를 조회합니다."""
    result = {}
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        page = browser.new_page()
        try:
            _seum_login(page, seum_id, seum_pw)
            result = _seum_search(page, address)
        finally:
            browser.close()
    return result


def _seum_login(page: Page, user_id: str, password: str):
    page.goto("https://www.seum.go.kr/", wait_until="networkidle")
    # 로그인 버튼 클릭
    page.click("text=로그인")
    page.wait_for_selector("input[name='userId'], input[id='userId']", timeout=10000)
    page.fill("input[name='userId'], input[id='userId']", user_id)
    page.fill("input[name='userPw'], input[type='password']", password)
    page.click("button[type='submit'], .btn-login, input[type='submit']")
    page.wait_for_load_state("networkidle")


def _seum_search(page: Page, address: str) -> dict:
    # 건축물대장 메뉴 이동
    page.goto("https://www.seum.go.kr/main/BD_index.do", wait_until="networkidle")

    # 주소 검색
    search_input = page.query_selector("input[name='schAdr'], input[placeholder*='주소']")
    if search_input:
        search_input.fill(address)
        page.keyboard.press("Enter")
        page.wait_for_load_state("networkidle")

    result = {}
    try:
        # 사용승인일
        approval = page.query_selector("td:near(:text('사용승인'))")
        if approval:
            result["approval_date"] = approval.inner_text().strip()
        # 구조
        structure = page.query_selector("td:near(:text('구조'))")
        if structure:
            result["structure"] = structure.inner_text().strip()
        # 주차
        parking = page.query_selector("td:near(:text('주차'))")
        if parking:
            result["parking_info"] = parking.inner_text().strip()
    except Exception:
        pass

    return result


# ── 집폼 ────────────────────────────────────────────────
def fetch_zipform(complex_name: str, headless: bool = True) -> dict:
    """집폼에서 단지 기본 정보를 조회합니다."""
    result = {}
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        page = browser.new_page()
        try:
            page.goto("https://www.zipform.kr/", wait_until="networkidle", timeout=15000)
            search = page.query_selector("input[type='search'], input[placeholder*='단지']")
            if search:
                search.fill(complex_name)
                page.keyboard.press("Enter")
                page.wait_for_load_state("networkidle")
                # 첫 번째 결과 클릭
                first = page.query_selector(".complex-item:first-child, .result-item:first-child")
                if first:
                    first.click()
                    page.wait_for_load_state("networkidle")
                    result = _parse_zipform(page)
        except Exception:
            pass
        finally:
            browser.close()
    return result


def _parse_zipform(page: Page) -> dict:
    result = {}
    try:
        area_text = page.query_selector(".area, [class*='area']")
        if area_text:
            result["area_info"] = area_text.inner_text()
    except Exception:
        pass
    return result


def merge(base: dict, enriched: dict) -> dict:
    """기존 데이터에 보완 데이터를 덮어쓰지 않고 채웁니다."""
    merged = dict(base)
    for k, v in enriched.items():
        if merged.get(k) is None and v is not None:
            merged[k] = v
    return merged

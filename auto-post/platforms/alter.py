"""
부동산알터 플랫폼 자동화
⚠️  ALTER_URL을 .env에 실제 URL로 설정하고,
    실제 화면을 보면서 셀렉터를 아래 SELECTORS에 맞게 조정하세요.
"""
from playwright.sync_api import Page
from .base import BasePlatform

# ── 화면별 셀렉터 (실제 화면 보고 수정 필요) ──────────────
SELECTORS = {
    # 로그인
    "login_id":       "input[name='userId'], #userId, input[placeholder*='아이디']",
    "login_pw":       "input[type='password']",
    "login_btn":      "button[type='submit'], .btn-login, input[value='로그인']",

    # 매물 등록 버튼
    "new_listing":    "text=매물등록, text=물건등록, .btn-regist",

    # 주소
    "address_btn":    "text=주소검색, .addr-search",
    "address_input":  "input[placeholder*='주소'], #addrInput",
    "address_select": ".addr-result li:first-child, .search-result:first-child",

    # 기본정보
    "dong":           "input[name='dong'], #dong",
    "ho":             "input[name='ho'], #ho",
    "floor":          "input[name='floor'], #floor",
    "area_exclusive": "input[name='areaExclusive'], input[placeholder*='전용']",
    "area_supply":    "input[name='areaSupply'], input[placeholder*='공급']",

    # 거래 유형
    "deal_sale":      "label:has-text('매매'), input[value='매매']",
    "deal_jeonse":    "label:has-text('전세'), input[value='전세']",
    "deal_monthly":   "label:has-text('월세'), input[value='월세']",

    # 가격
    "price_sale":     "input[name='priceSale'], input[placeholder*='매매가']",
    "price_jeonse":   "input[name='priceJeonse'], input[placeholder*='전세가']",
    "price_deposit":  "input[name='deposit'], input[placeholder*='보증금']",
    "price_monthly":  "input[name='monthlyRent'], input[placeholder*='월세']",

    # 추가 정보
    "direction":      "select[name='direction'], #direction",
    "room_count":     "input[name='roomCnt'], input[placeholder*='방']",
    "management_fee": "input[name='manageFee'], input[placeholder*='관리비']",
    "move_in_date":   "input[name='moveInDate'], input[type='date'][placeholder*='입주']",
    "description":    "textarea[name='desc'], textarea[name='content'], #description",

    # 제출
    "submit":         "button:has-text('등록'), button:has-text('저장'), button[type='submit']",
}

DIRECTION_MAP = {
    "남향": "S", "북향": "N", "동향": "E", "서향": "W",
    "남동향": "SE", "남서향": "SW", "북동향": "NE", "북서향": "NW",
}


class AlterPlatform(BasePlatform):
    def login(self, page: Page):
        page.goto(self.url, wait_until="networkidle")
        page.fill(SELECTORS["login_id"], self.user_id)
        page.fill(SELECTORS["login_pw"], self.password)
        page.click(SELECTORS["login_btn"])
        page.wait_for_load_state("networkidle")
        print(f"🔐 로그인 완료: {self.url}")

    def post_listing(self, page: Page, data: dict):
        # 1. 매물 등록 화면으로 이동
        page.click(SELECTORS["new_listing"])
        page.wait_for_load_state("networkidle")

        # 2. 거래 유형 선택
        deal = data.get("deal_type", "")
        if "매매" in deal:
            page.click(SELECTORS["deal_sale"])
        elif "전세" in deal:
            page.click(SELECTORS["deal_jeonse"])
        elif "월세" in deal:
            page.click(SELECTORS["deal_monthly"])

        # 3. 주소 입력
        if data.get("address"):
            _click_if_exists(page, SELECTORS["address_btn"])
            page.fill(SELECTORS["address_input"], data["address"])
            page.keyboard.press("Enter")
            page.wait_for_timeout(1500)
            _click_if_exists(page, SELECTORS["address_select"])

        # 4. 동/호/층
        _fill_if(page, SELECTORS["dong"], data.get("dong"))
        _fill_if(page, SELECTORS["ho"], data.get("ho"))
        _fill_if(page, SELECTORS["floor"], data.get("floor"))

        # 5. 면적
        _fill_if(page, SELECTORS["area_exclusive"], data.get("area_exclusive_m2"))
        _fill_if(page, SELECTORS["area_supply"], data.get("area_supply_m2"))

        # 6. 가격
        _fill_if(page, SELECTORS["price_sale"], data.get("price_sale"))
        _fill_if(page, SELECTORS["price_jeonse"], data.get("price_jeonse"))
        _fill_if(page, SELECTORS["price_deposit"], data.get("deposit"))
        _fill_if(page, SELECTORS["price_monthly"], data.get("monthly_rent"))

        # 7. 방향
        if data.get("direction"):
            dir_val = DIRECTION_MAP.get(data["direction"], data["direction"])
            _select_if(page, SELECTORS["direction"], dir_val)

        # 8. 방 개수
        _fill_if(page, SELECTORS["room_count"], data.get("room_count"))

        # 9. 관리비
        _fill_if(page, SELECTORS["management_fee"], data.get("management_fee"))

        # 10. 입주일
        _fill_if(page, SELECTORS["move_in_date"], data.get("move_in_date"))

        # 11. 특이사항
        _fill_if(page, SELECTORS["description"], data.get("description"))

        # 12. 등록
        page.click(SELECTORS["submit"])
        page.wait_for_load_state("networkidle")
        print(f"📋 매물 등록: {data.get('complex_name')} {data.get('dong')}동 {data.get('ho')}호")


def _fill_if(page: Page, selector: str, value):
    if value is None:
        return
    try:
        el = page.query_selector(selector)
        if el:
            el.fill(str(value))
    except Exception:
        pass


def _click_if_exists(page: Page, selector: str):
    try:
        el = page.query_selector(selector)
        if el:
            el.click()
            page.wait_for_timeout(800)
    except Exception:
        pass


def _select_if(page: Page, selector: str, value: str):
    try:
        el = page.query_selector(selector)
        if el:
            el.select_option(value=value)
    except Exception:
        pass

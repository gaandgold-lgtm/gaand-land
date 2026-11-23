"""플랫폼 기본 클래스 — 각 플랫폼은 이를 상속"""
from abc import ABC, abstractmethod
from playwright.sync_api import Page


class BasePlatform(ABC):
    def __init__(self, url: str, user_id: str, password: str):
        self.url = url
        self.user_id = user_id
        self.password = password

    @abstractmethod
    def login(self, page: Page): ...

    @abstractmethod
    def post_listing(self, page: Page, data: dict): ...

    def run(self, data: dict, headless: bool = False):
        from playwright.sync_api import sync_playwright
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=headless)
            page = browser.new_page()
            try:
                self.login(page)
                self.post_listing(page, data)
                print("✅ 등록 완료")
            except Exception as e:
                page.screenshot(path="error_screenshot.png")
                raise RuntimeError(f"등록 실패: {e}\n스크린샷 저장됨: error_screenshot.png")
            finally:
                browser.close()

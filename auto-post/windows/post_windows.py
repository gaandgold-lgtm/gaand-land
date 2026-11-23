"""
부동산알터(Windows) 매물 자동 등록
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
사용법:
  python post_windows.py --image 캡처.png
  python post_windows.py --text "전세 2억 ..."
  python post_windows.py --image 캡처.png --dry   (추출만, 등록 안 함)

⚠️  CONTROLS 딕셔너리는 inspect_window.py 결과 보고 조정 필요
"""
import argparse
import json
import os
import sys
import time
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent.parent / ".env")

# 상위 폴더의 extractor 사용
sys.path.insert(0, str(Path(__file__).parent.parent))
from extractor import extract_from_image, extract_from_text

try:
    from pywinauto import Application, Desktop
    from pywinauto.keyboard import send_keys
except ImportError:
    sys.exit("❌ pywinauto 없음. setup.bat 먼저 실행하세요.")

# ── 컨트롤 식별자 (inspect_window.py 결과 보고 채우세요) ──────────
# 형식: "항목명": ("컨트롤타입", "자동화ID 또는 텍스트")
# 예시: "dong": ("Edit", "txtDong")  또는  ("Edit", 3)  ← 탭 순서
CONTROLS = {
    # 창 제목 (일부만 써도 됨)
    "window_title":   "알터",

    # 매물 등록 버튼/메뉴
    "menu_new":       ("MenuItem", "매물등록"),   # 또는 버튼이면 ("Button", "매물등록")

    # 거래유형 (라디오 또는 콤보)
    "deal_type":      ("ComboBox", "cmbDealType"),

    # 주소 / 단지명
    "complex_name":   ("Edit", "txtComplex"),
    "dong":           ("Edit", "txtDong"),
    "ho":             ("Edit", "txtHo"),
    "floor":          ("Edit", "txtFloor"),

    # 면적
    "area_exclusive": ("Edit", "txtAreaExcl"),
    "area_supply":    ("Edit", "txtAreaSupply"),

    # 가격
    "price_sale":     ("Edit", "txtPriceSale"),
    "price_jeonse":   ("Edit", "txtPriceJeonse"),
    "deposit":        ("Edit", "txtDeposit"),
    "monthly_rent":   ("Edit", "txtMonthly"),

    # 기타
    "direction":      ("ComboBox", "cmbDirection"),
    "room_count":     ("Edit", "txtRoomCnt"),
    "management_fee": ("Edit", "txtManageFee"),
    "move_in_date":   ("Edit", "txtMoveIn"),
    "description":    ("Edit", "txtDesc"),

    # 저장/등록
    "btn_save":       ("Button", "저장"),
}

DEAL_TYPE_MAP = {
    "매매": "매매",
    "전세": "전세",
    "월세": "월세",
}


def connect_app(title_keyword: str):
    """부동산알터 창 연결"""
    try:
        app = Application(backend="uia").connect(
            title_re=f".*{title_keyword}.*", timeout=10
        )
        win = app.top_window()
        win.set_focus()
        print(f"✅ 연결됨: '{win.window_text()}'")
        return app, win
    except Exception as e:
        # uia 실패 시 win32 시도
        try:
            app = Application(backend="win32").connect(
                title_re=f".*{title_keyword}.*", timeout=5
            )
            win = app.top_window()
            win.set_focus()
            print(f"✅ 연결됨(win32): '{win.window_text()}'")
            return app, win
        except Exception:
            raise RuntimeError(
                f"'{title_keyword}' 창을 찾을 수 없습니다.\n"
                "부동산알터를 열고 다시 실행하세요.\n"
                f"원래 오류: {e}"
            )


def fill_field(win, ctrl_key: str, value):
    """컨트롤에 값 입력"""
    if value is None:
        return
    ctrl_type, ctrl_id = CONTROLS[ctrl_key]
    try:
        if ctrl_type == "Edit":
            ctrl = win.child_window(auto_id=ctrl_id, control_type="Edit")
            ctrl.set_text(str(value))
        elif ctrl_type == "ComboBox":
            ctrl = win.child_window(auto_id=ctrl_id, control_type="ComboBox")
            ctrl.select(str(value))
        elif ctrl_type == "Button":
            ctrl = win.child_window(title=ctrl_id, control_type="Button")
            ctrl.click()
        time.sleep(0.3)
    except Exception as e:
        print(f"⚠️  '{ctrl_key}' 입력 실패 (건너뜀): {e}")


def post_to_alter(data: dict):
    title = CONTROLS["window_title"]
    app, win = connect_app(title)

    # 매물 등록 메뉴 클릭
    try:
        menu_type, menu_id = CONTROLS["menu_new"]
        if menu_type == "MenuItem":
            app.menu_select(menu_id)
        else:
            win.child_window(title=menu_id, control_type="Button").click()
        time.sleep(1)
        win = app.top_window()
    except Exception as e:
        print(f"⚠️  매물등록 버튼/메뉴 클릭 실패: {e}")

    # 거래유형
    deal = DEAL_TYPE_MAP.get(data.get("deal_type", ""), "")
    if deal:
        fill_field(win, "deal_type", deal)

    # 단지/주소
    fill_field(win, "complex_name", data.get("complex_name"))
    fill_field(win, "dong",         data.get("dong"))
    fill_field(win, "ho",           data.get("ho"))
    fill_field(win, "floor",        data.get("floor"))

    # 면적
    fill_field(win, "area_exclusive", data.get("area_exclusive_m2"))
    fill_field(win, "area_supply",    data.get("area_supply_m2"))

    # 가격
    fill_field(win, "price_sale",   data.get("price_sale"))
    fill_field(win, "price_jeonse", data.get("price_jeonse"))
    fill_field(win, "deposit",      data.get("deposit"))
    fill_field(win, "monthly_rent", data.get("monthly_rent"))

    # 기타
    fill_field(win, "direction",      data.get("direction"))
    fill_field(win, "room_count",     data.get("room_count"))
    fill_field(win, "management_fee", data.get("management_fee"))
    fill_field(win, "move_in_date",   data.get("move_in_date"))
    fill_field(win, "description",    data.get("description"))

    # 저장
    fill_field(win, "btn_save", "")
    print(f"📋 등록 완료: {data.get('complex_name')} {data.get('dong')}동 {data.get('ho')}호")


def main():
    parser = argparse.ArgumentParser(description="부동산알터 Windows 자동 등록")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--image", help="매물 이미지 경로")
    group.add_argument("--text",  help="매물 텍스트")
    parser.add_argument("--dry", action="store_true", help="추출만, 등록 안 함")
    args = parser.parse_args()

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("❌ ANTHROPIC_API_KEY가 .env에 없습니다")

    print("📖 매물 정보 추출 중...")
    if args.image:
        data = extract_from_image(args.image, api_key)
    else:
        data = extract_from_text(args.text, api_key)

    print("\n📌 추출 결과:")
    print(json.dumps(data, ensure_ascii=False, indent=2))

    if args.dry:
        print("\n🔍 --dry 모드: 등록 생략")
        return

    print("\n⌨️  부동산알터에 입력 중...")
    post_to_alter(data)


if __name__ == "__main__":
    main()

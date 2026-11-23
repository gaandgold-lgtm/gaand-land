"""
부동산알터 창 구조 조사 도구
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
실행 방법:
  1. 부동산알터 프로그램을 열어 매물 등록 화면으로 이동
  2. 이 스크립트 실행: python inspect_window.py
  3. 출력된 내용을 복사해서 공유

결과물: window_controls.txt (자동 저장)
"""
import sys
import time
from pywinauto import Desktop, Application
from pywinauto.findwindows import find_windows


def list_all_windows():
    """현재 열린 모든 창 제목 출력"""
    print("\n[ 현재 열린 창 목록 ]")
    desktop = Desktop(backend="uia")
    wins = desktop.windows()
    for i, w in enumerate(wins):
        try:
            title = w.window_text()
            cls = w.class_name()
            if title.strip():
                print(f"  [{i:02d}] 제목: '{title}'  |  클래스: '{cls}'")
        except Exception:
            pass


def inspect_target(keyword: str):
    """키워드로 창 찾아서 컨트롤 구조 출력"""
    print(f"\n[ '{keyword}' 창 컨트롤 구조 조사 중... ]")
    try:
        app = Application(backend="uia").connect(title_re=f".*{keyword}.*", timeout=5)
        win = app.top_window()
        print(f"창 제목: {win.window_text()}\n")

        output = []
        output.append(f"창 제목: {win.window_text()}")
        output.append("=" * 60)

        # 컨트롤 트리 출력 (파일로도 저장)
        import io
        from contextlib import redirect_stdout
        f = io.StringIO()
        with redirect_stdout(f):
            win.print_control_identifiers(depth=6)
        ctrl_text = f.getvalue()
        print(ctrl_text)
        output.append(ctrl_text)

        # 파일 저장
        with open("window_controls.txt", "w", encoding="utf-8") as fout:
            fout.write("\n".join(output))
        print("\n✅ window_controls.txt 에 저장됐어요. 이 파일을 공유해주세요!")

    except Exception as e:
        print(f"❌ 창을 찾지 못했습니다: {e}")
        print("→ 프로그램이 열려 있는지 확인하고, 아래 목록에서 정확한 창 이름을 찾아주세요.")
        list_all_windows()


if __name__ == "__main__":
    # 프로그램 이름 키워드 (부동산알터 프로그램 창 제목의 일부)
    # 아래 keyword를 실제 창 제목에 맞게 수정하세요
    keyword = sys.argv[1] if len(sys.argv) > 1 else "알터"

    print("부동산 프로그램 창 구조 조사기")
    print("=" * 40)
    print(f"찾는 창 키워드: '{keyword}'")
    print("3초 후 시작... (부동산알터 매물등록 화면으로 미리 이동해 두세요)")
    time.sleep(3)

    inspect_target(keyword)

    print("\n[ 모든 창 목록도 확인 ]")
    list_all_windows()

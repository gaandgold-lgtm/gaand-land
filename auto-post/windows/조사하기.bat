@echo off
chcp 65001 > nul
echo 부동산 프로그램 창 조사 중...
echo.
pip install pywinauto -q 2>nul

python -c "
from pywinauto import Desktop
import json

results = []
desktop = Desktop(backend='uia')
for w in desktop.windows():
    try:
        t = w.window_text().strip()
        c = w.class_name()
        if t:
            results.append(f'  창제목: {t!r}  |  클래스: {c!r}')
    except:
        pass

print('=== 현재 열린 창 목록 ===')
for r in results:
    print(r)
print()
print('위 목록을 복사해서 Claude에게 붙여넣으세요.')
" > 창목록.txt 2>&1

type 창목록.txt
echo.
echo 창목록.txt 파일이 저장됐습니다. 이 파일 내용을 Claude에 붙여넣으세요.
pause

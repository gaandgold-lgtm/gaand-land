@echo off
echo [1] 패키지 설치 중...
pip install anthropic pywinauto python-dotenv pillow comtypes
echo.
echo [완료] 설치 끝. 다음 단계:
echo   1. auto-post\.env 파일 만들고 ANTHROPIC_API_KEY 입력
echo   2. 부동산알터 프로그램 열기
echo   3. python inspect_window.py 실행해서 창 구조 확인
pause

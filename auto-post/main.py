"""
부동산 매물 자동 등록 — 메인 실행 파일

사용법:
  python main.py --image 캡처.png          # 이미지에서 추출
  python main.py --text "전세 1억 ..."      # 텍스트에서 추출
  python main.py --dry                     # 추출만 (등록 안 함)
  python main.py --no-enrich               # 세움터 조회 생략
  python main.py --headless                # 브라우저 숨김
"""
import argparse
import json
import os
import sys
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / ".env")

from extractor import extract_from_image, extract_from_text
from enricher import fetch_seum, merge
from platforms.alter import AlterPlatform

REQUIRED_FIELDS = ["deal_type", "address", "floor", "area_exclusive_m2"]


def main():
    parser = argparse.ArgumentParser(description="부동산 매물 자동 등록")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--image", help="매물 정보 이미지 경로")
    group.add_argument("--text", help="매물 정보 텍스트")
    parser.add_argument("--dry", action="store_true", help="추출만 하고 등록 안 함")
    parser.add_argument("--no-enrich", action="store_true", help="세움터 조회 생략")
    parser.add_argument("--headless", action="store_true", help="브라우저 숨김")
    args = parser.parse_args()

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("❌ ANTHROPIC_API_KEY가 .env에 없습니다")

    # ── 1. 추출 ──────────────────────────────────────────
    print("📖 매물 정보 추출 중...")
    if args.image:
        data = extract_from_image(args.image, api_key)
    else:
        data = extract_from_text(args.text, api_key)

    print("\n📌 추출된 정보:")
    print(json.dumps(data, ensure_ascii=False, indent=2))

    # ── 2. 필수 항목 체크 ───────────────────────────────
    missing = [f for f in REQUIRED_FIELDS if not data.get(f)]
    if missing:
        print(f"\n⚠️  누락 항목: {missing}")

    # ── 3. 세움터 보완 ──────────────────────────────────
    if not args.no_enrich and data.get("address"):
        seum_id = os.environ.get("SEUM_ID")
        seum_pw = os.environ.get("SEUM_PW")
        if seum_id and seum_pw:
            print("\n🏛️  세움터 건축물대장 조회 중...")
            try:
                enriched = fetch_seum(data["address"], seum_id, seum_pw,
                                     headless=args.headless)
                data = merge(data, enriched)
                print("✅ 세움터 정보 보완 완료")
            except Exception as e:
                print(f"⚠️  세움터 조회 실패 (건너뜀): {e}")
        else:
            print("ℹ️  SEUM_ID/SEUM_PW 없어 세움터 조회 생략")

    # ── 4. 최종 확인 ────────────────────────────────────
    print("\n📋 최종 등록 데이터:")
    print(json.dumps(data, ensure_ascii=False, indent=2))

    if args.dry:
        print("\n🔍 --dry 모드: 등록 생략")
        return

    # ── 5. 등록 ─────────────────────────────────────────
    alter_url = os.environ.get("ALTER_URL")
    alter_id = os.environ.get("ALTER_ID")
    alter_pw = os.environ.get("ALTER_PW")
    if not all([alter_url, alter_id, alter_pw]):
        sys.exit("❌ ALTER_URL / ALTER_ID / ALTER_PW가 .env에 없습니다")

    platform = AlterPlatform(alter_url, alter_id, alter_pw)
    platform.run(data, headless=args.headless)


if __name__ == "__main__":
    main()

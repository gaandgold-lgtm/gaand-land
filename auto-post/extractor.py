"""
매물 정보 추출기
- 이미지(캡처본) 또는 텍스트에서 구조화된 매물 데이터 추출
- Claude Vision API 사용
"""
import base64
import json
import re
from pathlib import Path
import anthropic

PROMPT = """부동산 매물 정보를 추출하여 JSON만 반환하세요. 없는 항목은 null.

{
  "complex_name": "단지명/건물명",
  "address": "주소",
  "dong": "동",
  "ho": "호",
  "floor": 층수_숫자,
  "total_floors": 총층수_숫자,
  "area_exclusive_m2": 전용면적_숫자,
  "area_supply_m2": 공급면적_숫자,
  "deal_type": "매매 또는 전세 또는 월세",
  "price_sale": 매매가_만원_숫자,
  "price_jeonse": 전세가_만원_숫자,
  "deposit": 보증금_만원_숫자,
  "monthly_rent": 월세_만원_숫자,
  "direction": "남향 등",
  "room_count": 방수_숫자,
  "bathroom_count": 화장실수_숫자,
  "options": ["에어컨", "냉장고"],
  "parking": true_또는_false,
  "management_fee": 관리비_만원_숫자,
  "move_in_date": "즉시 또는 YYYY-MM-DD",
  "description": "특이사항",
  "contact": "연락처"
}

JSON 외 텍스트 절대 금지."""


def extract_from_image(image_path: str, api_key: str) -> dict:
    client = anthropic.Anthropic(api_key=api_key)
    img = Path(image_path)

    suffix = img.suffix.lower()
    media_map = {".jpg": "image/jpeg", ".jpeg": "image/jpeg",
                 ".png": "image/png", ".webp": "image/webp"}
    media_type = media_map.get(suffix, "image/jpeg")
    b64 = base64.standard_b64encode(img.read_bytes()).decode()

    msg = client.messages.create(
        model="claude-opus-4-5",
        max_tokens=1024,
        messages=[{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64",
                                             "media_type": media_type,
                                             "data": b64}},
                {"type": "text", "text": PROMPT}
            ]
        }]
    )
    return _parse(msg.content[0].text)


def extract_from_text(text: str, api_key: str) -> dict:
    client = anthropic.Anthropic(api_key=api_key)
    msg = client.messages.create(
        model="claude-haiku-4-5",
        max_tokens=1024,
        messages=[{
            "role": "user",
            "content": f"다음 매물 정보를 추출하세요:\n\n{text}\n\n{PROMPT}"
        }]
    )
    return _parse(msg.content[0].text)


def _parse(raw: str) -> dict:
    # 코드블록 제거
    raw = re.sub(r"```(?:json)?", "", raw).strip()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        m = re.search(r"\{.*\}", raw, re.DOTALL)
        if m:
            return json.loads(m.group())
        raise ValueError(f"JSON 파싱 실패:\n{raw}")

#!/usr/bin/env bash
# 사용법: sign.sh <서명 안 된 apk> <결과 apk>
# 예전 열쇠(공개됨) + 새 열쇠(GitHub Secrets)로 "열쇠 이어받기" 서명을 한다.
# 새 열쇠가 없으면 예전 열쇠만으로 서명한다(기존과 동일, 보호 안 됨).
set -euo pipefail
IN="$1"; OUT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"

# build-tools 위치 찾기 (ANDROID_HOME, ~/.android/sdk, 시스템 경로 순으로 시도)
_find_bt() {
  for dir in \
    "${ANDROID_HOME:-__none__}/build-tools" \
    "$HOME/.android/sdk/build-tools" \
    /usr/local/lib/android/sdk/build-tools; do
    [ -d "$dir" ] || continue
    local bt
    bt="$(ls -d "$dir"/* 2>/dev/null | sort -V | tail -1)"
    [ -n "$bt" ] && [ -f "$bt/apksigner" ] && { echo "$bt"; return 0; }
  done
  return 1
}
BT="$(_find_bt)" || { echo "ERROR: apksigner를 찾을 수 없어요. Android build-tools를 설치해야 해요." >&2; exit 1; }
echo "build-tools 경로: $BT"
ALIGNED="$(mktemp --suffix=.apk)"
"$BT/zipalign" -f -p 4 "$IN" "$ALIGNED"
OLD=(--ks "$HERE/../callback-app/gaand.jks" --ks-key-alias gaand --ks-pass pass:gaand1234 --key-pass pass:gaand1234)
if [ -n "${SIGN_KEY_B64:-}" ] && [ -n "${SIGN_KEY_PASS:-}" ]; then
  NEW="$(mktemp --suffix=.p12)"
  printf %s "$SIGN_KEY_B64" | tr -d ' \r\n' | base64 -d > "$NEW"
  "$BT/apksigner" sign "${OLD[@]}" \
    --next-signer --ks "$NEW" --ks-key-alias gaand2 --ks-pass env:SIGN_KEY_PASS \
    --lineage "$HERE/lineage.bin" --rotation-min-sdk-version 28 \
    --out "$OUT" "$ALIGNED"
  rm -f "$NEW"
  echo "새 열쇠로 이어받기 서명 완료: $OUT"
else
  echo "::warning::SIGN_KEY_B64 / SIGN_KEY_PASS 가 없어서 예전 열쇠로만 서명했어요 (보호 안 됨)"
  "$BT/apksigner" sign "${OLD[@]}" --out "$OUT" "$ALIGNED"
fi
rm -f "$ALIGNED"
"$BT/apksigner" verify --print-certs "$OUT" | grep -E "Signer|DN|SHA-256" || true

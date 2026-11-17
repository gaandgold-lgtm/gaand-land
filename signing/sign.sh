#!/usr/bin/env bash
# 사용법: sign.sh <서명 안 된 apk> <결과 apk>
# 예전 열쇠(공개됨) + 새 열쇠(GitHub Secrets)로 "열쇠 이어받기" 서명을 한다.
# 새 열쇠가 없으면 예전 열쇠만으로 서명한다(기존과 동일, 보호 안 됨).
set -euo pipefail
IN="$1"; OUT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
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

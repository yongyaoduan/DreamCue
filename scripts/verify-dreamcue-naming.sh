#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

memo_word="memo"
log_word="log"
memo_compound="${memo_word}${log_word}"
memo_pascal="Memo"
native_pascal="Native"
main_pascal="Main"
legacy_domain="com"
legacy_vendor="example"

patterns=(
  "${legacy_domain}.${legacy_vendor}.${memo_compound}"
  "${legacy_domain}/${legacy_vendor}/${memo_compound}"
  "Java_${legacy_domain}_${legacy_vendor}_${memo_compound}"
  "${memo_word}-android-ffi"
  "${memo_word}-core"
  "${memo_word}_android_ffi"
  "${memo_compound}"
  "${memo_pascal}Repository"
  "${memo_pascal}App"
  "${main_pascal}ViewModel"
  "${native_pascal}Bridge"
)

failed=0
for pattern in "${patterns[@]}"; do
  if rg -n --fixed-strings "$pattern" \
    --glob '!target/**' \
    --glob '!android/build/**' \
    --glob '!android/app/build/**' \
    --glob '!android/.gradle/**' \
    --glob '!dist/**' \
    --glob '!android/app/src/main/jniLibs/**' \
    .; then
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  echo "DreamCue naming check failed."
  exit 1
fi

echo "DreamCue naming check passed."

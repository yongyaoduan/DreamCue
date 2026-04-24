#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/android/app/src/main/jniLibs"

if ! command -v cargo >/dev/null 2>&1; then
  echo "cargo is not installed. Please install rustup first."
  exit 1
fi

if ! cargo ndk --help >/dev/null 2>&1; then
  echo "cargo-ndk is missing. Install it with: cargo install cargo-ndk"
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

cargo ndk \
  -t arm64-v8a \
  -t x86_64 \
  -o "$OUTPUT_DIR" \
  build \
  --package memo-android-ffi \
  --release

echo "Rust Android libraries have been generated in: $OUTPUT_DIR"

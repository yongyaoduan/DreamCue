#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_PATH="$ROOT_DIR/macos/DreamCueMac.xcodeproj"
SCHEME_NAME="DreamCueMac"
APP_NAME="DreamCue"
DERIVED_DATA_PATH="${DERIVED_DATA_PATH:-/tmp/dreamcue-macos-release}"
BUILD_APP_PATH="$DERIVED_DATA_PATH/Build/Products/Release/$APP_NAME.app"
DIST_DIR="${DREAMCUE_RELEASE_DIST_DIR:-$ROOT_DIR/dist}"
RELEASE_VERSION="${DREAMCUE_RELEASE_VERSION:-local}"
ZIP_PATH="$DIST_DIR/DreamCue-macos-$RELEASE_VERSION.zip"
INITIAL_ZIP_PATH="$DIST_DIR/DreamCue-macos-$RELEASE_VERSION.notary-upload.zip"
CODE_SIGN_IDENTITY="${DREAMCUE_MACOS_CODE_SIGN_IDENTITY:-}"
NOTARIZE="${DREAMCUE_NOTARIZE:-0}"
BUILD_LOG="${DREAMCUE_MACOS_BUILD_LOG:-/tmp/dreamcue-macos-release-build.log}"
export COPYFILE_DISABLE=1

require_notarization_secret() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "$name is required for macOS notarization." >&2
    exit 1
  fi
}

build_args=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME_NAME"
  -configuration Release
  -destination "platform=macOS,arch=arm64"
  -derivedDataPath "$DERIVED_DATA_PATH"
)

if [[ -n "$CODE_SIGN_IDENTITY" ]]; then
  if [[ -z "${APPLE_TEAM_ID:-}" ]]; then
    echo "APPLE_TEAM_ID is required when DREAMCUE_MACOS_CODE_SIGN_IDENTITY is set." >&2
    exit 1
  fi
  build_args+=(
    CODE_SIGN_STYLE=Manual
    CODE_SIGN_IDENTITY="$CODE_SIGN_IDENTITY"
    DEVELOPMENT_TEAM="$APPLE_TEAM_ID"
    OTHER_CODE_SIGN_FLAGS="--timestamp"
  )
fi

mkdir -p "$DIST_DIR"
rm -rf "$DERIVED_DATA_PATH"

if ! xcodebuild "${build_args[@]}" build >"$BUILD_LOG"; then
  tail -200 "$BUILD_LOG" >&2
  exit 1
fi

if [[ ! -d "$BUILD_APP_PATH" ]]; then
  echo "Release app was not built at $BUILD_APP_PATH" >&2
  exit 1
fi

codesign --verify --deep --strict "$BUILD_APP_PATH"

rm -f "$ZIP_PATH" "$INITIAL_ZIP_PATH"

if [[ "$NOTARIZE" == "1" ]]; then
  require_notarization_secret APPLE_ID
  require_notarization_secret APPLE_TEAM_ID
  require_notarization_secret APPLE_APP_SPECIFIC_PASSWORD

  ditto -c -k --norsrc --noextattr --noqtn --noacl --keepParent "$BUILD_APP_PATH" "$INITIAL_ZIP_PATH"
  xcrun notarytool submit "$INITIAL_ZIP_PATH" \
    --apple-id "$APPLE_ID" \
    --team-id "$APPLE_TEAM_ID" \
    --password "$APPLE_APP_SPECIFIC_PASSWORD" \
    --wait >&2
  xcrun stapler staple "$BUILD_APP_PATH" >&2
  xcrun stapler validate "$BUILD_APP_PATH" >&2
fi

ditto -c -k --norsrc --noextattr --noqtn --noacl --keepParent "$BUILD_APP_PATH" "$ZIP_PATH"
rm -f "$INITIAL_ZIP_PATH"

echo "$ZIP_PATH"

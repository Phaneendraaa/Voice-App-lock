#!/usr/bin/env bash
# Downloads the sherpa-onnx Android library (AAR) into app/libs.
# Run once before the first local build. CI runs it automatically.
set -euo pipefail

VER="1.13.7"
DEST="$(cd "$(dirname "$0")/.." && pwd)/app/libs"
FILE="$DEST/sherpa-onnx-$VER.aar"

mkdir -p "$DEST"
if [ -f "$FILE" ]; then
  echo "sherpa-onnx $VER already present."
  exit 0
fi

echo "Downloading sherpa-onnx $VER ..."
curl -L --fail --retry 3 -o "$FILE.part" \
  "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$VER/sherpa-onnx-$VER.aar"
mv "$FILE.part" "$FILE"
echo "Saved $FILE"

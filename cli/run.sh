#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$ROOT_DIR/build/classes/java/main"
SRC_DIR="$ROOT_DIR/src/main/java"

mkdir -p "$OUT_DIR"
find "$SRC_DIR" -name "*.java" > "$OUT_DIR/sources.txt"
javac -encoding UTF-8 -d "$OUT_DIR" @"$OUT_DIR/sources.txt"

exec java -cp "$OUT_DIR" com.r.Main "$@"
#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$PROJECT_DIR/out"

mkdir -p "$BUILD_DIR"
javac --release 17 -d "$BUILD_DIR" "$PROJECT_DIR"/src/main/java/com/gatekeeper/*.java
java -cp "$BUILD_DIR:$PROJECT_DIR/src/main/resources" com.gatekeeper.Main

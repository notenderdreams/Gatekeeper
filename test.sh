#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TEST_BUILD_DIR="$PROJECT_DIR/out-test"

test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/bedroom-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/street-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/shop-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/characters/alex-sprites.png"
test -s "$PROJECT_DIR/src/main/resources/assets/characters/mira-sprites.png"

mkdir -p "$TEST_BUILD_DIR"
javac --release 17 -d "$TEST_BUILD_DIR" \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GateType.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/Facing.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GameScene.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GameConstants.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/SaveData.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/SaveManager.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitRecipe.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitModel.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/AutoTester.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/CircuitModelTest.java
java -cp "$TEST_BUILD_DIR" com.gatekeeper.CircuitModelTest

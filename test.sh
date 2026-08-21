#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TEST_BUILD_DIR="$PROJECT_DIR/out-test"

test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/bedroom-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/street-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/backgrounds/shop-normal.png"
test -s "$PROJECT_DIR/src/main/resources/assets/characters/alex-sprites.png"
test -s "$PROJECT_DIR/src/main/resources/assets/characters/mira-sprites.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/canvas/node-texture.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/canvas/wire-end.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/canvas/endpoint.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/canvas/tester-plug.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/tester/tester.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/tester/tester-buttons.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/tester/tester-navigation-buttons.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/shop/shop.png"
test -s "$PROJECT_DIR/src/main/resources/assets/items/shop/shop-item.png"
test -s "$PROJECT_DIR/src/main/resources/assets/ui/notebook.png"
test -s "$PROJECT_DIR/src/main/resources/assets/ui/notebook.annotations.json"

mkdir -p "$TEST_BUILD_DIR"
javac --release 17 -d "$TEST_BUILD_DIR" \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GateType.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/Facing.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GameScene.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GameConstants.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/GameAssets.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitWireRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/LogicNodeRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/WorkbenchGraph.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/WorkbenchSnapshotCodec.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/TesterPlugLayout.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/NodeRadialMenu.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/SaveData.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/SaveManager.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitRecipe.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitModel.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/NotebookNoteEditor.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/AutoTester.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/AutoTesterRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/ShopProduct.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/ShopModel.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/ContractModel.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CraftedCircuitInventory.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/ProductGateRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CircuitPackageRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CraftCircuitModel.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/CraftCircuitRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/ShopRenderer.java \
  "$PROJECT_DIR"/src/main/java/com/gatekeeper/InventoryRenderer.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/CircuitModelTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/AutoTesterTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/AutoTesterRendererTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/LogicNodeRendererTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/WorkbenchGraphTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/TesterPlugLayoutTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/NodeRadialMenuTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/ShopModelTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/NotebookNoteEditorTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/CraftCircuitModelTest.java \
  "$PROJECT_DIR"/src/test/java/com/gatekeeper/CraftCircuitRendererTest.java
java -cp "$TEST_BUILD_DIR" com.gatekeeper.CircuitModelTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.AutoTesterTest
java -cp "$TEST_BUILD_DIR:$PROJECT_DIR/src/main/resources" com.gatekeeper.AutoTesterRendererTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.LogicNodeRendererTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.WorkbenchGraphTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.TesterPlugLayoutTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.NodeRadialMenuTest
java -Djava.awt.headless=true -cp "$TEST_BUILD_DIR:$PROJECT_DIR/src/main/resources" com.gatekeeper.ShopModelTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.NotebookNoteEditorTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.CraftCircuitModelTest
java -cp "$TEST_BUILD_DIR" com.gatekeeper.CraftCircuitRendererTest

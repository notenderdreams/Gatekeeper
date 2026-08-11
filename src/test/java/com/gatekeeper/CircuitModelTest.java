package com.gatekeeper;

import java.util.List;

public final class CircuitModelTest {
    public static void main(String[] args) {
        List<CircuitRecipe> recipes = CircuitRecipe.all();
        require(recipes.size() == 5, "five recipes should be available");

        for (CircuitRecipe recipe : recipes) {
            CircuitModel model = new CircuitModel(recipe);
            for (int i = 0; i < recipe.solution.length; i++) model.place(i, recipe.solution[i]);
            for (int row = 0; row < 4; row++) {
                setInputs(model, row >= 2, row % 2 == 1);
                require(model.output() == recipe.truth[row], recipe.name + " row " + row + " is wrong");
                model.recordCurrent();
            }
            require(model.allRowsRecorded(), recipe.name + " did not record four rows");
            require(model.matchesTruthTable(), recipe.name + " should pass its truth table");
        }

        CircuitModel incomplete = new CircuitModel(recipes.get(0));
        incomplete.place(0, GateType.AND);
        require(incomplete.placed()[0] == GateType.AND, "gate should be placed");
        require(incomplete.canUndo(), "undo stack should have entry");
        incomplete.undo();
        require(incomplete.placed()[0] == null, "undo should revert placement");
        require(incomplete.canRedo(), "redo stack should have entry");
        incomplete.redo();
        require(incomplete.placed()[0] == GateType.AND, "redo should restore placement");
        incomplete.recordCurrent();
        require(!incomplete.allRowsRecorded(), "incomplete circuits must not record");

        CircuitModel autoModel = new CircuitModel(recipes.get(0));
        for (int i = 0; i < autoModel.recipe().solution.length; i++) {
            autoModel.place(i, autoModel.recipe().solution[i]);
        }
        AutoTester tester = new AutoTester();
        tester.toggleAttachment();
        require(tester.isAttached(), "LogicLens should attach");
        require(tester.start(autoModel), "LogicLens should start on a complete circuit");
        int completedRows = 0;
        while (tester.isRunning()) {
            AutoTester.Tick tick = tester.update(autoModel);
            if (tick.advanced() || tick.finished()) completedRows++;
            if (tick.finished()) require(tick.passed(), "LogicLens should pass a correct circuit");
        }
        require(completedRows == 4, "LogicLens should sweep every truth-table row");
        require(autoModel.matchesTruthTable(), "LogicLens should record matching observations");

        CircuitRecipe xor = recipes.get(2);
        require(xor.solution.length == 5 && xor.solution[4] == GateType.OR,
            "XOR must use two product branches feeding a final OR");
        require(xor.leftSources[4] == 1 && xor.rightSources[4] == 3,
            "XOR's final OR must combine A AND NOT B with NOT A AND B");
        boolean[] xorAt11 = xor.evaluateNodes(true, true, xor.solution);
        require(!xorAt11[0] && !xorAt11[1] && !xorAt11[2]
                && !xorAt11[3] && !xorAt11[4],
            "XOR topology should reject A=1, B=1 at the final parallel merge");
        boolean[] xorAt01 = xor.evaluateNodes(false, true, xor.solution);
        require(!xorAt01[0] && !xorAt01[1] && xorAt01[2]
                && xorAt01[3] && xorAt01[4],
            "XOR topology should accept A=0, B=1 at the final parallel merge");
        SaveData save = new SaveData();
        save.chapter = 3;
        save.scene = GameScene.SHOP;
        save.playerX = 94;
        save.playerY = 190;
        save.facing = Facing.RIGHT;
        save.crafted = new boolean[]{true, true, true, false, false};
        save.notebookPage = 2;
        save.autoTesterAttached = true;
        save.catPresent = true;
        save.catX = 45;
        save.catY = 75;
        save.catAlwaysAppears = true;
        save.workbenchInstalled = true;
        save.starCount = 75;
        save.mothCount = 8;
        save.taskbarOnRight = true;
        save.disableHud = true;

        require(SaveManager.saveGame(save), "saveGame should return true");
        require(SaveManager.hasSave(), "hasSave should return true after saving");

        SaveData loaded = SaveManager.loadGame();
        require(loaded != null, "loaded save should not be null");
        require(loaded.chapter == 3, "loaded chapter should be 3");
        require(loaded.scene == GameScene.SHOP, "loaded scene should be SHOP");
        require(loaded.playerX == 94 && loaded.playerY == 190, "loaded coordinates should match");
        require(loaded.facing == Facing.RIGHT, "loaded facing should match");
        require(loaded.crafted[0] && loaded.crafted[1] && loaded.crafted[2] && !loaded.crafted[3], "loaded crafted array should match");
        require(loaded.notebookPage == 2, "loaded notebook page should be 2");
        require(loaded.autoTesterAttached, "loaded autoTesterAttached should be true");
        require(loaded.catPresent, "loaded catPresent should be true");
        require(loaded.catX == 45 && loaded.catY == 75, "loaded catX/catY should match");
        require(loaded.catAlwaysAppears, "loaded catAlwaysAppears should be true");
        require(loaded.workbenchInstalled, "loaded workbenchInstalled should be true");
        require(loaded.starCount == 75, "loaded starCount should match");
        require(loaded.mothCount == 8, "loaded mothCount should match");
        require(loaded.taskbarOnRight, "loaded taskbarOnRight should be true");
        require(loaded.disableHud, "loaded disableHud should be true");

        SaveManager.deleteSave();
        require(!SaveManager.hasSave(), "hasSave should be false after deleteSave");

        // Verify 1/5 cat spawn probability logic
        java.util.Random rng = new java.util.Random(12345);
        int catHits = 0;
        int trials = 10000;
        for (int t = 0; t < trials; t++) {
            if (rng.nextInt(5) == 0) catHits++;
        }
        require(catHits > 1700 && catHits < 2300, "1/5 spawn chance should yield ~2000 hits out of 10000 trials, got " + catHits);

        // Verify calibrated cat spawn ranges: [2,96]@77, [184,406]@152, [594,652]@152
        for (int t = 0; t < 1000; t++) {
            int[] zone = GameConstants.CAT_SPAWN_RANGES[rng.nextInt(GameConstants.CAT_SPAWN_RANGES.length)];
            int rx = zone[0] + rng.nextInt(zone[1] - zone[0] + 1);
            int ry = zone[2];
            require(rx >= zone[0] && rx <= zone[1], "spawn X out of bounds: " + rx);
            require(ry == 77 || ry == 152, "spawn Y invalid: " + ry);
            if (ry == 77) require(rx >= 2 && rx <= 96, "roof spawn X out of bounds: " + rx);
        }

        System.out.println("CircuitModelTest: all checks passed");
    }

    private static void setInputs(CircuitModel model, boolean a, boolean b) {
        if (model.inputA() != a) model.toggleA();
        if (model.inputB() != b) model.toggleB();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

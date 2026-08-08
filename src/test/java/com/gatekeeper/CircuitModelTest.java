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
        incomplete.recordCurrent();
        require(!incomplete.allRowsRecorded(), "incomplete circuits must not record");

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

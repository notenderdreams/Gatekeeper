package com.gatekeeper;

import java.util.List;

public final class CircuitRecipe {
    public static final int INPUT_A = -1;
    public static final int INPUT_B = -2;

    public final String name;
    public final String subtitle;
    public final GateType[] solution;
    public final int[] leftSources;
    public final int[] rightSources;
    public final boolean[] truth;
    public final boolean advanced;

    private CircuitRecipe(String name, String subtitle, boolean advanced,
                          GateType[] solution, int[] leftSources, int[] rightSources,
                          boolean... truth) {
        this.name = name;
        this.subtitle = subtitle;
        this.advanced = advanced;
        this.solution = solution;
        this.leftSources = leftSources;
        this.rightSources = rightSources;
        this.truth = truth;
        if (solution.length != leftSources.length || solution.length != rightSources.length) {
            throw new IllegalArgumentException("Every gate socket needs two routed input sources");
        }
    }

    public int slotCount() {
        return solution.length;
    }

    public boolean evaluate(boolean a, boolean b, GateType[] placed) {
        if (!isComplete(placed)) return false;
        boolean[] nodes = evaluateNodes(a, b, placed);
        return nodes[nodes.length - 1];
    }

    public boolean[] evaluateNodes(boolean a, boolean b, GateType[] placed) {
        boolean[] nodes = new boolean[solution.length];
        for (int i = 0; i < placed.length; i++) {
            GateType gate = placed[i];
            if (gate == null) continue;
            boolean left = sourceValue(leftSources[i], a, b, nodes);
            boolean right = sourceValue(rightSources[i], a, b, nodes);
            nodes[i] = switch (gate) {
                case AND -> left && right;
                case OR -> left || right;
                case NOT -> !left;
            };
        }
        return nodes;
    }

    public boolean sourceValue(int source, boolean a, boolean b, boolean[] nodes) {
        if (source == INPUT_A) return a;
        if (source == INPUT_B) return b;
        return source >= 0 && source < nodes.length && nodes[source];
    }

    public boolean isComplete(GateType[] placed) {
        if (placed.length != solution.length) return false;
        for (GateType gate : placed) if (gate == null) return false;
        return true;
    }

    public static List<CircuitRecipe> all() {
        return List.of(
            new CircuitRecipe("NAND", "AND, then turn the answer around", false,
                new GateType[]{GateType.AND, GateType.NOT},
                new int[]{INPUT_A, 0}, new int[]{INPUT_B, 0},
                true, true, true, false),
            new CircuitRecipe("NOR", "OR, then turn the answer around", false,
                new GateType[]{GateType.OR, GateType.NOT},
                new int[]{INPUT_A, 0}, new int[]{INPUT_B, 0},
                true, false, false, false),
            new CircuitRecipe("XOR", "One or the other — never both", false,
                new GateType[]{GateType.NOT, GateType.AND, GateType.NOT, GateType.AND, GateType.OR},
                new int[]{INPUT_B, INPUT_A, INPUT_A, 2, 1},
                new int[]{INPUT_B, 0, INPUT_A, INPUT_B, 3},
                false, true, true, false),
            new CircuitRecipe("XNOR", "XOR's quiet opposite", true,
                new GateType[]{GateType.OR, GateType.AND, GateType.NOT, GateType.AND, GateType.NOT},
                new int[]{INPUT_A, INPUT_A, 1, 0, 3},
                new int[]{INPUT_B, INPUT_B, 1, 2, 3},
                true, false, false, true),
            new CircuitRecipe("IMPLY", "If A, then B", true,
                new GateType[]{GateType.NOT, GateType.OR},
                new int[]{INPUT_A, 0}, new int[]{INPUT_A, INPUT_B},
                true, true, false, true)
        );
    }
}

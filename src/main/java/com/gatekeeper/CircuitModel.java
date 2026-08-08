package com.gatekeeper;

import java.util.Arrays;

public final class CircuitModel {
    private CircuitRecipe recipe;
    private GateType[] placed;
    private final Boolean[] observations = new Boolean[4];
    private boolean inputA;
    private boolean inputB;

    private final java.util.ArrayDeque<GateType[]> undoStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<GateType[]> redoStack = new java.util.ArrayDeque<>();

    public CircuitModel(CircuitRecipe recipe) {
        selectRecipe(recipe);
    }

    public void selectRecipe(CircuitRecipe next) {
        recipe = next;
        placed = new GateType[next.slotCount()];
        undoStack.clear();
        redoStack.clear();
        Arrays.fill(observations, null);
        inputA = false;
        inputB = false;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        redoStack.push(placed.clone());
        placed = undoStack.pop();
        Arrays.fill(observations, null);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        undoStack.push(placed.clone());
        placed = redoStack.pop();
        Arrays.fill(observations, null);
        return true;
    }

    public CircuitRecipe recipe() { return recipe; }
    public GateType[] placed() { return placed; }
    public Boolean[] observations() { return observations; }
    public boolean inputA() { return inputA; }
    public boolean inputB() { return inputB; }
    public void setInputs(boolean a, boolean b) {
        inputA = a;
        inputB = b;
    }
    public void toggleA() { inputA = !inputA; }
    public void toggleB() { inputB = !inputB; }

    public void clearObservations() { Arrays.fill(observations, null); }

    public void place(int slot, GateType gate) {
        if (slot >= 0 && slot < placed.length && placed[slot] != gate) {
            undoStack.push(placed.clone());
            redoStack.clear();
            placed[slot] = gate;
            Arrays.fill(observations, null);
        }
    }

    public boolean output() {
        return recipe.evaluate(inputA, inputB, placed);
    }

    public boolean[] nodeValues() {
        return recipe.evaluateNodes(inputA, inputB, placed);
    }

    public void recordCurrent() {
        if (!recipe.isComplete(placed)) return;
        int row = (inputA ? 2 : 0) + (inputB ? 1 : 0);
        observations[row] = output();
    }

    public boolean allRowsRecorded() {
        for (Boolean result : observations) if (result == null) return false;
        return true;
    }

    public boolean matchesTruthTable() {
        if (!allRowsRecorded()) return false;
        for (int i = 0; i < 4; i++) {
            if (observations[i] != recipe.truth[i]) return false;
        }
        return true;
    }
}

package com.gatekeeper;

import java.util.Arrays;

/**
 * Drives the LogicLens' timed four-row truth-table sweep over a workbench graph.
 *
 * <p>The workbench owns presentation and sound effects; this class owns only tester
 * attachment, observations, and test progression. The selected target describes
 * expected behavior only; it never changes the graph being tested.</p>
 */
final class AutoTester {
    private static final int FRAMES_PER_ROW = 36;

    record Tick(boolean advanced, boolean finished, boolean passed) {
        static final Tick IDLE = new Tick(false, false, false);
    }

    private boolean attached;
    private boolean running;
    private int row = -1;
    private int frame;
    private int failures;
    private CircuitRecipe target;
    private final Boolean[] observations = new Boolean[4];

    boolean isAttached() { return attached; }
    boolean isRunning() { return running; }
    int currentRow() { return row; }
    CircuitRecipe target() { return target; }
    Boolean[] observations() { return observations.clone(); }

    void toggleAttachment() { attached = !attached; }

    void reset() {
        attached = false;
        running = false;
        row = -1;
        frame = 0;
        failures = 0;
        target = null;
        Arrays.fill(observations, null);
    }

    boolean start(CircuitRecipe nextTarget) {
        if (running || nextTarget == null) return false;
        running = true;
        row = 0;
        frame = 0;
        failures = 0;
        target = nextTarget;
        Arrays.fill(observations, null);
        return true;
    }

    Tick update(WorkbenchGraph graph) {
        if (!running || ++frame < FRAMES_PER_ROW) return Tick.IDLE;

        boolean actual = graph.outputValue(0,
            new boolean[] { row >= 2, row % 2 == 1 });
        observations[row] = actual;
        if (actual != target.truth[row]) failures++;

        if (row == 3) {
            running = false;
            row = -1;
            return new Tick(false, true, failures == 0);
        }

        row++;
        frame = 0;
        return new Tick(true, false, false);
    }

    boolean stop() {
        if (!running) return false;
        running = false;
        row = -1;
        frame = 0;
        return true;
    }

    void clearResults() {
        running = false;
        row = -1;
        frame = 0;
        failures = 0;
        Arrays.fill(observations, null);
    }
}

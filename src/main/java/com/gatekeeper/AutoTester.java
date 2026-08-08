package com.gatekeeper;

/**
 * Drives the LogicLens' timed four-row truth-table sweep.
 *
 * <p>The workbench owns presentation and sound effects; this class owns only tester
 * attachment and test progression, making it usable by another board UI later.</p>
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

    boolean isAttached() { return attached; }
    boolean isRunning() { return running; }
    int currentRow() { return row; }

    void toggleAttachment() { attached = !attached; }

    void reset() {
        attached = false;
        running = false;
        row = -1;
        frame = 0;
        failures = 0;
    }

    boolean start(CircuitModel circuit) {
        if (running || !circuit.recipe().isComplete(circuit.placed())) return false;
        running = true;
        row = 0;
        frame = 0;
        failures = 0;
        circuit.clearObservations();
        circuit.setInputs(false, false);
        return true;
    }

    Tick update(CircuitModel circuit) {
        if (!running || ++frame < FRAMES_PER_ROW) return Tick.IDLE;

        boolean actual = circuit.output();
        circuit.recordCurrent();
        if (actual != circuit.recipe().truth[row]) failures++;

        if (row == 3) {
            running = false;
            row = -1;
            return new Tick(false, true, failures == 0);
        }

        row++;
        frame = 0;
        circuit.setInputs(row >= 2, row % 2 == 1);
        return new Tick(true, false, false);
    }
}

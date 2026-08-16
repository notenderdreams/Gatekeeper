package com.gatekeeper;

/** Logical placement and source-to-screen sizing for the LogicLens plug. */
final class TesterPlugLayout {
    private static final int CANVAS_WIDTH = 1536;
    private static final int CANVAS_HEIGHT = 1024;

    static final int LOGICAL_X = 387;
    static final int LOGICAL_Y = 236;
    static final int SOURCE_X = toSourceX(LOGICAL_X);
    static final int SOURCE_Y = toSourceY(LOGICAL_Y);

    private TesterPlugLayout() {}

    static int logicalWidth(int sourceWidth) {
        return Math.max(1, (int) Math.round(
            sourceWidth * GameConstants.W / (double) CANVAS_WIDTH));
    }

    static int logicalHeight(int sourceHeight) {
        return Math.max(1, (int) Math.round(
            sourceHeight * GameConstants.H / (double) CANVAS_HEIGHT));
    }

    private static int toSourceX(int logicalX) {
        return (int) Math.round(logicalX * CANVAS_WIDTH / (double) GameConstants.W);
    }

    private static int toSourceY(int logicalY) {
        return (int) Math.round(logicalY * CANVAS_HEIGHT / (double) GameConstants.H);
    }
}

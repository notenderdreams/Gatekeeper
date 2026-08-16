package com.gatekeeper;

public final class TesterPlugLayoutTest {
    private TesterPlugLayoutTest() {}

    public static void main(String[] args) {
        require(TesterPlugLayout.SOURCE_X == 1238
                && TesterPlugLayout.SOURCE_Y == 895,
            "the supplied logical coordinate should map into the source canvas");
        require(TesterPlugLayout.logicalWidth(136) == 43,
            "the plug width should follow the canvas-to-screen scale");
        require(TesterPlugLayout.logicalHeight(117) == 31,
            "the plug height should follow the canvas-to-screen scale");

        System.out.println("TesterPlugLayoutTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

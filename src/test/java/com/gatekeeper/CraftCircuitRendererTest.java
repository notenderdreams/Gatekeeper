package com.gatekeeper;

public final class CraftCircuitRendererTest {
    public static void main(String[] args) {
        CraftCircuitRenderer renderer = new CraftCircuitRenderer();
        require(CraftCircuitRenderer.panelBounds().width == 330, "panel width should be 330");
        require(CraftCircuitRenderer.panelBounds().height == 218, "panel height should be 218");

        // Test action hit testing
        require(renderer.actionAt(386, 36) == CraftCircuitRenderer.Action.CLOSE, "close button click");
        require(renderer.actionAt(140, 198) == CraftCircuitRenderer.Action.CRAFT, "craft button click");
        require(renderer.actionAt(250, 198) == CraftCircuitRenderer.Action.CANCEL, "cancel button click");
        require(renderer.actionAt(370, 70) == CraftCircuitRenderer.Action.CLEAR, "clear button click");
        require(renderer.actionAt(372, 105) == CraftCircuitRenderer.Action.SCROLL_UP, "scroll up button click");
        require(renderer.actionAt(372, 168) == CraftCircuitRenderer.Action.SCROLL_DOWN, "scroll down button click");
        require(renderer.actionAt(10, 10) == CraftCircuitRenderer.Action.NONE, "outside panel click");

        // Test item hit testing
        int itemIndex = renderer.itemIndexAt(120, 106, 5, 0);
        require(itemIndex == 0, "first visible row should map to index 0");
        itemIndex = renderer.itemIndexAt(120, 124, 5, 0);
        require(itemIndex == 1, "second visible row should map to index 1");
        itemIndex = renderer.itemIndexAt(120, 106, 5, 2);
        require(itemIndex == 2, "first visible row with offset 2 should map to index 2");
        itemIndex = renderer.itemIndexAt(120, 160, 2, 0);
        require(itemIndex == -1, "clicking empty row below count should return -1");

        System.out.println("CraftCircuitRendererTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

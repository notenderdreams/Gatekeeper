package com.gatekeeper;

import java.util.List;

public final class NodeRadialMenuTest {
    private NodeRadialMenuTest() {}

    public static void main(String[] args) {
        NodeRadialMenu menu = new NodeRadialMenu();
        menu.openAt(700, 400, List.of(GateType.AND, GateType.OR, GateType.NOT));
        require(menu.isOpen(), "holding E should open the node wheel");
        require(menu.unlocked().size() == 3, "wheel should show every unlocked node type");
        require(menu.gateAt(700, 265) == GateType.AND,
            "top wedge should select AND");
        require(menu.gateAt(817, 468) == GateType.OR,
            "lower-right wedge should select OR");
        require(menu.gateAt(583, 468) == GateType.NOT,
            "lower-left wedge should select NOT");
        require(menu.gateAt(700, 400) == null,
            "the circular center should not select a node");
        require(menu.gateAt(950, 400) == null,
            "clicks outside the wheel should not select a node");
        require(menu.releaseAt(817, 468) == GateType.OR,
            "releasing E over a wedge should select its gate");
        require(!menu.isOpen(), "releasing E should close the wheel");

        menu.openAt(700, 400, List.of(GateType.AND, GateType.OR, GateType.NOT));
        require(menu.releaseAt(700, 400) == null,
            "releasing E in the center should leave the active gate unchanged");
        require(!menu.isOpen(), "a center release should still close the wheel");

        menu.openAt(WorkbenchGraph.WORK_X, WorkbenchGraph.WORK_Y,
            List.of(GateType.AND, GateType.AND));
        require(WorkbenchGraph.WORK_X == 192 && WorkbenchGraph.WORK_Y == 47,
            "node canvas origin should match canvas.annotations.json");
        require(WorkbenchGraph.WORK_WIDTH == 1293 && WorkbenchGraph.WORK_HEIGHT == 750,
            "node canvas size should match canvas.annotations.json");
        require(menu.centerX() == WorkbenchGraph.WORK_X + NodeRadialMenu.OUTER_RADIUS_X,
            "wheel should stay inside the left workbench edge");
        require(menu.centerY() == WorkbenchGraph.WORK_Y + NodeRadialMenu.OUTER_RADIUS_Y,
            "wheel should stay inside the top workbench edge");
        require(menu.unlocked().size() == 1,
            "duplicate unlocked types should appear only once");

        System.out.println("NodeRadialMenuTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

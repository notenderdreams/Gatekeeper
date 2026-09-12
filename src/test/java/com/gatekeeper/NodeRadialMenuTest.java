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

        WorkbenchGraph craftedGraph = new WorkbenchGraph(CircuitRecipe.all().get(0));
        CraftedCircuitInventory crafted = new CraftedCircuitInventory();
        require(crafted.add("NAND", craftedGraph.snapshot()),
            "test circuit should be stored");
        menu.openAt(700, 400, List.of(GateType.AND), crafted.circuits());
        NodeRadialMenu.Choice craftedChoice = menu.choiceAt(700, 535);
        require(craftedChoice != null && craftedChoice.isCrafted()
                && craftedChoice.craftedCircuitIndex() == 0,
            "crafted circuits should follow primitive gates in the wheel");
        require(menu.gateAt(700, 535) == null,
            "a crafted circuit choice must not be mistaken for a primitive gate");

        ShopModel shop = new ShopModel(ShopProduct.catalog(), 250);
        shop.grant("AND", 5);
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.AND), shop) == 5,
            "AND gate count should reflect purchased inventory");
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.OR), shop) == 0,
            "OR gate count should be 0 when unpurchased");
        require(menu.choiceCount(craftedChoice, shop) == 1,
            "crafted circuit count should reflect crafted circuit inventory");

        // Test count deduction when gates are placed on the workbench
        WorkbenchGraph bench = new WorkbenchGraph(CircuitRecipe.all().get(0));
        bench.clear();
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.AND), shop, bench) == 5,
            "count should be 5 on empty workbench");
        bench.click(500, 300, GateType.AND);
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.AND), shop, bench) == 4,
            "count should decrease to 4 when 1 AND gate is placed");
        bench.click(650, 300, GateType.AND);
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.AND), shop, bench) == 3,
            "count should decrease to 3 when 2 AND gates are placed");
        bench.deleteSelected();
        require(menu.choiceCount(NodeRadialMenu.Choice.gate(GateType.AND), shop, bench) == 4,
            "count should increase back to 4 when a placed gate is removed");

        System.out.println("NodeRadialMenuTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

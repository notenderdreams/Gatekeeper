package com.gatekeeper;

import java.util.ArrayList;
import java.util.List;

public final class CraftCircuitModelTest {
    public static void main(String[] args) {
        List<CircuitRecipe> recipes = CircuitRecipe.all();
        WorkbenchGraph nandGraph1 = new WorkbenchGraph(recipes.get(0));
        WorkbenchGraph norGraph = new WorkbenchGraph(recipes.get(1));

        // Create an equivalent NAND graph with shifted positions
        WorkbenchGraph.Snapshot nandSnap1 = nandGraph1.snapshot();
        List<WorkbenchGraph.NodeData> shiftedNodes = new ArrayList<>();
        for (WorkbenchGraph.NodeData node : nandSnap1.nodes()) {
            shiftedNodes.add(new WorkbenchGraph.NodeData(node.id() + 10, node.x() + 50,
                node.centerY() + 30, node.gate()));
        }
        List<WorkbenchGraph.WireData> shiftedWires = new ArrayList<>();
        for (WorkbenchGraph.WireData wire : nandSnap1.wires()) {
            int src = wire.sourceId() >= 0 ? wire.sourceId() + 10 : wire.sourceId();
            int tgt = wire.targetId() >= 0 ? wire.targetId() + 10 : wire.targetId();
            shiftedWires.add(new WorkbenchGraph.WireData(src, tgt, wire.targetPort(), List.of()));
        }
        WorkbenchGraph.Snapshot nandSnapShifted = new WorkbenchGraph.Snapshot(
            shiftedNodes, shiftedWires, List.of());

        require(WorkbenchGraph.isSameArchitecture(nandSnap1, nandSnapShifted),
            "isSameArchitecture should return true for shifted nodes with same topology");
        require(!WorkbenchGraph.isSameArchitecture(nandSnap1, norGraph.snapshot()),
            "isSameArchitecture should return false for different circuit architectures");

        // Test CraftCircuitModel candidate filtering based on architecture
        CraftedCircuitInventory.CraftedCircuit matchingNand =
            new CraftedCircuitInventory.CraftedCircuit("MYNAND", nandSnapShifted);
        CraftedCircuitInventory.CraftedCircuit nonMatchingNor =
            new CraftedCircuitInventory.CraftedCircuit("MYNOR", norGraph.snapshot());

        CraftCircuitModel model = new CraftCircuitModel();
        model.open(List.of(matchingNand, nonMatchingNor), nandSnap1, "");

        require(model.allCandidates().contains("MYNAND"), "candidates should include matching custom circuit");
        require(!model.allCandidates().contains("MYNOR"), "candidates should exclude non-matching circuit architecture");
        require(model.isBagName("MYNAND"), "MYNAND should be identified as bag name");
        require(model.draft().isEmpty(), "draft should be empty when no initial name is provided");
        require(model.filteredCandidates().contains("MYNAND"), "all candidates should be visible when draft is empty");

        // Test navigation
        model.navigateSelection(1);
        require(model.draft().equals("MYNAND"), "navigating should select MYNAND and update draft");

        // Test search filtering
        model.setDraft("NAND");
        require(model.filteredCandidates().contains("MYNAND"), "filtered should include MYNAND");

        model.setDraft("XYZ");
        require(model.filteredCandidates().isEmpty(), "filtered should be empty for non-matching query");

        // Test typing and character limits
        model.clearDraft();
        require(model.draft().isEmpty(), "draft should be empty after clear");
        require(model.filteredCandidates().size() == model.allCandidates().size(),
            "empty draft should show all candidates");

        require(model.typeChar('c'), "should accept letter");
        require(model.typeChar('1'), "should accept digit");
        require(model.typeChar('-'), "should accept hyphen");
        require(model.typeChar('_'), "should accept underscore");
        require(!model.typeChar('@'), "should reject special symbol");
        require(model.draft().equals("C1-_"), "draft should be upper case sanitized");

        model.setDraft("123456");
        require(!model.typeChar('7'), "should reject char when max length reached");
        require(model.draft().equals("123456"), "draft should remain 6 chars");

        // Test backspace
        require(model.backspace(), "backspace should succeed");
        require(model.draft().equals("12345"), "draft should be 12345");

        // Test scrolling and ensureVisible with multiple matching custom circuits
        List<CraftedCircuitInventory.CraftedCircuit> manyCircuits = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            manyCircuits.add(new CraftedCircuitInventory.CraftedCircuit("CKT" + i, nandSnapShifted));
        }
        model.open(manyCircuits, nandSnap1, "");
        require(model.allCandidates().size() == 7, "all 7 matching circuits should be in candidates");
        require(model.filteredCandidates().size() == 7, "all 7 matching circuits should be in filtered list initially");
        require(model.maxScroll() == 7 - CraftCircuitModel.VISIBLE_ROWS, "max scroll should match formula");
        model.scroll(2);
        require(model.scrollOffset() == 2, "scroll offset should be 2");
        model.scroll(10);
        require(model.scrollOffset() == model.maxScroll(), "scroll offset should clamp to max");
        model.scroll(-10);
        require(model.scrollOffset() == 0, "scroll offset should clamp to 0");

        model.selectIndex(6);
        require(model.scrollOffset() >= 3, "ensureVisible should scroll to bottom item");

        System.out.println("CraftCircuitModelTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

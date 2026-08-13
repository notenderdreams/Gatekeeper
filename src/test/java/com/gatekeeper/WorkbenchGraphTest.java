package com.gatekeeper;

public final class WorkbenchGraphTest {
    private WorkbenchGraphTest() {}

    public static void main(String[] args) {
        WorkbenchGraph graph = new WorkbenchGraph(CircuitRecipe.all().get(0));
        require(graph.nodes().size() == 2, "NAND preview should start with two nodes");
        require(graph.wires().size() == 4, "NAND preview should start fully wired");
        require(graph.click(WorkbenchGraph.OUTPUT_X, WorkbenchGraph.OUTPUT_Y, GateType.AND)
                == WorkbenchGraph.EditResult.NONE,
            "clicking OUT without a pending wire should not add a node");
        require(graph.nodes().size() == 2, "terminal clicks must stay outside placement handling");

        require(graph.click(760, 690, GateType.OR) == WorkbenchGraph.EditResult.ADDED,
            "empty canvas click should add the active gate");
        WorkbenchGraph.Node source = graph.nodes().get(2);
        require(source.gate() == GateType.OR, "new node should use the active gate type");
        require(Integer.valueOf(source.id()).equals(graph.selectedNodeId()),
            "new node should become selected");

        require(graph.click(1040, 650, GateType.NOT) == WorkbenchGraph.EditResult.ADDED,
            "a second empty click should add another node");
        WorkbenchGraph.Node target = graph.nodes().get(3);
        require(target.gate() == GateType.NOT, "second node should preserve its selected type");

        int[] sourcePort = graph.sourcePoint(source.id());
        int[] targetPort = graph.targetPoint(target.id(), 0);
        require(graph.click(sourcePort[0], sourcePort[1], GateType.AND)
                == WorkbenchGraph.EditResult.WIRE_STARTED,
            "clicking an output should start a wire");
        require(graph.click(targetPort[0], targetPort[1], GateType.AND)
                == WorkbenchGraph.EditResult.WIRED,
            "clicking an input should finish the wire");
        require(graph.wires().size() == 5, "completed wire should be retained");

        require(graph.deleteSelected(), "Backspace action should delete the selected node");
        require(graph.node(target.id()) == null, "selected node should be removed");
        require(graph.wires().size() == 4, "deleting a node should remove attached wires");

        int[] ownOutput = graph.sourcePoint(source.id());
        int[] ownInput = graph.targetPoint(source.id(), 0);
        graph.click(ownOutput[0], ownOutput[1], GateType.AND);
        require(graph.click(ownInput[0], ownInput[1], GateType.AND)
                == WorkbenchGraph.EditResult.INVALID_WIRE,
            "a node should not wire into itself");
        require(graph.cancelWire(), "Escape action should cancel an unfinished wire");

        int[] routeStart = graph.sourcePoint(source.id());
        graph.click(routeStart[0], routeStart[1], GateType.AND);
        require(graph.click(930, 730, GateType.AND)
                == WorkbenchGraph.EditResult.WIRE_CORNER,
            "clicking canvas during wiring should commit an intermediate corner");
        require(graph.pendingCorners().size() == 1,
            "manual routing should retain the committed corner");
        WorkbenchGraph.RoutePoint corner = graph.pendingCorners().get(0);
        require(corner.x() == 928 && corner.y() == routeStart[1],
            "wire corners should snap to the grid and remain orthogonal");
        require(graph.undoWireCorner(), "Backspace should undo the latest wire corner");
        require(graph.pendingCorners().isEmpty(), "undone route corner should be removed");
        graph.click(930, 730, GateType.AND);
        require(graph.click(WorkbenchGraph.OUTPUT_X, WorkbenchGraph.OUTPUT_Y, GateType.AND)
                == WorkbenchGraph.EditResult.WIRED,
            "manual route should finish at the output terminal");
        WorkbenchGraph.Wire routed = graph.wires().get(graph.wires().size() - 1);
        require(routed.corners().size() == 1,
            "completed wire should retain its Proteus-style corner");

        System.out.println("WorkbenchGraphTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

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

        WorkbenchGraph disconnected = new WorkbenchGraph(CircuitRecipe.all().get(0));
        require(disconnected.click(760, 690, GateType.OR) == WorkbenchGraph.EditResult.ADDED,
            "empty canvas click should add the active gate");
        WorkbenchGraph.Node reverseTarget = disconnected.nodes().get(2);
        int[] emptyInput = disconnected.targetPoint(reverseTarget.id(), 0);
        require(disconnected.click(emptyInput[0], emptyInput[1], GateType.OR)
                == WorkbenchGraph.EditResult.WIRE_STARTED,
            "an empty gate input should start a reverse wire");
        require(disconnected.hasPendingWire(),
            "reverse wiring should retain the input as the pending endpoint");
        require(disconnected.finishWireAt(WorkbenchGraph.INPUT_X, WorkbenchGraph.INPUT_0_Y)
                == WorkbenchGraph.EditResult.WIRED,
            "a disconnected first gate should reconnect by dragging its input to IN 0");
        require(disconnected.wires().stream().anyMatch(wire ->
                wire.sourceId() == WorkbenchGraph.INPUT_0
                    && wire.targetId() == reverseTarget.id()
                    && wire.targetPort() == 0),
            "reverse connection should be stored in source-to-input direction");

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

        int[] portBeforeMove = graph.sourcePoint(source.id());
        require(!graph.beginNodeDrag(portBeforeMove[0], portBeforeMove[1]),
            "dragging from a port must remain reserved for wiring");
        require(graph.beginNodeDrag(source.x() + LogicNodeRenderer.BODY_WIDTH / 2,
                source.centerY()),
            "pressing a node body should begin a drag");
        require(graph.isDraggingNode(), "graph should expose its active drag state");
        require(graph.dragNodeTo(1200, 220), "dragging should update the node position");
        require(source.x() == 1144 && source.centerY() == 224,
            "dragged node should preserve the grab offset and snap to the grid");
        int[] portAfterMove = graph.sourcePoint(source.id());
        require(portAfterMove[0] != portBeforeMove[0] || portAfterMove[1] != portBeforeMove[1],
            "attached wire endpoints should follow a moved node");
        require(graph.endNodeDrag(), "mouse release should end node dragging");
        require(!graph.isDraggingNode(), "drag state should clear on release");

        WorkbenchGraph nand = new WorkbenchGraph(CircuitRecipe.all().get(0));
        WorkbenchGraph.Snapshot savedNand = nand.snapshot();
        String encodedWorkspace = WorkbenchSnapshotCodec.encode(savedNand);
        require(WorkbenchSnapshotCodec.decode(encodedWorkspace).equals(savedNand),
            "workspace codec should round-trip the active graph exactly");
        require(WorkbenchSnapshotCodec.decode("##").nodes().isEmpty(),
            "workspace codec should preserve an intentionally empty canvas");

        WorkbenchGraph composite = new WorkbenchGraph(CircuitRecipe.all().get(0));
        composite.clear();
        require(composite.click(820, 365, "NAND", savedNand)
                == WorkbenchGraph.EditResult.ADDED,
            "a crafted circuit should be placed as one custom node");
        WorkbenchGraph.Node customNode = composite.nodes().get(0);
        require(customNode.isCustom() && customNode.gate() == null
                && customNode.label().equals("NAND"),
            "the imported circuit should remain a named composite node");
        require(customNode.inputPorts() == 2 && customNode.outputPorts() == 1,
            "a composite node should expose its stored circuit ports");
        for (int port = 0; port < customNode.inputPorts(); port++) {
            int inputY = port == 0 ? WorkbenchGraph.INPUT_0_Y : WorkbenchGraph.INPUT_1_Y;
            require(composite.click(WorkbenchGraph.INPUT_X, inputY, GateType.AND)
                    == WorkbenchGraph.EditResult.WIRE_STARTED,
                "an input terminal should start wiring into the custom node");
            int[] customInput = composite.targetPoint(customNode.id(), port);
            require(composite.click(customInput[0], customInput[1], GateType.AND)
                    == WorkbenchGraph.EditResult.WIRED,
                "each custom-node input should accept a wire");
        }
        int[] customOutput = composite.sourcePoint(customNode.id());
        require(composite.click(customOutput[0], customOutput[1], GateType.AND)
                == WorkbenchGraph.EditResult.WIRE_STARTED,
            "the custom-node output should start a wire");
        require(composite.click(WorkbenchGraph.OUTPUT_X, WorkbenchGraph.OUTPUT_Y,
                GateType.AND) == WorkbenchGraph.EditResult.WIRED,
            "the custom node should connect to the workspace output");
        require(composite.matches(CircuitRecipe.all().get(0)),
            "a NAND composite node should evaluate like its stored circuit");
        require(composite.gateCounts().getOrDefault(GateType.AND, 0) == 1
                && composite.gateCounts().getOrDefault(GateType.NOT, 0) == 1,
            "crafting with a custom node should count its primitive components");
        String encodedComposite = WorkbenchSnapshotCodec.encode(composite.snapshot());
        WorkbenchGraph.Snapshot decodedComposite =
            WorkbenchSnapshotCodec.decode(encodedComposite);
        require(decodedComposite.equals(composite.snapshot())
                && decodedComposite.nodes().get(0).customGraph().equals(savedNand),
            "saved workspaces should round-trip nested custom nodes exactly");

        nand.clear();
        require(nand.nodes().isEmpty() && nand.wires().isEmpty(),
            "a commissioned project should support a genuinely empty canvas");
        require(!nand.isCompleteCircuit(), "an empty canvas must not be craftable");
        nand.restore(savedNand);
        require(nand.isCompleteCircuit() && nand.matches(CircuitRecipe.all().get(0)),
            "a crafted snapshot should restore and evaluate the player's exact graph");
        CraftedCircuitInventory craftedInventory = new CraftedCircuitInventory();
        require(craftedInventory.add("ABC", nand.snapshot()),
            "a complete player graph should fit in crafted circuit storage");
        String[] encodedCircuits = craftedInventory.saveData();
        CraftedCircuitInventory restoredInventory = new CraftedCircuitInventory();
        restoredInventory.restore(encodedCircuits, 0);
        require(restoredInventory.selected() != null
                && restoredInventory.selected().name().equals("ABC"),
            "crafted circuit selection should survive serialization");
        WorkbenchGraph restoredCircuit = new WorkbenchGraph(CircuitRecipe.all().get(0));
        restoredCircuit.restore(restoredInventory.selected().graph());
        require(restoredCircuit.matches(CircuitRecipe.all().get(0)),
            "serialized crafted wiring should still pass the requested truth table");
        require(CircuitPackageRenderer.inputCount(restoredInventory.selected().graph()) == 2
                && CircuitPackageRenderer.outputCount(restoredInventory.selected().graph()) == 1,
            "crafted circuit thumbnails should calculate their pins from stored wiring");
        require(restoredCircuit.failedCases(CircuitRecipe.all().get(0)) == 0,
            "Mira's integrated order test should report zero failed cases for NAND");
        require(restoredInventory.add("ABC", nand.snapshot())
                && restoredInventory.names().size() == 1,
            "multiple circuits should be assignable to the same player-defined name");
        require(!restoredInventory.add("TOOLONG", nand.snapshot()),
            "crafted circuit names must be limited to six characters");
        require(nand.outputValue(0, new boolean[]{false, false}),
            "OUT 0 should evaluate NAND high for input switches 0=0, 1=0");
        require(nand.outputValue(0, new boolean[]{true, false}),
            "OUT 0 should evaluate NAND high for input switches 0=1, 1=0");
        require(!nand.outputValue(0, new boolean[]{true, true}),
            "OUT 0 should evaluate NAND low for input switches 0=1, 1=1");
        require(!nand.outputValue(1, new boolean[]{false, false}),
            "unmapped left indicators should remain off");
        require(!nand.sourceValue(WorkbenchGraph.INPUT_0, new boolean[]{false, true}),
            "IN 0 should read bottom switch 0");
        require(nand.sourceValue(WorkbenchGraph.INPUT_1, new boolean[]{false, true}),
            "IN 1 should read bottom switch 1");

        int outputSource = nand.wires().stream()
            .filter(wire -> wire.targetId() == WorkbenchGraph.OUTPUT)
            .findFirst().orElseThrow().sourceId();
        int[] outputSourcePort = nand.sourcePoint(outputSource);
        nand.click(outputSourcePort[0], outputSourcePort[1], GateType.AND);
        int[] firstNodeInput = nand.targetPoint(nand.nodes().get(0).id(), 0);
        nand.click(firstNodeInput[0], firstNodeInput[1], GateType.AND);
        boolean feedbackValue = nand.outputValue(0, new boolean[]{true, true});
        require(feedbackValue == nand.outputValue(0, new boolean[]{true, true}),
            "feedback cycles should resolve safely and deterministically");

        WorkbenchGraph branched = new WorkbenchGraph(CircuitRecipe.all().get(0));
        int wireCountBeforeJunction = branched.wires().size();
        require(branched.addJunctionAt(430, WorkbenchGraph.INPUT_0_Y)
                == WorkbenchGraph.EditResult.JUNCTION_ADDED,
            "Shift-clicking a wire should create a junction source");
        require(branched.junctions().size() == 1,
            "created routing point should be retained by the graph");
        WorkbenchGraph.Junction junction = branched.junctions().get(0);
        require(junction.y() == WorkbenchGraph.INPUT_0_Y,
            "junction should project precisely onto the tapped wire");
        require(Integer.valueOf(junction.id()).equals(branched.pendingSourceId()),
            "new junction should immediately become the active wire source");
        require(!branched.sourceValue(junction.id(), new boolean[]{false, true}),
            "junction should inherit its tapped wire signal when low");
        require(branched.sourceValue(junction.id(), new boolean[]{true, false}),
            "junction should inherit its tapped wire signal when high");
        int[] branchTarget = branched.targetPoint(branched.nodes().get(0).id(), 1);
        require(branched.click(branchTarget[0], branchTarget[1], GateType.AND)
                == WorkbenchGraph.EditResult.WIRED,
            "a branch wire should finish from the new junction");
        require(branched.wires().size() == wireCountBeforeJunction,
            "branching onto an occupied input should replace that input wire");
        int nodeCountBeforeJunctionClick = branched.nodes().size();
        require(branched.click(junction.x(), junction.y(), GateType.OR)
                == WorkbenchGraph.EditResult.WIRE_STARTED,
            "clicking an existing junction should start a new branch wire");
        require(Integer.valueOf(junction.id()).equals(branched.pendingSourceId()),
            "clicked junction should become the active wire source");
        require(branched.nodes().size() == nodeCountBeforeJunctionClick,
            "clicking a junction must not add the selected gate");
        require(branched.finishWireAt(branchTarget[0], branchTarget[1])
                == WorkbenchGraph.EditResult.WIRED,
            "dragging from a junction should connect when released on an input");
        require(branched.pendingSourceId() == null,
            "finishing a junction drag should clear the active wire source");

        require(branched.click(800, 700, GateType.NOT)
                == WorkbenchGraph.EditResult.ADDED,
            "an independent source node should be placeable for junction wiring");
        WorkbenchGraph.Node junctionDriver = branched.nodes()
            .get(branched.nodes().size() - 1);
        int[] driverOutput = branched.sourcePoint(junctionDriver.id());
        require(branched.click(driverOutput[0], driverOutput[1], GateType.NOT)
                == WorkbenchGraph.EditResult.WIRE_STARTED,
            "a node output should start a wire toward a junction");
        require(branched.finishWireAt(junction.x(), junction.y())
                == WorkbenchGraph.EditResult.WIRED,
            "a node output wire should finish on an existing junction");
        require(branched.wires().stream().anyMatch(wire ->
                wire.sourceId() == junctionDriver.id()
                    && wire.targetId() == junction.id()
                    && wire.targetPort() == 0),
            "the node-to-junction connection should be retained");
        require(branched.sourceValue(junction.id(), new boolean[]{false, false}),
            "a connected node output should drive the junction signal");

        require(branched.addJunctionAt(10, 10) == WorkbenchGraph.EditResult.NONE,
            "Shift-click away from a wire should not create a junction");

        System.out.println("WorkbenchGraphTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

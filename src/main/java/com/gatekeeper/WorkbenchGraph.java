package com.gatekeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;

/** Mutable node-and-wire state for the physical workbench editor. */
final class WorkbenchGraph {
    static final int INPUT_0 = -1;
    static final int INPUT_1 = -2;
    static final int OUTPUT = -3;

    static final int INPUT_X = 340;
    static final int INPUT_0_Y = 300;
    static final int INPUT_1_Y = 455;
    static final int OUTPUT_X = 1390;
    static final int OUTPUT_Y = 365;

    // "Node Canvas" from assets/items/canvas/canvas.annotations.json.
    static final int WORK_X = 192;
    static final int WORK_Y = 47;
    static final int WORK_WIDTH = 1293;
    static final int WORK_HEIGHT = 750;
    static final int WORK_RIGHT = WORK_X + WORK_WIDTH;
    static final int WORK_BOTTOM = WORK_Y + WORK_HEIGHT;

    private static final int PORT_HIT_RADIUS = 24;
    private static final int WIRE_HIT_RADIUS = 18;
    private static final int MAX_NODES = 32;
    private static final int FIRST_JUNCTION_ID = -1000;

    enum EditResult {
        NONE, ADDED, SELECTED, WIRE_STARTED, WIRE_CORNER, JUNCTION_ADDED,
        WIRED, INVALID_WIRE
    }

    static final class Node {
        private final int id;
        private int x;
        private int centerY;
        private final GateType gate;
        private final String customName;
        private final Snapshot customGraph;

        Node(int id, int x, int centerY, GateType gate) {
            this(id, x, centerY, gate, null, null);
        }

        Node(int id, int x, int centerY, String customName, Snapshot customGraph) {
            this(id, x, centerY, null, customName, customGraph);
        }

        private Node(int id, int x, int centerY, GateType gate,
                     String customName, Snapshot customGraph) {
            this.id = id;
            this.x = x;
            this.centerY = centerY;
            this.gate = gate;
            this.customName = customName;
            this.customGraph = customGraph;
        }

        int id() { return id; }
        int x() { return x; }
        int centerY() { return centerY; }
        GateType gate() { return gate; }
        boolean isCustom() { return customGraph != null; }
        String label() { return isCustom() ? customName : gate.label; }
        Snapshot customGraph() { return customGraph; }
        int inputPorts() {
            return isCustom() ? WorkbenchGraph.inputCount(customGraph) : gate.inputPorts;
        }
        int outputPorts() {
            return isCustom() ? WorkbenchGraph.outputCount(customGraph) : gate.outputPorts;
        }

        void moveTo(int nextX, int nextCenterY) {
            x = nextX;
            centerY = nextCenterY;
        }
    }

    static final class Wire {
        private final int sourceId;
        private final int targetId;
        private final int targetPort;
        private final List<RoutePoint> corners;

        Wire(int sourceId, int targetId, int targetPort) {
            this(sourceId, targetId, targetPort, List.of());
        }

        Wire(int sourceId, int targetId, int targetPort, List<RoutePoint> corners) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.targetPort = targetPort;
            this.corners = List.copyOf(corners);
        }

        int sourceId() { return sourceId; }
        int targetId() { return targetId; }
        int targetPort() { return targetPort; }
        List<RoutePoint> corners() { return corners; }
    }

    record RoutePoint(int x, int y) {}
    record NodeData(int id, int x, int centerY, GateType gate,
                    String customName, Snapshot customGraph) {
        NodeData(int id, int x, int centerY, GateType gate) {
            this(id, x, centerY, gate, null, null);
        }

        static NodeData custom(int id, int x, int centerY,
                               String name, Snapshot graph) {
            return new NodeData(id, x, centerY, null, name, graph);
        }

        NodeData {
            if ((gate == null) == (customGraph == null)) {
                throw new IllegalArgumentException(
                    "A workbench node must be primitive or custom");
            }
            if (customGraph != null && (customName == null || customName.isBlank())) {
                throw new IllegalArgumentException("A custom node needs a name");
            }
        }
    }
    record WireData(int sourceId, int targetId, int targetPort, List<RoutePoint> corners) {
        WireData { corners = List.copyOf(corners); }
    }
    record JunctionData(int id, int upstreamSourceId, int x, int y) {}
    record Snapshot(List<NodeData> nodes, List<WireData> wires,
                    List<JunctionData> junctions) {
        Snapshot {
            nodes = List.copyOf(nodes);
            wires = List.copyOf(wires);
            junctions = List.copyOf(junctions);
        }
    }

    static final class Junction {
        private final int id;
        private final int upstreamSourceId;
        private final int x;
        private final int y;

        Junction(int id, int upstreamSourceId, int x, int y) {
            this.id = id;
            this.upstreamSourceId = upstreamSourceId;
            this.x = x;
            this.y = y;
        }

        int id() { return id; }
        int upstreamSourceId() { return upstreamSourceId; }
        int x() { return x; }
        int y() { return y; }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Wire> wires = new ArrayList<>();
    private final List<Junction> junctions = new ArrayList<>();
    private int nextNodeId;
    private int nextJunctionId = FIRST_JUNCTION_ID;
    private Integer selectedNodeId;
    private Integer pendingSourceId;
    private Target pendingTarget;
    private final List<RoutePoint> pendingCorners = new ArrayList<>();
    private Integer draggingNodeId;
    private int dragOffsetX;
    private int dragOffsetY;

    WorkbenchGraph(CircuitRecipe recipe) {
        loadRecipe(recipe);
    }

    private WorkbenchGraph(Snapshot snapshot) {
        restore(snapshot);
    }

    void clear() {
        nodes.clear();
        wires.clear();
        junctions.clear();
        nextNodeId = 0;
        nextJunctionId = FIRST_JUNCTION_ID;
        selectedNodeId = null;
        pendingSourceId = null;
        pendingTarget = null;
        pendingCorners.clear();
        draggingNodeId = null;
    }

    void loadRecipe(CircuitRecipe recipe) {
        clear();

        int[][] layout = initialLayout(recipe);
        for (int index = 0; index < recipe.slotCount(); index++) {
            addNode(layout[index][0], layout[index][1], recipe.solution[index]);
        }
        for (int index = 0; index < recipe.slotCount(); index++) {
            addRecipeWire(recipe.leftSources[index], index, 0);
            if (recipe.solution[index].inputPorts > 1) {
                addRecipeWire(recipe.rightSources[index], index, 1);
            }
        }
        if (!nodes.isEmpty()) wires.add(new Wire(nodes.get(nodes.size() - 1).id(), OUTPUT, 0));
        selectedNodeId = null;
    }

    Snapshot snapshot() {
        List<NodeData> nodeData = nodes.stream()
            .map(node -> node.isCustom()
                ? NodeData.custom(node.id, node.x, node.centerY,
                    node.customName, node.customGraph)
                : new NodeData(node.id, node.x, node.centerY, node.gate)).toList();
        List<WireData> wireData = wires.stream()
            .map(wire -> new WireData(wire.sourceId, wire.targetId,
                wire.targetPort, wire.corners)).toList();
        List<JunctionData> junctionData = junctions.stream()
            .map(junction -> new JunctionData(junction.id, junction.upstreamSourceId,
                junction.x, junction.y)).toList();
        return new Snapshot(nodeData, wireData, junctionData);
    }

    void restore(Snapshot snapshot) {
        clear();
        if (snapshot == null) return;
        for (NodeData data : snapshot.nodes) {
            nodes.add(data.customGraph == null
                ? new Node(data.id, data.x, data.centerY, data.gate)
                : new Node(data.id, data.x, data.centerY,
                    data.customName, data.customGraph));
            nextNodeId = Math.max(nextNodeId, data.id + 1);
        }
        for (WireData data : snapshot.wires) {
            wires.add(new Wire(data.sourceId, data.targetId, data.targetPort, data.corners));
        }
        for (JunctionData data : snapshot.junctions) {
            junctions.add(new Junction(data.id, data.upstreamSourceId, data.x, data.y));
            nextJunctionId = Math.min(nextJunctionId, data.id - 1);
        }
    }

    boolean isCompleteCircuit() {
        if (nodes.isEmpty() || wireTo(OUTPUT, 0) == null) return false;
        for (Node node : nodes) {
            for (int port = 0; port < node.inputPorts(); port++) {
                if (wireTo(node.id, port) == null) return false;
            }
        }
        return true;
    }

    boolean matches(CircuitRecipe recipe) {
        if (!isCompleteCircuit()) return false;
        for (int row = 0; row < recipe.truth.length; row++) {
            boolean actual = outputValue(0,
                new boolean[]{row >= 2, row % 2 == 1});
            if (actual != recipe.truth[row]) return false;
        }
        return true;
    }

    int failedCases(CircuitRecipe recipe) {
        if (!isCompleteCircuit()) return recipe.truth.length;
        int failures = 0;
        for (int row = 0; row < recipe.truth.length; row++) {
            if (outputValue(0, new boolean[]{row >= 2, row % 2 == 1})
                != recipe.truth[row]) failures++;
        }
        return failures;
    }

    int inputCount() {
        return inputCount(snapshot());
    }

    int outputCount() {
        return outputCount(snapshot());
    }

    static int inputCount(Snapshot graph) {
        return inputSources(graph).size();
    }

    static int outputCount(Snapshot graph) {
        if (graph == null) return 0;
        return (int) graph.wires().stream()
            .filter(wire -> wire.targetId() == OUTPUT)
            .map(WireData::targetPort).distinct().count();
    }

    Map<GateType, Integer> gateCounts() {
        Map<GateType, Integer> result = new EnumMap<>(GateType.class);
        addGateCounts(snapshot(), result);
        return result;
    }

    List<Node> nodes() { return Collections.unmodifiableList(nodes); }
    List<Wire> wires() { return Collections.unmodifiableList(wires); }
    List<Junction> junctions() { return Collections.unmodifiableList(junctions); }
    Integer selectedNodeId() { return selectedNodeId; }
    Integer pendingSourceId() { return pendingSourceId; }
    boolean hasPendingWire() { return pendingSourceId != null || pendingTarget != null; }
    int[] pendingStartPoint() {
        if (pendingSourceId != null) return sourcePoint(pendingSourceId);
        return pendingTarget == null ? null
            : targetPoint(pendingTarget.nodeId, pendingTarget.port);
    }
    boolean isDraggingNode() { return draggingNodeId != null; }
    List<RoutePoint> pendingCorners() {
        return Collections.unmodifiableList(pendingCorners);
    }

    Node node(int id) {
        for (Node node : nodes) if (node.id == id) return node;
        return null;
    }

    EditResult addJunctionAt(int x, int y) {
        WireHit hit = wireAt(x, y);
        if (hit == null) return EditResult.NONE;
        for (Junction junction : junctions) {
            if (near(hit.point.x, hit.point.y, junction.x, junction.y)) {
                pendingSourceId = junction.id;
                pendingTarget = null;
                pendingCorners.clear();
                selectedNodeId = null;
                return EditResult.WIRE_STARTED;
            }
        }
        Junction junction = new Junction(nextJunctionId--,
            hit.wire.sourceId, hit.point.x, hit.point.y);
        junctions.add(junction);
        pendingSourceId = junction.id;
        pendingTarget = null;
        pendingCorners.clear();
        selectedNodeId = null;
        return EditResult.JUNCTION_ADDED;
    }

    boolean beginNodeDrag(int x, int y) {
        if (hasPendingWire() || sourceAt(x, y) != null || targetAt(x, y) != null) {
            return false;
        }
        Node hitNode = nodeAt(x, y);
        if (hitNode == null) return false;
        selectedNodeId = hitNode.id;
        draggingNodeId = hitNode.id;
        dragOffsetX = x - hitNode.x;
        dragOffsetY = y - hitNode.centerY;
        return true;
    }

    boolean dragNodeTo(int x, int y) {
        if (draggingNodeId == null) return false;
        Node dragged = node(draggingNodeId);
        if (dragged == null) {
            draggingNodeId = null;
            return false;
        }
        int height = LogicNodeRenderer.bodyHeight(
            dragged.inputPorts(), dragged.outputPorts());
        int nextX = clamp(snap(x - dragOffsetX),
            WORK_X, WORK_RIGHT - LogicNodeRenderer.BODY_WIDTH);
        int nextCenterY = clamp(snap(y - dragOffsetY),
            WORK_Y + height / 2, WORK_BOTTOM - height / 2);
        if (nextX == dragged.x && nextCenterY == dragged.centerY) return false;
        dragged.moveTo(nextX, nextCenterY);
        return true;
    }

    boolean endNodeDrag() {
        if (draggingNodeId == null) return false;
        draggingNodeId = null;
        return true;
    }

    EditResult click(int x, int y, GateType gateToAdd) {
        return click(x, y, gateToAdd, null, null);
    }

    EditResult click(int x, int y, String customName, Snapshot customGraph) {
        if (customName == null || customName.isBlank() || customGraph == null) {
            return EditResult.NONE;
        }
        return click(x, y, null, customName, customGraph);
    }

    private EditResult click(int x, int y, GateType gateToAdd,
                             String customName, Snapshot customGraph) {
        if (pendingSourceId != null) {
            Target target = targetAt(x, y);
            if (target != null) return connectTo(target);
        }

        Integer source = sourceAt(x, y);
        if (source != null) {
            if (pendingTarget != null) return connectFrom(source);
            pendingSourceId = source;
            pendingTarget = null;
            pendingCorners.clear();
            selectedNodeId = source >= 0 ? source : null;
            return EditResult.WIRE_STARTED;
        }

        Target target = targetAt(x, y);
        if (target != null) {
            if (pendingSourceId != null) return connectTo(target);
            if (target.nodeId == OUTPUT) return EditResult.NONE;
            pendingTarget = target;
            pendingCorners.clear();
            selectedNodeId = target.nodeId >= 0 ? target.nodeId : null;
            return EditResult.WIRE_STARTED;
        }

        Node hitNode = nodeAt(x, y);
        if (hitNode != null) {
            selectedNodeId = hitNode.id;
            pendingSourceId = null;
            pendingTarget = null;
            pendingCorners.clear();
            return EditResult.SELECTED;
        }

        if (hasPendingWire() && insideWorkArea(x, y)) {
            RoutePoint previous = pendingCorners.isEmpty()
                ? pendingStartRoutePoint()
                : pendingCorners.get(pendingCorners.size() - 1);
            RoutePoint corner = snappedPoint(x, y, previous);
            if (!corner.equals(previous)) pendingCorners.add(corner);
            return EditResult.WIRE_CORNER;
        }

        if (insideWorkArea(x, y) && nodes.size() < MAX_NODES) {
            int inputPorts = customGraph == null
                ? gateToAdd.inputPorts : inputCount(customGraph);
            int outputPorts = customGraph == null
                ? gateToAdd.outputPorts : outputCount(customGraph);
            int bodyHeight = LogicNodeRenderer.bodyHeight(
                inputPorts, outputPorts);
            int bodyX = clamp(x - LogicNodeRenderer.BODY_WIDTH / 2,
                WORK_X, WORK_RIGHT - LogicNodeRenderer.BODY_WIDTH);
            int centerY = clamp(y, WORK_Y + bodyHeight / 2,
                WORK_BOTTOM - bodyHeight / 2);
            Node added = customGraph == null
                ? addNode(bodyX, centerY, gateToAdd)
                : addNode(bodyX, centerY, customName, customGraph);
            selectedNodeId = added.id;
            pendingSourceId = null;
            pendingTarget = null;
            pendingCorners.clear();
            return EditResult.ADDED;
        }

        selectedNodeId = null;
        pendingSourceId = null;
        pendingTarget = null;
        pendingCorners.clear();
        return EditResult.NONE;
    }

    EditResult finishWireAt(int x, int y) {
        if (pendingSourceId != null) {
            Target target = targetAt(x, y);
            return target == null ? EditResult.NONE : connectTo(target);
        }
        if (pendingTarget != null) {
            Integer source = sourceAt(x, y);
            return source == null ? EditResult.NONE : connectFrom(source);
        }
        return EditResult.NONE;
    }

    boolean deleteSelected() {
        if (selectedNodeId == null) return false;
        int removedId = selectedNodeId;
        boolean removed = nodes.removeIf(node -> node.id == removedId);
        if (!removed) return false;
        wires.removeIf(wire -> wire.sourceId == removedId || wire.targetId == removedId);
        removeOrphanedJunctions(removedId);
        if (pendingSourceId != null && pendingSourceId == removedId) pendingSourceId = null;
        if (pendingTarget != null && pendingTarget.nodeId == removedId) pendingTarget = null;
        selectedNodeId = null;
        if (draggingNodeId != null && draggingNodeId == removedId) draggingNodeId = null;
        return true;
    }

    boolean cancelWire() {
        if (!hasPendingWire()) return false;
        pendingSourceId = null;
        pendingTarget = null;
        pendingCorners.clear();
        return true;
    }

    boolean undoWireCorner() {
        if (!hasPendingWire() || pendingCorners.isEmpty()) return false;
        pendingCorners.remove(pendingCorners.size() - 1);
        return true;
    }

    boolean outputValue(int outputIndex, boolean[] externalInputs) {
        if (outputIndex != 0) return false;
        Wire outputWire = wireTo(OUTPUT, outputIndex);
        if (outputWire == null) return false;
        return sourceValue(outputWire.sourceId, externalInputs,
            new HashMap<>(), new HashSet<>());
    }

    boolean sourceValue(int sourceId, boolean[] externalInputs) {
        return sourceValue(sourceId, externalInputs,
            new HashMap<>(), new HashSet<>());
    }

    int[] sourcePoint(int sourceId) {
        if (sourceId == INPUT_0) return new int[]{INPUT_X, INPUT_0_Y};
        if (sourceId == INPUT_1) return new int[]{INPUT_X, INPUT_1_Y};
        Junction junction = junction(sourceId);
        if (junction != null) return new int[]{junction.x, junction.y};
        Node source = node(sourceId);
        if (source == null || source.outputPorts() == 0) return null;
        return new int[]{LogicNodeRenderer.outputPortX(source.x),
            LogicNodeRenderer.outputPortY(source.centerY,
                source.inputPorts(), source.outputPorts(), 0)};
    }

    int[] targetPoint(int targetId, int targetPort) {
        if (targetId == OUTPUT) return new int[]{OUTPUT_X, OUTPUT_Y};
        Junction junction = junction(targetId);
        if (junction != null && targetPort == 0) {
            return new int[]{junction.x, junction.y};
        }
        Node target = node(targetId);
        if (target == null || targetPort < 0 || targetPort >= target.inputPorts()) return null;
        return new int[]{LogicNodeRenderer.inputPortX(target.x),
            LogicNodeRenderer.inputPortY(target.centerY,
                target.inputPorts(), target.outputPorts(), targetPort)};
    }

    private void addRecipeWire(int recipeSource, int targetIndex, int targetPort) {
        int sourceId = recipeSource == CircuitRecipe.INPUT_A ? INPUT_0
            : recipeSource == CircuitRecipe.INPUT_B ? INPUT_1
            : nodes.get(recipeSource).id;
        wires.add(new Wire(sourceId, nodes.get(targetIndex).id, targetPort));
    }

    private boolean sourceValue(int sourceId, boolean[] externalInputs,
                                Map<Integer, Boolean> memo,
                                Set<Integer> evaluating) {
        if (sourceId == INPUT_0) return inputValue(externalInputs, 0);
        if (sourceId == INPUT_1) return inputValue(externalInputs, 1);
        Junction junction = junction(sourceId);
        if (junction != null) {
            Boolean cached = memo.get(sourceId);
            if (cached != null) return cached;
            if (!evaluating.add(sourceId)) return false;
            Wire incoming = wireTo(junction.id, 0);
            int driverId = incoming == null
                ? junction.upstreamSourceId : incoming.sourceId;
            boolean value = sourceValue(
                driverId, externalInputs, memo, evaluating);
            evaluating.remove(sourceId);
            memo.put(sourceId, value);
            return value;
        }
        Boolean cached = memo.get(sourceId);
        if (cached != null) return cached;
        Node source = node(sourceId);
        if (source == null || !evaluating.add(sourceId)) return false;

        boolean value;
        if (source.isCustom()) {
            boolean[] customInputs = new boolean[2];
            List<Integer> customInputSources = inputSources(source.customGraph);
            for (int port = 0; port < customInputSources.size(); port++) {
                int inputIndex = customInputSources.get(port) == INPUT_1 ? 1 : 0;
                customInputs[inputIndex] = inputPortValue(
                    source, port, externalInputs, memo, evaluating);
            }
            value = new WorkbenchGraph(source.customGraph).outputValue(0, customInputs);
        } else {
            boolean first = inputPortValue(source, 0,
                externalInputs, memo, evaluating);
            value = switch (source.gate) {
                case AND -> first
                    && inputPortValue(source, 1, externalInputs, memo, evaluating);
                case OR -> first
                    || inputPortValue(source, 1, externalInputs, memo, evaluating);
                case NOT -> !first;
            };
        }
        evaluating.remove(sourceId);
        memo.put(sourceId, value);
        return value;
    }

    private boolean inputPortValue(Node target, int port,
                                   boolean[] externalInputs,
                                   Map<Integer, Boolean> memo,
                                   Set<Integer> evaluating) {
        Wire incoming = wireTo(target.id, port);
        return incoming != null && sourceValue(
            incoming.sourceId, externalInputs, memo, evaluating);
    }

    private Wire wireTo(int targetId, int targetPort) {
        for (Wire wire : wires) {
            if (wire.targetId == targetId && wire.targetPort == targetPort) return wire;
        }
        return null;
    }

    private static boolean inputValue(boolean[] externalInputs, int index) {
        return externalInputs != null && index >= 0
            && index < externalInputs.length && externalInputs[index];
    }

    private Node addNode(int x, int centerY, GateType gate) {
        Node node = new Node(nextNodeId++, x, centerY, gate);
        nodes.add(node);
        return node;
    }

    private Node addNode(int x, int centerY, String name, Snapshot graph) {
        Node node = new Node(nextNodeId++, x, centerY, name, graph);
        nodes.add(node);
        return node;
    }

    private static List<Integer> inputSources(Snapshot graph) {
        if (graph == null) return List.of();
        boolean input0 = false;
        boolean input1 = false;
        for (WireData wire : graph.wires()) {
            if (wire.sourceId() == INPUT_0) input0 = true;
            if (wire.sourceId() == INPUT_1) input1 = true;
        }
        for (JunctionData junction : graph.junctions()) {
            if (junction.upstreamSourceId() == INPUT_0) input0 = true;
            if (junction.upstreamSourceId() == INPUT_1) input1 = true;
        }
        List<Integer> result = new ArrayList<>(2);
        if (input0) result.add(INPUT_0);
        if (input1) result.add(INPUT_1);
        return result;
    }

    private static void addGateCounts(Snapshot graph,
                                      Map<GateType, Integer> counts) {
        if (graph == null) return;
        for (NodeData node : graph.nodes()) {
            if (node.customGraph() == null) counts.merge(node.gate(), 1, Integer::sum);
            else addGateCounts(node.customGraph(), counts);
        }
    }

    private Junction junction(int id) {
        for (Junction junction : junctions) if (junction.id == id) return junction;
        return null;
    }

    private EditResult connectTo(Target target) {
        if (pendingSourceId == target.nodeId) return EditResult.INVALID_WIRE;
        wires.removeIf(wire -> wire.targetId == target.nodeId
            && wire.targetPort == target.port);
        wires.add(new Wire(pendingSourceId, target.nodeId, target.port,
            normalizedCorners(target)));
        pendingSourceId = null;
        pendingTarget = null;
        pendingCorners.clear();
        selectedNodeId = target.nodeId >= 0 ? target.nodeId : selectedNodeId;
        return EditResult.WIRED;
    }

    private EditResult connectFrom(int sourceId) {
        Target target = pendingTarget;
        if (target == null) return EditResult.NONE;
        if (sourceId == target.nodeId) return EditResult.INVALID_WIRE;
        wires.removeIf(wire -> wire.targetId == target.nodeId
            && wire.targetPort == target.port);
        List<RoutePoint> corners = new ArrayList<>(pendingCorners);
        Collections.reverse(corners);
        wires.add(new Wire(sourceId, target.nodeId, target.port, corners));
        pendingSourceId = null;
        pendingTarget = null;
        pendingCorners.clear();
        selectedNodeId = target.nodeId >= 0 ? target.nodeId : selectedNodeId;
        return EditResult.WIRED;
    }

    private void removeOrphanedJunctions(int removedSourceId) {
        Set<Integer> removed = new HashSet<>();
        removed.add(removedSourceId);
        boolean changed;
        do {
            changed = false;
            for (Junction junction : List.copyOf(junctions)) {
                if (removed.contains(junction.upstreamSourceId)) {
                    removed.add(junction.id);
                    junctions.remove(junction);
                    changed = true;
                }
            }
        } while (changed);
        wires.removeIf(wire -> removed.contains(wire.sourceId));
        if (pendingSourceId != null && removed.contains(pendingSourceId)) {
            pendingSourceId = null;
            pendingCorners.clear();
        }
    }

    private WireHit wireAt(int x, int y) {
        WireHit closest = null;
        double closestDistance = WIRE_HIT_RADIUS + 1.0;
        for (Wire wire : wires) {
            List<RoutePoint> points = wireRoute(wire);
            for (int index = 1; index < points.size(); index++) {
                RoutePoint projected = projectOrthogonal(
                    x, y, points.get(index - 1), points.get(index));
                double distance = Math.hypot(x - projected.x, y - projected.y);
                if (distance <= WIRE_HIT_RADIUS && distance < closestDistance) {
                    closest = new WireHit(wire, projected);
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private List<RoutePoint> wireRoute(Wire wire) {
        int[] start = sourcePoint(wire.sourceId);
        int[] end = targetPoint(wire.targetId, wire.targetPort);
        if (start == null || end == null) return List.of();
        List<RoutePoint> points = new ArrayList<>();
        points.add(new RoutePoint(start[0], start[1]));
        if (wire.corners.isEmpty()) {
            int bendX = start[0] + Math.max(36, (end[0] - start[0]) / 2);
            points.add(new RoutePoint(bendX, start[1]));
            points.add(new RoutePoint(bendX, end[1]));
        } else {
            RoutePoint previous = points.get(0);
            for (RoutePoint corner : wire.corners) {
                if (previous.x != corner.x && previous.y != corner.y) {
                    points.add(new RoutePoint(corner.x, previous.y));
                }
                points.add(corner);
                previous = corner;
            }
            if (previous.x != end[0] && previous.y != end[1]) {
                points.add(new RoutePoint(end[0], previous.y));
            }
        }
        points.add(new RoutePoint(end[0], end[1]));
        return points;
    }

    private static RoutePoint projectOrthogonal(int x, int y,
                                                RoutePoint start,
                                                RoutePoint end) {
        if (start.x == end.x) {
            return new RoutePoint(start.x, clamp(y,
                Math.min(start.y, end.y), Math.max(start.y, end.y)));
        }
        return new RoutePoint(clamp(x,
            Math.min(start.x, end.x), Math.max(start.x, end.x)), start.y);
    }

    private List<RoutePoint> normalizedCorners(Target target) {
        if (pendingCorners.isEmpty()) return List.of();
        int[] end = targetPoint(target.nodeId, target.port);
        List<RoutePoint> result = new ArrayList<>();
        RoutePoint previous = sourceRoutePoint(pendingSourceId);
        for (RoutePoint corner : pendingCorners) {
            if (!corner.equals(previous)) result.add(corner);
            previous = corner;
        }
        RoutePoint destination = new RoutePoint(end[0], end[1]);
        while (!result.isEmpty() && result.get(result.size() - 1).equals(destination)) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private RoutePoint sourceRoutePoint(int sourceId) {
        int[] source = sourcePoint(sourceId);
        return new RoutePoint(source[0], source[1]);
    }

    private RoutePoint pendingStartRoutePoint() {
        int[] start = pendingStartPoint();
        return new RoutePoint(start[0], start[1]);
    }

    private static RoutePoint snappedPoint(int x, int y, RoutePoint previous) {
        int grid = 8;
        int snappedX = (int) Math.round(x / (double) grid) * grid;
        int snappedY = (int) Math.round(y / (double) grid) * grid;
        int dx = Math.abs(snappedX - previous.x);
        int dy = Math.abs(snappedY - previous.y);
        return dx >= dy
            ? new RoutePoint(snappedX, previous.y)
            : new RoutePoint(previous.x, snappedY);
    }

    private Integer sourceAt(int x, int y) {
        for (int index = junctions.size() - 1; index >= 0; index--) {
            Junction junction = junctions.get(index);
            if (near(x, y, junction.x, junction.y)) return junction.id;
        }
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            int portX = LogicNodeRenderer.outputPortX(node.x);
            if (node.outputPorts() > 0) {
                int portY = LogicNodeRenderer.outputPortY(node.centerY,
                    node.inputPorts(), node.outputPorts(), 0);
                if (near(x, y, portX, portY)) return node.id;
            }
        }
        if (near(x, y, INPUT_X, INPUT_0_Y)) return INPUT_0;
        if (near(x, y, INPUT_X, INPUT_1_Y)) return INPUT_1;
        return null;
    }

    private Target targetAt(int x, int y) {
        for (int index = junctions.size() - 1; index >= 0; index--) {
            Junction junction = junctions.get(index);
            if (near(x, y, junction.x, junction.y)) {
                return new Target(junction.id, 0);
            }
        }
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            for (int port = 0; port < node.inputPorts(); port++) {
                int portX = LogicNodeRenderer.inputPortX(node.x);
                int portY = LogicNodeRenderer.inputPortY(node.centerY,
                    node.inputPorts(), node.outputPorts(), port);
                if (near(x, y, portX, portY)) return new Target(node.id, port);
            }
        }
        return near(x, y, OUTPUT_X, OUTPUT_Y) ? new Target(OUTPUT, 0) : null;
    }

    private Node nodeAt(int x, int y) {
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            int height = LogicNodeRenderer.bodyHeight(
                node.inputPorts(), node.outputPorts());
            if (x >= node.x && x <= node.x + LogicNodeRenderer.BODY_WIDTH
                && y >= node.centerY - height / 2 && y <= node.centerY + height / 2) {
                return node;
            }
        }
        return null;
    }

    private static boolean near(int x, int y, int pointX, int pointY) {
        int dx = x - pointX;
        int dy = y - pointY;
        return dx * dx + dy * dy <= PORT_HIT_RADIUS * PORT_HIT_RADIUS;
    }

    private static boolean insideWorkArea(int x, int y) {
        return x >= WORK_X && x <= WORK_RIGHT && y >= WORK_Y && y <= WORK_BOTTOM;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int snap(int value) {
        int grid = 8;
        return (int) Math.round(value / (double) grid) * grid;
    }

    private static int[][] initialLayout(CircuitRecipe recipe) {
        return switch (recipe.name) {
            case "XOR" -> new int[][]{
                {500, 500}, {790, 500}, {500, 230}, {790, 230}, {1130, 365}
            };
            case "XNOR" -> new int[][]{
                {430, 230}, {430, 500}, {700, 500}, {950, 365}, {1190, 365}
            };
            case "IMPLY" -> new int[][]{{570, 365}, {1010, 365}};
            default -> new int[][]{{570, 365}, {1010, 365}};
        };
    }

    private record Target(int nodeId, int port) {}
    private record WireHit(Wire wire, RoutePoint point) {}
}

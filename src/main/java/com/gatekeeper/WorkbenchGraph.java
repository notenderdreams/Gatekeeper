package com.gatekeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable node-and-wire state for the physical workbench editor. */
final class WorkbenchGraph {
    static final int INPUT_A = -1;
    static final int INPUT_B = -2;
    static final int OUTPUT = -3;

    static final int INPUT_X = 340;
    static final int INPUT_A_Y = 300;
    static final int INPUT_B_Y = 455;
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
    private static final int MAX_NODES = 32;

    enum EditResult {
        NONE, ADDED, SELECTED, WIRE_STARTED, WIRE_CORNER, WIRED, INVALID_WIRE
    }

    static final class Node {
        private final int id;
        private int x;
        private int centerY;
        private final GateType gate;

        Node(int id, int x, int centerY, GateType gate) {
            this.id = id;
            this.x = x;
            this.centerY = centerY;
            this.gate = gate;
        }

        int id() { return id; }
        int x() { return x; }
        int centerY() { return centerY; }
        GateType gate() { return gate; }

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

    private final List<Node> nodes = new ArrayList<>();
    private final List<Wire> wires = new ArrayList<>();
    private int nextNodeId;
    private Integer selectedNodeId;
    private Integer pendingSourceId;
    private final List<RoutePoint> pendingCorners = new ArrayList<>();
    private Integer draggingNodeId;
    private int dragOffsetX;
    private int dragOffsetY;

    WorkbenchGraph(CircuitRecipe recipe) {
        loadRecipe(recipe);
    }

    void loadRecipe(CircuitRecipe recipe) {
        nodes.clear();
        wires.clear();
        nextNodeId = 0;
        selectedNodeId = null;
        pendingSourceId = null;
        pendingCorners.clear();
        draggingNodeId = null;

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

    List<Node> nodes() { return Collections.unmodifiableList(nodes); }
    List<Wire> wires() { return Collections.unmodifiableList(wires); }
    Integer selectedNodeId() { return selectedNodeId; }
    Integer pendingSourceId() { return pendingSourceId; }
    boolean isDraggingNode() { return draggingNodeId != null; }
    List<RoutePoint> pendingCorners() {
        return Collections.unmodifiableList(pendingCorners);
    }

    Node node(int id) {
        for (Node node : nodes) if (node.id == id) return node;
        return null;
    }

    boolean beginNodeDrag(int x, int y) {
        if (pendingSourceId != null || sourceAt(x, y) != null || targetAt(x, y) != null) {
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
            dragged.gate.inputPorts, dragged.gate.outputPorts);
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
        Integer source = sourceAt(x, y);
        if (source != null) {
            pendingSourceId = source;
            pendingCorners.clear();
            selectedNodeId = source >= 0 ? source : null;
            return EditResult.WIRE_STARTED;
        }

        Target target = targetAt(x, y);
        if (target != null) {
            if (pendingSourceId != null) {
                if (pendingSourceId == target.nodeId) return EditResult.INVALID_WIRE;
                wires.removeIf(wire -> wire.targetId == target.nodeId
                    && wire.targetPort == target.port);
                wires.add(new Wire(pendingSourceId, target.nodeId, target.port,
                    normalizedCorners(target)));
                pendingSourceId = null;
                pendingCorners.clear();
                selectedNodeId = target.nodeId >= 0 ? target.nodeId : selectedNodeId;
                return EditResult.WIRED;
            }
            selectedNodeId = target.nodeId >= 0 ? target.nodeId : null;
            return target.nodeId >= 0 ? EditResult.SELECTED : EditResult.NONE;
        }

        Node hitNode = nodeAt(x, y);
        if (hitNode != null) {
            selectedNodeId = hitNode.id;
            pendingSourceId = null;
            pendingCorners.clear();
            return EditResult.SELECTED;
        }

        if (pendingSourceId != null && insideWorkArea(x, y)) {
            RoutePoint previous = pendingCorners.isEmpty()
                ? sourceRoutePoint(pendingSourceId)
                : pendingCorners.get(pendingCorners.size() - 1);
            RoutePoint corner = snappedPoint(x, y, previous);
            if (!corner.equals(previous)) pendingCorners.add(corner);
            return EditResult.WIRE_CORNER;
        }

        if (insideWorkArea(x, y) && nodes.size() < MAX_NODES) {
            int bodyHeight = LogicNodeRenderer.bodyHeight(
                gateToAdd.inputPorts, gateToAdd.outputPorts);
            int bodyX = clamp(x - LogicNodeRenderer.BODY_WIDTH / 2,
                WORK_X, WORK_RIGHT - LogicNodeRenderer.BODY_WIDTH);
            int centerY = clamp(y, WORK_Y + bodyHeight / 2,
                WORK_BOTTOM - bodyHeight / 2);
            Node added = addNode(bodyX, centerY, gateToAdd);
            selectedNodeId = added.id;
            pendingSourceId = null;
            pendingCorners.clear();
            return EditResult.ADDED;
        }

        selectedNodeId = null;
        pendingSourceId = null;
        pendingCorners.clear();
        return EditResult.NONE;
    }

    boolean deleteSelected() {
        if (selectedNodeId == null) return false;
        int removedId = selectedNodeId;
        boolean removed = nodes.removeIf(node -> node.id == removedId);
        if (!removed) return false;
        wires.removeIf(wire -> wire.sourceId == removedId || wire.targetId == removedId);
        if (pendingSourceId != null && pendingSourceId == removedId) pendingSourceId = null;
        selectedNodeId = null;
        if (draggingNodeId != null && draggingNodeId == removedId) draggingNodeId = null;
        return true;
    }

    boolean cancelWire() {
        if (pendingSourceId == null) return false;
        pendingSourceId = null;
        pendingCorners.clear();
        return true;
    }

    boolean undoWireCorner() {
        if (pendingSourceId == null || pendingCorners.isEmpty()) return false;
        pendingCorners.remove(pendingCorners.size() - 1);
        return true;
    }

    int[] sourcePoint(int sourceId) {
        if (sourceId == INPUT_A) return new int[]{INPUT_X, INPUT_A_Y};
        if (sourceId == INPUT_B) return new int[]{INPUT_X, INPUT_B_Y};
        Node source = node(sourceId);
        if (source == null) return null;
        return new int[]{LogicNodeRenderer.outputPortX(source.x),
            LogicNodeRenderer.outputPortY(source.centerY, source.gate, 0)};
    }

    int[] targetPoint(int targetId, int targetPort) {
        if (targetId == OUTPUT) return new int[]{OUTPUT_X, OUTPUT_Y};
        Node target = node(targetId);
        if (target == null || targetPort < 0 || targetPort >= target.gate.inputPorts) return null;
        return new int[]{LogicNodeRenderer.inputPortX(target.x),
            LogicNodeRenderer.inputPortY(target.centerY, target.gate, targetPort)};
    }

    private void addRecipeWire(int recipeSource, int targetIndex, int targetPort) {
        int sourceId = recipeSource == CircuitRecipe.INPUT_A ? INPUT_A
            : recipeSource == CircuitRecipe.INPUT_B ? INPUT_B
            : nodes.get(recipeSource).id;
        wires.add(new Wire(sourceId, nodes.get(targetIndex).id, targetPort));
    }

    private Node addNode(int x, int centerY, GateType gate) {
        Node node = new Node(nextNodeId++, x, centerY, gate);
        nodes.add(node);
        return node;
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
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            int portX = LogicNodeRenderer.outputPortX(node.x);
            int portY = LogicNodeRenderer.outputPortY(node.centerY, node.gate, 0);
            if (near(x, y, portX, portY)) return node.id;
        }
        if (near(x, y, INPUT_X, INPUT_A_Y)) return INPUT_A;
        if (near(x, y, INPUT_X, INPUT_B_Y)) return INPUT_B;
        return null;
    }

    private Target targetAt(int x, int y) {
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            for (int port = 0; port < node.gate.inputPorts; port++) {
                int portX = LogicNodeRenderer.inputPortX(node.x);
                int portY = LogicNodeRenderer.inputPortY(node.centerY, node.gate, port);
                if (near(x, y, portX, portY)) return new Target(node.id, port);
            }
        }
        return near(x, y, OUTPUT_X, OUTPUT_Y) ? new Target(OUTPUT, 0) : null;
    }

    private Node nodeAt(int x, int y) {
        for (int index = nodes.size() - 1; index >= 0; index--) {
            Node node = nodes.get(index);
            int height = LogicNodeRenderer.bodyHeight(
                node.gate.inputPorts, node.gate.outputPorts);
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
}

package com.gatekeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/** Stores the player's exact, unverified circuit builds separately from components. */
final class CraftedCircuitInventory {
    record CraftedCircuit(String name, WorkbenchGraph.Snapshot graph) {}

    static final int MAX_NAME_LENGTH = 6;
    private static final int MAX_CIRCUITS = 8;
    private final List<CraftedCircuit> circuits = new ArrayList<>();
    private int selectedIndex = -1;

    List<CraftedCircuit> circuits() { return List.copyOf(circuits); }
    int selectedIndex() { return selectedIndex; }
    CraftedCircuit selected() {
        return selectedIndex >= 0 && selectedIndex < circuits.size()
            ? circuits.get(selectedIndex) : null;
    }
    boolean isFull() { return circuits.size() >= MAX_CIRCUITS; }

    boolean add(String name, WorkbenchGraph.Snapshot graph) {
        if (name == null || graph == null || isFull()) return false;
        String trimmedName = name.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > MAX_NAME_LENGTH) return false;
        circuits.add(new CraftedCircuit(trimmedName, graph));
        selectedIndex = circuits.size() - 1;
        return true;
    }

    CraftedCircuit removeSelected() {
        if (selected() == null) return null;
        CraftedCircuit removed = circuits.remove(selectedIndex);
        if (circuits.isEmpty()) selectedIndex = -1;
        else selectedIndex = Math.min(selectedIndex, circuits.size() - 1);
        return removed;
    }

    void select(int index) {
        if (index >= 0 && index < circuits.size()) selectedIndex = index;
    }

    void moveSelection(int direction) {
        if (circuits.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        if (selectedIndex < 0) selectedIndex = 0;
        else selectedIndex = (selectedIndex + direction + circuits.size()) % circuits.size();
    }

    void clear() {
        circuits.clear();
        selectedIndex = -1;
    }

    List<String> names() {
        return circuits.stream().map(CraftedCircuit::name).distinct().toList();
    }

    String[] saveData() {
        String[] result = new String[circuits.size()];
        for (int index = 0; index < circuits.size(); index++) {
            CraftedCircuit circuit = circuits.get(index);
            String name = Base64.getUrlEncoder().withoutPadding().encodeToString(
                circuit.name.getBytes(StandardCharsets.UTF_8));
            result[index] = name + "@" + encode(circuit.graph);
        }
        return result;
    }

    void restore(String[] saved, int savedSelection) {
        clear();
        if (saved != null) {
            for (String value : saved) {
                try {
                    int split = value.indexOf('@');
                    String storedName = value.substring(0, split);
                    String name;
                    try {
                        name = new String(Base64.getUrlDecoder().decode(storedName),
                            StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException oldFormat) {
                        name = "CKT" + (Integer.parseInt(storedName) + 1);
                    }
                    name = name.trim();
                    if (name.isEmpty()) name = "CKT" + (circuits.size() + 1);
                    if (name.length() > MAX_NAME_LENGTH) {
                        name = name.substring(0, MAX_NAME_LENGTH);
                    }
                    circuits.add(new CraftedCircuit(name, decode(value.substring(split + 1))));
                } catch (RuntimeException ignored) { }
                if (circuits.size() == MAX_CIRCUITS) break;
            }
        }
        if (!circuits.isEmpty()) selectedIndex = Math.max(0,
            Math.min(savedSelection, circuits.size() - 1));
    }

    private static String encode(WorkbenchGraph.Snapshot snapshot) {
        StringBuilder nodes = new StringBuilder();
        for (WorkbenchGraph.NodeData node : snapshot.nodes()) {
            appendSeparator(nodes, ';');
            nodes.append(node.id()).append(',').append(node.x()).append(',')
                .append(node.centerY()).append(',').append(node.gate().name());
        }
        StringBuilder wires = new StringBuilder();
        for (WorkbenchGraph.WireData wire : snapshot.wires()) {
            appendSeparator(wires, ';');
            wires.append(wire.sourceId()).append(',').append(wire.targetId()).append(',')
                .append(wire.targetPort()).append(',');
            if (wire.corners().isEmpty()) wires.append('-');
            for (int index = 0; index < wire.corners().size(); index++) {
                if (index > 0) wires.append('.');
                WorkbenchGraph.RoutePoint point = wire.corners().get(index);
                wires.append(point.x()).append(':').append(point.y());
            }
        }
        StringBuilder junctions = new StringBuilder();
        for (WorkbenchGraph.JunctionData junction : snapshot.junctions()) {
            appendSeparator(junctions, ';');
            junctions.append(junction.id()).append(',').append(junction.upstreamSourceId())
                .append(',').append(junction.x()).append(',').append(junction.y());
        }
        return nodes + "#" + wires + "#" + junctions;
    }

    private static WorkbenchGraph.Snapshot decode(String encoded) {
        String[] sections = encoded.split("#", -1);
        List<WorkbenchGraph.NodeData> nodes = new ArrayList<>();
        for (String item : entries(sections[0])) {
            String[] field = item.split(",");
            nodes.add(new WorkbenchGraph.NodeData(Integer.parseInt(field[0]),
                Integer.parseInt(field[1]), Integer.parseInt(field[2]),
                GateType.valueOf(field[3])));
        }
        List<WorkbenchGraph.WireData> wires = new ArrayList<>();
        for (String item : entries(sections[1])) {
            String[] field = item.split(",", -1);
            List<WorkbenchGraph.RoutePoint> corners = new ArrayList<>();
            if (!field[3].equals("-")) {
                for (String corner : field[3].split("\\.")) {
                    String[] coordinate = corner.split(":");
                    corners.add(new WorkbenchGraph.RoutePoint(
                        Integer.parseInt(coordinate[0]), Integer.parseInt(coordinate[1])));
                }
            }
            wires.add(new WorkbenchGraph.WireData(Integer.parseInt(field[0]),
                Integer.parseInt(field[1]), Integer.parseInt(field[2]), corners));
        }
        List<WorkbenchGraph.JunctionData> junctions = new ArrayList<>();
        for (String item : entries(sections[2])) {
            String[] field = item.split(",");
            junctions.add(new WorkbenchGraph.JunctionData(Integer.parseInt(field[0]),
                Integer.parseInt(field[1]), Integer.parseInt(field[2]),
                Integer.parseInt(field[3])));
        }
        return new WorkbenchGraph.Snapshot(nodes, wires, junctions);
    }

    private static List<String> entries(String section) {
        return section.isEmpty() ? List.of() : List.of(section.split(";"));
    }

    private static void appendSeparator(StringBuilder value, char separator) {
        if (!value.isEmpty()) value.append(separator);
    }
}

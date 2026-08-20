package com.gatekeeper;

import java.util.ArrayList;
import java.util.List;

/** Compact text serialization for saved workbench node-and-wire snapshots. */
final class WorkbenchSnapshotCodec {
    private WorkbenchSnapshotCodec() {}

    static String encode(WorkbenchGraph.Snapshot snapshot) {
        if (snapshot == null) return "";
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

    static WorkbenchGraph.Snapshot decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        String[] sections = encoded.split("#", -1);
        if (sections.length != 3) throw new IllegalArgumentException("Invalid workbench snapshot");

        List<WorkbenchGraph.NodeData> nodes = new ArrayList<>();
        for (String item : entries(sections[0])) {
            String[] field = item.split(",");
            if (field.length != 4) throw new IllegalArgumentException("Invalid workbench node");
            nodes.add(new WorkbenchGraph.NodeData(Integer.parseInt(field[0]),
                Integer.parseInt(field[1]), Integer.parseInt(field[2]),
                GateType.valueOf(field[3])));
        }
        List<WorkbenchGraph.WireData> wires = new ArrayList<>();
        for (String item : entries(sections[1])) {
            String[] field = item.split(",", -1);
            if (field.length != 4) throw new IllegalArgumentException("Invalid workbench wire");
            List<WorkbenchGraph.RoutePoint> corners = new ArrayList<>();
            if (!field[3].equals("-")) {
                for (String corner : field[3].split("\\.")) {
                    String[] coordinate = corner.split(":");
                    if (coordinate.length != 2) {
                        throw new IllegalArgumentException("Invalid workbench wire corner");
                    }
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
            if (field.length != 4) throw new IllegalArgumentException("Invalid workbench junction");
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

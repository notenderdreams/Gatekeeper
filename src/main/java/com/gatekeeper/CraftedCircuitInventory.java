package com.gatekeeper;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            result[index] = name + "@" + WorkbenchSnapshotCodec.encode(circuit.graph);
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
                    WorkbenchGraph.Snapshot graph = WorkbenchSnapshotCodec.decode(
                        value.substring(split + 1));
                    if (graph != null) circuits.add(new CraftedCircuit(name, graph));
                } catch (RuntimeException ignored) { }
                if (circuits.size() == MAX_CIRCUITS) break;
            }
        }
        if (!circuits.isEmpty()) selectedIndex = Math.max(0,
            Math.min(savedSelection, circuits.size() - 1));
    }

}

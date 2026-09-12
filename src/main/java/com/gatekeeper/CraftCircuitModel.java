package com.gatekeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages state, candidate circuit names, search query / draft name, filtering,
 * selection, and scrolling for the Craft Circuit modal.
 *
 * Only lists existing custom crafted circuits whose circuit architecture matches
 * the currently built workbench graph.
 */
public final class CraftCircuitModel {
    public static final int VISIBLE_ROWS = 4;
    public static final int MAX_NAME_LENGTH = CraftedCircuitInventory.MAX_NAME_LENGTH;

    private final List<String> allCandidates = new ArrayList<>();
    private final Set<String> bagNames = new HashSet<>();
    private final List<String> filteredCandidates = new ArrayList<>();

    private String draft = "";
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private String status = "";

    public void open(List<CraftedCircuitInventory.CraftedCircuit> craftedCircuits,
                     WorkbenchGraph.Snapshot currentGraph,
                     String initialName) {
        allCandidates.clear();
        bagNames.clear();

        if (craftedCircuits != null && currentGraph != null) {
            for (CraftedCircuitInventory.CraftedCircuit circuit : craftedCircuits) {
                if (circuit != null && circuit.name() != null && !circuit.name().isBlank()) {
                    if (WorkbenchGraph.isSameArchitecture(circuit.graph(), currentGraph)) {
                        String clean = circuit.name().trim().toUpperCase();
                        if (!allCandidates.contains(clean)) {
                            allCandidates.add(clean);
                        }
                        bagNames.add(clean);
                    }
                }
            }
        }

        status = "";
        scrollOffset = 0;

        if (initialName != null && !initialName.isBlank() && allCandidates.contains(initialName.trim().toUpperCase())) {
            draft = sanitize(initialName);
        } else {
            draft = "";
        }

        updateFiltered();
        if (!draft.isEmpty() && selectedIndex >= 0) {
            ensureVisible(selectedIndex);
        }
    }

    public void setDraft(String newDraft) {
        draft = sanitize(newDraft);
        updateFiltered();
        status = "";
    }

    public boolean typeChar(char c) {
        char upper = Character.toUpperCase(c);
        if (!isValidChar(upper)) return false;
        if (draft.length() >= MAX_NAME_LENGTH) return false;
        draft += upper;
        updateFiltered();
        status = "";
        return true;
    }

    public boolean backspace() {
        if (draft.isEmpty()) return false;
        draft = draft.substring(0, draft.length() - 1);
        updateFiltered();
        status = "";
        return true;
    }

    public void clearDraft() {
        draft = "";
        updateFiltered();
        status = "";
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < filteredCandidates.size()) {
            selectedIndex = index;
            draft = filteredCandidates.get(index);
            ensureVisible(selectedIndex);
            status = "";
        }
    }

    public void navigateSelection(int direction) {
        if (filteredCandidates.isEmpty()) return;
        if (selectedIndex < 0) {
            selectedIndex = direction > 0 ? 0 : filteredCandidates.size() - 1;
        } else {
            selectedIndex = (selectedIndex + direction + filteredCandidates.size()) % filteredCandidates.size();
        }
        draft = filteredCandidates.get(selectedIndex);
        ensureVisible(selectedIndex);
        status = "";
    }

    public void scroll(int delta) {
        int max = maxScroll();
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, max));
    }

    public void ensureVisible(int index) {
        if (index < 0 || filteredCandidates.isEmpty()) return;
        if (index < scrollOffset) {
            scrollOffset = index;
        } else if (index >= scrollOffset + VISIBLE_ROWS) {
            scrollOffset = index - VISIBLE_ROWS + 1;
        }
        scroll(0);
    }

    public int maxScroll() {
        return Math.max(0, filteredCandidates.size() - VISIBLE_ROWS);
    }

    public List<String> allCandidates() {
        return Collections.unmodifiableList(allCandidates);
    }

    public List<String> filteredCandidates() {
        return Collections.unmodifiableList(filteredCandidates);
    }

    public String draft() {
        return draft;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, maxScroll()));
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "";
    }

    public boolean isBagName(String name) {
        return name != null && bagNames.contains(name.toUpperCase());
    }

    private void updateFiltered() {
        filteredCandidates.clear();
        String query = draft.trim().toUpperCase();
        for (String candidate : allCandidates) {
            if (query.isEmpty() || candidate.toUpperCase().contains(query)) {
                filteredCandidates.add(candidate);
            }
        }
        selectedIndex = -1;
        if (!draft.isEmpty()) {
            for (int i = 0; i < filteredCandidates.size(); i++) {
                if (filteredCandidates.get(i).equalsIgnoreCase(draft.trim())) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        int max = maxScroll();
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toCharArray()) {
            char upper = Character.toUpperCase(ch);
            if (isValidChar(upper) && sb.length() < MAX_NAME_LENGTH) {
                sb.append(upper);
            }
        }
        return sb.toString();
    }

    public static boolean isValidChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
    }
}

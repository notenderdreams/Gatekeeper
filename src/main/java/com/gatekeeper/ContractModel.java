package com.gatekeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Mira's behavior specifications. Player-built circuits are submitted against them. */
final class ContractModel {
    record Contract(int recipeIndex, String product, int reward) {}

    private static final List<Contract> CATALOG = List.of(
        new Contract(0, "NAND", 30),
        new Contract(1, "NOR", 30),
        new Contract(2, "XOR", 70),
        new Contract(3, "XNOR", 78),
        new Contract(4, "IMPLY", 60)
    );

    private final int[] deliveries = new int[CATALOG.size()];

    List<Contract> available(int chapter) {
        List<Contract> result = new ArrayList<>();
        int count = chapter >= 3 ? 5 : chapter >= 2 ? 3 : 0;
        for (int i = 0; i < count; i++) result.add(CATALOG.get(i));
        return result;
    }

    void complete(Contract contract) {
        deliveries[CATALOG.indexOf(contract)]++;
    }

    int deliveries(Contract contract) {
        int index = CATALOG.indexOf(contract);
        return index < 0 ? 0 : deliveries[index];
    }

    int[] deliveries() { return Arrays.copyOf(deliveries, deliveries.length); }

    void restore(int[] saved) {
        Arrays.fill(deliveries, 0);
        if (saved == null) return;
        for (int i = 0; i < Math.min(saved.length, deliveries.length); i++) {
            deliveries[i] = Math.max(0, saved[i]);
        }
    }
}

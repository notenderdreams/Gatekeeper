package com.gatekeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Mira's work orders and behavioral contracts. */
final class ContractModel {
    record Contract(int recipeIndex, String product, int requiredCount, int unitReward) {
        int totalReward() { return requiredCount * unitReward; }
    }

    private static final List<Contract> CATALOG = List.of(
        new Contract(0, "NAND", 3, 30),
        new Contract(1, "NOR", 2, 35),
        new Contract(2, "XOR", 2, 70),
        new Contract(3, "XNOR", 3, 75),
        new Contract(4, "IMPLY", 2, 60)
    );

    private final int[] deliveries = new int[CATALOG.size()];

    List<Contract> available(int chapter) {
        List<Contract> result = new ArrayList<>();
        int count = chapter >= 3 ? 5 : chapter >= 2 ? 3 : 0;
        for (int i = 0; i < count; i++) result.add(CATALOG.get(i));
        return result;
    }

    List<Contract> all() {
        return CATALOG;
    }

    void complete(Contract contract) {
        deliver(contract, 1);
    }

    void deliver(Contract contract, int amount) {
        if (contract == null || amount <= 0) return;
        int index = CATALOG.indexOf(contract);
        if (index >= 0) {
            deliveries[index] += amount;
        }
    }

    int deliveries(Contract contract) {
        if (contract == null) return 0;
        int index = CATALOG.indexOf(contract);
        return index < 0 ? 0 : deliveries[index];
    }

    int remaining(Contract contract) {
        if (contract == null) return 0;
        return Math.max(0, contract.requiredCount() - deliveries(contract));
    }

    boolean isCompleted(Contract contract) {
        if (contract == null) return false;
        return deliveries(contract) >= contract.requiredCount();
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

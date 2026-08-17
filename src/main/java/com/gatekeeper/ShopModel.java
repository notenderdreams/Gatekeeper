package com.gatekeeper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Owns the small amount of mutable state used by Mira's shop overlay. */
final class ShopModel {
    private static final int MAX_QUANTITY = 99;
    private static final int MIN_QUANTITY = -99;

    private final List<ShopProduct> products;
    private final int[] purchased;
    private int selectedIndex;
    private int quantity;
    private int balance;

    ShopModel(List<ShopProduct> products, int startingBalance) {
        if (products.isEmpty()) throw new IllegalArgumentException("Shop needs at least one item");
        this.products = List.copyOf(products);
        this.purchased = new int[products.size()];
        this.balance = Math.max(0, startingBalance);
    }

    List<ShopProduct> products() { return products; }
    ShopProduct selected() { return products.get(selectedIndex); }
    int selectedIndex() { return selectedIndex; }
    int quantity() { return quantity; }
    int balance() { return balance; }
    int total() {
        if (quantity > 0) return -selected().buyPrice() * quantity;
        if (quantity < 0) return selected().sellPrice() * -quantity;
        return 0;
    }
    int purchased(int index) { return purchased[index]; }
    int purchased(String productName) { return purchased[productIndex(productName)]; }
    int[] purchased() { return Arrays.copyOf(purchased, purchased.length); }

    void restore(int savedBalance, int[] savedInventory) {
        balance = Math.max(0, savedBalance);
        selectedIndex = 0;
        Arrays.fill(purchased, 0);
        if (savedInventory != null) {
            for (int index = 0; index < Math.min(purchased.length, savedInventory.length); index++) {
                purchased[index] = Math.max(0, savedInventory[index]);
            }
        }
        quantity = 0;
    }

    void reset() { restore(250, null); }

    void grant(String productName, int amount) {
        if (amount <= 0) return;
        for (int index = 0; index < products.size(); index++) {
            if (products.get(index).name().equals(productName)) {
                purchased[index] += amount;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown shop product: " + productName);
    }

    boolean craft(String productName, GateType[] ingredients) {
        return produce(productName, ingredients, 1);
    }

    boolean produce(String productName, GateType[] ingredients, int quantity) {
        if (quantity <= 0) return false;
        int productIndex = productIndex(productName);
        int[] required = new int[purchased.length];
        for (GateType ingredient : ingredients) {
            int ingredientIndex = productIndex(ingredient.label);
            required[ingredientIndex] += quantity;
        }
        for (int index = 0; index < required.length; index++) {
            if (purchased[index] < required[index]) return false;
        }
        for (int index = 0; index < required.length; index++) {
            purchased[index] -= required[index];
        }
        purchased[productIndex] += quantity;
        return true;
    }

    boolean consume(String productName, int amount) {
        if (amount <= 0) return false;
        int index = productIndex(productName);
        if (purchased[index] < amount) return false;
        purchased[index] -= amount;
        return true;
    }

    boolean consumeParts(Map<GateType, Integer> parts) {
        if (parts == null || parts.isEmpty()) return false;
        for (Map.Entry<GateType, Integer> entry : parts.entrySet()) {
            if (entry.getValue() <= 0
                || purchased(entry.getKey().label) < entry.getValue()) return false;
        }
        for (Map.Entry<GateType, Integer> entry : parts.entrySet()) {
            purchased[productIndex(entry.getKey().label)] -= entry.getValue();
        }
        return true;
    }

    void grantParts(Map<GateType, Integer> parts) {
        if (parts == null) return;
        for (Map.Entry<GateType, Integer> entry : parts.entrySet()) {
            if (entry.getValue() > 0) grant(entry.getKey().label, entry.getValue());
        }
    }

    void credit(int amount) {
        if (amount > 0) balance += amount;
    }

    private int productIndex(String productName) {
        for (int index = 0; index < products.size(); index++) {
            if (products.get(index).name().equals(productName)) return index;
        }
        throw new IllegalArgumentException("Unknown shop product: " + productName);
    }

    void select(int index) {
        if (index < 0 || index >= products.size() || index == selectedIndex) return;
        selectedIndex = index;
        quantity = 0;
    }

    void decreaseQuantity() { quantity = Math.max(MIN_QUANTITY, quantity - 1); }
    void increaseQuantity() { quantity = Math.min(MAX_QUANTITY, quantity + 1); }

    boolean trade() {
        int total = total();
        if (quantity == 0 || balance + total < 0
            || purchased[selectedIndex] + quantity < 0) {
            return false;
        }
        balance += total;
        purchased[selectedIndex] += quantity;
        quantity = 0;
        return true;
    }
}

package com.gatekeeper;

import java.util.Arrays;
import java.util.List;

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
    int total() { return -selected().price() * quantity; }
    int purchased(int index) { return purchased[index]; }
    int[] purchased() { return Arrays.copyOf(purchased, purchased.length); }

    void restore(int savedBalance, int[] savedInventory) {
        balance = Math.max(0, savedBalance);
        Arrays.fill(purchased, 0);
        if (savedInventory != null) {
            for (int index = 0; index < Math.min(purchased.length, savedInventory.length); index++) {
                purchased[index] = Math.max(0, savedInventory[index]);
            }
        }
        quantity = 0;
    }

    void reset() { restore(250, null); }

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

package com.gatekeeper;

import java.util.List;

/** Owns the small amount of mutable state used by Mira's shop overlay. */
final class ShopModel {
    private static final int MAX_QUANTITY = 99;

    private final List<ShopProduct> products;
    private final int[] purchased;
    private int selectedIndex;
    private int quantity = 1;
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
    int total() { return selected().price() * quantity; }
    int purchased(int index) { return purchased[index]; }

    void select(int index) {
        if (index < 0 || index >= products.size() || index == selectedIndex) return;
        selectedIndex = index;
        quantity = 1;
    }

    void decreaseQuantity() { quantity = Math.max(1, quantity - 1); }
    void increaseQuantity() { quantity = Math.min(MAX_QUANTITY, quantity + 1); }

    boolean purchase() {
        int total = total();
        if (total > balance) return false;
        balance -= total;
        purchased[selectedIndex] += quantity;
        quantity = 1;
        return true;
    }
}

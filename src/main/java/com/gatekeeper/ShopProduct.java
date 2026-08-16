package com.gatekeeper;

import java.util.List;

/** An item sold by Mira without expanding the workbench's primitive gate types. */
record ShopProduct(String name, String description, int buyPrice, int sellPrice,
                   int inputs, int outputs,
                   GateType... parts) {
    ShopProduct {
        parts = parts.clone();
    }

    @Override public GateType[] parts() {
        return parts.clone();
    }

    static List<ShopProduct> catalog() {
        return List.of(
            new ShopProduct("AND", "Outputs ON only when both inputs are ON.",
                10, 6, 2, 1, GateType.AND),
            new ShopProduct("OR", "Outputs ON when either input is ON.",
                10, 6, 2, 1, GateType.OR),
            new ShopProduct("NOT", "Inverts a single input signal.",
                8, 5, 1, 1, GateType.NOT),
            new ShopProduct("NAND", "An AND gate with an inverted output.",
                32, 24, 2, 1, GateType.AND, GateType.NOT),
            new ShopProduct("NOR", "An OR gate with an inverted output.",
                32, 24, 2, 1, GateType.OR, GateType.NOT),
            new ShopProduct("XOR", "Outputs ON when the inputs differ.",
                82, 62, 2, 1, GateType.NOT, GateType.AND, GateType.NOT,
                GateType.AND, GateType.OR),
            new ShopProduct("XNOR", "Outputs ON when both inputs match.",
                90, 68, 2, 1, GateType.OR, GateType.AND, GateType.NOT,
                GateType.AND, GateType.NOT),
            new ShopProduct("IMPLY", "Outputs OFF only when A is ON and B is OFF.",
                35, 26, 2, 1, GateType.NOT, GateType.OR)
        );
    }
}

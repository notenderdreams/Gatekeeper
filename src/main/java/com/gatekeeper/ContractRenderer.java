package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/** Textured overlay for inspecting, fulfilling, and batch-delivering Mira's work orders. */
final class ContractRenderer {
    enum Action {
        NONE, SELECT_ORDER, DECREASE_AMOUNT, INCREASE_AMOUNT, MAX_AMOUNT, DELIVER, CLOSE
    }

    private static final Rectangle PANEL_BOUNDS = new Rectangle(20, 12, 440, 246);
    private static final Rectangle LIST_BOUNDS = new Rectangle(28, 48, 172, 168);
    private static final Rectangle DETAILS_BOUNDS = new Rectangle(208, 48, 244, 168);

    private static final Rectangle DECREASE_BUTTON = new Rectangle(296, 154, 22, 18);
    private static final Rectangle AMOUNT_BOX = new Rectangle(322, 154, 36, 18);
    private static final Rectangle INCREASE_BUTTON = new Rectangle(362, 154, 22, 18);
    private static final Rectangle MAX_BUTTON = new Rectangle(390, 154, 38, 18);

    private static final Rectangle DELIVER_BUTTON = new Rectangle(220, 184, 130, 24);
    private static final Rectangle CLOSE_BUTTON = new Rectangle(358, 184, 80, 24);

    private final CircuitPackageRenderer circuitRenderer;
    private final Font font;

    ContractRenderer(BufferedImage texture, BufferedImage portImage, Font font) {
        this.font = font != null ? font : GameAssets.loadPixelFont();
        this.circuitRenderer = new CircuitPackageRenderer(texture, portImage, this.font);
    }

    void draw(Graphics2D g, List<ContractModel.Contract> contracts, int selectedOrder,
              ContractModel model, CraftedCircuitInventory inventory, ShopModel shopModel,
              int pushAmount, String status, int mouseX, int mouseY) {
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, W, H);

        // Main window panel
        g.setColor(new Color(24, 20, 17));
        g.fillRect(PANEL_BOUNDS.x, PANEL_BOUNDS.y, PANEL_BOUNDS.width, PANEL_BOUNDS.height);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(PANEL_BOUNDS.x, PANEL_BOUNDS.y, PANEL_BOUNDS.width - 1, PANEL_BOUNDS.height - 1);
        g.drawRect(PANEL_BOUNDS.x + 3, PANEL_BOUNDS.y + 3, PANEL_BOUNDS.width - 7, PANEL_BOUNDS.height - 7);

        // Header
        g.setColor(new Color(45, 34, 25));
        g.fillRect(PANEL_BOUNDS.x + 4, PANEL_BOUNDS.y + 4, PANEL_BOUNDS.width - 8, 28);
        g.setColor(YELLOW);
        centeredText(g, "MIRA'S WORK COMMISSIONS", W / 2, 28, 1.4f);

        // Left section: Orders list
        g.setColor(DIM);
        text(g, "ACTIVE COMMISSIONS (" + contracts.size() + ")", LIST_BOUNDS.x + 2, LIST_BOUNDS.y - 4, 0.9f);
        g.setColor(new Color(12, 13, 14));
        g.fillRect(LIST_BOUNDS.x, LIST_BOUNDS.y, LIST_BOUNDS.width, LIST_BOUNDS.height);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(LIST_BOUNDS.x, LIST_BOUNDS.y, LIST_BOUNDS.width - 1, LIST_BOUNDS.height - 1);

        for (int i = 0; i < contracts.size(); i++) {
            ContractModel.Contract contract = contracts.get(i);
            Rectangle row = orderRowBounds(i);
            boolean isSelected = (i == selectedOrder);
            boolean isHovered = row.contains(mouseX, mouseY);
            int delivered = model.deliveries(contract);
            boolean completed = model.isCompleted(contract);

            if (isSelected) {
                g.setColor(new Color(84, 62, 40));
                g.fillRect(row.x, row.y, row.width, row.height);
                g.setColor(YELLOW);
                g.drawRect(row.x, row.y, row.width - 1, row.height - 1);
            } else if (isHovered) {
                g.setColor(new Color(45, 42, 36));
                g.fillRect(row.x, row.y, row.width, row.height);
                g.setColor(CYAN);
                g.drawRect(row.x, row.y, row.width - 1, row.height - 1);
            } else {
                g.setColor(i % 2 == 0 ? new Color(18, 19, 20) : new Color(14, 15, 16));
                g.fillRect(row.x, row.y, row.width, row.height);
            }

            g.setColor(isSelected ? YELLOW : INK);
            text(g, (isSelected ? "> " : "  ") + contract.product(), row.x + 4, row.y + 14, 1.0f);

            if (completed) {
                g.setColor(CYAN);
                text(g, "[DONE " + delivered + "/" + contract.requiredCount() + "]", row.x + row.width - 70, row.y + 14, 0.85f);
            } else {
                g.setColor(delivered > 0 ? YELLOW : DIM);
                text(g, delivered + "/" + contract.requiredCount() + " (+" + contract.unitReward() + "C)",
                    row.x + row.width - 72, row.y + 14, 0.85f);
            }
        }

        // Right section: Details & Delivery
        if (selectedOrder >= 0 && selectedOrder < contracts.size()) {
            ContractModel.Contract contract = contracts.get(selectedOrder);
            int delivered = model.deliveries(contract);
            int required = contract.requiredCount();
            boolean completed = model.isCompleted(contract);

            // Compute available stock
            int matchingCraftedCount = countMatchingCrafted(contract, inventory);
            int producedCount = shopModel != null ? shopModel.purchased(contract.product()) : 0;
            int totalAvailable = matchingCraftedCount + producedCount;

            g.setColor(new Color(15, 17, 18));
            g.fillRect(DETAILS_BOUNDS.x, DETAILS_BOUNDS.y, DETAILS_BOUNDS.width, DETAILS_BOUNDS.height);
            g.setColor(new Color(84, 62, 40));
            g.drawRect(DETAILS_BOUNDS.x, DETAILS_BOUNDS.y, DETAILS_BOUNDS.width - 1, DETAILS_BOUNDS.height - 1);

            // Details Header
            g.setColor(YELLOW);
            text(g, "ORDER: " + contract.product() + " GATE", DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 16, 1.05f);
            g.setColor(DIM);
            text(g, "+" + contract.unitReward() + "C EACH", DETAILS_BOUNDS.x + DETAILS_BOUNDS.width - 64, DETAILS_BOUNDS.y + 16, 0.9f);

            // Progress Bar
            g.setColor(INK);
            text(g, "PROGRESS: " + delivered + " / " + required + " DELIVERED", DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 36, 0.95f);

            int barX = DETAILS_BOUNDS.x + 8;
            int barY = DETAILS_BOUNDS.y + 44;
            int barW = DETAILS_BOUNDS.width - 16;
            int barH = 10;
            g.setColor(new Color(8, 9, 10));
            g.fillRect(barX, barY, barW, barH);
            g.setColor(new Color(60, 45, 30));
            g.drawRect(barX, barY, barW - 1, barH - 1);

            int fillW = Math.min(barW - 2, Math.round((float) delivered / required * (barW - 2)));
            if (fillW > 0) {
                g.setColor(completed ? CYAN : YELLOW);
                g.fillRect(barX + 1, barY + 1, fillW, barH - 2);
            }

            // Stock Details
            g.setColor(new Color(25, 29, 28));
            g.fillRect(DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 60, DETAILS_BOUNDS.width - 16, 42);
            g.setColor(new Color(70, 55, 35));
            g.drawRect(DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 60, DETAILS_BOUNDS.width - 17, 41);

            g.setColor(totalAvailable > 0 ? INK : RED);
            text(g, "READY IN STOCK: " + totalAvailable + " UNITS", DETAILS_BOUNDS.x + 14, DETAILS_BOUNDS.y + 74, 0.95f);
            g.setColor(DIM);
            text(g, "• Crafted in Bag: " + matchingCraftedCount + "  |  • Produced: " + producedCount,
                DETAILS_BOUNDS.x + 14, DETAILS_BOUNDS.y + 92, 0.85f);

            // Mini circuit preview if available
            CraftedCircuitInventory.CraftedCircuit firstMatching = firstMatchingCrafted(contract, inventory);
            if (firstMatching != null) {
                Rectangle miniPreview = new Rectangle(DETAILS_BOUNDS.x + 14, DETAILS_BOUNDS.y + 108, 60, 38);
                circuitRenderer.draw(g, firstMatching, miniPreview);
            }

            // Push Amount Selector
            g.setColor(INK);
            text(g, "PUSH AMOUNT:", DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 167, 0.95f);

            // Decrease button [-]
            boolean decHover = DECREASE_BUTTON.contains(mouseX, mouseY);
            g.setColor(decHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(DECREASE_BUTTON.x, DECREASE_BUTTON.y, DECREASE_BUTTON.width, DECREASE_BUTTON.height);
            g.setColor(decHover ? YELLOW : new Color(90, 70, 45));
            g.drawRect(DECREASE_BUTTON.x, DECREASE_BUTTON.y, DECREASE_BUTTON.width - 1, DECREASE_BUTTON.height - 1);
            g.setColor(INK);
            centeredText(g, "-", DECREASE_BUTTON.x + DECREASE_BUTTON.width / 2, DECREASE_BUTTON.y + 13, 1.0f);

            // Amount box
            g.setColor(new Color(10, 12, 11));
            g.fillRect(AMOUNT_BOX.x, AMOUNT_BOX.y, AMOUNT_BOX.width, AMOUNT_BOX.height);
            g.setColor(YELLOW);
            g.drawRect(AMOUNT_BOX.x, AMOUNT_BOX.y, AMOUNT_BOX.width - 1, AMOUNT_BOX.height - 1);
            centeredText(g, Integer.toString(pushAmount), AMOUNT_BOX.x + AMOUNT_BOX.width / 2, AMOUNT_BOX.y + 13, 1.0f);

            // Increase button [+]
            boolean incHover = INCREASE_BUTTON.contains(mouseX, mouseY);
            g.setColor(incHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(INCREASE_BUTTON.x, INCREASE_BUTTON.y, INCREASE_BUTTON.width, INCREASE_BUTTON.height);
            g.setColor(incHover ? YELLOW : new Color(90, 70, 45));
            g.drawRect(INCREASE_BUTTON.x, INCREASE_BUTTON.y, INCREASE_BUTTON.width - 1, INCREASE_BUTTON.height - 1);
            g.setColor(INK);
            centeredText(g, "+", INCREASE_BUTTON.x + INCREASE_BUTTON.width / 2, INCREASE_BUTTON.y + 13, 1.0f);

            // Max button [MAX]
            boolean maxHover = MAX_BUTTON.contains(mouseX, mouseY);
            g.setColor(maxHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(MAX_BUTTON.x, MAX_BUTTON.y, MAX_BUTTON.width, MAX_BUTTON.height);
            g.setColor(maxHover ? YELLOW : new Color(90, 70, 45));
            g.drawRect(MAX_BUTTON.x, MAX_BUTTON.y, MAX_BUTTON.width - 1, MAX_BUTTON.height - 1);
            g.setColor(CYAN);
            centeredText(g, "MAX", MAX_BUTTON.x + MAX_BUTTON.width / 2, MAX_BUTTON.y + 13, 0.85f);

            // Deliver button
            boolean canDeliver = pushAmount > 0 && totalAvailable >= pushAmount;
            boolean deliverHover = DELIVER_BUTTON.contains(mouseX, mouseY) && canDeliver;
            g.setColor(deliverHover ? new Color(50, 100, 60) : (canDeliver ? new Color(35, 70, 42) : new Color(25, 30, 27)));
            g.fillRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width, DELIVER_BUTTON.height);
            g.setColor(deliverHover ? YELLOW : (canDeliver ? new Color(90, 180, 100) : new Color(60, 65, 62)));
            g.drawRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width - 1, DELIVER_BUTTON.height - 1);
            g.setColor(canDeliver ? INK : DIM);
            int payout = pushAmount * contract.unitReward();
            centeredText(g, "DELIVER " + pushAmount + " (+" + payout + "C)",
                DELIVER_BUTTON.x + DELIVER_BUTTON.width / 2, DELIVER_BUTTON.y + 16, 0.95f);

            // Close button
            boolean closeHover = CLOSE_BUTTON.contains(mouseX, mouseY);
            g.setColor(closeHover ? new Color(80, 40, 35) : new Color(50, 28, 25));
            g.fillRect(CLOSE_BUTTON.x, CLOSE_BUTTON.y, CLOSE_BUTTON.width, CLOSE_BUTTON.height);
            g.setColor(closeHover ? YELLOW : new Color(140, 60, 50));
            g.drawRect(CLOSE_BUTTON.x, CLOSE_BUTTON.y, CLOSE_BUTTON.width - 1, CLOSE_BUTTON.height - 1);
            g.setColor(INK);
            centeredText(g, "CLOSE [ESC]", CLOSE_BUTTON.x + CLOSE_BUTTON.width / 2, CLOSE_BUTTON.y + 16, 0.95f);
        }

        // Status Line
        g.setColor(status.startsWith("FAILED") || status.startsWith("NO ") ? RED : (status.startsWith("ORDER COMPLETE") ? YELLOW : CYAN));
        centeredText(g, status, W / 2, 226, 0.95f);

        // Footer Hints
        g.setColor(DIM);
        centeredText(g, "UP/DOWN: SELECT ORDER | LEFT/RIGHT: AMOUNT | P: MAX | ENTER: DELIVER | ESC: CLOSE",
            W / 2, 245, 0.8f);
    }

    Action actionAt(int mouseX, int mouseY, List<ContractModel.Contract> contracts) {
        if (CLOSE_BUTTON.contains(mouseX, mouseY)) return Action.CLOSE;
        if (DELIVER_BUTTON.contains(mouseX, mouseY)) return Action.DELIVER;
        if (DECREASE_BUTTON.contains(mouseX, mouseY)) return Action.DECREASE_AMOUNT;
        if (INCREASE_BUTTON.contains(mouseX, mouseY)) return Action.INCREASE_AMOUNT;
        if (MAX_BUTTON.contains(mouseX, mouseY)) return Action.MAX_AMOUNT;

        if (contracts != null) {
            for (int i = 0; i < contracts.size(); i++) {
                if (orderRowBounds(i).contains(mouseX, mouseY)) return Action.SELECT_ORDER;
            }
        }
        return Action.NONE;
    }

    int orderIndexAt(int mouseX, int mouseY, List<ContractModel.Contract> contracts) {
        if (contracts == null) return -1;
        for (int i = 0; i < contracts.size(); i++) {
            if (orderRowBounds(i).contains(mouseX, mouseY)) return i;
        }
        return -1;
    }

    static Rectangle orderRowBounds(int index) {
        return new Rectangle(LIST_BOUNDS.x + 4, LIST_BOUNDS.y + 6 + index * 32, LIST_BOUNDS.width - 8, 26);
    }

    static int countMatchingCrafted(ContractModel.Contract contract, CraftedCircuitInventory inventory) {
        if (contract == null || inventory == null) return 0;
        CircuitRecipe target = CircuitRecipe.all().get(contract.recipeIndex());
        int count = 0;
        for (CraftedCircuitInventory.CraftedCircuit circuit : inventory.circuits()) {
            WorkbenchGraph tested = new WorkbenchGraph(target);
            tested.restore(circuit.graph());
            if (tested.failedCases(target) == 0) count++;
        }
        return count;
    }

    static CraftedCircuitInventory.CraftedCircuit firstMatchingCrafted(
        ContractModel.Contract contract, CraftedCircuitInventory inventory) {
        if (contract == null || inventory == null) return null;
        CircuitRecipe target = CircuitRecipe.all().get(contract.recipeIndex());
        for (CraftedCircuitInventory.CraftedCircuit circuit : inventory.circuits()) {
            WorkbenchGraph tested = new WorkbenchGraph(target);
            tested.restore(circuit.graph());
            if (tested.failedCases(target) == 0) return circuit;
        }
        return null;
    }

    private void text(Graphics2D g, String text, int x, int y, float scale) {
        Font old = g.getFont();
        g.setFont(font.deriveFont(font.getSize2D() * scale));
        g.drawString(text, x, y);
        g.setFont(old);
    }

    private void centeredText(Graphics2D g, String text, int cx, int y, float scale) {
        Font old = g.getFont();
        g.setFont(font.deriveFont(font.getSize2D() * scale));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, y);
        g.setFont(old);
    }
}

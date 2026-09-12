package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/** Textured overlay for inspecting, fulfilling, and batch-delivering Mira's work orders. */
final class ContractRenderer {
    enum Action {
        NONE, SELECT_ORDER, DECREASE_AMOUNT, INCREASE_AMOUNT, MAX_AMOUNT, DELIVER, CLOSE,
        OPEN_CIRCUIT_PICKER, CLOSE_CIRCUIT_PICKER, SELECT_PICKER_CIRCUIT, CLEAR_CIRCUIT_SELECTION
    }

    private static final Rectangle PANEL_BOUNDS = new Rectangle(20, 12, 440, 246);
    private static final Rectangle CLOSE_X_BUTTON = new Rectangle(436, 18, 16, 16);
    private static final Rectangle LIST_BOUNDS = new Rectangle(28, 48, 172, 166);
    private static final Rectangle DETAILS_BOUNDS = new Rectangle(206, 48, 246, 166);

    static final Rectangle CIRCUIT_SLOT = new Rectangle(218, 103, 60, 40);

    private static final Rectangle DECREASE_BUTTON = new Rectangle(290, 151, 24, 18);
    private static final Rectangle AMOUNT_BOX = new Rectangle(318, 151, 36, 18);
    private static final Rectangle INCREASE_BUTTON = new Rectangle(358, 151, 24, 18);
    private static final Rectangle MAX_BUTTON = new Rectangle(386, 151, 46, 18);

    private static final Rectangle DELIVER_BUTTON = new Rectangle(212, 182, 138, 24);
    private static final Rectangle CLOSE_BUTTON = new Rectangle(356, 182, 90, 24);

    // Modal Circuit Picker Bounds
    static final Rectangle PICKER_PANEL = new Rectangle(40, 20, 400, 230);
    static final Rectangle PICKER_CLOSE_X = new Rectangle(414, 26, 16, 16);
    static final Rectangle PICKER_CLEAR_BUTTON = new Rectangle(100, 218, 130, 22);
    static final Rectangle PICKER_CANCEL_BUTTON = new Rectangle(250, 218, 130, 22);

    private final CircuitPackageRenderer circuitRenderer;
    private final Font font;

    ContractRenderer(BufferedImage texture, BufferedImage portImage, Font font) {
        Font baseFont = font != null ? font : GameAssets.loadPixelFont();
        float baseSize = baseFont.getSize2D() > 2.0f ? baseFont.getSize2D() : PIXEL_FONT_BASE_SIZE;
        this.font = baseFont.deriveFont(Font.PLAIN, baseSize);
        this.circuitRenderer = new CircuitPackageRenderer(texture, portImage, this.font);
    }

    void draw(Graphics2D g, List<ContractModel.Contract> contracts, int selectedOrder,
              ContractModel model, CraftedCircuitInventory inventory, ShopModel shopModel,
              int pushAmount, String status, int mouseX, int mouseY) {
        draw(g, contracts, selectedOrder, model, inventory, shopModel, pushAmount, status, mouseX, mouseY,
            false, null, -1);
    }

    void draw(Graphics2D g, List<ContractModel.Contract> contracts, int selectedOrder,
              ContractModel model, CraftedCircuitInventory inventory, ShopModel shopModel,
              int pushAmount, String status, int mouseX, int mouseY,
              boolean pickerOpen, CraftedCircuitInventory.CraftedCircuit assignedCircuit,
              int focusedPickerIndex) {
        g.setColor(new Color(0, 0, 0, 195));
        g.fillRect(0, 0, W, H);

        // Main window panel
        g.setColor(new Color(24, 20, 17));
        g.fillRect(PANEL_BOUNDS.x, PANEL_BOUNDS.y, PANEL_BOUNDS.width, PANEL_BOUNDS.height);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(PANEL_BOUNDS.x, PANEL_BOUNDS.y, PANEL_BOUNDS.width - 1, PANEL_BOUNDS.height - 1);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(PANEL_BOUNDS.x + 3, PANEL_BOUNDS.y + 3, PANEL_BOUNDS.width - 7, PANEL_BOUNDS.height - 7);

        // Header
        g.setColor(new Color(42, 31, 23));
        g.fillRect(PANEL_BOUNDS.x + 4, PANEL_BOUNDS.y + 4, PANEL_BOUNDS.width - 8, 26);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(PANEL_BOUNDS.x + 4, PANEL_BOUNDS.y + 4, PANEL_BOUNDS.width - 9, 25);
        g.setColor(YELLOW);
        centeredText(g, "MIRA'S WORK COMMISSIONS", W / 2, 33, 1.35f);

        // Header Close X Button
        boolean xHover = !pickerOpen && CLOSE_X_BUTTON.contains(mouseX, mouseY);
        g.setColor(xHover ? RED : DIM);
        centeredText(g, "X", CLOSE_X_BUTTON.x + CLOSE_X_BUTTON.width / 2, CLOSE_X_BUTTON.y + 12, 1.0f);

        // Left section: Orders list
        g.setColor(DIM);
        text(g, "ACTIVE COMMISSIONS (" + contracts.size() + ")", LIST_BOUNDS.x + 2, LIST_BOUNDS.y - 4, 0.85f);
        g.setColor(new Color(12, 13, 14));
        g.fillRect(LIST_BOUNDS.x, LIST_BOUNDS.y, LIST_BOUNDS.width, LIST_BOUNDS.height);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(LIST_BOUNDS.x, LIST_BOUNDS.y, LIST_BOUNDS.width - 1, LIST_BOUNDS.height - 1);

        for (int i = 0; i < contracts.size(); i++) {
            ContractModel.Contract contract = contracts.get(i);
            Rectangle row = orderRowBounds(i);
            boolean isSelected = (i == selectedOrder);
            boolean isHovered = !pickerOpen && row.contains(mouseX, mouseY);
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
            text(g, (isSelected ? "> " : "  ") + contract.product() + " GATE", row.x + 4, row.y + 18, 0.95f);

            String statusBadge;
            Color badgeColor;
            if (completed) {
                statusBadge = "[DONE " + delivered + "/" + contract.requiredCount() + "]";
                badgeColor = CYAN;
            } else {
                statusBadge = delivered + "/" + contract.requiredCount() + " (+" + contract.unitReward() + "C)";
                badgeColor = delivered > 0 ? YELLOW : DIM;
            }
            g.setColor(badgeColor);
            rightAlignedText(g, statusBadge, row.x + row.width - 6, row.y + 18, 0.8f);
        }

        // Right section: Details & Delivery
        if (selectedOrder >= 0 && selectedOrder < contracts.size()) {
            ContractModel.Contract contract = contracts.get(selectedOrder);
            int delivered = model.deliveries(contract);
            int required = contract.requiredCount();
            boolean completed = model.isCompleted(contract);

            // Compute available stock
            int producedCount = shopModel != null ? shopModel.purchased(contract.product()) : 0;
            int bagCount = (inventory != null) ? inventory.circuits().size() : 0;
            int totalAvailable = (assignedCircuit != null ? 1 : 0) + producedCount;

            g.setColor(new Color(15, 17, 18));
            g.fillRect(DETAILS_BOUNDS.x, DETAILS_BOUNDS.y, DETAILS_BOUNDS.width, DETAILS_BOUNDS.height);
            g.setColor(new Color(84, 62, 40));
            g.drawRect(DETAILS_BOUNDS.x, DETAILS_BOUNDS.y, DETAILS_BOUNDS.width - 1, DETAILS_BOUNDS.height - 1);

            // Details Header
            g.setColor(new Color(25, 22, 20));
            g.fillRect(DETAILS_BOUNDS.x + 4, DETAILS_BOUNDS.y + 4, DETAILS_BOUNDS.width - 8, 20);
            g.setColor(YELLOW);
            text(g, "COMMISSION: " + contract.product() + " GATE", DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 18, 1.0f);
            g.setColor(DIM);
            rightAlignedText(g, "+" + contract.unitReward() + "C EACH", DETAILS_BOUNDS.x + DETAILS_BOUNDS.width - 8, DETAILS_BOUNDS.y + 18, 0.85f);

            // Progress Bar
            g.setColor(INK);
            text(g, "PROGRESS: " + delivered + " / " + required + " DELIVERED", DETAILS_BOUNDS.x + 8, DETAILS_BOUNDS.y + 34, 0.85f);

            int barX = DETAILS_BOUNDS.x + 8;
            int barY = DETAILS_BOUNDS.y + 39;
            int barW = DETAILS_BOUNDS.width - 16;
            int barH = 8;
            g.setColor(new Color(8, 9, 10));
            g.fillRect(barX, barY, barW, barH);
            g.setColor(new Color(60, 45, 30));
            g.drawRect(barX, barY, barW - 1, barH - 1);

            int fillW = Math.min(barW - 2, Math.round((float) delivered / required * (barW - 2)));
            if (fillW > 0) {
                g.setColor(completed ? CYAN : YELLOW);
                g.fillRect(barX + 1, barY + 1, fillW, barH - 2);
            }

            // Stock Details & Circuit Preview Card
            int cardX = DETAILS_BOUNDS.x + 8;
            int cardY = DETAILS_BOUNDS.y + 52;
            int cardW = DETAILS_BOUNDS.width - 16;
            int cardH = 46;
            Rectangle cardRect = new Rectangle(cardX, cardY, cardW, cardH);
            g.setColor(new Color(22, 25, 24));
            g.fillRect(cardX, cardY, cardW, cardH);
            g.setColor(new Color(70, 55, 35));
            g.drawRect(cardX, cardY, cardW - 1, cardH - 1);

            // Interactive Circuit Slot Box
            boolean slotHover = !pickerOpen && (CIRCUIT_SLOT.contains(mouseX, mouseY) || cardRect.contains(mouseX, mouseY));
            g.setColor(slotHover ? new Color(24, 30, 26) : new Color(10, 12, 11));
            g.fillRect(CIRCUIT_SLOT.x, CIRCUIT_SLOT.y, CIRCUIT_SLOT.width, CIRCUIT_SLOT.height);
            g.setColor(slotHover ? YELLOW : (assignedCircuit != null ? new Color(140, 105, 55) : new Color(60, 50, 38)));
            g.drawRect(CIRCUIT_SLOT.x, CIRCUIT_SLOT.y, CIRCUIT_SLOT.width - 1, CIRCUIT_SLOT.height - 1);

            if (assignedCircuit != null) {
                circuitRenderer.draw(g, assignedCircuit, CIRCUIT_SLOT);
                if (slotHover) {
                    g.setColor(new Color(0, 0, 0, 180));
                    g.fillRect(CIRCUIT_SLOT.x + 1, CIRCUIT_SLOT.y + CIRCUIT_SLOT.height - 13, CIRCUIT_SLOT.width - 2, 12);
                    g.setColor(YELLOW);
                    centeredText(g, "CHANGE", CIRCUIT_SLOT.x + CIRCUIT_SLOT.width / 2, CIRCUIT_SLOT.y + CIRCUIT_SLOT.height - 3, 0.75f);
                }
            } else {
                if (slotHover) {
                    g.setColor(YELLOW);
                    centeredText(g, "+ CHOOSE", CIRCUIT_SLOT.x + CIRCUIT_SLOT.width / 2, CIRCUIT_SLOT.y + 17, 0.85f);
                    g.setColor(CYAN);
                    centeredText(g, "FROM BAG", CIRCUIT_SLOT.x + CIRCUIT_SLOT.width / 2, CIRCUIT_SLOT.y + 29, 0.75f);
                } else {
                    g.setColor(new Color(175, 145, 95));
                    centeredText(g, "+ SELECT", CIRCUIT_SLOT.x + CIRCUIT_SLOT.width / 2, CIRCUIT_SLOT.y + 17, 0.85f);
                    g.setColor(DIM);
                    centeredText(g, "CIRCUIT", CIRCUIT_SLOT.x + CIRCUIT_SLOT.width / 2, CIRCUIT_SLOT.y + 29, 0.75f);
                }
            }

            // Stock info (on the right side of circuit slot)
            int infoX = CIRCUIT_SLOT.x + CIRCUIT_SLOT.width + 8;
            if (assignedCircuit != null) {
                g.setColor(YELLOW);
                text(g, "IC: " + assignedCircuit.name(), infoX, cardY + 14, 0.95f);
            } else {
                g.setColor(DIM);
                text(g, "IC: (NONE SELECTED)", infoX, cardY + 14, 0.85f);
            }

            g.setColor(DIM);
            text(g, "Bag: " + bagCount + " ICs  |  Produced: " + producedCount, infoX, cardY + 27, 0.8f);

            if (assignedCircuit != null) {
                g.setColor(CYAN);
                text(g, "Click slot to change IC", infoX, cardY + 40, 0.8f);
            } else if (bagCount > 0) {
                g.setColor(YELLOW);
                text(g, "Click slot to choose IC", infoX, cardY + 40, 0.8f);
            } else {
                g.setColor(RED);
                text(g, "Craft at workbench [B]", infoX, cardY + 40, 0.8f);
            }

            // Quantity Selector Row
            g.setColor(INK);
            text(g, "DELIVER QTY:", DETAILS_BOUNDS.x + 8, DECREASE_BUTTON.y + 14, 0.9f);

            int displayQty = (assignedCircuit != null) ? 1 : pushAmount;
            boolean canAdjustQty = (assignedCircuit == null && producedCount > 0);

            // Decrease button [-]
            boolean decHover = !pickerOpen && canAdjustQty && DECREASE_BUTTON.contains(mouseX, mouseY);
            g.setColor(decHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(DECREASE_BUTTON.x, DECREASE_BUTTON.y, DECREASE_BUTTON.width, DECREASE_BUTTON.height);
            g.setColor(decHover ? YELLOW : (canAdjustQty ? new Color(90, 70, 45) : new Color(50, 45, 38)));
            g.drawRect(DECREASE_BUTTON.x, DECREASE_BUTTON.y, DECREASE_BUTTON.width - 1, DECREASE_BUTTON.height - 1);
            g.setColor(canAdjustQty ? INK : DIM);
            centeredText(g, "-", DECREASE_BUTTON.x + DECREASE_BUTTON.width / 2, DECREASE_BUTTON.y + 14, 1.0f);

            // Amount box
            g.setColor(new Color(10, 12, 11));
            g.fillRect(AMOUNT_BOX.x, AMOUNT_BOX.y, AMOUNT_BOX.width, AMOUNT_BOX.height);
            g.setColor(assignedCircuit != null ? CYAN : YELLOW);
            g.drawRect(AMOUNT_BOX.x, AMOUNT_BOX.y, AMOUNT_BOX.width - 1, AMOUNT_BOX.height - 1);
            g.setColor((assignedCircuit != null || displayQty > 0) ? INK : DIM);
            centeredText(g, Integer.toString(displayQty), AMOUNT_BOX.x + AMOUNT_BOX.width / 2, AMOUNT_BOX.y + 14, 1.0f);

            // Increase button [+]
            boolean incHover = !pickerOpen && canAdjustQty && INCREASE_BUTTON.contains(mouseX, mouseY);
            g.setColor(incHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(INCREASE_BUTTON.x, INCREASE_BUTTON.y, INCREASE_BUTTON.width, INCREASE_BUTTON.height);
            g.setColor(incHover ? YELLOW : (canAdjustQty ? new Color(90, 70, 45) : new Color(50, 45, 38)));
            g.drawRect(INCREASE_BUTTON.x, INCREASE_BUTTON.y, INCREASE_BUTTON.width - 1, INCREASE_BUTTON.height - 1);
            g.setColor(canAdjustQty ? INK : DIM);
            centeredText(g, "+", INCREASE_BUTTON.x + INCREASE_BUTTON.width / 2, INCREASE_BUTTON.y + 14, 1.0f);

            // Max button [MAX]
            boolean maxHover = !pickerOpen && canAdjustQty && MAX_BUTTON.contains(mouseX, mouseY);
            g.setColor(maxHover ? new Color(70, 75, 72) : new Color(30, 34, 32));
            g.fillRect(MAX_BUTTON.x, MAX_BUTTON.y, MAX_BUTTON.width, MAX_BUTTON.height);
            g.setColor(maxHover ? YELLOW : (canAdjustQty ? new Color(90, 70, 45) : new Color(50, 45, 38)));
            g.drawRect(MAX_BUTTON.x, MAX_BUTTON.y, MAX_BUTTON.width - 1, MAX_BUTTON.height - 1);
            g.setColor(canAdjustQty ? CYAN : DIM);
            centeredText(g, "MAX", MAX_BUTTON.x + MAX_BUTTON.width / 2, MAX_BUTTON.y + 13, 0.85f);

            // Deliver button
            boolean canDeliver = !completed && (assignedCircuit != null || (producedCount >= pushAmount && pushAmount > 0));
            boolean deliverHover = !pickerOpen && DELIVER_BUTTON.contains(mouseX, mouseY) && canDeliver;
            g.setColor(deliverHover ? new Color(50, 105, 60) : (canDeliver ? new Color(32, 72, 42) : new Color(24, 28, 26)));
            g.fillRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width, DELIVER_BUTTON.height);
            g.setColor(deliverHover ? YELLOW : (canDeliver ? new Color(90, 180, 100) : new Color(50, 55, 52)));
            g.drawRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width - 1, DELIVER_BUTTON.height - 1);
            g.setColor(canDeliver ? INK : DIM);

            String deliverLabel;
            if (completed) {
                deliverLabel = "[COMPLETED]";
            } else if (assignedCircuit != null) {
                deliverLabel = "DELIVER " + assignedCircuit.name() + " (+" + contract.unitReward() + "C)";
            } else if (producedCount > 0) {
                int payout = pushAmount * contract.unitReward();
                deliverLabel = "DELIVER " + pushAmount + " (+" + payout + "C)";
            } else if (bagCount > 0) {
                deliverLabel = "CHOOSE IC TO DELIVER";
            } else {
                deliverLabel = "NO CIRCUITS IN BAG";
            }
            centeredText(g, deliverLabel, DELIVER_BUTTON.x + DELIVER_BUTTON.width / 2, DELIVER_BUTTON.y + 17, 0.90f);

            // Close button
            boolean closeHover = !pickerOpen && CLOSE_BUTTON.contains(mouseX, mouseY);
            g.setColor(closeHover ? new Color(75, 38, 32) : new Color(48, 26, 22));
            g.fillRect(CLOSE_BUTTON.x, CLOSE_BUTTON.y, CLOSE_BUTTON.width, CLOSE_BUTTON.height);
            g.setColor(closeHover ? YELLOW : new Color(135, 55, 45));
            g.drawRect(CLOSE_BUTTON.x, CLOSE_BUTTON.y, CLOSE_BUTTON.width - 1, CLOSE_BUTTON.height - 1);
            g.setColor(INK);
            centeredText(g, "CLOSE [ESC]", CLOSE_BUTTON.x + CLOSE_BUTTON.width / 2, CLOSE_BUTTON.y + 17, 0.92f);
        }

        // Status Line
        if (!pickerOpen) {
            g.setColor(status.startsWith("FAILED") || status.startsWith("NO ") || status.startsWith("REJECTED")
                ? RED : (status.startsWith("ORDER COMPLETE") ? YELLOW : CYAN));
            centeredText(g, status, W / 2, 224, 0.92f);
        }

        // Footer Hints
        g.setColor(DIM);
        if (!pickerOpen) {
            centeredText(g, "[CLICK SLOT] CHOOSE IC  [W/S] ORDER  [ENTER] DELIVER  [ESC] CLOSE",
                W / 2, 244, 0.8f);
        }

        // Circuit Picker Modal Overlay
        if (pickerOpen) {
            drawCircuitPicker(g, contracts, selectedOrder, inventory, assignedCircuit, focusedPickerIndex, mouseX, mouseY);
        }
    }

    private void drawCircuitPicker(Graphics2D g, List<ContractModel.Contract> contracts, int selectedOrder,
                                   CraftedCircuitInventory inventory,
                                   CraftedCircuitInventory.CraftedCircuit assignedCircuit,
                                   int focusedPickerIndex, int mouseX, int mouseY) {
        // Dark backdrop
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRect(0, 0, W, H);

        // Modal panel
        g.setColor(new Color(24, 20, 17));
        g.fillRect(PICKER_PANEL.x, PICKER_PANEL.y, PICKER_PANEL.width, PICKER_PANEL.height);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(PICKER_PANEL.x, PICKER_PANEL.y, PICKER_PANEL.width - 1, PICKER_PANEL.height - 1);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(PICKER_PANEL.x + 3, PICKER_PANEL.y + 3, PICKER_PANEL.width - 7, PICKER_PANEL.height - 7);

        // Header bar
        g.setColor(new Color(42, 31, 23));
        g.fillRect(PICKER_PANEL.x + 4, PICKER_PANEL.y + 4, PICKER_PANEL.width - 8, 24);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(PICKER_PANEL.x + 4, PICKER_PANEL.y + 4, PICKER_PANEL.width - 9, 23);
        g.setColor(YELLOW);
        String orderName = (selectedOrder >= 0 && selectedOrder < contracts.size())
            ? contracts.get(selectedOrder).product() + " GATE" : "COMMISSION";
        centeredText(g, "SELECT CIRCUIT FOR " + orderName, W / 2, PICKER_PANEL.y + 20, 1.25f);

        // Header Close X
        boolean xHover = PICKER_CLOSE_X.contains(mouseX, mouseY);
        g.setColor(xHover ? RED : DIM);
        centeredText(g, "X", PICKER_CLOSE_X.x + PICKER_CLOSE_X.width / 2, PICKER_CLOSE_X.y + 12, 1.0f);

        // Subtitle
        g.setColor(DIM);
        centeredText(g, "Choose a circuit from your bag to fulfill this work order:", W / 2, PICKER_PANEL.y + 42, 0.85f);

        List<CraftedCircuitInventory.CraftedCircuit> circuits = (inventory != null)
            ? inventory.circuits() : List.of();

        if (circuits.isEmpty()) {
            g.setColor(RED);
            centeredText(g, "NO CRAFTED CIRCUITS IN YOUR BAG", W / 2, PICKER_PANEL.y + 96, 1.0f);
            g.setColor(DIM);
            centeredText(g, "Return to the workbench in your bedroom [B] to assemble one.", W / 2, PICKER_PANEL.y + 116, 0.85f);
        } else {
            for (int i = 0; i < circuits.size(); i++) {
                CraftedCircuitInventory.CraftedCircuit circuit = circuits.get(i);
                Rectangle card = pickerCardBounds(i);
                boolean isAssigned = circuit.equals(assignedCircuit);
                boolean isHovered = card.contains(mouseX, mouseY) || (i == focusedPickerIndex);

                if (isAssigned) {
                    g.setColor(new Color(65, 48, 25));
                    g.fillRect(card.x, card.y, card.width, card.height);
                    g.setColor(YELLOW);
                    g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
                    g.drawRect(card.x + 1, card.y + 1, card.width - 3, card.height - 3);
                } else if (isHovered) {
                    g.setColor(new Color(38, 44, 42));
                    g.fillRect(card.x, card.y, card.width, card.height);
                    g.setColor(CYAN);
                    g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
                } else {
                    g.setColor(new Color(16, 18, 17));
                    g.fillRect(card.x, card.y, card.width, card.height);
                    g.setColor(new Color(70, 56, 38));
                    g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
                }

                // Preview frame
                Rectangle preview = new Rectangle(card.x + 4, card.y + 4, card.width - 8, 36);
                g.setColor(new Color(10, 11, 12));
                g.fillRect(preview.x, preview.y, preview.width, preview.height);
                g.setColor(new Color(45, 40, 32));
                g.drawRect(preview.x, preview.y, preview.width - 1, preview.height - 1);
                circuitRenderer.draw(g, circuit, preview);

                // Circuit name
                g.setColor(isAssigned ? YELLOW : (isHovered ? CYAN : INK));
                centeredText(g, circuit.name(), card.x + card.width / 2, card.y + 51, 0.92f);

                // Pin specs
                int inputs = CircuitPackageRenderer.inputCount(circuit.graph());
                int outputs = CircuitPackageRenderer.outputCount(circuit.graph());
                g.setColor(isAssigned ? new Color(220, 190, 100) : DIM);
                centeredText(g, inputs + " IN / " + outputs + " OUT", card.x + card.width / 2, card.y + 63, 0.75f);
            }
        }

        // Footer Clear button
        boolean clearHover = PICKER_CLEAR_BUTTON.contains(mouseX, mouseY);
        g.setColor(clearHover ? new Color(75, 38, 32) : new Color(42, 24, 20));
        g.fillRect(PICKER_CLEAR_BUTTON.x, PICKER_CLEAR_BUTTON.y, PICKER_CLEAR_BUTTON.width, PICKER_CLEAR_BUTTON.height);
        g.setColor(clearHover ? YELLOW : new Color(135, 55, 45));
        g.drawRect(PICKER_CLEAR_BUTTON.x, PICKER_CLEAR_BUTTON.y, PICKER_CLEAR_BUTTON.width - 1, PICKER_CLEAR_BUTTON.height - 1);
        g.setColor(assignedCircuit != null ? INK : DIM);
        centeredText(g, "CLEAR SELECTION", PICKER_CLEAR_BUTTON.x + PICKER_CLEAR_BUTTON.width / 2, PICKER_CLEAR_BUTTON.y + 15, 0.85f);

        // Footer Cancel button
        boolean cancelHover = PICKER_CANCEL_BUTTON.contains(mouseX, mouseY);
        g.setColor(cancelHover ? new Color(60, 65, 62) : new Color(30, 34, 32));
        g.fillRect(PICKER_CANCEL_BUTTON.x, PICKER_CANCEL_BUTTON.y, PICKER_CANCEL_BUTTON.width, PICKER_CANCEL_BUTTON.height);
        g.setColor(cancelHover ? YELLOW : new Color(90, 70, 45));
        g.drawRect(PICKER_CANCEL_BUTTON.x, PICKER_CANCEL_BUTTON.y, PICKER_CANCEL_BUTTON.width - 1, PICKER_CANCEL_BUTTON.height - 1);
        g.setColor(INK);
        centeredText(g, "CANCEL [ESC]", PICKER_CANCEL_BUTTON.x + PICKER_CANCEL_BUTTON.width / 2, PICKER_CANCEL_BUTTON.y + 15, 0.85f);

        // Hints
        g.setColor(DIM);
        centeredText(g, "[CLICK / ENTER] SELECT IC   [ESC] CANCEL   [DEL] CLEAR", W / 2, 244, 0.8f);
    }

    Action actionAt(int mouseX, int mouseY, List<ContractModel.Contract> contracts) {
        return actionAt(mouseX, mouseY, contracts, false, null);
    }

    Action actionAt(int mouseX, int mouseY, List<ContractModel.Contract> contracts,
                    boolean pickerOpen, CraftedCircuitInventory inventory) {
        if (pickerOpen) {
            if (PICKER_CLOSE_X.contains(mouseX, mouseY) || PICKER_CANCEL_BUTTON.contains(mouseX, mouseY)) {
                return Action.CLOSE_CIRCUIT_PICKER;
            }
            if (PICKER_CLEAR_BUTTON.contains(mouseX, mouseY)) {
                return Action.CLEAR_CIRCUIT_SELECTION;
            }
            if (inventory != null) {
                for (int i = 0; i < inventory.circuits().size(); i++) {
                    if (pickerCardBounds(i).contains(mouseX, mouseY)) {
                        return Action.SELECT_PICKER_CIRCUIT;
                    }
                }
            }
            return Action.NONE;
        }

        if (CLOSE_BUTTON.contains(mouseX, mouseY) || CLOSE_X_BUTTON.contains(mouseX, mouseY)) return Action.CLOSE;
        if (DELIVER_BUTTON.contains(mouseX, mouseY)) return Action.DELIVER;
        if (CIRCUIT_SLOT.contains(mouseX, mouseY) || isOverCircuitCard(mouseX, mouseY)) return Action.OPEN_CIRCUIT_PICKER;
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

    int pickerCircuitIndexAt(int mouseX, int mouseY, CraftedCircuitInventory inventory) {
        if (inventory == null) return -1;
        for (int i = 0; i < inventory.circuits().size(); i++) {
            if (pickerCardBounds(i).contains(mouseX, mouseY)) return i;
        }
        return -1;
    }

    static Rectangle pickerCardBounds(int index) {
        int col = index % 4;
        int row = index / 4;
        int cardW = 86;
        int cardH = 68;
        int gapX = 8;
        int gapY = 8;
        int startX = PICKER_PANEL.x + (PICKER_PANEL.width - (4 * cardW + 3 * gapX)) / 2;
        int startY = 62;
        return new Rectangle(startX + col * (cardW + gapX), startY + row * (cardH + gapY), cardW, cardH);
    }

    private static boolean isOverCircuitCard(int x, int y) {
        int cardX = DETAILS_BOUNDS.x + 8;
        int cardY = DETAILS_BOUNDS.y + 52;
        int cardW = DETAILS_BOUNDS.width - 16;
        int cardH = 46;
        return x >= cardX && x < cardX + cardW && y >= cardY && y < cardY + cardH;
    }

    int orderIndexAt(int mouseX, int mouseY, List<ContractModel.Contract> contracts) {
        if (contracts == null) return -1;
        for (int i = 0; i < contracts.size(); i++) {
            if (orderRowBounds(i).contains(mouseX, mouseY)) return i;
        }
        return -1;
    }

    static Rectangle orderRowBounds(int index) {
        return new Rectangle(LIST_BOUNDS.x + 4, LIST_BOUNDS.y + 4 + index * 32, LIST_BOUNDS.width - 8, 28);
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
        int fontSize = Math.max(1, Math.round(this.font.getSize2D() * scale));
        Font derived = font.deriveFont(Font.PLAIN, fontSize);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(derived);
        g.drawString(text, x, y);
        g.setFont(old);
    }

    private void centeredText(Graphics2D g, String text, int cx, int y, float scale) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(this.font.getSize2D() * scale));
        Font derived = font.deriveFont(Font.PLAIN, fontSize);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(derived);
        FontMetrics fm = g.getFontMetrics(derived);
        g.drawString(text, cx - fm.stringWidth(text) / 2, y);
        g.setFont(old);
    }

    private void rightAlignedText(Graphics2D g, String text, int rx, int y, float scale) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(this.font.getSize2D() * scale));
        Font derived = font.deriveFont(Font.PLAIN, fontSize);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(derived);
        FontMetrics fm = g.getFontMetrics(derived);
        g.drawString(text, rx - fm.stringWidth(text), y);
        g.setFont(old);
    }
}

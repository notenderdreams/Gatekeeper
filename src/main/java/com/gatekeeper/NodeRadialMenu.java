package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Modeless circular picker for unlocked workbench node types. */
final class NodeRadialMenu {
    record Choice(GateType gate, int craftedCircuitIndex) {
        static Choice gate(GateType gate) { return new Choice(gate, -1); }
        static Choice crafted(int index) { return new Choice(null, index); }
        boolean isCrafted() { return craftedCircuitIndex >= 0; }
    }
    // The 1536x1024 source canvas is displayed at 480x270. These Y radii
    // compensate for that non-uniform scale so the wheel is circular on screen.
    static final int OUTER_RADIUS_X = 190;
    static final int OUTER_RADIUS_Y = 225;
    static final int INNER_RADIUS_X = 64;
    static final int INNER_RADIUS_Y = 76;

    private static final int NODE_RADIUS_X = 126;
    private static final int NODE_RADIUS_Y = 149;
    private static final double NODE_SCALE = 0.76;
    private static final Color SCRIM = new Color(22, 18, 12, 92);
    private static final Color PANEL = new Color(25, 29, 25, 248);
    private static final Color PANEL_INNER = new Color(16, 19, 17, 252);
    private static final Color PANEL_HOVER = new Color(130, 88, 35, 205);
    private static final Color PANEL_ACTIVE = new Color(71, 58, 34, 180);
    private static final Color EDGE = new Color(91, 77, 51);
    private static final Color BRASS = new Color(225, 187, 105);

    private final List<GateType> unlocked = new ArrayList<>();
    private final List<Choice> choices = new ArrayList<>();
    private List<CraftedCircuitInventory.CraftedCircuit> craftedCircuits = List.of();
    private int centerX;
    private int centerY;
    private boolean open;

    boolean isOpen() { return open; }
    int centerX() { return centerX; }
    int centerY() { return centerY; }
    List<GateType> unlocked() { return List.copyOf(unlocked); }

    void openAt(int requestedX, int requestedY, List<GateType> available) {
        openAt(requestedX, requestedY, available, List.of());
    }

    void openAt(int requestedX, int requestedY, List<GateType> available,
                List<CraftedCircuitInventory.CraftedCircuit> crafted) {
        unlocked.clear();
        choices.clear();
        for (GateType gate : available) {
            if (!unlocked.contains(gate)) {
                unlocked.add(gate);
                choices.add(Choice.gate(gate));
            }
        }
        craftedCircuits = crafted == null ? List.of() : List.copyOf(crafted);
        for (int index = 0; index < craftedCircuits.size(); index++) {
            choices.add(Choice.crafted(index));
        }
        if (choices.isEmpty()) return;
        centerX = clamp(requestedX,
            WorkbenchGraph.WORK_X + OUTER_RADIUS_X,
            WorkbenchGraph.WORK_RIGHT - OUTER_RADIUS_X);
        centerY = clamp(requestedY,
            WorkbenchGraph.WORK_Y + OUTER_RADIUS_Y,
            WorkbenchGraph.WORK_BOTTOM - OUTER_RADIUS_Y);
        open = true;
    }

    boolean close() {
        if (!open) return false;
        open = false;
        return true;
    }

    GateType releaseAt(int x, int y) {
        GateType selected = gateAt(x, y);
        close();
        return selected;
    }

    GateType gateAt(int x, int y) {
        Choice choice = choiceAt(x, y);
        return choice == null ? null : choice.gate();
    }

    Choice choiceAt(int x, int y) {
        if (!open || choices.isEmpty()) return null;
        int dx = x - centerX;
        int dy = y - centerY;
        double outerDistance = square(dx / (double) OUTER_RADIUS_X)
            + square(dy / (double) OUTER_RADIUS_Y);
        double innerDistance = square(dx / (double) INNER_RADIUS_X)
            + square(dy / (double) INNER_RADIUS_Y);
        if (innerDistance < 1.0 || outerDistance > 1.0) return null;
        return choices.get(wedgeIndex(x, y));
    }

    void draw(Graphics2D graphics, LogicNodeRenderer nodeRenderer,
              BufferedImage panelTexture,
              GateType activeGate,
              CraftedCircuitInventory.CraftedCircuit activeCircuit,
              int pointerX, int pointerY) {
        draw(graphics, nodeRenderer, panelTexture, activeGate, activeCircuit, null, null, pointerX, pointerY);
    }

    void draw(Graphics2D graphics, LogicNodeRenderer nodeRenderer,
              BufferedImage panelTexture,
              GateType activeGate,
              CraftedCircuitInventory.CraftedCircuit activeCircuit,
              ShopModel inventory,
              int pointerX, int pointerY) {
        draw(graphics, nodeRenderer, panelTexture, activeGate, activeCircuit, inventory, null, pointerX, pointerY);
    }

    void draw(Graphics2D graphics, LogicNodeRenderer nodeRenderer,
              BufferedImage panelTexture,
              GateType activeGate,
              CraftedCircuitInventory.CraftedCircuit activeCircuit,
              ShopModel inventory,
              WorkbenchGraph graph,
              int pointerX, int pointerY) {
        if (!open || choices.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        // Keep every popup layer, including its shadow, inside the annotated paper canvas.
        g.clipRect(WorkbenchGraph.WORK_X, WorkbenchGraph.WORK_Y,
            WorkbenchGraph.WORK_WIDTH, WorkbenchGraph.WORK_HEIGHT);
        g.setColor(SCRIM);
        g.fillRect(WorkbenchGraph.WORK_X, WorkbenchGraph.WORK_Y,
            WorkbenchGraph.WORK_WIDTH, WorkbenchGraph.WORK_HEIGHT);

        Choice hovered = choiceAt(pointerX, pointerY);
        double wedgeDegrees = 360.0 / choices.size();

        g.setColor(new Color(24, 16, 8, 105));
        g.fillOval(centerX - OUTER_RADIUS_X + 9, centerY - OUTER_RADIUS_Y + 12,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        g.setColor(PANEL);
        g.fillOval(centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        drawPanelTexture(g, panelTexture);

        for (int index = 0; index < choices.size(); index++) {
            Choice choice = choices.get(index);
            GateType gate = choice.gate();
            double centerDegrees = 90.0 - index * wedgeDegrees;
            boolean active = isActive(choice, activeGate, activeCircuit);
            if (choice.equals(hovered) || active) {
                Arc2D wedge = new Arc2D.Double(
                    centerX - OUTER_RADIUS_X + 8, centerY - OUTER_RADIUS_Y + 9,
                    (OUTER_RADIUS_X - 8) * 2.0,
                    (OUTER_RADIUS_Y - 9) * 2.0,
                    centerDegrees - wedgeDegrees / 2.0, wedgeDegrees,
                    Arc2D.PIE);
                g.setColor(choice.equals(hovered) ? PANEL_HOVER : PANEL_ACTIVE);
                g.fill(wedge);
            }

            double radians = Math.toRadians(centerDegrees);
            int nodeCenterX = centerX
                + (int) Math.round(Math.cos(radians) * NODE_RADIUS_X);
            int nodeCenterY = centerY
                - (int) Math.round(Math.sin(radians) * NODE_RADIUS_Y);
            Graphics2D nodeGraphics = (Graphics2D) g.create();
            nodeGraphics.translate(nodeCenterX, nodeCenterY);
            double itemScale = choices.size() > 7 ? 0.55 : NODE_SCALE;
            nodeGraphics.scale(itemScale, itemScale);
            if (choice.isCrafted()) {
                CraftedCircuitInventory.CraftedCircuit circuit =
                    craftedCircuits.get(choice.craftedCircuitIndex());
                nodeRenderer.draw(nodeGraphics, -LogicNodeRenderer.BODY_WIDTH / 2, 0,
                    circuit.name(), CircuitPackageRenderer.inputCount(circuit.graph()),
                    CircuitPackageRenderer.outputCount(circuit.graph()));
            } else {
                nodeRenderer.draw(nodeGraphics,
                    -LogicNodeRenderer.BODY_WIDTH / 2, 0, gate);
            }
            nodeGraphics.dispose();

            int inputs = choice.isCrafted()
                ? CircuitPackageRenderer.inputCount(
                    craftedCircuits.get(choice.craftedCircuitIndex()).graph())
                : gate.inputPorts;
            int outputs = choice.isCrafted()
                ? CircuitPackageRenderer.outputCount(
                    craftedCircuits.get(choice.craftedCircuitIndex()).graph())
                : gate.outputPorts;
            int scaledHeight = (int) Math.round(
                LogicNodeRenderer.bodyHeight(inputs, outputs) * itemScale);
            int halfWidth = (int) Math.round(LogicNodeRenderer.BODY_WIDTH * itemScale / 2.0);

            // Draw count badge next to the gate
            int count = choiceCount(choice, inventory, graph);
            String countText = "x" + count;
            drawCountBadge(g, countText, nodeCenterX + halfWidth + 6, nodeCenterY, count > 0, choices.size() > 7);

            if (active) {
                g.setColor(BRASS);
                g.fillRect(nodeCenterX - 12,
                    nodeCenterY - scaledHeight / 2 - 8, 24, 3);
            }

            double separatorDegrees = centerDegrees + wedgeDegrees / 2.0;
            double separatorRadians = Math.toRadians(separatorDegrees);
            g.setColor(new Color(116, 92, 54, 135));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(
                centerX + (int) Math.round(Math.cos(separatorRadians) * (INNER_RADIUS_X + 8)),
                centerY - (int) Math.round(Math.sin(separatorRadians) * (INNER_RADIUS_Y + 8)),
                centerX + (int) Math.round(Math.cos(separatorRadians) * (OUTER_RADIUS_X - 10)),
                centerY - (int) Math.round(Math.sin(separatorRadians) * (OUTER_RADIUS_Y - 12)));
        }

        g.setColor(new Color(6, 8, 7, 230));
        g.setStroke(new BasicStroke(9f));
        g.drawOval(centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - OUTER_RADIUS_X + 5, centerY - OUTER_RADIUS_Y + 6,
            (OUTER_RADIUS_X - 5) * 2, (OUTER_RADIUS_Y - 6) * 2);

        g.setColor(PANEL_INNER);
        g.fillOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        g.setColor(new Color(7, 9, 8, 210));
        g.setStroke(new BasicStroke(8f));
        g.drawOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        String centerLabel = hovered == null
            ? activeCircuit != null ? activeCircuit.name() : (activeGate != null ? activeGate.label : "")
            : hovered.isCrafted()
                ? craftedCircuits.get(hovered.craftedCircuitIndex()).name()
                : hovered.gate().label;
        drawCentered(g, centerLabel, centerX, centerY - 3, 31f, BRASS);

        Choice activeChoice = hovered != null ? hovered
            : (activeCircuit != null ? Choice.crafted(craftedCircuits.indexOf(activeCircuit))
                : (activeGate != null ? Choice.gate(activeGate) : null));
        int activeCount = choiceCount(activeChoice, inventory, graph);
        String subLabel = (hovered == null ? "ACTIVE" : hovered.isCrafted() ? "CRAFTED" : "PRIMITIVE")
            + " (" + activeCount + ")";
        drawCentered(g, subLabel, centerX, centerY + 27, 16f, new Color(150, 128, 85));

        g.dispose();
    }

    int choiceCount(Choice choice, ShopModel inventory) {
        return choiceCount(choice, inventory, null);
    }

    int choiceCount(Choice choice, ShopModel inventory, WorkbenchGraph graph) {
        if (choice == null) return 0;
        if (choice.isCrafted()) {
            if (choice.craftedCircuitIndex() >= 0 && choice.craftedCircuitIndex() < craftedCircuits.size()) {
                String name = craftedCircuits.get(choice.craftedCircuitIndex()).name();
                int total = (int) craftedCircuits.stream()
                    .filter(c -> c.name().equalsIgnoreCase(name))
                    .count();
                int placed = (graph != null) ? (int) graph.nodes().stream()
                    .filter(n -> n.isCustom() && n.customName().equalsIgnoreCase(name))
                    .count() : 0;
                return Math.max(0, total - placed);
            }
            return 1;
        }
        if (choice.gate() != null) {
            int total = (inventory != null) ? inventory.purchased(choice.gate().label) : 0;
            int placed = (graph != null) ? (int) graph.nodes().stream()
                .filter(n -> !n.isCustom() && n.gate() == choice.gate())
                .count() : 0;
            return Math.max(0, total - placed);
        }
        return 0;
    }

    private static void drawCountBadge(Graphics2D g, String text, int x, int y, boolean inStock, boolean dense) {
        Font oldFont = g.getFont();
        float fontSize = dense ? 18f : 23f;
        Font badgeFont = oldFont.deriveFont(Font.BOLD, fontSize);
        g.setFont(badgeFont);
        FontMetrics metrics = g.getFontMetrics(badgeFont);
        int textWidth = metrics.stringWidth(text);
        int pillW = textWidth + 14;
        int pillH = dense ? 22 : 26;
        int pillX = x;
        int pillY = y - pillH / 2;

        g.setColor(new Color(10, 13, 11, 240));
        g.fillRoundRect(pillX, pillY, pillW, pillH, 8, 8);
        g.setColor(inStock ? new Color(140, 112, 65) : new Color(160, 45, 45));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(pillX, pillY, pillW, pillH, 8, 8);

        g.setColor(inStock ? BRASS : new Color(255, 85, 85));
        g.drawString(text, pillX + (pillW - textWidth) / 2, pillY + (pillH - metrics.getHeight()) / 2 + metrics.getAscent());
        g.setFont(oldFont);
    }

    private boolean isActive(Choice choice, GateType activeGate,
                             CraftedCircuitInventory.CraftedCircuit activeCircuit) {
        if (choice.isCrafted()) {
            return activeCircuit != null && activeCircuit.equals(
                craftedCircuits.get(choice.craftedCircuitIndex()));
        }
        return activeCircuit == null && choice.gate() == activeGate;
    }

    private void drawPanelTexture(Graphics2D graphics, BufferedImage texture) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ellipse2D housing = new Ellipse2D.Double(
            centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2.0, OUTER_RADIUS_Y * 2.0);
        g.clip(housing);
        if (texture != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(texture,
                centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
                OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2, null);
        }
        g.setColor(new Color(12, 25, 19, 118));
        g.fill(housing);

        // Shallow machined wear rings keep the texture readable without adding clutter.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2f));
        for (int inset = 24; inset <= 52; inset += 14) {
            int insetY = (int) Math.round(inset
                * OUTER_RADIUS_Y / (double) OUTER_RADIUS_X);
            g.setColor(new Color(229, 190, 111, 25));
            g.drawOval(centerX - OUTER_RADIUS_X + inset,
                centerY - OUTER_RADIUS_Y + insetY,
                (OUTER_RADIUS_X - inset) * 2,
                (OUTER_RADIUS_Y - insetY) * 2);
            g.setColor(new Color(0, 0, 0, 45));
            g.drawOval(centerX - OUTER_RADIUS_X + inset + 2,
                centerY - OUTER_RADIUS_Y + insetY + 2,
                (OUTER_RADIUS_X - inset - 2) * 2,
                (OUTER_RADIUS_Y - insetY - 2) * 2);
        }
        g.dispose();
    }

    private int wedgeIndex(int x, int y) {
        double pointerDegrees = Math.toDegrees(
            Math.atan2((centerY - y) / (double) OUTER_RADIUS_Y,
                (x - centerX) / (double) OUTER_RADIUS_X));
        pointerDegrees = normalize(pointerDegrees);
        double wedgeDegrees = 360.0 / choices.size();
        return (int) Math.floor(normalize(
            90.0 + wedgeDegrees / 2.0 - pointerDegrees) / wedgeDegrees)
            % choices.size();
    }

    private static void drawCentered(Graphics2D g, String text, int centerX,
                                     int baselineY, float size, Color color) {
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(Font.BOLD, size));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
        g.setFont(oldFont);
    }

    private static double normalize(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double square(double value) {
        return value * value;
    }
}

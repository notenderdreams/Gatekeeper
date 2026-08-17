package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Map;

import static com.gatekeeper.GameConstants.*;

/** Textured workbench overlays for requirements, certification, and production. */
final class ProgressionRenderer {
    private static final Color PANEL = new Color(20, 24, 25, 236);
    private static final Color EDGE = new Color(181, 126, 68);
    private static final Color PAPER = new Color(230, 213, 164);

    void drawCertification(Graphics2D g, CircuitRecipe recipe) {
        shade(g);
        panel(g, 101, 67, 278, 132);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "DESIGN CERTIFIED", W / 2, 91, 2);
        g.setColor(PAPER);
        GamePanel.drawCenteredPixelText(g, recipe.name, W / 2, 120, 2);
        g.setColor(CYAN);
        GamePanel.drawCenteredPixelText(g, "REPEAT PRODUCTION UNLOCKED", W / 2, 146, 1);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g, "P: PRODUCE   ENTER / ESC: CLOSE", W / 2, 180, 1);
    }

    void drawProduction(Graphics2D g, CircuitRecipe recipe, ShopModel inventory,
                        int quantity, String status) {
        shade(g);
        panel(g, 75, 42, 330, 184);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "CERTIFIED PRODUCTION", W / 2, 65, 2);
        g.setColor(PAPER);
        GamePanel.drawCenteredPixelText(g, recipe.name, W / 2, 92, 2);
        g.setColor(CYAN);
        GamePanel.drawCenteredPixelText(g, "QUANTITY  <  " + quantity + "  >", W / 2, 119, 1);
        g.setColor(PAPER);
        GamePanel.drawCenteredPixelText(g, requirementLine(recipe, inventory, quantity), W / 2, 143, 1);
        g.setColor(canProduce(recipe, inventory, quantity) ? CYAN : RED);
        GamePanel.drawCenteredPixelText(g, status, W / 2, 169, 1);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g, "UP/DOWN: DESIGN  LEFT/RIGHT: AMOUNT", W / 2, 194, 1);
        GamePanel.drawCenteredPixelText(g, "ENTER: PRODUCE   P/ESC: CLOSE", W / 2, 211, 1);
    }

    static boolean canProduce(CircuitRecipe recipe, ShopModel inventory, int quantity) {
        for (Map.Entry<GateType, Integer> entry : requirements(recipe).entrySet()) {
            if (inventory.purchased(entry.getKey().label) < entry.getValue() * quantity) return false;
        }
        return true;
    }

    private static String requirementLine(CircuitRecipe recipe, ShopModel inventory, int quantity) {
        StringBuilder text = new StringBuilder("NEED ");
        for (Map.Entry<GateType, Integer> entry : requirements(recipe).entrySet()) {
            if (text.length() > 5) text.append("  ");
            int need = entry.getValue() * quantity;
            text.append(entry.getKey().label).append(' ')
                .append(inventory.purchased(entry.getKey().label)).append('/').append(need);
        }
        return text.toString();
    }

    private static Map<GateType, Integer> requirements(CircuitRecipe recipe) {
        Map<GateType, Integer> counts = new EnumMap<>(GateType.class);
        for (GateType gate : recipe.solution) counts.merge(gate, 1, Integer::sum);
        return counts;
    }

    private static void shade(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, W, H);
    }

    private static void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(PANEL);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(78, 47, 32));
        g.fillRect(x + 4, y + 4, w - 8, 3);
        g.setColor(EDGE);
        g.drawRect(x, y, w, h);
        g.drawRect(x + 3, y + 3, w - 6, h - 6);
    }
}

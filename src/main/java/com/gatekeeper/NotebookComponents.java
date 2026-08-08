package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;

final class NotebookComponents {
    private NotebookComponents() {}

    static void drawGateCard(Graphics2D g, GateType gate, int x, int y,
                             String note, String truth) {
        g.setColor(new Color(213, 201, 165, 185));
        g.fillRect(x, y, 166, 48);
        g.setColor(new Color(128, 104, 82));
        g.drawRect(x, y, 166, 48);
        g.setColor(new Color(52, 48, 44));
        CircuitComponents.drawGate(g, x + 5, y + 4, gate, false);
        GamePanel.pixelText(g, gate.label, x + 46, y + 17, 1);
        g.setColor(new Color(100, 67, 54));
        GamePanel.pixelText(g, note, x + 46, y + 31, 1);
        g.setColor(new Color(54, 51, 47));
        GamePanel.pixelText(g, truth, x + 7, y + 43, 1);
    }

    static void drawTruthTable(Graphics2D g, CircuitRecipe recipe, int x, int y) {
        g.setColor(new Color(212, 198, 160, 205));
        g.fillRect(x, y, 57, 108);
        g.setColor(new Color(125, 100, 77));
        g.drawRect(x, y, 57, 108);
        g.setColor(new Color(76, 55, 47));
        GamePanel.pixelText(g, "TARGET", x + 8, y + 13, 1);
        g.drawLine(x + 5, y + 18, x + 52, y + 18);
        GamePanel.pixelText(g, "A B | O", x + 7, y + 31, 1);
        for (int row = 0; row < 4; row++) {
            boolean a = row >= 2;
            boolean b = row % 2 == 1;
            g.setColor(recipe.truth[row] ? new Color(29, 110, 99) : new Color(76, 55, 47));
            GamePanel.pixelText(g, bit(a) + " " + bit(b) + " | " + bit(recipe.truth[row]),
                x + 7, y + 46 + row * 14, 1);
        }
        g.setColor(new Color(117, 82, 61));
        GamePanel.pixelText(g, "MATCH ALL", x + 3, y + 103, 1);
    }

    static void drawWiringPlan(Graphics2D g, CircuitRecipe recipe, int x, int y) {
        g.setColor(new Color(212, 198, 160, 205));
        g.fillRect(x, y, 101, 108);
        g.setColor(new Color(125, 100, 77));
        g.drawRect(x, y, 101, 108);
        g.setColor(new Color(76, 55, 47));
        GamePanel.pixelText(g, "WIRING PLAN", x + 7, y + 13, 1);
        g.drawLine(x + 5, y + 18, x + 96, y + 18);
        for (int i = 0; i < recipe.slotCount(); i++) {
            GateType gate = recipe.solution[i];
            String sources = sourceName(recipe.leftSources[i]);
            if (gate != GateType.NOT) {
                sources += "+" + sourceName(recipe.rightSources[i]);
            }
            g.setColor(i % 2 == 0 ? new Color(66, 58, 51) : new Color(94, 68, 55));
            GamePanel.pixelText(g, "G" + (i + 1) + " " + gate.label + " <- " + sources,
                x + 6, y + 34 + i * 14, 1);
        }
        g.setColor(new Color(29, 110, 99));
        GamePanel.pixelText(g, "LAST GATE -> OUT", x + 5, y + 103, 1);
    }

    private static int bit(boolean value) {
        return value ? 1 : 0;
    }

    private static String sourceName(int source) {
        if (source == CircuitRecipe.INPUT_A) return "A";
        if (source == CircuitRecipe.INPUT_B) return "B";
        return "G" + (source + 1);
    }
}

package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

final class NotebookComponents {
    private static final Color INK = new Color(62, 48, 36);
    private static final Color MUTED_INK = new Color(102, 76, 54);
    private static final Color ACCENT = new Color(126, 68, 37);
    private static final Color COMPLETE = new Color(37, 91, 66);
    private static final Color PANEL = new Color(216, 202, 168, 118);
    private static final Color RULE = new Color(116, 89, 63, 165);

    private NotebookComponents() {}

    static void drawGateInfo(Graphics2D g, GateType gate, int page, int pageCount,
                             Rectangle leftPage, Rectangle rightPage) {
        int leftCenter = leftPage.x + leftPage.width / 2;
        int rightCenter = rightPage.x + rightPage.width / 2;

        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "GATE GUIDE", leftCenter, leftPage.y + 24, 1);
        drawRule(g, leftPage.x + 13, leftPage.y + 31, leftPage.width - 26);

        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, gate.label, leftCenter, leftPage.y + 58, 2);
        drawGateSymbol(g, gate, leftCenter - 34, leftPage.y + 76, 68, 44);
        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "INPUTS  " + gate.inputPorts,
            leftCenter, leftPage.y + 145, 1);
        GamePanel.drawCenteredPixelText(g, "OUTPUTS " + gate.outputPorts,
            leftCenter, leftPage.y + 161, 1);

        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "HOW IT WORKS", rightCenter, rightPage.y + 24, 1);
        drawRule(g, rightPage.x + 13, rightPage.y + 31, rightPage.width - 26);
        g.setColor(INK);
        drawWrapped(g, gate.hint + ".", rightPage.x + 13, rightPage.y + 55, 18);
        g.setColor(ACCENT);
        drawWrapped(g, explanation(gate), rightPage.x + 13, rightPage.y + 103, 18);
        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "GATE " + (page + 1) + " OF " + pageCount,
            rightCenter, rightPage.y + 181, 1);
    }

    static void drawTruthTableSpread(Graphics2D g, CircuitRecipe recipe, boolean crafted,
                                     int page, int pageCount, Rectangle leftPage,
                                     Rectangle rightPage) {
        int leftCenter = leftPage.x + leftPage.width / 2;
        int rightCenter = rightPage.x + rightPage.width / 2;

        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "TRUTH TABLE", leftCenter, leftPage.y + 24, 1);
        drawRule(g, leftPage.x + 13, leftPage.y + 31, leftPage.width - 26);
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, recipe.name, leftCenter, leftPage.y + 55, 2);
        g.setColor(crafted ? COMPLETE : ACCENT);
        GamePanel.drawCenteredPixelText(g, crafted ? "[ COMPLETE ]" : "[ TO BUILD ]",
            leftCenter, leftPage.y + 73, 1);
        g.setColor(MUTED_INK);
        drawWrapped(g, recipe.subtitle, leftPage.x + 13, leftPage.y + 94, 18);
        drawTruthTable(g, recipe, leftPage.x + 20, leftPage.y + 132,
            leftPage.width - 40);

        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "BUILD PLAN", rightCenter, rightPage.y + 24, 1);
        drawRule(g, rightPage.x + 13, rightPage.y + 31, rightPage.width - 26);
        drawWiringPlan(g, recipe, rightPage.x + 11, rightPage.y + 48,
            rightPage.width - 22);
        g.setColor(MUTED_INK);
        GamePanel.drawCenteredPixelText(g, "TABLE " + (page + 1) + " OF " + pageCount,
            rightCenter, rightPage.y + 181, 1);
    }

    private static void drawTruthTable(Graphics2D g, CircuitRecipe recipe, int x, int y,
                                       int width) {
        int rowHeight = 15;
        int height = 20 + recipe.truth.length * rowHeight;
        g.setColor(PANEL);
        g.fillRect(x, y, width, height);
        g.setColor(RULE);
        g.drawRect(x, y, width, height);
        GamePanel.drawCenteredPixelText(g, "A   B   OUT", x + width / 2, y + 15, 1);
        g.drawLine(x + 6, y + 20, x + width - 6, y + 20);
        for (int row = 0; row < recipe.truth.length; row++) {
            boolean a = row >= 2;
            boolean b = row % 2 == 1;
            g.setColor(recipe.truth[row] ? COMPLETE : INK);
            GamePanel.drawCenteredPixelText(g,
                bit(a) + "   " + bit(b) + "     " + bit(recipe.truth[row]),
                x + width / 2, y + 34 + row * rowHeight, 1);
        }
    }

    private static void drawWiringPlan(Graphics2D g, CircuitRecipe recipe, int x, int y,
                                       int width) {
        int rowHeight = 19;
        int height = Math.min(139, 25 + recipe.slotCount() * rowHeight);
        g.setColor(PANEL);
        g.fillRect(x, y, width, height);
        g.setColor(RULE);
        g.drawRect(x, y, width, height);
        for (int index = 0; index < recipe.slotCount(); index++) {
            GateType gate = recipe.solution[index];
            String sources = sourceName(recipe.leftSources[index]);
            if (gate != GateType.NOT) {
                sources += "+" + sourceName(recipe.rightSources[index]);
            }
            g.setColor(index % 2 == 0 ? INK : MUTED_INK);
            GamePanel.pixelText(g, "G" + (index + 1) + " " + gate.label + " <- " + sources,
                x + 7, y + 17 + index * rowHeight, 1);
        }
        g.setColor(COMPLETE);
        GamePanel.drawCenteredPixelText(g, "LAST GATE -> OUT", x + width / 2,
            y + height - 7, 1);
    }

    private static void drawGateSymbol(Graphics2D g, GateType gate, int x, int y,
                                       int width, int height) {
        g.setColor(new Color(77, 57, 42, 55));
        g.fillRect(x + 3, y + 4, width, height);
        g.setColor(INK);
        if (gate == GateType.NOT) {
            int[] xs = {x + 10, x + 10, x + width - 13};
            int[] ys = {y + 3, y + height - 3, y + height / 2};
            g.drawPolygon(xs, ys, 3);
            g.drawOval(x + width - 12, y + height / 2 - 4, 8, 8);
            g.drawLine(x, y + height / 2, x + 10, y + height / 2);
            g.drawLine(x + width - 4, y + height / 2, x + width + 3, y + height / 2);
            return;
        }

        g.drawRect(x + 10, y + 3, width - 20, height - 6);
        g.drawLine(x, y + 13, x + 10, y + 13);
        g.drawLine(x, y + height - 13, x + 10, y + height - 13);
        g.drawLine(x + width - 10, y + height / 2, x + width + 3, y + height / 2);
        if (gate == GateType.OR) {
            g.drawLine(x + 10, y + 3, x + 22, y + height / 2);
            g.drawLine(x + 22, y + height / 2, x + 10, y + height - 3);
        }
        GamePanel.drawCenteredPixelText(g, gate == GateType.AND ? "&" : ">",
            x + width / 2, y + height / 2 + 5, 2);
    }

    private static String explanation(GateType gate) {
        return switch (gate) {
            case AND -> "The output is ON only when both inputs are ON.";
            case OR -> "The output is ON when either input is ON.";
            case NOT -> "The output is always the opposite of its input.";
        };
    }

    private static void drawRule(Graphics2D g, int x, int y, int width) {
        g.setColor(RULE);
        g.drawLine(x, y, x + width, y);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int columns) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : words) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > columns) {
                GamePanel.pixelText(g, line.toString(), x, lineY, 1);
                line.setLength(0);
                lineY += 14;
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) GamePanel.pixelText(g, line.toString(), x, lineY, 1);
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

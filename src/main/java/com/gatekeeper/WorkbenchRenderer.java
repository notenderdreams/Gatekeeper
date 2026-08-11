package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

final class WorkbenchRenderer {
    private final List<CircuitRecipe> recipes;
    private final boolean[] crafted;
    private final CircuitModel circuit;
    private final AutoTester autoTester;
    private final BufferedImage logicLensImage;
    private int chapter;
    private int selectedRecipe;
    private GateType heldGate;
    private int boardMessageTimer;
    private String boardMessage;
    private int mouseX;
    private int mouseY;

    WorkbenchRenderer(List<CircuitRecipe> recipes, boolean[] crafted, CircuitModel circuit,
                      AutoTester autoTester, BufferedImage logicLensImage) {
        this.recipes = recipes;
        this.crafted = crafted;
        this.circuit = circuit;
        this.autoTester = autoTester;
        this.logicLensImage = logicLensImage;
    }

    void draw(Graphics2D g, int chapter, int selectedRecipe, GateType heldGate,
              int boardMessageTimer, String boardMessage, int mouseX, int mouseY) {
        this.chapter = chapter;
        this.selectedRecipe = selectedRecipe;
        this.heldGate = heldGate;
        this.boardMessageTimer = boardMessageTimer;
        this.boardMessage = boardMessage;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // The workbench sits on Alex's scarred wooden desk.
        g.setColor(new Color(22, 13, 12));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(53, 30, 22));
        for (int y = 5; y < H; y += 13) g.drawLine(0, y, W, y + 3);
        g.setColor(new Color(3, 5, 7, 150));
        g.fillRect(8, 9, 469, 259);
        g.setColor(new Color(91, 55, 34));
        g.fillRect(3, 3, 472, 262);
        g.setColor(new Color(190, 126, 60));
        g.drawRect(3, 3, 471, 261);
        g.setColor(new Color(48, 29, 24));
        g.drawRect(7, 7, 463, 253);
        g.setColor(new Color(7, 27, 28));
        g.fillRect(10, 10, 458, 248);
        g.setColor(new Color(18, 59, 54));
        for (int x = 14; x < 468; x += 15) {
            for (int y = 14; y < 259; y += 15) g.fillRect(x, y, 1, 1);
        }
        // Etched copper traces around the board's edge.
        g.setColor(new Color(116, 72, 37));
        g.drawLine(13, 53, 13, 255);
        g.drawLine(13, 255, 294, 255);
        g.drawLine(467, 53, 467, 255);
        g.setColor(new Color(201, 139, 65));
        for (int[] screw : new int[][]{{8, 8}, {466, 8}, {8, 256}, {466, 256}}) {
            g.fillRect(screw[0], screw[1], 5, 5);
            g.setColor(new Color(61, 39, 32));
            g.drawLine(screw[0] + 1, screw[1] + 2, screw[0] + 3, screw[1] + 2);
            g.setColor(new Color(201, 139, 65));
        }

        g.setColor(new Color(5, 12, 15));
        g.fillRect(12, 12, 456, 20);
        g.setColor(new Color(21, 48, 46));
        g.drawLine(13, 32, 467, 32);
        g.setColor(INK);
        GamePanel.pixelText(g, "LOGIC WORKBENCH", 18, 26, 1);

        boolean exitHovered = isHovered(420, 15, 44, 14);
        g.setColor(exitHovered ? RED : DIM);
        GamePanel.pixelText(g, "EXIT", 430, 26, 1);

        int available = chapter >= 3 ? 5 : 3;
        for (int i = 0; i < available; i++) {
            int x = 126 + i * 52;
            boolean selected = i == selectedRecipe;
            boolean hovered = isHovered(x, 15, 46, 14);
            g.setColor(selected ? new Color(83, 67, 25)
                : hovered ? new Color(28, 63, 58) : new Color(17, 31, 34));
            g.fillRect(x, 15, 46, 14);
            g.setColor(crafted[i] ? CYAN : selected ? YELLOW : hovered ? INK : DIM);
            g.drawRect(x, 15, 46, 14);
            GamePanel.pixelText(g, (crafted[i] ? "*" : " ") + recipes.get(i).name, x + 3, 26, 1);
        }

        CircuitRecipe recipe = circuit.recipe();
        drawPanel(g, 13, 36, 345, 186);
        g.setColor(new Color(20, 57, 51));
        for (int x = 22; x < 350; x += 12) {
            for (int y = 58; y < 216; y += 12) g.fillRect(x, y, 2, 2);
        }
        g.setColor(INK);
        GamePanel.pixelText(g, recipe.name, 20, 52, 1);
        g.setColor(DIM);
        GamePanel.pixelText(g, recipe.subtitle, 62, 52, 1);
        g.setColor(crafted[selectedRecipe] ? CYAN : YELLOW);
        GamePanel.pixelText(g, crafted[selectedRecipe] ? "BUILT" : "ACTIVE", 305, 52, 1);
        drawSwitch(g, 24, 90, "A", circuit.inputA(), autoTester.isAttached());
        drawSwitch(g, 24, 138, "B", circuit.inputB(), autoTester.isAttached());

        int[][] layout = socketLayout(recipe);
        boolean[] nodeValues = circuit.nodeValues();
        drawCircuitWires(g, recipe, layout, nodeValues);
        for (int i = 0; i < recipe.slotCount(); i++) {
            drawSocket(g, layout[i][0], layout[i][1], i, circuit.placed()[i], nodeValues[i]);
        }
        int[] last = layout[recipe.slotCount() - 1];
        drawRoutedWire(g, last[0] + BOARD_SOCKET_W, last[1] + BOARD_SOCKET_H / 2,
            340, 122, circuit.output());
        g.setColor(autoTester.isAttached() ? new Color(39, 76, 51)
            : circuit.output() ? new Color(32, 100, 99) : new Color(25, 32, 34));
        g.fillOval(338, 114, 16, 16);
        g.setColor(autoTester.isAttached() ? new Color(143, 190, 128)
            : circuit.output() ? CYAN : DIM);
        g.drawOval(338, 114, 15, 15);
        g.fillOval(343, 119, 6, 6);
        g.setColor(autoTester.isAttached() ? new Color(143, 190, 128) : INK);
        GamePanel.pixelText(g, "OUT", 338, 140, 1);

        drawTruthTable(g);
        drawGatePalette(g);
        drawBoardButtons(g);
    }

    private void drawTruthTable(Graphics2D g) {
        CircuitRecipe r = circuit.recipe();
        int x = 366, y = 36;
        drawPanel(g, 362, 36, 105, 186);
        g.setColor(INK);
        GamePanel.pixelText(g, "TARGET / LIVE", x + 2, y + 15, 1);
        g.setColor(DIM);
        GamePanel.pixelText(g, "A B | WANT GOT", x + 5, y + 30, 1);
        g.drawLine(x + 5, y + 35, x + 92, y + 35);
        int currentRow = (circuit.inputA() ? 2 : 0) + (circuit.inputB() ? 1 : 0);
        for (int row = 0; row < 4; row++) {
            int yy = y + 54 + row * 18;
            boolean a = row >= 2;
            boolean b = row % 2 == 1;
            Boolean seen = circuit.observations()[row];
            String value = seen == null ? "?" : bit(seen);
            if (row == currentRow) {
                g.setColor(new Color(27, 64, 59));
                g.fillRect(x + 4, yy - 12, 92, 16);
            }
            g.setColor(seen == null ? DIM : (seen == r.truth[row] ? CYAN : RED));
            GamePanel.pixelText(g, bit(a) + " " + bit(b) + " |  " + bit(r.truth[row]) + "    " + value, x + 9, yy, 1);
        }
        int recorded = 0;
        for (Boolean observation : circuit.observations()) if (observation != null) recorded++;
        g.setColor(recorded == 4 ? CYAN : DIM);
        GamePanel.pixelText(g, "REC " + recorded + "/4", x + 52, y + 172, 1);
    }

    private void drawGatePalette(Graphics2D g) {
        drawPanel(g, 13, 226, 252, 30);
        GateType[] gates = GateType.values();
        for (int i = 0; i < gates.length; i++) {
            int x = 18 + i * 80;
            boolean selected = heldGate == gates[i];
            boolean hovered = isHovered(x, 229, 74, 24);
            g.setColor(selected ? new Color(76, 63, 25)
                : hovered ? new Color(28, 61, 56) : new Color(14, 27, 30));
            g.fillRect(x, 229, 74, 24);
            g.setColor(selected ? YELLOW : hovered ? INK : DIM);
            g.drawRect(x, 229, 74, 24);
            if (selected) g.fillRect(x + 2, 231, 2, 20);
            CircuitComponents.drawGate(g, x + 5, 232, gates[i], false);
            GamePanel.pixelText(g, (i + 1) + " " + gates[i].label, x + 32, 245, 1);
        }
    }

    private void drawBoardButtons(Graphics2D g) {
        drawPanel(g, 273, 226, 194, 30);
        if (chapter < 3) {
            boolean lockHover = isHovered(277, 229, 60, 24);
            g.setColor(lockHover ? new Color(25, 28, 30) : new Color(16, 19, 21));
            g.fillRect(277, 229, 60, 24);
            g.setColor(new Color(60, 68, 70));
            g.drawRect(277, 229, 60, 24);
            g.setColor(new Color(110, 118, 120));
            GamePanel.pixelText(g, "H LOCKED", 281, 244, 1);
        } else {
            boolean attached = autoTester.isAttached();
            boolean hookHover = isHovered(277, 229, 60, 24);
            g.setColor(hookHover ? new Color(28, 73, 62) : attached ? new Color(20, 50, 42) : new Color(16, 43, 41));
            g.fillRect(277, 229, 60, 24);
            g.setColor(attached ? new Color(143, 190, 128) : new Color(105, 116, 117));
            g.drawRect(277, 229, 60, 24);
            GamePanel.pixelText(g, attached ? "H UNHOOK" : "H HOOK", 280, 244, 1);
        }

        boolean testerRunning = chapter >= 3 && autoTester.isAttached();
        boolean recordHover = !testerRunning && isHovered(342, 229, 58, 24);
        g.setColor(testerRunning ? new Color(22, 26, 28)
            : recordHover ? new Color(102, 78, 24) : new Color(59, 48, 23));
        g.fillRect(342, 229, 58, 24);
        g.setColor(testerRunning ? DIM : YELLOW);
        g.drawRect(342, 229, 58, 24);
        GamePanel.pixelText(g, "R REC", 348, 244, 1);

        boolean verifyHover = isHovered(405, 229, 58, 24);
        g.setColor(verifyHover ? new Color(23, 85, 83) : new Color(18, 52, 54));
        g.fillRect(405, 229, 58, 24);
        g.setColor(CYAN);
        g.drawRect(405, 229, 58, 24);
        GamePanel.pixelText(g, (chapter >= 3 && autoTester.isAttached()) ? "T RUN" : "T VERIFY", 410, 244, 1);
    }

    private void drawPanel(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(1, 5, 7, 125));
        g.fillRect(x + 3, y + 3, width, height);
        g.setColor(new Color(5, 13, 16, 235));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(52, 103, 88));
        g.drawRect(x, y, width, height);
        g.setColor(new Color(14, 39, 38));
        g.drawRect(x + 2, y + 2, width - 4, height - 4);
        g.setColor(new Color(184, 119, 52));
        g.fillRect(x + 5, y + 5, 3, 3);
        g.fillRect(x + width - 7, y + 5, 3, 3);
    }

    private void drawSocket(Graphics2D g, int x, int y, int number, GateType gate, boolean powered) {
        boolean hovered = isHovered(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(new Color(1, 4, 5, 155));
        g.fillRect(x + 2, y + 2, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(powered ? new Color(17, 70, 67)
            : hovered ? new Color(42, 55, 51) : new Color(12, 23, 26));
        g.fillRect(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(hovered ? YELLOW : gate == null ? DIM : powered ? CYAN : INK);
        g.drawRect(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(hovered ? YELLOW : DIM);
        GamePanel.pixelText(g, "G" + (number + 1), x + 2, y + 8, 1);
        g.fillRect(x - 2, y + 6, 3, 3);
        g.fillRect(x - 2, y + 17, 3, 3);
        g.fillRect(x + BOARD_SOCKET_W, y + 11, 3, 3);
        if (gate == null) {
            g.setColor(hovered ? YELLOW : DIM);
            GamePanel.pixelText(g, "+", x + 15, y + 18, 1);
        } else {
            g.setColor(powered ? CYAN : INK);
            CircuitComponents.drawGate(g, x + 2, y + 3, gate, powered);
        }
    }

    private void drawSwitch(Graphics2D g, int x, int y, String name, boolean on, boolean hooked) {
        boolean hovered = isHovered(x, y, 52, 16);
        g.setColor(hovered ? new Color(36, 61, 56) : new Color(12, 25, 28));
        g.fillRect(x - 2, y - 1, 52, 16);
        g.setColor(hooked ? new Color(143, 190, 128) : hovered ? YELLOW : INK);
        GamePanel.pixelText(g, name, x, y + 11, 1);
        g.drawRect(x + 12, y, 20, 14);
        g.setColor(hooked ? new Color(143, 190, 128) : on ? CYAN : DIM);
        g.fillRect(on ? x + 23 : x + 14, y + 3, 6, 8);
        g.setColor(hooked ? new Color(143, 190, 128) : on ? CYAN : hovered ? YELLOW : DIM);
        GamePanel.pixelText(g, on ? "1" : "0", x + 36, y + 11, 1);
    }

    private void drawWire(Graphics2D g, int x1, int y1, int x2, int y2, boolean on) {
        g.setColor(new Color(1, 5, 6, 190));
        g.setStroke(new BasicStroke(4));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1));
    }

    private void drawSourceWire(Graphics2D g, int source, int targetX, int targetY,
                                int[][] layout, boolean[] nodeValues) {
        int sourceX;
        int sourceY;
        boolean powered;
        if (source == CircuitRecipe.INPUT_A) {
            sourceX = 57;
            sourceY = 97;
            powered = circuit.inputA();
        } else if (source == CircuitRecipe.INPUT_B) {
            sourceX = 57;
            sourceY = 145;
            powered = circuit.inputB();
        } else {
            sourceX = layout[source][0] + BOARD_SOCKET_W;
            sourceY = layout[source][1] + 11;
            powered = nodeValues[source];
        }
        drawRoutedWire(g, sourceX, sourceY, targetX, targetY, powered);
    }

    private void drawCircuitWires(Graphics2D g, CircuitRecipe recipe,
                                  int[][] layout, boolean[] nodeValues) {
        if ("XOR".equals(recipe.name)) {
            drawXorWires(g, layout, nodeValues);
            return;
        }
        for (int i = 0; i < recipe.slotCount(); i++) {
            int x = layout[i][0] - 2;
            int y = layout[i][1];
            drawSourceWire(g, recipe.leftSources[i], x, y + 6, layout, nodeValues);
            if (recipe.solution[i] != GateType.NOT) {
                drawSourceWire(g, recipe.rightSources[i], x, y + 17, layout, nodeValues);
            }
        }
    }

    private void drawXorWires(Graphics2D g, int[][] layout, boolean[] values) {
        int aX = 57, aY = 97;
        int bX = 57, bY = 145;

        // G3 = NOT A and G1 = NOT B: short, direct branch starters.
        drawWirePath(g, circuit.inputA(), aX, aY, 79, aY, 79, layout[2][1] + 6,
            layout[2][0] - 2, layout[2][1] + 6);
        drawWirePath(g, circuit.inputB(), bX, bY, 79, bY, 79, layout[0][1] + 6,
            layout[0][0] - 2, layout[0][1] + 6);

        // The un-inverted inputs take clearly separated outer lanes to the
        // opposite AND gates instead of disappearing behind other modules.
        drawWirePath(g, circuit.inputA(), aX, aY, 69, aY, 69, 175, 175, 175,
            175, layout[1][1] + 6, layout[1][0] - 2, layout[1][1] + 6);
        drawWirePath(g, circuit.inputB(), bX, bY, 64, bY, 64, 75, 175, 75,
            175, layout[3][1] + 17, layout[3][0] - 2, layout[3][1] + 17);

        // Each NOT feeds only its neighboring AND.
        drawWirePath(g, values[2], layout[2][0] + BOARD_SOCKET_W,
            layout[2][1] + 11, 158, layout[2][1] + 11,
            158, layout[3][1] + 6, layout[3][0] - 2, layout[3][1] + 6);
        drawWirePath(g, values[0], layout[0][0] + BOARD_SOCKET_W,
            layout[0][1] + 11, 158, layout[0][1] + 11,
            158, layout[1][1] + 17, layout[1][0] - 2, layout[1][1] + 17);

        // The two product terms remain separate until the final OR.
        drawWirePath(g, values[3], layout[3][0] + BOARD_SOCKET_W,
            layout[3][1] + 11, 246, layout[3][1] + 11,
            246, layout[4][1] + 6, layout[4][0] - 2, layout[4][1] + 6);
        drawWirePath(g, values[1], layout[1][0] + BOARD_SOCKET_W,
            layout[1][1] + 11, 252, layout[1][1] + 11,
            252, layout[4][1] + 17, layout[4][0] - 2, layout[4][1] + 17);

        // Break the two visual crossings so they cannot be mistaken for
        // junctions, then annotate both product terms directly on the board.
        drawHorizontalCrossover(g, 64, aY, circuit.inputA());
        drawHorizontalCrossover(g, 69, bY, circuit.inputB());
        g.setColor(new Color(5, 13, 16, 235));
        g.fillRect(222, 83, 47, 12);
        g.fillRect(222, 158, 47, 12);
        g.setColor(new Color(127, 205, 194));
        GamePanel.pixelText(g, "!A & B", 225, 93, 1);
        GamePanel.pixelText(g, "A & !B", 225, 168, 1);
    }

    private void drawHorizontalCrossover(Graphics2D g, int x, int y, boolean on) {
        g.setColor(new Color(5, 13, 16));
        g.fillRect(x - 4, y - 4, 9, 9);
        drawWire(g, x - 5, y, x + 5, y, on);
    }

    private void drawWirePath(Graphics2D g, boolean on, int... points) {
        g.setColor(new Color(1, 5, 6, 195));
        g.setStroke(new BasicStroke(4));
        drawPathSegments(g, points);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        drawPathSegments(g, points);
        g.setStroke(new BasicStroke(1));
        for (int i = 2; i < points.length - 2; i += 2) {
            g.fillRect(points[i] - 2, points[i + 1] - 2, 4, 4);
        }
        int end = points.length - 2;
        g.fillRect(points[end] - 2, points[end + 1] - 2, 4, 4);
    }

    private static void drawPathSegments(Graphics2D g, int[] points) {
        for (int i = 0; i < points.length - 2; i += 2) {
            g.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3]);
        }
    }

    private void drawRoutedWire(Graphics2D g, int x1, int y1, int x2, int y2, boolean on) {
        int bendX = x1 + Math.max(7, (x2 - x1) / 2);
        g.setColor(new Color(1, 5, 6, 195));
        g.setStroke(new BasicStroke(4));
        g.drawLine(x1, y1, bendX, y1);
        g.drawLine(bendX, y1, bendX, y2);
        g.drawLine(bendX, y2, x2, y2);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        g.drawLine(x1, y1, bendX, y1);
        g.drawLine(bendX, y1, bendX, y2);
        g.drawLine(bendX, y2, x2, y2);
        g.setStroke(new BasicStroke(1));
        g.fillRect(bendX - 2, y1 - 2, 4, 4);
        g.fillRect(x2 - 2, y2 - 2, 4, 4);
        if (on) {
            g.setColor(new Color(196, 255, 247));
            g.fillRect(bendX, y1, 1, 1);
        }
    }

    static int[][] socketLayout(CircuitRecipe recipe) {
        return switch (recipe.name) {
            case "XOR" -> new int[][]{{100, 138}, {180, 138}, {100, 84}, {180, 84}, {272, 111}};
            case "XNOR" -> new int[][]{{92, 84}, {92, 138}, {158, 138}, {224, 111}, {286, 111}};
            case "IMPLY" -> new int[][]{{125, 92}, {235, 111}};
            default -> new int[][]{{125, 111}, {235, 111}};
        };
    }

    private boolean isHovered(int x, int y, int width, int height) {
        return GamePanel.inside(mouseX, mouseY, x, y, width, height);
    }

    private static String bit(boolean value) {
        return value ? "1" : "0";
    }
}

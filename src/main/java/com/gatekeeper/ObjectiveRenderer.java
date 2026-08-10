package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;

import static com.gatekeeper.GameConstants.*;

/** Draws only the player's current story objective while exploring. */
final class ObjectiveRenderer {
    private ObjectiveRenderer() {}

    static void draw(Graphics2D g, int chapter, boolean boxRetrieved, boolean boxOpened,
                     boolean workbenchInstalled, boolean[] crafted,
                     String completedObjective) {
        boolean complete = completedObjective != null;
        if (!complete && chapter == 2 && (!crafted[0] || !crafted[1] || !crafted[2])) {
            drawBuildObjectives(g, "BUILD MIRA'S CIRCUITS", new String[]{"NAND", "NOR", "XOR"},
                new boolean[]{crafted[0], crafted[1], crafted[2]});
            return;
        }
        if (!complete && chapter == 3 && (!crafted[3] || !crafted[4])) {
            drawBuildObjectives(g, "BUILD MIRA'S CIRCUITS", new String[]{"XNOR", "IMPLY"},
                new boolean[]{crafted[3], crafted[4]});
            return;
        }
        String objective = complete ? completedObjective : currentObjective(chapter,
            boxRetrieved, boxOpened, workbenchInstalled, crafted);
        if (objective == null) return;

        int x = 9;
        int baselineY = 38;
        drawCheckboxLine(g, objective, x, baselineY, complete);
    }

    private static void drawBuildObjectives(Graphics2D g, String heading, String[] labels,
                                            boolean[] completed) {
        int x = 9;
        int baselineY = 38;
        g.setColor(new Color(0, 0, 0, 190));
        GamePanel.pixelText(g, heading, x + 1, baselineY + 1, 1);
        g.setColor(YELLOW);
        GamePanel.pixelText(g, heading, x, baselineY, 1);
        for (int i = 0; i < labels.length; i++) {
            drawCheckboxLine(g, labels[i], x + 8, baselineY + 15 + i * 14, completed[i]);
        }
    }

    private static void drawCheckboxLine(Graphics2D g, String label, int x, int baselineY,
                                         boolean complete) {
        g.setColor(new Color(0, 0, 0, 190));
        GamePanel.pixelText(g, label, x + 15, baselineY + 1, 1);
        g.setColor(complete ? CYAN : INK);
        g.drawRect(x, baselineY - 8, 7, 7);
        if (complete) {
            g.setColor(CYAN);
            g.drawLine(x + 2, baselineY - 5, x + 3, baselineY - 3);
            g.drawLine(x + 3, baselineY - 3, x + 6, baselineY - 7);
        }
        g.setColor(complete ? CYAN : INK);
        GamePanel.pixelText(g, label, x + 14, baselineY, 1);
    }

    private static String currentObjective(int chapter, boolean boxRetrieved,
                                           boolean boxOpened, boolean workbenchInstalled,
                                           boolean[] crafted) {
        if (chapter < 2) {
            if (!boxRetrieved) return "FIND THE REASON FOR THE KNOCK";
            if (!boxOpened) return "OPEN THE MYSTERY BOX";
            if (!workbenchInstalled) return "INSTALL THE WORKBENCH";
            return "ASK MIRA ABOUT THE GATES";
        }
        if (chapter == 2) {
            return "RETURN TO MIRA";
        }
        if (chapter == 3) {
            return "RETURN TO MIRA";
        }
        return null;
    }
}

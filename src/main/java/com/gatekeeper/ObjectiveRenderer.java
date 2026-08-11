package com.gatekeeper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.Objects;

import static com.gatekeeper.GameConstants.*;

/** Draws only the player's current story objective while exploring with fade-in and fade-out animations. */
final class ObjectiveRenderer {
    private static String currentKey = null;
    private static String fadingOutKey = null;
    private static float alpha = 0.0f;
    private static final float FADE_SPEED = 0.08f;

    private ObjectiveRenderer() {}

    static void reset() {
        currentKey = null;
        fadingOutKey = null;
        alpha = 0.0f;
    }

    static void draw(Graphics2D g, int chapter, boolean boxRetrieved, boolean boxOpened,
                     boolean workbenchInstalled, boolean[] crafted,
                     String completedObjective, boolean taskbarOnRight) {
        String targetKey = buildKey(chapter, boxRetrieved, boxOpened, workbenchInstalled, crafted, completedObjective);

        if (fadingOutKey != null) {
            alpha -= FADE_SPEED;
            if (alpha <= 0.0f) {
                alpha = 0.0f;
                fadingOutKey = null;
                currentKey = targetKey;
            }
        } else if (!Objects.equals(targetKey, currentKey)) {
            if (currentKey != null) {
                fadingOutKey = currentKey;
            } else {
                currentKey = targetKey;
                alpha = 0.0f;
            }
        } else {
            if (currentKey != null && alpha < 1.0f) {
                alpha += FADE_SPEED;
                if (alpha > 1.0f) alpha = 1.0f;
            }
        }

        String activeRenderKey = fadingOutKey != null ? fadingOutKey : currentKey;
        if (activeRenderKey == null || alpha <= 0.001f) return;

        Composite originalComposite = g.getComposite();
        float renderAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, renderAlpha));

        renderKey(g, activeRenderKey, taskbarOnRight);

        g.setComposite(originalComposite);
    }

    private static String buildKey(int chapter, boolean boxRetrieved, boolean boxOpened,
                                  boolean workbenchInstalled, boolean[] crafted,
                                  String completedObjective) {
        if (completedObjective != null) {
            return "COMPLETE:" + completedObjective;
        }
        if (chapter == 2 && (!crafted[0] || !crafted[1] || !crafted[2])) {
            return "BUILD_CH2:" + crafted[0] + "_" + crafted[1] + "_" + crafted[2];
        }
        if (chapter == 3 && (!crafted[3] || !crafted[4])) {
            return "BUILD_CH3:" + crafted[3] + "_" + crafted[4];
        }
        String obj = currentObjective(chapter, boxRetrieved, boxOpened, workbenchInstalled, crafted);
        return obj != null ? "ACTIVE:" + obj : null;
    }

    private static void renderKey(Graphics2D g, String key, boolean taskbarOnRight) {
        if (key.startsWith("COMPLETE:")) {
            String label = key.substring(9);
            int labelWidth = GamePanel.pixelTextWidth(g, label, 1);
            int totalWidth = 14 + labelWidth;
            int x = taskbarOnRight ? (W - totalWidth - 9) : 9;
            drawCheckboxLine(g, label, x, 38, true);
        } else if (key.startsWith("ACTIVE:")) {
            String label = key.substring(7);
            int labelWidth = GamePanel.pixelTextWidth(g, label, 1);
            int totalWidth = 14 + labelWidth;
            int x = taskbarOnRight ? (W - totalWidth - 9) : 9;
            drawCheckboxLine(g, label, x, 38, false);
        } else if (key.startsWith("BUILD_CH2:")) {
            String[] parts = key.substring(10).split("_");
            boolean[] c = new boolean[]{
                Boolean.parseBoolean(parts[0]),
                Boolean.parseBoolean(parts[1]),
                Boolean.parseBoolean(parts[2])
            };
            drawBuildObjectives(g, "BUILD MIRA'S CIRCUITS", new String[]{"NAND", "NOR", "XOR"}, c, taskbarOnRight);
        } else if (key.startsWith("BUILD_CH3:")) {
            String[] parts = key.substring(10).split("_");
            boolean[] c = new boolean[]{
                Boolean.parseBoolean(parts[0]),
                Boolean.parseBoolean(parts[1])
            };
            drawBuildObjectives(g, "BUILD MIRA'S CIRCUITS", new String[]{"XNOR", "IMPLY"}, c, taskbarOnRight);
        }
    }

    private static void drawBuildObjectives(Graphics2D g, String heading, String[] labels,
                                            boolean[] completed, boolean taskbarOnRight) {
        int maxWidth = GamePanel.pixelTextWidth(g, heading, 1);
        for (String label : labels) {
            int width = 22 + GamePanel.pixelTextWidth(g, label, 1);
            if (width > maxWidth) maxWidth = width;
        }
        int x = taskbarOnRight ? (W - maxWidth - 9) : 9;
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

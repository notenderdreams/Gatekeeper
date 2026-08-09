package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

final class NotebookRenderer {
    private static final int FLIP_DURATION_TICKS = 24;
    private final List<CircuitRecipe> recipes;
    private final boolean[] crafted;
    private final BufferedImage coverImage;
    private final BufferedImage leftPageImage;
    private final BufferedImage rightPageImage;
    private int renderedPage = -1;
    private int flipDirection;
    private long flipStartedAt = Long.MIN_VALUE;

    NotebookRenderer(List<CircuitRecipe> recipes, boolean[] crafted,
                     BufferedImage coverImage, BufferedImage leftPageImage,
                     BufferedImage rightPageImage) {
        this.recipes = recipes;
        this.crafted = crafted;
        this.coverImage = coverImage;
        this.leftPageImage = leftPageImage;
        this.rightPageImage = rightPageImage;
    }

    int drawNotebook(Graphics2D g, int chapter, int notebookPage, long ticks) {
        // The Travel Book assets provide the cover and both open pages.
        g.setColor(new Color(25, 15, 14));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(57, 32, 23));
        for (int y = 6; y < H; y += 14) g.drawLine(0, y, W, y + 4);
        g.setColor(new Color(3, 4, 6, 145));
        g.fillRect(31, 20, 425, 238);
        drawBookAssets(g);

        g.setColor(new Color(52, 44, 40));
        GamePanel.pixelText(g, "ALEX'S LOGIC NOTES", 67, 35, 1);
        g.setColor(new Color(110, 71, 57));
        GamePanel.pixelText(g, "THE THREE BUILDING BLOCKS", 67, 47, 1);
        NotebookComponents.drawGateCard(g, GateType.AND, 61, 55,
            "BOTH must be 1", "00:0  01:0  10:0  11:1");
        NotebookComponents.drawGateCard(g, GateType.OR, 61, 111,
            "EITHER can be 1", "00:0  01:1  10:1  11:1");
        NotebookComponents.drawGateCard(g, GateType.NOT, 61, 167,
            "FLIPS the signal", "0 -> 1       1 -> 0");

        int available = chapter >= 3 ? 5 : 3;
        notebookPage = clamp(notebookPage, 0, available - 1);
        beginPageFlip(notebookPage, ticks);
        CircuitRecipe recipe = recipes.get(notebookPage);
        g.setColor(new Color(110, 71, 57));
        GamePanel.pixelText(g, "PROJECT " + (notebookPage + 1) + " / " + available, 270, 34, 1);
        g.setColor(new Color(43, 39, 37));
        GamePanel.pixelText(g, recipe.name, 270, 51, 1);
        g.setColor(crafted[notebookPage] ? new Color(30, 116, 104) : new Color(159, 91, 48));
        GamePanel.pixelText(g, crafted[notebookPage] ? "[ COMPLETE ]" : "[ TO BUILD ]", 360, 51, 1);
        g.setColor(new Color(91, 72, 60));
        GamePanel.drawWrapped(g, recipe.subtitle, 270, 65, 27);

        NotebookComponents.drawTruthTable(g, recipe, 270, 91);
        NotebookComponents.drawWiringPlan(g, recipe, 337, 91);

        drawPageFlip(g, ticks);

        g.setColor(new Color(79, 59, 51));
        g.drawRect(270, 211, 22, 19);
        g.drawRect(416, 211, 22, 19);
        GamePanel.pixelText(g, "<", 278, 225, 1);
        GamePanel.pixelText(g, ">", 424, 225, 1);
        for (int i = 0; i < available; i++) {
            int x = 316 + i * 16;
            g.setColor(i == notebookPage ? new Color(153, 80, 50) : new Color(124, 106, 85));
            if (crafted[i]) g.fillRect(x - 2, 216, 11, 11);
            else g.drawRect(x - 2, 216, 11, 11);
            g.setColor(i == notebookPage ? new Color(245, 232, 197) : new Color(66, 56, 50));
            GamePanel.pixelText(g, Integer.toString(i + 1), x, 225, 1);
        }
        g.setColor(new Color(83, 67, 58));
        GamePanel.pixelText(g, "ARROWS: PAGE   N / ESC: CLOSE", 270, 241, 1);
        return notebookPage;
    }

    private void beginPageFlip(int notebookPage, long ticks) {
        if (renderedPage < 0) {
            renderedPage = notebookPage;
            return;
        }
        if (notebookPage == renderedPage) return;
        flipDirection = Integer.compare(notebookPage, renderedPage);
        flipStartedAt = ticks;
        renderedPage = notebookPage;
    }

    private void drawPageFlip(Graphics2D g, long ticks) {
        long elapsed = ticks - flipStartedAt;
        if (elapsed < 0 || elapsed >= FLIP_DURATION_TICKS) return;
        float progress = elapsed / (float) FLIP_DURATION_TICKS;
        if (flipDirection > 0) {
            if (progress < 0.5f) {
                int width = Math.max(1, Math.round(204 * (1.0f - progress * 2.0f)));
                drawTurningPage(g, rightPageImage, 240, true, width);
            } else {
                int width = Math.max(1, Math.round(204 * ((progress - 0.5f) * 2.0f)));
                drawTurningPage(g, leftPageImage, 240, false, width);
            }
        } else if (progress < 0.5f) {
            int width = Math.max(1, Math.round(204 * (1.0f - progress * 2.0f)));
            drawTurningPage(g, leftPageImage, 240, false, width);
        } else {
            int width = Math.max(1, Math.round(204 * ((progress - 0.5f) * 2.0f)));
            drawTurningPage(g, rightPageImage, 240, true, width);
        }
    }

    private void drawTurningPage(Graphics2D g, BufferedImage page, int spineX,
                                 boolean opensRight, int width) {
        if (page == null) return;
        int x = opensRight ? spineX : spineX - width;
        g.drawImage(page, x, 18, width, 228, null);
        g.setColor(new Color(76, 39, 31, 90));
        g.drawLine(spineX, 20, spineX, 244);
    }

    private void drawBookAssets(Graphics2D g) {
        if (coverImage != null) {
            g.drawImage(coverImage, 25, 13, 430, 239, null);
        } else {
            g.setColor(new Color(76, 39, 31));
            g.fillRect(25, 13, 430, 239);
        }
        if (leftPageImage != null) {
            g.drawImage(leftPageImage, 32, 18, 204, 228, null);
        } else {
            g.setColor(new Color(233, 222, 186));
            g.fillRect(32, 18, 204, 228);
        }
        if (rightPageImage != null) {
            g.drawImage(rightPageImage, 244, 18, 204, 228, null);
        } else {
            g.setColor(new Color(224, 211, 175));
            g.fillRect(244, 18, 204, 228);
        }
    }

    void drawEnding(Graphics2D g) {
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, "THE SIGNAL IS CLEAR.", W / 2, 78, 2);
        g.setColor(CYAN);
        GamePanel.drawCenteredPixelText(g, "Mira pins Alex's circuits above the counter.", W / 2, 119, 1);
        GamePanel.drawCenteredPixelText(g, "Tomorrow, the notebook has harder pages.", W / 2, 136, 1);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "But tonight, every little light is on.", W / 2, 169, 1);
        g.setColor(RED);
        drawHeart(g, (W - 12) / 2, 194);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g, "ENTER: begin again", W / 2, 238, 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawHeart(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 3, 3);
        g.fillRect(x + 5, y, 3, 3);
        g.fillRect(x - 2, y + 3, 12, 5);
        g.fillRect(x, y + 8, 8, 3);
        g.fillRect(x + 2, y + 11, 4, 3);
    }
}

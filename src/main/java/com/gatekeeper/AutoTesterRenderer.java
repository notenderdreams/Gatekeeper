package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/** Draws the asset-backed LogicLens overlay and owns its annotated UI geometry. */
final class AutoTesterRenderer {
    enum Action { NONE, RUN, STOP, CLEAR, PREVIOUS, NEXT }

    private static final int SOURCE_SIZE = 1254;
    private static final int FRAME_SIZE = 260;
    private static final int FRAME_X = (GameConstants.W - FRAME_SIZE) / 2;
    private static final int FRAME_Y = (GameConstants.H - FRAME_SIZE) / 2;
    private static final Rectangle[][] TESTER_BUTTON_SOURCES = {
        { new Rectangle(0, 0, 501, 509), new Rectangle(528, 0, 496, 509) },
        { new Rectangle(0, 542, 501, 479), new Rectangle(528, 542, 496, 479) },
        { new Rectangle(0, 1057, 501, 479), new Rectangle(528, 1057, 496, 479) }
    };
    private static final Rectangle NAV_LEFT_IDLE = new Rectangle(247, 0, 216, 264);
    private static final Rectangle NAV_LEFT_PRESSED = new Rectangle(247, 308, 216, 262);
    private static final Rectangle NAV_RIGHT_IDLE = new Rectangle(715, 0, 216, 264);
    private static final Rectangle NAV_RIGHT_PRESSED = new Rectangle(715, 308, 216, 262);

    // Regions from tester.annotations.json, expressed in the source image space.
    private static final Rectangle SOURCE_SCREEN = new Rectangle(95, 199, 857, 800);
    private static final Rectangle SOURCE_SIDEBAR = new Rectangle(1018, 187, 171, 797);

    private static final Color CRT = new Color(112, 232, 122);
    private static final Color CRT_BRIGHT = new Color(198, 255, 187);
    private static final Color CRT_DIM = new Color(40, 110, 52);
    private static final Color CRT_DARK = new Color(7, 27, 17, 220);
    private static final Color FAIL = new Color(255, 158, 89);

    private final BufferedImage frameImage;
    private final BufferedImage buttonSheet;
    private final BufferedImage navigationButtonSheet;
    private final BufferedImage plugImage;
    private final Font pixelFont;

    AutoTesterRenderer(BufferedImage frameImage, BufferedImage buttonSheet,
                       BufferedImage navigationButtonSheet, BufferedImage plugImage,
                       Font pixelFont) {
        this.frameImage = frameImage;
        this.buttonSheet = buttonSheet;
        this.navigationButtonSheet = navigationButtonSheet;
        this.plugImage = plugImage;
        this.pixelFont = pixelFont;
    }

    void draw(Graphics2D graphics, CircuitRecipe target, Boolean[] observations,
              int activeRow, int mouseX, int mouseY, Action pressedAction,
              String status) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g.setColor(new Color(0, 0, 0, 155));
        g.fillRect(0, 0, GameConstants.W, GameConstants.H);
        if (frameImage != null) {
            // Normalize the frame into the square space used by tester.annotations.json.
            g.drawImage(frameImage, FRAME_X, FRAME_Y, FRAME_SIZE, FRAME_SIZE, null);
        }
        if (plugImage != null) {
            g.drawImage(plugImage, TesterPlugLayout.LOGICAL_X, TesterPlugLayout.LOGICAL_Y,
                TesterPlugLayout.logicalWidth(plugImage.getWidth()),
                TesterPlugLayout.logicalHeight(plugImage.getHeight()), null);
        }

        Rectangle screen = screenBounds();
        Rectangle sidebar = sidebarBounds();
        drawScreen(g, target, observations, activeRow, screen, status);
        drawSidebar(g, sidebar, mouseX, mouseY, pressedAction);
        g.dispose();
    }

    Action actionAt(int x, int y) {
        Rectangle sidebar = sidebarBounds();
        if (buttonBounds(sidebar, 0).contains(x, y)) return Action.RUN;
        if (buttonBounds(sidebar, 1).contains(x, y)) return Action.STOP;
        if (buttonBounds(sidebar, 2).contains(x, y)) return Action.CLEAR;
        if (previousBounds(sidebar).contains(x, y)) return Action.PREVIOUS;
        if (nextBounds(sidebar).contains(x, y)) return Action.NEXT;
        return Action.NONE;
    }

    static Rectangle frameBounds() {
        return new Rectangle(FRAME_X, FRAME_Y, FRAME_SIZE, FRAME_SIZE);
    }

    static Rectangle screenBounds() {
        return scaledRegion(SOURCE_SCREEN);
    }

    static Rectangle sidebarBounds() {
        return scaledRegion(SOURCE_SIDEBAR);
    }

    private void drawScreen(Graphics2D g, CircuitRecipe target, Boolean[] observations,
                            int activeRow, Rectangle screen, String status) {
        Graphics2D crt = (Graphics2D) g.create();
        crt.setClip(screen);
        drawCrtGlass(crt, screen);
        drawPhosphorContent(crt, target, observations, activeRow, screen, status);
        crt.dispose();
    }

    private void drawPhosphorContent(Graphics2D g, CircuitRecipe target,
                                     Boolean[] observations, int activeRow,
                                     Rectangle screen, String status) {
        int left = screen.x + 11;

        g.setColor(CRT_BRIGHT);
        text(g, "LOGICLENS / " + target.name, left, screen.y + 15, 0.65f);
        g.setColor(CRT);
        text(g, "TARGET CONFIG // " + target.name, left, screen.y + 29, 0.55f);

        int statusX = left;
        int rowX = left + 30;
        int aX = left + 44;
        int bX = left + 57;
        int expectedX = left + 76;
        int actualX = left + 103;
        int headerY = screen.y + 47;
        g.setColor(CRT);
        text(g, "STATE", statusX, headerY, 0.5f);
        text(g, "#", rowX, headerY, 0.5f);
        text(g, "A", aX, headerY, 0.5f);
        text(g, "B", bX, headerY, 0.5f);
        text(g, "EXP", expectedX, headerY, 0.5f);
        text(g, "GOT", actualX, headerY, 0.5f);

        for (int row = 0; row < 4; row++) {
            int baseline = screen.y + 63 + row * 15;

            Boolean actual = observations[row];
            boolean expected = target.truth[row];
            if (row == activeRow) g.setColor(CRT_BRIGHT);
            else if (actual == null) g.setColor(CRT);
            else g.setColor(actual == expected ? CRT_BRIGHT : FAIL);
            text(g, resultLabel(actual, expected), statusX, baseline, 0.5f);
            text(g, Integer.toString(row + 1), rowX, baseline, 0.52f);
            text(g, row >= 2 ? "1" : "0", aX, baseline, 0.52f);
            text(g, row % 2 == 1 ? "1" : "0", bX, baseline, 0.52f);
            text(g, expected ? "1" : "0", expectedX + 4, baseline, 0.52f);
            text(g, actual == null ? "-" : (actual ? "1" : "0"),
                actualX + 4, baseline, 0.52f);
        }

        int footerY = screen.y + 135;
        g.setColor(CRT);
        text(g, status, left, footerY, 0.48f);
    }

    private static void drawCrtGlass(Graphics2D g, Rectangle screen) {
        g.setColor(new Color(0, 0, 0, 26));
        for (int y = screen.y + 1; y < screen.y + screen.height - 1; y += 3) {
            g.fillRect(screen.x + 2, y, screen.width - 4, 1);
        }

        g.setColor(new Color(165, 255, 157, 13));
        for (int y = screen.y + 3; y < screen.y + screen.height - 3; y += 5) {
            for (int x = screen.x + 3; x < screen.x + screen.width - 3; x += 7) {
                if (((x * 31 + y * 17) & 15) == 0) g.fillRect(x, y, 1, 1);
            }
        }

        float radius = Math.max(screen.width, screen.height) * 0.68f;
        g.setPaint(new RadialGradientPaint(
            new Point2D.Float(screen.x + screen.width / 2f,
                screen.y + screen.height / 2f),
            radius,
            new float[] { 0.58f, 1f },
            new Color[] { new Color(0, 0, 0, 0), new Color(0, 7, 3, 170) }));
        g.fillRect(screen.x, screen.y, screen.width, screen.height);
    }

    private void drawSidebar(Graphics2D g, Rectangle sidebar, int mouseX, int mouseY,
                             Action pressedAction) {
        drawTesterButton(g, buttonBounds(sidebar, 0), 0, "RUN", mouseX, mouseY);
        drawTesterButton(g, buttonBounds(sidebar, 1), 1, "STOP", mouseX, mouseY);
        drawTesterButton(g, buttonBounds(sidebar, 2), 2, "CLEAR", mouseX, mouseY);

        drawNavigationButton(g, previousBounds(sidebar), NAV_LEFT_IDLE, NAV_LEFT_PRESSED,
            "<", pressedAction == Action.PREVIOUS, mouseX, mouseY);
        drawNavigationButton(g, nextBounds(sidebar), NAV_RIGHT_IDLE, NAV_RIGHT_PRESSED,
            ">", pressedAction == Action.NEXT, mouseX, mouseY);
    }

    private void drawTesterButton(Graphics2D g, Rectangle bounds, int row, String fallbackLabel,
                                  int mouseX, int mouseY) {
        if (buttonSheet == null) {
            drawTextButton(g, bounds, fallbackLabel, mouseX, mouseY);
            return;
        }

        int column = bounds.contains(mouseX, mouseY) ? 1 : 0;
        Rectangle source = TESTER_BUTTON_SOURCES[row][column];
        g.drawImage(buttonSheet,
            bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
            source.x, source.y, source.x + source.width, source.y + source.height, null);
    }

    private void drawTextButton(Graphics2D g, Rectangle bounds, String label,
                                int mouseX, int mouseY) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        g.setColor(hovered ? new Color(38, 88, 44, 210) : CRT_DARK);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(hovered ? CRT_BRIGHT : CRT_DIM);
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(hovered ? CRT_BRIGHT : CRT);
        centeredText(g, label, bounds.x + bounds.width / 2,
            bounds.y + bounds.height / 2 + 3, label.length() > 4 ? 0.34f : 0.4f);
    }

    private void drawNavigationButton(Graphics2D g, Rectangle bounds, Rectangle idleSource,
                                      Rectangle pressedSource, String fallbackLabel,
                                      boolean pressed, int mouseX, int mouseY) {
        if (navigationButtonSheet == null) {
            drawTextButton(g, bounds, fallbackLabel, mouseX, mouseY);
            return;
        }

        Rectangle source = pressed ? pressedSource : idleSource;
        g.drawImage(navigationButtonSheet,
            bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
            source.x, source.y, source.x + source.width, source.y + source.height, null);
    }

    private static Rectangle buttonBounds(Rectangle sidebar, int index) {
        return new Rectangle(sidebar.x + 7, sidebar.y + 16 + index * 30,
            sidebar.width - 8, 28);
    }

    private static Rectangle previousBounds(Rectangle sidebar) {
        return new Rectangle(sidebar.x + 8, sidebar.y + 124, 13, 18);
    }

    private static Rectangle nextBounds(Rectangle sidebar) {
        return new Rectangle(sidebar.x + sidebar.width - 13, sidebar.y + 124, 13, 18);
    }

    private static Rectangle scaledRegion(Rectangle source) {
        double scale = FRAME_SIZE / (double) SOURCE_SIZE;
        return new Rectangle(
            FRAME_X + (int) Math.round(source.x * scale),
            FRAME_Y + (int) Math.round(source.y * scale),
            (int) Math.round(source.width * scale),
            (int) Math.round(source.height * scale));
    }

    private static String resultLabel(Boolean actual, boolean expected) {
        if (actual == null) return "----";
        return actual == expected ? "PASS" : "FAIL";
    }

    private void text(Graphics2D g, String value, int x, int y, float size) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(GameConstants.PIXEL_FONT_BASE_SIZE * size));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(pixelFont.deriveFont(Font.PLAIN, fontSize));
        g.drawString(value, x, y);
        g.setFont(old);
    }

    private void centeredText(Graphics2D g, String value, int centerX,
                              int baselineY, float size) {
        int fontSize = Math.max(1, Math.round(GameConstants.PIXEL_FONT_BASE_SIZE * size));
        Font font = pixelFont.deriveFont(Font.PLAIN, fontSize);
        int width = g.getFontMetrics(font).stringWidth(value);
        text(g, value, centerX - width / 2, baselineY, size);
    }
}

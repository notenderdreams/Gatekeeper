package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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

    private static final Color CRT = new Color(83, 166, 87);
    private static final Color CRT_BRIGHT = new Color(120, 211, 117);
    private static final Color CRT_DIM = new Color(38, 91, 49);
    private static final Color CRT_SCANLINE = new Color(29, 82, 40, 72);
    private static final Color CRT_DARK = new Color(7, 27, 17, 220);
    private static final Color FAIL = new Color(205, 122, 69);

    private final BufferedImage frameImage;
    private final BufferedImage buttonSheet;
    private final BufferedImage navigationButtonSheet;
    private final Font pixelFont;

    AutoTesterRenderer(BufferedImage frameImage, BufferedImage buttonSheet,
                       BufferedImage navigationButtonSheet, Font pixelFont) {
        this.frameImage = frameImage;
        this.buttonSheet = buttonSheet;
        this.navigationButtonSheet = navigationButtonSheet;
        this.pixelFont = pixelFont;
    }

    void draw(Graphics2D graphics, CircuitModel circuit, int selectedRow,
              int mouseX, int mouseY, Action pressedAction, String status) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g.setColor(new Color(0, 0, 0, 155));
        g.fillRect(0, 0, GameConstants.W, GameConstants.H);
        if (frameImage != null) {
            // tester.png is square; equal destination dimensions preserve its aspect ratio.
            g.drawImage(frameImage, FRAME_X, FRAME_Y, FRAME_SIZE, FRAME_SIZE, null);
        }

        Rectangle screen = screenBounds();
        Rectangle sidebar = sidebarBounds();
        drawScreen(g, circuit, selectedRow, screen, status);
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

    private void drawScreen(Graphics2D g, CircuitModel circuit, int selectedRow,
                            Rectangle screen, String status) {
        int left = screen.x + 11;
        int tableRight = left + 126;
        g.setClip(screen);

        g.setColor(CRT_SCANLINE);
        for (int y = screen.y + 2; y < screen.y + screen.height; y += 3) {
            g.drawLine(screen.x + 2, y, screen.x + screen.width - 3, y);
        }

        g.setColor(CRT_BRIGHT);
        text(g, "LOGICLENS / " + circuit.recipe().name, left, screen.y + 15, 0.55f);
        g.setColor(CRT);
        text(g, "TRUTH TABLE  CASE #" + (selectedRow + 1), left, screen.y + 28, 0.46f);

        int dividerY = screen.y + 35;
        g.setColor(CRT_DIM);
        g.drawLine(left, dividerY, tableRight, dividerY);

        int statusX = left;
        int rowX = left + 30;
        int aX = left + 44;
        int bX = left + 57;
        int expectedX = left + 76;
        int actualX = left + 103;
        int headerY = screen.y + 47;
        g.setColor(CRT);
        text(g, "STATE", statusX, headerY, 0.38f);
        text(g, "#", rowX, headerY, 0.38f);
        text(g, "A", aX, headerY, 0.38f);
        text(g, "B", bX, headerY, 0.38f);
        text(g, "EXP", expectedX, headerY, 0.38f);
        text(g, "GOT", actualX, headerY, 0.38f);

        Boolean[] observations = circuit.observations();
        for (int row = 0; row < 4; row++) {
            int top = screen.y + 53 + row * 15;
            int baseline = top + 10;
            boolean selected = row == selectedRow;
            if (selected) {
                g.setColor(new Color(39, 101, 51, 105));
                g.fillRect(left - 3, top, tableRight - left + 6, 12);
                g.setColor(CRT_BRIGHT);
                g.drawRect(left - 3, top, tableRight - left + 5, 12);
            }

            Boolean actual = observations[row];
            boolean expected = circuit.recipe().truth[row];
            if (actual == null) g.setColor(selected ? CRT_BRIGHT : CRT);
            else g.setColor(actual == expected ? CRT_BRIGHT : FAIL);
            text(g, resultLabel(actual, expected), statusX, baseline, 0.4f);
            text(g, Integer.toString(row + 1), rowX, baseline, 0.42f);
            text(g, row >= 2 ? "1" : "0", aX, baseline, 0.42f);
            text(g, row % 2 == 1 ? "1" : "0", bX, baseline, 0.42f);
            text(g, expected ? "1" : "0", expectedX + 4, baseline, 0.42f);
            text(g, actual == null ? "-" : (actual ? "1" : "0"),
                actualX + 4, baseline, 0.42f);
        }

        int footerY = screen.y + 135;
        g.setColor(CRT_DIM);
        g.drawLine(left, footerY - 11, tableRight, footerY - 11);
        g.setColor(CRT);
        text(g, status, left, footerY, 0.38f);
        g.setClip(null);
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

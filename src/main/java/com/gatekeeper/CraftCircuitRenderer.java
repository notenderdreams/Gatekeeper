package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/**
 * Renders the Craft Circuit modal with a scrollable list of existing/template circuits,
 * an interactive search/name input field, scrollbar, action buttons, and hit detection.
 */
final class CraftCircuitRenderer {
    enum Action {
        NONE, CLOSE, CRAFT, CANCEL, CLEAR, SCROLL_UP, SCROLL_DOWN
    }

    private static final Rectangle PANEL = new Rectangle(75, 26, 330, 218);
    private static final Rectangle CLOSE_BUTTON = new Rectangle(384, 32, 14, 14);
    private static final Rectangle INPUT_BOX = new Rectangle(95, 62, 290, 24);
    private static final Rectangle CLEAR_BUTTON = new Rectangle(367, 66, 14, 16);
    private static final Rectangle LIST_CONTAINER = new Rectangle(95, 98, 290, 78);
    private static final Rectangle SCROLL_UP_BUTTON = new Rectangle(369, 101, 12, 10);
    private static final Rectangle SCROLL_DOWN_BUTTON = new Rectangle(369, 163, 12, 10);
    private static final Rectangle SCROLLBAR_TRACK = new Rectangle(369, 112, 12, 50);
    private static final Rectangle CRAFT_BUTTON = new Rectangle(130, 194, 105, 20);
    private static final Rectangle CANCEL_BUTTON = new Rectangle(245, 194, 105, 20);

    private final Font font;

    CraftCircuitRenderer() {
        this(GameAssets.loadPixelFont());
    }

    CraftCircuitRenderer(Font font) {
        this.font = font != null ? font : new Font(Font.MONOSPACED, Font.PLAIN, PIXEL_FONT_BASE_SIZE);
    }

    static Rectangle panelBounds() { return PANEL; }
    static Rectangle closeBounds() { return CLOSE_BUTTON; }
    static Rectangle inputBoxBounds() { return INPUT_BOX; }
    static Rectangle clearButtonBounds() { return CLEAR_BUTTON; }
    static Rectangle listContainerBounds() { return LIST_CONTAINER; }
    static Rectangle scrollUpBounds() { return SCROLL_UP_BUTTON; }
    static Rectangle scrollDownBounds() { return SCROLL_DOWN_BUTTON; }
    static Rectangle scrollbarTrackBounds() { return SCROLLBAR_TRACK; }
    static Rectangle craftButtonBounds() { return CRAFT_BUTTON; }
    static Rectangle cancelButtonBounds() { return CANCEL_BUTTON; }

    static Rectangle itemRowBounds(int visibleRow) {
        return new Rectangle(98, 101 + visibleRow * 18, 266, 17);
    }

    Action actionAt(int x, int y) {
        if (CLOSE_BUTTON.contains(x, y)) return Action.CLOSE;
        if (CRAFT_BUTTON.contains(x, y)) return Action.CRAFT;
        if (CANCEL_BUTTON.contains(x, y)) return Action.CANCEL;
        if (CLEAR_BUTTON.contains(x, y)) return Action.CLEAR;
        if (SCROLL_UP_BUTTON.contains(x, y)) return Action.SCROLL_UP;
        if (SCROLL_DOWN_BUTTON.contains(x, y)) return Action.SCROLL_DOWN;
        return Action.NONE;
    }

    int itemIndexAt(int x, int y, int filteredCount, int scrollOffset) {
        for (int r = 0; r < CraftCircuitModel.VISIBLE_ROWS; r++) {
            int itemIndex = scrollOffset + r;
            if (itemIndex >= filteredCount) break;
            if (itemRowBounds(r).contains(x, y)) {
                return itemIndex;
            }
        }
        return -1;
    }

    void handleScrollbarClick(int x, int y, CraftCircuitModel model) {
        if (SCROLLBAR_TRACK.contains(x, y)) {
            int maxScroll = model.maxScroll();
            if (maxScroll > 0) {
                float ratio = (float) (y - SCROLLBAR_TRACK.y) / SCROLLBAR_TRACK.height;
                int targetScroll = Math.round(ratio * maxScroll);
                model.setScrollOffset(targetScroll);
            }
        }
    }

    void draw(Graphics2D g, CraftCircuitModel model, int mouseX, int mouseY) {
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, W, H);

        // Modal frame
        g.setColor(new Color(25, 25, 22));
        g.fillRect(PANEL.x, PANEL.y, PANEL.width, PANEL.height);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(PANEL.x, PANEL.y, PANEL.width - 1, PANEL.height - 1);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(PANEL.x + 3, PANEL.y + 3, PANEL.width - 7, PANEL.height - 7);

        // Title & close
        g.setColor(YELLOW);
        centeredText(g, "CRAFT CIRCUIT", W / 2, 46, 1.8f);

        boolean closeHover = CLOSE_BUTTON.contains(mouseX, mouseY);
        g.setColor(closeHover ? RED : DIM);
        centeredText(g, "X", CLOSE_BUTTON.x + CLOSE_BUTTON.width / 2, CLOSE_BUTTON.y + 11, 1.0f);

        // Input / Search field
        g.setColor(DIM);
        text(g, "CIRCUIT NAME / SEARCH", 95, 58, 0.9f);
        text(g, model.draft().length() + "/" + CraftCircuitModel.MAX_NAME_LENGTH, 362, 58, 0.9f);

        boolean inputHover = INPUT_BOX.contains(mouseX, mouseY);
        g.setColor(new Color(7, 9, 9));
        g.fillRect(INPUT_BOX.x, INPUT_BOX.y, INPUT_BOX.width, INPUT_BOX.height);
        g.setColor(inputHover ? YELLOW : CYAN);
        g.drawRect(INPUT_BOX.x, INPUT_BOX.y, INPUT_BOX.width - 1, INPUT_BOX.height - 1);

        if (model.draft().isEmpty()) {
            g.setColor(DIM);
            text(g, "TYPE NAME OR SEARCH...", INPUT_BOX.x + 8, INPUT_BOX.y + 16, 1.0f);
            g.setColor(INK);
            text(g, "_", INPUT_BOX.x + 8, INPUT_BOX.y + 16, 1.0f);
        } else {
            g.setColor(INK);
            text(g, model.draft() + "_", INPUT_BOX.x + 8, INPUT_BOX.y + 16, 1.0f);

            boolean clearHover = CLEAR_BUTTON.contains(mouseX, mouseY);
            g.setColor(clearHover ? RED : DIM);
            centeredText(g, "x", CLEAR_BUTTON.x + CLEAR_BUTTON.width / 2, CLEAR_BUTTON.y + 12, 1.0f);
        }

        // List container
        g.setColor(DIM);
        text(g, "MATCHING CRAFTED CIRCUITS", 95, 94, 0.9f);
        text(g, model.filteredCandidates().size() + " MATCHES", 320, 94, 0.9f);

        g.setColor(new Color(12, 13, 14));
        g.fillRect(LIST_CONTAINER.x, LIST_CONTAINER.y, LIST_CONTAINER.width, LIST_CONTAINER.height);
        g.setColor(new Color(84, 62, 40));
        g.drawRect(LIST_CONTAINER.x, LIST_CONTAINER.y, LIST_CONTAINER.width - 1, LIST_CONTAINER.height - 1);

        List<String> filtered = model.filteredCandidates();
        if (filtered.isEmpty()) {
            g.setColor(DIM);
            centeredText(g, "NO MATCHING CIRCUITS", 230, 133, 1.0f);
            g.setColor(CYAN);
            centeredText(g, "ENTER A NEW NAME TO CRAFT", 230, 149, 0.85f);
        } else {
            int count = Math.min(CraftCircuitModel.VISIBLE_ROWS, filtered.size() - model.scrollOffset());
            for (int r = 0; r < count; r++) {
                int index = model.scrollOffset() + r;
                String name = filtered.get(index);
                Rectangle row = itemRowBounds(r);
                boolean isSelected = (index == model.selectedIndex());
                boolean isHovered = row.contains(mouseX, mouseY);

                if (isSelected) {
                    g.setColor(new Color(84, 62, 40));
                    g.fillRect(row.x, row.y, row.width, row.height);
                    g.setColor(YELLOW);
                    g.drawRect(row.x, row.y, row.width - 1, row.height - 1);
                    text(g, "> " + name, row.x + 5, row.y + 12, 1.0f);
                } else if (isHovered) {
                    g.setColor(new Color(45, 48, 46));
                    g.fillRect(row.x, row.y, row.width, row.height);
                    g.setColor(CYAN);
                    g.drawRect(row.x, row.y, row.width - 1, row.height - 1);
                    g.setColor(INK);
                    text(g, "  " + name, row.x + 5, row.y + 12, 1.0f);
                } else {
                    g.setColor(r % 2 == 0 ? new Color(18, 19, 20) : new Color(14, 15, 16));
                    g.fillRect(row.x, row.y, row.width, row.height);
                    g.setColor(INK);
                    text(g, "  " + name, row.x + 5, row.y + 12, 1.0f);
                }

                if (model.isBagName(name)) {
                    g.setColor(CYAN);
                    text(g, "[IN BAG]", row.x + row.width - 54, row.y + 12, 0.85f);
                }
            }
        }

        // Scrollbar
        boolean upHover = SCROLL_UP_BUTTON.contains(mouseX, mouseY);
        boolean downHover = SCROLL_DOWN_BUTTON.contains(mouseX, mouseY);

        g.setColor(upHover ? new Color(60, 64, 62) : new Color(20, 22, 24));
        g.fillRect(SCROLL_UP_BUTTON.x, SCROLL_UP_BUTTON.y, SCROLL_UP_BUTTON.width, SCROLL_UP_BUTTON.height);
        g.setColor(upHover ? YELLOW : new Color(84, 62, 40));
        g.drawRect(SCROLL_UP_BUTTON.x, SCROLL_UP_BUTTON.y, SCROLL_UP_BUTTON.width - 1, SCROLL_UP_BUTTON.height - 1);
        g.setColor(upHover ? INK : DIM);
        centeredText(g, "^", SCROLL_UP_BUTTON.x + SCROLL_UP_BUTTON.width / 2, SCROLL_UP_BUTTON.y + 9, 0.8f);

        g.setColor(downHover ? new Color(60, 64, 62) : new Color(20, 22, 24));
        g.fillRect(SCROLL_DOWN_BUTTON.x, SCROLL_DOWN_BUTTON.y, SCROLL_DOWN_BUTTON.width, SCROLL_DOWN_BUTTON.height);
        g.setColor(downHover ? YELLOW : new Color(84, 62, 40));
        g.drawRect(SCROLL_DOWN_BUTTON.x, SCROLL_DOWN_BUTTON.y, SCROLL_DOWN_BUTTON.width - 1, SCROLL_DOWN_BUTTON.height - 1);
        g.setColor(downHover ? INK : DIM);
        centeredText(g, "v", SCROLL_DOWN_BUTTON.x + SCROLL_DOWN_BUTTON.width / 2, SCROLL_DOWN_BUTTON.y + 8, 0.8f);

        g.setColor(new Color(8, 9, 10));
        g.fillRect(SCROLLBAR_TRACK.x, SCROLLBAR_TRACK.y, SCROLLBAR_TRACK.width, SCROLLBAR_TRACK.height);
        g.setColor(new Color(60, 45, 30));
        g.drawRect(SCROLLBAR_TRACK.x, SCROLLBAR_TRACK.y, SCROLLBAR_TRACK.width - 1, SCROLLBAR_TRACK.height - 1);

        int total = filtered.size();
        if (total > CraftCircuitModel.VISIBLE_ROWS) {
            int maxScroll = model.maxScroll();
            int thumbHeight = Math.max(10, Math.round((float) CraftCircuitModel.VISIBLE_ROWS / total * SCROLLBAR_TRACK.height));
            int thumbY = SCROLLBAR_TRACK.y + Math.round((float) model.scrollOffset() / maxScroll * (SCROLLBAR_TRACK.height - thumbHeight));
            Rectangle thumb = new Rectangle(SCROLLBAR_TRACK.x + 1, thumbY, SCROLLBAR_TRACK.width - 2, thumbHeight);
            boolean thumbHover = thumb.contains(mouseX, mouseY);

            g.setColor(thumbHover ? YELLOW : new Color(181, 126, 68));
            g.fillRect(thumb.x, thumb.y, thumb.width, thumb.height);
            g.setColor(new Color(226, 180, 91));
            g.drawRect(thumb.x, thumb.y, thumb.width - 1, thumb.height - 1);
        }

        // Status
        String status = model.status();
        if (!status.isBlank()) {
            g.setColor(RED);
            centeredText(g, status, W / 2, 186, 0.95f);
        } else if (model.selectedIndex() >= 0) {
            g.setColor(CYAN);
            centeredText(g, "GROUP WITH: " + model.draft(), W / 2, 186, 0.95f);
        } else if (!model.draft().isBlank()) {
            g.setColor(DIM);
            centeredText(g, "NEW CIRCUIT: " + model.draft(), W / 2, 186, 0.95f);
        }

        // Buttons
        boolean craftHover = CRAFT_BUTTON.contains(mouseX, mouseY);
        boolean cancelHover = CANCEL_BUTTON.contains(mouseX, mouseY);

        g.setColor(craftHover ? new Color(50, 95, 56) : new Color(36, 68, 40));
        g.fillRect(CRAFT_BUTTON.x, CRAFT_BUTTON.y, CRAFT_BUTTON.width, CRAFT_BUTTON.height);
        g.setColor(craftHover ? YELLOW : new Color(90, 180, 100));
        g.drawRect(CRAFT_BUTTON.x, CRAFT_BUTTON.y, CRAFT_BUTTON.width - 1, CRAFT_BUTTON.height - 1);
        g.setColor(INK);
        centeredText(g, "CRAFT [ENTER]", CRAFT_BUTTON.x + CRAFT_BUTTON.width / 2, CRAFT_BUTTON.y + 14, 0.95f);

        g.setColor(cancelHover ? new Color(75, 40, 35) : new Color(55, 30, 25));
        g.fillRect(CANCEL_BUTTON.x, CANCEL_BUTTON.y, CANCEL_BUTTON.width, CANCEL_BUTTON.height);
        g.setColor(cancelHover ? YELLOW : new Color(181, 126, 68));
        g.drawRect(CANCEL_BUTTON.x, CANCEL_BUTTON.y, CANCEL_BUTTON.width - 1, CANCEL_BUTTON.height - 1);
        g.setColor(INK);
        centeredText(g, "CANCEL [ESC]", CANCEL_BUTTON.x + CANCEL_BUTTON.width / 2, CANCEL_BUTTON.y + 14, 0.95f);

        // Footer
        g.setColor(DIM);
        centeredText(g, "TYPE: SEARCH/NAME   UP/DOWN: SELECT   ENTER: CRAFT   ESC: CANCEL",
            W / 2, 227, 0.85f);
    }

    private void text(Graphics2D g, String value, int x, int y, float size) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(PIXEL_FONT_BASE_SIZE * size));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(font.deriveFont(Font.PLAIN, fontSize));
        g.drawString(value, x, y);
        g.setFont(old);
    }

    private void centeredText(Graphics2D g, String value, int centerX, int baselineY, float size) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(PIXEL_FONT_BASE_SIZE * size));
        Font currentFont = font.deriveFont(Font.PLAIN, fontSize);
        g.setFont(currentFont);
        int width = g.getFontMetrics(currentFont).stringWidth(value);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.drawString(value, centerX - width / 2, baselineY);
        g.setFont(old);
    }
}

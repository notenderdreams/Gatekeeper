package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the character's gate inventory as a compact textured 3x2 grid. */
final class InventoryRenderer {
    private static final Rectangle PANEL = new Rectangle(54, 76, 372, 118);
    private static final int SLOT_COUNT = 8;
    private static final int CARD_W = 38;
    private static final int CARD_H = 43;
    private static final int CARD_GAP_X = 4;
    private static final int GRID_X = 74;
    private static final int GRID_Y = 108;
    private static final Color CREAM = new Color(246, 218, 157);
    private static final Color GOLD = new Color(255, 196, 37);

    private final BufferedImage panelTexture;
    private final BufferedImage cardTexture;
    private final Font font;
    private final ProductGateRenderer gateRenderer;

    InventoryRenderer(BufferedImage panelTexture, BufferedImage cardTexture,
                      BufferedImage portImage, Font font) {
        this.panelTexture = panelTexture;
        this.cardTexture = cardTexture;
        this.font = font;
        gateRenderer = new ProductGateRenderer(panelTexture, portImage, font);
    }

    void draw(Graphics2D graphics, ShopModel inventory, int knownProductCount) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, GameConstants.W, GameConstants.H);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(PANEL.x + 5, PANEL.y + 7, PANEL.width, PANEL.height);
        if (panelTexture != null) {
            g.drawImage(panelTexture, PANEL.x, PANEL.y, PANEL.width, PANEL.height, null);
        } else {
            g.setColor(new Color(35, 31, 24));
            g.fillRect(PANEL.x, PANEL.y, PANEL.width, PANEL.height);
        }
        g.setColor(new Color(5, 7, 7, 205));
        g.fillRect(PANEL.x + 3, PANEL.y + 3, PANEL.width - 6, PANEL.height - 6);
        g.setColor(new Color(108, 76, 35));
        g.drawRect(PANEL.x, PANEL.y, PANEL.width - 1, PANEL.height - 1);
        g.setColor(new Color(226, 180, 91));
        g.drawRect(PANEL.x + 2, PANEL.y + 2, PANEL.width - 5, PANEL.height - 5);
        g.setColor(new Color(65, 43, 22));
        g.drawRect(PANEL.x + 5, PANEL.y + 5, PANEL.width - 11, PANEL.height - 11);

        g.setColor(CREAM);
        centeredText(g, "COMPONENT INVENTORY", new Rectangle(PANEL.x, PANEL.y + 8,
            PANEL.width, 16), 12f);
        g.setColor(new Color(156, 111, 51));
        g.drawLine(PANEL.x + 16, PANEL.y + 27, PANEL.x + PANEL.width - 16, PANEL.y + 27);

        int visibleCount = Math.min(SLOT_COUNT,
            Math.min(Math.max(0, knownProductCount), inventory.products().size()));
        for (int slot = 0; slot < visibleCount; slot++) {
            drawCard(g, inventory, slot, cardBounds(slot));
        }

        g.setColor(new Color(190, 164, 112));
        centeredText(g, "I / ESC  CLOSE", new Rectangle(PANEL.x, PANEL.y + 99,
            PANEL.width, 10), 7.5f);
        g.dispose();
    }

    static Rectangle cardBounds(int index) {
        return new Rectangle(GRID_X + index * (CARD_W + CARD_GAP_X),
            GRID_Y, CARD_W, CARD_H);
    }

    private void drawCard(Graphics2D g, ShopModel inventory, int index, Rectangle card) {
        if (cardTexture != null) {
            g.drawImage(cardTexture, card.x, card.y, card.width, card.height, null);
        } else {
            g.setColor(new Color(31, 27, 20));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(new Color(130, 88, 38));
            g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
        }

        ShopProduct product = inventory.products().get(index);
        gateRenderer.draw(g, product,
            new Rectangle(card.x + 4, card.y + 3, card.width - 8, 19));
        g.setColor(CREAM);
        centeredText(g, product.name(),
            new Rectangle(card.x + 3, card.y + 25, card.width - 6, 7), 6.5f);

        g.setColor(new Color(6, 7, 7, 225));
        g.fillRect(card.x + 3, card.y + 33, card.width - 6, 8);
        g.setColor(GOLD);
        centeredText(g, "OWNED " + inventory.purchased(index),
            new Rectangle(card.x + 3, card.y + 33, card.width - 6, 8), 6f);
    }

    private void centeredText(Graphics2D g, String value, Rectangle bounds, float size) {
        Font old = g.getFont();
        g.setFont(font.deriveFont(Font.PLAIN, size));
        int x = bounds.x + (bounds.width - g.getFontMetrics().stringWidth(value)) / 2;
        int y = bounds.y + (bounds.height - g.getFontMetrics().getHeight()) / 2
            + g.getFontMetrics().getAscent();
        g.drawString(value, x, y);
        g.setFont(old);
    }
}

package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws Mira's annotated, asset-backed shop overlay and owns all of its hit regions. */
final class ShopRenderer {
    enum Action { NONE, CLOSE, DECREASE, INCREASE, BUY_SELL }

    private static final int SOURCE_W = 1362;
    private static final int SOURCE_H = 948;
    private static final float FRAME_SCALE = GameConstants.H / (float) SOURCE_H;
    private static final int FRAME_W = Math.round(SOURCE_W * FRAME_SCALE);
    private static final int FRAME_H = Math.round(SOURCE_H * FRAME_SCALE);
    private static final int FRAME_X = (GameConstants.W - FRAME_W) / 2;
    private static final int FRAME_Y = (GameConstants.H - FRAME_H) / 2;
    private static final int CARD_SOURCE_W = 189;
    private static final int CARD_SOURCE_H = 213;
    private static final int CARD_W = scale(CARD_SOURCE_W);
    private static final int CARD_H = scale(CARD_SOURCE_H);

    // Exact source-space regions from shop.annotations.json.
    private static final Rectangle SOURCE_THUMBNAIL = new Rectangle(964, 221, 335, 190);
    private static final Rectangle SOURCE_NAME = new Rectangle(964, 159, 341, 33);
    private static final Rectangle SOURCE_DESCRIPTION = new Rectangle(960, 431, 342, 64);
    private static final Rectangle SOURCE_SPECIFICATIONS = new Rectangle(957, 566, 345, 104);
    private static final Rectangle SOURCE_QUANTITY = new Rectangle(1181, 713, 69, 32);
    private static final Rectangle SOURCE_TOTAL = new Rectangle(1203, 786, 92, 28);
    private static final Rectangle SOURCE_ITEMS_GRID = new Rectangle(43, 180, 847, 732);
    private static final Rectangle SOURCE_BALANCE = new Rectangle(1099, 64, 106, 31);
    private static final Rectangle SOURCE_CLOSE = new Rectangle(1252, 41, 78, 76);
    private static final Rectangle SOURCE_DECREASE = new Rectangle(1120, 707, 42, 45);
    private static final Rectangle SOURCE_INCREASE = new Rectangle(1268, 707, 41, 44);
    private static final Rectangle SOURCE_TRADE = new Rectangle(945, 841, 369, 74);

    // Exact source-space regions from shop-item.annotations.json.
    private static final Rectangle CARD_GATE_IMAGE = new Rectangle(19, 17, 150, 99);
    private static final Rectangle CARD_NAME = new Rectangle(26, 131, 143, 25);
    private static final Rectangle CARD_PRICE = new Rectangle(92, 173, 76, 15);

    private static final int CARD_COLUMNS = 4;
    private static final int CARD_GAP_X = 6;
    private static final int CARD_GAP_Y = 8;

    private static final Color CREAM = new Color(246, 218, 157);
    private static final Color GOLD = new Color(255, 196, 37);
    private static final Color SELECTED = new Color(255, 214, 101, 170);

    private final BufferedImage frame;
    private final BufferedImage pressedFrame;
    private final BufferedImage itemFrame;
    private final Font font;
    private final LogicNodeRenderer nodeRenderer;

    ShopRenderer(BufferedImage frame, BufferedImage itemFrame,
                 BufferedImage nodeTexture, BufferedImage portImage, Font font) {
        this.frame = frame;
        this.pressedFrame = createPressedFrame(frame);
        this.itemFrame = itemFrame;
        this.font = font;
        this.nodeRenderer = new LogicNodeRenderer(nodeTexture, portImage);
    }

    void draw(Graphics2D graphics, ShopModel model, int mouseX, int mouseY,
              Action pressedAction) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, GameConstants.W, GameConstants.H);
        if (frame != null) {
            g.drawImage(frame, FRAME_X, FRAME_Y, FRAME_W, FRAME_H, null);
        }
        else drawFallbackFrame(g);

        for (int i = 0; i < model.products().size(); i++) {
            Rectangle card = cardBounds(i);
            if (itemFrame != null) g.drawImage(itemFrame, card.x, card.y, card.width, card.height, null);
            else drawFallbackCard(g, card);
            drawCardContents(g, model.products().get(i), card);
            if (i == model.selectedIndex()) {
                g.setColor(SELECTED);
                g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
            }
        }

        drawDetails(g, model);
        if (pressedAction != Action.NONE) drawPressedControl(g, pressedAction);
        drawTradeLabel(g, pressedAction == Action.BUY_SELL);
        g.dispose();
    }

    Action actionAt(int x, int y) {
        if (closeBounds().contains(x, y)) return Action.CLOSE;
        if (decreaseBounds().contains(x, y)) return Action.DECREASE;
        if (increaseBounds().contains(x, y)) return Action.INCREASE;
        if (tradeBounds().contains(x, y)) return Action.BUY_SELL;
        return Action.NONE;
    }

    int productAt(int x, int y, int productCount) {
        for (int i = 0; i < productCount; i++) {
            if (cardBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    static Rectangle cardBounds(int index) {
        Rectangle grid = itemsGridBounds();
        int usedWidth = CARD_COLUMNS * CARD_W + (CARD_COLUMNS - 1) * CARD_GAP_X;
        int startX = grid.x + Math.max(0, (grid.width - usedWidth) / 2);
        int x = startX + (index % CARD_COLUMNS) * (CARD_W + CARD_GAP_X);
        int y = grid.y + 4 + (index / CARD_COLUMNS) * (CARD_H + CARD_GAP_Y);
        return new Rectangle(x, y, CARD_W, CARD_H);
    }

    static Rectangle frameBounds() { return new Rectangle(FRAME_X, FRAME_Y, FRAME_W, FRAME_H); }
    static Rectangle itemsGridBounds() { return scaledRegion(SOURCE_ITEMS_GRID); }
    static Rectangle closeBounds() { return scaledRegion(SOURCE_CLOSE); }
    static Rectangle decreaseBounds() { return scaledRegion(SOURCE_DECREASE); }
    static Rectangle increaseBounds() { return scaledRegion(SOURCE_INCREASE); }
    static Rectangle tradeBounds() { return scaledRegion(SOURCE_TRADE); }

    private void drawCardContents(Graphics2D g, ShopProduct product, Rectangle card) {
        Rectangle gate = cardRegion(card, CARD_GATE_IMAGE);
        Rectangle name = cardRegion(card, CARD_NAME);
        Rectangle price = cardRegion(card, CARD_PRICE);
        drawCanvasGate(g, product, gate);
        g.setColor(CREAM);
        centeredText(g, product.name(), name, 10f);
        g.setColor(GOLD);
        centeredText(g, Integer.toString(product.price()), price, 9.5f);
    }

    private void drawDetails(Graphics2D g, ShopModel model) {
        ShopProduct product = model.selected();
        g.setColor(CREAM);
        centeredText(g, product.name(), scaledRegion(SOURCE_NAME), 13f);
        drawCanvasGate(g, product, inset(scaledRegion(SOURCE_THUMBNAIL), 9, 6));

        Rectangle description = scaledRegion(SOURCE_DESCRIPTION);
        g.setColor(new Color(228, 205, 160));
        wrappedText(g, product.description(), description, 8f);

        Rectangle specs = scaledRegion(SOURCE_SPECIFICATIONS);
        g.setColor(CREAM);
        text(g, "INPUTS", specs.x + 5, specs.y + 12, 9f);
        text(g, Integer.toString(product.inputs()), specs.x + specs.width - 13, specs.y + 12, 9f);
        text(g, "OUTPUTS", specs.x + 5, specs.y + 25, 9f);
        text(g, Integer.toString(product.outputs()), specs.x + specs.width - 13, specs.y + 25, 9f);

        g.setColor(CREAM);
        centeredText(g, Integer.toString(model.quantity()), scaledRegion(SOURCE_QUANTITY), 10f);
        g.setColor(GOLD);
        centeredText(g, signedTotal(model.total()), scaledRegion(SOURCE_TOTAL), 10f);
        centeredText(g, Integer.toString(model.balance()), scaledRegion(SOURCE_BALANCE), 10f);
    }

    private void drawCanvasGate(Graphics2D g, ShopProduct product, Rectangle area) {
        int bodyHeight = LogicNodeRenderer.bodyHeight(product.inputs(), product.outputs());
        double sourceWidth = LogicNodeRenderer.BODY_WIDTH + 28.0;
        double sourceHeight = bodyHeight + 28.0;
        double scale = Math.min(area.width / sourceWidth, area.height / sourceHeight);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;

        Graphics2D gate = (Graphics2D) g.create();
        gate.clip(area);
        gate.translate(area.x + (area.width - drawWidth) / 2.0,
            area.y + (area.height - drawHeight) / 2.0);
        gate.scale(scale, scale);
        gate.setFont(font);
        nodeRenderer.draw(gate, 14, 14 + bodyHeight / 2,
            product.name(), product.inputs(), product.outputs());
        gate.dispose();
    }

    private void drawPressedControl(Graphics2D g, Action action) {
        Rectangle source = switch (action) {
            case CLOSE -> SOURCE_CLOSE;
            case DECREASE -> SOURCE_DECREASE;
            case INCREASE -> SOURCE_INCREASE;
            case BUY_SELL -> SOURCE_TRADE;
            case NONE -> null;
        };
        if (source == null || pressedFrame == null) return;
        Rectangle bounds = scaledRegion(source);
        g.drawImage(pressedFrame,
            bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
            source.x, source.y, source.x + source.width, source.y + source.height, null);
    }

    private void drawTradeLabel(Graphics2D g, boolean pressed) {
        BufferedImage sourceImage = pressed ? pressedFrame : frame;
        if (sourceImage == null) return;
        Rectangle button = tradeBounds();
        Rectangle label = new Rectangle(
            button.x + 7, button.y + 3, button.width - 14, button.height - 6);

        // Replace only the old label pixels with an untouched patch from the same
        // green button, preserving the surrounding frame and button border exactly.
        int cleanX = SOURCE_TRADE.x + 18;
        int cleanY = SOURCE_TRADE.y + 19;
        int cleanWidth = 50;
        int cleanHeight = 36;
        g.drawImage(sourceImage,
            label.x, label.y, label.x + label.width, label.y + label.height,
            cleanX, cleanY, cleanX + cleanWidth, cleanY + cleanHeight, null);
        g.setColor(pressed ? new Color(132, 129, 116) : CREAM);
        centeredText(g, "BUY / SELL", label, 13f);
    }

    private static String signedTotal(int total) {
        return total > 0 ? "+" + total : Integer.toString(total);
    }

    private void drawFallbackFrame(Graphics2D g) {
        g.setColor(new Color(10, 10, 8));
        g.fillRect(FRAME_X, FRAME_Y, FRAME_W, FRAME_H);
        g.setColor(new Color(112, 77, 30));
        g.drawRect(FRAME_X, FRAME_Y, FRAME_W - 1, FRAME_H - 1);
    }

    private void drawFallbackCard(Graphics2D g, Rectangle card) {
        g.setColor(new Color(40, 31, 20));
        g.fillRect(card.x, card.y, card.width, card.height);
        g.setColor(new Color(155, 111, 48));
        g.drawRect(card.x, card.y, card.width - 1, card.height - 1);
    }

    private void text(Graphics2D g, String value, int x, int baseline, float size) {
        Font old = g.getFont();
        g.setFont(font.deriveFont(Font.PLAIN, size));
        g.drawString(value, x, baseline);
        g.setFont(old);
    }

    private void centeredText(Graphics2D g, String value, Rectangle bounds, float size) {
        Font old = g.getFont();
        Font selected = font.deriveFont(Font.PLAIN, size);
        g.setFont(selected);
        int x = bounds.x + (bounds.width - g.getFontMetrics().stringWidth(value)) / 2;
        int y = bounds.y + (bounds.height - g.getFontMetrics().getHeight()) / 2
            + g.getFontMetrics().getAscent();
        g.drawString(value, x, y);
        g.setFont(old);
    }

    private void wrappedText(Graphics2D g, String value, Rectangle bounds, float size) {
        Font old = g.getFont();
        g.setFont(font.deriveFont(Font.PLAIN, size));
        StringBuilder line = new StringBuilder();
        int y = bounds.y + g.getFontMetrics().getAscent();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && g.getFontMetrics().stringWidth(candidate) > bounds.width) {
                g.drawString(line.toString(), bounds.x, y);
                line = new StringBuilder(word);
                y += g.getFontMetrics().getHeight();
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty() && y <= bounds.y + bounds.height) g.drawString(line.toString(), bounds.x, y);
        g.setFont(old);
    }

    private static Rectangle scaledRegion(Rectangle source) {
        return new Rectangle(FRAME_X + scale(source.x), FRAME_Y + scale(source.y),
            scale(source.width), scale(source.height));
    }

    private static Rectangle cardRegion(Rectangle card, Rectangle source) {
        return new Rectangle(
            card.x + source.x * card.width / CARD_SOURCE_W,
            card.y + source.y * card.height / CARD_SOURCE_H,
            Math.max(1, source.width * card.width / CARD_SOURCE_W),
            Math.max(1, source.height * card.height / CARD_SOURCE_H));
    }

    private static Rectangle inset(Rectangle source, int x, int y) {
        return new Rectangle(source.x + x, source.y + y,
            Math.max(1, source.width - x * 2), Math.max(1, source.height - y * 2));
    }

    private static BufferedImage createPressedFrame(BufferedImage source) {
        if (source == null) return null;
        BufferedImage result = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                Color.RGBtoHSB(red, green, blue, hsb);
                int adjusted = Color.HSBtoRGB(
                    hsb[0], hsb[1] * 0.22f, hsb[2] * 0.52f);
                result.setRGB(x, y, (alpha << 24) | (adjusted & 0x00ffffff));
            }
        }
        return result;
    }

    private static int scale(int value) {
        return Math.round(value * FRAME_SCALE);
    }
}

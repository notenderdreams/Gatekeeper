package com.gatekeeper;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Reuses the workbench node artwork wherever a shop product needs a thumbnail. */
final class ProductGateRenderer {
    private final LogicNodeRenderer nodeRenderer;
    private final Font font;

    ProductGateRenderer(BufferedImage nodeTexture, BufferedImage portImage, Font font) {
        nodeRenderer = new LogicNodeRenderer(nodeTexture, portImage);
        this.font = font;
    }

    void draw(Graphics2D graphics, ShopProduct product, Rectangle area) {
        int bodyHeight = LogicNodeRenderer.bodyHeight(product.inputs(), product.outputs());
        double sourceWidth = LogicNodeRenderer.BODY_WIDTH + 28.0;
        double sourceHeight = bodyHeight + 28.0;
        double scale = Math.min(area.width / sourceWidth, area.height / sourceHeight);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;

        Graphics2D gate = (Graphics2D) graphics.create();
        gate.clip(area);
        gate.translate(area.x + (area.width - drawWidth) / 2.0,
            area.y + (area.height - drawHeight) / 2.0);
        gate.scale(scale, scale);
        gate.setFont(font);
        nodeRenderer.draw(gate, 14, 14 + bodyHeight / 2,
            product.name(), product.inputs(), product.outputs());
        gate.dispose();
    }
}

package com.gatekeeper;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class ShopModelTest {
    public static void main(String[] args) {
        List<ShopProduct> catalog = ShopProduct.catalog();
        require(catalog.size() == 6, "shop should expose the requested six gates");
        require(catalog.get(0).name().equals("AND"), "AND should be first");
        require(catalog.get(5).name().equals("XNOR"), "XNOR should be sixth");

        ShopModel model = new ShopModel(catalog, 250);
        model.select(3);
        model.increaseQuantity();
        require(model.selected().name().equals("NAND"), "selection should update details");
        require(model.quantity() == 2, "plus should increase quantity");
        require(model.total() == 48, "total should be price times quantity");
        require(model.purchase(), "affordable purchase should succeed");
        require(model.balance() == 202, "purchase should reduce the balance");
        require(model.purchased(3) == 2, "purchase count should retain purchased quantity");
        require(model.quantity() == 1, "purchase should reset quantity");

        ShopModel poor = new ShopModel(catalog, 1);
        require(!poor.purchase(), "unaffordable purchase should fail");
        require(poor.balance() == 1, "failed purchase should preserve balance");

        require(new Rectangle(46, 0, 388, 270).equals(ShopRenderer.frameBounds()),
            "shop frame should fit the canvas without changing aspect ratio");
        Rectangle close = ShopRenderer.closeBounds();
        require(new Rectangle(403, 12, 22, 22).equals(close),
            "close hit region should use the frame's uniform annotation scale");
        ShopRenderer renderer = new ShopRenderer(
            null, null, null, null, GameAssets.loadPixelFont());
        require(renderer.actionAt(centerX(close), centerY(close)) == ShopRenderer.Action.CLOSE,
            "close annotation should map to close action");
        Rectangle plus = ShopRenderer.increaseBounds();
        require(renderer.actionAt(centerX(plus), centerY(plus)) == ShopRenderer.Action.INCREASE,
            "plus annotation should map to increase action");
        Rectangle first = ShopRenderer.cardBounds(0);
        require(renderer.productAt(centerX(first), centerY(first), 6) == 0,
            "first item card should be selectable");
        Rectangle sixth = ShopRenderer.cardBounds(5);
        require(renderer.productAt(centerX(sixth), centerY(sixth), 6) == 5,
            "second-row item card should be selectable");

        BufferedImage frame = GameAssets.loadRawImage("/assets/items/shop/shop.png");
        BufferedImage card = GameAssets.loadRawImage("/assets/items/shop/shop-item.png");
        BufferedImage node = GameAssets.loadRawImage("/assets/items/canvas/node-texture.png");
        BufferedImage port = GameAssets.loadRawImage("/assets/items/canvas/wire-end.png");
        require(frame != null && card != null, "shop art should load from packaged resources");
        require(node != null && port != null, "shop should reuse the canvas gate art");
        ShopRenderer assetRenderer = new ShopRenderer(
            frame, card, node, port, GameAssets.loadPixelFont());
        BufferedImage idle = render(assetRenderer, model, ShopRenderer.Action.NONE);
        BufferedImage pressed = render(assetRenderer, model, ShopRenderer.Action.ADD_TO_CART);
        Rectangle add = ShopRenderer.addBounds();
        int sampleX = centerX(add);
        int sampleY = centerY(add);
        int idleButton = idle.getRGB(sampleX, sampleY);
        int pressedButton = pressed.getRGB(sampleX, sampleY);
        require(brightness(pressedButton) < brightness(idleButton),
            "pressed button should render darker than its idle state");
        require(saturation(pressedButton) < saturation(idleButton),
            "pressed button should render less saturated than its idle state");
        require(idle.getRGB(add.x - 2, sampleY) == pressed.getRGB(add.x - 2, sampleY),
            "pressed effect should not draw outside the annotated button crop");
        System.out.println("ShopModelTest: all checks passed");
    }

    private static int centerX(Rectangle rectangle) { return rectangle.x + rectangle.width / 2; }
    private static int centerY(Rectangle rectangle) { return rectangle.y + rectangle.height / 2; }

    private static BufferedImage render(ShopRenderer renderer, ShopModel model,
                                        ShopRenderer.Action action) {
        BufferedImage image = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        renderer.draw(graphics, model, -1, -1, action);
        graphics.dispose();
        return image;
    }

    private static int brightness(int rgb) {
        return ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff);
    }

    private static int saturation(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

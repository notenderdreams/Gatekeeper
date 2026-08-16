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
        require(model.quantity() == 0, "shop should open with a neutral zero quantity");
        model.select(3);
        model.increaseQuantity();
        model.increaseQuantity();
        require(model.selected().name().equals("NAND"), "selection should update details");
        require(model.quantity() == 2, "plus should increase quantity");
        require(model.total() == -48, "buy total should show the balance deduction");
        require(model.trade(), "affordable purchase should succeed");
        require(model.balance() == 202, "purchase should reduce the balance");
        require(model.purchased(3) == 2, "purchase count should retain purchased quantity");
        require(model.quantity() == 0, "purchase should reset quantity to neutral");

        model.decreaseQuantity();
        require(model.quantity() == -1, "minus should support negative sell quantities");
        require(model.total() == 24, "sell total should show the balance addition");
        require(model.trade(), "owned inventory should be sellable");
        require(model.balance() == 226, "sale should add its value back to the balance");
        require(model.purchased(3) == 1, "sale should remove owned inventory");

        model.decreaseQuantity();
        model.decreaseQuantity();
        require(!model.trade(), "selling more than owned inventory should fail");
        require(model.balance() == 226, "failed sale should preserve balance");

        ShopModel poor = new ShopModel(catalog, 1);
        poor.increaseQuantity();
        require(!poor.trade(), "unaffordable purchase should fail");
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
        BufferedImage pressed = render(assetRenderer, model, ShopRenderer.Action.BUY_SELL);
        Rectangle add = ShopRenderer.tradeBounds();
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

        require(new Rectangle(74, 108, 38, 43).equals(InventoryRenderer.cardBounds(0)),
            "inventory should start its eight-slot row at the expected position");
        require(new Rectangle(368, 108, 38, 43).equals(InventoryRenderer.cardBounds(7)),
            "inventory should reserve room for an eighth card in the same row");
        InventoryRenderer inventoryRenderer = new InventoryRenderer(
            node, card, port, GameAssets.loadPixelFont());
        BufferedImage inventoryImage = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D inventoryGraphics = inventoryImage.createGraphics();
        inventoryRenderer.draw(inventoryGraphics, model);
        inventoryGraphics.dispose();
        require(brightness(inventoryImage.getRGB(54, 76)) > 0,
            "inventory should render its textured panel border");
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

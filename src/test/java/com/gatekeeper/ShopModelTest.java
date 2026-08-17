package com.gatekeeper;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public final class ShopModelTest {
    public static void main(String[] args) {
        List<ShopProduct> catalog = ShopProduct.catalog();
        require(catalog.size() == 8, "shop should expose all eight economy components");
        require(catalog.get(0).name().equals("AND"), "AND should be first");
        require(catalog.get(7).name().equals("IMPLY"), "IMPLY should be eighth");
        require(catalog.get(0).buyPrice() == 10 && catalog.get(0).sellPrice() == 6,
            "primitive gates should have distinct low buy and sell prices");
        require(catalog.get(6).buyPrice() == 90 && catalog.get(6).sellPrice() == 68,
            "advanced gates should carry a larger complexity premium");
        for (int index = 3; index < catalog.size(); index++) {
            ShopProduct product = catalog.get(index);
            int ingredientCost = 0;
            for (GateType part : product.parts()) {
                ingredientCost += switch (part) {
                    case AND -> catalog.get(0).buyPrice();
                    case OR -> catalog.get(1).buyPrice();
                    case NOT -> catalog.get(2).buyPrice();
                };
            }
            require(product.sellPrice() > ingredientCost,
                product.name() + " should sell above its purchased ingredient cost");
            require(product.buyPrice() > product.sellPrice(),
                product.name() + " should cost more to buy than it returns when sold");
        }

        ShopModel model = new ShopModel(catalog, 250);
        require(model.quantity() == 0, "shop should open with a neutral zero quantity");
        model.grant("AND", 5);
        model.grant("OR", 5);
        model.grant("NOT", 5);
        require(model.purchased(0) == 5 && model.purchased(1) == 5
                && model.purchased(2) == 5,
            "opening-box grants should add five primitive gates to inventory");
        require(model.consumeParts(Map.of(GateType.AND, 1, GateType.NOT, 1)),
            "crafting a player graph should consume its actual placed gates");
        require(model.purchased("AND") == 4 && model.purchased("NOT") == 4,
            "placed components should leave the bag while stored in a crafted circuit");
        model.grantParts(Map.of(GateType.AND, 1, GateType.NOT, 1));
        require(model.purchased("AND") == 5 && model.purchased("NOT") == 5,
            "unpacking a crafted circuit for editing should recover its components");
        require(model.craft("NAND", new GateType[]{GateType.AND, GateType.NOT}),
            "crafting should consume available primitive ingredients");
        require(model.purchased(0) == 4 && model.purchased(2) == 4
                && model.purchased(3) == 1,
            "crafting should replace ingredients with one finished component");
        require(model.produce("NAND", new GateType[]{GateType.AND, GateType.NOT}, 2),
            "certified production should support batches");
        require(model.purchased("AND") == 2 && model.purchased("NOT") == 2
                && model.purchased("NAND") == 3,
            "batch production should consume and grant the selected quantity atomically");
        require(!model.produce("NAND", new GateType[]{GateType.AND, GateType.NOT}, 3),
            "production should reject a batch when any primitive is short");
        require(model.purchased("AND") == 2 && model.purchased("NAND") == 3,
            "failed production should not consume partial materials");
        require(model.consume("NAND", 2), "test setup should restore one crafted NAND");

        ContractModel contracts = new ContractModel();
        require(contracts.available(2).size() == 3,
            "the first NAND, NOR, and XOR orders should appear before certification");
        ContractModel.Contract nandOrder = contracts.available(2).get(0);
        contracts.complete(nandOrder);
        require(contracts.deliveries(nandOrder) == 1,
            "verified order completion count should be tracked");
        model.select(3);
        model.increaseQuantity();
        model.increaseQuantity();
        require(model.selected().name().equals("NAND"), "selection should update details");
        require(model.quantity() == 2, "plus should increase quantity");
        require(model.total() == -64, "buy total should use the selected buy price");
        require(model.trade(), "affordable purchase should succeed");
        require(model.balance() == 186, "purchase should reduce balance by the buy price");
        require(model.purchased(3) == 3, "purchase should add to crafted inventory");
        require(model.quantity() == 0, "purchase should reset quantity to neutral");

        model.decreaseQuantity();
        require(model.quantity() == -1, "minus should support negative sell quantities");
        require(model.total() == 24, "sell total should use the selected sell price");
        require(model.trade(), "owned inventory should be sellable");
        require(model.balance() == 210, "sale should add the lower sell price to balance");
        require(model.purchased(3) == 2, "sale should remove owned inventory");

        model.decreaseQuantity();
        model.decreaseQuantity();
        model.decreaseQuantity();
        require(!model.trade(), "selling more than owned inventory should fail");
        require(model.balance() == 210, "failed sale should preserve balance");

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
        require(renderer.productAt(centerX(first), centerY(first), 8) == 0,
            "first item card should be selectable");
        Rectangle eighth = ShopRenderer.cardBounds(7);
        require(renderer.productAt(centerX(eighth), centerY(eighth), 8) == 7,
            "eighth item card should be selectable on the second row");

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

        require(new Rectangle(74, 80, 38, 43).equals(InventoryRenderer.cardBounds(0)),
            "inventory should start its eight-slot row at the expected position");
        require(new Rectangle(368, 80, 38, 43).equals(InventoryRenderer.cardBounds(7)),
            "inventory should reserve room for an eighth card in the same row");
        InventoryRenderer inventoryRenderer = new InventoryRenderer(
            node, card, port, GameAssets.loadPixelFont());
        BufferedImage inventoryImage = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D inventoryGraphics = inventoryImage.createGraphics();
        inventoryRenderer.draw(inventoryGraphics, model, 6);
        inventoryGraphics.dispose();
        require(brightness(inventoryImage.getRGB(54, 76)) > 0,
            "inventory should render its textured panel border");
        BufferedImage earlyInventory = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D earlyGraphics = earlyInventory.createGraphics();
        inventoryRenderer.draw(earlyGraphics, model, 3);
        earlyGraphics.dispose();
        Rectangle unknownCard = InventoryRenderer.cardBounds(3);
        int unknownX = centerX(unknownCard);
        int unknownY = unknownCard.y + 8;
        require(brightness(earlyInventory.getRGB(unknownX, unknownY))
                < brightness(inventoryImage.getRGB(unknownX, unknownY)),
            "inventory should hide products the story has not introduced");
        System.out.println("ShopModelTest: all checks passed");
    }

    private static int centerX(Rectangle rectangle) { return rectangle.x + rectangle.width / 2; }
    private static int centerY(Rectangle rectangle) { return rectangle.y + rectangle.height / 2; }

    private static BufferedImage render(ShopRenderer renderer, ShopModel model,
                                        ShopRenderer.Action action) {
        BufferedImage image = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        renderer.draw(graphics, model, -1, -1, action, model.products().size());
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

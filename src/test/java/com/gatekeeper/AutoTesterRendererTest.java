package com.gatekeeper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;

public final class AutoTesterRendererTest {
    private AutoTesterRendererTest() {}

    public static void main(String[] args) throws Exception {
        boolean writeSnapshot = args.length > 0 && "--snapshot".equals(args[0]);

        Rectangle frameBounds = AutoTesterRenderer.frameBounds();
        Rectangle screen = AutoTesterRenderer.screenBounds();
        Rectangle sidebar = AutoTesterRenderer.sidebarBounds();
        require(frameBounds.width == frameBounds.height, "draw bounds must preserve frame aspect ratio");
        require(frameBounds.contains(screen), "annotated screen should remain inside the frame");
        require(frameBounds.contains(sidebar), "annotated sidebar should remain inside the frame");
        require(!screen.intersects(sidebar), "annotated screen and sidebar should not overlap");

        AutoTesterRenderer renderer = new AutoTesterRenderer(null, null, null, null);
        EnumSet<AutoTesterRenderer.Action> found = EnumSet.noneOf(AutoTesterRenderer.Action.class);
        for (int y = sidebar.y; y <= sidebar.y + sidebar.height; y++) {
            for (int x = sidebar.x; x <= sidebar.x + sidebar.width; x++) {
                found.add(renderer.actionAt(x, y));
            }
        }
        found.remove(AutoTesterRenderer.Action.NONE);
        require(found.size() == 5, "all five sidebar controls should have hit regions");

        if (writeSnapshot) {
            BufferedImage frame = GameAssets.loadRawImage("/assets/items/tester/tester.png");
            BufferedImage buttons = GameAssets.loadRawImage("/assets/items/tester/tester-buttons.png");
            BufferedImage navigationButtons =
                GameAssets.loadRawImage("/assets/items/tester/tester-navigation-buttons.png");
            require(frame != null, "tester frame should load");
            require(buttons != null, "tester button sheet should load");
            require(navigationButtons != null, "tester navigation button sheet should load");
            require(navigationButtons.getWidth() == 1171
                    && navigationButtons.getHeight() == 570,
                "tester navigation button sheet must retain its tightly cropped layout");
            require(buttons.getWidth() == 1024 && buttons.getHeight() == 1536,
                "tester button sheet must retain its tightly cropped 2x3 layout");
            renderer = new AutoTesterRenderer(
                frame, buttons, navigationButtons, GameAssets.loadPixelFont());
            BufferedImage snapshot = new BufferedImage(GameConstants.W, GameConstants.H,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = snapshot.createGraphics();
            BufferedImage canvas = GameAssets.loadRawImage("/assets/items/canvas/canvas.png");
            graphics.drawImage(canvas, 0, 0, GameConstants.W, GameConstants.H, null);
            CircuitRecipe target = CircuitRecipe.all().get(0);
            renderer.draw(graphics, target, new Boolean[4], -1,
                sidebar.x + sidebar.width / 2, sidebar.y + 20,
                AutoTesterRenderer.Action.PREVIOUS, "UI READY // LOGIC OFFLINE");
            graphics.dispose();
            require((snapshot.getRGB(frameBounds.x + frameBounds.width / 2,
                frameBounds.y + frameBounds.height / 2) >>> 24) != 0,
                "overlay should render visible pixels");
            ImageIO.write(snapshot, "png", new File("/private/tmp/gatekeeper-tester-overlay.png"));
        }
        System.out.println("AutoTesterRendererTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

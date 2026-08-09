package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.gatekeeper.GameConstants.*;

final class WorldRenderer {
    private final BufferedImage bedroomBackground;
    private final BufferedImage streetBackground;
    private final BufferedImage shopBackground;
    private final BufferedImage alexSprites;
    private final BufferedImage miraSprites;
    private final BufferedImage catSprites;
    private final Rectangle[] alexFrameBounds;
    private final Rectangle[] miraFrameBounds;
    private final Rectangle[] catFrameBounds;
    private int chapter;
    private int playerX;
    private int playerY;
    private long ticks;
    private String line;
    private boolean playerMoving;
    private Facing facing;
    private int walkDistance;
    private Set<Integer> keys;
    private float uiScale;

    WorldRenderer(BufferedImage bedroomBackground, BufferedImage streetBackground,
                  BufferedImage shopBackground, BufferedImage alexSprites,
                  BufferedImage miraSprites, BufferedImage catSprites,
                  Rectangle[] alexFrameBounds, Rectangle[] miraFrameBounds,
                  Rectangle[] catFrameBounds) {
        this.bedroomBackground = bedroomBackground;
        this.streetBackground = streetBackground;
        this.shopBackground = shopBackground;
        this.alexSprites = alexSprites;
        this.miraSprites = miraSprites;
        this.catSprites = catSprites;
        this.alexFrameBounds = alexFrameBounds;
        this.miraFrameBounds = miraFrameBounds;
        this.catFrameBounds = catFrameBounds;
    }

    void update(int chapter, int playerX, int playerY, long ticks, String line,
                boolean playerMoving, Facing facing, int walkDistance,
                Set<Integer> keys, float uiScale) {
        this.chapter = chapter;
        this.playerX = playerX;
        this.playerY = playerY;
        this.ticks = ticks;
        this.line = line;
        this.playerMoving = playerMoving;
        this.facing = facing;
        this.walkDistance = walkDistance;
        this.keys = keys;
        this.uiScale = uiScale;
    }

    void drawBedroom(Graphics2D g) {
        if (bedroomBackground != null) {
            g.drawImage(bedroomBackground, 0, 0, W, H, null);
            EnvironmentArt.drawWorldVignette(g);
            if (chapter == 0) EnvironmentArt.drawInteractionGlow(g, 299, 132, YELLOW, ticks);
            drawPlayer(g, playerX, playerY, BEDROOM_PLAYER_HEIGHT);
            drawHud(g, "ALEX'S ROOM");
            if (chapter == 0 && near(299, 132)) prompt(g, "E  OPEN THE BOX");
            else if (near(205, 126)) prompt(g, chapter >= 2 ? "E  USE CRAFTING BOARD" : "E  LOOK AT DESK");
            else if (near(407, 132)) prompt(g, "E  GO OUTSIDE");

            if (showCollisions) {
                drawCollisionOverlay(g, GameScene.BEDROOM, playerX, playerY, 0);
            }
            drawCalibratedPoints(g, 0);
            return;
        }
        // Layered night-time room: wallpaper, moonlit window, floor and rug.
        g.setColor(new Color(20, 21, 34));
        g.fillRect(0, 20, W, 169);
        g.setColor(new Color(31, 31, 48));
        for (int y = 29; y < 181; y += 16) g.drawLine(0, y, W, y);
        g.setColor(new Color(45, 43, 61));
        for (int x = 12; x < W; x += 28) {
            g.fillRect(x, 37 + (x % 3) * 16, 2, 2);
            g.fillRect(x + 8, 83 + (x % 2) * 24, 1, 1);
        }
        g.setColor(new Color(74, 58, 58));
        g.fillRect(0, 181, W, 89);
        g.setColor(new Color(102, 73, 62));
        for (int y = 190; y < H; y += 13) g.drawLine(0, y, W, y);
        for (int x = -30; x < W + 30; x += 38) g.drawLine(240, 181, x, H);
        g.setColor(new Color(35, 32, 46));
        g.fillOval(143, 194, 197, 55);
        g.setColor(new Color(70, 61, 91));
        g.drawOval(147, 198, 189, 47);
        g.drawOval(169, 205, 145, 33);

        EnvironmentArt.drawBedroomWindow(g);

        // Bed with a quilt and pillow.
        g.setColor(new Color(8, 9, 14));
        g.fillRect(17, 115, 111, 59);
        g.setColor(new Color(107, 69, 78));
        g.fillRect(21, 112, 103, 52);
        g.setColor(new Color(151, 94, 102));
        for (int x = 24; x < 120; x += 16) for (int y = 128; y < 159; y += 13) g.fillRect(x, y, 7, 5);
        g.setColor(new Color(226, 207, 183));
        g.fillRect(24, 115, 35, 13);
        g.setColor(INK);
        g.drawRect(19, 108, 107, 57);
        g.fillRect(19, 165, 5, 12);
        g.fillRect(121, 165, 5, 12);

        // Work desk, lamp, pegboard and a stool.
        g.setColor(new Color(8, 9, 14));
        g.fillRect(49, 84, 89, 49);
        g.setColor(new Color(116, 79, 57));
        g.fillRect(52, 80, 84, 13);
        g.fillRect(58, 93, 6, 39);
        g.fillRect(126, 93, 6, 39);
        g.setColor(INK);
        g.drawRect(52, 80, 84, 13);
        g.setColor(chapter >= 2 ? new Color(34, 96, 85) : new Color(54, 58, 65));
        g.fillRect(75, 56, 39, 21);
        g.setColor(chapter >= 2 ? CYAN : DIM);
        g.drawRect(75, 56, 39, 21);
        g.drawLine(81, 62, 108, 62);
        g.drawLine(81, 68, 103, 68);
        GamePanel.pixelText(g, "BOARD", 77, 53, 1);
        g.setColor(YELLOW);
        g.fillRect(120, 65, 8, 4);
        g.drawLine(124, 65, 124, 78);
        g.drawLine(115, 78, 133, 78);
        g.setColor(new Color(80, 57, 48));
        g.fillRect(73, 132, 37, 8);
        g.fillRect(88, 140, 7, 26);

        // The box receives a subtle warm glow before it is opened.
        if (chapter == 0) {
            g.setColor(new Color(250, 204, 21, 32));
            g.fillRect(263, 99, 72, 58);
            g.setColor(new Color(250, 204, 21, 45));
            g.fillRect(269, 105, 60, 46);
        }
        g.setColor(new Color(126, 82, 46));
        g.fillRect(275, 112, 49, 32);
        g.setColor(new Color(169, 111, 57));
        g.fillRect(278, 115, 43, 7);
        g.setColor(INK);
        g.drawRect(275, 112, 49, 32);
        g.drawLine(275, 122, 324, 122);
        g.drawLine(299, 112, 299, 144);
        g.setColor(YELLOW);
        g.fillRect(296, 119, 7, 6);

        // Notes on the wall and the shop door.
        g.setColor(new Color(208, 198, 169));
        g.fillRect(344, 53, 44, 52);
        g.setColor(new Color(55, 48, 48));
        g.drawRect(344, 53, 44, 52);
        GamePanel.pixelText(g, "0 0 | ?", 349, 68, 1);
        GamePanel.pixelText(g, "0 1 | ?", 349, 80, 1);
        GamePanel.pixelText(g, "1 0 | ?", 349, 92, 1);
        g.setColor(new Color(48, 37, 43));
        g.fillRect(422, 64, 42, 119);
        g.setColor(new Color(91, 61, 62));
        g.fillRect(427, 70, 31, 109);
        g.setColor(INK);
        g.drawRect(422, 64, 42, 119);
        g.setColor(YELLOW);
        g.fillRect(450, 123, 4, 4);
        g.setColor(INK);
        GamePanel.pixelText(g, "OUTSIDE", 418, 57, 1);

        drawPlayer(g, playerX, playerY, BEDROOM_PLAYER_HEIGHT);
        drawHud(g, "ALEX'S ROOM");
        if (chapter == 0 && near(299, 128)) prompt(g, "E  OPEN THE BOX");
        else if (near(93, 91)) prompt(g, chapter >= 2 ? "E  USE CRAFTING BOARD" : "E  LOOK AT DESK");
        else if (near(442, 130)) prompt(g, "E  GO OUTSIDE");

        if (showCollisions) {
            drawCollisionOverlay(g, GameScene.BEDROOM, playerX, playerY, 0);
        }
        drawCalibratedPoints(g, 0);
    }

    void drawStreet(Graphics2D g) {
        int cameraX = streetCameraX();
        if (streetBackground != null) {
            g.drawImage(streetBackground, 0, 0, W, H,
                cameraX, 0, cameraX + W, H, null);
        } else {
            g.setColor(new Color(8, 19, 38));
            g.fillRect(0, 0, W, H);
            g.setColor(new Color(35, 40, 48));
            g.fillRect(0, 146, W, 64);
            g.setColor(new Color(13, 17, 25));
            g.fillRect(0, 210, W, 60);
            g.setColor(YELLOW);
            g.fillRect(62, 78, 42, 68);
            g.setColor(CYAN);
            g.fillRect(420, 78, 38, 68);
        }

        // Subtle flickering street lamp and entrance light halos
        EnvironmentArt.drawStreetLampFlicker(g, cameraX, ticks);

        // Small animated reflections keep the exterior from feeling like a still image.
        int shimmer = (int) ((ticks / 18) % 3);
        g.setColor(new Color(250, 204, 21, 45));
        g.fillRect(86 - cameraX - shimmer, 218, 27 + shimmer * 2, 1);
        g.fillRect(518 - cameraX, 219 + shimmer, 38, 1);
        g.setColor(new Color(54, 211, 224, 48));
        g.fillRect(868 - cameraX - shimmer, 215, 38 + shimmer * 2, 1);
        if ((ticks / 40) % 2 == 0) {
            g.setColor(new Color(197, 222, 230, 130));
            g.fillRect(189 - cameraX, 24, 1, 1);
            g.fillRect(685 - cameraX, 36, 1, 1);
        }

        EnvironmentArt.drawWorldVignette(g);
        drawCat(g, cameraX);
        drawPlayer(g, playerX - cameraX, STREET_GROUND_Y, STREET_PLAYER_HEIGHT);
        drawHud(g, "LANTERN STREET");
        if (Math.abs(playerX - STREET_HOME_X) < 38) {
            prompt(g, "E  ENTER HOME");
        } else if (Math.abs(playerX - STREET_SHOP_X) < 38) {
            prompt(g, "E  ENTER MIRA'S SHOP");
        } else if (Math.abs(playerX - STREET_CAT_X) < 38) {
            prompt(g, "E  PET CAT");
        }

        if (showCollisions) {
            drawCollisionOverlay(g, GameScene.STREET, playerX, playerY, cameraX);
        }
        drawCalibratedPoints(g, cameraX);
    }

    static final class CalibratedPoint {
        final int worldX;
        final int worldY;
        final int index;
        CalibratedPoint(int worldX, int worldY, int index) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.index = index;
        }
    }

    private final List<CalibratedPoint> calibratedPoints = new ArrayList<>();

    void addCalibratedPoint(int worldX, int worldY) {
        calibratedPoints.add(new CalibratedPoint(worldX, worldY, calibratedPoints.size() + 1));
        dumpCalibratedPoints();
    }

    void undoCalibratedPoint() {
        if (!calibratedPoints.isEmpty()) {
            calibratedPoints.remove(calibratedPoints.size() - 1);
            dumpCalibratedPoints();
        }
    }

    void clearCalibratedPoints() {
        calibratedPoints.clear();
        System.out.println("[POSITION MARKER] Cleared all marked points.");
    }

    void dumpCalibratedPoints() {
        System.out.println("\n==================== POSITION CALIBRATION DUMP ====================");
        System.out.println("// Total points marked: " + calibratedPoints.size());
        System.out.println("static final int[][] CALIBRATED_POINTS = {");
        for (int i = 0; i < calibratedPoints.size(); i++) {
            CalibratedPoint p = calibratedPoints.get(i);
            System.out.print("    { " + p.worldX + ", " + p.worldY + " }");
            if (i < calibratedPoints.size() - 1) System.out.print(",");
            System.out.println(" // Point #" + p.index);
        }
        System.out.println("};");
        System.out.println("===================================================================\n");
    }

    private void drawCalibratedPoints(Graphics2D g, int cameraX) {
        for (CalibratedPoint p : calibratedPoints) {
            int screenX = p.worldX - cameraX;
            int screenY = p.worldY;
            if (screenX >= -20 && screenX <= W + 20) {
                g.setColor(new Color(255, 60, 60, 230));
                g.drawOval(screenX - 7, screenY - 7, 14, 14);
                g.drawLine(screenX - 10, screenY, screenX + 10, screenY);
                g.drawLine(screenX, screenY - 10, screenX, screenY + 10);
                g.setColor(YELLOW);
                GamePanel.pixelText(g, "#" + p.index + " (" + p.worldX + "," + p.worldY + ")",
                    Math.max(5, Math.min(W - 90, screenX - 25)), Math.max(25, screenY - 10), 1);
            }
        }

        if (!calibratedPoints.isEmpty()) {
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(10, 22, 315, 14);
            g.setColor(CYAN);
            GamePanel.pixelText(g, "POSITION MARKER: " + calibratedPoints.size() + " PTS | BACKSPACE UNDO | C CLEAR | P DUMP", 14, 32, 1);
        }
    }

    private boolean showCollisions;

    void setShowCollisions(boolean showCollisions) {
        this.showCollisions = showCollisions;
    }

    private void drawCollisionOverlay(Graphics2D g, GameScene scene, int px, int py, int cameraX) {
        if (scene == GameScene.BEDROOM) {
            g.setColor(new Color(40, 200, 100, 45));
            g.fillRect(22, 132, 430, 100);
            g.setColor(new Color(60, 240, 120, 210));
            g.drawRect(22, 132, 430, 100);

            int[] polyX = {22, 148, 58, 22};
            int[] polyY = {132, 131, 206, 206};
            g.setColor(new Color(240, 60, 60, 85));
            g.fillPolygon(polyX, polyY, 4);
            g.setColor(new Color(255, 90, 90, 230));
            g.drawPolygon(polyX, polyY, 4);
            GamePanel.pixelText(g, "BLOCKED (BED/DESK)", 28, 165, 1);

            // Blocked Box Physical Footprint (Red) — calibrated: {310,141}, {331,116}, {271,84}, {272,131}
            int[] boxPolyX = {310, 331, 271, 272};
            int[] boxPolyY = {141, 116,  84, 131};
            g.setColor(new Color(240, 60, 60, 85));
            g.fillPolygon(boxPolyX, boxPolyY, 4);
            g.setColor(new Color(255, 90, 90, 230));
            g.drawPolygon(boxPolyX, boxPolyY, 4);
            GamePanel.pixelText(g, "BOX COLLISION", 270, 148, 1);

            // Interaction Triggers (Yellow)
            g.setColor(new Color(255, 230, 50, 200));
            g.drawRect(163, 86, 84, 80);
            GamePanel.pixelText(g, "BOARD/DESK", 172, 126, 1);

            g.drawRect(257, 92, 84, 80);
            GamePanel.pixelText(g, "BOX TRIGGER", 278, 112, 1);

            g.drawRect(365, 92, 84, 80);
            GamePanel.pixelText(g, "EXIT DOOR", 382, 132, 1);

            g.setColor(Color.WHITE);
            g.fillOval(px - 3, py - 3, 6, 6);
            GamePanel.pixelText(g, "FEET: (" + px + "," + py + ")", Math.max(10, px - 35), Math.min(260, py + 12), 1);
        }
        else if (scene == GameScene.STREET) {
            int screenMinX = 45 - cameraX;
            int screenMaxX = (STREET_WORLD_WIDTH - 30) - cameraX;
            g.setColor(new Color(40, 200, 100, 60));
            g.fillRect(screenMinX, 192, screenMaxX - screenMinX, 8);
            g.setColor(new Color(60, 240, 120, 220));
            g.drawRect(screenMinX, 192, screenMaxX - screenMinX, 8);

            int homeScreenX = STREET_HOME_X - cameraX;
            if (homeScreenX >= -50 && homeScreenX <= W + 50) {
                g.setColor(new Color(255, 230, 50, 200));
                g.drawRect(homeScreenX - 38, 160, 76, 50);
                GamePanel.pixelText(g, "HOME ENTRANCE", homeScreenX - 34, 185, 1);
            }

            int shopScreenX = STREET_SHOP_X - cameraX;
            if (shopScreenX >= -50 && shopScreenX <= W + 50) {
                g.setColor(new Color(255, 230, 50, 200));
                g.drawRect(shopScreenX - 38, 160, 76, 50);
                GamePanel.pixelText(g, "SHOP ENTRANCE", shopScreenX - 34, 185, 1);
            }

            int playerScreenX = px - cameraX;
            g.setColor(Color.WHITE);
            g.fillOval(playerScreenX - 3, STREET_GROUND_Y - 3, 6, 6);
            GamePanel.pixelText(g, "FEET: (" + px + "," + STREET_GROUND_Y + ")", Math.max(10, playerScreenX - 35), STREET_GROUND_Y + 12, 1);
        }
        else if (scene == GameScene.SHOP) {
            g.setColor(new Color(40, 200, 100, 45));
            g.fillRect(22, 158, 430, 74);
            g.setColor(new Color(60, 240, 120, 210));
            g.drawRect(22, 158, 430, 74);

            g.setColor(new Color(255, 230, 50, 200));
            g.drawRect(13, 134, 84, 80);
            GamePanel.pixelText(g, "EXIT DOOR", 28, 174, 1);

            g.setColor(Color.WHITE);
            g.fillOval(px - 3, py - 3, 6, 6);
            GamePanel.pixelText(g, "FEET: (" + px + "," + py + ")", Math.max(10, px - 35), Math.min(260, py + 12), 1);
        }
    }

    int streetCameraX() {
        return clamp(playerX - W / 2, 0, STREET_WORLD_WIDTH - W);
    }

    void drawShop(Graphics2D g) {
        if (shopBackground != null) {
            g.drawImage(shopBackground, 0, 0, W, H, null);
            EnvironmentArt.drawWorldVignette(g);
            drawMaskedShopkeeper(g, 240, 136, 102);
            drawPlayer(g, playerX, playerY, INDOOR_PLAYER_HEIGHT);
            drawHud(g, "MIRA'S ELECTRONICS");
            if (near(240, 160)) prompt(g, "E  TALK TO MIRA");
            else if (playerX < 45) prompt(g, "E  GO OUTSIDE");

            if (showCollisions) {
                drawCollisionOverlay(g, GameScene.SHOP, playerX, playerY, 0);
            }
            drawCalibratedPoints(g, 0);
            return;
        }
        // A warm, crowded neighborhood electronics shop.
        g.setColor(new Color(16, 28, 29));
        g.fillRect(0, 20, W, 161);
        g.setColor(new Color(24, 44, 43));
        for (int x = 0; x < W; x += 24) g.drawLine(x, 20, x, 181);
        for (int y = 32; y < 181; y += 18) g.drawLine(0, y, W, y);

        // Hanging lamps and their warm pools of light.
        for (int x : new int[]{116, 364}) {
            g.setColor(new Color(55, 58, 51));
            g.drawLine(x, 20, x, 42);
            g.setColor(new Color(237, 181, 72));
            g.fillRect(x - 13, 42, 27, 5);
            g.fillRect(x - 8, 47, 17, 3);
            g.setColor(new Color(250, 204, 21, 26));
            g.fillRect(x - 28, 50, 57, 75);
        }

        // Neon store mark.
        g.setColor(new Color(7, 13, 15));
        g.fillRect(178, 31, 124, 32);
        g.setColor(CYAN);
        g.drawRect(178, 31, 124, 32);
        GamePanel.pixelText(g, "MIRA // LOGIC", 197, 51, 1);
        g.setColor(YELLOW);
        g.fillRect(186, 40, 4, 4);
        g.fillRect(290, 40, 4, 4);

        EnvironmentArt.drawShopShelf(g, 17, 72, 157);
        EnvironmentArt.drawShopShelf(g, 306, 72, 157);

        // Checkerboard floor recedes beneath the player.
        for (int y = 181; y < H; y += 15) {
            for (int x = 0; x < W; x += 24) {
                boolean alt = ((x / 24) + (y / 15)) % 2 == 0;
                g.setColor(alt ? new Color(46, 59, 55) : new Color(31, 43, 41));
                g.fillRect(x, y, 24, 15);
            }
        }

        // Counter with component display and a live oscilloscope.
        g.setColor(new Color(9, 14, 15));
        g.fillRect(77, 142, 326, 51);
        g.setColor(new Color(103, 69, 48));
        g.fillRect(82, 136, 316, 49);
        g.setColor(new Color(151, 99, 58));
        g.fillRect(78, 134, 324, 9);
        g.setColor(INK);
        g.drawRect(78, 134, 324, 52);
        g.drawLine(82, 143, 398, 143);
        for (int x = 102; x < 378; x += 55) {
            g.setColor(new Color(43, 38, 35));
            g.fillRect(x, 151, 34, 21);
            g.setColor((x / 55) % 2 == 0 ? YELLOW : CYAN);
            g.fillRect(x + 7, 158, 4, 4);
            g.fillRect(x + 21, 164, 4, 4);
        }
        g.setColor(new Color(24, 34, 34));
        g.fillRect(322, 106, 48, 28);
        g.setColor(CYAN);
        g.drawRect(322, 106, 48, 28);
        g.drawLine(328, 122, 334, 122);
        g.drawLine(334, 122, 339, 114);
        g.drawLine(339, 114, 346, 128);
        g.drawLine(346, 128, 354, 117);
        g.drawLine(354, 117, 365, 117);

        drawShopkeeper(g, 240, 120);
        drawPlayer(g, playerX, playerY, INDOOR_PLAYER_HEIGHT);
        drawHud(g, "MIRA'S ELECTRONICS");
        if (near(240, 155)) prompt(g, "E  TALK TO MIRA");
        if (playerX < 45) prompt(g, "E  GO OUTSIDE");
    }

    private void drawHud(Graphics2D g, String location) {
        g.setColor(VOID);
        g.fillRect(0, 0, W, 20);
        g.setColor(INK);
        GamePanel.pixelText(g, location, 8, 14, 1);
        if (chapter >= 1) GamePanel.pixelText(g, "N: NOTEBOOK", 393, 14, 1);
    }

    private void drawPlayer(Graphics2D g, int x, int y) {
        drawPlayer(g, x, y, 44);
    }

    private void drawPlayer(Graphics2D g, int x, int y, int spriteHeight) {
        if (alexSprites != null && alexFrameBounds.length == 12) {
            boolean walking = line == null && playerMoving;
            int column = switch (facing) {
                case DOWN -> 0;
                case LEFT -> 1;
                case RIGHT -> 2;
                case UP -> 3;
            };
            int phase = walking ? (walkDistance / 12) % 4 : 0;
            int row = switch (phase) {
                case 1 -> 1;
                case 3 -> 2;
                default -> 0;
            };
            Rectangle frame = alexFrameBounds[row * 4 + column];
            int height = spriteHeight;
            int width = Math.max(12, Math.round(height * frame.width / (float) frame.height));
            int feetY = y + 5;
            g.setColor(new Color(3, 5, 8, 105));
            int shadowWidth = row == 0 ? 20 : 17;
            g.fillOval(x - shadowWidth / 2, feetY - 3, shadowWidth, row == 0 ? 6 : 5);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(alexSprites, x - width / 2, feetY - height,
                x - width / 2 + width, feetY,
                frame.x, frame.y, frame.x + frame.width, frame.y + frame.height, null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            return;
        }
        boolean walking = line == null && (!keys.isEmpty()) && (ticks / 8) % 2 == 0;
        g.setColor(new Color(7, 8, 12, 90));
        g.fillOval(x - 10, y + 6, 20, 6);
        g.setColor(new Color(218, 164, 124));
        g.fillRect(x - 6, y - 19, 12, 10);
        g.setColor(new Color(64, 43, 37));
        g.fillRect(x - 7, y - 22, 14, 5);
        g.fillRect(x - 7, y - 19, 3, 5);
        g.setColor(new Color(39, 52, 83));
        g.fillRect(x - 8, y - 9, 16, 13);
        g.setColor(new Color(67, 106, 164));
        g.fillRect(x - 5, y - 8, 10, 11);
        g.setColor(new Color(218, 164, 124));
        g.fillRect(x - 10, y - 7, 3, 8);
        g.fillRect(x + 8, y - 7, 3, 8);
        g.setColor(INK);
        g.fillRect(x - (walking ? 7 : 5), y + 3, 4, 8);
        g.fillRect(x + (walking ? 3 : 2), y + 3, 4, 8);
        g.setColor(RED);
        drawHeart(g, x - 2, y - 6);
    }

    private void drawShopkeeper(Graphics2D g, int x, int y) {
        if (miraSprites != null && miraFrameBounds.length == 6) {
            boolean talking = line != null && line.startsWith("MIRA|");
            int column = (int) ((ticks / (talking ? 18 : 48)) % 3);
            int row = talking ? 1 : 0;
            Rectangle frame = miraFrameBounds[row * 3 + column];
            int height = 70;
            int width = Math.max(28, Math.round(height * frame.width / (float) frame.height));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(miraSprites, x - width / 2, y - height,
                x - width / 2 + width, y,
                frame.x, frame.y, frame.x + frame.width, frame.y + frame.height, null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            return;
        }
        g.setColor(new Color(7, 9, 10, 80));
        g.fillOval(x - 18, y + 15, 36, 7);
        g.setColor(new Color(48, 32, 31));
        g.fillRect(x - 14, y - 32, 28, 19);
        g.setColor(new Color(110, 79, 57));
        g.fillRect(x - 11, y - 28, 22, 17);
        g.setColor(new Color(45, 30, 29));
        g.fillRect(x - 14, y - 31, 28, 6);
        g.fillRect(x - 14, y - 27, 4, 13);
        g.setColor(INK);
        g.fillRect(x - 8, y - 22, 3, 3);
        g.fillRect(x + 5, y - 22, 3, 3);
        g.drawLine(x - 3, y - 16, x + 4, y - 16);
        g.setColor(new Color(56, 105, 93));
        g.fillRect(x - 14, y - 11, 28, 31);
        g.setColor(new Color(204, 180, 135));
        g.fillRect(x - 8, y - 7, 16, 27);
        g.setColor(new Color(56, 105, 93));
        g.fillRect(x - 5, y - 4, 10, 24);
        g.setColor(YELLOW);
        g.fillRect(x - 2, y + 1, 4, 4);
    }

    private void drawCat(Graphics2D g, int cameraX) {
        int catWorldX = STREET_CAT_X;
        int catWorldY = 152;
        int screenX = catWorldX - cameraX;
        if (screenX + 40 < 0 || screenX - 40 > W) return;

        if (catSprites != null && catFrameBounds != null && catFrameBounds.length > 0) {
            int frameIndex = (int) ((ticks / 10) % catFrameBounds.length);
            Rectangle frame = catFrameBounds[frameIndex];
            int height = 18;
            int width = Math.max(12, Math.round(height * frame.width / (float) frame.height));

            g.setColor(new Color(3, 5, 8, 90));
            g.fillOval(screenX - width / 2, catWorldY - 2, width, 4);

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(catSprites, screenX - width / 2, catWorldY - height,
                screenX - width / 2 + width, catWorldY,
                frame.x, frame.y, frame.x + frame.width, frame.y + frame.height, null);
        }
    }

    private void drawMaskedShopkeeper(Graphics2D g, int x, int groundY, int counterFrontY) {
        Shape previousClip = g.getClip();
        g.clipRect(0, 0, W, counterFrontY);
        drawShopkeeper(g, x, groundY);
        g.setClip(previousClip);
        // A narrow warm lip reinforces the foreground plane at the mask edge.
        g.setColor(new Color(111, 73, 39, 190));
        g.fillRect(94, counterFrontY - 2, 319, 3);
        g.setColor(new Color(211, 153, 73, 150));
        g.drawLine(96, counterFrontY - 2, 410, counterFrontY - 2);
    }


    private boolean near(int x, int y) {
        return Math.abs(playerX - x) < 42 && Math.abs(playerY - y) < 40;
    }

    private void prompt(Graphics2D g, String text) {
        DialogueRenderer.drawPrompt(g, text, uiScale);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawHeart(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 5, 4);
        g.fillRect(x - 2, y + 1, 9, 3);
        g.fillRect(x, y + 4, 5, 2);
        g.fillRect(x + 1, y + 6, 3, 2);
    }
}

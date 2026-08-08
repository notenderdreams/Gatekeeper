package com.gatekeeper;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@SuppressWarnings("serial")
public final class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
    private static final int W = 480;
    private static final int H = 270;
    private static final int BOARD_SOCKET_W = 40;
    private static final int BOARD_SOCKET_H = 34;
    private static final int INDOOR_PLAYER_HEIGHT = 52;
    private static final int STREET_PLAYER_HEIGHT = 36;
    private static final String AUDIO_ROOT = "/assets/audio/game/";
    private static final String MUSIC_LOOP = "/assets/audio/music/solitude-main.wav";
    private static final int STREET_WORLD_WIDTH = 922;
    private static final int STREET_HOME_X = 93;
    private static final int STREET_SHOP_X = 870;
    private static final int STREET_GROUND_Y = 196;
    private static final Color INK = new Color(242, 241, 234);
    private static final Color VOID = new Color(10, 10, 14);
    private static final Color RED = new Color(244, 63, 74);
    private static final Color CYAN = new Color(54, 211, 224);
    private static final Color YELLOW = new Color(250, 204, 21);
    private static final Color DIM = new Color(100, 104, 112);

    private enum Scene { TITLE, CONTROLS, BEDROOM, STREET, SHOP, BOARD, NOTEBOOK, END }
    private enum Direction { DOWN, LEFT, RIGHT, UP }

    private final BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage bedroomBackground = loadImage("/assets/alex-bedroom.png");
    private final BufferedImage streetBackground = loadStreetImage("/assets/night-street-long.png");
    private final BufferedImage shopBackground = loadImage("/assets/mira-shop.png");
    private final BufferedImage alexSprites = loadRawImage("/assets/characters/alex-sprites.png");
    private final BufferedImage miraSprites = loadRawImage("/assets/characters/mira-sprites.png");
    private final Rectangle[] alexFrameBounds = buildFrameBounds(alexSprites, 4, 3);
    private final Rectangle[] miraFrameBounds = buildFrameBounds(miraSprites, 3, 2);
    private final Set<Integer> keys = new HashSet<>();
    private final Queue<String> dialogue = new ArrayDeque<>();
    private final List<CircuitRecipe> recipes = CircuitRecipe.all();
    private final boolean[] crafted = new boolean[5];
    private final CircuitModel circuit = new CircuitModel(recipes.get(0));
    private final SoundManager sound = new SoundManager();
    private Scene scene = Scene.TITLE;
    private Scene returnScene = Scene.BEDROOM;
    private String line;
    private int lineAge;
    private int chapter;
    private int titleSelection;
    private int selectedRecipe;
    private int notebookPage;
    private int playerX = 210;
    private int playerY = 157;
    private double precisePlayerX = 210;
    private double precisePlayerY = 157;
    private Direction facing = Direction.DOWN;
    private boolean playerMoving;
    private int walkDistance;
    private int lastFootstep;
    private GateType heldGate = GateType.AND;
    private String boardMessage = "Choose a gate, then place it in a socket.";
    private int boardMessageTimer;
    private int mouseX = -1;
    private int mouseY = -1;
    private long ticks;

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        Timer timer = new Timer(1000 / 60, event -> updateGame());
        timer.start();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        g.setColor(VOID);
        g.fillRect(0, 0, W, H);

        switch (scene) {
            case TITLE -> drawTitle(g);
            case CONTROLS -> drawControls(g);
            case BEDROOM -> drawBedroom(g);
            case STREET -> drawStreet(g);
            case SHOP -> drawShop(g);
            case BOARD -> drawBoard(g);
            case NOTEBOOK -> drawNotebook(g);
            case END -> drawEnding(g);
        }
        if (line != null) drawDialogue(g);
        g.dispose();

        Graphics2D out = (Graphics2D) graphics.create();
        out.setColor(Color.BLACK);
        out.fillRect(0, 0, getWidth(), getHeight());
        out.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        int dw = (int) (W * scale);
        int dh = (int) (H * scale);
        out.drawImage(canvas, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);
        out.dispose();
    }

    private void updateGame() {
        ticks++;
        sound.loop(MUSIC_LOOP);
        if (line != null) lineAge++;
        if (boardMessageTimer > 0) boardMessageTimer--;
        if (line == null && (scene == Scene.BEDROOM || scene == Scene.STREET || scene == Scene.SHOP)) {
            int speed = 2;
            boolean sideView = scene == Scene.STREET;
            int oldX = playerX;
            int oldY = playerY;
            double oldPreciseX = precisePlayerX;
            double oldPreciseY = precisePlayerY;
            int axisX = 0;
            int axisY = 0;
            if (keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_A)) {
                axisX--;
                facing = Direction.LEFT;
            }
            if (keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D)) {
                axisX++;
                facing = Direction.RIGHT;
            }
            if (!sideView && (keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_W))) {
                axisY--;
                facing = Direction.UP;
            }
            if (!sideView && (keys.contains(KeyEvent.VK_DOWN) || keys.contains(KeyEvent.VK_S))) {
                axisY++;
                facing = Direction.DOWN;
            }

            double vectorLength = Math.hypot(axisX, axisY);
            if (vectorLength > 0) {
                double movementScale = speed / vectorLength;
                precisePlayerX += axisX * movementScale;
                precisePlayerY += axisY * movementScale;
            }
            int minX = sideView ? 45 : 22;
            int maxX = sideView ? STREET_WORLD_WIDTH - 30 : 452;
            int minY = scene == Scene.SHOP ? 158 : 132;
            precisePlayerX = clamp(precisePlayerX, minX, maxX);
            if (sideView) precisePlayerY = STREET_GROUND_Y;
            else precisePlayerY = clamp(precisePlayerY, minY, 232);
            if (scene == Scene.BEDROOM) {
                double targetX = precisePlayerX;
                double targetY = precisePlayerY;
                precisePlayerX = oldPreciseX;
                precisePlayerY = oldPreciseY;
                if (!bedroomBlocked(targetX, precisePlayerY)) precisePlayerX = targetX;
                if (!bedroomBlocked(precisePlayerX, targetY)) precisePlayerY = targetY;
            }
            playerX = (int) Math.round(precisePlayerX);
            playerY = (int) Math.round(precisePlayerY);
            playerMoving = playerX != oldX || playerY != oldY;
            if (playerMoving) {
                walkDistance += Math.max(1, (int) Math.round(
                    Math.hypot(precisePlayerX - oldPreciseX, precisePlayerY - oldPreciseY)));
                int footstep = walkDistance / 18;
                if (footstep > lastFootstep) {
                    lastFootstep = footstep;
                    sound.play(AUDIO_ROOT + "footstep-" + ((footstep - 1) % 4 + 1) + ".wav");
                }
            }
        } else {
            playerMoving = false;
        }
        repaint();
    }

    private void drawTitle(Graphics2D g) {
        if (bedroomBackground != null) g.drawImage(bedroomBackground, 0, 0, W, H, null);
        g.setColor(new Color(3, 6, 12, 188));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(4, 7, 12, 150));
        g.fillRect(0, 0, W, 25);
        g.fillRect(0, 245, W, 25);

        // A live circuit motif frames the logo.
        boolean pulseA = (ticks / 36) % 2 == 0;
        boolean pulseB = (ticks / 54) % 2 == 0;
        drawTitleTrace(g, 24, 50, 117, 50, pulseA);
        drawTitleTrace(g, 363, 50, 456, 50, pulseB);
        drawTitleTrace(g, 34, 119, 121, 119, pulseB);
        drawTitleTrace(g, 359, 119, 447, 119, pulseA);
        g.setColor(pulseA ? CYAN : DIM);
        g.fillOval(19, 46, 8, 8);
        g.setColor(pulseB ? YELLOW : DIM);
        g.fillOval(453, 46, 8, 8);

        g.setColor(new Color(0, 0, 0, 180));
        pixelText(g, "GATEKEEPER", 153, 83, 3);
        g.setColor(INK);
        pixelText(g, "GATEKEEPER", 150, 80, 3);
        g.setColor(CYAN);
        pixelText(g, "A  L O G I C  T A L E", 168, 104, 1);
        g.setColor(YELLOW);
        g.fillRect(146, 112, 188, 2);
        g.setColor(DIM);
        pixelText(g, "EVERY CIRCUIT MAKES A PROMISE", 151, 128, 1);

        drawTitleButton(g, 145, 151, 190, 28, "START STORY", 0);
        drawTitleButton(g, 145, 184, 190, 28, "HOW TO PLAY", 1);

        Direction previousFacing = facing;
        facing = Direction.DOWN;
        drawPlayer(g, 394, 225);
        facing = previousFacing;
        g.setColor(new Color(0, 0, 0, 125));
        g.fillRect(355, 231, 80, 3);

        g.setColor((ticks / 35) % 2 == 0 ? INK : DIM);
        pixelText(g, "W/S OR MOUSE  •  ENTER", 169, 231, 1);
        g.setColor(new Color(91, 106, 109));
        pixelText(g, "JAVA 2D  //  BUILD 01", 12, 260, 1);
    }

    private void drawControls(Graphics2D g) {
        if (bedroomBackground != null) g.drawImage(bedroomBackground, 0, 0, W, H, null);
        g.setColor(new Color(3, 7, 11, 211));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(10, 17, 21));
        g.fillRect(54, 24, 372, 222);
        g.setColor(new Color(67, 129, 111));
        g.drawRect(54, 24, 372, 222);
        g.setColor(new Color(152, 95, 47));
        g.drawRect(58, 28, 364, 214);

        g.setColor(INK);
        pixelText(g, "HOW TO PLAY", 148, 54, 2);
        g.setColor(YELLOW);
        g.fillRect(83, 65, 314, 2);

        drawControlSection(g, 77, 79, "EXPLORE",
            "WASD / ARROWS", "Move Alex",
            "E / ENTER", "Interact and talk",
            "N", "Open the notebook");
        drawControlSection(g, 250, 79, "WORKBENCH",
            "MOUSE", "Place gates and test",
            "1 / 2 / 3", "Select AND / OR / NOT",
            "A / B", "Toggle circuit inputs");

        g.setColor(new Color(18, 39, 39));
        g.fillRect(77, 185, 320, 25);
        g.setColor(CYAN);
        pixelText(g, "TIP", 88, 201, 1);
        g.setColor(INK);
        pixelText(g, "Record all four input combinations.", 121, 201, 1);

        boolean hovered = inside(mouseX, mouseY, 164, 217, 152, 22);
        g.setColor(hovered ? new Color(71, 63, 26) : new Color(27, 35, 35));
        g.fillRect(164, 217, 152, 22);
        g.setColor(hovered ? YELLOW : INK);
        g.drawRect(164, 217, 152, 22);
        pixelText(g, "< BACK TO TITLE", 192, 232, 1);
    }

    private void drawTitleTrace(Graphics2D g, int x1, int y1, int x2, int y2, boolean powered) {
        int middle = (x1 + x2) / 2;
        g.setColor(powered ? CYAN : new Color(48, 75, 75));
        g.setStroke(new BasicStroke(powered ? 2 : 1));
        g.drawLine(x1, y1, middle, y1);
        g.drawLine(middle, y1, middle, y1 + 8);
        g.drawLine(middle, y1 + 8, x2, y1 + 8);
        g.setStroke(new BasicStroke(1));
        g.fillRect(middle - 1, y1 - 1, 3, 3);
    }

    private void drawTitleButton(Graphics2D g, int x, int y, int width, int height,
                                 String label, int selection) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        boolean selected = titleSelection == selection;
        g.setColor(selected || hovered ? new Color(49, 67, 61, 230) : new Color(8, 15, 18, 215));
        g.fillRect(x, y, width, height);
        g.setColor(selected || hovered ? YELLOW : new Color(78, 100, 97));
        g.drawRect(x, y, width, height);
        if (selected || hovered) {
            g.fillRect(x + 7, y + 8, 4, 12);
            g.fillRect(x + width - 11, y + 8, 4, 12);
        }
        g.setColor(selected || hovered ? INK : DIM);
        int textX = x + (width - label.length() * 6) / 2;
        pixelText(g, label, textX, y + 18, 1);
    }

    private void drawControlSection(Graphics2D g, int x, int y, String heading,
                                    String key1, String action1,
                                    String key2, String action2,
                                    String key3, String action3) {
        g.setColor(CYAN);
        pixelText(g, heading, x, y, 1);
        String[] keys = {key1, key2, key3};
        String[] actions = {action1, action2, action3};
        for (int i = 0; i < 3; i++) {
            int rowY = y + 22 + i * 27;
            g.setColor(new Color(53, 62, 63));
            g.fillRect(x, rowY - 11, 145, 24);
            g.setColor(YELLOW);
            pixelText(g, keys[i], x + 7, rowY, 1);
            g.setColor(INK);
            pixelText(g, actions[i], x + 7, rowY + 11, 1);
        }
    }

    private void drawBedroom(Graphics2D g) {
        if (bedroomBackground != null) {
            g.drawImage(bedroomBackground, 0, 0, W, H, null);
            drawWorldVignette(g);
            if (chapter == 0) drawInteractionGlow(g, 299, 132, YELLOW);
            drawPlayer(g, playerX, playerY, INDOOR_PLAYER_HEIGHT);
            drawHud(g, "ALEX'S ROOM");
            if (chapter == 0 && near(299, 132)) prompt(g, "E  OPEN THE BOX");
            else if (near(205, 126)) prompt(g, chapter >= 2 ? "E  USE CRAFTING BOARD" : "E  LOOK AT DESK");
            else if (near(407, 132)) prompt(g, "E  GO OUTSIDE");
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

        drawBedroomWindow(g);

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
        pixelText(g, "BOARD", 77, 53, 1);
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
        pixelText(g, "0 0 | ?", 349, 68, 1);
        pixelText(g, "0 1 | ?", 349, 80, 1);
        pixelText(g, "1 0 | ?", 349, 92, 1);
        g.setColor(new Color(48, 37, 43));
        g.fillRect(422, 64, 42, 119);
        g.setColor(new Color(91, 61, 62));
        g.fillRect(427, 70, 31, 109);
        g.setColor(INK);
        g.drawRect(422, 64, 42, 119);
        g.setColor(YELLOW);
        g.fillRect(450, 123, 4, 4);
        g.setColor(INK);
        pixelText(g, "OUTSIDE", 418, 57, 1);

        drawPlayer(g, playerX, playerY, INDOOR_PLAYER_HEIGHT);
        drawHud(g, "ALEX'S ROOM");
        if (chapter == 0 && near(299, 128)) prompt(g, "E  OPEN THE BOX");
        else if (near(93, 91)) prompt(g, chapter >= 2 ? "E  USE CRAFTING BOARD" : "E  LOOK AT DESK");
        else if (near(442, 130)) prompt(g, "E  GO OUTSIDE");
    }

    private void drawStreet(Graphics2D g) {
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

        drawWorldVignette(g);
        drawPlayer(g, playerX - cameraX, STREET_GROUND_Y, STREET_PLAYER_HEIGHT);
        drawHud(g, "LANTERN STREET");
        if (Math.abs(playerX - STREET_HOME_X) < 38) {
            prompt(g, "E  ENTER HOME");
        } else if (Math.abs(playerX - STREET_SHOP_X) < 38) {
            prompt(g, "E  ENTER MIRA'S SHOP");
        }
    }

    private int streetCameraX() {
        return clamp(playerX - W / 2, 0, STREET_WORLD_WIDTH - W);
    }

    private void drawShop(Graphics2D g) {
        if (shopBackground != null) {
            g.drawImage(shopBackground, 0, 0, W, H, null);
            drawWorldVignette(g);
            drawMaskedShopkeeper(g, 240, 136, 102);
            drawPlayer(g, playerX, playerY, INDOOR_PLAYER_HEIGHT);
            drawHud(g, "MIRA'S ELECTRONICS");
            if (near(240, 160)) prompt(g, "E  TALK TO MIRA");
            else if (playerX < 45) prompt(g, "E  GO OUTSIDE");
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
        pixelText(g, "MIRA // LOGIC", 197, 51, 1);
        g.setColor(YELLOW);
        g.fillRect(186, 40, 4, 4);
        g.fillRect(290, 40, 4, 4);

        drawShopShelf(g, 17, 72, 157);
        drawShopShelf(g, 306, 72, 157);

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

    private void drawBoard(Graphics2D g) {
        // The workbench sits on Alex's scarred wooden desk.
        g.setColor(new Color(22, 13, 12));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(53, 30, 22));
        for (int y = 5; y < H; y += 13) g.drawLine(0, y, W, y + 3);
        g.setColor(new Color(3, 5, 7, 150));
        g.fillRect(8, 9, 469, 259);
        g.setColor(new Color(91, 55, 34));
        g.fillRect(3, 3, 472, 262);
        g.setColor(new Color(190, 126, 60));
        g.drawRect(3, 3, 471, 261);
        g.setColor(new Color(48, 29, 24));
        g.drawRect(7, 7, 463, 253);
        g.setColor(new Color(7, 27, 28));
        g.fillRect(10, 10, 458, 248);
        g.setColor(new Color(18, 59, 54));
        for (int x = 14; x < 468; x += 15) {
            for (int y = 14; y < 259; y += 15) g.fillRect(x, y, 1, 1);
        }
        // Etched copper traces around the board's edge.
        g.setColor(new Color(116, 72, 37));
        g.drawLine(13, 53, 13, 228);
        g.drawLine(13, 228, 294, 228);
        g.drawLine(467, 53, 467, 228);
        g.setColor(new Color(201, 139, 65));
        for (int[] screw : new int[][]{{8, 8}, {466, 8}, {8, 256}, {466, 256}}) {
            g.fillRect(screw[0], screw[1], 5, 5);
            g.setColor(new Color(61, 39, 32));
            g.drawLine(screw[0] + 1, screw[1] + 2, screw[0] + 3, screw[1] + 2);
            g.setColor(new Color(201, 139, 65));
        }

        g.setColor(new Color(5, 12, 15));
        g.fillRect(12, 12, 456, 39);
        g.setColor(new Color(21, 48, 46));
        g.drawLine(13, 50, 467, 50);
        g.setColor(INK);
        pixelText(g, "LOGIC WORKBENCH", 18, 25, 1);
        g.setColor(CYAN);
        pixelText(g, "N GUIDE", 18, 42, 1);
        g.setColor(DIM);
        pixelText(g, "ESC EXIT", 418, 25, 1);

        int available = chapter >= 3 ? 5 : 3;
        for (int i = 0; i < available; i++) {
            int x = 126 + i * 66;
            boolean selected = i == selectedRecipe;
            boolean hovered = isHovered(x, 29, 59, 20);
            g.setColor(selected ? new Color(83, 67, 25)
                : hovered ? new Color(28, 63, 58) : new Color(17, 31, 34));
            g.fillRect(x, 29, 59, 20);
            g.setColor(crafted[i] ? CYAN : selected ? YELLOW : hovered ? INK : DIM);
            g.drawRect(x, 30, 59, 18);
            pixelText(g, (crafted[i] ? "*" : " ") + recipes.get(i).name, x + 4, 43, 1);
        }

        CircuitRecipe recipe = circuit.recipe();
        drawPanel(g, 13, 55, 345, 116);
        g.setColor(new Color(20, 57, 51));
        for (int x = 22; x < 350; x += 12) {
            for (int y = 72; y < 165; y += 12) g.fillRect(x, y, 2, 2);
        }
        g.setColor(INK);
        pixelText(g, recipe.name, 20, 67, 1);
        g.setColor(DIM);
        pixelText(g, recipe.subtitle, 62, 67, 1);
        g.setColor(crafted[selectedRecipe] ? CYAN : YELLOW);
        pixelText(g, crafted[selectedRecipe] ? "BUILT" : "ACTIVE", 305, 67, 1);
        drawSwitch(g, 20, 81, "A", circuit.inputA());
        drawSwitch(g, 20, 116, "B", circuit.inputB());

        int[][] layout = socketLayout(recipe);
        boolean[] nodeValues = circuit.nodeValues();
        drawCircuitWires(g, recipe, layout, nodeValues);
        for (int i = 0; i < recipe.slotCount(); i++) {
            drawSocket(g, layout[i][0], layout[i][1], i, circuit.placed()[i], nodeValues[i]);
        }
        int[] last = layout[recipe.slotCount() - 1];
        drawRoutedWire(g, last[0] + BOARD_SOCKET_W, last[1] + BOARD_SOCKET_H / 2,
            337, 112, circuit.output());
        g.setColor(circuit.output() ? new Color(32, 100, 99) : new Color(25, 32, 34));
        g.fillOval(334, 101, 22, 22);
        g.setColor(circuit.output() ? CYAN : DIM);
        g.drawOval(334, 101, 21, 21);
        g.fillOval(340, 107, 10, 10);
        g.setColor(INK);
        pixelText(g, "OUT", 336, 134, 1);

        drawTruthTable(g);
        drawGatePalette(g);
        drawBoardButtons(g);
        g.setColor(new Color(5, 12, 15));
        g.fillRect(13, 232, 454, 27);
        g.setColor(new Color(37, 77, 69));
        g.drawLine(14, 232, 466, 232);
        g.setColor(boardMessageTimer > 0 ? YELLOW : CYAN);
        g.fillRect(18, 239, 4, 12);
        String status = boardMessageTimer > 0 ? boardMessage
            : heldGate.label + " selected — " + heldGate.hint;
        pixelText(g, status, 28, 249, 1);
    }

    private void drawTruthTable(Graphics2D g) {
        CircuitRecipe r = circuit.recipe();
        int x = 366, y = 61;
        drawPanel(g, 362, 55, 105, 116);
        g.setColor(INK);
        pixelText(g, "TARGET / LIVE", x + 2, y + 8, 1);
        g.setColor(DIM);
        pixelText(g, "A B | WANT GOT", x + 5, y + 23, 1);
        g.drawLine(x + 5, y + 28, x + 92, y + 28);
        int currentRow = (circuit.inputA() ? 2 : 0) + (circuit.inputB() ? 1 : 0);
        for (int row = 0; row < 4; row++) {
            int yy = y + 42 + row * 14;
            boolean a = row >= 2;
            boolean b = row % 2 == 1;
            Boolean seen = circuit.observations()[row];
            String value = seen == null ? "?" : bit(seen);
            if (row == currentRow) {
                g.setColor(new Color(27, 64, 59));
                g.fillRect(x + 4, yy - 10, 92, 13);
            }
            g.setColor(seen == null ? DIM : (seen == r.truth[row] ? CYAN : RED));
            pixelText(g, bit(a) + " " + bit(b) + " |  " + bit(r.truth[row]) + "    " + value, x + 9, yy, 1);
        }
        int recorded = 0;
        for (Boolean observation : circuit.observations()) if (observation != null) recorded++;
        g.setColor(recorded == 4 ? CYAN : DIM);
        pixelText(g, "REC " + recorded + "/4", x + 52, y + 105, 1);
    }

    private void drawGatePalette(Graphics2D g) {
        drawPanel(g, 13, 176, 281, 52);
        g.setColor(INK);
        pixelText(g, "PARTS BIN", 18, 187, 1);
        GateType[] gates = GateType.values();
        for (int i = 0; i < gates.length; i++) {
            int x = 20 + i * 93;
            boolean selected = heldGate == gates[i];
            boolean hovered = isHovered(x, 190, 80, 33);
            g.setColor(selected ? new Color(76, 63, 25)
                : hovered ? new Color(28, 61, 56) : new Color(14, 27, 30));
            g.fillRect(x, 190, 80, 33);
            g.setColor(selected ? YELLOW : hovered ? INK : DIM);
            g.drawRect(x, 190, 80, 33);
            if (selected) g.fillRect(x + 2, 192, 2, 29);
            drawGate(g, x + 5, 192, gates[i], false);
            pixelText(g, (i + 1) + " " + gates[i].label, x + 40, 211, 1);
        }
    }

    private void drawBoardButtons(Graphics2D g) {
        boolean tester = chapter >= 3;
        drawPanel(g, 302, 176, 165, 52);
        boolean recordHover = isHovered(310, 187, 72, 36);
        g.setColor(recordHover ? new Color(102, 78, 24) : new Color(59, 48, 23));
        g.fillRect(310, 187, 72, 36);
        g.setColor(YELLOW);
        g.drawRect(310, 187, 72, 36);
        pixelText(g, tester ? "R  AUTO" : "R RECORD", 318, 202, 1);
        pixelText(g, tester ? "TEST" : "THIS ROW", 320, 214, 1);
        boolean verifyHover = isHovered(390, 187, 70, 36);
        g.setColor(verifyHover ? new Color(23, 85, 83) : new Color(18, 52, 54));
        g.fillRect(390, 187, 70, 36);
        g.setColor(CYAN);
        g.drawRect(390, 187, 70, 36);
        pixelText(g, tester ? "T RUN KIT" : "T VERIFY", 396, 208, 1);
    }

    private void drawPanel(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(1, 5, 7, 125));
        g.fillRect(x + 3, y + 3, width, height);
        g.setColor(new Color(5, 13, 16, 235));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(52, 103, 88));
        g.drawRect(x, y, width, height);
        g.setColor(new Color(14, 39, 38));
        g.drawRect(x + 2, y + 2, width - 4, height - 4);
        g.setColor(new Color(184, 119, 52));
        g.fillRect(x + 5, y + 5, 3, 3);
        g.fillRect(x + width - 7, y + 5, 3, 3);
    }

    private void drawNotebook(Graphics2D g) {
        // Desk, leather cover, page shadows, and slightly uneven paper edges.
        g.setColor(new Color(25, 15, 14));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(57, 32, 23));
        for (int y = 6; y < H; y += 14) g.drawLine(0, y, W, y + 4);
        g.setColor(new Color(3, 4, 6, 145));
        g.fillRect(31, 20, 425, 238);
        g.setColor(new Color(76, 39, 31));
        g.fillRect(25, 13, 430, 239);
        g.setColor(new Color(143, 81, 47));
        g.drawRect(25, 13, 429, 238);
        g.setColor(new Color(233, 222, 186));
        g.fillRect(32, 18, 204, 228);
        g.setColor(new Color(224, 211, 175));
        g.fillRect(244, 18, 204, 228);
        g.setColor(new Color(194, 177, 143));
        g.drawRect(32, 18, 203, 227);
        g.drawRect(244, 18, 203, 227);

        // Faint ruled paper with red notebook margins.
        for (int y = 47; y < 237; y += 13) {
            g.setColor(new Color(169, 179, 167));
            g.drawLine(39, y, 229, y);
            g.drawLine(251, y, 441, y);
        }
        g.setColor(new Color(189, 111, 99));
        g.drawLine(55, 24, 55, 239);
        g.drawLine(263, 24, 263, 239);

        // Dark center crease and brass binding loops.
        g.setColor(new Color(91, 72, 58));
        g.fillRect(235, 20, 9, 224);
        g.setColor(new Color(39, 29, 27));
        g.drawLine(239, 20, 239, 244);
        for (int y = 34; y < 235; y += 25) {
            g.setColor(new Color(181, 126, 62));
            g.drawOval(233, y, 12, 7);
            g.setColor(new Color(91, 58, 38));
            g.drawLine(236, y + 4, 242, y + 4);
        }

        g.setColor(new Color(52, 44, 40));
        pixelText(g, "ALEX'S LOGIC NOTES", 67, 35, 1);
        g.setColor(new Color(110, 71, 57));
        pixelText(g, "THE THREE BUILDING BLOCKS", 67, 47, 1);
        drawNotebookGateCard(g, GateType.AND, 61, 55,
            "BOTH must be 1", "00:0  01:0  10:0  11:1");
        drawNotebookGateCard(g, GateType.OR, 61, 111,
            "EITHER can be 1", "00:0  01:1  10:1  11:1");
        drawNotebookGateCard(g, GateType.NOT, 61, 167,
            "FLIPS the signal", "0 -> 1       1 -> 0");

        int available = chapter >= 3 ? 5 : 3;
        notebookPage = clamp(notebookPage, 0, available - 1);
        CircuitRecipe recipe = recipes.get(notebookPage);
        g.setColor(new Color(110, 71, 57));
        pixelText(g, "PROJECT " + (notebookPage + 1) + " / " + available, 270, 34, 1);
        g.setColor(new Color(43, 39, 37));
        pixelText(g, recipe.name, 270, 51, 1);
        g.setColor(crafted[notebookPage] ? new Color(30, 116, 104) : new Color(159, 91, 48));
        pixelText(g, crafted[notebookPage] ? "[ COMPLETE ]" : "[ TO BUILD ]", 360, 51, 1);
        g.setColor(new Color(91, 72, 60));
        drawWrapped(g, recipe.subtitle, 270, 65, 27);

        drawNotebookTruthTable(g, recipe, 270, 91);
        drawNotebookPlan(g, recipe, 337, 91);

        g.setColor(new Color(79, 59, 51));
        g.drawRect(270, 211, 22, 19);
        g.drawRect(416, 211, 22, 19);
        pixelText(g, "<", 278, 225, 1);
        pixelText(g, ">", 424, 225, 1);
        for (int i = 0; i < available; i++) {
            int x = 316 + i * 16;
            g.setColor(i == notebookPage ? new Color(153, 80, 50) : new Color(124, 106, 85));
            if (crafted[i]) g.fillRect(x - 2, 216, 11, 11);
            else g.drawRect(x - 2, 216, 11, 11);
            g.setColor(i == notebookPage ? new Color(245, 232, 197) : new Color(66, 56, 50));
            pixelText(g, Integer.toString(i + 1), x, 225, 1);
        }
        g.setColor(new Color(83, 67, 58));
        pixelText(g, "ARROWS: PAGE   N / ESC: CLOSE", 270, 241, 1);
    }

    private void drawEnding(Graphics2D g) {
        g.setColor(INK);
        pixelText(g, "THE SIGNAL IS CLEAR.", 131, 78, 2);
        g.setColor(CYAN);
        pixelText(g, "Mira pins Alex's circuits above the counter.", 102, 119, 1);
        pixelText(g, "Tomorrow, the notebook has harder pages.", 111, 136, 1);
        g.setColor(YELLOW);
        pixelText(g, "But tonight, every little light is on.", 119, 169, 1);
        g.setColor(RED);
        drawHeart(g, 235, 194);
        g.setColor(DIM);
        pixelText(g, "ENTER: begin again", 178, 238, 1);
    }

    private void drawHud(Graphics2D g, String location) {
        g.setColor(VOID);
        g.fillRect(0, 0, W, 20);
        g.setColor(INK);
        pixelText(g, location, 8, 14, 1);
        if (chapter >= 1) pixelText(g, "N: NOTEBOOK", 393, 14, 1);
    }

    private void drawWorldVignette(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 42));
        g.fillRect(0, 20, 9, H - 20);
        g.fillRect(W - 9, 20, 9, H - 20);
        g.fillRect(0, H - 10, W, 10);
    }

    private void drawInteractionGlow(Graphics2D g, int x, int y, Color color) {
        int pulse = 3 + (int) ((ticks / 12) % 3);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        g.fillOval(x - 25 - pulse, y - 12 - pulse, 50 + pulse * 2, 25 + pulse * 2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g.drawOval(x - 20 - pulse, y - 9 - pulse, 40 + pulse * 2, 19 + pulse * 2);
    }

    private void drawBedroomWindow(Graphics2D g) {
        g.setColor(new Color(8, 10, 18));
        g.fillRect(160, 39, 111, 74);
        g.setColor(new Color(44, 65, 92));
        g.fillRect(166, 45, 99, 62);
        g.setColor(new Color(16, 23, 39));
        g.fillRect(170, 49, 91, 54);
        g.setColor(new Color(220, 223, 190));
        g.fillRect(235, 55, 12, 12);
        g.setColor(new Color(16, 23, 39));
        g.fillRect(230, 52, 12, 12);
        g.setColor(INK);
        g.fillRect(181, 58, 2, 2);
        g.fillRect(211, 72, 2, 2);
        g.fillRect(253, 82, 1, 1);
        g.setColor(new Color(29, 39, 55));
        for (int x = 171; x < 260; x += 13) {
            int height = 8 + (x % 17);
            g.fillRect(x, 103 - height, 11, height);
            g.setColor(YELLOW);
            if (x % 2 == 1) g.fillRect(x + 3, 98 - height / 2, 2, 2);
            g.setColor(new Color(29, 39, 55));
        }
        g.setColor(new Color(91, 72, 101));
        g.fillRect(151, 37, 12, 78);
        g.fillRect(268, 37, 12, 78);
        g.setColor(new Color(130, 91, 120));
        g.drawLine(157, 42, 157, 108);
        g.drawLine(274, 42, 274, 108);
        g.setColor(INK);
        g.drawRect(160, 39, 111, 74);
        g.drawLine(215, 42, 215, 110);
        g.drawLine(163, 77, 268, 77);
    }

    private void drawShopShelf(Graphics2D g, int x, int top, int width) {
        g.setColor(new Color(8, 13, 14));
        g.fillRect(x, top, width, 60);
        g.setColor(new Color(94, 68, 48));
        g.fillRect(x - 3, top - 3, width + 6, 5);
        g.fillRect(x - 3, top + 27, width + 6, 5);
        g.fillRect(x - 3, top + 58, width + 6, 5);
        for (int bx = x + 7; bx < x + width - 20; bx += 31) {
            int colorIndex = (bx / 31) % 3;
            g.setColor(colorIndex == 0 ? new Color(105, 73, 59)
                : colorIndex == 1 ? new Color(54, 87, 81) : new Color(78, 72, 100));
            g.fillRect(bx, top + 7, 24, 17);
            g.fillRect(bx, top + 38, 24, 16);
            g.setColor(colorIndex == 1 ? CYAN : YELLOW);
            g.fillRect(bx + 5, top + 12, 3, 3);
            g.fillRect(bx + 15, top + 44, 3, 3);
        }
        g.setColor(INK);
        g.drawRect(x, top, width, 60);
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

    private void drawSocket(Graphics2D g, int x, int y, int number, GateType gate, boolean powered) {
        boolean hovered = isHovered(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(new Color(1, 4, 5, 155));
        g.fillRect(x + 3, y + 3, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(powered ? new Color(17, 70, 67)
            : hovered ? new Color(42, 55, 51) : new Color(12, 23, 26));
        g.fillRect(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(hovered ? YELLOW : gate == null ? DIM : powered ? CYAN : INK);
        g.drawRect(x, y, BOARD_SOCKET_W, BOARD_SOCKET_H);
        g.setColor(hovered ? YELLOW : DIM);
        pixelText(g, "G" + (number + 1), x + 2, y + 9, 1);
        g.fillRect(x - 2, y + 8, 3, 4);
        g.fillRect(x - 2, y + 22, 3, 4);
        g.fillRect(x + BOARD_SOCKET_W, y + 15, 3, 4);
        if (gate == null) {
            g.setColor(hovered ? YELLOW : DIM);
            pixelText(g, "+", x + 17, y + 25, 1);
        } else {
            g.setColor(powered ? CYAN : INK);
            drawGate(g, x + 3, y + 6, gate, powered);
        }
    }

    private void drawGate(Graphics2D g, int x, int y, GateType gate, boolean powered) {
        g.setColor(powered ? CYAN : g.getColor());
        if (gate == GateType.NOT) {
            int[] xs = {x + 5, x + 5, x + 29};
            int[] ys = {y + 4, y + 24, y + 14};
            g.drawPolygon(xs, ys, 3);
            g.drawOval(x + 29, y + 11, 5, 5);
        } else {
            g.drawRect(x + 3, y + 4, 29, 21);
            if (gate == GateType.OR) g.drawLine(x + 3, y + 4, x + 10, y + 14);
            pixelText(g, gate == GateType.AND ? "&" : ">", x + 14, y + 19, 1);
        }
    }

    private void drawSwitch(Graphics2D g, int x, int y, String name, boolean on) {
        boolean hovered = isHovered(x, y, 64, 20);
        g.setColor(hovered ? new Color(36, 61, 56) : new Color(12, 25, 28));
        g.fillRect(x - 2, y - 1, 62, 20);
        g.setColor(hovered ? YELLOW : INK);
        pixelText(g, name, x, y + 14, 1);
        g.drawRect(x + 14, y, 23, 18);
        g.setColor(on ? CYAN : DIM);
        g.fillRect(on ? x + 27 : x + 17, y + 4, 7, 10);
        g.setColor(on ? CYAN : hovered ? YELLOW : DIM);
        pixelText(g, on ? "1" : "0", x + 43, y + 14, 1);
    }

    private void drawWire(Graphics2D g, int x1, int y1, int x2, int y2, boolean on) {
        g.setColor(new Color(1, 5, 6, 190));
        g.setStroke(new BasicStroke(4));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1));
    }

    private void drawSourceWire(Graphics2D g, int source, int targetX, int targetY,
                                int[][] layout, boolean[] nodeValues) {
        int sourceX;
        int sourceY;
        boolean powered;
        if (source == CircuitRecipe.INPUT_A) {
            sourceX = 57;
            sourceY = 94;
            powered = circuit.inputA();
        } else if (source == CircuitRecipe.INPUT_B) {
            sourceX = 57;
            sourceY = 129;
            powered = circuit.inputB();
        } else {
            sourceX = layout[source][0] + BOARD_SOCKET_W;
            sourceY = layout[source][1] + BOARD_SOCKET_H / 2;
            powered = nodeValues[source];
        }
        drawRoutedWire(g, sourceX, sourceY, targetX, targetY, powered);
    }

    private void drawCircuitWires(Graphics2D g, CircuitRecipe recipe,
                                  int[][] layout, boolean[] nodeValues) {
        if ("XOR".equals(recipe.name)) {
            drawXorWires(g, layout, nodeValues);
            return;
        }
        for (int i = 0; i < recipe.slotCount(); i++) {
            int x = layout[i][0];
            int y = layout[i][1];
            drawSourceWire(g, recipe.leftSources[i], x, y + 10, layout, nodeValues);
            if (recipe.solution[i] != GateType.NOT) {
                drawSourceWire(g, recipe.rightSources[i], x, y + 24, layout, nodeValues);
            }
        }
    }

    private void drawXorWires(Graphics2D g, int[][] layout, boolean[] values) {
        int aX = 57, aY = 94;
        int bX = 57, bY = 129;

        // G3 = NOT A and G1 = NOT B: short, direct branch starters.
        drawWirePath(g, circuit.inputA(), aX, aY, 79, aY, 79, layout[2][1] + 10,
            layout[2][0], layout[2][1] + 10);
        drawWirePath(g, circuit.inputB(), bX, bY, 79, bY, 79, layout[0][1] + 10,
            layout[0][0], layout[0][1] + 10);

        // The un-inverted inputs take clearly separated outer lanes to the
        // opposite AND gates instead of disappearing behind other modules.
        drawWirePath(g, circuit.inputA(), aX, aY, 69, aY, 69, 168, 164, 168,
            164, layout[1][1] + 10, layout[1][0], layout[1][1] + 10);
        drawWirePath(g, circuit.inputB(), bX, bY, 64, bY, 64, 71, 164, 71,
            164, layout[3][1] + 24, layout[3][0], layout[3][1] + 24);

        // Each NOT feeds only its neighboring AND.
        drawWirePath(g, values[2], layout[2][0] + BOARD_SOCKET_W,
            layout[2][1] + BOARD_SOCKET_H / 2, 158, layout[2][1] + BOARD_SOCKET_H / 2,
            158, layout[3][1] + 10, layout[3][0], layout[3][1] + 10);
        drawWirePath(g, values[0], layout[0][0] + BOARD_SOCKET_W,
            layout[0][1] + BOARD_SOCKET_H / 2, 158, layout[0][1] + BOARD_SOCKET_H / 2,
            158, layout[1][1] + 24, layout[1][0], layout[1][1] + 24);

        // The two product terms remain separate until the final OR.
        drawWirePath(g, values[3], layout[3][0] + BOARD_SOCKET_W,
            layout[3][1] + BOARD_SOCKET_H / 2, 246, layout[3][1] + BOARD_SOCKET_H / 2,
            246, layout[4][1] + 10, layout[4][0], layout[4][1] + 10);
        drawWirePath(g, values[1], layout[1][0] + BOARD_SOCKET_W,
            layout[1][1] + BOARD_SOCKET_H / 2, 252, layout[1][1] + BOARD_SOCKET_H / 2,
            252, layout[4][1] + 24, layout[4][0], layout[4][1] + 24);

        // Break the two visual crossings so they cannot be mistaken for
        // junctions, then annotate both product terms directly on the board.
        drawHorizontalCrossover(g, 64, aY, circuit.inputA());
        drawHorizontalCrossover(g, 69, bY, circuit.inputB());
        g.setColor(new Color(5, 13, 16, 235));
        g.fillRect(222, 75, 47, 12);
        g.fillRect(222, 150, 47, 12);
        g.setColor(new Color(127, 205, 194));
        pixelText(g, "!A & B", 225, 85, 1);
        pixelText(g, "A & !B", 225, 160, 1);
    }

    private void drawHorizontalCrossover(Graphics2D g, int x, int y, boolean on) {
        g.setColor(new Color(5, 13, 16));
        g.fillRect(x - 4, y - 4, 9, 9);
        drawWire(g, x - 5, y, x + 5, y, on);
    }

    private void drawWirePath(Graphics2D g, boolean on, int... points) {
        g.setColor(new Color(1, 5, 6, 195));
        g.setStroke(new BasicStroke(4));
        drawPathSegments(g, points);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        drawPathSegments(g, points);
        g.setStroke(new BasicStroke(1));
        for (int i = 2; i < points.length - 2; i += 2) {
            g.fillRect(points[i] - 2, points[i + 1] - 2, 4, 4);
        }
        int end = points.length - 2;
        g.fillRect(points[end] - 2, points[end + 1] - 2, 4, 4);
    }

    private static void drawPathSegments(Graphics2D g, int[] points) {
        for (int i = 0; i < points.length - 2; i += 2) {
            g.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3]);
        }
    }

    private void drawRoutedWire(Graphics2D g, int x1, int y1, int x2, int y2, boolean on) {
        int bendX = x1 + Math.max(7, (x2 - x1) / 2);
        g.setColor(new Color(1, 5, 6, 195));
        g.setStroke(new BasicStroke(4));
        g.drawLine(x1, y1, bendX, y1);
        g.drawLine(bendX, y1, bendX, y2);
        g.drawLine(bendX, y2, x2, y2);
        g.setColor(on ? new Color(103, 255, 244) : new Color(155, 164, 164));
        g.setStroke(new BasicStroke(on ? 3 : 2));
        g.drawLine(x1, y1, bendX, y1);
        g.drawLine(bendX, y1, bendX, y2);
        g.drawLine(bendX, y2, x2, y2);
        g.setStroke(new BasicStroke(1));
        g.fillRect(bendX - 2, y1 - 2, 4, 4);
        g.fillRect(x2 - 2, y2 - 2, 4, 4);
        if (on) {
            g.setColor(new Color(196, 255, 247));
            g.fillRect(bendX, y1, 1, 1);
        }
    }

    private int[][] socketLayout(CircuitRecipe recipe) {
        return switch (recipe.name) {
            case "XOR" -> new int[][]{{100, 127}, {180, 127}, {100, 76}, {180, 76}, {272, 102}};
            case "XNOR" -> new int[][]{{92, 76}, {92, 127}, {158, 127}, {224, 102}, {286, 102}};
            case "IMPLY" -> new int[][]{{125, 84}, {235, 102}};
            default -> new int[][]{{125, 102}, {235, 102}};
        };
    }

    private void drawNotebookGateCard(Graphics2D g, GateType gate, int x, int y,
                                      String note, String truth) {
        g.setColor(new Color(213, 201, 165, 185));
        g.fillRect(x, y, 166, 48);
        g.setColor(new Color(128, 104, 82));
        g.drawRect(x, y, 166, 48);
        g.setColor(new Color(52, 48, 44));
        drawGate(g, x + 5, y + 4, gate, false);
        pixelText(g, gate.label, x + 46, y + 17, 1);
        g.setColor(new Color(100, 67, 54));
        pixelText(g, note, x + 46, y + 31, 1);
        g.setColor(new Color(54, 51, 47));
        pixelText(g, truth, x + 7, y + 43, 1);
    }

    private void drawNotebookTruthTable(Graphics2D g, CircuitRecipe recipe, int x, int y) {
        g.setColor(new Color(212, 198, 160, 205));
        g.fillRect(x, y, 57, 108);
        g.setColor(new Color(125, 100, 77));
        g.drawRect(x, y, 57, 108);
        g.setColor(new Color(76, 55, 47));
        pixelText(g, "TARGET", x + 8, y + 13, 1);
        g.drawLine(x + 5, y + 18, x + 52, y + 18);
        pixelText(g, "A B | O", x + 7, y + 31, 1);
        for (int row = 0; row < 4; row++) {
            boolean a = row >= 2;
            boolean b = row % 2 == 1;
            g.setColor(recipe.truth[row] ? new Color(29, 110, 99) : new Color(76, 55, 47));
            pixelText(g, bit(a) + " " + bit(b) + " | " + bit(recipe.truth[row]),
                x + 7, y + 46 + row * 14, 1);
        }
        g.setColor(new Color(117, 82, 61));
        pixelText(g, "MATCH ALL", x + 3, y + 103, 1);
    }

    private void drawNotebookPlan(Graphics2D g, CircuitRecipe recipe, int x, int y) {
        g.setColor(new Color(212, 198, 160, 205));
        g.fillRect(x, y, 101, 108);
        g.setColor(new Color(125, 100, 77));
        g.drawRect(x, y, 101, 108);
        g.setColor(new Color(76, 55, 47));
        pixelText(g, "WIRING PLAN", x + 7, y + 13, 1);
        g.drawLine(x + 5, y + 18, x + 96, y + 18);
        for (int i = 0; i < recipe.slotCount(); i++) {
            GateType gate = recipe.solution[i];
            String sources = sourceName(recipe.leftSources[i]);
            if (gate != GateType.NOT) sources += "+" + sourceName(recipe.rightSources[i]);
            g.setColor(i % 2 == 0 ? new Color(66, 58, 51) : new Color(94, 68, 55));
            pixelText(g, "G" + (i + 1) + " " + gate.label + " <- " + sources,
                x + 6, y + 34 + i * 14, 1);
        }
        g.setColor(new Color(29, 110, 99));
        pixelText(g, "LAST GATE -> OUT", x + 5, y + 103, 1);
    }

    private static String sourceName(int source) {
        if (source == CircuitRecipe.INPUT_A) return "A";
        if (source == CircuitRecipe.INPUT_B) return "B";
        return "G" + (source + 1);
    }

    private void drawDialogue(Graphics2D g) {
        int y = 200;
        g.setColor(VOID);
        g.fillRect(16, y, 448, 61);
        g.setColor(INK);
        g.setStroke(new BasicStroke(3));
        g.drawRect(17, y + 1, 446, 59);
        g.setStroke(new BasicStroke(1));
        String[] parts = line.split("\\|", 2);
        String speaker = parts.length == 2 ? parts[0] : "";
        String words = parts.length == 2 ? parts[1] : parts[0];
        g.setColor(speaker.equals("MIRA") ? CYAN : (speaker.equals("ALEX") ? YELLOW : INK));
        if (!speaker.isEmpty()) pixelText(g, speaker, 28, y + 17, 1);
        g.setColor(INK);
        int visible = Math.min(words.length(), lineAge / 2 + 1);
        drawWrapped(g, "* " + words.substring(0, visible), 28, y + 34, 66);
        if (visible == words.length() && (ticks / 25) % 2 == 0) pixelText(g, "v", 444, y + 51, 1);
    }

    private void prompt(Graphics2D g, String text) {
        int width = text.length() * 6 + 14;
        g.setColor(VOID);
        g.fillRect((W - width) / 2, 219, width, 19);
        g.setColor(YELLOW);
        g.drawRect((W - width) / 2, 219, width, 19);
        pixelText(g, text, (W - text.length() * 6) / 2, 232, 1);
    }

    private void interact() {
        if (scene == Scene.BEDROOM) {
            if (chapter == 0 && near(299, 132)) {
                chapter = 1;
                playSound("ui-open");
                say("ALEX|A box full of tiny black pieces... AND, OR, NOT.",
                    "ALEX|And a notebook. The first pages have diagrams.",
                    "ALEX|After that? Just rows of zeroes and ones.");
            } else if (near(205, 126)) {
                if (chapter >= 2) openBoard();
                else say("ALEX|An old pegboard. Maybe I can build something on it.");
            } else if (near(407, 132)) {
                scene = Scene.STREET;
                playSound("door-open");
                setPlayerPosition(STREET_HOME_X + 44, STREET_GROUND_Y);
                facing = Direction.RIGHT;
            }
        } else if (scene == Scene.STREET) {
            if (Math.abs(playerX - STREET_HOME_X) < 38) {
                scene = Scene.BEDROOM;
                playSound("door-open");
                setPlayerPosition(420, 160);
                facing = Direction.LEFT;
            } else if (Math.abs(playerX - STREET_SHOP_X) < 38) {
                scene = Scene.SHOP;
                playSound("door-open");
                setPlayerPosition(55, 174);
                facing = Direction.RIGHT;
            }
        } else if (scene == Scene.SHOP) {
            if (playerX < 50) {
                scene = Scene.STREET;
                playSound("door-close");
                setPlayerPosition(STREET_SHOP_X - 47, STREET_GROUND_Y);
                facing = Direction.LEFT;
            } else if (near(240, 160)) talkToMira();
        }
    }

    private void talkToMira() {
        if (chapter == 0) {
            say("MIRA|Hey, kid. Bring me something interesting.");
        } else if (chapter == 1) {
            chapter = 2;
            say("MIRA|Logic gates! AND, OR, and NOT are the alphabet.",
                "MIRA|Build me a NAND, a NOR, and an XOR.",
                "MIRA|Use every switch setting. Match the notebook exactly.",
                "ALEX|So the truth table is... a list of promises?",
                "MIRA|Exactly. A circuit must keep every one.");
        } else if (chapter == 2 && basicComplete()) {
            chapter = 3;
            say("MIRA|Clean work. You tested every possible input.",
                "MIRA|Take this LogicLens. It checks every row at once.",
                "MIRA|Now try XNOR and IMPLY. The notebook has new pages.");
        } else if (chapter == 2) {
            say("MIRA|I still need NAND, NOR, and XOR. Your board is at home.");
        } else if (chapter == 3 && advancedComplete()) {
            chapter = 4;
            say("MIRA|Five devices, and every promise kept.",
                "MIRA|You don't just connect gates, Alex. You understand them.",
                "ALEX|What's on the next page?",
                "MIRA|That's tomorrow's circuit.");
            dialogue.add("@END");
        } else {
            say("MIRA|Let the LogicLens test XNOR and IMPLY for you.");
        }
    }

    private void openBoard() {
        returnScene = scene;
        scene = Scene.BOARD;
        playSound("ui-open");
        selectRecipe(selectedRecipe);
    }

    private void turnNotebookPage(int direction) {
        int available = chapter >= 3 ? 5 : 3;
        notebookPage = (notebookPage + direction + available) % available;
        playSound("book-flip");
    }

    private void selectRecipe(int index) {
        int max = chapter >= 3 ? 4 : 2;
        selectedRecipe = clamp(index, 0, max);
        circuit.selectRecipe(recipes.get(selectedRecipe));
        playSound("ui-select");
        boardMessage = crafted[selectedRecipe] ? "Already delivered. You can rebuild it." : "Build the requested device.";
        boardMessageTimer = 180;
    }

    private void recordOrAutoTest() {
        if (!circuit.recipe().isComplete(circuit.placed())) {
            boardMessage = "Every socket needs a gate first.";
            playSound("ui-error");
        } else if (chapter >= 3) {
            autoTest();
            return;
        } else {
            circuit.recordCurrent();
            boardMessage = "Recorded A=" + bit(circuit.inputA()) + " B=" + bit(circuit.inputB()) + ".";
            playSound("ui-confirm");
        }
        boardMessageTimer = 180;
    }

    private void verify() {
        if (chapter >= 3) {
            autoTest();
        } else if (!circuit.allRowsRecorded()) {
            boardMessage = "Test and RECORD all four switch settings.";
            playSound("ui-error");
        } else if (circuit.matchesTruthTable()) {
            completeCurrent();
        } else {
            boardMessage = "Mismatch! Replace a gate and test again.";
            playSound("failure");
        }
        boardMessageTimer = 240;
    }

    private void autoTest() {
        if (!circuit.recipe().isComplete(circuit.placed())) {
            boardMessage = "LogicLens: incomplete circuit.";
            playSound("ui-error");
        } else {
            boolean passed = true;
            for (int row = 0; row < 4; row++) {
                boolean a = row >= 2;
                boolean b = row % 2 == 1;
                if (circuit.recipe().evaluate(a, b, circuit.placed()) != circuit.recipe().truth[row]) {
                    passed = false;
                    break;
                }
            }
            if (passed) completeCurrent();
            else {
                boardMessage = "LogicLens: FAILED on one or more rows.";
                playSound("failure");
            }
        }
        boardMessageTimer = 240;
    }

    private void completeCurrent() {
        crafted[selectedRecipe] = true;
        boardMessage = circuit.recipe().name + " COMPLETE! Take it to Mira.";
        playSound("success");
    }

    private void say(String... lines) {
        dialogue.addAll(Arrays.asList(lines));
        nextLine();
    }

    private void playSound(String file) {
        sound.play(AUDIO_ROOT + file + ".wav");
    }

    private void toggleInputA() {
        circuit.toggleA();
        playSound("switch");
    }

    private void toggleInputB() {
        circuit.toggleB();
        playSound("switch");
    }

    private void nextLine() {
        String next = dialogue.poll();
        if ("@END".equals(next)) {
            line = null;
            scene = Scene.END;
        } else {
            line = next;
            lineAge = 0;
            playSound("ui-click");
        }
    }

    private boolean basicComplete() { return crafted[0] && crafted[1] && crafted[2]; }
    private boolean advancedComplete() { return crafted[3] && crafted[4]; }
    private boolean near(int x, int y) { return Math.abs(playerX - x) < 42 && Math.abs(playerY - y) < 40; }

    private static boolean bedroomBlocked(double x, double y) {
        // The bed and its left-side furniture occupy the upper-left footprint.
        // Padding keeps Alex's feet outside the mattress instead of letting the
        // taller sprite appear to walk across it.
        return x < 160 && y < 203;
    }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        keys.add(key);
        if (line != null && (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_E || key == KeyEvent.VK_SPACE)) {
            nextLine();
            return;
        }
        if (scene == Scene.TITLE) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) titleSelection = (titleSelection + 1) % 2;
            else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) titleSelection = (titleSelection + 1) % 2;
            else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) activateTitleSelection();
            return;
        } else if (scene == Scene.CONTROLS) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_BACK_SPACE
                || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) scene = Scene.TITLE;
            return;
        } else if (scene == Scene.END && key == KeyEvent.VK_ENTER) {
            resetToTitle();
        } else if ((scene == Scene.BEDROOM || scene == Scene.STREET || scene == Scene.SHOP)
            && (key == KeyEvent.VK_E || key == KeyEvent.VK_ENTER)) {
            interact();
        } else if (chapter >= 1 && key == KeyEvent.VK_N) {
            if (scene == Scene.NOTEBOOK) {
                scene = returnScene;
                playSound("book-close");
            }
            else {
                returnScene = scene;
                notebookPage = selectedRecipe;
                scene = Scene.NOTEBOOK;
                playSound("book-open");
            }
        } else if (scene == Scene.NOTEBOOK) {
            if (key == KeyEvent.VK_ESCAPE) {
                scene = returnScene;
                playSound("book-close");
            }
            else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) turnNotebookPage(-1);
            else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) turnNotebookPage(1);
        } else if (scene == Scene.BOARD) {
            if (key == KeyEvent.VK_ESCAPE) {
                scene = returnScene;
                playSound("ui-close");
            }
            else if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) {
                heldGate = GateType.values()[key - KeyEvent.VK_1];
                playSound("ui-select");
            }
            else if (key == KeyEvent.VK_A) toggleInputA();
            else if (key == KeyEvent.VK_B) toggleInputB();
            else if (key == KeyEvent.VK_R) recordOrAutoTest();
            else if (key == KeyEvent.VK_T || key == KeyEvent.VK_ENTER) verify();
        }
    }

    @Override public void keyReleased(KeyEvent event) { keys.remove(event.getKeyCode()); }
    @Override public void keyTyped(KeyEvent event) {}

    @Override public void mousePressed(MouseEvent event) {
        requestFocusInWindow();
        int[] point = logicalPoint(event);
        int x = point[0];
        int y = point[1];
        mouseX = x;
        mouseY = y;

        if (scene == Scene.TITLE) {
            if (inside(x, y, 145, 151, 190, 28)) {
                titleSelection = 0;
                activateTitleSelection();
            } else if (inside(x, y, 145, 184, 190, 28)) {
                titleSelection = 1;
                activateTitleSelection();
            }
            return;
        }
        if (scene == Scene.CONTROLS) {
            if (inside(x, y, 164, 217, 152, 22)) scene = Scene.TITLE;
            return;
        }
        if (scene == Scene.NOTEBOOK) {
            if (inside(x, y, 270, 211, 22, 19)) turnNotebookPage(-1);
            else if (inside(x, y, 416, 211, 22, 19)) turnNotebookPage(1);
            else {
                int available = chapter >= 3 ? 5 : 3;
                for (int i = 0; i < available; i++) {
                    if (inside(x, y, 314 + i * 16, 214, 11, 11)) {
                        notebookPage = i;
                        playSound("book-flip");
                    }
                }
            }
            return;
        }
        if (scene != Scene.BOARD || line != null) return;

        int available = chapter >= 3 ? 5 : 3;
        for (int i = 0; i < available; i++) if (inside(x, y, 126 + i * 66, 30, 59, 18)) selectRecipe(i);
        if (inside(x, y, 20, 81, 64, 20)) toggleInputA();
        if (inside(x, y, 20, 116, 64, 20)) toggleInputB();
        for (int i = 0; i < 3; i++) if (inside(x, y, 20 + i * 93, 190, 80, 33)) {
            heldGate = GateType.values()[i];
            playSound("ui-select");
        }

        int[][] layout = socketLayout(circuit.recipe());
        for (int i = 0; i < circuit.recipe().slotCount(); i++) {
            if (inside(x, y, layout[i][0], layout[i][1], BOARD_SOCKET_W, BOARD_SOCKET_H)) {
                circuit.place(i, heldGate);
                boardMessage = heldGate.label + " placed in socket " + (i + 1) + ".";
                boardMessageTimer = 120;
                playSound("gate-place");
            }
        }
        if (inside(x, y, 310, 187, 72, 36)) recordOrAutoTest();
        if (inside(x, y, 390, 187, 70, 36)) verify();
    }

    @Override public void mouseReleased(MouseEvent event) {}
    @Override public void mouseClicked(MouseEvent event) {}
    @Override public void mouseEntered(MouseEvent event) { requestFocusInWindow(); }
    @Override public void mouseExited(MouseEvent event) { mouseX = -1; mouseY = -1; }
    @Override public void mouseMoved(MouseEvent event) {
        int[] point = logicalPoint(event);
        mouseX = point[0];
        mouseY = point[1];
        if (scene == Scene.TITLE) {
            if (inside(mouseX, mouseY, 145, 151, 190, 28)) titleSelection = 0;
            else if (inside(mouseX, mouseY, 145, 184, 190, 28)) titleSelection = 1;
        }
        repaint();
    }
    @Override public void mouseDragged(MouseEvent event) { mouseMoved(event); }

    private int[] logicalPoint(MouseEvent event) {
        double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        double ox = (getWidth() - W * scale) / 2.0;
        double oy = (getHeight() - H * scale) / 2.0;
        return new int[]{
            (int) ((event.getX() - ox) / scale),
            (int) ((event.getY() - oy) / scale)
        };
    }

    private boolean isHovered(int x, int y, int width, int height) {
        return scene == Scene.BOARD && inside(mouseX, mouseY, x, y, width, height);
    }

    private void activateTitleSelection() {
        if (titleSelection == 0) {
            scene = Scene.BEDROOM;
            setPlayerPosition(210, 157);
            facing = Direction.DOWN;
            say("ALEX|It started with a box I wasn't supposed to find.");
        } else {
            scene = Scene.CONTROLS;
        }
    }

    private void resetToTitle() {
        Arrays.fill(crafted, false);
        dialogue.clear();
        line = null;
        chapter = 0;
        selectedRecipe = 0;
        notebookPage = 0;
        circuit.selectRecipe(recipes.get(0));
        setPlayerPosition(210, 157);
        facing = Direction.DOWN;
        walkDistance = 0;
        titleSelection = 0;
        scene = Scene.TITLE;
    }

    private static boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = GamePanel.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            BufferedImage source = ImageIO.read(stream);
            BufferedImage scaled = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, W, H, null);
            graphics.dispose();
            return scaled;
        } catch (IOException error) {
            return null;
        }
    }

    private static BufferedImage loadStreetImage(String path) {
        try (InputStream stream = GamePanel.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            BufferedImage source = ImageIO.read(stream);
            int cropTop = Math.min(90, source.getHeight() - 1);
            int cropHeight = Math.min(600, source.getHeight() - cropTop);
            BufferedImage scaled = new BufferedImage(STREET_WORLD_WIDTH, H,
                BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, STREET_WORLD_WIDTH, H,
                0, cropTop, source.getWidth(), cropTop + cropHeight, null);
            graphics.dispose();
            return scaled;
        } catch (IOException error) {
            return null;
        }
    }

    private static BufferedImage loadRawImage(String path) {
        try (InputStream stream = GamePanel.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (IOException error) {
            return null;
        }
    }

    private static Rectangle[] buildFrameBounds(BufferedImage sheet, int columns, int rows) {
        if (sheet == null) return new Rectangle[0];
        Rectangle[] frames = new Rectangle[columns * rows];
        int cellWidth = sheet.getWidth() / columns;
        int cellHeight = sheet.getHeight() / rows;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int cellX = column * cellWidth;
                int cellY = row * cellHeight;
                int minX = cellX + cellWidth;
                int minY = cellY + cellHeight;
                int maxX = cellX;
                int maxY = cellY;
                for (int y = cellY; y < cellY + cellHeight; y++) {
                    for (int x = cellX; x < cellX + cellWidth; x++) {
                        int alpha = (sheet.getRGB(x, y) >>> 24) & 0xff;
                        if (alpha <= 24) continue;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
                if (maxX < minX || maxY < minY) {
                    frames[row * columns + column] = new Rectangle(cellX, cellY, cellWidth, cellHeight);
                } else {
                    frames[row * columns + column] = new Rectangle(
                        minX, minY, maxX - minX + 1, maxY - minY + 1);
                }
            }
        }
        return frames;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void setPlayerPosition(int x, int y) {
        playerX = x;
        playerY = y;
        precisePlayerX = x;
        precisePlayerY = y;
    }
    private static String bit(boolean value) { return value ? "1" : "0"; }

    private static void pixelText(Graphics2D g, String text, int x, int y, int scale) {
        Font old = g.getFont();
        g.setFont(old.deriveFont((float) (10 * scale)));
        g.drawString(text, x, y);
        g.setFont(old);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int columns) {
        String[] words = text.split(" ");
        StringBuilder row = new StringBuilder();
        int lineY = y;
        for (String word : words) {
            if (row.length() + word.length() + 1 > columns) {
                pixelText(g, row.toString(), x, lineY, 1);
                row.setLength(0);
                lineY += 13;
            }
            if (!row.isEmpty()) row.append(' ');
            row.append(word);
        }
        pixelText(g, row.toString(), x, lineY, 1);
    }

    private static void drawHeart(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 5, 4);
        g.fillRect(x - 2, y + 1, 9, 3);
        g.fillRect(x, y + 4, 5, 2);
        g.fillRect(x + 1, y + 6, 3, 2);
    }
}

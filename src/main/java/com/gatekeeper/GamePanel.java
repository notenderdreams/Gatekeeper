package com.gatekeeper;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import static com.gatekeeper.GameAssets.buildCellBounds;
import static com.gatekeeper.GameAssets.buildFrameBounds;
import static com.gatekeeper.GameAssets.loadBackground;
import static com.gatekeeper.GameAssets.loadPixelFont;
import static com.gatekeeper.GameAssets.loadRawImage;
import static com.gatekeeper.GameAssets.loadStreetBackground;
import static com.gatekeeper.GameConstants.*;

@SuppressWarnings("serial")
public final class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
    private static final int INTRO_DIALOGUE_TICK = 2 * 60;
    private static final Polygon BOX_COLLISION = new Polygon(
        new int[] {272, 310, 333, 338, 271},
        new int[] {136, 145, 124, 89, 84}, 5);
    private static final Polygon SHOP_CRATE_COLLISION = new Polygon(
        new int[] {477, 400, 399, 375, 356, 350, 351, 472},
        new int[] {190, 190, 213, 216, 223, 239, 267, 265}, 8);
    private static final Polygon SHOP_COUNTER_COLLISION = new Polygon(
        new int[] {106, 13, 8, 105},
        new int[] {236, 195, 260, 268}, 4);
    private static float uiScale = 1.0f;
    static final Font PIXEL_FONT = loadPixelFont();

    private final BufferedImage bedroomBackgroundNormal = loadBackground("/assets/backgrounds/bedroom-normal.png");
    private final BufferedImage bedroomBackgroundCc = loadBackground("/assets/backgrounds/bedroom-cc.jpg");
    private final BufferedImage bedroomNoBoxImage = loadBackground("/assets/backgrounds/bedroom-nobox.jpg");
    private final BufferedImage bedroomClosedBoxImage = loadBackground("/assets/backgrounds/bedroom-closedbox.jpg");
    private boolean ccBedroomBackground = true;
    private boolean boxRetrieved = false;
    private boolean boxOpened = false;
    private boolean workbenchInstalled = false;
    private final BufferedImage streetBackgroundNormal = loadStreetBackground("/assets/backgrounds/street-normal.png");
    private final BufferedImage streetBackgroundCc = loadStreetBackground("/assets/backgrounds/street-cc.jpg");
    private final BufferedImage streetBackgroundWithBox = loadStreetBackground("/assets/backgrounds/street-box.jpg");
    private boolean ccStreetBackground = true;
    private int starCount = 25;
    private int mothCount = 3;
    private final BufferedImage shopBackground = loadBackground("/assets/backgrounds/shop-normal.png");
    private final BufferedImage alexSprites = loadRawImage("/assets/characters/alex-sprites.png");
    private final BufferedImage miraSprites = loadRawImage("/assets/characters/mira-sprites.png");
    private final BufferedImage catSprites = loadRawImage("/assets/characters/cat-sprites.png");
    private final BufferedImage boxImage = loadRawImage("/assets/items/box.png");
    private final BufferedImage letterImage = loadRawImage("/assets/items/letter.png");
    private final BufferedImage logicLensImage = loadRawImage("/assets/items/logiclens.png");
    private final BufferedImage notebookItemImage = loadRawImage("/assets/items/notebook.png");
    private final BufferedImage workbenchItemImage = loadRawImage("/assets/items/workbench.png");
    private final BufferedImage installedWorkbenchImage = loadRawImage("/assets/items/workbench-ontable.png");
    private final BufferedImage workbenchCanvasImage = loadRawImage("/assets/items/canvas/canvas.png");
    private final BufferedImage workbenchLightOffImage = loadRawImage("/assets/items/canvas/light-off.png");
    private final BufferedImage workbenchSwitchImage = loadRawImage("/assets/items/canvas/switch.png");
    private final BufferedImage workbenchLightOnImage = loadRawImage("/assets/items/canvas/light-on.png");
    private final BufferedImage workbenchNodeTextureImage = loadRawImage("/assets/items/canvas/node-texture.png");
    private final BufferedImage workbenchWireEndImage = loadRawImage("/assets/items/canvas/wire-end.png");
    private final BufferedImage workbenchEndpointImage = loadRawImage("/assets/items/canvas/endpoint.png");
    private final BufferedImage autoTesterPlugImage =
        loadRawImage("/assets/items/canvas/tester-plug.png");
    private final BufferedImage autoTesterFrameImage = loadRawImage("/assets/items/tester/tester.png");
    private final BufferedImage autoTesterButtonSheet = loadRawImage("/assets/items/tester/tester-buttons.png");
    private final BufferedImage autoTesterNavigationButtonSheet =
        loadRawImage("/assets/items/tester/tester-navigation-buttons.png");
    private final BufferedImage shopFrameImage = loadRawImage("/assets/items/shop/shop.png");
    private final BufferedImage shopItemFrameImage =
        loadRawImage("/assets/items/shop/shop-item.png");
    private final BufferedImage notebookCoverImage = loadRawImage("/assets/ui/book-cover.png");
    private final BufferedImage notebookLeftPageImage = loadRawImage("/assets/ui/book-page-left.png");
    private final BufferedImage notebookRightPageImage = loadRawImage("/assets/ui/book-page-right.png");
    private final BufferedImage erisIdleSprites = loadRawImage("/assets/characters/eris-idle.png");
    private final BufferedImage erisWalkSprites = loadRawImage("/assets/characters/eris-walk.png");
    private final BufferedImage erisInteractSprites = loadRawImage("/assets/characters/eris-interact.png");
    private final BufferedImage erisRunSprites = loadRawImage("/assets/characters/eris-run.png");
    private final Rectangle[] alexFrameBounds = buildFrameBounds(alexSprites, 4, 3);
    private final Rectangle[] miraFrameBounds = buildFrameBounds(miraSprites, 3, 2);
    private final Rectangle[] catFrameBounds = buildCellBounds(catSprites, 15, 1);
    private final Rectangle[] erisIdleBounds = buildCellBounds(erisIdleSprites, 4, 5);
    private final Rectangle[] erisWalkBounds = buildCellBounds(erisWalkSprites, 4, 5);
    private final Rectangle[] erisInteractBounds = buildCellBounds(erisInteractSprites, 4, 5);
    private final Rectangle[] erisRunBounds = buildCellBounds(erisRunSprites, 6, 5);
    private final Set<Integer> keys = new HashSet<>();
    private final Queue<String> dialogue = new ArrayDeque<>();
    private final List<CircuitRecipe> recipes = CircuitRecipe.all();
    private final boolean[] crafted = new boolean[5];
    private final CircuitModel circuit = new CircuitModel(recipes.get(0));
    private final WorkbenchGraph workbenchGraph = new WorkbenchGraph(recipes.get(0));
    private final NodeRadialMenu nodeRadialMenu = new NodeRadialMenu();
    private final SoundManager sound = new SoundManager();
    private final Random random = new Random();
    private boolean catPresent;
    private int catX = STREET_CAT_X;
    private int catY = 152;
    private GameScene scene = GameScene.TITLE;
    private GameScene returnScene = GameScene.BEDROOM;
    private GameScene notebookReturnScene = GameScene.BEDROOM;
    private String line;
    private String completedObjective;
    private int lineAge;
    private int chapter;
    private int titleSelection;
    private int settingsSelection;
    private boolean taskbarOnRight = false;
    private int devSection = 0;
    private int devSelection;
    private int soundSceneSelection;
    private boolean devFocusRight = true;
    private boolean calibratorEnabled = false;
    private boolean showCollisions = false;
    private boolean disableHud = false;
    private boolean instantStart = false;
    private boolean catAlwaysAppears = false;
    private GameScene devReturnScene = GameScene.TITLE;
    private boolean exitPrompt;
    private int exitPromptSelection;
    private boolean settingsOpenedFromPause;
    private GameScene pausedScene = GameScene.BEDROOM;
    private int selectedRecipe;
    private int testerTargetRecipe;
    private int notebookPage;
    private int playerX = 210;
    private int playerY = 157;
    private double precisePlayerX = 210;
    private double precisePlayerY = 157;
    private Facing facing = Facing.DOWN;
    private boolean playerMoving;
    private boolean playerRunning;
    private int walkDistance;
    private int lastFootstep;
    private GateType heldGate = GateType.AND;
    private boolean nodeWheelHeld;
    private String boardMessage = "Click empty space to add. Wire output to input. Backspace deletes.";
    private int boardMessageTimer;
    private final AutoTester autoTester = new AutoTester();
    private final AutoTesterRenderer autoTesterRenderer = new AutoTesterRenderer(
        autoTesterFrameImage, autoTesterButtonSheet, autoTesterNavigationButtonSheet,
        autoTesterPlugImage, PIXEL_FONT);
    private boolean autoTesterOverlayVisible;
    private boolean autoTesterRunFromOverlay;
    private AutoTesterRenderer.Action pressedAutoTesterAction = AutoTesterRenderer.Action.NONE;
    private String autoTesterUiStatus = "UI READY // LOGIC OFFLINE";
    private final ShopModel shopModel = new ShopModel(ShopProduct.catalog(), 250);
    private final ShopRenderer shopRenderer = new ShopRenderer(
        shopFrameImage, shopItemFrameImage, workbenchNodeTextureImage,
        workbenchWireEndImage, PIXEL_FONT);
    private final InventoryRenderer inventoryRenderer = new InventoryRenderer(
        workbenchNodeTextureImage, shopItemFrameImage, workbenchWireEndImage, PIXEL_FONT);
    private boolean shopOverlayVisible;
    private boolean inventoryVisible;
    private ShopRenderer.Action pressedShopAction = ShopRenderer.Action.NONE;
    private final WorkbenchRenderer workbenchRenderer = new WorkbenchRenderer(
        workbenchCanvasImage, workbenchLightOffImage, workbenchSwitchImage,
        workbenchLightOnImage, workbenchNodeTextureImage, workbenchWireEndImage,
        workbenchEndpointImage);
    private final NotebookRenderer notebookRenderer = new NotebookRenderer(
        recipes, crafted, notebookCoverImage, notebookLeftPageImage, notebookRightPageImage);
    private final WorldRenderer worldRenderer = new WorldRenderer(
        bedroomNoBoxImage != null ? bedroomNoBoxImage : (ccBedroomBackground ? bedroomBackgroundCc : bedroomBackgroundNormal),
        ccStreetBackground ? streetBackgroundCc : streetBackgroundNormal, shopBackground,
        installedWorkbenchImage, alexSprites, miraSprites, catSprites, boxImage,
        alexFrameBounds, miraFrameBounds, catFrameBounds,
        erisIdleSprites, erisWalkSprites, erisInteractSprites, erisRunSprites,
        erisIdleBounds, erisWalkBounds, erisInteractBounds, erisRunBounds);
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean wireDragActive;
    private boolean wireDragMoved;
    private int wireDragStartX;
    private int wireDragStartY;
    private long ticks;
    private int introTimer = 0;
    private int introStage = 0;

    private BufferedImage getBedroomBackground() {
        if (!boxRetrieved) {
            return bedroomNoBoxImage != null ? bedroomNoBoxImage :
                (ccBedroomBackground ? bedroomBackgroundCc : bedroomBackgroundNormal);
        }
        if (!boxOpened) {
            return bedroomClosedBoxImage != null ? bedroomClosedBoxImage :
                (ccBedroomBackground ? bedroomBackgroundCc : bedroomBackgroundNormal);
        }
        return ccBedroomBackground ? bedroomBackgroundCc : bedroomBackgroundNormal;
    }

    private void updateBedroomBackground() {
        BufferedImage bg = getBedroomBackground();
        worldRenderer.setBedroomBackground(bg);
        worldRenderer.setBoxRetrieved(boxRetrieved);
        worldRenderer.setBoxOpened(boxOpened);
        worldRenderer.setWorkbenchInstalled(workbenchInstalled);
    }

    private void updateStreetBackground() {
        if (!boxRetrieved && streetBackgroundWithBox != null) {
            worldRenderer.setStreetBackground(streetBackgroundWithBox);
        } else {
            worldRenderer.setStreetBackground(ccStreetBackground ? streetBackgroundCc : streetBackgroundNormal);
        }
    }

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        if (SaveManager.hasSave()) {
            SaveData data = SaveManager.loadGame();
            if (data != null) {
                instantStart = data.instantStart;
                taskbarOnRight = data.taskbarOnRight;
                ccBedroomBackground = data.ccBedroomBackground;
                ccStreetBackground = data.ccStreetBackground;
                starCount = data.starCount;
                mothCount = data.mothCount;
                disableHud = data.disableHud;
                worldRenderer.setStarCount(starCount);
                worldRenderer.setMothCount(mothCount);
                worldRenderer.setDisableHud(disableHud);
                updateBedroomBackground();
                updateStreetBackground();
                if (instantStart) {
                    loadSavedProgress();
                }
            }
        } else {
            updateBedroomBackground();
            updateStreetBackground();
        }

        Timer timer = new Timer(1000 / 60, event -> updateGame());
        timer.start();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        double windowScale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        double offsetX = (getWidth() - W * windowScale) / 2.0;
        double offsetY = (getHeight() - H * windowScale) / 2.0;
        g.translate(offsetX, offsetY);
        g.scale(windowScale, windowScale);
        g.setClip(0, 0, W, H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setFont(PIXEL_FONT.deriveFont(Font.PLAIN, PIXEL_FONT_BASE_SIZE));
        g.setColor(VOID);
        g.fillRect(0, 0, W, H);
        g.setColor(VOID);
        g.fillRect(0, 0, W, H);

        worldRenderer.update(chapter, playerX, playerY, ticks, line, playerMoving, playerRunning,
            facing, walkDistance, keys, uiScale, catPresent, catX, catY);
        switch (scene) {
            case TITLE -> MenuRenderer.drawTitle(g, getBedroomBackground(), ticks,
                mouseX, mouseY, titleSelection);
            case CONTROLS -> MenuRenderer.drawControls(g, getBedroomBackground(), mouseX, mouseY);
            case SETTINGS -> MenuRenderer.drawSettings(g, ticks, mouseX, mouseY,
                settingsSelection, sound, uiScale, taskbarOnRight);
            case DEV -> MenuRenderer.drawDeveloper(g, mouseX, mouseY, devSection, devSelection,
                devFocusRight, calibratorEnabled, showCollisions, disableHud, instantStart, catAlwaysAppears, ccBedroomBackground, ccStreetBackground, starCount, mothCount, sound, soundSceneSelection);
            case INTRO -> {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, W, H);
            }
            case BEDROOM -> worldRenderer.drawBedroom(g);
            case STREET -> worldRenderer.drawStreet(g);
            case SHOP -> worldRenderer.drawShop(g);
            case BOARD -> workbenchRenderer.draw(g, workbenchGraph, heldGate,
                nodeRadialMenu, mouseX, mouseY);
            case NOTEBOOK -> notebookPage = notebookRenderer.drawNotebook(
                g, chapter, notebookPage, ticks);
            case END -> notebookRenderer.drawEnding(g);
        }
        if (scene == GameScene.BOARD) {
            worldRenderer.drawPositionMarkers(g, 0);
        }
        if (scene == GameScene.BOARD && autoTesterOverlayVisible) {
            autoTesterRenderer.draw(g, testerTarget(), autoTester.observations(),
                autoTester.currentRow(), mouseX, mouseY, pressedAutoTesterAction,
                autoTesterUiStatus);
        }
        if (scene == GameScene.SHOP && shopOverlayVisible) {
            shopRenderer.draw(g, shopModel, mouseX, mouseY, pressedShopAction);
        }
        if (inventoryVisible) inventoryRenderer.draw(g, shopModel);
        if (!disableHud && !shopOverlayVisible && !inventoryVisible
            && (scene == GameScene.BEDROOM || scene == GameScene.STREET || scene == GameScene.SHOP)) {
            ObjectiveRenderer.draw(g, chapter, boxRetrieved, boxOpened, workbenchInstalled,
                crafted, completedObjective, taskbarOnRight);
        }
        if (!disableHud && dialogueVisible()) {
            DialogueRenderer.draw(g, line, lineAge, ticks, uiScale, logicLensImage,
                notebookItemImage, workbenchItemImage, boxImage, letterImage);
        }
        if (exitPrompt) MenuRenderer.drawPause(g, mouseX, mouseY, exitPromptSelection);
        g.dispose();
    }

    private void startIntroCutscene() {
        chapter = 0;
        boxRetrieved = false;
        boxOpened = false;
        workbenchInstalled = false;
        updateBedroomBackground();
        updateStreetBackground();
        scene = GameScene.INTRO;
        introTimer = 0;
        introStage = 0;
        dialogue.clear();
        line = null;
        completedObjective = null;
        keys.clear();
        Arrays.fill(crafted, false);
        shopModel.reset();
        selectedRecipe = 0;
        notebookPage = 0;
        resetAutoTesterState();
        circuit.selectRecipe(recipes.get(0));
        playSound("knock");
        say("*knock knock*",
            "ALEX|Who's knocking at the door in the middle of the night? I should check.",
            "@START_BEDROOM");
    }

    private void updateIntroCutscene() {
        introTimer++;
    }

    private void updateGame() {
        ticks++;
        sound.setMusicMuted(scene == GameScene.DEV);
        sound.loop(MUSIC_LOOP);
        sound.updateMusic();
        sound.updateCrossfade();
        if (scene == GameScene.STREET) sound.loopAmbient(ROAD_AMBIENCE);
        else sound.stopAmbient();
        if (dialogueVisible()) lineAge++;
        if (scene == GameScene.INTRO) {
            updateIntroCutscene();
            repaint();
            return;
        }
        if (scene == GameScene.BOARD && autoTester.isRunning()) updateAutoTest();
        if (boardMessageTimer > 0) boardMessageTimer--;
        if (!exitPrompt && line == null && !shopOverlayVisible && !inventoryVisible
            && (scene == GameScene.BEDROOM || scene == GameScene.STREET || scene == GameScene.SHOP)) {
            boolean sideView = scene == GameScene.STREET;
            boolean shift = keys.contains(KeyEvent.VK_SHIFT);
            double speed = shift ? 1.5 : 1.0;
            int oldX = playerX;
            int oldY = playerY;
            double oldPreciseX = precisePlayerX;
            double oldPreciseY = precisePlayerY;
            int axisX = 0;
            int axisY = 0;
            if (keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_A)) {
                axisX--;
            }
            if (keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D)) {
                axisX++;
            }
            if (!sideView && (keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_W))) {
                axisY--;
            }
            if (!sideView && (keys.contains(KeyEvent.VK_DOWN) || keys.contains(KeyEvent.VK_S))) {
                axisY++;
            }

            if (axisX > 0 && axisY > 0) facing = Facing.DOWN_RIGHT;
            else if (axisX < 0 && axisY > 0) facing = Facing.DOWN_LEFT;
            else if (axisX > 0 && axisY < 0) facing = Facing.UP_RIGHT;
            else if (axisX < 0 && axisY < 0) facing = Facing.UP_LEFT;
            else if (axisX > 0) facing = Facing.RIGHT;
            else if (axisX < 0) facing = Facing.LEFT;
            else if (axisY > 0) facing = Facing.DOWN;
            else if (axisY < 0) facing = Facing.UP;

            if (axisX != 0 || axisY != 0) {
                if (sideView) {
                    precisePlayerX += axisX * speed;
                } else {
                    precisePlayerX += axisX * speed;
                    precisePlayerY += axisY * speed;
                }
            }
            int minX = sideView ? 45 : 22;
            int maxX = sideView ? STREET_WORLD_WIDTH - 30 : 452;
            int minY = scene == GameScene.SHOP ? 158 : 132;
            precisePlayerX = clamp(precisePlayerX, minX, maxX);
            if (sideView) precisePlayerY = STREET_GROUND_Y;
            else precisePlayerY = clamp(precisePlayerY, minY, 232);
            if (scene == GameScene.BEDROOM || scene == GameScene.SHOP) {
                double targetX = precisePlayerX;
                double targetY = precisePlayerY;
                precisePlayerX = oldPreciseX;
                precisePlayerY = oldPreciseY;
                boolean blockedX = scene == GameScene.BEDROOM ? bedroomBlocked(targetX, precisePlayerY) : shopBlocked(targetX, precisePlayerY);
                boolean blockedY = scene == GameScene.BEDROOM ? bedroomBlocked(precisePlayerX, targetY) : shopBlocked(precisePlayerX, targetY);
                if (!blockedX) precisePlayerX = targetX;
                if (!blockedY) precisePlayerY = targetY;
            }
            playerX = (int) Math.round(precisePlayerX);
            playerY = (int) Math.round(precisePlayerY);

            boolean movedX = playerX != oldX;
            boolean movedY = playerY != oldY;
            playerMoving = movedX || movedY;
            playerRunning = playerMoving && shift;

            if (movedX && !movedY) {
                facing = (playerX > oldX) ? Facing.RIGHT : Facing.LEFT;
            } else if (!movedX && movedY) {
                facing = (playerY > oldY) ? Facing.DOWN : Facing.UP;
            }
            if (playerMoving) {
                walkDistance += Math.max(1, (int) Math.round(
                    Math.hypot(precisePlayerX - oldPreciseX, precisePlayerY - oldPreciseY)));
                int footstep = walkDistance / 30;
                if (footstep > lastFootstep) {
                    lastFootstep = footstep;
                    sound.play(AUDIO_ROOT + String.format("footstep-%02d.wav", (footstep - 1) % 4 + 1),
                        scene.name(), 0.25f);
                }
            }
        } else {
            playerMoving = false;
        }
        repaint();
    }

    private void prompt(Graphics2D g, String text) {
        DialogueRenderer.drawPrompt(g, text, uiScale);
    }

    private void rollCatSpawn() {
        catPresent = catAlwaysAppears || random.nextInt(5) == 0;
        if (catPresent) {
            int[] zone = CAT_SPAWN_RANGES[random.nextInt(CAT_SPAWN_RANGES.length)];
            catX = zone[0] + random.nextInt(zone[1] - zone[0] + 1);
            catY = zone[2];
        }
    }

    private void interact() {
        if (scene == GameScene.BEDROOM) {
            if (boxRetrieved && !boxOpened && near(299, 132)) {
                completedObjective = "OPEN THE MYSTERY BOX";
                boxOpened = true;
                if (chapter == 0) chapter = 1;
                updateBedroomBackground();
                playSound("ui-open");
                say(NOTEBOOK_ITEM_CARD,
                    WORKBENCH_ITEM_CARD,
                    "ALEX|A box full of tiny black pieces... AND, OR, NOT.",
                    "ALEX|And a notebook. The first pages have diagrams.",
                    "ALEX|After that? Just rows of zeroes and ones.");
                saveCurrentProgress();
            } else if (near(205, 126)) {
                if (boxOpened && !workbenchInstalled) {
                    completedObjective = "INSTALL THE WORKBENCH";
                    workbenchInstalled = true;
                    worldRenderer.setWorkbenchInstalled(true);
                    playSound("gate-place");
                    say("ALEX|There. The workbench fits perfectly on the desk.",
                        chapter >= 2
                            ? "ALEX|Mira's circuit plans should work here."
                            : "ALEX|These logic components look like electronics... Mira down at the shop might know something about them.");
                    saveCurrentProgress();
                } else if (workbenchInstalled && chapter >= 2) {
                    openBoard();
                } else if (workbenchInstalled) {
                    say("ALEX|The workbench is ready. I should ask Mira at the electronics shop about these gates.");
                }
            } else if (near(407, 132)) {
                scene = GameScene.STREET;
                rollCatSpawn();
                playSound("door-open");
                setPlayerPosition(STREET_HOME_X + 44, STREET_GROUND_Y);
                facing = Facing.RIGHT;
                saveCurrentProgress();
            }
        } else if (scene == GameScene.STREET) {
            if (Math.abs(playerX - STREET_HOME_X) < 38) {
                scene = GameScene.BEDROOM;
                playSound("door-open");
                setPlayerPosition(420, 160);
                facing = Facing.LEFT;
                saveCurrentProgress();
            } else if (Math.abs(playerX - STREET_SHOP_X) < 38) {
                scene = GameScene.SHOP;
                playSound("door-open");
                setPlayerPosition(55, 174);
                facing = Facing.RIGHT;
                saveCurrentProgress();
            } else if (!boxRetrieved && Math.abs(playerX - STREET_BOX_X) < 38) {
                say("ALEX|A mystery package left on Lantern Street... with a letter attached.",
                    LETTER_ITEM_CARD,
                    "@ENTER_BEDROOM_WITH_BOX");
            } else if (catPresent && Math.abs(playerX - catX) < 38) {
                int catSound = random.nextInt(3) + 1;
                sound.play(AUDIO_ROOT + "cat/cat" + catSound + ".wav", scene.name(), 1.0f);
                if (catSound == 3) {
                    say("CAT|Meow meow Meow Meow ");
                } else {
                    say("CAT|Meow.");
                }
            }
        } else if (scene == GameScene.SHOP) {
            if (playerX < 50) {
                scene = GameScene.STREET;
                rollCatSpawn();
                playSound("door-close");
                setPlayerPosition(STREET_SHOP_X - 47, STREET_GROUND_Y);
                facing = Facing.LEFT;
                saveCurrentProgress();
            } else if (near(240, 160)) talkToMira();
        }
    }

    private void talkToMira() {
        if (chapter == 0) {
            say("MIRA|Hey, kid. Bring me something interesting.");
        } else if (chapter == 1) {
            completedObjective = "ASK MIRA ABOUT THE GATES";
            chapter = 2;
            say("ALEX|Mira, someone left a mystery box on my doorstep with these logic gates and a notebook.",
                "MIRA|Logic gates! AND, OR, and NOT are the alphabet of electronics.",
                "MIRA|Bring them to life! Build me a NAND, a NOR, and an XOR on your workbench.",
                "MIRA|Use every switch setting. Match the notebook truth tables exactly.",
                "ALEX|So a truth table is... a list of promises the circuit has to keep?",
                "MIRA|Exactly. A circuit must keep every single one.");
            saveCurrentProgress();
        } else if (chapter == 2 && basicComplete()) {
            completedObjective = "RETURN TO MIRA";
            chapter = 3;
            say("MIRA|Clean work. You tested every possible input.",
                "MIRA|Take this LogicLens. It checks every row at once.",
                LOGICLENS_ITEM_CARD,
                "MIRA|Now try XNOR and IMPLY. The notebook has new pages.");
            saveCurrentProgress();
        } else if (chapter == 2) {
            say("MIRA|I still need NAND, NOR, and XOR. Your board is at home.");
        } else if (chapter == 3 && advancedComplete()) {
            completedObjective = "RETURN TO MIRA";
            chapter = 4;
            say("MIRA|Five devices, and every promise kept.",
                "MIRA|You don't just connect gates, Alex. You understand them.",
                "ALEX|What's on the next page?",
                "MIRA|That's tomorrow's circuit.");
            dialogue.add("@END");
            saveCurrentProgress();
        } else {
            say("MIRA|Let the LogicLens test XNOR and IMPLY for you.");
        }
    }

    private void openBoard() {
        returnScene = scene;
        scene = GameScene.BOARD;
        playSound("ui-open");
        selectRecipe(selectedRecipe);
    }

    private void turnNotebookPage(int direction) {
        int available = chapter >= 3 ? 5 : 3;
        notebookPage = (notebookPage + direction + available) % available;
        playSound("book-flip");
    }

    private void selectRecipe(int index) {
        nodeRadialMenu.close();
        int max = chapter >= 3 ? 4 : 2;
        selectedRecipe = clamp(index, 0, max);
        circuit.selectRecipe(recipes.get(selectedRecipe));
        workbenchGraph.loadRecipe(recipes.get(selectedRecipe));
        playSound("ui-select");
        boardMessage = crafted[selectedRecipe] ? "Already delivered. You can rebuild it." : "Build the requested device.";
        boardMessageTimer = 180;
    }

    private List<GateType> unlockedGateTypes() {
        int availableRecipes = chapter >= 3 ? 5 : 3;
        List<GateType> unlocked = new ArrayList<>();
        for (GateType gate : GateType.values()) {
            for (int recipeIndex = 0; recipeIndex < availableRecipes; recipeIndex++) {
                if (Arrays.asList(recipes.get(recipeIndex).solution).contains(gate)) {
                    unlocked.add(gate);
                    break;
                }
            }
        }
        return unlocked;
    }

    private void openNodeRadialMenu() {
        workbenchGraph.endNodeDrag();
        workbenchGraph.cancelWire();
        int logicalX = mouseX >= 0 ? mouseX : W / 2;
        int logicalY = mouseY >= 0 ? mouseY : H / 2;
        nodeRadialMenu.openAt(WorkbenchRenderer.canvasX(logicalX),
            WorkbenchRenderer.canvasY(logicalY), unlockedGateTypes());
        playSound("ui-open");
    }

    private void recordOrAutoTest() {
        if (chapter >= 3 && autoTester.isAttached()) {
            boardMessage = "LogicLens attached — press RUN KIT (T) to test.";
            playSound("ui-error");
            boardMessageTimer = 180;
            return;
        }
        if (!circuit.recipe().isComplete(circuit.placed())) {
            boardMessage = "Every socket needs a gate first.";
            playSound("ui-error");
        } else {
            circuit.recordCurrent();
            boardMessage = "Recorded A=" + bit(circuit.inputA()) + " B=" + bit(circuit.inputB()) + ".";
            playSound("ui-confirm");
        }
        boardMessageTimer = 180;
    }

    private void verify() {
        if (chapter >= 3 && autoTester.isAttached()) {
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
        if (!autoTester.start(recipes.get(selectedRecipe))) return;
        autoTesterRunFromOverlay = false;
        boardMessage = "LogicLens: starting four-row sweep.";
        boardMessageTimer = 180;
        playSound("ui-open");
    }

    private void updateAutoTest() {
        AutoTester.Tick tick = autoTester.update(workbenchGraph);
        if (tick.finished()) {
            boardMessageTimer = 240;
            if (autoTesterRunFromOverlay) {
                autoTesterUiStatus = tick.passed()
                    ? "PASS // " + autoTester.target().name
                    : "FAIL // " + autoTester.target().name;
                boardMessage = tick.passed()
                    ? "LogicLens: current circuit matches " + autoTester.target().name + "."
                    : "LogicLens: current circuit does not match " + autoTester.target().name + ".";
                playSound(tick.passed() ? "success" : "failure");
            } else if (tick.passed()) {
                completeCurrent();
            } else {
                boardMessage = "LogicLens: FAILED on one or more rows.";
                playSound("failure");
            }
            return;
        }
        if (tick.advanced()) {
            if (autoTesterRunFromOverlay) {
                autoTesterUiStatus = "RUNNING // ROW "
                    + (autoTester.currentRow() + 1) + " OF 4";
            }
            boardMessage = "LogicLens: testing row " + (autoTester.currentRow() + 1) + " of 4.";
            boardMessageTimer = 180;
            playSound("ui-click");
        }
    }

    private void toggleAutoTester() {
        if (chapter < 3) return;
        if (autoTester.isRunning()) {
            boardMessage = "Finish the LogicLens sweep first.";
            boardMessageTimer = 120;
            return;
        }
        autoTester.toggleAttachment();
        boardMessage = autoTester.isAttached()
            ? "LogicLens leads clipped to A, B, and OUT."
            : "LogicLens detached. Manual recording restored.";
        boardMessageTimer = 180;
        playSound(autoTester.isAttached() ? "ui-confirm" : "ui-close");
    }

    private void openAutoTesterOverlay() {
        if (!autoTesterUnlocked()) {
            boardMessage = "The tester is not available yet. Talk to Mira.";
            boardMessageTimer = 180;
            playSound("ui-error");
            repaint();
            return;
        }
        nodeWheelHeld = false;
        nodeRadialMenu.close();
        cancelWireInteraction();
        workbenchGraph.endNodeDrag();
        autoTesterOverlayVisible = true;
        autoTesterUiStatus = "TEST CONFIG // " + testerTarget().name;
        playSound("ui-open");
        repaint();
    }

    private void closeAutoTesterOverlay() {
        autoTesterOverlayVisible = false;
        pressedAutoTesterAction = AutoTesterRenderer.Action.NONE;
        playSound("ui-close");
        repaint();
    }

    private void navigateTesterRecipe(int direction) {
        if (autoTester.isRunning()) {
            autoTesterUiStatus = "STOP TEST BEFORE CHANGING CONFIG";
            playSound("ui-error");
            repaint();
            return;
        }
        testerTargetRecipe = (testerTargetRecipe + direction + recipes.size()) % recipes.size();
        autoTester.clearResults();
        autoTesterRunFromOverlay = false;
        autoTesterUiStatus = "TEST CONFIG // " + testerTarget().name;
        playSound("ui-select");
        repaint();
    }

    private CircuitRecipe testerTarget() {
        return recipes.get(testerTargetRecipe);
    }

    private boolean autoTesterUnlocked() {
        return chapter >= 3;
    }

    private void resetAutoTesterState() {
        autoTester.reset();
        testerTargetRecipe = 0;
        autoTesterOverlayVisible = false;
        autoTesterRunFromOverlay = false;
        pressedAutoTesterAction = AutoTesterRenderer.Action.NONE;
        autoTesterUiStatus = "UI READY // LOGIC OFFLINE";
    }

    private void handleAutoTesterAction(AutoTesterRenderer.Action action) {
        switch (action) {
            case RUN -> {
                if (autoTester.start(testerTarget())) {
                    autoTesterRunFromOverlay = true;
                    autoTesterUiStatus = "RUNNING // ROW 1 OF 4";
                    boardMessage = "LogicLens: testing the current circuit as "
                        + testerTarget().name + ".";
                    boardMessageTimer = 180;
                    playSound("ui-open");
                } else {
                    autoTesterUiStatus = "TEST ALREADY RUNNING";
                    playSound("ui-error");
                }
            }
            case STOP -> {
                boolean stopped = autoTester.stop();
                autoTesterRunFromOverlay = false;
                autoTesterUiStatus = stopped
                    ? "STOPPED // " + testerTarget().name
                    : "TESTER IDLE // " + testerTarget().name;
                playSound(stopped ? "ui-close" : "ui-select");
            }
            case CLEAR -> {
                autoTester.clearResults();
                autoTesterRunFromOverlay = false;
                autoTesterUiStatus = "READY // " + testerTarget().name;
                playSound("ui-select");
            }
            case PREVIOUS -> navigateTesterRecipe(-1);
            case NEXT -> navigateTesterRecipe(1);
            case NONE -> { }
        }
        repaint();
    }

    private void completeCurrent() {
        crafted[selectedRecipe] = true;
        boardMessage = circuit.recipe().name + " COMPLETE! Take it to Mira.";
        playSound("success");
        saveCurrentProgress();
    }

    private void say(String... lines) {
        dialogue.addAll(Arrays.asList(lines));
        nextLine();
    }

    private void playSound(String file) {
        sound.play(AUDIO_ROOT + file + ".wav", scene.name(), 1.0f);
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
            completedObjective = null;
            scene = GameScene.END;
        } else if ("@START_BEDROOM".equals(next)) {
            line = null;
            completedObjective = null;
            scene = GameScene.BEDROOM;
            setPlayerPosition(210, 157);
            facing = Facing.DOWN;
            saveCurrentProgress();
        } else if ("@ENTER_BEDROOM_WITH_BOX".equals(next)) {
            line = null;
            completedObjective = null;
            boxRetrieved = true;
            if (chapter == 0) chapter = 1;
            updateBedroomBackground();
            updateStreetBackground();
            playSound("door-open");
            scene = GameScene.BEDROOM;
            setPlayerPosition(210, 157);
            facing = Facing.DOWN;
            saveCurrentProgress();
            say("ALEX|Now let me open the box and see what's inside.");
        } else {
            line = next;
            if (next == null) completedObjective = null;
            lineAge = 0;
            playSound(isItemCard(next) ? "success" : "ui-click");
        }
    }

    private static boolean isItemCard(String value) {
        return LOGICLENS_ITEM_CARD.equals(value) || NOTEBOOK_ITEM_CARD.equals(value)
            || WORKBENCH_ITEM_CARD.equals(value) || LETTER_ITEM_CARD.equals(value);
    }

    private boolean dialogueLineComplete() {
        if (line == null) return true;
        if (isItemCard(line)) return true;
        int separator = line.indexOf('|');
        String words = separator >= 0 ? line.substring(separator + 1) : line;
        return lineAge / 2 + 1 >= words.length();
    }

    private boolean dialogueVisible() {
        return line != null && !exitPrompt
            && (scene == GameScene.INTRO || scene == GameScene.BEDROOM || scene == GameScene.STREET || scene == GameScene.SHOP);
    }

    private boolean basicComplete() { return crafted[0] && crafted[1] && crafted[2]; }
    private boolean advancedComplete() { return crafted[3] && crafted[4]; }
    private boolean near(int x, int y) { return Math.abs(playerX - x) < 42 && Math.abs(playerY - y) < 40; }

    private boolean bedroomBlocked(double x, double y) {
        // Perspective-angled bed/desk footprint calibrated by user points: {97,92}, {148,131}, {58,206}, {1,142}
        if (y < 206 && y >= 110) {
            if (y < 131 && x < 148) return true;
            double maxX = 148.0 - (y - 131.0) * 1.2;
            if (x < maxX) return true;
        }

        // Calibrated box physical collision footprint: {272,136}, {310,145}, {333,124}, {338,89}, {271,84}
        // Test the character's foot-width, not just its center point, against the calibrated outline.
        if (boxRetrieved && BOX_COLLISION.intersects(x - 6, y, 12, 10)) {
            return true;
        }

        return false;
    }

    private boolean shopBlocked(double x, double y) {
        // Calibrated shop crate physical collision footprint:
        // {477,190}, {400,190}, {399,213}, {375,216}, {356,223}, {350,239}, {351,267}, {472,265}
        if (SHOP_CRATE_COLLISION.intersects(x - 6, y, 12, 10)) {
            return true;
        }
        // Calibrated shop counter/shelf physical collision footprint:
        // {106,236}, {13,195}, {8,260}, {105,268}
        if (SHOP_COUNTER_COLLISION.intersects(x - 6, y, 12, 10)) {
            return true;
        }
        return false;
    }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        boolean firstPress = keys.add(key);
        if (key == KeyEvent.VK_F1 && scene != GameScene.DEV) {
            devReturnScene = scene;
            devSection = 0;
            devSelection = 0;
            soundSceneSelection = 0;
            devFocusRight = true;
            scene = GameScene.DEV;
            keys.clear();
            playSound("ui-open");
            return;
        }
        if (scene == GameScene.DEV) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_F1) {
                scene = devReturnScene;
                playSound("ui-back");
            } else if (key == KeyEvent.VK_P) {
                dumpDevState();
            } else if (key == KeyEvent.VK_TAB) {
                devFocusRight = !devFocusRight;
                playSound("ui-select");
            } else if (devSection == 1 && devFocusRight
                && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A
                    || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)) {
                adjustSelectedDevSound((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) ? -1 : 1);
            } else if (devSection == 2 && devSelection == 5 && devFocusRight
                && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A
                    || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)) {
                int step = (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) ? -25 : 25;
                starCount = clamp(starCount + step, 0, 200);
                worldRenderer.setStarCount(starCount);
                playSound("ui-click");
                saveCurrentProgress();
            } else if (devSection == 2 && devSelection == 6 && devFocusRight
                && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A
                    || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)) {
                int step = (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) ? -1 : 1;
                mothCount = clamp(mothCount + step, 0, 50);
                worldRenderer.setMothCount(mothCount);
                playSound("ui-click");
                saveCurrentProgress();
            } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                devFocusRight = false;
                playSound("ui-select");
            } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                devFocusRight = true;
                playSound("ui-select");
            } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W
                || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                int direction = (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) ? -1 : 1;
                if (!devFocusRight) {
                    devSection = (devSection + direction + 4) % 4;
                    devSelection = 0;
                } else {
                    int max = devOptionCount();
                    devSelection = (devSelection + direction + max) % max;
                }
                playSound("ui-select");
            } else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                if (!devFocusRight) {
                    devFocusRight = true;
                    devSelection = 0;
                    playSound("ui-select");
                } else {
                    activateDevSelection();
                }
            }
            return;
        }
        if (exitPrompt) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W
                || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                exitPromptSelection = (exitPromptSelection + 1) % 2;
                playSound("ui-select");
            } else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                activateExitPromptSelection();
            } else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_BACK_SPACE) {
                exitPrompt = false;
                playSound("ui-back");
            }
            return;
        }
        if (shopOverlayVisible) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_F) {
                closeShopOverlay();
            } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A
                || key == KeyEvent.VK_MINUS) {
                shopModel.decreaseQuantity();
                playSound("ui-click");
                repaint();
            } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D
                || key == KeyEvent.VK_EQUALS || key == KeyEvent.VK_PLUS) {
                shopModel.increaseQuantity();
                playSound("ui-click");
                repaint();
            } else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                tradeSelectedShopItem();
            }
            return;
        }
        if (inventoryVisible) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_I) {
                inventoryVisible = false;
                keys.clear();
                playSound("ui-close");
                repaint();
            }
            return;
        }
        if (calibratorEnabled && supportsPositionMarker(scene)) {
            if (key == KeyEvent.VK_BACK_SPACE) {
                worldRenderer.undoCalibratedPoint();
                repaint();
                return;
            } else if (key == KeyEvent.VK_C) {
                worldRenderer.clearCalibratedPoints();
                repaint();
                return;
            } else if (key == KeyEvent.VK_P) {
                worldRenderer.dumpCalibratedPoints(scene.name().toLowerCase());
                return;
            }
        }
        if (key == KeyEvent.VK_ESCAPE
            && (scene == GameScene.BEDROOM || scene == GameScene.STREET
                || scene == GameScene.SHOP || scene == GameScene.END)) {
            exitPrompt = true;
            exitPromptSelection = 0;
            keys.clear();
            playSound("ui-select");
            return;
        }
        if (line != null && (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_E || key == KeyEvent.VK_SPACE)) {
            if (dialogueLineComplete()) nextLine();
            else lineAge = Integer.MAX_VALUE / 2;
            return;
        }
        if (key == KeyEvent.VK_I && firstPress && line == null
            && (scene == GameScene.BEDROOM || scene == GameScene.STREET
                || scene == GameScene.SHOP)) {
            inventoryVisible = true;
            keys.clear();
            playSound("ui-open");
            repaint();
            return;
        }
        if (scene == GameScene.SHOP && key == KeyEvent.VK_F && firstPress
            && line == null && near(240, 160)) {
            shopOverlayVisible = true;
            pressedShopAction = ShopRenderer.Action.NONE;
            keys.clear();
            playSound("ui-open");
            repaint();
            return;
        }
        if (scene == GameScene.BOARD && autoTesterOverlayVisible) {
            if ((key == KeyEvent.VK_H && firstPress) || key == KeyEvent.VK_ESCAPE) {
                closeAutoTesterOverlay();
            } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                navigateTesterRecipe(-1);
            } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                navigateTesterRecipe(1);
            } else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                handleAutoTesterAction(AutoTesterRenderer.Action.RUN);
            }
            return;
        }
        if (scene == GameScene.TITLE) {
            int max = SaveManager.hasSave() ? 5 : 4;
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W
                || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                int direction = (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) ? -1 : 1;
                titleSelection = (titleSelection + direction + max) % max;
                playSound("ui-select");
            }
            else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) activateTitleSelection();
            return;
        } else if (scene == GameScene.CONTROLS) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_BACK_SPACE
                || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                scene = GameScene.TITLE;
                playSound("ui-back");
            }
            return;
        } else if (scene == GameScene.SETTINGS) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_BACK_SPACE) {
                leaveSettings();
                playSound("ui-back");
            } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W
                || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                int direction = (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) ? -1 : 1;
                settingsSelection = (settingsSelection + direction + 5) % 5;
                playSound("ui-select");
            } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A
                || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                if (settingsSelection == 4) {
                    taskbarOnRight = !taskbarOnRight;
                    saveCurrentProgress();
                    playSound("ui-click");
                } else {
                    adjustSelectedVolume((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) ? -1 : 1);
                }
            }
            return;
        } else if (scene == GameScene.END && key == KeyEvent.VK_ENTER) {
            resetToTitle();
        } else if (scene == GameScene.STREET && (key == KeyEvent.VK_BACK_SPACE || key == KeyEvent.VK_C || key == KeyEvent.VK_P)) {
            if (key == KeyEvent.VK_BACK_SPACE) worldRenderer.undoCalibratedPoint();
            else if (key == KeyEvent.VK_C) worldRenderer.clearCalibratedPoints();
            else if (key == KeyEvent.VK_P) worldRenderer.dumpCalibratedPoints("street");
            repaint();
        } else if ((scene == GameScene.BEDROOM || scene == GameScene.STREET || scene == GameScene.SHOP)
            && (key == KeyEvent.VK_E || key == KeyEvent.VK_ENTER)) {
            interact();
        } else if (chapter >= 1 && key == KeyEvent.VK_N) {
            if (scene == GameScene.NOTEBOOK) {
                scene = notebookReturnScene;
                playSound("ui-close");
            }
            else {
                notebookReturnScene = scene;
                notebookPage = selectedRecipe;
                scene = GameScene.NOTEBOOK;
                playSound("book-open");
            }
        } else if (scene == GameScene.NOTEBOOK) {
            if (key == KeyEvent.VK_ESCAPE) {
                scene = notebookReturnScene;
                playSound("ui-close");
            }
            else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) turnNotebookPage(-1);
            else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) turnNotebookPage(1);
        } else if (scene == GameScene.BOARD) {
            if (key == KeyEvent.VK_H && firstPress) {
                openAutoTesterOverlay();
            } else if (nodeRadialMenu.isOpen()) {
                if (key == KeyEvent.VK_ESCAPE) {
                    nodeWheelHeld = false;
                    nodeRadialMenu.close();
                    playSound("ui-close");
                }
                return;
            }
            if (key == KeyEvent.VK_E && firstPress) {
                nodeWheelHeld = true;
                openNodeRadialMenu();
            } else if (key == KeyEvent.VK_Q) {
                if (cancelWireInteraction()) {
                    boardMessage = "Cancelled wire.";
                    boardMessageTimer = 90;
                    playSound("ui-back");
                    repaint();
                }
            } else if (key == KeyEvent.VK_ESCAPE) {
                if (workbenchGraph.cancelWire()) {
                    playSound("ui-back");
                } else {
                    scene = returnScene;
                    playSound("ui-close");
                }
            } else if (key == KeyEvent.VK_BACK_SPACE || key == KeyEvent.VK_DELETE) {
                if (workbenchGraph.undoWireCorner()) {
                    boardMessage = "Removed last wire corner.";
                    boardMessageTimer = 90;
                    playSound("ui-back");
                } else if (workbenchGraph.hasPendingWire()) {
                    workbenchGraph.cancelWire();
                    boardMessage = "Cancelled wire.";
                    boardMessageTimer = 90;
                    playSound("ui-back");
                } else if (workbenchGraph.deleteSelected()) {
                    boardMessage = "Deleted selected node and its wires.";
                    boardMessageTimer = 120;
                    playSound("ui-close");
                }
            }
        }
    }

    @Override public void keyReleased(KeyEvent event) {
        int key = event.getKeyCode();
        keys.remove(key);
        if (key != KeyEvent.VK_E || !nodeWheelHeld) return;
        nodeWheelHeld = false;
        if (scene != GameScene.BOARD || !nodeRadialMenu.isOpen()) return;

        int logicalX = mouseX >= 0 ? mouseX : W / 2;
        int logicalY = mouseY >= 0 ? mouseY : H / 2;
        GateType chosen = nodeRadialMenu.releaseAt(
            WorkbenchRenderer.canvasX(logicalX),
            WorkbenchRenderer.canvasY(logicalY));
        if (chosen != null) {
            heldGate = chosen;
            playSound("ui-confirm");
        } else {
            playSound("ui-close");
        }
        repaint();
    }
    @Override public void keyTyped(KeyEvent event) {}

    @Override public void mousePressed(MouseEvent event) {
        requestFocusInWindow();
        int[] point = logicalPoint(event);
        int x = point[0];
        int y = point[1];
        mouseX = x;
        mouseY = y;

        if (exitPrompt) {
            if (inside(x, y, 170, 118, 140, 20)) {
                exitPromptSelection = 0;
                activateExitPromptSelection();
            } else if (inside(x, y, 170, 143, 140, 20)) {
                exitPromptSelection = 1;
                activateExitPromptSelection();
            }
            return;
        }

        if (shopOverlayVisible) {
            int product = shopRenderer.productAt(x, y, shopModel.products().size());
            if (product >= 0) {
                shopModel.select(product);
                playSound("ui-select");
            }
            pressedShopAction = shopRenderer.actionAt(x, y);
            repaint();
            return;
        }

        if (inventoryVisible) return;

        if (dialogueVisible()) {
            if (dialogueLineComplete()) nextLine();
            else lineAge = Integer.MAX_VALUE / 2;
            return;
        }

        if (scene == GameScene.BOARD && autoTesterOverlayVisible) {
            AutoTesterRenderer.Action action = autoTesterRenderer.actionAt(x, y);
            pressedAutoTesterAction = action;
            if (action != AutoTesterRenderer.Action.NONE) handleAutoTesterAction(action);
            else repaint();
            return;
        }

        if (scene == GameScene.TITLE) {
            boolean hasSave = SaveManager.hasSave();
            int max = hasSave ? 5 : 4;
            int startY = hasSave ? 92 : 102;
            for (int i = 0; i < max; i++) {
                int itemY = startY + i * TITLE_MENU_GAP;
                if (inside(x, y, TITLE_MENU_X, itemY, TITLE_MENU_W, TITLE_MENU_H)) {
                    titleSelection = i;
                    activateTitleSelection();
                    return;
                }
            }
            return;
        }
        if (scene == GameScene.CONTROLS) {
            if (inside(x, y, 164, 217, 152, 22)) {
                scene = GameScene.TITLE;
                playSound("ui-back");
            }
            return;
        }
        if (scene == GameScene.SETTINGS) {
            if (inside(x, y, 170, 226, 140, 20)) {
                leaveSettings();
                playSound("ui-back");
            } else {
                for (int row = 0; row < 5; row++) {
                    int rowY = 86 + row * 28;
                    if (inside(x, y, 115, rowY, 265, 20)) {
                        settingsSelection = row;
                        if (row == 4) {
                            taskbarOnRight = !taskbarOnRight;
                            saveCurrentProgress();
                        } else {
                            float volume = (float) clamp((x - 223) / 107.0f, 0.0f, 1.0f);
                            setSelectedVolume(volume);
                        }
                        playSound("ui-click");
                    }
                }
            }
            return;
        }
        if (scene == GameScene.NOTEBOOK) {
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
        if (scene == GameScene.DEV) {
            String[] sections = {"BREAKPOINTS", "SOUNDS", "DEBUG TOOLS", "COLOR CORRECT"};
            for (int i = 0; i < sections.length; i++) {
                int sy = 58 + i * 28;
                if (inside(x, y, 25, sy, 118, 22)) {
                    devSection = i;
                    devFocusRight = true;
                    devSelection = 0;
                    playSound("ui-select");
                    return;
                }
            }
            if (devSection == 0) {
                for (int i = 0; i < DEV_OPTION_COUNT; i++) {
                    int oy = 54 + i * 22;
                    if (inside(x, y, 160, oy, 285, 18)) {
                        devFocusRight = true;
                        devSelection = i;
                        activateDevSelection();
                        return;
                    }
                }
            } else if (devSection == 1) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 56 + i * 18;
                    if (inside(x, y, 160, oy, 285, 17)) {
                        devFocusRight = true;
                        devSelection = i;
                        activateDevSelection();
                        return;
                    }
                }
            } else if (devSection == 2) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 58 + i * 26;
                    if (inside(x, y, 160, oy, 285, 22)) {
                        devFocusRight = true;
                        devSelection = i;
                        activateDevSelection();
                        return;
                    }
                }
            } else if (devSection == 3) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 58 + i * 26;
                    if (inside(x, y, 160, oy, 285, 22)) {
                        devFocusRight = true;
                        devSelection = i;
                        activateDevSelection();
                        return;
                    }
                }
            }
            return;
        }
        if (calibratorEnabled && supportsPositionMarker(scene)) {
            int cameraX = (scene == GameScene.STREET) ? worldRenderer.streetCameraX() : 0;
            int worldX = cameraX + x;
            int worldY = y;
            worldRenderer.addCalibratedPoint(worldX, worldY);
            repaint();
            return;
        }
        if (scene != GameScene.BOARD || line != null
            || event.getButton() != MouseEvent.BUTTON1) return;
        int canvasX = WorkbenchRenderer.canvasX(x);
        int canvasY = WorkbenchRenderer.canvasY(y);
        if (nodeRadialMenu.isOpen()) {
            GateType chosen = nodeRadialMenu.gateAt(canvasX, canvasY);
            nodeRadialMenu.close();
            if (chosen != null) {
                heldGate = chosen;
                playSound("ui-confirm");
            } else {
                playSound("ui-close");
            }
            repaint();
            return;
        }
        if (workbenchRenderer.toggleSwitchAt(x, y)) {
            playSound("ui-click");
            repaint();
            return;
        }

        if (event.isShiftDown()) {
            WorkbenchGraph.EditResult junction = workbenchGraph.addJunctionAt(
                canvasX, canvasY);
            if (junction == WorkbenchGraph.EditResult.JUNCTION_ADDED
                || junction == WorkbenchGraph.EditResult.WIRE_STARTED) {
                beginWireDrag(canvasX, canvasY);
                playSound("ui-confirm");
            } else {
                playSound("ui-error");
            }
            repaint();
            return;
        }

        if (workbenchGraph.beginNodeDrag(canvasX, canvasY)) {
            playSound("ui-select");
            repaint();
            return;
        }

        WorkbenchGraph.EditResult edit = workbenchGraph.click(canvasX, canvasY, heldGate);
        if (edit == WorkbenchGraph.EditResult.WIRE_STARTED) {
            beginWireDrag(canvasX, canvasY);
        }
        switch (edit) {
            case ADDED -> playSound("gate-place");
            case WIRED -> playSound("ui-confirm");
            case WIRE_STARTED, WIRE_CORNER, SELECTED -> playSound("ui-select");
            case JUNCTION_ADDED -> playSound("ui-confirm");
            case INVALID_WIRE -> playSound("ui-error");
            case NONE -> { }
        }
        repaint();
    }

    @Override public void mouseReleased(MouseEvent event) {
        if (shopOverlayVisible) {
            int[] point = logicalPoint(event);
            mouseX = point[0];
            mouseY = point[1];
            ShopRenderer.Action releasedAction = shopRenderer.actionAt(mouseX, mouseY);
            ShopRenderer.Action action = pressedShopAction;
            pressedShopAction = ShopRenderer.Action.NONE;
            if (action == releasedAction) handleShopAction(action);
            repaint();
            return;
        }
        if (scene == GameScene.BOARD && autoTesterOverlayVisible) {
            int[] point = logicalPoint(event);
            mouseX = point[0];
            mouseY = point[1];
            pressedAutoTesterAction = AutoTesterRenderer.Action.NONE;
            repaint();
            return;
        }
        if (scene == GameScene.BOARD && wireDragActive) {
            int[] point = logicalPoint(event);
            mouseX = point[0];
            mouseY = point[1];
            if (wireDragMoved) {
                WorkbenchGraph.EditResult edit = workbenchGraph.finishWireAt(
                    WorkbenchRenderer.canvasX(mouseX),
                    WorkbenchRenderer.canvasY(mouseY));
                if (edit == WorkbenchGraph.EditResult.WIRED) {
                    playSound("ui-confirm");
                } else if (edit == WorkbenchGraph.EditResult.INVALID_WIRE) {
                    playSound("ui-error");
                }
            }
            wireDragActive = false;
            wireDragMoved = false;
            repaint();
            return;
        }
        if (workbenchGraph.endNodeDrag()) repaint();
    }
    @Override public void mouseClicked(MouseEvent event) {}
    @Override public void mouseEntered(MouseEvent event) { requestFocusInWindow(); }
    @Override public void mouseExited(MouseEvent event) {
        mouseX = -1;
        mouseY = -1;
        pressedAutoTesterAction = AutoTesterRenderer.Action.NONE;
        pressedShopAction = ShopRenderer.Action.NONE;
        repaint();
    }
    @Override public void mouseMoved(MouseEvent event) {
        int[] point = logicalPoint(event);
        mouseX = point[0];
        mouseY = point[1];
        if (exitPrompt) {
            int previous = exitPromptSelection;
            if (inside(mouseX, mouseY, 170, 118, 140, 20)) exitPromptSelection = 0;
            else if (inside(mouseX, mouseY, 170, 143, 140, 20)) exitPromptSelection = 1;
            if (previous != exitPromptSelection) playSound("ui-select");
            repaint();
            return;
        }
        if (scene == GameScene.DEV) {
            int prevSection = devSection;
            int prevSelection = devSelection;
            boolean prevFocus = devFocusRight;

            for (int i = 0; i < 4; i++) {
                int sy = 58 + i * 28;
                if (inside(mouseX, mouseY, 25, sy, 118, 22)) {
                    devSection = i;
                    devFocusRight = false;
                    break;
                }
            }

            if (devSection == 0) {
                for (int i = 0; i < DEV_OPTION_COUNT; i++) {
                    int oy = 54 + i * 22;
                    if (inside(mouseX, mouseY, 160, oy, 285, 18)) {
                        devFocusRight = true;
                        devSelection = i;
                        break;
                    }
                }
            } else if (devSection == 1) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 56 + i * 18;
                    if (inside(mouseX, mouseY, 160, oy, 285, 17)) {
                        devFocusRight = true;
                        devSelection = i;
                        break;
                    }
                }
            } else if (devSection == 2) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 58 + i * 26;
                    if (inside(mouseX, mouseY, 160, oy, 285, 22)) {
                        devFocusRight = true;
                        devSelection = i;
                        break;
                    }
                }
            } else if (devSection == 3) {
                for (int i = 0; i < devOptionCount(); i++) {
                    int oy = 58 + i * 26;
                    if (inside(mouseX, mouseY, 160, oy, 285, 22)) {
                        devFocusRight = true;
                        devSelection = i;
                        break;
                    }
                }
            }

            if (prevSection != devSection || prevSelection != devSelection || prevFocus != devFocusRight) {
                playSound("ui-select");
            }
        }
        if (scene == GameScene.TITLE) {
            int previous = titleSelection;
            boolean hasSave = SaveManager.hasSave();
            int max = hasSave ? 5 : 4;
            int startY = hasSave ? 92 : 102;
            for (int i = 0; i < max; i++) {
                int y = startY + i * TITLE_MENU_GAP;
                if (inside(mouseX, mouseY, TITLE_MENU_X, y, TITLE_MENU_W, TITLE_MENU_H)) {
                    titleSelection = i;
                    break;
                }
            }
            if (previous != titleSelection) playSound("ui-select");
        }
        repaint();
    }
    @Override public void mouseDragged(MouseEvent event) {
        if (scene == GameScene.BOARD && autoTesterOverlayVisible) {
            mouseMoved(event);
            return;
        }
        if (scene == GameScene.BOARD && nodeRadialMenu.isOpen()) {
            mouseMoved(event);
            return;
        }
        if (scene == GameScene.BOARD && workbenchGraph.isDraggingNode()) {
            int[] point = logicalPoint(event);
            mouseX = point[0];
            mouseY = point[1];
            workbenchGraph.dragNodeTo(WorkbenchRenderer.canvasX(mouseX),
                WorkbenchRenderer.canvasY(mouseY));
            repaint();
            return;
        }
        if (scene == GameScene.BOARD && wireDragActive) {
            int[] point = logicalPoint(event);
            mouseX = point[0];
            mouseY = point[1];
            int canvasX = WorkbenchRenderer.canvasX(mouseX);
            int canvasY = WorkbenchRenderer.canvasY(mouseY);
            int dx = canvasX - wireDragStartX;
            int dy = canvasY - wireDragStartY;
            if (dx * dx + dy * dy >= 12 * 12) wireDragMoved = true;
            repaint();
            return;
        }
        mouseMoved(event);
    }

    private void beginWireDrag(int canvasX, int canvasY) {
        wireDragActive = true;
        wireDragMoved = false;
        wireDragStartX = canvasX;
        wireDragStartY = canvasY;
    }

    private void handleShopAction(ShopRenderer.Action action) {
        switch (action) {
            case CLOSE -> closeShopOverlay();
            case DECREASE -> {
                shopModel.decreaseQuantity();
                playSound("ui-click");
            }
            case INCREASE -> {
                shopModel.increaseQuantity();
                playSound("ui-click");
            }
            case BUY_SELL -> tradeSelectedShopItem();
            case NONE -> { }
        }
    }

    private void tradeSelectedShopItem() {
        boolean traded = shopModel.trade();
        playSound(traded ? "ui-confirm" : "ui-error");
        if (traded) saveCurrentProgress();
        repaint();
    }

    private void closeShopOverlay() {
        shopOverlayVisible = false;
        pressedShopAction = ShopRenderer.Action.NONE;
        keys.clear();
        playSound("ui-close");
        repaint();
    }

    private boolean cancelWireInteraction() {
        boolean cancelled = workbenchGraph.cancelWire();
        if (wireDragActive) cancelled = true;
        wireDragActive = false;
        wireDragMoved = false;
        return cancelled;
    }

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
        return scene == GameScene.BOARD && inside(mouseX, mouseY, x, y, width, height);
    }

    private void activateTitleSelection() {
        boolean hasSave = SaveManager.hasSave();
        int action = hasSave ? titleSelection : titleSelection + 1;

        if (action == 0) {
            if (loadSavedProgress()) {
                if (scene == GameScene.TITLE || scene == GameScene.INTRO) {
                    scene = GameScene.BEDROOM;
                }
                playSound("ui-confirm");
            } else {
                playSound("ui-error");
            }
            return;
        }
        if (action == 1) {
            playSound("ui-confirm");
            startIntroCutscene();
            return;
        }
        if (action == 2) {
            playSound("ui-confirm");
            scene = GameScene.CONTROLS;
            return;
        }
        if (action == 3) {
            playSound("ui-confirm");
            settingsOpenedFromPause = false;
            scene = GameScene.SETTINGS;
            return;
        }
        if (action == 4) {
            playSound("ui-confirm");
            System.exit(0);
        }
    }

    private void activateDevSelection() {
        if (devSection == 0) {
            loadDevPreset(devSelection);
        } else if (devSection == 1) {
            if (devSelection == 0) {
                String[] scenes = SoundManager.soundScenes();
                soundSceneSelection = (soundSceneSelection + 1) % scenes.length;
                playSound("ui-select");
            }
        } else if (devSection == 2) {
            if (devSelection == 0) {
                calibratorEnabled = !calibratorEnabled;
                playSound("ui-confirm");
            } else if (devSelection == 1) {
                showCollisions = !showCollisions;
                worldRenderer.setShowCollisions(showCollisions);
                playSound("ui-confirm");
            } else if (devSelection == 2) {
                disableHud = !disableHud;
                worldRenderer.setDisableHud(disableHud);
                playSound("ui-confirm");
                saveCurrentProgress();
            } else if (devSelection == 3) {
                instantStart = !instantStart;
                playSound("ui-confirm");
                saveCurrentProgress();
            } else if (devSelection == 4) {
                catAlwaysAppears = !catAlwaysAppears;
                if (catAlwaysAppears && !catPresent) rollCatSpawn();
                playSound("ui-confirm");
                saveCurrentProgress();
            } else if (devSelection == 5) {
                int[] counts = {0, 25, 50, 75, 100, 150, 200};
                int idx = 0;
                for (int i = 0; i < counts.length; i++) {
                    if (starCount == counts[i]) { idx = i; break; }
                }
                starCount = counts[(idx + 1) % counts.length];
                worldRenderer.setStarCount(starCount);
                playSound("ui-confirm");
                saveCurrentProgress();
            } else if (devSelection == 6) {
                int[] counts = {0, 1, 2, 3, 5, 8, 12, 15, 20};
                int idx = 0;
                for (int i = 0; i < counts.length; i++) {
                    if (mothCount == counts[i]) { idx = i; break; }
                }
                mothCount = counts[(idx + 1) % counts.length];
                worldRenderer.setMothCount(mothCount);
                playSound("ui-confirm");
                saveCurrentProgress();
            }
        } else if (devSection == 3) {
            if (devSelection == 0) {
                ccBedroomBackground = !ccBedroomBackground;
                worldRenderer.setBedroomBackground(ccBedroomBackground ? bedroomBackgroundCc : bedroomBackgroundNormal);
                playSound("ui-confirm");
                saveCurrentProgress();
            } else if (devSelection == 1) {
                ccStreetBackground = !ccStreetBackground;
                updateStreetBackground();
                playSound("ui-confirm");
                saveCurrentProgress();
            }
        }
    }

    private void dumpDevState() {
        List<String> modified = new ArrayList<>();
        if (calibratorEnabled) {
            modified.add("    \"light_calibrator\": \"ENABLED\"");
        }
        if (showCollisions) {
            modified.add("    \"show_collisions\": \"ENABLED\"");
        }
        if (disableHud) {
            modified.add("    \"disable_hud\": \"ENABLED\"");
        }
        if (instantStart) {
            modified.add("    \"instant_start\": \"ENABLED\"");
        }
        if (catAlwaysAppears) {
            modified.add("    \"cat_always_appears\": \"ENABLED\"");
        }
        if (starCount != 0) {
            modified.add("    \"star_count\": " + starCount);
        }
        if (mothCount != 3) {
            modified.add("    \"moth_count\": " + mothCount);
        }
        if (ccBedroomBackground) {
            modified.add("    \"bedroom_background_cc\": \"CC\"");
        }
        if (ccStreetBackground) {
            modified.add("    \"street_background_cc\": \"CC\"");
        }

        Map<String, Map<String, Float>> modifiedSounds = sound.modifiedSceneVolumes();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"scene\": \"dev_menu\",\n");
        sb.append("  \"modified_settings\": {");
        if (modified.isEmpty()) {
            sb.append("},\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < modified.size(); i++) {
                sb.append(modified.get(i));
                if (i < modified.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  },\n");
        }

        sb.append("  \"modified_sound_volumes\": {");
        if (modifiedSounds.isEmpty()) {
            sb.append("}\n}");
        } else {
            sb.append("\n");
            int sceneIdx = 0;
            int totalScenes = modifiedSounds.size();
            for (Map.Entry<String, Map<String, Float>> sceneEntry : modifiedSounds.entrySet()) {
                sb.append("    \"").append(sceneEntry.getKey()).append("\": {\n");
                int soundIdx = 0;
                int totalSounds = sceneEntry.getValue().size();
                for (Map.Entry<String, Float> soundEntry : sceneEntry.getValue().entrySet()) {
                    sb.append(String.format(java.util.Locale.US, "      \"%s\": %.2f", soundEntry.getKey(), soundEntry.getValue()));
                    if (soundIdx < totalSounds - 1) sb.append(",");
                    sb.append("\n");
                    soundIdx++;
                }
                sb.append("    }");
                if (sceneIdx < totalScenes - 1) sb.append(",");
                sb.append("\n");
                sceneIdx++;
            }
            sb.append("  }\n}");
        }
        DevLog.log(sb.toString());
    }

    private int devOptionCount() {
        if (devSection == 0) return DEV_OPTION_COUNT;
        if (devSection == 1) {
            return 1 + SoundManager.sceneSounds(
                SoundManager.soundScenes()[soundSceneSelection]).length;
        }
        if (devSection == 2) return 7;
        if (devSection == 3) return 2;
        return 0;
    }

    private void adjustSelectedDevSound(int direction) {
        String sceneName = SoundManager.soundScenes()[soundSceneSelection];
        if (devSelection == 0) {
            String[] scenes = SoundManager.soundScenes();
            soundSceneSelection = (soundSceneSelection + direction + scenes.length) % scenes.length;
        } else {
            String file = SoundManager.sceneSounds(sceneName)[devSelection - 1];
            String resourcePath = AUDIO_ROOT + file;
            sound.setSceneVolume(sceneName, resourcePath,
                sound.sceneVolume(sceneName, resourcePath) + direction * 0.1f);
            sound.preview(resourcePath, sceneName);
        }
        playSound("ui-click");
    }

    private void loadDevPreset(int preset) {
        dialogue.clear();
        line = null;
        completedObjective = null;
        ObjectiveRenderer.reset();
        exitPrompt = false;
        resetAutoTesterState();
        Arrays.fill(crafted, false);
        selectedRecipe = 0;
        notebookPage = 0;
        circuit.selectRecipe(recipes.get(0));

        if (preset == 0) {
            boxRetrieved = false;
            boxOpened = false;
            workbenchInstalled = false;
            startIntroCutscene();
        } else {
            boxRetrieved = true;
            boxOpened = true;
            workbenchInstalled = true;
        }

        switch (preset) {
            case 0 -> {}
            case 1 -> {
                chapter = 1;
                scene = GameScene.BEDROOM;
                setPlayerPosition(205, 126);
            }
            case 2 -> {
                chapter = 1;
                scene = GameScene.STREET;
                rollCatSpawn();
                setPlayerPosition(STREET_HOME_X + 30, STREET_GROUND_Y);
            }
            case 3 -> {
                chapter = 1;
                scene = GameScene.SHOP;
                setPlayerPosition(94, 190);
            }
            case 4 -> {
                chapter = 2;
                scene = GameScene.BEDROOM;
                setPlayerPosition(205, 126);
            }
            case 5 -> {
                chapter = 2;
                crafted[0] = true;
                crafted[1] = true;
                crafted[2] = true;
                scene = GameScene.SHOP;
                setPlayerPosition(94, 190);
            }
            case 6 -> {
                chapter = 3;
                crafted[0] = true;
                crafted[1] = true;
                crafted[2] = true;
                scene = GameScene.BEDROOM;
                setPlayerPosition(205, 126);
            }
            case 7 -> {
                chapter = 3;
                Arrays.fill(crafted, true);
                scene = GameScene.SHOP;
                setPlayerPosition(94, 190);
            }
            case 8 -> {
                chapter = 4;
                Arrays.fill(crafted, true);
                scene = GameScene.END;
            }
        }
        worldRenderer.setBoxRetrieved(boxRetrieved);
        worldRenderer.setBoxOpened(boxOpened);
        worldRenderer.setWorkbenchInstalled(workbenchInstalled);
        updateBedroomBackground();
        updateStreetBackground();
        facing = Facing.DOWN;
        playSound("ui-confirm");
        saveCurrentProgress();
    }

    private void activateExitPromptSelection() {
        playSound("ui-confirm");
        if (exitPromptSelection == 1) {
            resetToTitle();
        } else {
            pausedScene = scene;
            settingsOpenedFromPause = true;
            exitPrompt = false;
            scene = GameScene.SETTINGS;
        }
    }

    private void leaveSettings() {
        if (settingsOpenedFromPause) {
            scene = pausedScene;
            settingsOpenedFromPause = false;
            exitPrompt = true;
            exitPromptSelection = 0;
        } else {
            scene = GameScene.TITLE;
        }
    }

    private void adjustSelectedVolume(int direction) {
        float current = switch (settingsSelection) {
            case 0 -> sound.masterVolume();
            case 1 -> sound.musicVolume();
            case 2 -> sound.fxVolume();
            default -> uiScale;
        };
        float step = settingsSelection == 3 ? 0.05f : 0.1f;
        setSelectedVolume(Math.round((current + direction * step) / step) * step);
        playSound("ui-click");
    }

    private void setSelectedVolume(float volume) {
        switch (settingsSelection) {
            case 0 -> sound.setMasterVolume(volume);
            case 1 -> sound.setMusicVolume(volume);
            case 2 -> sound.setFxVolume(volume);
            default -> uiScale = (float) clamp(volume, 0.75, 1.0);
        }
    }

    private void resetToTitle() {
        saveCurrentProgress();
        exitPrompt = false;
        settingsOpenedFromPause = false;
        Arrays.fill(crafted, false);
        dialogue.clear();
        line = null;
        completedObjective = null;
        ObjectiveRenderer.reset();
        chapter = 0;
        boxRetrieved = false;
        updateBedroomBackground();
        updateStreetBackground();
        resetAutoTesterState();
        selectedRecipe = 0;
        notebookPage = 0;
        circuit.selectRecipe(recipes.get(0));
        setPlayerPosition(210, 157);
        facing = Facing.DOWN;
        walkDistance = 0;
        titleSelection = 0;
        scene = GameScene.TITLE;
    }

    public void saveCurrentProgress() {
        SaveData data = SaveManager.hasSave() ? SaveManager.loadGame() : null;
        if (data == null) data = new SaveData();
        data.instantStart = instantStart;
        data.taskbarOnRight = taskbarOnRight;
        data.catAlwaysAppears = catAlwaysAppears;
        data.ccBedroomBackground = ccBedroomBackground;
        data.ccStreetBackground = ccStreetBackground;
        data.boxRetrieved = boxRetrieved;
        data.boxOpened = boxOpened;
        data.workbenchInstalled = workbenchInstalled;
        data.starCount = starCount;
        data.mothCount = mothCount;
        data.disableHud = disableHud;
        data.shopBalance = shopModel.balance();
        data.gateInventory = shopModel.purchased();

        if (scene != GameScene.TITLE && scene != GameScene.CONTROLS
            && scene != GameScene.SETTINGS && scene != GameScene.DEV && scene != GameScene.INTRO) {
            data.chapter = chapter;
            data.scene = (scene == GameScene.BOARD || scene == GameScene.NOTEBOOK) ? devReturnScene : scene;
            data.playerX = playerX;
            data.playerY = playerY;
            data.facing = facing;
            data.crafted = Arrays.copyOf(crafted, crafted.length);
            data.notebookPage = notebookPage;
            data.autoTesterAttached = autoTester.isAttached();
            data.catPresent = catPresent;
            data.catX = catX;
            data.catY = catY;
        }
        SaveManager.saveGame(data);
    }

    public boolean loadSavedProgress() {
        SaveData data = SaveManager.loadGame();
        if (data == null) return false;
        this.chapter = data.chapter;
        this.scene = (data.scene != null && data.scene != GameScene.TITLE && data.scene != GameScene.INTRO
            && data.scene != GameScene.CONTROLS && data.scene != GameScene.SETTINGS && data.scene != GameScene.DEV) ? data.scene : GameScene.BEDROOM;
        setPlayerPosition(data.playerX, data.playerY);
        this.facing = data.facing != null ? data.facing : Facing.DOWN;
        if (data.crafted != null && data.crafted.length == crafted.length) {
            System.arraycopy(data.crafted, 0, this.crafted, 0, crafted.length);
        }
        this.notebookPage = data.notebookPage;
        this.instantStart = data.instantStart;
        this.taskbarOnRight = data.taskbarOnRight;
        this.catAlwaysAppears = data.catAlwaysAppears;
        this.catPresent = data.catPresent || data.catAlwaysAppears;
        this.catX = data.catX;
        this.catY = data.catY;
        this.ccBedroomBackground = data.ccBedroomBackground;
        this.ccStreetBackground = data.ccStreetBackground;
        this.boxRetrieved = data.boxRetrieved;
        this.boxOpened = data.boxOpened;
        this.workbenchInstalled = data.workbenchInstalled;
        this.starCount = data.starCount;
        this.mothCount = data.mothCount;
        this.disableHud = data.disableHud;
        this.shopModel.restore(data.shopBalance, data.gateInventory);
        updateBedroomBackground();
        updateStreetBackground();
        this.worldRenderer.setStarCount(starCount);
        this.worldRenderer.setMothCount(mothCount);
        this.worldRenderer.setDisableHud(disableHud);
        resetAutoTesterState();
        if (data.autoTesterAttached && !autoTester.isAttached()) {
            this.autoTester.toggleAttachment();
        }
        this.circuit.selectRecipe(recipes.get(0));
        this.dialogue.clear();
        this.line = null;
        this.completedObjective = null;
        this.exitPrompt = false;
        return true;
    }

    static boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static boolean supportsPositionMarker(GameScene targetScene) {
        return targetScene == GameScene.BEDROOM
            || targetScene == GameScene.STREET
            || targetScene == GameScene.SHOP
            || targetScene == GameScene.BOARD;
    }
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

    static void pixelText(Graphics2D g, String text, int x, int y, int scale) {
        pixelTextScaled(g, text, x, y, scale, 1.0f);
    }

    static void drawCenteredPixelText(Graphics2D g, String text, int centerX, int y,
                                              int scale) {
        pixelText(g, text, centerX - pixelTextWidth(g, text, scale) / 2, y, scale);
    }

    static int pixelTextWidth(Graphics2D g, String text, int scale) {
        Font font = PIXEL_FONT.deriveFont(Font.PLAIN,
            Math.max(1, Math.round(PIXEL_FONT_BASE_SIZE * scale * uiScale)));
        return g.getFontMetrics(font).stringWidth(text);
    }

    static void pixelTextScaled(Graphics2D g, String text, int x, int y, int scale,
                                        float sizeFactor) {
        Font old = g.getFont();
        int fontSize = Math.max(1, Math.round(PIXEL_FONT_BASE_SIZE * scale * uiScale * sizeFactor));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g.setFont(PIXEL_FONT.deriveFont(Font.PLAIN, fontSize));
        g.drawString(text, x, y);
        g.setFont(old);
    }

    static void drawWrapped(Graphics2D g, String text, int x, int y, int columns) {
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

}

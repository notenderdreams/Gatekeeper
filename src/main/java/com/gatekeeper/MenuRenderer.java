package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.*;

/** Draws title, controls, settings, developer, and pause menu components. */
final class MenuRenderer {
    private MenuRenderer() {}

    static void drawTitle(Graphics2D g, BufferedImage background, long ticks,
                          int mouseX, int mouseY, int selection) {
        if (background != null) g.drawImage(background, 0, 0, W, H, null);
        g.setColor(new Color(1, 3, 7, 202));
        g.fillRect(0, 0, W, H);
        drawBorder(g);
        drawStars(g, ticks);

        g.setColor(new Color(111, 105, 148));
        boolean hasSave = SaveManager.hasSave();
        String[] options = hasSave ?
            new String[]{"CONTINUE", "BEGIN", "CONTROLS", "SETTINGS", "EXIT"} :
            new String[]{"BEGIN", "CONTROLS", "SETTINGS", "EXIT"};

        int startY = hasSave ? 92 : 102;
        GamePanel.pixelText(g, "MAIN MENU", TITLE_MENU_X + 23, startY - 12, 1);
        g.setColor(new Color(55, 58, 72, 190));
        g.fillRect(220, 86, 1, 101);
        g.setColor(new Color(119, 110, 178));
        g.fillRect(217, 134, 7, 1);
        g.fillRect(220, 131, 1, 7);

        g.setColor(new Color(157, 149, 202));
        GamePanel.drawCenteredPixelText(g, "A LOGIC TALE", TITLE_COPY_CENTER_X, 156, 1);
        g.setColor(new Color(0, 0, 0, 180));
        GamePanel.drawCenteredPixelText(g, "GATEKEEPER", TITLE_COPY_CENTER_X + 2, 141, 3);
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, "GATEKEEPER", TITLE_COPY_CENTER_X, 138, 3);
        g.setColor(new Color(166, 143, 71));
        g.fillOval(418, 65, 34, 34);
        g.setColor(new Color(1, 3, 7));
        g.fillOval(409, 58, 34, 34);
        g.setColor(new Color(115, 111, 164));
        GamePanel.pixelText(g, "+", 452, 146, 1);

        for (int i = 0; i < options.length; i++) {
            int y = startY + i * TITLE_MENU_GAP;
            drawTitleButton(g, mouseX, mouseY, selection, TITLE_MENU_X, y,
                TITLE_MENU_W, TITLE_MENU_H, options[i], i);
        }
    }

    static void drawControls(Graphics2D g, BufferedImage background, int mouseX, int mouseY) {
        if (background != null) g.drawImage(background, 0, 0, W, H, null);
        g.setColor(new Color(3, 7, 11, 211));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(10, 17, 21));
        g.fillRect(54, 24, 372, 222);
        g.setColor(new Color(67, 129, 111));
        g.drawRect(54, 24, 372, 222);
        g.setColor(new Color(152, 95, 47));
        g.drawRect(58, 28, 364, 214);
        g.setColor(INK);
        GamePanel.pixelText(g, "HOW TO PLAY", 148, 54, 2);
        g.setColor(YELLOW);
        g.fillRect(83, 65, 314, 2);
        drawControlSection(g, 77, 79, "EXPLORE", "WASD / ARROWS", "Move Alex",
            "E / ENTER", "Interact and talk", "N", "Open the notebook");
        drawControlSection(g, 250, 79, "WORKBENCH", "MOUSE", "Place gates and test",
            "1 / 2 / 3", "Select AND / OR / NOT", "A / B", "Toggle circuit inputs");
        g.setColor(new Color(18, 39, 39));
        g.fillRect(77, 185, 320, 25);
        g.setColor(CYAN);
        GamePanel.pixelText(g, "ESC", 88, 201, 1);
        g.setColor(INK);
        GamePanel.pixelText(g, "Open the return menu.", 121, 201, 1);
        boolean hovered = GamePanel.inside(mouseX, mouseY, 164, 217, 152, 22);
        g.setColor(hovered ? new Color(71, 63, 26) : new Color(27, 35, 35));
        g.fillRect(164, 217, 152, 22);
        g.setColor(hovered ? YELLOW : INK);
        g.drawRect(164, 217, 152, 22);
        GamePanel.pixelText(g, "< BACK TO TITLE", 192, 232, 1);
    }

    static void drawSettings(Graphics2D g, long ticks, int mouseX, int mouseY,
                             int selection, SoundManager sound, float uiScale) {
        g.setColor(new Color(1, 3, 7));
        g.fillRect(0, 0, W, H);
        drawBorder(g);
        drawStars(g, ticks);
        g.setColor(INK);
        GamePanel.pixelTextScaled(g, "SETTINGS", 198, 56, 2, 0.85f);
        g.setColor(new Color(105, 116, 117));
        GamePanel.pixelTextScaled(g, "AUDIO", 227, 76, 1, 0.85f);
        drawVolumeRow(g, 92, "MASTER", sound.masterVolume(), 0, selection);
        drawVolumeRow(g, 121, "MUSIC", sound.musicVolume(), 1, selection);
        drawVolumeRow(g, 150, "FX", sound.fxVolume(), 2, selection);
        drawVolumeRow(g, 179, "UI SIZE", uiScale, 3, selection);
        boolean hovered = GamePanel.inside(mouseX, mouseY, 170, 207, 140, 20);
        g.setColor(hovered ? new Color(143, 190, 128) : DIM);
        GamePanel.pixelTextScaled(g, "< BACK", 219, 221, 1, 0.85f);
    }

    static void drawDeveloper(Graphics2D g, int mouseX, int mouseY, int section, int selection,
                               boolean focusRight, boolean calibratorEnabled, boolean showCollisions,
                               boolean instantStart, SoundManager sound, int soundSceneSelection) {
        g.setColor(new Color(2, 5, 8));
        g.fillRect(0, 0, W, H);
        drawBorder(g);

        g.setColor(new Color(240, 102, 110));
        GamePanel.drawCenteredPixelText(g, "DEVELOPER MENU", 240, 26, 1);
        g.setColor(new Color(112, 123, 125));
        GamePanel.drawCenteredPixelText(g, "F1 DEBUG & BREAKPOINTS", 240, 38, 1);

        g.setColor(new Color(35, 44, 48));
        g.fillRect(25, 46, 430, 1);
        g.fillRect(150, 52, 1, 205);

        // --- LEFT COLUMN: SECTIONS ---
        String[] sections = {"BREAKPOINTS", "SOUNDS", "DEBUG TOOLS"};
        for (int i = 0; i < sections.length; i++) {
            int y = 58 + i * 28;
            boolean isCurrentSection = (section == i);
            boolean isFocused = !focusRight && isCurrentSection;
            boolean hovered = GamePanel.inside(mouseX, mouseY, 25, y, 118, 22);

            if (isCurrentSection) {
                g.setColor(new Color(28, 52, 45, 200));
                g.fillRect(25, y, 118, 22);
                g.setColor(new Color(60, 120, 100));
                g.drawRect(25, y, 118, 22);
                g.setColor(new Color(143, 190, 128));
                g.fillRect(25, y, 3, 22);
                if (isFocused || hovered) {
                    GamePanel.pixelText(g, ">", 32, y + 15, 1);
                    GamePanel.pixelText(g, sections[i], 42, y + 15, 1);
                } else {
                    GamePanel.pixelText(g, sections[i], 35, y + 15, 1);
                }
            } else if (hovered) {
                g.setColor(new Color(18, 28, 34, 150));
                g.fillRect(25, y, 118, 22);
                g.setColor(new Color(40, 50, 56));
                g.drawRect(25, y, 118, 22);
                g.setColor(new Color(191, 192, 185));
                GamePanel.pixelText(g, ">", 32, y + 15, 1);
                GamePanel.pixelText(g, sections[i], 42, y + 15, 1);
            } else {
                g.setColor(new Color(8, 14, 18, 120));
                g.fillRect(25, y, 118, 22);
                g.setColor(new Color(20, 28, 32));
                g.drawRect(25, y, 118, 22);
                g.setColor(new Color(140, 148, 148));
                GamePanel.pixelText(g, sections[i], 35, y + 15, 1);
            }
        }

        // --- RIGHT COLUMN: OPTIONS ---
        if (section == 0) { // BREAKPOINTS
            String[] options = {"FRESH BEDROOM", "BOARD PROGRESSION", "STREET",
                "SHOP", "FIRST 3 COMPLETE", "AUTO TESTER HOME", "ADVANCED CHAPTER", "ENDING"};

            for (int i = 0; i < options.length; i++) {
                int y = 56 + i * 24;
                boolean isSelected = focusRight && (selection == i);
                boolean hovered = GamePanel.inside(mouseX, mouseY, 160, y, 285, 20);

                if (isSelected) {
                    g.setColor(new Color(25, 48, 42, 190));
                    g.fillRect(160, y, 285, 20);
                    g.setColor(new Color(55, 110, 92, 200));
                    g.drawRect(160, y, 285, 20);
                    g.setColor(new Color(143, 190, 128));
                    g.fillRect(160, y, 3, 20);
                    GamePanel.pixelText(g, ">", 170, y + 14, 1);
                    GamePanel.pixelText(g, options[i], 183, y + 14, 1);
                } else if (hovered) {
                    g.setColor(new Color(18, 32, 28, 160));
                    g.fillRect(160, y, 285, 20);
                    g.setColor(new Color(45, 75, 68, 180));
                    g.drawRect(160, y, 285, 20);
                    g.setColor(new Color(191, 192, 185));
                    GamePanel.pixelText(g, ">", 170, y + 14, 1);
                    GamePanel.pixelText(g, options[i], 183, y + 14, 1);
                } else {
                    g.setColor(new Color(8, 14, 18, 120));
                    g.fillRect(160, y, 285, 20);
                    g.setColor(new Color(20, 28, 32));
                    g.drawRect(160, y, 285, 20);
                    g.setColor(new Color(160, 168, 168));
                    GamePanel.pixelText(g, options[i], 183, y + 14, 1);
                }
            }
        } else if (section == 1) { // SOUNDS
            String[] soundScenes = SoundManager.soundScenes();
            String sceneName = soundScenes[Math.max(0, Math.min(soundSceneSelection, soundScenes.length - 1))];
            String[] soundFiles = SoundManager.sceneSounds(sceneName);
            for (int i = 0; i <= soundFiles.length; i++) {
                int y = 56 + i * 18;
                boolean isSelected = focusRight && selection == i;
                boolean hovered = GamePanel.inside(mouseX, mouseY, 160, y, 285, 17);
                g.setColor(isSelected ? new Color(25, 48, 42, 190)
                    : hovered ? new Color(18, 32, 28, 160) : new Color(8, 14, 18, 120));
                g.fillRect(160, y, 285, 17);
                g.setColor(isSelected ? new Color(55, 110, 92, 200)
                    : hovered ? new Color(45, 75, 68, 180) : new Color(20, 28, 32));
                g.drawRect(160, y, 285, 17);
                g.setColor(isSelected ? new Color(143, 190, 128) : new Color(160, 168, 168));
                if (isSelected) GamePanel.pixelText(g, ">", 170, y + 12, 1);
                String label;
                if (i == 0) {
                    label = "SCENE: " + sceneName;
                } else {
                    String file = soundFiles[i - 1];
                    label = file.substring(0, file.length() - 4);
                    int percent = Math.round(sound.sceneVolume(sceneName, AUDIO_ROOT + file) * 100.0f);
                    GamePanel.pixelText(g, percent + "%", 402, y + 12, 1);
                }
                GamePanel.pixelText(g, label, 183, y + 12, 1);
            }
        } else if (section == 2) { // DEBUG TOOLS
            String[] tools = {
                "GLOBAL POSITION MARKER: " + (calibratorEnabled ? "ON" : "OFF"),
                "SHOW COLLISION AREAS: " + (showCollisions ? "ON" : "OFF"),
                "INSTANT START: " + (instantStart ? "ON" : "OFF")
            };
            boolean[] activeState = {calibratorEnabled, showCollisions, instantStart};

            for (int i = 0; i < tools.length; i++) {
                int y = 58 + i * 26;
                boolean isSelected = focusRight && (selection == i);
                boolean hovered = GamePanel.inside(mouseX, mouseY, 160, y, 285, 22);

                if (isSelected) {
                    g.setColor(new Color(25, 48, 42, 190));
                    g.fillRect(160, y, 285, 22);
                    g.setColor(new Color(55, 110, 92, 200));
                    g.drawRect(160, y, 285, 22);
                    g.setColor(new Color(143, 190, 128));
                    g.fillRect(160, y, 3, 22);
                    GamePanel.pixelText(g, ">", 170, y + 15, 1);
                    g.setColor(activeState[i] ? new Color(143, 190, 128) : new Color(240, 120, 120));
                    GamePanel.pixelText(g, tools[i], 183, y + 15, 1);
                } else if (hovered) {
                    g.setColor(new Color(18, 32, 28, 160));
                    g.fillRect(160, y, 285, 22);
                    g.setColor(new Color(45, 75, 68, 180));
                    g.drawRect(160, y, 285, 22);
                    g.setColor(new Color(191, 192, 185));
                    GamePanel.pixelText(g, ">", 170, y + 15, 1);
                    g.setColor(activeState[i] ? new Color(143, 190, 128) : new Color(200, 160, 160));
                    GamePanel.pixelText(g, tools[i], 183, y + 15, 1);
                } else {
                    g.setColor(new Color(8, 14, 18, 120));
                    g.fillRect(160, y, 285, 22);
                    g.setColor(new Color(20, 28, 32));
                    g.drawRect(160, y, 285, 22);
                    g.setColor(activeState[i] ? new Color(143, 190, 128) : new Color(160, 168, 168));
                    GamePanel.pixelText(g, tools[i], 183, y + 15, 1);
                }
            }
        }
    }

    static void drawPause(Graphics2D g, int mouseX, int mouseY, int selection) {
        g.setColor(new Color(0, 0, 0, 174));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(5, 8, 12, 245));
        g.fillRect(139, 84, 202, 105);
        g.setColor(new Color(151, 155, 151));
        g.drawRect(139, 84, 202, 105);
        g.setColor(new Color(65, 70, 70));
        g.drawRect(143, 88, 194, 97);
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, "PAUSED", 240, 108, 1);
        drawPauseChoice(g, mouseX, mouseY, selection, 170, 118, 140, 20, "SETTINGS", 0);
        drawPauseChoice(g, mouseX, mouseY, selection, 170, 143, 140, 20,
            "RETURN TO MENU", 1);
        g.setColor(new Color(77, 84, 85));
        GamePanel.drawCenteredPixelText(g, "ESC  CLOSE", 240, 179, 1);
    }

    static void drawBorder(Graphics2D g) {
        g.setColor(new Color(150, 154, 151));
        g.drawRect(7, 7, W - 15, H - 15);
        g.setColor(new Color(65, 70, 70));
        g.drawRect(10, 10, W - 21, H - 21);
        g.setColor(new Color(188, 190, 183));
        for (int[] corner : new int[][]{{7, 7}, {W - 8, 7}, {7, H - 8}, {W - 8, H - 8}}) {
            g.fillRect(corner[0] - 2, corner[1] - 2, 5, 5);
        }
    }

    private static void drawStars(Graphics2D g, long ticks) {
        int shimmer = (ticks / 45) % 2 == 0 ? 190 : 115;
        g.setColor(new Color(119, 110, 178, shimmer));
        g.fillRect(181, 43, 1, 11);
        g.fillRect(176, 48, 11, 1);
        g.fillRect(238, 39, 2, 2);
        g.fillRect(354, 57, 2, 2);
        g.fillRect(116, 89, 2, 2);
    }

    private static void drawTitleButton(Graphics2D g, int mouseX, int mouseY, int current,
                                        int x, int y, int width, int height,
                                        String label, int selection) {
        boolean hovered = GamePanel.inside(mouseX, mouseY, x, y, width, height);
        boolean selected = current == selection;

        if (selected) {
            g.setColor(new Color(143, 190, 128));
            GamePanel.pixelText(g, ">", x + 10, y + 13, 1);
            GamePanel.pixelText(g, label, x + 23, y + 13, 1);
        } else if (hovered) {
            g.setColor(new Color(191, 192, 185));
            GamePanel.pixelText(g, ">", x + 10, y + 13, 1);
            GamePanel.pixelText(g, label, x + 23, y + 13, 1);
        } else {
            g.setColor(new Color(160, 168, 168));
            GamePanel.pixelText(g, label, x + 23, y + 13, 1);
        }
    }

    private static void drawVolumeRow(Graphics2D g, int y, String label, float volume,
                                      int row, int selection) {
        boolean selected = selection == row;
        g.setColor(selected ? new Color(143, 190, 128) : INK);
        GamePanel.pixelTextScaled(g, selected ? "> " + label : "  " + label,
            132, y + 12, 1, 0.85f);
        int filled = Math.round(volume * 10.0f);
        for (int i = 0; i < 10; i++) {
            g.setColor(i < filled ? new Color(143, 190, 128) : new Color(45, 53, 55));
            g.fillRect(223 + i * 11, y + 3, 8, 10);
        }
        g.setColor(DIM);
        GamePanel.pixelTextScaled(g, Math.round(volume * 100.0f) + "%", 347, y + 12, 1, 0.85f);
    }

    private static void drawPauseChoice(Graphics2D g, int mouseX, int mouseY, int current,
                                        int x, int y, int width, int height,
                                        String label, int selection) {
        boolean hovered = GamePanel.inside(mouseX, mouseY, x, y, width, height);
        boolean selected = current == selection;
        g.setColor(selected || hovered ? new Color(143, 190, 128) : DIM);
        int textX = x + (width - GamePanel.pixelTextWidth(g, label, 1)) / 2;
        GamePanel.pixelText(g, label, textX, y + 14, 1);
        if (selected || hovered) {
            GamePanel.pixelText(g, ">", textX - GamePanel.pixelTextWidth(g, ">", 1) - 7, y + 14, 1);
        }
    }

    private static void drawControlSection(Graphics2D g, int x, int y, String heading,
                                           String key1, String text1, String key2, String text2,
                                           String key3, String text3) {
        g.setColor(CYAN);
        GamePanel.pixelText(g, heading, x, y, 1);
        String[][] rows = {{key1, text1}, {key2, text2}, {key3, text3}};
        for (int i = 0; i < rows.length; i++) {
            int rowY = y + 22 + i * 27;
            g.setColor(new Color(53, 62, 63));
            g.fillRect(x, rowY - 11, 145, 24);
            g.setColor(YELLOW);
            GamePanel.pixelText(g, rows[i][0], x + 7, rowY, 1);
            g.setColor(INK);
            GamePanel.pixelText(g, rows[i][1], x + 7, rowY + 11, 1);
        }
    }
}

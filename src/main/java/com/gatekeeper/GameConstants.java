package com.gatekeeper;

import java.awt.Color;

/** Shared logical-canvas dimensions, UI geometry, and palette. */
final class GameConstants {
    static final int W = 480;
    static final int H = 270;
    static final int TITLE_MENU_X = 52;
    static final int TITLE_MENU_Y = 92;
    static final int TITLE_MENU_W = 142;
    static final int TITLE_MENU_H = 18;
    static final int TITLE_MENU_GAP = 20;
    static final int TITLE_COPY_CENTER_X = 348;
    static final int DEV_OPTION_COUNT = 8;
    static final int BOARD_SOCKET_W = 26;
    static final int BOARD_SOCKET_H = 18;
    static final int BEDROOM_PLAYER_HEIGHT = 68;
    static final int INDOOR_PLAYER_HEIGHT = 54;
    static final int STREET_PLAYER_HEIGHT = 36;
    static final int STREET_WORLD_WIDTH = 922;
    static final int STREET_HOME_X = 93;
    static final int STREET_BOX_X = 175;
    static final int STREET_BOX_Y = 206;
    static final int STREET_CAT_X = 605;
    static final int[][] CAT_SPAWN_RANGES = {
        { 2, 96, 77 },
        { 184, 406, 152 },
        { 594, 652, 152 }
    };
    static final int STREET_SHOP_X = 870;
    static final int STREET_GROUND_Y = 196;
    static final int PIXEL_FONT_BASE_SIZE = 14;

    static final String AUDIO_ROOT = "/assets/audio/game/";
    static final String LOGICLENS_ITEM_CARD = "@ITEM_LOGICLENS";
    static final String MUSIC_LOOP = "/assets/audio/music/solitude-main.wav";
    static final String ROAD_AMBIENCE = "/assets/audio/ambience/road-ambience.wav";

    static final Color INK = new Color(242, 241, 234);
    static final Color VOID = new Color(10, 10, 14);
    static final Color RED = new Color(244, 63, 74);
    static final Color CYAN = new Color(54, 211, 224);
    static final Color YELLOW = new Color(250, 204, 21);
    static final Color DIM = new Color(100, 104, 112);

    private GameConstants() {}
}

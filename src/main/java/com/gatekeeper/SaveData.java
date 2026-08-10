package com.gatekeeper;

/** Data transfer object containing full serialized game state snapshot. */
public final class SaveData {
    public int chapter;
    public GameScene scene;
    public int playerX;
    public int playerY;
    public Facing facing;
    public boolean[] crafted = new boolean[5];
    public int notebookPage;
    public boolean autoTesterAttached;
    public boolean catPresent;
    public int catX = GameConstants.STREET_CAT_X;
    public int catY = 152;
    public boolean catAlwaysAppears;
    public boolean instantStart;
    public boolean ccBedroomBackground = true;
    public boolean ccStreetBackground = true;
    public int starCount = 25;

    public SaveData() {
        this.chapter = 0;
        this.scene = GameScene.BEDROOM;
        this.playerX = 210;
        this.playerY = 157;
        this.facing = Facing.DOWN;
    }
}

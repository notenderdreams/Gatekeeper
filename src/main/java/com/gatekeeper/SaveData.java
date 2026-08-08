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

    public SaveData() {
        this.chapter = 0;
        this.scene = GameScene.BEDROOM;
        this.playerX = 210;
        this.playerY = 157;
        this.facing = Facing.DOWN;
    }
}

package com.gatekeeper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.gatekeeper.GameConstants.*;

/** Procedural starfield rendered within the street scene sky region above building rooflines. */
final class Starfield {
    private static final int[][] ROOFLINE_POINTS = {
        { 0, 58 }, { 20, 50 }, { 40, 50 }, { 60, 45 }, { 80, 45 },
        { 100, 45 }, { 120, 28 }, { 140, 14 }, { 160, 118 }, { 180, 65 },
        { 200, 62 }, { 220, 64 }, { 240, 57 }, { 260, 58 }, { 280, 60 },
        { 300, 63 }, { 320, 62 }, { 340, 63 }, { 360, 68 }, { 380, 62 },
        { 400, 50 }, { 420, 66 }, { 440, 61 }, { 460, 76 }, { 480, 43 },
        { 500, 68 }, { 520, 58 }, { 540, 69 }, { 560, 66 }, { 580, 60 },
        { 600, 65 }, { 620, 58 }, { 640, 54 }, { 660, 48 }, { 680, 48 },
        { 700, 41 }, { 720, 44 }, { 740, 46 }, { 760, 55 }, { 780, 42 },
        { 800, 48 }, { 820, 47 }, { 840, 39 }, { 860, 60 }, { 880, 55 },
        { 900, 55 }, { 922, 60 }
    };

    static int skyMaxY(int worldX) {
        int x = Math.max(0, Math.min(STREET_WORLD_WIDTH, worldX));
        for (int i = 0; i < ROOFLINE_POINTS.length - 1; i++) {
            int x1 = ROOFLINE_POINTS[i][0];
            int y1 = ROOFLINE_POINTS[i][1];
            int x2 = ROOFLINE_POINTS[i + 1][0];
            int y2 = ROOFLINE_POINTS[i + 1][1];
            if (x >= x1 && x <= x2) {
                if (x1 == x2) return y1;
                float t = (float) (x - x1) / (x2 - x1);
                return Math.round(y1 + t * (y2 - y1));
            }
        }
        return ROOFLINE_POINTS[ROOFLINE_POINTS.length - 1][1];
    }

    private static final class Star {
        final int worldX;
        final int y;
        final int size; // 1, 2, or 3
        final int period; // flicker period in ticks
        final int phase; // flicker phase offset
        final float minAlpha;
        final float maxAlpha;
        final Color color;

        Star(int worldX, int y, int size, int period, int phase, float minAlpha, float maxAlpha, Color color) {
            this.worldX = worldX;
            this.y = y;
            this.size = size;
            this.period = period;
            this.phase = phase;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.color = color;
        }
    }

    private final List<Star> pool = new ArrayList<>();

    Starfield() {
        Random rand = new Random(1337);
        Color[] starColors = {
            new Color(245, 248, 255), // crisp white
            new Color(210, 240, 255), // soft cyan
            new Color(255, 246, 215), // warm gold
            new Color(225, 235, 255)  // subtle blue-white
        };

        for (int i = 0; i < 200; i++) {
            int wx = rand.nextInt(STREET_WORLD_WIDTH);
            int maxY = skyMaxY(wx);
            int safeMaxY = maxY - 18; // Keep generous safety margin above roofline
            if (safeMaxY <= 4) { i--; continue; }
            int wy = 4 + rand.nextInt(Math.max(1, safeMaxY - 4));
            int period = 40 + rand.nextInt(120); // 40-160 ticks
            int phase = rand.nextInt(period);
            float minAlpha = 0.08f + rand.nextFloat() * 0.18f;
            float maxAlpha = 0.45f + rand.nextFloat() * 0.35f;
            Color color = starColors[rand.nextInt(starColors.length)];

            pool.add(new Star(wx, wy, 1, period, phase, minAlpha, maxAlpha, color));
        }
    }

    void draw(Graphics2D g, int cameraX, long ticks, int starCount) {
        if (starCount <= 0) return;
        int count = Math.min(starCount, pool.size());
        Composite oldComp = g.getComposite();

        for (int i = 0; i < count; i++) {
            Star s = pool.get(i);
            int screenX = s.worldX - cameraX;
            if (screenX < -5 || screenX > W + 5) continue;

            double wave = Math.sin((ticks + s.phase) * (2.0 * Math.PI / s.period));
            float alpha = (float) Math.max(0.0, Math.min(1.0, s.minAlpha + (s.maxAlpha - s.minAlpha) * (0.5 + 0.5 * wave)));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(s.color);
            g.fillRect(screenX, s.y, 1, 1);
        }

        g.setComposite(oldComp);
    }
}

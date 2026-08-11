package com.gatekeeper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.*;

/** Reusable procedural environment art for the room, shop, and street scenes. */
final class EnvironmentArt {
    private static final BufferedImage AMBER_GLOW = createRadialLightTexture(
        new Color(255, 239, 178, 180), new Color(250, 180, 40), 160);
    private static final BufferedImage CYAN_GLOW = createRadialLightTexture(
        new Color(218, 253, 255, 190), new Color(54, 211, 224), 160);
    private static final BufferedImage BEDROOM_LAMP_CONE = createBedroomLampConeTexture();

    private EnvironmentArt() {}

    private static BufferedImage createRadialLightTexture(Color centerColor, Color edgeColor, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float radius = size / 2.0f;
        float[] fractions = {0.0f, 0.35f, 0.70f, 1.0f};
        Color[] colors = {
            centerColor,
            new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), (int) (centerColor.getAlpha() * 0.56f)),
            new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), (int) (centerColor.getAlpha() * 0.21f)),
            new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 0)
        };

        RadialGradientPaint paint = new RadialGradientPaint(
            radius, radius, radius, fractions, colors
        );
        g2d.setPaint(paint);
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();
        return image;
    }

    private static BufferedImage createBedroomLampConeTexture() {
        int width = 76;
        int height = 58;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Polygon cone = new Polygon(
            new int[] {37, 5, 38, 70, 50},
            new int[] {5, 37, 52, 45, 8},
            5
        );
        int[] pointX = {37, 5, 38, 70, 50};
        int[] pointY = {5, 37, 52, 45, 8};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!cone.contains(x + 0.5, y + 0.5)) continue;

                float progress = Math.max(0.0f, Math.min(1.0f, (y - 5.0f) / 47.0f));
                float distanceFade = 1.0f - 0.74f * progress;
                float edgeDistance = Float.MAX_VALUE;
                for (int i = 0; i < pointX.length; i++) {
                    int next = (i + 1) % pointX.length;
                    edgeDistance = Math.min(edgeDistance,
                        distanceToSegment(x + 0.5f, y + 0.5f,
                            pointX[i], pointY[i], pointX[next], pointY[next]));
                }
                float edgeFade = Math.min(1.0f, edgeDistance / 3.5f);
                edgeFade = edgeFade * edgeFade * (3.0f - 2.0f * edgeFade);
                int alpha = Math.round(168.0f * distanceFade * edgeFade);
                image.setRGB(x, y, new Color(255, 205, 92, alpha).getRGB());
            }
        }
        return image;
    }

    private static float distanceToSegment(float px, float py, float x1, float y1,
                                           float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lengthSquared = dx * dx + dy * dy;
        float position = lengthSquared == 0.0f ? 0.0f
            : Math.max(0.0f, Math.min(1.0f, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        float nearestX = x1 + position * dx;
        float nearestY = y1 + position * dy;
        return (float) Math.hypot(px - nearestX, py - nearestY);
    }

    static void drawWorldVignette(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 42));
        g.fillRect(0, 20, 9, H - 20);
        g.fillRect(W - 9, 20, 9, H - 20);
        g.fillRect(0, H - 10, W, 10);
    }

    static void drawInteractionGlow(Graphics2D g, int x, int y, Color color, long ticks) {
        int pulse = 3 + (int) ((ticks / 12) % 3);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        g.fillOval(x - 25 - pulse, y - 12 - pulse, 50 + pulse * 2, 25 + pulse * 2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g.drawOval(x - 20 - pulse, y - 9 - pulse, 40 + pulse * 2, 19 + pulse * 2);
    }

    static void drawBedroomLampFlicker(Graphics2D g, long ticks) {
        Composite oldComposite = g.getComposite();

        // Irregular value noise gives the bulb a small electrical shimmer and a slower
        // brightness drift. Some time blocks also contain a short, smoothly recovered dip.
        float opacity = Math.max(0.64f, Math.min(1.0f,
            0.93f * lampFlickerMultiplier(ticks, 0x51A7)));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // User-calibrated outline: source (197,45)-(210,48), spreading through
        // (165,77), (198,92), and an expanded right edge at (230,85).
        g.drawImage(BEDROOM_LAMP_CONE, 160, 40, null);
        g.setComposite(oldComposite);
    }

    private static float lampFlickerMultiplier(long ticks, int seed) {
        float shimmer = smoothNoise(ticks, 3, seed) * 0.055f;
        float drift = smoothNoise(ticks, 17, seed ^ 0x7B29) * 0.045f;
        long shiftedTicks = ticks + Math.floorMod(seed, 53);
        long block = Math.floorDiv(shiftedTicks, 53L);
        float blockPosition = Math.floorMod(shiftedTicks, 53L) / 52.0f;
        float dipChance = unitNoise(block, seed ^ 0x2E17);
        float dipEnvelope = dipChance < 0.30f
            ? (float) Math.pow(Math.sin(Math.PI * blockPosition), 12.0) * 0.22f
            : 0.0f;
        return Math.max(0.66f, Math.min(1.08f, 1.0f + shimmer + drift - dipEnvelope));
    }

    private static float smoothNoise(long ticks, int interval, int seed) {
        long sample = Math.floorDiv(ticks, interval);
        float position = Math.floorMod(ticks, interval) / (float) interval;
        position = position * position * (3.0f - 2.0f * position);
        float current = unitNoise(sample, seed) * 2.0f - 1.0f;
        float next = unitNoise(sample + 1, seed) * 2.0f - 1.0f;
        return current + (next - current) * position;
    }

    private static float unitNoise(long value, int seed) {
        long mixed = value + seed;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        mixed ^= mixed >>> 31;
        return (mixed & 0xFFFFFFL) / (float) 0xFFFFFFL;
    }

    static void drawStreetLampFlicker(Graphics2D g, int cameraX, long ticks) {
        Composite oldComp = g.getComposite();

        // 1. Light #1 (World X: 30, Y: 157) - Far Left Glow (Independent frequency & phase)
        int light1X = 30 - cameraX;
        if (light1X + 90 >= 0 && light1X - 90 <= W) {
            float alpha = Math.min(1.0f, 0.31f * lampFlickerMultiplier(ticks, 0x1421));
            int size = 112;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light1X - size / 2, 157 - size / 2, size, size, null);
        }

        // 2. Light #2 (World X: 109, Y: 151) - Left Lamp Glow (Independent frequency & phase)
        int light2X = 109 - cameraX;
        if (light2X + 90 >= 0 && light2X - 90 <= W) {
            float alpha = Math.min(1.0f, 0.37f * lampFlickerMultiplier(ticks, 0x35A9));
            int size = 130;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light2X - size / 2, 151 - size / 2, size, size, null);
        }

        // 3. Light #3 (World X: 521, Y: 113) - Center Lamp Glow (Independent frequency & phase)
        int light3X = 521 - cameraX;
        if (light3X + 90 >= 0 && light3X - 90 <= W) {
            float alpha = Math.min(1.0f, 0.34f * lampFlickerMultiplier(ticks, 0x58D3));
            int size = 130;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light3X - size / 2, 113 - size / 2, size, size, null);
        }

        // 4. Light #4 (World X: 904, Y: 156) - Mira's Shop Cyan Entrance Glow (Independent frequency & phase)
        int light4X = 904 - cameraX;
        if (light4X + 90 >= 0 && light4X - 90 <= W) {
            float alpha = Math.min(1.0f, 0.39f * lampFlickerMultiplier(ticks, 0x71C5));
            int size = 135;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(CYAN_GLOW, light4X - size / 2, 156 - size / 2, size, size, null);
        }

        g.setComposite(oldComp);
    }

    static void drawShopLights(Graphics2D g, long ticks) {
        Composite oldComp = g.getComposite();

        int[][] points = {
            {184, 25, 0x1842},
            {277, 24, 0x2774},
            {371, 24, 0x3718},
            {43, 61, 0x4361}
        };

        for (int[] pt : points) {
            int px = pt[0];
            int py = pt[1];
            int seed = pt[2];

            float flicker = lampFlickerMultiplier(ticks, seed);
            float baseAlpha = 0.35f * flicker;

            // 1. Soft Overhead Light Cone extending downwards
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, 0.16f * flicker)));
            int coneWidthTop = 16;
            int coneWidthBottom = 90;
            int coneHeight = 155;
            int[] xPoints = {px - coneWidthTop / 2, px + coneWidthTop / 2, px + coneWidthBottom / 2, px - coneWidthBottom / 2};
            int[] yPoints = {py + 4, py + 4, py + coneHeight, py + coneHeight};

            g.setColor(new Color(255, 210, 110, 35));
            g.fillPolygon(xPoints, yPoints, 4);

            // Inner brighter beam core
            int[] innerX = {px - 5, px + 5, px + 32, px - 32};
            g.setColor(new Color(255, 230, 150, 45));
            g.fillPolygon(innerX, yPoints, 4);

            // 2. Radial Amber Glow Halo centered at lamp fixture (x, y)
            int glowSize = 120;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, baseAlpha)));
            g.drawImage(AMBER_GLOW, px - glowSize / 2, py - glowSize / 2 + 8, glowSize, glowSize, null);
        }

        g.setComposite(oldComp);
    }

    static void drawBedroomWindow(Graphics2D g) {
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

    static void drawShopShelf(Graphics2D g, int x, int top, int width) {
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
}

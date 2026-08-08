package com.gatekeeper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
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

    static void drawStreetLampFlicker(Graphics2D g, int cameraX, long ticks) {
        Composite oldComp = g.getComposite();

        // 1. Light #1 (World X: 30, Y: 157) - Far Left Glow (Independent frequency & phase)
        int light1X = 30 - cameraX;
        if (light1X + 90 >= 0 && light1X - 90 <= W) {
            double f1 = Math.sin(ticks * 0.08 + 1.4) * 0.065 + Math.cos(ticks * 0.23 + 0.7) * 0.035;
            double m1 = ((ticks + 3) % 13 == 0) ? -0.05 : 0.0;
            float alphaMult1 = (float) Math.max(0.74, Math.min(1.24, 1.0 + f1 + m1));
            float alpha = Math.min(1.0f, 0.31f * alphaMult1);
            int size = (int) (112 + f1 * 11);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light1X - size / 2, 157 - size / 2, size, size, null);
        }

        // 2. Light #2 (World X: 109, Y: 151) - Left Lamp Glow (Independent frequency & phase)
        int light2X = 109 - cameraX;
        if (light2X + 90 >= 0 && light2X - 90 <= W) {
            double f2 = Math.sin(ticks * 0.11 + 4.2) * 0.06 + Math.cos(ticks * 0.17 + 2.1) * 0.045;
            double m2 = ((ticks + 7) % 19 == 0) ? 0.06 : 0.0;
            float alphaMult2 = (float) Math.max(0.76, Math.min(1.25, 1.0 + f2 + m2));
            float alpha = Math.min(1.0f, 0.37f * alphaMult2);
            int size = (int) (130 + f2 * 13);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light2X - size / 2, 151 - size / 2, size, size, null);
        }

        // 3. Light #3 (World X: 521, Y: 113) - Center Lamp Glow (Independent frequency & phase)
        int light3X = 521 - cameraX;
        if (light3X + 90 >= 0 && light3X - 90 <= W) {
            double f3 = Math.sin(ticks * 0.06 + 2.8) * 0.075 + Math.cos(ticks * 0.29 + 5.3) * 0.03;
            double m3 = ((ticks + 11) % 17 == 0) ? -0.065 : 0.0;
            float alphaMult3 = (float) Math.max(0.72, Math.min(1.26, 1.0 + f3 + m3));
            float alpha = Math.min(1.0f, 0.34f * alphaMult3);
            int size = (int) (130 + f3 * 13);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(AMBER_GLOW, light3X - size / 2, 113 - size / 2, size, size, null);
        }

        // 4. Light #4 (World X: 904, Y: 156) - Mira's Shop Cyan Entrance Glow (Independent frequency & phase)
        int light4X = 904 - cameraX;
        if (light4X + 90 >= 0 && light4X - 90 <= W) {
            double f4 = Math.sin(ticks * 0.13 + 5.1) * 0.052 + Math.cos(ticks * 0.19 + 3.4) * 0.052;
            double m4 = ((ticks + 5) % 23 == 0) ? 0.045 : 0.0;
            float alphaMult4 = (float) Math.max(0.76, Math.min(1.24, 1.0 + f4 + m4));
            float alpha = Math.min(1.0f, 0.39f * alphaMult4);
            int size = (int) (135 + f4 * 12);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(CYAN_GLOW, light4X - size / 2, 156 - size / 2, size, size, null);
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

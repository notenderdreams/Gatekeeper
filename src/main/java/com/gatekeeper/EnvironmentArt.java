package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;

import static com.gatekeeper.GameConstants.*;

/** Reusable procedural environment art for the room, shop, and street scenes. */
final class EnvironmentArt {
    private EnvironmentArt() {}

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

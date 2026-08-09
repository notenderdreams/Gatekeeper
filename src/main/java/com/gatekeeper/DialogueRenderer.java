package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.*;

/** Draws dialogue, interaction prompts, and item-received overlays. */
final class DialogueRenderer {
    private DialogueRenderer() {}

    static void draw(Graphics2D g, String line, int lineAge, long ticks, float uiScale,
                     BufferedImage logicLensImage) {
        if (LOGICLENS_ITEM_CARD.equals(line)) {
            drawLogicLensReceived(g, ticks, logicLensImage);
            return;
        }
        int boxWidth = W;
        int boxHeight = Math.round(61 * uiScale);
        int x = 0;
        int y = H - boxHeight - 9;
        g.setColor(VOID);
        g.fillRect(x, y, boxWidth, boxHeight);
        g.setColor(INK);
        g.setStroke(new BasicStroke(1));
        g.drawLine(x + 1, y + 1, x + boxWidth - 2, y + 1);
        String[] parts = line.split("\\|", 2);
        String speaker = parts.length == 2 ? parts[0] : "";
        String words = parts.length == 2 ? parts[1] : parts[0];
        g.setColor(speaker.equals("MIRA") ? CYAN : speaker.equals("ALEX") ? YELLOW :
            speaker.equals("CAT") ? new Color(250, 180, 70) : INK);
        if (!speaker.isEmpty()) GamePanel.pixelText(g, speaker, x + Math.round(12 * uiScale),
            y + Math.round(17 * uiScale), 1);
        g.setColor(INK);
        int visible = Math.min(words.length(), lineAge / 2 + 1);
        GamePanel.drawWrapped(g, "* " + words.substring(0, visible),
            x + Math.round(12 * uiScale), y + Math.round(34 * uiScale), 72);
        if (visible == words.length() && (ticks / 25) % 2 == 0) {
            GamePanel.pixelText(g, "v", x + boxWidth - Math.round(20 * uiScale),
                y + Math.round(51 * uiScale), 1);
        }
    }

    static void drawPrompt(Graphics2D g, String text, float uiScale) {
        Font promptFont = GamePanel.PIXEL_FONT.deriveFont(Font.PLAIN,
            Math.max(1, Math.round(PIXEL_FONT_BASE_SIZE * uiScale)));
        FontMetrics metrics = g.getFontMetrics(promptFont);
        String[] parts = text.split(" ", 2);
        String key = parts[0];
        String action = parts.length > 1 ? " " + parts[1] : "";
        int gap = Math.max(1, Math.round(3 * uiScale));
        int totalWidth = metrics.stringWidth(key) + gap + metrics.stringWidth(action);
        int x = (W - totalWidth) / 2;
        g.setColor(YELLOW);
        GamePanel.pixelText(g, key, x, 242, 1);
        g.setColor(INK);
        GamePanel.pixelText(g, action, x + metrics.stringWidth(key) + gap, 242, 1);
    }

    private static void drawLogicLensReceived(Graphics2D g, long ticks,
                                               BufferedImage logicLensImage) {
        g.setColor(new Color(0, 0, 0, 196));
        g.fillRect(0, 0, W, H);
        int x = 74, y = 34, width = 332, height = 202;
        g.setColor(new Color(5, 9, 13, 252));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(35, 72, 68));
        g.fillRect(x + 5, y + 5, width - 10, height - 10);
        g.setColor(new Color(7, 12, 16));
        g.fillRect(x + 7, y + 7, width - 14, height - 14);
        g.setColor(new Color(94, 145, 124));
        g.drawRect(x, y, width, height);
        g.setColor(new Color(152, 95, 47));
        g.drawRect(x + 4, y + 4, width - 8, height - 8);
        g.fillRect(x - 2, y + 10, 4, 12);
        g.fillRect(x + width - 1, y + height - 22, 4, 12);
        g.setColor(new Color(204, 180, 135));
        GamePanel.drawCenteredPixelText(g, "MIRA GAVE YOU", W / 2, y + 25, 1);
        g.setColor(new Color(48, 67, 68));
        g.fillRect(x + 18, y + 34, width - 36, 1);
        g.setColor(new Color(143, 190, 128));
        g.fillRect(W / 2 - 18, y + 33, 36, 2);
        g.setColor(new Color(13, 20, 21));
        g.fillRect(x + 18, y + 48, 116, 105);
        g.setColor(new Color(84, 68, 50));
        g.drawRect(x + 18, y + 48, 116, 105);
        g.setColor(new Color(37, 32, 27));
        g.fillRect(x + 24, y + 146, 104, 2);
        if (logicLensImage != null) g.drawImage(logicLensImage, x + 20, y + 51, 112, 100, null);
        int copyX = x + 154;
        g.setColor(new Color(0, 0, 0, 190));
        GamePanel.pixelText(g, "LOGICLENS", copyX + 2, y + 72, 2);
        g.setColor(INK);
        GamePanel.pixelText(g, "LOGICLENS", copyX, y + 70, 2);
        g.setColor(new Color(143, 190, 128));
        GamePanel.pixelText(g, "AUTOMATIC TESTING TOOL", copyX, y + 91, 1);
        g.setColor(new Color(54, 62, 65));
        g.fillRect(copyX, y + 100, 150, 1);
        g.setColor(new Color(183, 185, 180));
        GamePanel.pixelText(g, "TESTS ALL INPUT ROWS", copyX, y + 119, 1);
        GamePanel.pixelText(g, "IN A SINGLE RUN.", copyX, y + 135, 1);
        g.setColor(new Color(45, 39, 29));
        g.fillRect(copyX, y + 145, 153, 23);
        g.setColor(new Color(152, 95, 47));
        g.drawRect(copyX, y + 145, 153, 23);
        g.setColor(new Color(204, 180, 135));
        GamePanel.pixelText(g, "ADDED TO WORKBENCH", copyX + 12, y + 161, 1);
        g.setColor(new Color(48, 67, 68));
        g.fillRect(x + 18, y + 177, width - 36, 1);
        g.setColor((ticks / 28) % 2 == 0 ? INK : new Color(120, 125, 123));
        GamePanel.drawCenteredPixelText(g, "ENTER  CONTINUE", W / 2, y + 193, 1);
    }
}

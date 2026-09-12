package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.*;

/** Draws dialogue, interaction prompts, and item-received overlays. */
final class DialogueRenderer {
    static final Rectangle DELIVER_BUTTON = new Rectangle(296, 233, 114, 20);
    static final Rectangle LEAVE_BUTTON = new Rectangle(416, 233, 52, 20);

    private DialogueRenderer() {}

    static void draw(Graphics2D g, String line, int lineAge, long ticks, float uiScale,
                     BufferedImage logicLensImage, BufferedImage notebookImage,
                     BufferedImage workbenchImage, BufferedImage boxImage,
                     BufferedImage letterImage) {
        draw(g, line, lineAge, ticks, uiScale, logicLensImage, notebookImage,
             workbenchImage, boxImage, letterImage, false, -1, -1);
    }

    static void draw(Graphics2D g, String line, int lineAge, long ticks, float uiScale,
                     BufferedImage logicLensImage, BufferedImage notebookImage,
                     BufferedImage workbenchImage, BufferedImage boxImage,
                     BufferedImage letterImage, boolean showDeliveryOption,
                     int mouseX, int mouseY) {
        if (LETTER_ITEM_CARD.equals(line)) {
            drawLetterPaper(g, uiScale, letterImage);
            return;
        }
        if (LOGICLENS_ITEM_CARD.equals(line)) {
            drawItemReceived(g, ticks, logicLensImage, "MIRA GAVE YOU", "LOGICLENS",
                "AUTOMATIC TESTING TOOL", "TESTS ALL INPUT ROWS", "IN A SINGLE RUN.",
                "ADDED TO WORKBENCH");
            return;
        }
        if (NOTEBOOK_ITEM_CARD.equals(line)) {
            drawItemReceived(g, ticks, notebookImage, "YOU FOUND A", "NOTEBOOK",
                "CIRCUIT REFERENCE", "HOLDS DIAGRAMS AND", "TRUTH TABLES.",
                "PRESS N TO OPEN");
            return;
        }
        if (WORKBENCH_ITEM_CARD.equals(line)) {
            drawItemReceived(g, ticks, workbenchImage, "YOU FOUND A", "WORKBENCH",
                "LOGIC CIRCUIT BUILDER", "PLACE GATES AND TEST", "YOUR CIRCUITS.",
                "READY TO USE");
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
        if (showDeliveryOption) {
            boolean deliverHover = DELIVER_BUTTON.contains(mouseX, mouseY);
            g.setColor(deliverHover ? new Color(42, 98, 54) : new Color(24, 62, 35));
            g.fillRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width, DELIVER_BUTTON.height);
            g.setColor(deliverHover ? YELLOW : new Color(75, 155, 85));
            g.drawRect(DELIVER_BUTTON.x, DELIVER_BUTTON.y, DELIVER_BUTTON.width - 1, DELIVER_BUTTON.height - 1);
            g.setColor(deliverHover ? Color.WHITE : INK);
            GamePanel.pixelText(g, "[D] DELIVER GOODS", DELIVER_BUTTON.x + 6, DELIVER_BUTTON.y + 14, 1);

            boolean leaveHover = LEAVE_BUTTON.contains(mouseX, mouseY);
            g.setColor(leaveHover ? new Color(55, 50, 45) : new Color(30, 28, 26));
            g.fillRect(LEAVE_BUTTON.x, LEAVE_BUTTON.y, LEAVE_BUTTON.width, LEAVE_BUTTON.height);
            g.setColor(leaveHover ? YELLOW : DIM);
            g.drawRect(LEAVE_BUTTON.x, LEAVE_BUTTON.y, LEAVE_BUTTON.width - 1, LEAVE_BUTTON.height - 1);
            g.setColor(leaveHover ? INK : DIM);
            GamePanel.pixelText(g, "LEAVE", LEAVE_BUTTON.x + 11, LEAVE_BUTTON.y + 14, 1);
        } else {
            if (visible == words.length() && (ticks / 25) % 2 == 0) {
                GamePanel.pixelText(g, "v", x + boxWidth - Math.round(20 * uiScale),
                    y + Math.round(51 * uiScale), 1);
            }
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

    private static void drawItemReceived(Graphics2D g, long ticks, BufferedImage itemImage,
                                         String heading, String itemName, String subtitle,
                                         String descriptionOne, String descriptionTwo,
                                         String footer) {
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
        GamePanel.drawCenteredPixelText(g, heading, W / 2, y + 25, 1);
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
        if (itemImage != null) g.drawImage(itemImage, x + 20, y + 51, 112, 100, null);
        int copyX = x + 154;
        g.setColor(new Color(0, 0, 0, 190));
        GamePanel.pixelText(g, itemName, copyX + 2, y + 72, 2);
        g.setColor(INK);
        GamePanel.pixelText(g, itemName, copyX, y + 70, 2);
        g.setColor(new Color(143, 190, 128));
        GamePanel.pixelText(g, subtitle, copyX, y + 91, 1);
        g.setColor(new Color(54, 62, 65));
        g.fillRect(copyX, y + 100, 150, 1);
        g.setColor(new Color(183, 185, 180));
        GamePanel.pixelText(g, descriptionOne, copyX, y + 119, 1);
        GamePanel.pixelText(g, descriptionTwo, copyX, y + 135, 1);
        g.setColor(new Color(45, 39, 29));
        g.fillRect(copyX, y + 145, 153, 23);
        g.setColor(new Color(152, 95, 47));
        g.drawRect(copyX, y + 145, 153, 23);
        g.setColor(new Color(204, 180, 135));
        GamePanel.drawCenteredPixelText(g, footer, copyX + 76, y + 161, 1);
        g.setColor(new Color(48, 67, 68));
        g.fillRect(x + 18, y + 177, width - 36, 1);
        g.setColor((ticks / 28) % 2 == 0 ? INK : new Color(120, 125, 123));
        GamePanel.drawCenteredPixelText(g, "ENTER  CONTINUE", W / 2, y + 193, 1);
    }

    private static void drawLetterPaper(Graphics2D g, float uiScale, BufferedImage letterImage) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, W, H);

        if (letterImage != null) {
            int drawHeight = 220;
            int drawWidth = Math.round(drawHeight * ((float) letterImage.getWidth() / letterImage.getHeight()));
            int x = (W - drawWidth) / 2;
            int y = (H - drawHeight) / 2 - 10;

            g.setColor(new Color(10, 6, 3, 160));
            g.fillRect(x + 4, y + 4, drawWidth, drawHeight);
            g.drawImage(letterImage, x, y, drawWidth, drawHeight, null);
        } else {
            int width = 330;
            int height = 175;
            int x = (W - width) / 2;
            int y = (H - height) / 2 - 12;

            g.setColor(new Color(12, 8, 4, 150));
            g.fillRect(x + 5, y + 5, width, height);

            g.setColor(new Color(245, 237, 216));
            g.fillRect(x, y, width, height);

            g.setColor(new Color(234, 223, 197));
            g.fillRect(x + 5, y + 5, width - 10, height - 10);

            g.setColor(new Color(115, 85, 55));
            g.drawRect(x, y, width, height);
            g.setColor(new Color(185, 155, 115));
            g.drawRect(x + 3, y + 3, width - 6, height - 6);

            int sealX = x + width - 36;
            int sealY = y + height - 36;
            g.setColor(new Color(150, 32, 32));
            g.fillOval(sealX, sealY, 22, 22);
            g.setColor(new Color(180, 48, 48));
            g.fillOval(sealX + 2, sealY + 2, 18, 18);
            g.setColor(new Color(230, 190, 120));
            GamePanel.pixelText(g, "G&G", sealX + 4, sealY + 15, 1);

            int textX = x + 24;
            int startY = y + 30;
            int lineGap = 19;

            g.setColor(new Color(35, 25, 16));
            GamePanel.pixelText(g, "Dear Alex,", textX, startY, 1);
            GamePanel.pixelText(g, "We found this mystery box in your late uncle's attic.", textX, startY + lineGap, 1);
            GamePanel.pixelText(g, "He was a circuit engineer and left these tools behind.", textX, startY + lineGap * 2, 1);
            GamePanel.pixelText(g, "We thought they might be useful for your workshop!", textX, startY + lineGap * 3, 1);
            GamePanel.pixelText(g, "Take care, kiddo.", textX, startY + lineGap * 4 + 4, 1);

            g.setColor(new Color(110, 40, 20));
            GamePanel.pixelText(g, "- Love, Grandma & Grandpa", textX + 90, startY + lineGap * 5 + 4, 1);
        }

        drawPrompt(g, "E CLOSE LETTER", uiScale);
    }
}

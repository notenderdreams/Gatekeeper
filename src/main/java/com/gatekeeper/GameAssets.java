package com.gatekeeper;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Resource loading and one-time preprocessing for game art and fonts. */
final class GameAssets {
    private GameAssets() {}

    static Font loadPixelFont() {
        try (InputStream stream = GameAssets.class.getResourceAsStream("/assets/font.ttf")) {
            if (stream != null) return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (FontFormatException | IOException ignored) {
            // Fall back to a logical monospaced font if the packaged font is unavailable.
        }
        return new Font(Font.MONOSPACED, Font.BOLD, 10);
    }

    static BufferedImage loadBackground(String path) {
        try (InputStream stream = GameAssets.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            BufferedImage source = ImageIO.read(stream);
            BufferedImage scaled = new BufferedImage(GameConstants.W, GameConstants.H,
                BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, GameConstants.W, GameConstants.H, null);
            graphics.dispose();
            return scaled;
        } catch (IOException error) {
            return null;
        }
    }

    static BufferedImage loadStreetBackground(String path) {
        try (InputStream stream = GameAssets.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            BufferedImage source = ImageIO.read(stream);
            int cropTop = Math.min(90, source.getHeight() - 1);
            int cropHeight = Math.min(600, source.getHeight() - cropTop);
            BufferedImage scaled = new BufferedImage(GameConstants.STREET_WORLD_WIDTH,
                GameConstants.H, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, GameConstants.STREET_WORLD_WIDTH, GameConstants.H,
                0, cropTop, source.getWidth(), cropTop + cropHeight, null);
            graphics.dispose();
            return scaled;
        } catch (IOException error) {
            return null;
        }
    }

    static BufferedImage loadRawImage(String path) {
        try (InputStream stream = GameAssets.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException error) {
            return null;
        }
    }

    static Rectangle[] buildFrameBounds(BufferedImage sheet, int columns, int rows) {
        if (sheet == null) return new Rectangle[0];
        Rectangle[] frames = new Rectangle[columns * rows];
        int cellWidth = sheet.getWidth() / columns;
        int cellHeight = sheet.getHeight() / rows;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int cellX = column * cellWidth;
                int cellY = row * cellHeight;
                int minX = cellX + cellWidth;
                int minY = cellY + cellHeight;
                int maxX = cellX;
                int maxY = cellY;
                for (int y = cellY; y < cellY + cellHeight; y++) {
                    for (int x = cellX; x < cellX + cellWidth; x++) {
                        int alpha = (sheet.getRGB(x, y) >>> 24) & 0xff;
                        if (alpha <= 24) continue;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
                frames[row * columns + column] = maxX < minX || maxY < minY
                    ? new Rectangle(cellX, cellY, cellWidth, cellHeight)
                    : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            }
        }
        return frames;
    }

    static Rectangle[] buildCellBounds(BufferedImage sheet, int columns, int rows) {
        if (sheet == null) return new Rectangle[0];
        Rectangle[] frames = new Rectangle[columns * rows];
        int cellWidth = sheet.getWidth() / columns;
        int cellHeight = sheet.getHeight() / rows;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                frames[row * columns + column] = new Rectangle(
                    column * cellWidth, row * cellHeight, cellWidth, cellHeight);
            }
        }
        return frames;
    }
}

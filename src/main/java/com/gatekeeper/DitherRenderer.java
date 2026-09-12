package com.gatekeeper;

import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.CYAN;
import static com.gatekeeper.GameConstants.INK;
import static com.gatekeeper.GameConstants.YELLOW;

/** Applies animated ordered color quantization to playable scene art. */
final class DitherRenderer {
    private static final int COLOR_LEVELS = 20;
    private static final int[][] BAYER_4X4 = {
        {0, 8, 2, 10},
        {12, 4, 14, 6},
        {3, 11, 1, 9},
        {15, 7, 13, 5}
    };
    private static final double[][] GRAIN_PHASE_4X4 = {
        {0.19, 2.41, 4.77, 1.08},
        {3.56, 5.84, 0.62, 2.96},
        {4.11, 1.73, 3.24, 5.31},
        {2.18, 4.48, 0.91, 3.88}
    };

    private DitherRenderer() {}

    static boolean appliesTo(GameScene scene, boolean enabled) {
        return enabled && switch (scene) {
            case BEDROOM, STREET, SHOP -> true;
            default -> false;
        };
    }

    static void applyColorDither(BufferedImage playfield, long ticks) {
        // Each Bayer cell drifts at its own smooth phase. This avoids the old
        // whole-pattern jump and produces a gentle, film-grain-like shimmer.
        int[][] thresholds = animatedThresholds(ticks);
        for (int y = 0; y < playfield.getHeight(); y++) {
            for (int x = 0; x < playfield.getWidth(); x++) {
                int argb = playfield.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                int sourceRed = (argb >>> 16) & 0xFF;
                int sourceGreen = (argb >>> 8) & 0xFF;
                int sourceBlue = argb & 0xFF;
                if (isKeyTextColor(sourceRed, sourceGreen, sourceBlue)) continue;
                int threshold = thresholds[y & 3][x & 3];
                int red = quantize(sourceRed, threshold);
                int green = quantize(sourceGreen, threshold);
                int blue = quantize(sourceBlue, threshold);
                playfield.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
    }

    private static int[][] animatedThresholds(long ticks) {
        int[][] thresholds = new int[4][4];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                double phase = GRAIN_PHASE_4X4[y][x];
                double drift = Math.sin(ticks * 0.29 + phase) * 0.95
                    + Math.sin(ticks * 0.11 + phase * 1.7) * 0.55;
                thresholds[y][x] = clamp(BAYER_4X4[y][x] + (int) Math.round(drift), 0, 15);
            }
        }
        return thresholds;
    }

    private static int quantize(int component, int threshold) {
        int scaled = component * (COLOR_LEVELS - 1);
        int lower = scaled / 255;
        int remainder = scaled % 255;
        if (lower < COLOR_LEVELS - 1 && remainder * 16 > (threshold + 1) * 255) {
            lower++;
        }
        return Math.round(lower * 255.0f / (COLOR_LEVELS - 1));
    }

    private static boolean isKeyTextColor(int red, int green, int blue) {
        return matches(red, green, blue, INK.getRGB())
            || matches(red, green, blue, YELLOW.getRGB())
            || matches(red, green, blue, CYAN.getRGB());
    }

    private static boolean matches(int red, int green, int blue, int color) {
        return red == ((color >>> 16) & 0xFF)
            && green == ((color >>> 8) & 0xFF)
            && blue == (color & 0xFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

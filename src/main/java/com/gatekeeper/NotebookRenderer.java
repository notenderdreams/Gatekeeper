package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/** Renders and maps interactions for the annotated open-notebook artwork. */
final class NotebookRenderer {
    enum Section {
        GATE_INFO,
        TRUTH_TABLES,
        EMPTY
    }

    private static final int SOURCE_WIDTH = 1355;
    private static final int SOURCE_HEIGHT = 1050;
    private static final double BOOK_SCALE = H / (double) SOURCE_HEIGHT;
    private static final double BOOK_X = (W - SOURCE_WIDTH * BOOK_SCALE) / 2.0;

    // Source-space regions from assets/ui/notebook.annotations.json.
    private static final Rectangle PAGE_LEFT = new Rectangle(75, 56, 538, 902);
    private static final Rectangle PAGE_RIGHT = new Rectangle(725, 60, 522, 901);
    private static final Rectangle[] TAB_REGIONS = {
        new Rectangle(1274, 134, 49, 101),
        new Rectangle(1275, 254, 49, 94),
        new Rectangle(1274, 364, 54, 94)
    };
    private static final Rectangle PREVIOUS_PAGE = new Rectangle(105, 892, 85, 55);
    private static final Rectangle NEXT_PAGE = new Rectangle(1138, 892, 85, 55);

    private static final Color PAGE_ACCENT = new Color(126, 68, 37);

    private final List<CircuitRecipe> recipes;
    private final boolean[] crafted;
    private final BufferedImage notebookImage;
    private final BufferedImage highlightedNotebookImage;

    NotebookRenderer(List<CircuitRecipe> recipes, boolean[] crafted,
                     BufferedImage notebookImage) {
        this.recipes = recipes;
        this.crafted = crafted;
        this.notebookImage = notebookImage;
        this.highlightedNotebookImage = brightenAndSaturate(notebookImage);
    }

    int drawNotebook(Graphics2D g, int chapter, Section section, int page) {
        drawNotebookArt(g);
        drawSelectedTab(g, section);

        int selectedPage = clamp(page, 0, pageCount(section, chapter) - 1);
        switch (section) {
            case GATE_INFO -> NotebookComponents.drawGateInfo(
                g, GateType.values()[selectedPage], selectedPage, GateType.values().length,
                screenBounds(PAGE_LEFT), screenBounds(PAGE_RIGHT));
            case TRUTH_TABLES -> NotebookComponents.drawTruthTableSpread(
                g, recipes.get(selectedPage), crafted[selectedPage], selectedPage,
                pageCount(section, chapter), screenBounds(PAGE_LEFT), screenBounds(PAGE_RIGHT));
            case EMPTY -> {
                // This tab is intentionally an untouched pair of notebook pages.
            }
        }

        if (pageCount(section, chapter) > 1) {
            drawPageControls(g);
        }
        return selectedPage;
    }

    int pageCount(Section section, int chapter) {
        return switch (section) {
            case GATE_INFO -> GateType.values().length;
            case TRUTH_TABLES -> Math.min(recipes.size(), chapter >= 3 ? 5 : 3);
            case EMPTY -> 1;
        };
    }

    Section sectionAt(int x, int y) {
        for (int index = 0; index < TAB_REGIONS.length; index++) {
            if (contains(TAB_REGIONS[index], x, y)) return Section.values()[index];
        }
        return null;
    }

    int pageDirectionAt(int x, int y) {
        if (contains(PREVIOUS_PAGE, x, y)) return -1;
        if (contains(NEXT_PAGE, x, y)) return 1;
        return 0;
    }

    private void drawNotebookArt(Graphics2D graphics) {
        graphics.setColor(new Color(11, 8, 6));
        graphics.fillRect(0, 0, W, H);
        if (notebookImage == null) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(notebookImage, bookTransform(), null);
        g.dispose();
    }

    private void drawSelectedTab(Graphics2D graphics, Section section) {
        if (highlightedNotebookImage == null) return;
        Rectangle tab = screenBounds(TAB_REGIONS[section.ordinal()]);
        Graphics2D g = (Graphics2D) graphics.create();
        Shape oldClip = g.getClip();
        g.clip(tab);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(highlightedNotebookImage, bookTransform(), null);
        g.setClip(oldClip);
        g.dispose();
    }

    private static void drawPageControls(Graphics2D g) {
        Rectangle previous = screenBounds(PREVIOUS_PAGE);
        Rectangle next = screenBounds(NEXT_PAGE);
        g.setColor(PAGE_ACCENT);
        GamePanel.drawCenteredPixelText(g, "<", centerX(previous), previous.y + 13, 1);
        GamePanel.drawCenteredPixelText(g, ">", centerX(next), next.y + 13, 1);
    }

    private static AffineTransform bookTransform() {
        AffineTransform transform = AffineTransform.getTranslateInstance(BOOK_X, 0.0);
        transform.scale(BOOK_SCALE, BOOK_SCALE);
        return transform;
    }

    private static Rectangle screenBounds(Rectangle source) {
        int left = (int) Math.floor(BOOK_X + source.x * BOOK_SCALE);
        int top = (int) Math.floor(source.y * BOOK_SCALE);
        int right = (int) Math.ceil(BOOK_X + (source.x + source.width) * BOOK_SCALE);
        int bottom = (int) Math.ceil((source.y + source.height) * BOOK_SCALE);
        return new Rectangle(left, top, right - left, bottom - top);
    }

    private static boolean contains(Rectangle source, int x, int y) {
        double sourceX = (x - BOOK_X) / BOOK_SCALE;
        double sourceY = y / BOOK_SCALE;
        return sourceX >= source.x && sourceX < source.x + source.width
            && sourceY >= source.y && sourceY < source.y + source.height;
    }

    private static BufferedImage brightenAndSaturate(BufferedImage source) {
        if (source == null) return null;
        BufferedImage adjusted = new BufferedImage(
            source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                double red = (argb >>> 16) & 0xff;
                double green = (argb >>> 8) & 0xff;
                double blue = argb & 0xff;
                double luminance = red * 0.2126 + green * 0.7152 + blue * 0.0722;

                // +30 saturation, then +1.0 exposure (one stop).
                red = (luminance + (red - luminance) * 1.30) * 2.0;
                green = (luminance + (green - luminance) * 1.30) * 2.0;
                blue = (luminance + (blue - luminance) * 1.30) * 2.0;
                adjusted.setRGB(x, y, (alpha << 24)
                    | (clampChannel(red) << 16)
                    | (clampChannel(green) << 8)
                    | clampChannel(blue));
            }
        }
        return adjusted;
    }

    private static int clampChannel(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(255.0, value)));
    }

    private static int centerX(Rectangle bounds) {
        return bounds.x + bounds.width / 2;
    }

    void drawEnding(Graphics2D g) {
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, "THE SIGNAL IS CLEAR.", W / 2, 78, 2);
        g.setColor(CYAN);
        GamePanel.drawCenteredPixelText(g, "Mira pins Alex's circuits above the counter.", W / 2, 119, 1);
        GamePanel.drawCenteredPixelText(g, "Tomorrow, the notebook has harder pages.", W / 2, 136, 1);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "But tonight, every little light is on.", W / 2, 169, 1);
        g.setColor(RED);
        drawHeart(g, (W - 12) / 2, 194);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g, "ENTER: begin again", W / 2, 238, 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawHeart(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 3, 3);
        g.fillRect(x + 5, y, 3, 3);
        g.fillRect(x - 2, y + 3, 12, 5);
        g.fillRect(x, y + 8, 8, 3);
        g.fillRect(x + 2, y + 11, 4, 3);
    }
}

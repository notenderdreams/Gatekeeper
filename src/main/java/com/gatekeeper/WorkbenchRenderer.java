package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.H;
import static com.gatekeeper.GameConstants.W;

/** Draws the asset-backed workbench shell and its temporary visual-only I/O controls. */
final class WorkbenchRenderer {
    private static final int CANVAS_WIDTH = 1536;
    private static final int CANVAS_HEIGHT = 1024;
    private static final int CONTROL_COUNT = 8;

    // Regions from canvas.annotations.json, expressed in the source canvas coordinate space.
    private static final int INPUT_REGION_X = 213;
    private static final int INPUT_REGION_Y = 850;
    private static final int INPUT_REGION_WIDTH = 919;
    private static final int INPUT_REGION_HEIGHT = 115;
    private static final int OUTPUT_REGION_X = 52;
    private static final int OUTPUT_REGION_Y = 108;
    private static final int OUTPUT_REGION_WIDTH = 85;
    private static final int OUTPUT_REGION_HEIGHT = 659;

    private static final int SWITCH_Y = 880;
    private static final int INPUT_NUMBER_BASELINE_Y = 875;
    private static final int OUTPUT_LED_X = 77;
    private static final int OUTPUT_LED_WIDTH = 55;
    private static final int OUTPUT_LED_HEIGHT = 64;
    private static final int OUTPUT_NUMBER_X = 53;

    private final BufferedImage canvasImage;
    private final BufferedImage lightOffImage;
    private final BufferedImage switchImage;
    private final BufferedImage lightOnImage;
    private final LogicNodeRenderer nodeRenderer;
    private final boolean[] switches = new boolean[CONTROL_COUNT];

    WorkbenchRenderer(BufferedImage canvasImage, BufferedImage lightOffImage,
                      BufferedImage switchImage, BufferedImage lightOnImage,
                      BufferedImage nodeTextureImage, BufferedImage wireEndImage) {
        this.canvasImage = canvasImage;
        this.lightOffImage = lightOffImage;
        this.switchImage = switchImage;
        this.lightOnImage = lightOnImage;
        nodeRenderer = new LogicNodeRenderer(nodeTextureImage, wireEndImage);
    }

    void draw(Graphics2D graphics, CircuitRecipe recipe) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.scale(W / (double) CANVAS_WIDTH, H / (double) CANVAS_HEIGHT);

        if (canvasImage != null) {
            g.drawImage(canvasImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }

        drawNodePreview(g, recipe);
        drawOutputs(g);
        drawInputs(g);
        g.dispose();
    }

    boolean toggleSwitchAt(int logicalX, int logicalY) {
        if (switchImage == null) return false;

        double sourceX = logicalX * CANVAS_WIDTH / (double) W;
        double sourceY = logicalY * CANVAS_HEIGHT / (double) H;
        int switchWidth = switchImage.getWidth();
        int switchHeight = switchImage.getHeight();
        double cellWidth = INPUT_REGION_WIDTH / (double) CONTROL_COUNT;

        for (int index = 0; index < CONTROL_COUNT; index++) {
            double x = inputSwitchX(index, switchWidth, cellWidth);
            if (sourceX >= x && sourceX <= x + switchWidth
                && sourceY >= SWITCH_Y && sourceY <= SWITCH_Y + switchHeight) {
                switches[index] = !switches[index];
                return true;
            }
        }
        return false;
    }

    private void drawInputs(Graphics2D g) {
        if (switchImage == null) return;

        int switchWidth = switchImage.getWidth();
        int switchHeight = switchImage.getHeight();
        double cellWidth = INPUT_REGION_WIDTH / (double) CONTROL_COUNT;
        g.setColor(new Color(225, 187, 105));

        for (int index = 0; index < CONTROL_COUNT; index++) {
            int x = (int) Math.round(inputSwitchX(index, switchWidth, cellWidth));
            int centerX = (int) Math.round(INPUT_REGION_X + (index + 0.5) * cellWidth);
            GamePanel.drawCenteredPixelText(g, Integer.toString(index), centerX,
                INPUT_NUMBER_BASELINE_Y, 2);
            drawSwitch(g, x, SWITCH_Y, switchWidth, switchHeight, switches[index]);
        }
    }

    private void drawOutputs(Graphics2D g) {
        if (lightOffImage == null || lightOnImage == null) return;

        double rowHeight = OUTPUT_REGION_HEIGHT / (double) CONTROL_COUNT;
        g.setColor(new Color(225, 187, 105));
        for (int index = 0; index < CONTROL_COUNT; index++) {
            BufferedImage light = switches[index] ? lightOnImage : lightOffImage;
            int y = (int) Math.round(OUTPUT_REGION_Y + index * rowHeight
                + (rowHeight - OUTPUT_LED_HEIGHT) / 2.0);
            int numberBaseline = (int) Math.round(
                OUTPUT_REGION_Y + (index + 0.5) * rowHeight + 10);
            GamePanel.pixelText(g, Integer.toString(index), OUTPUT_NUMBER_X,
                numberBaseline, 2);
            g.drawImage(light, OUTPUT_LED_X, y, OUTPUT_LED_WIDTH, OUTPUT_LED_HEIGHT, null);
        }
    }

    private void drawNodePreview(Graphics2D g, CircuitRecipe recipe) {
        int[][] layout = nodeLayout(recipe);
        for (int index = 0; index < recipe.solution.length; index++) {
            int[] position = layout[index];
            nodeRenderer.draw(g, position[0], position[1], recipe.solution[index]);
        }
    }

    private static int[][] nodeLayout(CircuitRecipe recipe) {
        return switch (recipe.name) {
            case "XOR" -> new int[][]{
                {500, 500}, {790, 500}, {500, 230}, {790, 230}, {1130, 365}
            };
            case "XNOR" -> new int[][]{
                {430, 230}, {430, 500}, {700, 500}, {950, 365}, {1190, 365}
            };
            case "IMPLY" -> new int[][]{{570, 365}, {1010, 365}};
            default -> new int[][]{{570, 365}, {1010, 365}};
        };
    }

    private void drawSwitch(Graphics2D g, int x, int y, int width, int height, boolean on) {
        if (on) {
            g.drawImage(switchImage, x, y, null);
            return;
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(x + width, y + height);
        transform.rotate(Math.PI);
        g.drawImage(switchImage, transform, null);
    }

    private static double inputSwitchX(int index, int switchWidth, double cellWidth) {
        return INPUT_REGION_X + index * cellWidth + (cellWidth - switchWidth) / 2.0;
    }
}

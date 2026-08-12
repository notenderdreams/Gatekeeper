package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Draws the physical cable layer beneath workbench logic nodes. */
final class CircuitWireRenderer {
    private static final int INPUT_X = 340;
    private static final int INPUT_A_Y = 300;
    private static final int INPUT_B_Y = 455;
    private static final int OUTPUT_X = 1390;
    private static final int ENDPOINT_RENDER_HEIGHT = 36;

    private static final Color SHADOW = new Color(50, 34, 20, 88);
    private static final Color CABLE_EDGE = new Color(18, 20, 18);
    private static final Color CABLE_FACE = new Color(46, 49, 43);
    private static final Color CABLE_HIGHLIGHT = new Color(150, 145, 116, 125);

    private final BufferedImage endpointImage;

    CircuitWireRenderer(BufferedImage endpointImage) {
        this.endpointImage = endpointImage;
    }

    void draw(Graphics2D graphics, CircuitRecipe recipe, int[][] layout) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);

        for (int nodeIndex = 0; nodeIndex < recipe.slotCount(); nodeIndex++) {
            GateType gate = recipe.solution[nodeIndex];
            drawSource(g, recipe, layout, recipe.leftSources[nodeIndex],
                nodeIndex, 0);
            if (gate.inputPorts > 1) {
                drawSource(g, recipe, layout, recipe.rightSources[nodeIndex],
                    nodeIndex, 1);
            }
        }

        int lastIndex = recipe.slotCount() - 1;
        int[] last = layout[lastIndex];
        int outputY = LogicNodeRenderer.outputPortY(
            last[1], recipe.solution[lastIndex], 0);
        drawCable(g, LogicNodeRenderer.outputPortX(last[0]), outputY,
            OUTPUT_X, outputY, OUTPUT_X - 82);

        drawTerminal(g, INPUT_X, INPUT_A_Y, false);
        drawTerminal(g, INPUT_X, INPUT_B_Y, false);
        drawTerminal(g, OUTPUT_X, outputY, true);
        drawLabel(g, "IN 1", INPUT_X - 76, INPUT_A_Y - 20);
        drawLabel(g, "IN 2", INPUT_X - 76, INPUT_B_Y - 20);
        drawLabel(g, "OUT", OUTPUT_X + 28, outputY - 20);
        g.dispose();
    }

    private void drawSource(Graphics2D g, CircuitRecipe recipe, int[][] layout,
                            int source, int targetNode, int targetPort) {
        int startX;
        int startY;
        if (source == CircuitRecipe.INPUT_A) {
            startX = INPUT_X;
            startY = INPUT_A_Y;
        } else if (source == CircuitRecipe.INPUT_B) {
            startX = INPUT_X;
            startY = INPUT_B_Y;
        } else {
            int[] sourceNode = layout[source];
            startX = LogicNodeRenderer.outputPortX(sourceNode[0]);
            startY = LogicNodeRenderer.outputPortY(
                sourceNode[1], recipe.solution[source], 0);
        }

        int[] target = layout[targetNode];
        int endX = LogicNodeRenderer.inputPortX(target[0]);
        int endY = LogicNodeRenderer.inputPortY(
            target[1], recipe.solution[targetNode], targetPort);
        int bendX = startX + Math.max(36, (endX - startX) / 2);
        drawCable(g, startX, startY, endX, endY, bendX);
    }

    private static void drawCable(Graphics2D g, int startX, int startY,
                                  int endX, int endY, int bendX) {
        Path2D path = orthogonalPath(startX, startY, endX, endY, bendX);

        g.translate(4, 6);
        g.setColor(SHADOW);
        g.setStroke(stroke(11));
        g.draw(path);
        g.translate(-4, -6);

        g.setColor(CABLE_EDGE);
        g.setStroke(stroke(9));
        g.draw(path);
        g.setColor(CABLE_FACE);
        g.setStroke(stroke(5));
        g.draw(path);
        g.setColor(CABLE_HIGHLIGHT);
        g.setStroke(stroke(1));
        g.translate(0, -1);
        g.draw(path);
        g.translate(0, 1);
    }

    private static Path2D orthogonalPath(int startX, int startY,
                                         int endX, int endY, int bendX) {
        Path2D path = new Path2D.Double();
        path.moveTo(startX, startY);
        path.lineTo(bendX, startY);
        path.lineTo(bendX, endY);
        path.lineTo(endX, endY);
        return path;
    }

    private static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private void drawTerminal(Graphics2D g, int centerX, int centerY,
                              boolean flipHorizontally) {
        if (endpointImage == null) return;
        Graphics2D endpointGraphics = (Graphics2D) g.create();
        endpointGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        endpointGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int width = endpointImage.getWidth();
        int left = centerX - width / 2;
        int top = centerY - ENDPOINT_RENDER_HEIGHT / 2;
        if (flipHorizontally) {
            endpointGraphics.drawImage(endpointImage,
                left + width, top, -width, ENDPOINT_RENDER_HEIGHT, null);
        } else {
            endpointGraphics.drawImage(endpointImage,
                left, top, width, ENDPOINT_RENDER_HEIGHT, null);
        }
        endpointGraphics.dispose();
    }

    private static void drawLabel(Graphics2D g, String text, int x, int y) {
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(Font.PLAIN, 27f));
        g.setColor(new Color(42, 35, 25, 105));
        g.drawString(text, x + 1, y + 1);
        g.setColor(new Color(55, 48, 35));
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }
}

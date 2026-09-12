package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;

/** Draws the textured physical modules placed on the workbench canvas. */
final class LogicNodeRenderer {
    static final int BODY_WIDTH = 112;

    private static final int MIN_BODY_HEIGHT = 76;
    private static final int PORT_ROW_HEIGHT = 30;
    private static final int BODY_VERTICAL_PADDING = 30;
    private static final int SHADOW_CORNER_RADIUS = 8;
    private static final int PORT_OVERLAP = 4;
    private static final int SHADOW_PADDING = 20;
    private static final int SHADOW_OFFSET_X = 4;
    private static final int SHADOW_OFFSET_Y = 6;
    private static final int SHADOW_BLUR_RADIUS = 10;

    private final BufferedImage bodyTexture;
    private final BufferedImage portImage;
    private final Map<Integer, BufferedImage> shadowByHeight = new HashMap<>();

    LogicNodeRenderer(BufferedImage bodyTexture, BufferedImage portImage) {
        this.bodyTexture = bodyTexture;
        this.portImage = portImage;
    }

    void draw(Graphics2D graphics, int x, int centerY, GateType gate) {
        draw(graphics, x, centerY, gate.label, gate.inputPorts, gate.outputPorts);
    }

    void draw(Graphics2D graphics, int x, int centerY, String label,
              int inputPorts, int outputPorts) {
        int height = bodyHeight(inputPorts, outputPorts);
        int y = centerY - height / 2;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        drawShadow(g, x, y, height);
        drawPorts(g, x, y, height, inputPorts, false);
        drawPorts(g, x + BODY_WIDTH, y, height, outputPorts, true);
        drawBody(g, x, y, height);
        drawLabel(g, x, y, height, label);
        g.dispose();
    }

    static int bodyHeight(int inputPorts, int outputPorts) {
        if (inputPorts < 0 || outputPorts < 0) {
            throw new IllegalArgumentException("Port counts cannot be negative");
        }
        int largestSide = Math.max(inputPorts, outputPorts);
        return Math.max(MIN_BODY_HEIGHT, BODY_VERTICAL_PADDING + largestSide * PORT_ROW_HEIGHT);
    }

    static int inputPortX(int bodyX) {
        return bodyX;
    }

    static int outputPortX(int bodyX) {
        return bodyX + BODY_WIDTH;
    }

    static int inputPortY(int centerY, GateType gate, int portIndex) {
        return inputPortY(centerY, gate.inputPorts, gate.outputPorts, portIndex);
    }

    static int outputPortY(int centerY, GateType gate, int portIndex) {
        return outputPortY(centerY, gate.inputPorts, gate.outputPorts, portIndex);
    }

    static int inputPortY(int centerY, int inputPorts, int outputPorts,
                          int portIndex) {
        return portY(centerY, inputPorts, portIndex,
            bodyHeight(inputPorts, outputPorts));
    }

    static int outputPortY(int centerY, int inputPorts, int outputPorts,
                           int portIndex) {
        return portY(centerY, outputPorts, portIndex,
            bodyHeight(inputPorts, outputPorts));
    }

    private static int portY(int centerY, int count, int index, int height) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Port index is outside the node");
        }
        int top = centerY - height / 2;
        return top + (int) Math.round((index + 1) * height / (double) (count + 1));
    }

    private void drawShadow(Graphics2D g, int x, int y, int height) {
        BufferedImage shadow = shadowByHeight.computeIfAbsent(height,
            LogicNodeRenderer::createSoftShadow);
        g.drawImage(shadow, x - SHADOW_PADDING + SHADOW_OFFSET_X,
            y - SHADOW_PADDING + SHADOW_OFFSET_Y, null);
    }

    private void drawPorts(Graphics2D g, int edgeX, int y, int height,
                           int count, boolean rightSide) {
        if (portImage == null || count == 0) return;

        Graphics2D portGraphics = (Graphics2D) g.create();
        portGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        portGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int portWidth = portImage.getWidth();
        int portHeight = portImage.getHeight();
        for (int index = 0; index < count; index++) {
            int portCenterY = y + (int) Math.round((index + 1) * height / (double) (count + 1));
            int portY = portCenterY - portHeight / 2;
            if (rightSide) {
                portGraphics.drawImage(portImage, edgeX - PORT_OVERLAP, portY, null);
                continue;
            }

            int portX = edgeX - portWidth + PORT_OVERLAP;
            AffineTransform transform = new AffineTransform();
            transform.translate(portX + portWidth, portY);
            transform.scale(-1, 1);
            portGraphics.drawImage(portImage, transform, null);
        }
        portGraphics.dispose();
    }

    private void drawBody(Graphics2D g, int x, int y, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        Polygon outer = pixelBodyShape(x, y, BODY_WIDTH, height, 2);
        g.setColor(new Color(6, 6, 5));
        g.fillPolygon(outer);

        int inset = 3;
        Polygon inner = pixelBodyShape(x + inset, y + inset,
            BODY_WIDTH - inset * 2, height - inset * 2, 1);
        Shape oldClip = g.getClip();
        g.clip(inner);
        if (bodyTexture != null) {
            Object oldInterpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(bodyTexture, x + inset, y + inset,
                BODY_WIDTH - inset * 2, height - inset * 2, null);
            if (oldInterpolation != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
            }
        } else {
            g.setColor(new Color(35, 34, 29));
            g.fill(inner);
        }
        g.setColor(new Color(0, 0, 0, 34));
        g.fill(inner);
        g.setClip(oldClip);

        g.setColor(new Color(225, 216, 181, 72));
        g.drawLine(x + 6, y + 3, x + BODY_WIDTH - 7, y + 3);
        g.drawLine(x + 3, y + 6, x + 3, y + height - 7);

        g.setColor(new Color(0, 0, 0, 92));
        g.drawLine(x + BODY_WIDTH - 4, y + 6,
            x + BODY_WIDTH - 4, y + height - 7);
        g.drawLine(x + 6, y + height - 4,
            x + BODY_WIDTH - 7, y + height - 4);
        g.setColor(new Color(0, 0, 0, 210));
        g.drawPolygon(outer);
    }

    private static void drawLabel(Graphics2D g, int x, int y, int height, String label) {
        Font oldFont = g.getFont();
        float fontSize = 34f;
        g.setFont(oldFont.deriveFont(Font.PLAIN, fontSize));
        while (fontSize > 14f && g.getFontMetrics().stringWidth(label) > BODY_WIDTH - 12) {
            fontSize -= 1f;
            g.setFont(oldFont.deriveFont(Font.PLAIN, fontSize));
        }
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (BODY_WIDTH - metrics.stringWidth(label)) / 2;
        int baseline = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(new Color(0, 0, 0, 95));
        g.drawString(label, textX + 1, baseline + 1);
        g.setColor(new Color(238, 236, 216));
        g.drawString(label, textX, baseline);
        g.setFont(oldFont);
    }

    private static BufferedImage createSoftShadow(int height) {
        int width = BODY_WIDTH + SHADOW_PADDING * 2;
        int imageHeight = height + SHADOW_PADDING * 2;
        BufferedImage mask = new BufferedImage(width, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D shadowGraphics = mask.createGraphics();
        shadowGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        shadowGraphics.setColor(new Color(0, 0, 0, 105));
        shadowGraphics.fillRoundRect(SHADOW_PADDING, SHADOW_PADDING,
            BODY_WIDTH, height, SHADOW_CORNER_RADIUS, SHADOW_CORNER_RADIUS);
        shadowGraphics.dispose();

        int kernelSize = SHADOW_BLUR_RADIUS * 2 + 1;
        float[] weights = gaussianKernel(kernelSize, SHADOW_BLUR_RADIUS / 2.0);
        BufferedImage blurred = new BufferedImage(width, imageHeight, BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(new Kernel(kernelSize, kernelSize, weights),
            ConvolveOp.EDGE_NO_OP, null).filter(mask, blurred);
        return blurred;
    }

    private static float[] gaussianKernel(int size, double sigma) {
        float[] weights = new float[size * size];
        int radius = size / 2;
        double sum = 0.0;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double weight = Math.exp(-(x * x + y * y) / (2.0 * sigma * sigma));
                weights[(y + radius) * size + x + radius] = (float) weight;
                sum += weight;
            }
        }
        for (int index = 0; index < weights.length; index++) {
            weights[index] /= (float) sum;
        }
        return weights;
    }

    private static Polygon pixelBodyShape(int x, int y, int width, int height, int step) {
        int corner = step * 3;
        int[] xs = {
            x + corner, x + width - corner,
            x + width - corner, x + width - step,
            x + width - step, x + width,
            x + width, x + width - step,
            x + width - step, x + width - corner,
            x + width - corner, x + corner,
            x + corner, x + step,
            x + step, x,
            x, x + step,
            x + step, x + corner
        };
        int[] ys = {
            y, y,
            y + step, y + step,
            y + corner, y + corner,
            y + height - corner, y + height - corner,
            y + height - step, y + height - step,
            y + height, y + height,
            y + height - step, y + height - step,
            y + height - corner, y + height - corner,
            y + corner, y + corner,
            y + step, y + step
        };
        return new Polygon(xs, ys, xs.length);
    }
}

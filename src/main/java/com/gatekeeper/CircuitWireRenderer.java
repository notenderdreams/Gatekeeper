package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Draws the physical cable layer beneath workbench logic nodes. */
final class CircuitWireRenderer {
    private static final int ENDPOINT_RENDER_HEIGHT = 36;

    private static final Color SHADOW = new Color(50, 34, 20, 88);
    private static final Color CABLE_EDGE = new Color(18, 20, 18);
    private static final Color CABLE_FACE = new Color(46, 49, 43);
    private static final Color CABLE_HIGHLIGHT = new Color(150, 145, 116, 125);
    private static final Color POWERED_EDGE = new Color(26, 52, 42);
    private static final Color POWERED_FACE = new Color(72, 138, 101);
    private static final Color POWERED_HIGHLIGHT = new Color(190, 232, 166, 190);

    private final BufferedImage endpointImage;

    CircuitWireRenderer(BufferedImage endpointImage) {
        this.endpointImage = endpointImage;
    }

    void draw(Graphics2D graphics, WorkbenchGraph graph, boolean[] externalInputs,
              int pointerX, int pointerY) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);

        for (WorkbenchGraph.Wire wire : graph.wires()) {
            int[] start = graph.sourcePoint(wire.sourceId());
            int[] end = graph.targetPoint(wire.targetId(), wire.targetPort());
            if (start != null && end != null) {
                drawCable(g, start[0], start[1], end[0], end[1], wire.corners(),
                    graph.sourceValue(wire.sourceId(), externalInputs));
            }
        }

        if (graph.pendingSourceId() != null) {
            int[] start = graph.sourcePoint(graph.pendingSourceId());
            if (start != null) drawPreviewCable(g, start[0], start[1],
                pointerX, pointerY, graph.pendingCorners());
        }

        drawTerminal(g, WorkbenchGraph.INPUT_X, WorkbenchGraph.INPUT_0_Y, false);
        drawTerminal(g, WorkbenchGraph.INPUT_X, WorkbenchGraph.INPUT_1_Y, false);
        drawTerminal(g, WorkbenchGraph.OUTPUT_X, WorkbenchGraph.OUTPUT_Y, true);
        drawLabel(g, "IN 0", WorkbenchGraph.INPUT_X - 76, WorkbenchGraph.INPUT_0_Y - 20);
        drawLabel(g, "IN 1", WorkbenchGraph.INPUT_X - 76, WorkbenchGraph.INPUT_1_Y - 20);
        drawLabel(g, "OUT 0", WorkbenchGraph.OUTPUT_X + 10, WorkbenchGraph.OUTPUT_Y - 24);
        g.dispose();
    }

    private static void drawCable(Graphics2D g, int startX, int startY,
                                  int endX, int endY) {
        int bendX = startX + Math.max(36, (endX - startX) / 2);
        drawCable(g, startX, startY, endX, endY, bendX);
    }

    private static void drawCable(Graphics2D g, int startX, int startY,
                                  int endX, int endY,
                                  List<WorkbenchGraph.RoutePoint> corners,
                                  boolean powered) {
        if (corners.isEmpty()) {
            int bendX = startX + Math.max(36, (endX - startX) / 2);
            drawCablePath(g,
                orthogonalPath(startX, startY, endX, endY, bendX), powered);
            return;
        }
        Path2D path = routedPath(startX, startY, endX, endY, corners);
        drawCablePath(g, path, powered);
    }

    private static void drawPreviewCable(Graphics2D g, int startX, int startY,
                                         int endX, int endY,
                                         List<WorkbenchGraph.RoutePoint> corners) {
        Path2D path = routedPath(startX, startY, endX, endY, corners);
        g.setColor(new Color(95, 69, 35, 165));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 1f, new float[]{12f, 9f}, 0f));
        g.draw(path);
    }

    private static void drawCable(Graphics2D g, int startX, int startY,
                                  int endX, int endY, int bendX) {
        Path2D path = orthogonalPath(startX, startY, endX, endY, bendX);

        drawCablePath(g, path, false);
    }

    private static void drawCablePath(Graphics2D g, Path2D path, boolean powered) {

        g.translate(4, 6);
        g.setColor(SHADOW);
        g.setStroke(stroke(11));
        g.draw(path);
        g.translate(-4, -6);

        g.setColor(powered ? POWERED_EDGE : CABLE_EDGE);
        g.setStroke(stroke(9));
        g.draw(path);
        g.setColor(powered ? POWERED_FACE : CABLE_FACE);
        g.setStroke(stroke(5));
        g.draw(path);
        g.setColor(powered ? POWERED_HIGHLIGHT : CABLE_HIGHLIGHT);
        g.setStroke(stroke(1));
        g.translate(0, -1);
        g.draw(path);
        g.translate(0, 1);
    }

    private static Path2D routedPath(int startX, int startY, int endX, int endY,
                                     List<WorkbenchGraph.RoutePoint> corners) {
        Path2D path = new Path2D.Double();
        path.moveTo(startX, startY);
        int previousX = startX;
        int previousY = startY;
        for (WorkbenchGraph.RoutePoint corner : corners) {
            if (previousX != corner.x() && previousY != corner.y()) {
                path.lineTo(corner.x(), previousY);
            }
            path.lineTo(corner.x(), corner.y());
            previousX = corner.x();
            previousY = corner.y();
        }
        if (previousX != endX && previousY != endY) {
            path.lineTo(endX, previousY);
        }
        path.lineTo(endX, endY);
        return path;
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

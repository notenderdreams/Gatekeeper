package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Modeless circular picker for unlocked workbench node types. */
final class NodeRadialMenu {
    // The 1536x1024 source canvas is displayed at 480x270. These Y radii
    // compensate for that non-uniform scale so the wheel is circular on screen.
    static final int OUTER_RADIUS_X = 190;
    static final int OUTER_RADIUS_Y = 225;
    static final int INNER_RADIUS_X = 64;
    static final int INNER_RADIUS_Y = 76;

    private static final int NODE_RADIUS_X = 126;
    private static final int NODE_RADIUS_Y = 149;
    private static final double NODE_SCALE = 0.76;
    private static final Color SCRIM = new Color(22, 18, 12, 92);
    private static final Color PANEL = new Color(25, 29, 25, 248);
    private static final Color PANEL_INNER = new Color(16, 19, 17, 252);
    private static final Color PANEL_HOVER = new Color(130, 88, 35, 205);
    private static final Color PANEL_ACTIVE = new Color(71, 58, 34, 180);
    private static final Color EDGE = new Color(91, 77, 51);
    private static final Color BRASS = new Color(225, 187, 105);

    private final List<GateType> unlocked = new ArrayList<>();
    private int centerX;
    private int centerY;
    private boolean open;

    boolean isOpen() { return open; }
    int centerX() { return centerX; }
    int centerY() { return centerY; }
    List<GateType> unlocked() { return List.copyOf(unlocked); }

    void openAt(int requestedX, int requestedY, List<GateType> available) {
        unlocked.clear();
        for (GateType gate : available) {
            if (!unlocked.contains(gate)) unlocked.add(gate);
        }
        if (unlocked.isEmpty()) return;
        centerX = clamp(requestedX,
            WorkbenchGraph.WORK_X + OUTER_RADIUS_X,
            WorkbenchGraph.WORK_RIGHT - OUTER_RADIUS_X);
        centerY = clamp(requestedY,
            WorkbenchGraph.WORK_Y + OUTER_RADIUS_Y,
            WorkbenchGraph.WORK_BOTTOM - OUTER_RADIUS_Y);
        open = true;
    }

    boolean close() {
        if (!open) return false;
        open = false;
        return true;
    }

    GateType gateAt(int x, int y) {
        if (!open || unlocked.isEmpty()) return null;
        int dx = x - centerX;
        int dy = y - centerY;
        double outerDistance = square(dx / (double) OUTER_RADIUS_X)
            + square(dy / (double) OUTER_RADIUS_Y);
        double innerDistance = square(dx / (double) INNER_RADIUS_X)
            + square(dy / (double) INNER_RADIUS_Y);
        if (innerDistance < 1.0 || outerDistance > 1.0) return null;
        return unlocked.get(wedgeIndex(x, y));
    }

    void draw(Graphics2D graphics, LogicNodeRenderer nodeRenderer,
              BufferedImage panelTexture,
              GateType activeGate,
              int pointerX, int pointerY) {
        if (!open || unlocked.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        // Keep every popup layer, including its shadow, inside the annotated paper canvas.
        g.clipRect(WorkbenchGraph.WORK_X, WorkbenchGraph.WORK_Y,
            WorkbenchGraph.WORK_WIDTH, WorkbenchGraph.WORK_HEIGHT);
        g.setColor(SCRIM);
        g.fillRect(WorkbenchGraph.WORK_X, WorkbenchGraph.WORK_Y,
            WorkbenchGraph.WORK_WIDTH, WorkbenchGraph.WORK_HEIGHT);

        GateType hovered = gateAt(pointerX, pointerY);
        double wedgeDegrees = 360.0 / unlocked.size();

        g.setColor(new Color(24, 16, 8, 105));
        g.fillOval(centerX - OUTER_RADIUS_X + 9, centerY - OUTER_RADIUS_Y + 12,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        g.setColor(PANEL);
        g.fillOval(centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        drawPanelTexture(g, panelTexture);

        for (int index = 0; index < unlocked.size(); index++) {
            GateType gate = unlocked.get(index);
            double centerDegrees = 90.0 - index * wedgeDegrees;
            if (gate == hovered || gate == activeGate) {
                Arc2D wedge = new Arc2D.Double(
                    centerX - OUTER_RADIUS_X + 8, centerY - OUTER_RADIUS_Y + 9,
                    (OUTER_RADIUS_X - 8) * 2.0,
                    (OUTER_RADIUS_Y - 9) * 2.0,
                    centerDegrees - wedgeDegrees / 2.0, wedgeDegrees,
                    Arc2D.PIE);
                g.setColor(gate == hovered ? PANEL_HOVER : PANEL_ACTIVE);
                g.fill(wedge);
            }

            double radians = Math.toRadians(centerDegrees);
            int nodeCenterX = centerX
                + (int) Math.round(Math.cos(radians) * NODE_RADIUS_X);
            int nodeCenterY = centerY
                - (int) Math.round(Math.sin(radians) * NODE_RADIUS_Y);
            Graphics2D nodeGraphics = (Graphics2D) g.create();
            nodeGraphics.translate(nodeCenterX, nodeCenterY);
            nodeGraphics.scale(NODE_SCALE, NODE_SCALE);
            nodeRenderer.draw(nodeGraphics,
                -LogicNodeRenderer.BODY_WIDTH / 2, 0, gate);
            nodeGraphics.dispose();
            if (gate == activeGate) {
                g.setColor(BRASS);
                int scaledHeight = (int) Math.round(
                    LogicNodeRenderer.bodyHeight(
                        gate.inputPorts, gate.outputPorts) * NODE_SCALE);
                g.fillRect(nodeCenterX - 12,
                    nodeCenterY - scaledHeight / 2 - 8, 24, 3);
            }

            double separatorDegrees = centerDegrees + wedgeDegrees / 2.0;
            double separatorRadians = Math.toRadians(separatorDegrees);
            g.setColor(new Color(116, 92, 54, 135));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(
                centerX + (int) Math.round(Math.cos(separatorRadians) * (INNER_RADIUS_X + 8)),
                centerY - (int) Math.round(Math.sin(separatorRadians) * (INNER_RADIUS_Y + 8)),
                centerX + (int) Math.round(Math.cos(separatorRadians) * (OUTER_RADIUS_X - 10)),
                centerY - (int) Math.round(Math.sin(separatorRadians) * (OUTER_RADIUS_Y - 12)));
        }

        g.setColor(new Color(6, 8, 7, 230));
        g.setStroke(new BasicStroke(9f));
        g.drawOval(centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2);
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - OUTER_RADIUS_X + 5, centerY - OUTER_RADIUS_Y + 6,
            (OUTER_RADIUS_X - 5) * 2, (OUTER_RADIUS_Y - 6) * 2);

        g.setColor(PANEL_INNER);
        g.fillOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        g.setColor(new Color(7, 9, 8, 210));
        g.setStroke(new BasicStroke(8f));
        g.drawOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - INNER_RADIUS_X, centerY - INNER_RADIUS_Y,
            INNER_RADIUS_X * 2, INNER_RADIUS_Y * 2);
        drawCentered(g, activeGate.label, centerX, centerY - 3, 31f, BRASS);
        drawCentered(g, "ACTIVE", centerX, centerY + 27, 16f,
            new Color(132, 112, 75));

        g.dispose();
    }

    private void drawPanelTexture(Graphics2D graphics, BufferedImage texture) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ellipse2D housing = new Ellipse2D.Double(
            centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
            OUTER_RADIUS_X * 2.0, OUTER_RADIUS_Y * 2.0);
        g.clip(housing);
        if (texture != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(texture,
                centerX - OUTER_RADIUS_X, centerY - OUTER_RADIUS_Y,
                OUTER_RADIUS_X * 2, OUTER_RADIUS_Y * 2, null);
        }
        g.setColor(new Color(12, 25, 19, 118));
        g.fill(housing);

        // Shallow machined wear rings keep the texture readable without adding clutter.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2f));
        for (int inset = 24; inset <= 52; inset += 14) {
            int insetY = (int) Math.round(inset
                * OUTER_RADIUS_Y / (double) OUTER_RADIUS_X);
            g.setColor(new Color(229, 190, 111, 25));
            g.drawOval(centerX - OUTER_RADIUS_X + inset,
                centerY - OUTER_RADIUS_Y + insetY,
                (OUTER_RADIUS_X - inset) * 2,
                (OUTER_RADIUS_Y - insetY) * 2);
            g.setColor(new Color(0, 0, 0, 45));
            g.drawOval(centerX - OUTER_RADIUS_X + inset + 2,
                centerY - OUTER_RADIUS_Y + insetY + 2,
                (OUTER_RADIUS_X - inset - 2) * 2,
                (OUTER_RADIUS_Y - insetY - 2) * 2);
        }
        g.dispose();
    }

    private int wedgeIndex(int x, int y) {
        double pointerDegrees = Math.toDegrees(
            Math.atan2((centerY - y) / (double) OUTER_RADIUS_Y,
                (x - centerX) / (double) OUTER_RADIUS_X));
        pointerDegrees = normalize(pointerDegrees);
        double wedgeDegrees = 360.0 / unlocked.size();
        return (int) Math.floor(normalize(
            90.0 + wedgeDegrees / 2.0 - pointerDegrees) / wedgeDegrees)
            % unlocked.size();
    }

    private static void drawCentered(Graphics2D g, String text, int centerX,
                                     int baselineY, float size, Color color) {
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(Font.BOLD, size));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
        g.setFont(oldFont);
    }

    private static double normalize(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double square(double value) {
        return value * value;
    }
}

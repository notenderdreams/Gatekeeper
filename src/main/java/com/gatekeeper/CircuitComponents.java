package com.gatekeeper;

import java.awt.Graphics2D;

import static com.gatekeeper.GameConstants.CYAN;

final class CircuitComponents {
    private CircuitComponents() {}

    static void drawGate(Graphics2D g, int x, int y, GateType gate, boolean powered) {
        g.setColor(powered ? CYAN : g.getColor());
        if (gate == GateType.NOT) {
            int[] xs = {x + 5, x + 5, x + 29};
            int[] ys = {y + 4, y + 24, y + 14};
            g.drawPolygon(xs, ys, 3);
            g.drawOval(x + 29, y + 11, 5, 5);
            return;
        }

        g.drawRect(x + 3, y + 4, 29, 21);
        if (gate == GateType.OR) {
            g.drawLine(x + 3, y + 4, x + 10, y + 14);
        }
        GamePanel.pixelText(g, gate == GateType.AND ? "&" : ">", x + 14, y + 19, 1);
    }
}

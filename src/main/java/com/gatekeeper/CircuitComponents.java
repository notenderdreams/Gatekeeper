package com.gatekeeper;

import java.awt.Graphics2D;

import static com.gatekeeper.GameConstants.CYAN;

final class CircuitComponents {
    private CircuitComponents() {}

    static void drawGate(Graphics2D g, int x, int y, GateType gate, boolean powered) {
        g.setColor(powered ? CYAN : g.getColor());
        if (gate == GateType.NOT) {
            int[] xs = {x + 3, x + 3, x + 15};
            int[] ys = {y + 2, y + 12, y + 7};
            g.drawPolygon(xs, ys, 3);
            g.drawOval(x + 15, y + 5, 3, 3);
            return;
        }

        g.drawRect(x + 2, y + 2, 16, 10);
        if (gate == GateType.OR) {
            g.drawLine(x + 2, y + 2, x + 6, y + 7);
        }
        GamePanel.pixelText(g, gate == GateType.AND ? "&" : ">", x + 8, y + 10, 1);
    }
}

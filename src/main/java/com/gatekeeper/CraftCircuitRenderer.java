package com.gatekeeper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/** Modal used to name a graph before committing its physical parts. */
final class CraftCircuitRenderer {
    void draw(Graphics2D g, String draft, List<String> existingNames, String status) {
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(25, 25, 22));
        g.fillRect(82, 57, 316, 157);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(82, 57, 316, 157);
        g.drawRect(86, 61, 308, 149);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "CRAFT CIRCUIT", W / 2, 82, 2);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g, "CIRCUIT NAME (MAX 6)", W / 2, 104, 1);
        g.setColor(new Color(7, 9, 9));
        g.fillRect(112, 114, 256, 27);
        g.setColor(CYAN);
        g.drawRect(112, 114, 256, 27);
        g.setColor(INK);
        GamePanel.drawCenteredPixelText(g, draft + "_", W / 2, 132, 1);
        g.setColor(DIM);
        String names = existingNames.isEmpty() ? "NO EXISTING NAMES"
            : "EXISTING: " + existingNames.stream().limit(4)
                .collect(java.util.stream.Collectors.joining("  "));
        GamePanel.drawCenteredPixelText(g, names, W / 2, 158, 1);
        g.setColor(status.isBlank() ? DIM : RED);
        GamePanel.drawCenteredPixelText(g, status, W / 2, 179, 1);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g,
            "TYPE NAME   UP/DOWN: REUSE NAME   ENTER: CRAFT   ESC: CANCEL",
            W / 2, 199, 1);
    }
}

package com.gatekeeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static com.gatekeeper.GameConstants.*;

/** Draws order specifications beside the player-built circuit being submitted. */
final class ContractRenderer {
    private final CircuitPackageRenderer circuitRenderer;

    ContractRenderer(BufferedImage texture, BufferedImage portImage, Font font) {
        circuitRenderer = new CircuitPackageRenderer(texture, portImage, font);
    }

    void draw(Graphics2D g, List<ContractModel.Contract> contracts, int selectedOrder,
              ContractModel model, CraftedCircuitInventory inventory, String status) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(34, 26, 22));
        g.fillRect(43, 25, 394, 222);
        g.setColor(new Color(181, 126, 68));
        g.drawRect(43, 25, 394, 222);
        g.drawRect(47, 29, 386, 214);
        g.setColor(YELLOW);
        GamePanel.drawCenteredPixelText(g, "MIRA'S ORDERS", W / 2, 49, 2);

        g.setColor(DIM);
        GamePanel.pixelText(g, "REQUESTED BEHAVIOR", 65, 69, 1);
        GamePanel.pixelText(g, "YOUR CRAFTED CIRCUIT", 257, 69, 1);
        g.setColor(new Color(96, 67, 38));
        g.drawLine(238, 60, 238, 198);

        for (int i = 0; i < contracts.size(); i++) {
            ContractModel.Contract contract = contracts.get(i);
            int y = 91 + i * 24;
            if (i == selectedOrder) {
                g.setColor(new Color(84, 62, 40));
                g.fillRect(61, y - 14, 164, 20);
                g.setColor(YELLOW);
                g.drawRect(61, y - 14, 164, 20);
            }
            g.setColor(i == selectedOrder ? INK : DIM);
            GamePanel.pixelText(g, contract.product(), 70, y, 1);
            GamePanel.pixelText(g, "+" + contract.reward() + "C", 137, y, 1);
            GamePanel.pixelText(g, "DONE " + model.deliveries(contract), 174, y, 1);
        }

        CraftedCircuitInventory.CraftedCircuit selectedCircuit = inventory.selected();
        if (selectedCircuit == null) {
            g.setColor(DIM);
            GamePanel.drawCenteredPixelText(g, "NO CRAFTED CIRCUITS", 335, 129, 1);
        } else {
            Rectangle preview = new Rectangle(276, 82, 118, 76);
            circuitRenderer.draw(g, selectedCircuit, preview);
            g.setColor(INK);
            GamePanel.drawCenteredPixelText(g, selectedCircuit.name(), 335, 174, 1);
            int current = inventory.selectedIndex() + 1;
            GamePanel.drawCenteredPixelText(g,
                current + " / " + inventory.circuits().size(), 335, 190, 1);
        }

        g.setColor(status.startsWith("FAILED") ? RED : CYAN);
        GamePanel.drawCenteredPixelText(g, status, W / 2, 211, 1);
        g.setColor(DIM);
        GamePanel.drawCenteredPixelText(g,
            "UP/DOWN ORDER  LEFT/RIGHT CIRCUIT  ENTER SUBMIT  C/ESC CLOSE",
            W / 2, 232, 1);
    }
}

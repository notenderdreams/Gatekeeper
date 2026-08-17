package com.gatekeeper;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Draws a player-built circuit as a textured component with calculated I/O pins. */
final class CircuitPackageRenderer {
    private final LogicNodeRenderer nodeRenderer;
    private final Font font;

    CircuitPackageRenderer(BufferedImage texture, BufferedImage portImage, Font font) {
        nodeRenderer = new LogicNodeRenderer(texture, portImage);
        this.font = font;
    }

    void draw(Graphics2D graphics, CraftedCircuitInventory.CraftedCircuit circuit,
              Rectangle area) {
        int inputs = inputCount(circuit.graph());
        int outputs = outputCount(circuit.graph());
        int bodyHeight = LogicNodeRenderer.bodyHeight(inputs, outputs);
        double sourceWidth = LogicNodeRenderer.BODY_WIDTH + 28.0;
        double sourceHeight = bodyHeight + 28.0;
        double scale = Math.min(area.width / sourceWidth, area.height / sourceHeight);
        Graphics2D packageGraphics = (Graphics2D) graphics.create();
        packageGraphics.clip(area);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;
        packageGraphics.translate(area.x + (area.width - drawWidth) / 2.0,
            area.y + (area.height - drawHeight) / 2.0);
        packageGraphics.scale(scale, scale);
        packageGraphics.setFont(font);
        nodeRenderer.draw(packageGraphics, 14, 14 + bodyHeight / 2,
            "CIRCUIT", inputs, outputs);
        packageGraphics.dispose();
    }

    static int inputCount(WorkbenchGraph.Snapshot graph) {
        boolean first = false;
        boolean second = false;
        for (WorkbenchGraph.WireData wire : graph.wires()) {
            if (wire.sourceId() == WorkbenchGraph.INPUT_0) first = true;
            if (wire.sourceId() == WorkbenchGraph.INPUT_1) second = true;
        }
        for (WorkbenchGraph.JunctionData junction : graph.junctions()) {
            if (junction.upstreamSourceId() == WorkbenchGraph.INPUT_0) first = true;
            if (junction.upstreamSourceId() == WorkbenchGraph.INPUT_1) second = true;
        }
        return (first ? 1 : 0) + (second ? 1 : 0);
    }

    static int outputCount(WorkbenchGraph.Snapshot graph) {
        return (int) graph.wires().stream()
            .filter(wire -> wire.targetId() == WorkbenchGraph.OUTPUT)
            .map(WorkbenchGraph.WireData::targetPort).distinct().count();
    }
}

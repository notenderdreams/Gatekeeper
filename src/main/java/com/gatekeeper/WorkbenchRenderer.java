package com.gatekeeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static com.gatekeeper.GameConstants.H;
import static com.gatekeeper.GameConstants.W;

/** Draws the asset-backed workbench shell and editable circuit graph. */
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

    private static final int TOOL_X = 480;
    private static final int TOOL_Y = 70;
    private static final int TOOL_WIDTH = 150;
    private static final int TOOL_HEIGHT = 44;
    private static final int TOOL_GAP = 14;

    private final BufferedImage canvasImage;
    private final BufferedImage lightOffImage;
    private final BufferedImage switchImage;
    private final BufferedImage lightOnImage;
    private final LogicNodeRenderer nodeRenderer;
    private final CircuitWireRenderer wireRenderer;
    private final boolean[] switches = new boolean[CONTROL_COUNT];

    WorkbenchRenderer(BufferedImage canvasImage, BufferedImage lightOffImage,
                      BufferedImage switchImage, BufferedImage lightOnImage,
                      BufferedImage nodeTextureImage, BufferedImage wireEndImage,
                      BufferedImage endpointImage) {
        this.canvasImage = canvasImage;
        this.lightOffImage = lightOffImage;
        this.switchImage = switchImage;
        this.lightOnImage = lightOnImage;
        nodeRenderer = new LogicNodeRenderer(nodeTextureImage, wireEndImage);
        wireRenderer = new CircuitWireRenderer(endpointImage);
    }

    void draw(Graphics2D graphics, WorkbenchGraph graph, GateType selectedGate,
              int logicalMouseX, int logicalMouseY) {
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

        int pointerX = canvasX(logicalMouseX);
        int pointerY = canvasY(logicalMouseY);
        wireRenderer.draw(g, graph, pointerX, pointerY);
        drawNodePreview(g, graph);
        drawEditorToolbar(g, selectedGate, pointerX, pointerY);
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

    static GateType gateToolAt(int logicalX, int logicalY) {
        int sourceX = canvasX(logicalX);
        int sourceY = canvasY(logicalY);
        GateType[] gates = GateType.values();
        for (int index = 0; index < gates.length; index++) {
            int x = TOOL_X + index * (TOOL_WIDTH + TOOL_GAP);
            if (inside(sourceX, sourceY, x, TOOL_Y, TOOL_WIDTH, TOOL_HEIGHT)) {
                return gates[index];
            }
        }
        return null;
    }

    static int canvasX(int logicalX) {
        return (int) Math.round(logicalX * CANVAS_WIDTH / (double) W);
    }

    static int canvasY(int logicalY) {
        return (int) Math.round(logicalY * CANVAS_HEIGHT / (double) H);
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

    private void drawNodePreview(Graphics2D g, WorkbenchGraph graph) {
        for (WorkbenchGraph.Node node : graph.nodes()) {
            nodeRenderer.draw(g, node.x(), node.centerY(), node.gate());
            if (Integer.valueOf(node.id()).equals(graph.selectedNodeId())) {
                int height = LogicNodeRenderer.bodyHeight(
                    node.gate().inputPorts, node.gate().outputPorts);
                int y = node.centerY() - height / 2;
                g.setColor(new Color(107, 68, 25, 205));
                g.setStroke(new BasicStroke(3));
                g.drawRect(node.x() - 7, y - 7,
                    LogicNodeRenderer.BODY_WIDTH + 14, height + 14);
                g.setStroke(new BasicStroke(1));
            }
        }
    }

    private static void drawEditorToolbar(Graphics2D g, GateType selectedGate,
                                          int pointerX, int pointerY) {
        GateType[] gates = GateType.values();
        for (int index = 0; index < gates.length; index++) {
            int x = TOOL_X + index * (TOOL_WIDTH + TOOL_GAP);
            boolean selected = gates[index] == selectedGate;
            boolean hovered = inside(pointerX, pointerY, x, TOOL_Y,
                TOOL_WIDTH, TOOL_HEIGHT);
            g.setColor(selected ? new Color(68, 55, 34, 235)
                : hovered ? new Color(94, 72, 40, 220)
                : new Color(46, 39, 29, 205));
            g.fillRect(x, TOOL_Y, TOOL_WIDTH, TOOL_HEIGHT);
            g.setColor(selected ? new Color(225, 187, 105)
                : new Color(98, 80, 48));
            g.drawRect(x, TOOL_Y, TOOL_WIDTH, TOOL_HEIGHT);
            GamePanel.drawCenteredPixelText(g,
                (index + 1) + "  " + gates[index].label,
                x + TOOL_WIDTH / 2, TOOL_Y + 30, 2);
        }

        g.setColor(new Color(62, 49, 31, 190));
        GamePanel.pixelText(g, "CLICK: ADD / DRAG: MOVE", 1000, 84, 1);
        GamePanel.pixelText(g, "OUTPUT -> INPUT: WIRE", 1000, 101, 1);
        GamePanel.pixelText(g, "CLICK CANVAS: BEND", 1000, 118, 1);
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

    private static boolean inside(int x, int y, int left, int top,
                                  int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }
}

package com.gatekeeper;

public final class LogicNodeRendererTest {
    private LogicNodeRendererTest() {}

    public static void main(String[] args) {
        require(GateType.AND.inputPorts == 2, "AND should expose two input ports");
        require(GateType.AND.outputPorts == 1, "AND should expose one output port");
        require(GateType.NOT.inputPorts == 1, "NOT should expose one input port");

        int onePortTall = LogicNodeRenderer.bodyHeight(1, 1);
        int twoPortsTall = LogicNodeRenderer.bodyHeight(2, 1);
        require(twoPortsTall > onePortTall,
            "node body should grow with the larger port count");
        require(LogicNodeRenderer.bodyHeight(1, 4) == LogicNodeRenderer.bodyHeight(4, 1),
            "input and output counts should affect height symmetrically");

        boolean rejectedNegativeCount = false;
        try {
            LogicNodeRenderer.bodyHeight(-1, 1);
        } catch (IllegalArgumentException expected) {
            rejectedNegativeCount = true;
        }
        require(rejectedNegativeCount, "negative port counts should be rejected");

        System.out.println("LogicNodeRendererTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

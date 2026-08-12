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
        int firstAndPort = LogicNodeRenderer.inputPortY(200, GateType.AND, 0);
        int secondAndPort = LogicNodeRenderer.inputPortY(200, GateType.AND, 1);
        require(firstAndPort < 200 && secondAndPort > 200,
            "two input ports should straddle the node center");
        require(LogicNodeRenderer.outputPortY(200, GateType.AND, 0) == 200,
            "single output port should be centered");

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

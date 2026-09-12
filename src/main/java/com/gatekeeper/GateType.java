package com.gatekeeper;

public enum GateType {
    AND("AND", "Both inputs must be ON", 2, 1),
    OR("OR", "At least one input must be ON", 2, 1),
    NOT("NOT", "Flips its single input", 1, 1);

    public final String label;
    public final String hint;
    public final int inputPorts;
    public final int outputPorts;

    GateType(String label, String hint, int inputPorts, int outputPorts) {
        this.label = label;
        this.hint = hint;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
    }
}

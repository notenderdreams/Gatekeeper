package com.gatekeeper;

public enum GateType {
    AND("AND", "Both inputs must be ON"),
    OR("OR", "At least one input must be ON"),
    NOT("NOT", "Flips its single input");

    public final String label;
    public final String hint;

    GateType(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }
}

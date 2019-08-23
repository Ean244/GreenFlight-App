package com.genean.dronecontroller;

public enum Axis {
    YAW, PITCH, ROLL;

    public char getSymbol() {
        return name().charAt(0);
    }
}

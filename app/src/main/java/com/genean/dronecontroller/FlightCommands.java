package com.genean.dronecontroller;

public enum FlightCommands {
    CLIENT_CONNECT("PING#"),
    HOST_ACK("PONG"),
    CLIENT_ARM("ARM#"),
    HOST_ARM_ACK("ARMED"),
    CLIENT_DISARM("DISARM#"),
    HOST_DISARM_ACK("DISARMED"),
    SEND_CHANNEL_DATA("T%dY%dP%dR%d#");
    private String command;

    FlightCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}

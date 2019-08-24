package com.genean.dronecontroller;

public enum FlightCommand {
    CLIENT_CONNECT_QUERY("PING#"),
    HOST_CONNECT_RESPONSE("PONG"),
    CLIENT_ARM_QUERY("ARM#"),
    HOST_ARM_RESPONSE("ARMED"),
    CLIENT_DISARM_QUERY("DISARM#"),
    HOST_DISARM_RESPONSE("DISARMED"),
    CLIENT_CHANNEL_UPDATE("T%dY%dP%dR%d#"),
    CLIENT_VOLTAGE_QUERY("V#"),
    HOST_VOLTAGE_RESPONSE("V0000"),
    CLIENT_PID_UPDATE("!%cP%7.3fI%7.3fD%7.3f#"),
    HOST_PID_UPDATE_RESPONSE("OK"),
    CLIENT_PID_QUERY("?%c#");
    private String command;

    FlightCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }
}

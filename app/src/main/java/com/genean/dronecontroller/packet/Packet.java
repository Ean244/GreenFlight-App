package com.genean.dronecontroller.packet;

import com.genean.dronecontroller.FlightCommand;

public class Packet {
    private byte[] data;
    private ResponseHandler responseHandler;

    public Packet(FlightCommand command) {
        this(command.getBytes());
    }

    public Packet(byte[] data) {
        this.data = data;
        this.responseHandler = (r) -> { };
    }

    public byte[] getData() {
        return data;
    }

    protected void setData(byte[] data) {
        this.data = data;
    }

    public void setOnCompleted(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    protected ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public enum Response {
        SUCCESS, CORRUPTED_DATA, TIMEOUT, IO_ERROR
    }
}

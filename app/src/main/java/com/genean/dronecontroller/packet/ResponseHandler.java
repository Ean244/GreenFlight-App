package com.genean.dronecontroller.packet;

public interface ResponseHandler {
    void onCompleted(Packet.Response response);
}

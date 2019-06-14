package com.genean.dronecontroller;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class PacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();
    private static final String TAG = "PacketHandler";

    private static final String HOST_IP = "192.168.4.1";
    private static final int HOST_PORT = 333;
    private static final int RESPONSE_TIMEOUT_MILLS = 3000;

    private DatagramSocket socket = null;
    private InetAddress hostAddress = null;

    private PacketHandler() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(RESPONSE_TIMEOUT_MILLS);
        } catch (SocketException e) {
            Log.e(TAG, "Failed to establish socket!", e);
        }

        try {
            hostAddress = InetAddress.getByName(HOST_IP);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to initialize InetAdress for host!", e);
        }
    }

    public boolean sendPacket(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, HOST_PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send packet to host!", e);
            return false;
        }

        return true;
    }

    //WARN: Use asynchronously
    public boolean receivePacket(byte[] buff) {
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        try {
            socket.receive(packet);
        } catch (Exception e) {
            Log.e(TAG, "Failed to receive packet from host - timeout?", e);
            return false;
        }

        return true;
    }
}

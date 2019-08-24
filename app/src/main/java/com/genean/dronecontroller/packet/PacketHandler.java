package com.genean.dronecontroller.packet;

import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

//Note: Drone is the Host

public class PacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();
    private static final String TAG = "PacketHandler";

    private static final String HOST_IP = "192.168.4.1";
    private static final int HOST_PORT = 333;
    private static final int RESPONSE_TIMEOUT_MILLS = 1500;

    private DatagramSocket socket;
    private InetAddress hostAddress;

    private PacketHandler() {
        initSocket();
        initInetAddress();
    }

    private void initSocket() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(RESPONSE_TIMEOUT_MILLS);
        } catch (SocketException e) {
            Log.e(TAG, "Failed to establish socket!", e);
        }
    }

    private void initInetAddress() {
        try {
            hostAddress = InetAddress.getByName(HOST_IP);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to recognise IP Address for host!", e);
        }
    }

    @WorkerThread
    public void sendPacketToHost(Packet packet) {
        byte[] data = packet.getData();

        DatagramPacket udpPacket =
                new DatagramPacket(data, data.length, hostAddress, HOST_PORT);
        try {
            socket.send(udpPacket);
            packet.getResponseHandler().onCompleted(Packet.Response.SUCCESS);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send packet to host!", e);
            packet.getResponseHandler().onCompleted(Packet.Response.IO_ERROR);
        }
    }

    @WorkerThread
    public void receiveHostPacketResponse(Packet packet, boolean validate) {
        byte[] expectedData = packet.getData();
        byte[] buffer = new byte[expectedData.length];
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(udpPacket);
            Log.i(TAG, new String(udpPacket.getData()));
            if (!Arrays.equals(expectedData, udpPacket.getData()) && validate) {
                packet.getResponseHandler().onCompleted(Packet.Response.CORRUPTED_DATA);
            } else {
                packet.setData(udpPacket.getData());

                packet.getResponseHandler().onCompleted(Packet.Response.SUCCESS);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Failed to receive packet - Socket Timeout", e);
            packet.getResponseHandler().onCompleted(Packet.Response.TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to receive packet from client!", e);
            packet.getResponseHandler().onCompleted(Packet.Response.IO_ERROR);
        }
    }
}

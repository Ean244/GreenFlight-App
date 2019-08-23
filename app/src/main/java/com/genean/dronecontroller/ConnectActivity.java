package com.genean.dronecontroller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.genean.dronecontroller.packet.Packet;
import com.genean.dronecontroller.packet.PacketHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";
    private static final String SSID = String.format("\"%s\"", "GreenFlight");
    private static final String PSK = String.format("\"%s\"", "loremipsum");
    private final ExecutorService connectExecutor = Executors.newSingleThreadExecutor();
    private Future<?> connectTask = CompletableFuture.completedFuture(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        final Button connect = findViewById(R.id.connectButton);
        connect.setOnClickListener(this::onClickConnect);
    }

    @Override
    protected void onPause() {
        super.onPause();

        connectTask.cancel(true);
    }

    private void onClickConnect(View view) {
        if(!connectTask.isDone())
            return;

        if (!checkPermissions())
            return;

        if (!connectHostAp())
            return;

        connectTask = connectExecutor.submit(this::startConnection);
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            toast(R.string.no_perms);
            return false;
        }

        return true;
    }

    private boolean connectHostAp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager == null) {
            Log.w(TAG, "WifiManager is null!");
            return false;
        }

        WifiConfiguration wifiConfig = wifiManager.getConfiguredNetworks()
                .stream()
                .filter(c -> SSID.equals(c.SSID))
                .findAny()
                .orElse(null);

        if (wifiConfig == null) {
            wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = SSID;
            wifiConfig.preSharedKey = PSK;
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiManager.addNetwork(wifiConfig);
        }

        if (!wifiManager.enableNetwork(wifiConfig.networkId, true)) {
            toast(R.string.wifi_fail_conn);
            return false;
        }

        return true;
    }

    private void startConnection() {
        Packet packet = new Packet(FlightCommand.CLIENT_CONNECT_QUERY);
        packet.setOnCompleted(response -> {
            if(response == Packet.Response.SUCCESS) {
                waitForConnectResponse();
            } else {
                toast(R.string.packet_send_fail);
            }
        });

        PacketHandler.INSTANCE.sendPacketToHost(packet);
    }

    private void waitForConnectResponse() {
        Packet packet = new Packet(FlightCommand.HOST_CONNECT_RESPONSE);

        packet.setOnCompleted(response -> {
            if(response == Packet.Response.SUCCESS) {
                toast(R.string.connect_success);

                Intent intent = new Intent(this, ControllerActivity.class);
                this.startActivity(intent);
            } else if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.CORRUPTED_DATA) {
                toast(R.string.packet_response_corrupted);
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(packet, true);
    }

    private void toast(int id) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_LONG).show());
    }
}

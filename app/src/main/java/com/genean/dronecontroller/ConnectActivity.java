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

import java.util.Arrays;

public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";
    private static final String SSID = String.format("\"%s\"", "GreenFlight");
    private static final String PSK = String.format("\"%s\"", "loremipsum");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        final Button connect = findViewById(R.id.connectButton);
        connect.setOnClickListener(this::onClickConnect);

        final Button throttle = findViewById(R.id.throttleButton);
        throttle.setOnClickListener(this::onClickThrottle);
    }

    private void onClickConnect(View view) {
        if (!checkPermissions())
            return;

        if (!connectHostAp())
            return;

        startConnection();

        Intent intent = new Intent(this, ControllerActivity.class);
        this.startActivity(intent);
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
        Thread thread = new Thread(() -> {
            if (!PacketHandler.INSTANCE.sendPacket(FlightCommands.CLIENT_CONNECT.getCommand().getBytes())) {
                toast(R.string.conn_packet_send_fail);
            }

            byte[] ack = new byte[FlightCommands.HOST_ACK.getCommand().getBytes().length];

            if (!PacketHandler.INSTANCE.receivePacket(ack)) {
                toast(R.string.conn_no_response);
                return;
            }

            if (Arrays.equals(ack, FlightCommands.HOST_ACK.getCommand().getBytes())) {
                toast(R.string.conn_success);
                Intent intent = new Intent(this, ControllerActivity.class);
                this.startActivity(intent);
            } else {
                Log.w(TAG, "Corrupted ack received!");
                toast(R.string.conn_data_corrupted);
            }
        });

        thread.start();
    }

    private void toast(int id) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_LONG).show());
    }

    private void onClickThrottle(View view) {

    }
}

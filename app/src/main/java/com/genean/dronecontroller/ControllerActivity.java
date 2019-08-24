package com.genean.dronecontroller;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.genean.dronecontroller.packet.Packet;
import com.genean.dronecontroller.packet.PacketHandler;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ControllerActivity extends AppCompatActivity {
    private int pitchChannel = 1500;
    private int yawChannel = 1500;
    private int rollChannel = 1500;
    private int throttleChannel = 1500;

    private final ScheduledExecutorService channelScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> channelTask;
    private final ScheduledExecutorService batteryScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> batteryTask;
    private final ExecutorService armExecutor = Executors.newSingleThreadExecutor();
    private Future<?> armTask = CompletableFuture.completedFuture(null);

    private TextView channelDisplayText;
    private TextView voltageDisplayText;
    private Button armButton;

    private boolean armed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        channelDisplayText = findViewById(R.id.channelDisplayText);
        voltageDisplayText = findViewById(R.id.voltageDisplayText);

        JoystickView leftJoystick = findViewById(R.id.leftJoystick);
        JoystickView rightJoystick = findViewById(R.id.rightJoystick);
        leftJoystick.setAutoReCenterButton(false);
        rightJoystick.setAutoReCenterButton(false);
        leftJoystick.setOnMoveListener(this::onLeftJoystickMove);
        rightJoystick.setOnMoveListener(this::onRightJoystickMove);

        armButton = findViewById(R.id.armButton);
        armButton.setOnClickListener(this::onArmButtonClicked);

        final Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this::onSettingsClicked);
    }

    @Override
    protected void onResume() {
        super.onResume();

        channelTask = channelScheduler.scheduleWithFixedDelay(this::sendChannelData, 0L, 250L, TimeUnit.MILLISECONDS);
        batteryTask = batteryScheduler.scheduleWithFixedDelay(this::queryVoltage, 2L, 15L, TimeUnit.SECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        channelTask.cancel(false);
        batteryTask.cancel(false);
        armTask.cancel(true);
    }

    private void onLeftJoystickMove(int angle, int strength) {
        double angleRadians = Math.toRadians(angle);
        throttleChannel = 1500 + (int) (strength * 5 * Math.sin(angleRadians));
        yawChannel = 1500 + (int) (strength * 5 * Math.cos(angleRadians));

        updateChannelDisplay();
    }

    private void onRightJoystickMove(int angle, int strength) {
        double angleRadians = Math.toRadians(angle);
        pitchChannel = 1500 + (int) (strength * 5 * Math.sin(angleRadians));
        rollChannel = 1500 + (int) (strength * 5 * Math.cos(angleRadians));

        updateChannelDisplay();
    }

    private void updateChannelDisplay() {
        String text = String.format(getString(R.string.channel_display_info), throttleChannel,
                yawChannel, pitchChannel, rollChannel);
        runOnUiThread(() -> channelDisplayText.setText(text));
    }

    private void onArmButtonClicked(View view) {
        if (!armTask.isDone())
            return;

        if (!armed) {
            if (throttleChannel > 1100) {
                toast(R.string.throttle_not_low);
                return;
            }

            armTask = armExecutor.submit(this::sendArmPacket);
        } else {
            armTask = armExecutor.submit(this::sendDisarmPacket);
        }
    }

    private void sendArmPacket() {
        Packet packet = new Packet(FlightCommand.CLIENT_ARM_QUERY);
        packet.setOnCompleted((response) -> {
            if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_send_fail);
            } else if (response == Packet.Response.SUCCESS) {
                waitForArmResponse();
            }
        });

        PacketHandler.INSTANCE.sendPacketToHost(packet);
    }

    private void sendDisarmPacket() {
        Packet packet = new Packet(FlightCommand.CLIENT_DISARM_QUERY);
        packet.setOnCompleted((response) -> {
            if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_send_fail);
            } else if (response == Packet.Response.SUCCESS) {
                waitForDisarmResponse();
            }
        });

        PacketHandler.INSTANCE.sendPacketToHost(packet);
    }

    private void waitForArmResponse() {
        Packet packet = new Packet(FlightCommand.HOST_ARM_RESPONSE);
        packet.setOnCompleted((response) -> {
            if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.CORRUPTED_DATA) {
                toast(R.string.packet_response_corrupted);
            } else if (response == Packet.Response.SUCCESS) {
                toast(R.string.arm_success);
                armed = true;
                runOnUiThread(() -> armButton.setText(R.string.disarm));
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(packet, true);
    }

    private void waitForDisarmResponse() {
        Packet packet = new Packet(FlightCommand.HOST_DISARM_RESPONSE);
        packet.setOnCompleted((response) -> {
            if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.CORRUPTED_DATA) {
                toast(R.string.packet_response_corrupted);
            } else if (response == Packet.Response.SUCCESS) {
                toast(R.string.disarm_success);
                armed = false;
                runOnUiThread(() -> armButton.setText(R.string.arm));
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(packet, true);
    }

    private void sendChannelData() {
        String cmd = String.format(FlightCommand.CLIENT_CHANNEL_UPDATE.toString(), throttleChannel,
                yawChannel, pitchChannel, rollChannel);

        Packet packet = new Packet(cmd.getBytes());
        PacketHandler.INSTANCE.sendPacketToHost(packet);
    }

    private void queryVoltage() {
        Packet query = new Packet(FlightCommand.CLIENT_VOLTAGE_QUERY);
        PacketHandler.INSTANCE.sendPacketToHost(query);

        Packet response = new Packet(FlightCommand.HOST_VOLTAGE_RESPONSE);
        response.setOnCompleted(r -> {
            if (r == Packet.Response.SUCCESS) {
                byte[] data = response.getData();
                String s = new String(Arrays.copyOfRange(data, 1, data.length));

                if(!s.matches("\\d{4}")) {
                    toast(R.string.packet_response_corrupted);
                    return;
                }

                double voltage = (double) Integer.parseInt(s) / 100;

                String text = String.format(getString(R.string.voltage_display_info), voltage);
                runOnUiThread(() -> voltageDisplayText.setText(text));
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(response, false);
    }

    private void onSettingsClicked(View view) {
        if(armed) {
            toast(R.string.settings_drone_not_disarmed);
            return;
        }

        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    private void toast(int id) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_LONG).show());
    }
}

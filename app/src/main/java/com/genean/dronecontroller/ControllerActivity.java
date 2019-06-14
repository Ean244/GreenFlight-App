package com.genean.dronecontroller;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ControllerActivity extends AppCompatActivity {
    private int pitchChannel = 1500;
    private int yawChannel = 1500;
    private int rollChannel = 1500;
    private int throttleChannel = 1500;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> channelTask;

    private TextView channelDisplayText;
    private Button armButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        channelDisplayText = findViewById(R.id.channelDisplayText);

        JoystickView leftJoystick = findViewById(R.id.leftJoystick);
        JoystickView rightJoystick = findViewById(R.id.rightJoystick);

        leftJoystick.setAutoReCenterButton(false);
        rightJoystick.setAutoReCenterButton(false);

        //TODO: refresh rate
        leftJoystick.setOnMoveListener(this::onLeftJoystickMove);
        rightJoystick.setOnMoveListener(this::onRightJoystickMove);

        armButton = findViewById(R.id.armButton);
        armButton.setOnClickListener(this::onArmButtonClicked);
    }

    private void onArmButtonClicked(View view) {
        if (armButton.getText().equals(getString(R.string.arm))) {
            PacketHandler.INSTANCE.sendPacket(FlightCommands.CLIENT_ARM.getCommand().getBytes());

            new Thread(() -> {
                byte[] buff = new byte[5];
                byte[] ack = FlightCommands.HOST_ARM_ACK.getCommand().getBytes();

                if (!PacketHandler.INSTANCE.receivePacket(buff) || !Arrays.equals(ack, buff)) {
                    toast(R.string.arm_fail);
                    return;
                }

                toast(R.string.arm_success);
                armButton.setText(R.string.disarm);
            }).start();
        } else {
            PacketHandler.INSTANCE.sendPacket(FlightCommands.CLIENT_DISARM.getCommand().getBytes());

            new Thread(() -> {
                byte[] buff = new byte[8];
                byte[] ack = FlightCommands.HOST_DISARM_ACK.getCommand().getBytes();

                if (!PacketHandler.INSTANCE.receivePacket(buff) || !Arrays.equals(ack, buff)) {
                    toast(R.string.disarm_fail);
                    return;
                }

                toast(R.string.disarm_success);
                armButton.setText(R.string.arm);
            }).start();
        }
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
        channelDisplayText.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //200hz
        channelTask = scheduler.scheduleWithFixedDelay(this::sendChannelData, 0L, 5L, TimeUnit.MILLISECONDS);
    }

    private void sendChannelData() {
        String cmd = String.format(FlightCommands.SEND_CHANNEL_DATA.getCommand(), throttleChannel,
                yawChannel, pitchChannel, rollChannel);
        PacketHandler.INSTANCE.sendPacket(cmd.getBytes());
    }

    @Override
    protected void onPause() {
        super.onPause();

        channelTask.cancel(false);
    }

    private void toast(int id) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_LONG).show());
    }
}

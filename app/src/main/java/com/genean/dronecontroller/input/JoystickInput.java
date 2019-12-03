package com.genean.dronecontroller.input;

import android.app.Activity;
import android.view.View;

import com.genean.dronecontroller.R;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class JoystickInput implements InputDevice {
    private int throttle = 1000;
    private int roll = 1500;
    private int pitch = 1500;
    private int yaw = 1500;

    @Override
    public void init(Activity activity) {
        JoystickView leftJoystick = activity.findViewById(R.id.leftJoystick);
        JoystickView rightJoystick = activity.findViewById(R.id.rightJoystick);

        leftJoystick.setAutoReCenterButton(false);
        leftJoystick.setVisibility(View.VISIBLE);

        rightJoystick.setAutoReCenterButton(true);
        rightJoystick.setVisibility(View.VISIBLE);

        leftJoystick.setOnMoveListener(this::onLeftJoystickMove);
        rightJoystick.setOnMoveListener(this::onRightJoystickMove);
    }

    private void onLeftJoystickMove(int angle, int strength) {
        double angleRadians = Math.toRadians(angle);
        throttle = 1500 + (int) (strength * 5 * Math.sin(angleRadians));
        yaw = 1500 + (int) (strength * 5 * Math.cos(angleRadians));
    }

    private void onRightJoystickMove(int angle, int strength) {
        double angleRadians = Math.toRadians(angle);
        pitch = 1500 + (int) (strength * 5 * Math.sin(angleRadians));
        roll = 1500 + (int) (strength * 5 * Math.cos(angleRadians));
    }

    @Override
    public int getRoll() {
        return roll;
    }

    @Override
    public int getPitch() {
        return pitch;
    }

    @Override
    public int getYaw() {
        return yaw;
    }

    @Override
    public int getThrottle() {
        return throttle;
    }
}

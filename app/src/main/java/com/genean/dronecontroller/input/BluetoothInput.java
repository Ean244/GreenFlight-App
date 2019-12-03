package com.genean.dronecontroller.input;

import android.app.Activity;

public class BluetoothInput implements InputDevice {

    @Override
    public void init(Activity activity) {

    }

    @Override
    public int getRoll() {
        return 0;
    }

    @Override
    public int getPitch() {
        return 0;
    }

    @Override
    public int getYaw() {
        return 0;
    }

    @Override
    public int getThrottle() {
        return 0;
    }
}

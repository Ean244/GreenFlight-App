package com.genean.dronecontroller.input;

import android.app.Activity;

public interface InputDevice {
    String BUNDLE_KEY = "input-type";

    void init(Activity activity);

    int getRoll();

    int getPitch();

    int getYaw();

    int getThrottle();
}

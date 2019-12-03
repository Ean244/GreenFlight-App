package com.genean.dronecontroller.input;

public class InputDeviceFactory {
    private InputDeviceFactory() {
    }

    public static InputDevice createDevice(DeviceType type) {
        if (type == DeviceType.SOFTWARE_JOYSTICK) {
            return new JoystickInput();
        } else if (type == DeviceType.BLUETOOTH) {
            return new BluetoothInput();
        } else {
            throw new IllegalStateException("Unknown device type!");
        }
    }
}

package android.hardware.input;

import android.view.InputDevice;

interface IInputManager {
    boolean injectInputEvent(in InputEvent event, int mode);
    InputDevice getInputDevice(int id);
}
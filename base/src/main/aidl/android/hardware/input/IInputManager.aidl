package android.hardware.input;


interface IInputManager {
    boolean injectInputEvent(in InputEvent event, int mode);
}
package android.hardware.input;

/**
 * Created by sds100 on 15/07/2021.
 */
interface IInputManager {
    boolean injectInputEvent(in InputEvent event, int mode);
}
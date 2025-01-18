package io.github.sds100.keymapper.api;

import android.view.KeyEvent;
import android.view.MotionEvent;

interface IKeyEventRelayServiceCallback {
    boolean onKeyEvent(in KeyEvent event);
    boolean onMotionEvent(in MotionEvent event);
}
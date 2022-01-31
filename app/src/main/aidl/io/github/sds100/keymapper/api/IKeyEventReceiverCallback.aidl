package io.github.sds100.keymapper.api;

import android.view.KeyEvent;

interface IKeyEventReceiverCallback {
    boolean onKeyEvent(in KeyEvent event);
}
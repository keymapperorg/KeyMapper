package io.github.sds100.keymapper.api;

import android.view.KeyEvent;
import io.github.sds100.keymapper.api.IKeyEventReceiverCallback;

interface IKeyEventReceiver {
    boolean onKeyEvent(in KeyEvent event);
    void registerCallback(IKeyEventReceiverCallback client);
    void unregisterCallback(IKeyEventReceiverCallback client);
}
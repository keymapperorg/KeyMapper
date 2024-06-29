package io.github.sds100.keymapper.api;

import android.view.KeyEvent;
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback;

interface IKeyEventRelayService {
    /**
     * Send a key event to the target package that is registered with
     * a callback.
     */
    boolean sendKeyEvent(in KeyEvent event, in String targetPackageName);

    /**
     * Register a callback to receive key events from this relay service. The service
     * checks the process uid of the caller to this method and only permits certain applications
     * from connecting.
     */
    void registerCallback(IKeyEventRelayServiceCallback client);

    /**
     * Unregister a callback to receive key events from this relay service. The service
     * checks the process uid of the caller to this method and only permits certain applications
     * from connecting.
     */
    void unregisterCallback();
}
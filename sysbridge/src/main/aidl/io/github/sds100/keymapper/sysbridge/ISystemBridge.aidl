package io.github.sds100.keymapper.sysbridge;

import io.github.sds100.keymapper.sysbridge.IEvdevCallback;
import io.github.sds100.keymapper.sysbridge.utils.InputDeviceIdentifier;
import android.view.InputEvent;

interface ISystemBridge {
   // Destroy method defined by Shizuku server. This is required
   // for Shizuku user services.
   // See demo/service/UserService.java in the Shizuku-API repository.
   // TODO is this used?
   void destroy() = 16777114;

   boolean grabEvdevDevice(int deviceId) = 1;
   void registerEvdevCallback(IEvdevCallback callback) = 2;
   void unregisterEvdevCallback() = 3;
   boolean injectEvent(in InputEvent event, int mode) = 4;
}
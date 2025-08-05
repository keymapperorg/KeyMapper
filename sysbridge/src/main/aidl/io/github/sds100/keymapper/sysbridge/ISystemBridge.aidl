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
   boolean ungrabEvdevDevice(int deviceId) = 2;
   boolean ungrabAllEvdevDevices() = 3;
   void registerEvdevCallback(IEvdevCallback callback) = 4;
   void unregisterEvdevCallback() = 5;
   boolean writeEvdevEvent(int deviceId, int type, int code, int value) = 6;
   boolean injectInputEvent(in InputEvent event, int mode) = 7;
}
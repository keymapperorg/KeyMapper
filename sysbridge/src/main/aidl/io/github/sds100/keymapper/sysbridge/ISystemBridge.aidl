package io.github.sds100.keymapper.sysbridge;

import io.github.sds100.keymapper.sysbridge.IEvdevCallback;
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle;
import android.view.InputEvent;

interface ISystemBridge {
   // Destroy method defined by Shizuku server. This is required
   // for Shizuku user services.
   // See demo/service/UserService.java in the Shizuku-API repository.
   // TODO use this from Key Mapper to kill the system bridge
   void destroy() = 16777114;

   boolean grabEvdevDevice(String devicePath) = 1;

   boolean ungrabEvdevDevice(String devicePath) = 3;
   boolean ungrabAllEvdevDevices() = 4;

   void registerEvdevCallback(IEvdevCallback callback) = 5;
   void unregisterEvdevCallback() = 6;

   boolean writeEvdevEvent(String devicePath, int type, int code, int value) = 7;
   boolean injectInputEvent(in InputEvent event, int mode) = 8;

   EvdevDeviceHandle[] getEvdevInputDevices() = 9;
}
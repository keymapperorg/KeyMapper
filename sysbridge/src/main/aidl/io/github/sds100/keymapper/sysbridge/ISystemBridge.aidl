package io.github.sds100.keymapper.sysbridge;

import io.github.sds100.keymapper.sysbridge.IEvdevCallback;
import io.github.sds100.keymapper.common.models.EvdevDeviceHandle;
import android.view.InputEvent;

interface ISystemBridge {
   void destroy() = 16777114;
   int getProcessUid() = 16777113;
   int getVersionCode() = 16777112;
   String executeCommand(String command) = 16777111;

   boolean grabEvdevDevice(String devicePath) = 1;
   boolean grabEvdevDeviceArray(in String[] devicePath) = 2;

   boolean ungrabEvdevDevice(String devicePath) = 3;
   boolean ungrabAllEvdevDevices() = 4;

   void registerEvdevCallback(IEvdevCallback callback) = 5;
   void unregisterEvdevCallback() = 6;

   boolean writeEvdevEvent(String devicePath, int type, int code, int value) = 7;
   boolean injectInputEvent(in InputEvent event, int mode) = 8;

   EvdevDeviceHandle[] getEvdevInputDevices() = 9;

   boolean setWifiEnabled(boolean enable) = 10;

   void grantPermission(String permission, int deviceId) = 11;

   void setDataEnabled(int subId, boolean enable) = 12;
}
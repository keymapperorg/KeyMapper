package io.github.sds100.keymapper.sysbridge;

import io.github.sds100.keymapper.evdev.IEvdevCallback;
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo;
import io.github.sds100.keymapper.common.models.GrabbedDeviceHandle;
import io.github.sds100.keymapper.common.models.ShellResult;
import android.view.InputEvent;

interface ISystemBridge {
   void destroy() = 16777114;
   int getProcessUid() = 16777113;
   int getVersionCode() = 16777112;
   ShellResult executeCommand(String command, long timeoutMillis) = 16777111;

   GrabbedDeviceHandle[] setGrabbedDevices(in EvdevDeviceInfo[] devices) = 1;

   void registerEvdevCallback(IEvdevCallback callback) = 5;
   void unregisterEvdevCallback() = 6;

   boolean writeEvdevEvent(int deviceId, int type, int code, int value) = 7;
   boolean injectInputEvent(in InputEvent event, int mode) = 8;

   EvdevDeviceInfo[] getEvdevInputDevices() = 9;

   boolean setWifiEnabled(boolean enable) = 10;

   void grantPermission(String permission, int deviceId) = 11;

   void setDataEnabled(int subId, boolean enable) = 12;

   void setBluetoothEnabled(boolean enable) = 13;

   void setNfcEnabled(boolean enable) = 14;

   void setAirplaneMode(boolean enable) = 15;

   void forceStopPackage(String packageName) = 16;

   void removeTasks(String packageName) = 17;

   void setRingerMode(int ringerMode) = 18;

   boolean isTetheringEnabled() = 19;

   void setTetheringEnabled(boolean enable) = 20;

   long getUsbScreenUnlockedFunctions() = 21;
}
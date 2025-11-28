package io.github.sds100.keymapper.evdev;

import io.github.sds100.keymapper.common.models.GrabbedDeviceHandle;

interface IEvdevCallback {
  /**
   * deviceId is the internal system bridge ID for the device. This is used rather than referencing
   * with a path because primitives have lower overhead and are safer over the JNI boundary.
   */
   boolean onEvdevEvent(int deviceId, long timeSec, long timeUsec, int type, int code, int value, int androidCode);
   void onEmergencyKillSystemBridge();
   void onGrabbedDevicesChanged(in GrabbedDeviceHandle[] devices);
}
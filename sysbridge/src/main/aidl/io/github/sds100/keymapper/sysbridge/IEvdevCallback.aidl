package io.github.sds100.keymapper.sysbridge;

interface IEvdevCallback {
   void onEvdevEvent(int deviceId, long timeSec, long timeUsec, int type, int code, int value, int androidCode);
}
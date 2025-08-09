package io.github.sds100.keymapper.sysbridge;

interface IEvdevCallback {
   void onEvdevEventLoopStarted();
   boolean onEvdevEvent(String devicePath, long timeSec, long timeUsec, int type, int code, int value, int androidCode);
}
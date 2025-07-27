package io.github.sds100.keymapper.sysbridge;

interface IEvdevCallback {
   void onEvdevEvent(int type, int code, int value);
}
package io.github.sds100.keymapper.sysbridge;

import io.github.sds100.keymapper.sysbridge.IEvdevCallback;
import io.github.sds100.keymapper.sysbridge.utils.InputDeviceIdentifier;

interface ISystemBridge {
   // Destroy method defined by Shizuku server. This is required
   // for Shizuku user services.
   // See demo/service/UserService.java in the Shizuku-API repository.
   void destroy() = 16777114;

   void grabEvdevDevice(in InputDeviceIdentifier deviceId, IEvdevCallback callback) = 1;
}
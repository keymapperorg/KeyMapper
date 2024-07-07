package android.permission;

import android.os.IBinder;

public interface IPermissionManager extends android.os.IInterface {
    void grantRuntimePermission(String packageName, String permissionName, int userId);

    void grantRuntimePermission(String packageName, String permissionName, int deviceId, int userId);

    void grantRuntimePermission(String packageName, String permissionName, String persistentDeviceId, int userId);

    abstract class Stub extends android.os.Binder implements IPermissionManager {
        public static IPermissionManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
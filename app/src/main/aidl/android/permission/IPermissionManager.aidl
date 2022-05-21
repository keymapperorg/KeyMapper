package android.permission;

// Only on Android 12+
interface IPermissionManager {
    void grantRuntimePermission(String packageName, String permissionName, int userId);
}
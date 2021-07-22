package android.content.pm;

interface IPackageManager {
    void grantRuntimePermission(String packageName, String permissionName, int userId);
}
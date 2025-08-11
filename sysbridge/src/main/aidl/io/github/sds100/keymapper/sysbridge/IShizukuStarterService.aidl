package io.github.sds100.keymapper.sysbridge;

interface IShizukuStarterService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    // Make it oneway so that an exception isn't thrown when the method kills itself at the end
    oneway void startSystemBridge(String scriptPath, String apkPath, String libPath, String packageName) = 1;
}
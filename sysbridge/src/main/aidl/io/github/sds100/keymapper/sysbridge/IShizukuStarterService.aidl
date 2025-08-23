package io.github.sds100.keymapper.sysbridge;

interface IShizukuStarterService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    String executeCommand(String command) = 1;
}
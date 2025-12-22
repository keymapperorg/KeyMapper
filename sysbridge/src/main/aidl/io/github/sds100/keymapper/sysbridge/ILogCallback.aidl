package io.github.sds100.keymapper.sysbridge;

interface ILogCallback {
    /**
     * Called when a log message is emitted from the SystemBridge.
     * @param level The log level: 0=ERROR, 1=WARN, 2=INFO, 3=DEBUG
     * @param message The log message (already prefixed with "systembridge:")
     */
    void onLog(int level, String message);
}

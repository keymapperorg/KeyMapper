package io.github.sds100.keymapper.sysbridge.service

enum class SystemBridgeSetupStep(val stepIndex: Int) {
    ACCESSIBILITY_SERVICE(stepIndex = 0),
    NOTIFICATION_PERMISSION(stepIndex = 1),
    DEVELOPER_OPTIONS(stepIndex = 2),
    WIFI_NETWORK(stepIndex = 3),
    WIRELESS_DEBUGGING(stepIndex = 4),
    ADB_PAIRING(stepIndex = 5),
    START_SERVICE(stepIndex = 6),
    STARTED(stepIndex = 7),
}

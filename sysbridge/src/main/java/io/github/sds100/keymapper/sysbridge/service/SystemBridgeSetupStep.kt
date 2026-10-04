package io.github.sds100.keymapper.sysbridge.service

enum class SystemBridgeSetupStep(val stepIndex: Int) {
    ACCESSIBILITY_SERVICE(stepIndex = 0),
    NOTIFICATION_PERMISSION(stepIndex = 1),
    ACCESS_LOCAL_NETWORK_PERMISSION(stepIndex = 2),
    SAMSUNG_AUTO_BLOCKER(stepIndex = 3),
    DEVELOPER_OPTIONS(stepIndex = 4),
    WIFI_NETWORK(stepIndex = 5),
    WIRELESS_DEBUGGING(stepIndex = 6),
    ADB_PAIRING(stepIndex = 7),
    START_SERVICE(stepIndex = 8),
    STARTED(stepIndex = 9),
}

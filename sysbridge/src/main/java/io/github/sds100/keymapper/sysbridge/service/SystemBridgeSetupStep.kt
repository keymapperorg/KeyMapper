package io.github.sds100.keymapper.sysbridge.service

enum class SystemBridgeSetupStep(val stepIndex: Int) {
    ACCESSIBILITY_SERVICE(stepIndex = 0),
    NOTIFICATION_PERMISSION(stepIndex = 1),
    ACCESS_LOCAL_NETWORK_PERMISSION(stepIndex = 2),
    DEVELOPER_OPTIONS(stepIndex = 3),
    WIFI_NETWORK(stepIndex = 4),
    WIRELESS_DEBUGGING(stepIndex = 5),
    ADB_PAIRING(stepIndex = 6),
    START_SERVICE(stepIndex = 7),
    STARTED(stepIndex = 8),
}

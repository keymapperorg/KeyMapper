package io.github.sds100.keymapper.sysbridge.service

enum class SystemBridgeSetupStep(val stepIndex: Int) {
    ACCESSIBILITY_SERVICE(stepIndex = 0),
    DEVELOPER_OPTIONS(stepIndex = 1),
    WIFI_NETWORK(stepIndex = 2),
    WIRELESS_DEBUGGING(stepIndex = 3),
    ADB_PAIRING(stepIndex = 4),
    ADB_CONNECT(stepIndex = 5),
    START_SERVICE(stepIndex = 6)
}
package io.github.sds100.keymapper.system.inputevents

sealed interface KMInputEvent {
    val deviceId: Int?
}
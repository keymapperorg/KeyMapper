package io.github.sds100.keymapper.system.inputevents

import io.github.sds100.keymapper.common.models.EvdevDeviceHandle

data class KMEvdevEvent(
    val device: EvdevDeviceHandle,
    val type: Int,
    val code: Int,
    val value: Int,
    val androidCode: Int,
    val timeSec: Long,
    val timeUsec: Long,
) : KMInputEvent {

    companion object {
        const val TYPE_SYN_EVENT = 0
        const val TYPE_KEY_EVENT = 1
        const val TYPE_REL_EVENT = 2

        const val VALUE_DOWN = 1
        const val VALUE_UP = 0
    }

    // Look at input-event-codes.h for where these are defined.
    // EV_SYN
    val isSynEvent: Boolean = type == TYPE_SYN_EVENT

    // EV_KEY
    val isKeyEvent: Boolean = type == TYPE_KEY_EVENT

    // EV_REL
    val isRelEvent: Boolean = type == TYPE_REL_EVENT

    val isDownEvent: Boolean = isKeyEvent && value == VALUE_DOWN
    val isUpEvent: Boolean = isKeyEvent && value == VALUE_UP
}

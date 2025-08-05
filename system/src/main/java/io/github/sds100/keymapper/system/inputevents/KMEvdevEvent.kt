package io.github.sds100.keymapper.system.inputevents

data class KMEvdevEvent(
    val deviceId: Int,
    val type: Int,
    val code: Int,
    val value: Int,

    // This is only non null when receiving an event. If sending an event
    // then these values do not need to be set.
    val androidCode: Int? = null,
    val timeSec: Long? = null,
    val timeUsec: Long? = null
) : KMInputEvent {

    // Look at input-event-codes.h for where these are defined.
    // EV_SYN
    val isSynEvent: Boolean = type == 0

    // EV_KEY
    val isKeyEvent: Boolean = type == 1

    // EV_REL
    val isRelEvent: Boolean = type == 2
}

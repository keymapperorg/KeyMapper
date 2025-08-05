package io.github.sds100.keymapper.system.inputevents

data class KMEvdevEvent(
    val deviceId: Int,
    val type: Int,
    val code: Int,
    val value: Int,

    // This is only non null when receiving an event. If sending an event
    // then the time does not need to be set.
    val timeSec: Long? = null,
    val timeUsec: Long? = null
) : KMInputEvent

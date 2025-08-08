package io.github.sds100.keymapper.base.input

import android.os.SystemClock
import android.view.KeyEvent

data class InjectKeyEventModel(
    val keyCode: Int,
    val action: Int,
    val metaState: Int,
    val deviceId: Int,
    val scanCode: Int,
    val source: Int,
    val repeatCount: Int = 0
) {
    fun toAndroidKeyEvent(): KeyEvent {
        val eventTime = SystemClock.uptimeMillis()
        return KeyEvent(
            eventTime,
            eventTime,
            action,
            keyCode,
            repeatCount,
            metaState,
            deviceId,
            scanCode,
            source
        )
    }
}

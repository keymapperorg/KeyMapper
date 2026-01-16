package io.github.sds100.keymapper.common.utils

import android.os.SystemClock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClockImpl @Inject constructor() : Clock {
    override fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }

    override fun unixTimestamp(): Long {
        return Instant.now().epochSecond
    }
}

interface Clock {
    fun elapsedRealtime(): Long
    fun unixTimestamp(): Long
}

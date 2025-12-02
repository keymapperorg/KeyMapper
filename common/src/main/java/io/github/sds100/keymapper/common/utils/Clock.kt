package io.github.sds100.keymapper.common.utils

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClockImpl @Inject constructor() : Clock {
    override fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }
}

interface Clock {
    fun elapsedRealtime(): Long
}

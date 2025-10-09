package io.github.sds100.keymapper.system.lock

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow


interface LockScreenAdapter {
    fun secureLockDevice(): KMResult<*>

    fun isLocked(): Boolean
    fun isLockedFlow(): Flow<Boolean>
    fun isLockScreenShowing(): Boolean
    fun isLockScreenShowingFlow(): Flow<Boolean>
}

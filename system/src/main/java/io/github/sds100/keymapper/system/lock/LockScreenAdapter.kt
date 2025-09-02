package io.github.sds100.keymapper.system.lock

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow


interface LockScreenAdapter {
    fun secureLockDevice(): KMResult<*>

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun isLocked(): Boolean

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun isLockedFlow(): Flow<Boolean>

    fun isLockScreenShowing(): Boolean
    fun isLockScreenShowingFlow(): Flow<Boolean>
}

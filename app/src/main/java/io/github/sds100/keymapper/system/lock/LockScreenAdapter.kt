package io.github.sds100.keymapper.system.lock

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 20/04/2021.
 */
interface LockScreenAdapter {
    fun secureLockDevice(): Result<*>

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun isLocked(): Boolean

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun isLockedFlow(): Flow<Boolean>
}

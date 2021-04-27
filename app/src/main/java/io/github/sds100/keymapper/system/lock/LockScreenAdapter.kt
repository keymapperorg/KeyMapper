package io.github.sds100.keymapper.system.lock

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 20/04/2021.
 */
interface LockScreenAdapter {
    fun secureLockDevice(): Result<*>
}
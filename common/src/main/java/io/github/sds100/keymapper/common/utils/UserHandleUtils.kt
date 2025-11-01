package io.github.sds100.keymapper.common.utils

import android.os.UserHandle

object UserHandleUtils {
    fun getCallingUserId(): Int {
        return UserHandle::class.java.getMethod("getCallingUserId").invoke(null) as Int
    }
}

fun UserHandle.getIdentifier(): Int {
    return UserHandle::class.java.getMethod("getIdentifier").invoke(this) as Int
}

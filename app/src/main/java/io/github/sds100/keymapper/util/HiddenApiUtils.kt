package io.github.sds100.keymapper.util

import android.os.UserHandle

/**
 * Created by sds100 on 20/07/2021.
 */

fun UserHandle.getIdentifier(): Int {
    val getIdentifierMethod = UserHandle::class.java.getMethod("getIdentifier")

    return getIdentifierMethod.invoke(this) as Int
}
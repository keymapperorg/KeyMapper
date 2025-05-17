package io.github.sds100.keymapper.base.util

import android.os.UserHandle



fun UserHandle.getIdentifier(): Int {
    val getIdentifierMethod = UserHandle::class.java.getMethod("getIdentifier")

    return getIdentifierMethod.invoke(this) as Int
}

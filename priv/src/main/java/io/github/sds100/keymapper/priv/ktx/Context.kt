package io.github.sds100.keymapper.priv.ktx

import android.content.Context
import android.os.Build

fun Context.createDeviceProtectedStorageContextCompat(): Context {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        createDeviceProtectedStorageContext()
    } else {
        this
    }
}

package io.github.sds100.keymapper.util

import android.Manifest
import android.content.Context
import android.provider.Settings
import androidx.annotation.RequiresPermission

/**
 * Created by sds100 on 31/12/2018.
 */

@RequiresPermission(Manifest.permission.WRITE_SETTINGS)
inline fun <reified T> Context.putSystemSetting(name: String, value: T) {

    when (T::class) {

        Int::class -> Settings.System.putInt(contentResolver, name, value as Int)
        String::class -> Settings.System.putString(contentResolver, name, value as String)
        Float::class -> Settings.System.putFloat(contentResolver, name, value as Float)
        Long::class -> Settings.System.putLong(contentResolver, name, value as Long)

        else -> {
            throw Exception("Setting type ${T::class} is not supported")
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
inline fun <reified T> Context.putSecureSetting(name: String, value: T) {

    when (T::class) {
        Int::class -> Settings.Secure.putInt(contentResolver, name, value as Int)
        String::class -> Settings.Secure.putString(contentResolver, name, value as String)
        Float::class -> Settings.Secure.putFloat(contentResolver, name, value as Float)
        Long::class -> Settings.Secure.putLong(contentResolver, name, value as Long)

        else -> {
            throw Exception("Setting type ${T::class} is not supported")
        }
    }
}
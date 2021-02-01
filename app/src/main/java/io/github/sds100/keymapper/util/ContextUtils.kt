package io.github.sds100.keymapper.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.preference.PreferenceManager
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 31/12/2018.
 */

fun Context.sendPackageBroadcast(action: String, extras: Bundle = Bundle.EMPTY) =
    Intent(action).apply {
        setPackage(Constants.PACKAGE_NAME)
        putExtras(extras)

        sendBroadcast(this)
    }
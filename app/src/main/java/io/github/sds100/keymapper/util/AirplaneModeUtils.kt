package io.github.sds100.keymapper.util

import android.content.Context
import android.provider.Settings
import io.github.sds100.keymapper.data.AppPreferences
import timber.log.Timber

/**
 * Created by sds100 on 16/06/2020.
 */

object AirplaneModeUtils {
    fun toggleAirplaneMode(ctx: Context) {
        if (!AppPreferences.hasRootPermission) return

        if (ctx.getGlobalSetting<Int>(Settings.Global.AIRPLANE_MODE_ON) == 0) {
            enableAirplaneMode()
        } else {
            disableAirplaneMode()
        }
    }

    fun enableAirplaneMode() {
        if (!AppPreferences.hasRootPermission) return

        RootUtils.executeRootCommand("settings put global airplane_mode_on 1")
        broadcastAirplaneModeChanged(true)
    }

    fun disableAirplaneMode() {
        if (!AppPreferences.hasRootPermission) return

        RootUtils.executeRootCommand("settings put global airplane_mode_on 0")
        broadcastAirplaneModeChanged(false)
    }

    private fun broadcastAirplaneModeChanged(enabled: Boolean) {
        Timber.d("am broadcast -a android.intent.action.AIRPLANE_MODE -ez state $enabled")
        RootUtils.executeRootCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }
}

package io.github.sds100.keymapper.util

import android.content.Context
import android.provider.Settings
import io.github.sds100.keymapper.data.hasRootPermission
import io.github.sds100.keymapper.globalPreferences

/**
 * Created by sds100 on 16/06/2020.
 */

object AirplaneModeUtils {
    fun toggleAirplaneMode(ctx: Context) {
        if (!ctx.globalPreferences.hasRootPermission.firstBlocking()) return

        if (ctx.getGlobalSetting<Int>(Settings.Global.AIRPLANE_MODE_ON) == 0) {
            enableAirplaneMode(ctx)
        } else {
            disableAirplaneMode(ctx)
        }
    }

    fun enableAirplaneMode(ctx: Context) {
        if (!ctx.globalPreferences.hasRootPermission.firstBlocking()) return

        RootUtils.executeRootCommand("settings put global airplane_mode_on 1")
        broadcastAirplaneModeChanged(true)
    }

    fun disableAirplaneMode(ctx: Context) {
        if (!ctx.globalPreferences.hasRootPermission.firstBlocking()) return

        RootUtils.executeRootCommand("settings put global airplane_mode_on 0")
        broadcastAirplaneModeChanged(false)
    }

    private fun broadcastAirplaneModeChanged(enabled: Boolean) {
        RootUtils.executeRootCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }
}

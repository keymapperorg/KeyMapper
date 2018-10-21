package io.github.sds100.keymapper.Utils

import android.content.Context
import android.net.wifi.WifiManager
import io.github.sds100.keymapper.StateChange
import io.github.sds100.keymapper.StateChange.*

/**
 * Created by sds100 on 21/10/2018.
 */

object WifiUtils {
    fun changeWifiState(ctx: Context, stateChange: StateChange) {
        val wifiManager = ctx.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

        when (stateChange) {
            ENABLE -> wifiManager.isWifiEnabled = true
            DISABLE -> wifiManager.isWifiEnabled = false
            TOGGLE -> wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }
}
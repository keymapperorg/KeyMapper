package io.github.sds100.keymapper.Utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import io.github.sds100.keymapper.StateChange

/**
 * Created by sds100 on 12/12/2018.
 */

object NetworkUtils {
    //WiFi stuff
    fun changeWifiState(ctx: Context, stateChange: StateChange) {
        val wifiManager = ctx.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

        when (stateChange) {
            StateChange.ENABLE -> wifiManager.isWifiEnabled = true
            StateChange.DISABLE -> wifiManager.isWifiEnabled = false
            StateChange.TOGGLE -> wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    //Mobile data stuff

    /**
     * REQUIRES ROOT!!
     */
    fun enableMobileData() {
        RootUtils.executeRootCommand("svc data enable")
    }

    /**
     * REQUIRES ROOT!!!
     */
    fun disableMobileData() {
        RootUtils.executeRootCommand("svc data disable")
    }

    fun toggleMobileData(ctx: Context) {
        if (isMobileDataEnabled(ctx)) {
            disableMobileData()
        } else {
            enableMobileData()
        }
    }

    private fun isMobileDataEnabled(ctx: Context): Boolean {
        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.isDataEnabled
        } else if (telephonyManager.dataState == TelephonyManager.DATA_CONNECTED) {
            return true
        }

        return false
    }
}
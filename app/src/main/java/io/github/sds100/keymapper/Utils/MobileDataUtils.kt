package io.github.sds100.keymapper.Utils

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Created by sds100 on 31/10/2018.
 */
object MobileDataUtils {
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
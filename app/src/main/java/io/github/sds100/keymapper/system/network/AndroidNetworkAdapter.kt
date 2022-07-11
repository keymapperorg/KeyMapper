package io.github.sds100.keymapper.system.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by sds100 on 24/04/2021.
 */
@Singleton
class AndroidNetworkAdapter @Inject constructor(
    @ApplicationContext context: Context,
    private val suAdapter: SuAdapter
) : NetworkAdapter {
    private val ctx = context.applicationContext

    private val wifiManager: WifiManager by lazy { ctx.getSystemService()!! }
    private val telephonyManager: TelephonyManager by lazy { ctx.getSystemService()!! }

    override val connectedWifiSSID: String?
        get() = wifiManager.connectionInfo?.ssid?.let { ssid ->
            if (ssid == WifiManager.UNKNOWN_SSID) {
                null
            } else {
                ssid.removeSurrounding("\"")
            }
        }

    override fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    override fun enableWifi(): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return suAdapter.execute("svc wifi enable")
        } else {
            wifiManager.isWifiEnabled = true
            return Success(Unit)
        }
    }

    override fun disableWifi(): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return suAdapter.execute("svc wifi disable")
        } else {
            wifiManager.isWifiEnabled = false
            return Success(Unit)
        }
    }

    override fun isMobileDataEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.isDataEnabled
        } else {
            return telephonyManager.dataState == TelephonyManager.DATA_CONNECTED
        }
    }

    override fun enableMobileData(): Result<*> {
        return suAdapter.execute("svc data enable")
    }

    override fun disableMobileData(): Result<*> {
        return suAdapter.execute("svc data disable")
    }

    /**
     * @return Null on Android 10+ because there is no API to do this anymore.
     */
    override fun getKnownWifiSSIDs(): List<String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return null
        } else {
            return wifiManager.configuredNetworks?.map {
                it.SSID.removeSurrounding("\"")
            } ?: emptyList()
        }
    }
}
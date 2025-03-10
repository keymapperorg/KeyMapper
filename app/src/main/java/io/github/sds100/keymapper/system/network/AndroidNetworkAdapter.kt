package io.github.sds100.keymapper.system.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by sds100 on 24/04/2021.
 */
class AndroidNetworkAdapter(
    context: Context,
    private val suAdapter: SuAdapter,
) : NetworkAdapter {
    private val ctx = context.applicationContext

    private val wifiManager: WifiManager by lazy { ctx.getSystemService()!! }
    private val telephonyManager: TelephonyManager by lazy { ctx.getSystemService()!! }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action ?: return

            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)

                    _isWifiEnabled.update { state == WifiManager.WIFI_STATE_ENABLED }
                }

                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    connectedWifiSSIDFlow.update { connectedWifiSSID }
                }
            }
        }
    }

    override val connectedWifiSSID: String?
        get() = wifiManager.connectionInfo?.ssid?.let { ssid ->
            if (ssid == WifiManager.UNKNOWN_SSID) {
                null
            } else {
                ssid.removeSurrounding("\"")
            }
        }

    override val connectedWifiSSIDFlow = MutableStateFlow(connectedWifiSSID)
    private val _isWifiEnabled = MutableStateFlow(isWifiEnabled())

    init {
        IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)

            ContextCompat.registerReceiver(
                ctx,
                broadcastReceiver,
                this,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    override fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    override fun isWifiEnabledFlow(): Flow<Boolean> = _isWifiEnabled

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

    override fun enableMobileData(): Result<*> = suAdapter.execute("svc data enable")

    override fun disableMobileData(): Result<*> = suAdapter.execute("svc data disable")

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

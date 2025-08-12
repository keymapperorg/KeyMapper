package io.github.sds100.keymapper.system.network

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import okio.use
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNetworkAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val suAdapter: SuAdapter,
) : NetworkAdapter {
    private val ctx = context.applicationContext
    private val wifiManager: WifiManager by lazy { ctx.getSystemService()!! }
    private val telephonyManager: TelephonyManager by lazy { ctx.getSystemService()!! }
    private val connectivityManager: ConnectivityManager by lazy { ctx.getSystemService()!! }

    private val httpClient: OkHttpClient by lazy { OkHttpClient() }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action ?: return

            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)

                    isWifiEnabled.update { state == WifiManager.WIFI_STATE_ENABLED }
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
    override val isWifiConnected: MutableStateFlow<Boolean> = MutableStateFlow(getIsWifiConnected())

    private val isWifiEnabled = MutableStateFlow(isWifiEnabled())

    private val networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isWifiConnected.update { getIsWifiConnected() }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // A network was lost. Check if we are still connected to *any* Wi-Fi.
                // This is important because onLost is called for a specific network.
                // If multiple Wi-Fi networks were available and one is lost,
                // another might still be active.
                isWifiConnected.update { getIsWifiConnected() }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                isWifiConnected.update { getIsWifiConnected() }
            }
        }

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

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    override fun isWifiEnabledFlow(): Flow<Boolean> = isWifiEnabled

    override fun enableWifi(): KMResult<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return suAdapter.execute("svc wifi enable")
        } else {
            wifiManager.isWifiEnabled = true
            return Success(Unit)
        }
    }

    override fun disableWifi(): KMResult<*> {
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

    override fun enableMobileData(): KMResult<*> = suAdapter.execute("svc data enable")

    override fun disableMobileData(): KMResult<*> = suAdapter.execute("svc data disable")

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

    override suspend fun sendHttpRequest(
        method: HttpMethod,
        url: String,
        body: String,
        authorizationHeader: String,
    ): KMResult<*> {
        try {
            val requestBody = when (method) {
                HttpMethod.HEAD -> Request.Builder().head()
                HttpMethod.PUT -> Request.Builder().put(body.toRequestBody())
                HttpMethod.POST -> Request.Builder().post(body.toRequestBody())
                HttpMethod.GET -> Request.Builder().get()
                HttpMethod.DELETE -> Request.Builder().delete()
                HttpMethod.PATCH -> Request.Builder().patch(body.toRequestBody())
            }

            val headers = Headers.Builder()

            if (authorizationHeader.isNotBlank()) {
                headers.add("Authorization", authorizationHeader)
            }

            val request = requestBody
                .url(url)
                .headers(headers.build())
                .build()

            withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                .use { response ->
                    // Keep this in. It is useful for debugging.
                    Timber.d(response.toString())

                    if (!response.isSuccessful) {
                        return KMError.UnknownIOError
                    }

                    return Success(Unit)
                }
        } catch (e: IOException) {
            Timber.e(e)
            return KMError.UnknownIOError
        } catch (e: IllegalArgumentException) {
            return KMError.MalformedUrl
        }
    }

    override fun connectWifiNetwork() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Failed to start Wi-Fi settings activity")
        }
    }

    private fun getIsWifiConnected(): Boolean { // Add this to your NetworkAdapter interface too
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    fun invalidateState() {
        connectedWifiSSIDFlow.update { connectedWifiSSID }
        isWifiConnected.update { getIsWifiConnected() }
    }
}

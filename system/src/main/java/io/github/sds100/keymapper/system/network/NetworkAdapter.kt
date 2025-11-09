package io.github.sds100.keymapper.system.network

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface NetworkAdapter {
    val connectedWifiSSIDFlow: Flow<String?>
    val isWifiConnected: Flow<Boolean>

    fun isWifiEnabled(): Boolean
    fun isWifiEnabledFlow(): Flow<Boolean>

    fun enableWifi(): KMResult<*>
    fun disableWifi(): KMResult<*>
    fun connectWifiNetwork()

    fun isMobileDataEnabled(): Boolean

    fun enableMobileData(): KMResult<*>
    fun disableMobileData(): KMResult<*>

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun isHotspotEnabled(): KMResult<Boolean>

    @RequiresApi(Build.VERSION_CODES.R)
    fun enableHotspot(): KMResult<*>

    @RequiresApi(Build.VERSION_CODES.R)
    fun disableHotspot(): KMResult<*>

    fun getKnownWifiSSIDs(): List<String>

    suspend fun sendHttpRequest(
        method: HttpMethod,
        url: String,
        body: String,
        authorizationHeader: String,
    ): KMResult<*>
}

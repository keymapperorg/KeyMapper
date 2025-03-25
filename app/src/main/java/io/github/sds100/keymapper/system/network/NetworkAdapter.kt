package io.github.sds100.keymapper.system.network

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 24/04/2021.
 */
interface NetworkAdapter {
    val connectedWifiSSID: String?
    val connectedWifiSSIDFlow: Flow<String?>

    fun isWifiEnabled(): Boolean
    fun isWifiEnabledFlow(): Flow<Boolean>

    fun enableWifi(): Result<*>
    fun disableWifi(): Result<*>

    fun isMobileDataEnabled(): Boolean

    fun enableMobileData(): Result<*>
    fun disableMobileData(): Result<*>

    fun getKnownWifiSSIDs(): List<String>?
}

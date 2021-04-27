package io.github.sds100.keymapper.system.network

import io.github.sds100.keymapper.util.Result

/**
 * Created by sds100 on 24/04/2021.
 */
interface NetworkAdapter {
    fun isWifiEnabled(): Boolean

    fun enableWifi(): Result<*>
    fun disableWifi(): Result<*>

    fun isMobileDataEnabled(): Boolean

    fun enableMobileData(): Result<*>
    fun disableMobileData(): Result<*>
}
package io.github.sds100.keymapper.system.airplanemode

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.system.root.SuAdapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAirplaneModeAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val suAdapter: SuAdapter
) : AirplaneModeAdapter {

    override suspend fun enable(): KMResult<*> {
        return if (Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            systemBridgeConnectionManager.run { bridge -> bridge.setAirplaneMode(true) }
        } else {
            val success = SettingsUtils.putGlobalSetting(ctx, Settings.Global.AIRPLANE_MODE_ON, 1)
            broadcastAirplaneModeChanged(true)
            if (success) {
                Success(Unit)
            } else {
                KMError.FailedToModifySystemSetting(Settings.Global.AIRPLANE_MODE_ON)
            }
        }
    }

    override suspend fun disable(): KMResult<*> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemBridgeConnectionManager.run { bridge -> bridge.setAirplaneMode(false) }
        } else {
            val success = SettingsUtils.putGlobalSetting(ctx, Settings.Global.AIRPLANE_MODE_ON, 0)
            if (success) {
                broadcastAirplaneModeChanged(false)
                Success(Unit)
            } else {
                KMError.FailedToModifySystemSetting(Settings.Global.AIRPLANE_MODE_ON)
            }
        }
    }

    override fun isEnabled(): Boolean =
        SettingsUtils.getGlobalSetting<Int>(ctx, Settings.Global.AIRPLANE_MODE_ON) == 1

    private fun broadcastAirplaneModeChanged(enabled: Boolean) {
        suAdapter.execute("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }
}

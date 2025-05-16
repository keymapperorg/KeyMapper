package io.github.sds100.keymapper.system.airplanemode

import android.content.Context
import android.provider.Settings
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.onSuccess
import io.github.sds100.keymapper.system.SettingsUtils
import io.github.sds100.keymapper.system.root.SuAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAirplaneModeAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    val suAdapter: SuAdapter,
) : AirplaneModeAdapter {
    private val ctx = context.applicationContext

    override fun enable(): Result<*> =
        suAdapter.execute("settings put global airplane_mode_on 1").onSuccess {
            broadcastAirplaneModeChanged(false)
        }

    override fun disable(): Result<*> =
        suAdapter.execute("settings put global airplane_mode_on 0").onSuccess {
            broadcastAirplaneModeChanged(false)
        }

    override fun isEnabled(): Boolean =
        SettingsUtils.getGlobalSetting<Int>(ctx, Settings.Global.AIRPLANE_MODE_ON) == 1

    private fun broadcastAirplaneModeChanged(enabled: Boolean) {
        suAdapter.execute("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }
}

package io.github.sds100.keymapper.system.airplanemode

import android.content.Context
import android.provider.Settings
import io.github.sds100.keymapper.system.SettingsUtils
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.onSuccess

/**
 * Created by sds100 on 24/04/2021.
 */
class AndroidAirplaneModeAdapter(
    context: Context,
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

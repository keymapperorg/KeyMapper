package io.github.sds100.keymapper.system.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidPowerAdapter(context: Context) : PowerAdapter {
    private val ctx: Context = context.applicationContext
    private val batteryManager: BatteryManager by lazy { ctx.getSystemService()!! }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _isCharging.value = true
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    _isCharging.value = false
                }
            }
        }
    }

    private val _isCharging: MutableStateFlow<Boolean> = MutableStateFlow(getIsCharging())
    override val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    init {
        ctx.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
        )
    }

    private fun getIsCharging(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        batteryManager.isCharging
    } else {
        // no other way to synchronously get the information
        false
    }
}

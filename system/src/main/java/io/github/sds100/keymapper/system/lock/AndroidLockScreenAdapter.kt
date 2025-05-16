package io.github.sds100.keymapper.system.lock

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by sds100 on 22/04/2021.
 */
class AndroidLockScreenAdapter(context: Context) : LockScreenAdapter {
    private val ctx = context.applicationContext

    private val devicePolicyManager: DevicePolicyManager by lazy { ctx.getSystemService()!! }
    private val keyguardManager: KeyguardManager by lazy { ctx.getSystemService()!! }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT, Intent.ACTION_USER_UNLOCKED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        isLockedFlow.update {
                            isLocked()
                        }
                    }

                    isLockscreenShowingFlow.update { isLockScreenShowing() }
                }
            }
        }
    }

    private val isLockedFlow by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            MutableStateFlow(isLocked())
        } else {
            MutableStateFlow(false)
        }
    }

    private val isLockscreenShowingFlow = MutableStateFlow(isLockScreenShowing())

    init {
        // There is no broadcast or callback for when the screen is locked for user apps.
        // There is a way in Tiramisu but the permission SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE
        // is only granted to system apps. So monitor whether the screen is turned on instead.
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filter.addAction(Intent.ACTION_USER_UNLOCKED)
        }

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun isLockedFlow(): Flow<Boolean> = isLockedFlow

    override fun secureLockDevice(): Result<*> {
        devicePolicyManager.lockNow()
        return Success(Unit)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun isLocked(): Boolean = keyguardManager.isDeviceLocked

    override fun isLockScreenShowing(): Boolean = keyguardManager.isKeyguardLocked
    override fun isLockScreenShowingFlow(): Flow<Boolean> = isLockscreenShowingFlow
}

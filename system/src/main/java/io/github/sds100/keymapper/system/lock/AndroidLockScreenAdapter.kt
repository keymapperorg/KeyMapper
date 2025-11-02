package io.github.sds100.keymapper.system.lock

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AndroidLockScreenAdapter @Inject constructor(@ApplicationContext private val ctx: Context) :
    LockScreenAdapter {

    private val devicePolicyManager: DevicePolicyManager by lazy { ctx.getSystemService()!! }
    private val keyguardManager: KeyguardManager by lazy { ctx.getSystemService()!! }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_USER_UNLOCKED,
                    -> {
                    isLockedFlow.update {
                        isLocked()
                    }

                    isLockscreenShowingFlow.update { isLockScreenShowing() }
                }
            }
        }
    }

    private val isLockedFlow by lazy { MutableStateFlow(isLocked()) }

    private val isLockscreenShowingFlow = MutableStateFlow(isLockScreenShowing())

    init {
        // There is no broadcast or callback for when the screen is locked for user apps.
        // There is a way in Tiramisu but the permission SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE
        // is only granted to system apps. So monitor whether the screen is turned on instead.
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_USER_UNLOCKED)

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun isLockedFlow(): Flow<Boolean> = isLockedFlow

    override fun secureLockDevice(): KMResult<*> {
        devicePolicyManager.lockNow()
        return Success(Unit)
    }

    override fun isLocked(): Boolean = keyguardManager.isDeviceLocked

    override fun isLockScreenShowing(): Boolean = keyguardManager.isKeyguardLocked
    override fun isLockScreenShowingFlow(): Flow<Boolean> = isLockscreenShowingFlow
}

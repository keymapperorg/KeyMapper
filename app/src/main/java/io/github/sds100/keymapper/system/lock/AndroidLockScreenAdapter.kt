package io.github.sds100.keymapper.system.lock

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success

/**
 * Created by sds100 on 22/04/2021.
 */
class AndroidLockScreenAdapter(context: Context) : LockScreenAdapter {
    private val ctx = context.applicationContext

    private val devicePolicyManager: DevicePolicyManager by lazy { ctx.getSystemService()!! }
    private val keyguardManager: KeyguardManager by lazy { ctx.getSystemService()!! }

    override fun secureLockDevice(): Result<*> {
        devicePolicyManager.lockNow()
        return Success(Unit)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun isLocked(): Boolean = keyguardManager.isDeviceLocked
}

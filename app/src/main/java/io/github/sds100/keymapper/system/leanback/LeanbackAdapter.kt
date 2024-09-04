package io.github.sds100.keymapper.system.leanback

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.getSystemService

/**
 * Created by sds100 on 21/07/2021.
 */

class LeanbackAdapterImpl(context: Context) : LeanbackAdapter {
    private val ctx = context.applicationContext

    override fun isTvDevice(): Boolean {
        val uiModeManager = ctx.getSystemService<UiModeManager>() ?: return false

        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

interface LeanbackAdapter {
    fun isTvDevice(): Boolean
}

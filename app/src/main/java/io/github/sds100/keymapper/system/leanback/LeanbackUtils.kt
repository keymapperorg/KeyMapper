package io.github.sds100.keymapper.system.leanback

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.getSystemService

/**
 * Created by sds100 on 17/06/2021.
 */
object LeanbackUtils {
    fun isTvDevice(ctx: Context): Boolean {
        val uiModeManager = ctx.getSystemService<UiModeManager>() ?: return false

        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

package io.github.sds100.keymapper.util

import android.content.Context

/**
 * Created by sds100 on 18/07/2019.
 */

object DexUtils {
    fun isDexSupported(ctx: Context): Boolean {
        val config = ctx.resources.configuration

        try {
            val configClass = config.javaClass

            if (configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                == configClass.getField("semDesktopModeEnabled").getInt(config)) {

                return true
            }

        } catch (e: NoSuchFieldException) {
        } catch (e: IllegalAccessException) {
        } catch (e: IllegalArgumentException) {
        }

        return false
    }
}
package io.github.sds100.keymapper.util

import android.content.Context

/**
 * Created by sds100 on 18/07/2019.
 */

object DexUtils {
    fun Context.isDexSupported(): Boolean {
        val config = resources.configuration

        try {
            val configClass = config.javaClass

            if (configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                    == configClass.getField("semDesktopModeEnabled").getInt(config)) {

                Logger.write(this, "Samsung Dex is supported", "This device supports Samsung Dex mode. This is" +
                        "not an issue")

                return true
            }

        } catch (e: NoSuchFieldException) {
            Logger.write(this, "Samsung Dex isn't supported", "This device doesn't support Samsung Dex mode. This is" +
                    "not an issue")
        } catch (e: IllegalAccessException) {
        } catch (e: IllegalArgumentException) {
        }

        return false
    }
}
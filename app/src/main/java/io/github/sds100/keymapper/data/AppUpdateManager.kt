package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.intPreferencesKey
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 18/01/21.
 */

class AppUpdateManager(private val globalPreferences: IGlobalPreferences) {

    suspend fun getLastVersionCodeHomeScreen() =
        globalPreferences.get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_HOME_SCREEN) ?: -1

    suspend fun getLastVersionCodeAccessibilityService() =
        globalPreferences.get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE)
            ?: -1

    fun handledAppUpdateOnHomeScreen() {
        globalPreferences.set(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_HOME_SCREEN, Constants.VERSION_CODE)
    }

    fun handledAppUpdateInAccessibilityService() {
        globalPreferences.set(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE,
            Constants.VERSION_CODE)
    }

    private object PreferenceKeys {
        val LAST_INSTALLED_VERSION_CODE_HOME_SCREEN =
            intPreferencesKey("last_installed_version_home_screen")

        val LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE =
            intPreferencesKey("last_installed_version_accessibility_service")
    }
}
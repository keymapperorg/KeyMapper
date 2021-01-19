package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.preferencesKey
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 18/01/21.
 */

class AppUpdateManager(private val dataStoreManager: IDataStoreManager) : IDataStoreManager by dataStoreManager {

    suspend fun getLastVersionCodeHomeScreen() =
        dataStoreManager.get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_HOME_SCREEN) ?: -1

    suspend fun getLastVersionCodeAccessibilityService() =
        dataStoreManager.get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE) ?: -1

    suspend fun handledAppUpdateOnHomeScreen() {
        set(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_HOME_SCREEN, Constants.VERSION_CODE)
    }

    suspend fun handledAppUpdateInAccessibilityService() {
        set(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE,
            Constants.VERSION_CODE)
    }

    private object PreferenceKeys {
        val LAST_INSTALLED_VERSION_CODE_HOME_SCREEN =
            preferencesKey<Int>("last_installed_version_home_screen")

        val LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE =
            preferencesKey<Int>("last_installed_version_accessibility_service")
    }
}
package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.preferencesKey
import androidx.lifecycle.asLiveData
import io.github.sds100.keymapper.Constants
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 18/01/21.
 */

class AppUpdateManager(dataStore: IPreferenceDataStore) : IPreferenceDataStore by dataStore {

    val lastVersionCodeHomeScreen =
        get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_HOME_SCREEN)
            .map { it ?: -1 }
            .asLiveData()

    val lastVersionCodeAccessibilityService =
        get(PreferenceKeys.LAST_INSTALLED_VERSION_CODE_ACCESSIBILITY_SERVICE)
            .map { it ?: -1 }
            .asLiveData()

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
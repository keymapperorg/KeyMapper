package io.github.sds100.keymapper.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.remove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 19/01/21.
 */

class GlobalPreferences(private val dataStore: DataStore<Preferences>,
                        private val coroutineScope: CoroutineScope) : IGlobalPreferences {

    override fun <T> getFlow(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map { it[key] }
    }

    override suspend fun <T> get(key: Preferences.Key<T>): T? {
        return dataStore.data.first()[key]
    }

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
        }
    }

    override fun toggle(key: Preferences.Key<Boolean>) {
        coroutineScope.launch {
            dataStore.edit {
                val oldValue = it[key] ?: false

                it[key] = !oldValue
            }
        }
    }
}

val IGlobalPreferences.darkThemeMode
    get() = getFlow(Keys.darkTheme).map {
        when (it) {
            "0" -> AppCompatDelegate.MODE_NIGHT_YES
            "1" -> AppCompatDelegate.MODE_NIGHT_NO
            "2" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

val IGlobalPreferences.hasRootPermission
    get() = getFlow(Keys.hasRootPermission).map {
        it ?: false
    }

val IGlobalPreferences.automaticBackupLocation
    get() = getFlow(Keys.automaticBackupLocation).map {
        it ?: ""
    }

val IGlobalPreferences.shownAppIntro
    get() = getFlow(Keys.shownAppIntro).map {
        it ?: false
    }

val IGlobalPreferences.showGuiKeyboardAd
    get() = getFlow(Keys.showGuiKeyboardAd).map {
        it ?: false
    }

val IGlobalPreferences.showDeviceDescriptors
    get() = getFlow(Keys.showDeviceDescriptors).map {
        it ?: false
    }

val IGlobalPreferences.keymapsPaused
    get() = getFlow(Keys.keymapsPaused).map {
        it ?: false
    }

val IGlobalPreferences.showImePickerNotification
    get() = getFlow(Keys.showImePickerNotification).map {
        it ?: false
    }

val IGlobalPreferences.showToggleKeyboardNotification
    get() = getFlow(Keys.showToggleKeyboardNotification).map {
        it ?: false
    }

val IGlobalPreferences.showToggleKeymapsNotification
    get() = getFlow(Keys.showToggleKeymapsNotification).map {
        it ?: false
    }

val IGlobalPreferences.approvedFingerprintFeaturePrompt
    get() = getFlow(Keys.approvedFingerprintFeaturePrompt).map {
        it ?: false
    }

val IGlobalPreferences.longPressDelay
    get() = getFlow(Keys.longPressDelay).map {
        it ?: PreferenceDefaults.LONG_PRESS_DELAY
    }

val IGlobalPreferences.doublePressDelay
    get() = getFlow(Keys.doublePressDelay).map {
        it ?: PreferenceDefaults.DOUBLE_PRESS_DELAY
    }

val IGlobalPreferences.vibrationDuration
    get() = getFlow(Keys.vibrateDuration).map {
        it ?: PreferenceDefaults.VIBRATION_DURATION
    }

val IGlobalPreferences.repeatDelay
    get() = getFlow(Keys.repeatDelay).map {
        it ?: PreferenceDefaults.REPEAT_DELAY
    }

val IGlobalPreferences.repeatRate
    get() = getFlow(Keys.repeatRate).map {
        it ?: PreferenceDefaults.REPEAT_RATE
    }

val IGlobalPreferences.sequenceTriggerTimeout
    get() = getFlow(Keys.sequenceTriggerTimeout).map {
        it ?: PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT
    }

val IGlobalPreferences.holdDownDuration
    get() = getFlow(Keys.holdDownDuration).map {
        it ?: PreferenceDefaults.HOLD_DOWN_DURATION
    }
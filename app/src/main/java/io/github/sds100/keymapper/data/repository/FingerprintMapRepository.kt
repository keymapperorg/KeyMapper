package io.github.sds100.keymapper.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.remove
import androidx.lifecycle.asLiveData
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.util.FingerprintMapUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 17/11/20.
 */
class FingerprintMapRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val PREF_KEYS = mapOf(
            FingerprintMapUtils.SWIPE_DOWN to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN,
            FingerprintMapUtils.SWIPE_UP to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP,
            FingerprintMapUtils.SWIPE_LEFT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT,
            FingerprintMapUtils.SWIPE_RIGHT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT
        )
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(FingerprintMap.DESERIALIZER)
        .registerTypeAdapter(Action.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER)
        .registerTypeAdapter(Constraint.DESERIALIZER).create()

    val swipeDown: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN)
    }

    val swipeUp: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP)
    }

    val swipeLeft: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT)
    }

    val swipeRight: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT)
    }

    val fingerprintGestureMapsLiveData = combine(
        swipeDown,
        swipeUp,
        swipeLeft,
        swipeRight
    ) { swipeDown, swipeUp, swipeLeft, swipeRight ->
        mapOf(
            FingerprintMapUtils.SWIPE_DOWN to swipeDown,
            FingerprintMapUtils.SWIPE_UP to swipeUp,
            FingerprintMapUtils.SWIPE_LEFT to swipeLeft,
            FingerprintMapUtils.SWIPE_RIGHT to swipeRight
        )
    }.asLiveData()

    val fingerprintGesturesAvailable = dataStore.data.map {
        it[PreferenceKeys.FINGERPRINT_GESTURES_AVAILABLE]
    }

    suspend fun setFingerprintGesturesAvailable(available: Boolean) {
        dataStore.edit {
            it[PreferenceKeys.FINGERPRINT_GESTURES_AVAILABLE] = available
        }
    }

    suspend fun editGesture(
        gestureId: String,
        block: (old: FingerprintMap) -> FingerprintMap
    ) {
        dataStore.edit { prefs ->
            val key = PREF_KEYS[gestureId]!!
            val new = block.invoke(prefs.getGesture(key))

            prefs[key] = gson.toJson(new)
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            PreferenceKeys.ALL_SWIPE_KEYS.forEach {
                prefs.remove(it)
            }
        }
    }

    private fun Preferences.getGesture(key: Preferences.Key<String>): FingerprintMap {
        val json = this[key]

        return if (json == null) {
            FingerprintMap()
        } else {
            gson.fromJson(json)
        }
    }

    private object PreferenceKeys {
        val FINGERPRINT_GESTURE_SWIPE_DOWN = preferencesKey<String>("swipe_down")
        val FINGERPRINT_GESTURE_SWIPE_UP = preferencesKey<String>("swipe_up")
        val FINGERPRINT_GESTURE_SWIPE_LEFT = preferencesKey<String>("swipe_left")
        val FINGERPRINT_GESTURE_SWIPE_RIGHT = preferencesKey<String>("swipe_right")

        val ALL_SWIPE_KEYS = arrayOf(
            FINGERPRINT_GESTURE_SWIPE_DOWN,
            FINGERPRINT_GESTURE_SWIPE_LEFT,
            FINGERPRINT_GESTURE_SWIPE_UP,
            FINGERPRINT_GESTURE_SWIPE_RIGHT)

        val FINGERPRINT_GESTURES_AVAILABLE =
            preferencesKey<Boolean>("fingerprint_gestures_available")
    }
}
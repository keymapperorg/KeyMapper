package io.github.sds100.keymapper.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.clear
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.asLiveData
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.DataStoreKeys
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
class FingerprintMapRepository constructor(private val mDataStore: DataStore<Preferences>) {

    private val mGson = GsonBuilder()
        .registerTypeAdapter(FingerprintMap.DESERIALIZER)
        .registerTypeAdapter(Action.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER)
        .registerTypeAdapter(Constraint.DESERIALIZER).create()

    val swipeDown: Flow<FingerprintMap> = mDataStore.data.map { prefs ->
        prefs.getGesture(DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_DOWN)
    }

    val swipeUp: Flow<FingerprintMap> = mDataStore.data.map { prefs ->
        prefs.getGesture(DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_UP)
    }

    val swipeLeft: Flow<FingerprintMap> = mDataStore.data.map { prefs ->
        prefs.getGesture(DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_LEFT)
    }

    val swipeRight: Flow<FingerprintMap> = mDataStore.data.map { prefs ->
        prefs.getGesture(DataStoreKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT)
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

    suspend fun editGesture(
        gestureId: String,
        block: (old: FingerprintMap) -> FingerprintMap
    ) {
        mDataStore.edit { prefs ->
            val key = FingerprintMapUtils.PREF_KEYS[gestureId]!!
            val new = block.invoke(prefs.getGesture(key))

            prefs[key] = mGson.toJson(new)
        }
    }

    suspend fun reset() {
        mDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun Preferences.getGesture(key: Preferences.Key<String>): FingerprintMap {
        val json = this[key]

        return if (json == null) {
            FingerprintMap()
        } else {
            mGson.fromJson(json)
        }
    }
}
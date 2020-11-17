package io.github.sds100.keymapper.data.repository

import androidx.annotation.StringRes
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.util.FingerprintGestureUtils

/**
 * Created by sds100 on 17/11/20.
 */
class FingerprintGestureRepository
constructor(iPreferenceDataStore: IPreferenceDataStore) : IPreferenceDataStore by iPreferenceDataStore {

    private val mGson = GsonBuilder()
        .registerTypeAdapter(Action.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER).create()

    fun editFingerprintMap(
        gestureId: String,
        block: (old: FingerprintGestureMap) -> FingerprintGestureMap
    ) {
        val fingerprintGestureMap = retrieveFingerprintMap(FingerprintGestureUtils.PREF_KEYS[gestureId]!!)
            ?: FingerprintGestureMap()

        fingerprintGestureMap.apply {
            val prefKey = FingerprintGestureUtils.PREF_KEYS[gestureId]!!

            saveFingerprintMap(prefKey, block.invoke(fingerprintGestureMap))
        }
    }

    fun saveFingerprintMap(@StringRes prefKey: Int, gestureMap: FingerprintGestureMap) {
        val json = mGson.toJson(gestureMap)
        setStringPref(prefKey, json)
    }

    fun retrieveFingerprintMap(@StringRes prefKey: Int): FingerprintGestureMap? {
        val json = getStringPref(prefKey)
        return json?.let { mGson.fromJson(it) }
    }
}
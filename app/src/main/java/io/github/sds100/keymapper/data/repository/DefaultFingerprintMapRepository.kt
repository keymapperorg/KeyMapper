package io.github.sds100.keymapper.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.remove
import androidx.lifecycle.MutableLiveData
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.db.migration.JsonMigration
import io.github.sds100.keymapper.data.db.migration.fingerprintmaps.Migration_0_1
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.util.FingerprintMapUtils
import io.github.sds100.keymapper.util.MigrationUtils
import io.github.sds100.keymapper.util.RequestBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 17/11/20.
 */
class DefaultFingerprintMapRepository(private val dataStore: DataStore<Preferences>,
                                      private val coroutineScope: CoroutineScope
) : FingerprintMapRepository {

    companion object {
        val PREF_KEYS_MAP = mapOf(
            FingerprintMapUtils.SWIPE_DOWN to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN,
            FingerprintMapUtils.SWIPE_UP to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP,
            FingerprintMapUtils.SWIPE_LEFT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT,
            FingerprintMapUtils.SWIPE_RIGHT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT
        )

        private val MIGRATIONS = listOf(
            JsonMigration(0, 1) { gson, json -> Migration_0_1.migrate(gson, json) }
        )
    }

    override val requestBackup = MutableLiveData<RequestBackup<Map<String, FingerprintMap>>>()

    private val gson = GsonBuilder()
        .registerTypeAdapter(FingerprintMap.DESERIALIZER)
        .registerTypeAdapter(Action.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER)
        .registerTypeAdapter(Constraint.DESERIALIZER).create()

    private val jsonParser = JsonParser()

    override val swipeDown: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN)
    }.dropWhile { it.version < FingerprintMap.CURRENT_VERSION }

    override val swipeUp: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP)
    }.dropWhile { it.version < FingerprintMap.CURRENT_VERSION }

    override val swipeLeft: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT)
    }.dropWhile { it.version < FingerprintMap.CURRENT_VERSION }

    override val swipeRight: Flow<FingerprintMap> = dataStore.data.map { prefs ->
        prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT)
    }.dropWhile { it.version < FingerprintMap.CURRENT_VERSION }

    override val fingerprintGestureMaps = combine(
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
    }

    override val fingerprintGesturesAvailable = dataStore.data.map {
        it[PreferenceKeys.FINGERPRINT_GESTURES_AVAILABLE]
    }

    override fun setFingerprintGesturesAvailable(available: Boolean) {
        coroutineScope.launch {
            dataStore.edit {
                it[PreferenceKeys.FINGERPRINT_GESTURES_AVAILABLE] = available
            }
        }
    }

    override fun restore(
        gestureId: String,
        fingerprintMapJson: String
    ) {
        coroutineScope.launch {
            val rootElement = jsonParser.parse(fingerprintMapJson)
            val initialVersion = rootElement.asJsonObject.get(FingerprintMap.NAME_VERSION).nullInt
                ?: 0

            val migratedJson = MigrationUtils.migrate(
                gson,
                MIGRATIONS,
                initialVersion,
                fingerprintMapJson,
                FingerprintMap.CURRENT_VERSION
            )

            setGesture(gestureId, migratedJson)
        }
    }

    override fun updateGesture(
        gestureId: String,
        block: (old: FingerprintMap) -> FingerprintMap
    ) {
        coroutineScope.launch {
            dataStore.edit { prefs ->
                val key = PREF_KEYS_MAP[gestureId]!!
                val new = block.invoke(prefs.getGesture(key))

                prefs[key] = gson.toJson(new)
            }

            requestBackup()
        }
    }

    override fun reset() {
        coroutineScope.launch {
            dataStore.edit { prefs ->
                PreferenceKeys.ALL_SWIPE_KEYS.forEach {
                    prefs.remove(it)
                }
            }
        }
    }

    private suspend fun setGesture(gestureId: String,
                                   fingerprintMapJson: String) {
        dataStore.edit { prefs ->
            val key = PREF_KEYS_MAP[gestureId]!!
            prefs[key] = fingerprintMapJson
        }

        requestBackup()
    }

    private suspend fun requestBackup() {
        val maps = PREF_KEYS_MAP.mapValues { (_, preferenceKey) ->
            dataStore.data.firstOrNull()?.getGesture(preferenceKey) ?: FingerprintMap()
        }

        //don't back up if they haven't been migrated
        if (maps.any { it.value.version < FingerprintMap.CURRENT_VERSION }) return

        requestBackup.value = RequestBackup(maps)
    }

    private suspend fun Preferences.getGesture(key: Preferences.Key<String>): FingerprintMap {
        val json = this[key] ?: return FingerprintMap()

        val rootElement = jsonParser.parse(json)
        val initialVersion = rootElement.asJsonObject.get(FingerprintMap.NAME_VERSION).nullInt ?: 0

        val migratedJson = MigrationUtils.migrate(
            gson,
            MIGRATIONS,
            initialVersion,
            json,
            FingerprintMap.CURRENT_VERSION
        )

        if (initialVersion < FingerprintMap.CURRENT_VERSION) {
            setGesture(key.name, migratedJson)
        }

        return gson.fromJson(migratedJson)
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
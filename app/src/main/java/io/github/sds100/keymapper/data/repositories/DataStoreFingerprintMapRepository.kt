package io.github.sds100.keymapper.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.data.migration.fingerprintmaps.Migration_0_1
import io.github.sds100.keymapper.data.migration.fingerprintmaps.Migration_1_2
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapEntityGroup
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 17/11/20.
 */
class DataStoreFingerprintMapRepository(
    private val dataStore: DataStore<Preferences>,
    private val coroutineScope: CoroutineScope
) : FingerprintMapRepository {

    companion object {
        val PREF_KEYS_MAP = mapOf(
            FingerprintMapEntity.ID_SWIPE_DOWN to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN,
            FingerprintMapEntity.ID_SWIPE_UP to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP,
            FingerprintMapEntity.ID_SWIPE_LEFT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT,
            FingerprintMapEntity.ID_SWIPE_RIGHT to PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT
        )

        val MIGRATIONS = listOf(
            JsonMigration(0, 1) { gson, json -> Migration_0_1.migrate(gson, json) },
            JsonMigration(1, 2) { gson, json -> Migration_1_2.migrate(gson, json) }
        )
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(FingerprintMapEntity.DESERIALIZER)
        .registerTypeAdapter(ActionEntity.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER)
        .registerTypeAdapter(ConstraintEntity.DESERIALIZER).create()

    private val jsonParser = JsonParser()

    override val fingerprintMaps: Flow<FingerprintMapEntityGroup> = dataStore.data.map { prefs ->
        val group = FingerprintMapEntityGroup(
            swipeDown = prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_DOWN),
            swipeUp = prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_UP),
            swipeLeft = prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_LEFT),
            swipeRight = prefs.getGesture(PreferenceKeys.FINGERPRINT_GESTURE_SWIPE_RIGHT),
        )

        group
    }.dropWhile { group ->
        group.swipeDown.version < FingerprintMapEntity.CURRENT_VERSION
            || group.swipeUp.version < FingerprintMapEntity.CURRENT_VERSION
            || group.swipeLeft.version < FingerprintMapEntity.CURRENT_VERSION
            || group.swipeRight.version < FingerprintMapEntity.CURRENT_VERSION
    }

    override val requestBackup = MutableSharedFlow<FingerprintMapEntityGroup>()

    override suspend fun restore(id: String, fingerprintMapJson: String) {
        val rootElement = jsonParser.parse(fingerprintMapJson)
        val initialVersion =
            rootElement.asJsonObject.get(FingerprintMapEntity.NAME_VERSION).nullInt
                ?: 0

        val migratedJson = MigrationUtils.migrate(
            gson,
            MIGRATIONS,
            initialVersion,
            fingerprintMapJson,
            FingerprintMapEntity.CURRENT_VERSION
        )

        //deserialize it to ensure that the json is valid
        val fingerprintMap = gson.fromJson<FingerprintMapEntity>(migratedJson)

        updateSuspend(id, fingerprintMap)
    }

    override fun update(id: String, fingerprintMap: FingerprintMapEntity) {
        coroutineScope.launch {
            updateSuspend(id, fingerprintMap)

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

            requestBackup()
        }
    }

    override fun enableFingerprintMap(id: String) {
        coroutineScope.launch {
            val fingerprintMap = fingerprintMaps.firstOrNull()?.get(id) ?: return@launch
            updateSuspend(id, fingerprintMap.copy(isEnabled = true))

            requestBackup()
        }
    }

    override fun disableFingerprintMap(id: String) {
        coroutineScope.launch {
            val fingerprintMap = fingerprintMaps.firstOrNull()?.get(id) ?: return@launch
            updateSuspend(id, fingerprintMap.copy(isEnabled = false))

            requestBackup()
        }
    }

    private suspend fun updateSuspend(id: String, fingerprintMap: FingerprintMapEntity) {
        dataStore.edit { prefs ->
            val key = PREF_KEYS_MAP[id]!!

            prefs[key] = gson.toJson(fingerprintMap)
        }
    }

    private suspend fun requestBackup() {
        val maps = fingerprintMaps.first()
        requestBackup.emit(maps)
    }

    private suspend fun Preferences.getGesture(key: Preferences.Key<String>): FingerprintMapEntity {
        val json = this[key] ?: return FingerprintMapEntity()

        val rootElement = jsonParser.parse(json)
        val initialVersion =
            rootElement.asJsonObject.get(FingerprintMapEntity.NAME_VERSION).nullInt ?: 0

        val migratedJson = MigrationUtils.migrate(
            gson,
            MIGRATIONS,
            initialVersion,
            json,
            FingerprintMapEntity.CURRENT_VERSION
        )

        if (initialVersion < FingerprintMapEntity.CURRENT_VERSION) {
            //deserialize it to ensure that the json is valid
            val fingerprintMap = gson.fromJson<FingerprintMapEntity>(migratedJson)
            updateSuspend(key.name, fingerprintMap)
        }

        return gson.fromJson(migratedJson)
    }

    private fun FingerprintMapEntityGroup.get(id: String): FingerprintMapEntity {
        return when (id) {
            FingerprintMapEntity.ID_SWIPE_DOWN -> swipeDown
            FingerprintMapEntity.ID_SWIPE_UP -> swipeUp
            FingerprintMapEntity.ID_SWIPE_LEFT -> swipeLeft
            FingerprintMapEntity.ID_SWIPE_RIGHT -> swipeRight
            else -> throw IllegalArgumentException("Don't know how to get fingerprint map for id $id")
        }
    }

    private object PreferenceKeys {
        val FINGERPRINT_GESTURE_SWIPE_DOWN = stringPreferencesKey("swipe_down")
        val FINGERPRINT_GESTURE_SWIPE_UP = stringPreferencesKey("swipe_up")
        val FINGERPRINT_GESTURE_SWIPE_LEFT = stringPreferencesKey("swipe_left")
        val FINGERPRINT_GESTURE_SWIPE_RIGHT = stringPreferencesKey("swipe_right")

        val ALL_SWIPE_KEYS = arrayOf(
            FINGERPRINT_GESTURE_SWIPE_DOWN,
            FINGERPRINT_GESTURE_SWIPE_LEFT,
            FINGERPRINT_GESTURE_SWIPE_UP,
            FINGERPRINT_GESTURE_SWIPE_RIGHT
        )
    }
}
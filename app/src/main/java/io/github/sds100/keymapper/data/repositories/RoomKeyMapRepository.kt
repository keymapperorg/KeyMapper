package io.github.sds100.keymapper.data.repositories

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.migration.JsonMigration
import io.github.sds100.keymapper.data.migration.keymaps.Migration_9_10
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.data.migration.MigrationUtils
import io.github.sds100.keymapper.data.migration.keymaps.Migration_10_11
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 18/03/2021.
 */
class RoomKeyMapRepository(
    private val dao: KeyMapDao,
    private val coroutineScope: CoroutineScope
) : KeyMapRepository {

    companion object {
        val MIGRATIONS = listOf(
            JsonMigration(9, 10) { gson, json -> Migration_9_10.migrateJson(gson, json) },
            JsonMigration(10, 11) { gson, json -> Migration_10_11.migrateJson(gson, json) }
        )
    }

    override val keyMapList = dao.getAllFlow()
        .map { State.Data(it) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override val requestBackup = MutableSharedFlow<List<KeyMapEntity>>()

    private val gson = Gson()

    override fun insert(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch {
            dao.insert(*keyMap)
            requestBackup()
        }
    }

    override fun update(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch {
            dao.update(*keyMap)
            requestBackup()
        }
    }

    override suspend fun get(uid: String): KeyMapEntity? {
        return dao.getByUid(uid)
    }

    override fun delete(vararg uid: String) {
        coroutineScope.launch {
            dao.deleteById(*uid)
            requestBackup()
        }
    }

    override suspend fun restore(dbVersion: Int, keyMapJsonList: List<String>) {
        val migratedKeymapList = keyMapJsonList.map {
            val migratedJson = MigrationUtils.migrate(
                gson,
                MIGRATIONS,
                dbVersion,
                it,
                AppDatabase.DATABASE_VERSION
            )

            val keyMap = gson.fromJson<KeyMapEntity>(migratedJson)

            keyMap.copy(id = 0, uid = UUID.randomUUID().toString())
        }

        //use dao directly so inserting happens in the same coroutine as the backup and restore
        dao.insert(*migratedKeymapList.toTypedArray())
    }

    override fun duplicate(vararg uid: String) {
        coroutineScope.launch {
            val keymaps = mutableListOf<KeyMapEntity>()

            uid.forEach {
                val keymap = get(it) ?: return@forEach
                keymaps.add(keymap.copy(id = 0, uid = UUID.randomUUID().toString()))
            }

            dao.insert(*keymaps.toTypedArray())
            requestBackup()
        }
    }

    override fun enableById(vararg uid: String) {
        coroutineScope.launch {
            dao.enableKeymapByUid(*uid)
            requestBackup()
        }
    }

    override fun disableById(vararg uid: String) {
        coroutineScope.launch {
            dao.disableKeymapByUid(*uid)
            requestBackup()
        }
    }

    private fun requestBackup() {
        coroutineScope.launch {
            val keyMapList = keyMapList.first { it is State.Data } as State.Data
            requestBackup.emit(keyMapList.data)
        }
    }
}
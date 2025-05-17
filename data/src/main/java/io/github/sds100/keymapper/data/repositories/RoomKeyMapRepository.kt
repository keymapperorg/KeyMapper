package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.migration.fingerprintmaps.FingerprintToKeyMapMigration
import io.github.sds100.keymapper.base.keymaps.KeyMapRepository
import io.github.sds100.keymapper.base.utils.DefaultDispatcherProvider
import io.github.sds100.keymapper.base.utils.DispatcherProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.base.utils.splitIntoBatches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomKeyMapRepository(
    private val keyMapDao: KeyMapDao,
    private val fingerprintMapDao: FingerprintMapDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : KeyMapRepository {

    companion object {
        private const val MAX_KEY_MAP_BATCH_SIZE = 200
    }

    override val keyMapList = keyMapDao.getAll()
        .map { State.Data(it) }
        .flowOn(dispatchers.io())
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    init {
        coroutineScope.launch {
            migrateFingerprintMaps()
        }
    }

    override fun getAll(): Flow<List<KeyMapEntity>> {
        return keyMapDao.getAll().flowOn(dispatchers.io())
    }

    override fun getByGroup(groupUid: String?): Flow<List<KeyMapEntity>> {
        return keyMapDao.getByGroup(groupUid).flowOn(dispatchers.io())
    }

    override fun insert(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.io()) {
            for (it in keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.insert(*it)
            }
        }
    }

    override suspend fun deleteAll() {
        withContext(dispatchers.io()) {
            keyMapDao.deleteAll()
        }
    }

    override fun update(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.io()) {
            for (it in keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.update(*it)
            }
        }
    }

    override suspend fun get(uid: String): KeyMapEntity? = keyMapDao.getByUid(uid)

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            for (it in uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.deleteById(*it)
            }
        }
    }

    override fun duplicate(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            for (uidBatch in uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                val keymaps = mutableListOf<KeyMapEntity>()

                for (keyMapUid in uidBatch) {
                    val keymap = get(keyMapUid) ?: continue
                    keymaps.add(keymap.copy(id = 0, uid = UUID.randomUUID().toString()))
                }

                keyMapDao.insert(*keymaps.toTypedArray())
            }
        }
    }

    override fun enableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            for (it in uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.enableKeyMapByUid(*it)
            }
        }
    }

    override fun disableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            for (it in uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.disableKeyMapByUid(*it)
            }
        }
    }

    override fun moveToGroup(groupUid: String?, vararg uid: String) {
        coroutineScope.launch {
            for (it in uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE)) {
                keyMapDao.setKeyMapGroup(groupUid, *it)
            }
        }
    }

    override fun count(): Flow<Int> {
        return keyMapDao.count().flowOn(dispatchers.io())
    }

    private suspend fun migrateFingerprintMaps() = withContext(dispatchers.io()) {
        val entities = fingerprintMapDao.getAll().first()

        for (entity in entities) {
            val keyMapEntity = FingerprintToKeyMapMigration.migrate(entity) ?: continue
            keyMapDao.insert(keyMapEntity)

            val migratedFingerprintMapEntity =
                entity.copy(flags = entity.flags or FingerprintMapEntity.FLAG_MIGRATED_TO_KEY_MAP)
            fingerprintMapDao.update(migratedFingerprintMapEntity)
        }
    }
}

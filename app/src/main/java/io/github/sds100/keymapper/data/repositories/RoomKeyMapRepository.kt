package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.splitIntoBatches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Created by sds100 on 18/03/2021.
 */
class RoomKeyMapRepository(
    private val dao: KeyMapDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : KeyMapRepository {

    companion object {
        private const val MAX_KEY_MAP_BATCH_SIZE = 200
    }

    override val keyMapList = dao.getAll()
        .map { State.Data(it) }
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override val requestBackup = MutableSharedFlow<List<KeyMapEntity>>()

    override fun insert(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.default()) {
            keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                dao.insert(*it)
            }

            requestBackup()
        }
    }

    override fun update(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.default()) {
            keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                dao.update(*it)
            }

            requestBackup()
        }
    }

    override suspend fun get(uid: String): KeyMapEntity? = dao.getByUid(uid)

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.default()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                dao.deleteById(*it)
            }

            requestBackup()
        }
    }

    override fun duplicate(vararg uid: String) {
        coroutineScope.launch(dispatchers.default()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach { uidBatch ->
                val keymaps = mutableListOf<KeyMapEntity>()

                for (keyMapUid in uidBatch) {
                    val keymap = get(keyMapUid) ?: continue
                    keymaps.add(keymap.copy(id = 0, uid = UUID.randomUUID().toString()))
                }

                dao.insert(*keymaps.toTypedArray())
            }

            requestBackup()
        }
    }

    override fun enableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.default()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                dao.enableKeymapByUid(*it)
            }

            requestBackup()
        }
    }

    override fun disableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.default()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                dao.disableKeymapByUid(*it)
            }

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

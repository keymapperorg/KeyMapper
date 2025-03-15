package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.FingerprintTriggerKeyEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.splitIntoBatches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.bitflags.hasFlag
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

    override val requestBackup = MutableSharedFlow<List<KeyMapEntity>>()

    init {
        coroutineScope.launch {
            migrateFingerprintMaps()
        }
    }

    override fun insert(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.io()) {
            keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                keyMapDao.insert(*it)
            }

            requestBackup()
        }
    }

    override fun update(vararg keyMap: KeyMapEntity) {
        coroutineScope.launch(dispatchers.io()) {
            keyMap.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                keyMapDao.update(*it)
            }

            requestBackup()
        }
    }

    override suspend fun get(uid: String): KeyMapEntity? = keyMapDao.getByUid(uid)

    override fun delete(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                keyMapDao.deleteById(*it)
            }

            requestBackup()
        }
    }

    override fun duplicate(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach { uidBatch ->
                val keymaps = mutableListOf<KeyMapEntity>()

                for (keyMapUid in uidBatch) {
                    val keymap = get(keyMapUid) ?: continue
                    keymaps.add(keymap.copy(id = 0, uid = UUID.randomUUID().toString()))
                }

                keyMapDao.insert(*keymaps.toTypedArray())
            }

            requestBackup()
        }
    }

    override fun enableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                keyMapDao.enableKeymapByUid(*it)
            }

            requestBackup()
        }
    }

    override fun disableById(vararg uid: String) {
        coroutineScope.launch(dispatchers.io()) {
            uid.splitIntoBatches(MAX_KEY_MAP_BATCH_SIZE).forEach {
                keyMapDao.disableKeymapByUid(*it)
            }

            requestBackup()
        }
    }

    private suspend fun migrateFingerprintMaps() = withContext(dispatchers.io()) {
        val entities = fingerprintMapDao.getAll().first()

        for (entity in entities) {
            if (entity.flags.hasFlag(FingerprintMapEntity.FLAG_MIGRATED_TO_KEY_MAP)) {
                continue
            }

            val triggerKey = FingerprintTriggerKeyEntity(
                type = when (entity.id) {
                    FingerprintMapEntity.ID_SWIPE_DOWN -> FingerprintTriggerKeyEntity.ID_SWIPE_DOWN
                    FingerprintMapEntity.ID_SWIPE_UP -> FingerprintTriggerKeyEntity.ID_SWIPE_UP
                    FingerprintMapEntity.ID_SWIPE_LEFT -> FingerprintTriggerKeyEntity.ID_SWIPE_LEFT
                    FingerprintMapEntity.ID_SWIPE_RIGHT -> FingerprintTriggerKeyEntity.ID_SWIPE_RIGHT
                    else -> FingerprintTriggerKeyEntity.ID_SWIPE_DOWN
                },
                clickType = TriggerKeyEntity.SHORT_PRESS,
            )

            val extras = entity.extras.mapNotNull { extra ->
                when (extra.id) {
                    FingerprintMapEntity.EXTRA_VIBRATION_DURATION -> EntityExtra(
                        TriggerEntity.EXTRA_VIBRATION_DURATION,
                        extra.data,
                    )

                    else -> null
                }
            }

            var flags = 0

            if (entity.flags.hasFlag(FingerprintMapEntity.FLAG_VIBRATE)) {
                flags = flags or TriggerEntity.TRIGGER_FLAG_VIBRATE
            }

            if (entity.flags.hasFlag(FingerprintMapEntity.FLAG_SHOW_TOAST)) {
                flags = flags or TriggerEntity.TRIGGER_FLAG_SHOW_TOAST
            }

            val keyMapEntity = KeyMapEntity(
                id = 0,
                trigger = TriggerEntity(
                    keys = listOf(triggerKey),
                    extras = extras,
                    mode = TriggerEntity.UNDEFINED,
                    flags = flags,
                ),
                actionList = entity.actionList,
                constraintList = entity.constraintList,
                constraintMode = entity.constraintMode,
                isEnabled = entity.isEnabled,
            )

            keyMapDao.insert(keyMapEntity)

            val migratedFingerprintMapEntity =
                entity.copy(flags = entity.flags or FingerprintMapEntity.FLAG_MIGRATED_TO_KEY_MAP)
            fingerprintMapDao.update(migratedFingerprintMapEntity)
        }
    }

    private fun requestBackup() {
        coroutineScope.launch {
            val keyMapList = keyMapList.first { it is State.Data } as State.Data
            requestBackup.emit(keyMapList.data)
        }
    }
}

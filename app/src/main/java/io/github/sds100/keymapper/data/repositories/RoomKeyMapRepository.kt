package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.splitIntoBatches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Created by sds100 on 18/03/2021.
 */
class RoomKeyMapRepository(
    private val dao: KeyMapDao,
    private val devicesAdapter: DevicesAdapter,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : KeyMapRepository {

    companion object {
        private const val MAX_KEY_MAP_BATCH_SIZE = 200
    }

    override val keyMapList = dao.getAll()
        .map { State.Data(it) }
        .map { state ->
            if (fixUnknownDeviceNamesInKeyMaps(state.data)) {
                State.Loading
            } else {
                state
            }
        }
        .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override val requestBackup = MutableSharedFlow<List<KeyMapEntity>>()

    init {
        keyMapList.onEach { keyMapListState ->
            keyMapListState.ifIsData {
                fixUnknownDeviceNamesInKeyMaps(it)
            }
        }.flowOn(dispatchers.default()).launchIn(coroutineScope)
    }

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

    /**
     * See issue #612.
     * This will check if any triggers or actions have unknown device names and if the device is connected
     * then it will update the device name with the correct one.
     * This only has to check for uses of devices in Key Mapper 2.2 and older.
     *
     * @return whether any key maps were updated
     */
    private suspend fun fixUnknownDeviceNamesInKeyMaps(keyMapList: List<KeyMapEntity>): Boolean {
        val keyMapsToUpdate = mutableListOf<KeyMapEntity>()
        val connectedInputDevices =
            devicesAdapter.connectedInputDevices.first { it is State.Data } as State.Data

        if (connectedInputDevices.data.isEmpty()) {
            return false
        }

        for (keyMap in keyMapList) {
            var updateKeyMap = false

            val newTriggerKeys = keyMap.trigger.keys.map { triggerKey ->
                if (triggerKey.deviceId != TriggerKeyEntity.DEVICE_ID_THIS_DEVICE ||
                    triggerKey.deviceId != TriggerKeyEntity.DEVICE_ID_ANY_DEVICE
                ) {
                    val deviceDescriptor = triggerKey.deviceId

                    if (triggerKey.deviceName.isNullOrBlank()) {
                        val newDeviceName =
                            connectedInputDevices.data.find { it.descriptor == deviceDescriptor }?.name

                        if (newDeviceName != null) {
                            updateKeyMap = true

                            return@map triggerKey.copy(
                                deviceName = newDeviceName,
                            )
                        }
                    }
                }

                return@map triggerKey
            }

            val newActions = keyMap.actionList.map { action ->
                if (action.type == ActionEntity.Type.KEY_EVENT) {
                    val deviceDescriptor =
                        action.extras.find { it.id == ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR }?.data
                    val oldDeviceName =
                        action.extras.find { it.id == ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME }?.data

                    if (deviceDescriptor != null && oldDeviceName.isNullOrBlank()) {
                        val newDeviceName =
                            connectedInputDevices.data.find { it.descriptor == deviceDescriptor }?.name

                        if (newDeviceName != null) {
                            updateKeyMap = true

                            val newExtras = action.extras.toMutableList().apply {
                                removeAll { it.id == ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME }
                                add(Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, newDeviceName))
                            }

                            return@map action.copy(extras = newExtras)
                        }
                    }
                }

                return@map action
            }

            if (updateKeyMap) {
                val newKeyMap = keyMap.copy(
                    trigger = keyMap.trigger.copy(keys = newTriggerKeys),
                    actionList = newActions,
                )

                keyMapsToUpdate.add(newKeyMap)
            }
        }

        if (keyMapsToUpdate.isNotEmpty()) {
            dao.update(*keyMapsToUpdate.toTypedArray())
        }

        return keyMapsToUpdate.isNotEmpty()
    }

    private fun requestBackup() {
        coroutineScope.launch {
            val keyMapList = keyMapList.first { it is State.Data } as State.Data
            requestBackup.emit(keyMapList.data)
        }
    }
}

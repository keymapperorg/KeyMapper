package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.util.DefaultDispatcherProvider
import io.github.sds100.keymapper.util.DispatcherProvider
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 28/04/2021.
 */
class RoomFingerprintMapRepository(
    private val dao: FingerprintMapDao,
    private val coroutineScope: CoroutineScope,
    private val devicesAdapter: DevicesAdapter,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : FingerprintMapRepository {

    override val fingerprintMapList: Flow<State<List<FingerprintMapEntity>>> =
        dao.getAll()
            .map { list ->
                val fingerprintMapsToInsert = mutableListOf<FingerprintMapEntity>()

                if (list.none { it.id == FingerprintMapEntity.ID_SWIPE_DOWN }) {
                    fingerprintMapsToInsert.add(FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_DOWN))
                }

                if (list.none { it.id == FingerprintMapEntity.ID_SWIPE_UP }) {
                    fingerprintMapsToInsert.add(FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_UP))
                }

                if (list.none { it.id == FingerprintMapEntity.ID_SWIPE_LEFT }) {
                    fingerprintMapsToInsert.add(FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_LEFT))
                }

                if (list.none { it.id == FingerprintMapEntity.ID_SWIPE_RIGHT }) {
                    fingerprintMapsToInsert.add(FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_RIGHT))
                }

                if (fingerprintMapsToInsert.isNotEmpty()) {
                    dao.insert(*fingerprintMapsToInsert.toTypedArray())
                }

                if (fingerprintMapsToInsert.isEmpty()) {
                    return@map State.Data(list)
                } else {
                    return@map State.Loading
                }
            }
            .map { state ->
                if (state is State.Data) {
                    if (fixUnknownDeviceNamesInFingerprintMaps(state.data)) {
                        State.Loading
                    } else {
                        state
                    }
                } else {
                    State.Loading
                }
            }
            .flowOn(dispatchers.default())
            .stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override val requestBackup = MutableSharedFlow<List<FingerprintMapEntity>>()

    override fun enableFingerprintMap(id: Int) {
        coroutineScope.launch(dispatchers.default()) {
            dao.enableById(id)
        }
    }

    override fun disableFingerprintMap(id: Int) {
        coroutineScope.launch(dispatchers.default()) {
            dao.disableById(id)
        }
    }

    override fun update(vararg fingerprintMap: FingerprintMapEntity) {
        coroutineScope.launch(dispatchers.default()) {
            dao.update(*fingerprintMap)
        }
    }

    override suspend fun get(id: Int): FingerprintMapEntity = withContext(dispatchers.default()) {
        dao.getById(id) ?: FingerprintMapEntity(id = id)
    }

    override fun reset() {
        coroutineScope.launch {
            dao.update(
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_DOWN),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_UP),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_LEFT),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_RIGHT),
            )

            requestBackup()
        }
    }

    /**
     * See issue #612.
     * This will check if any actions have unknown device names and if the device is connected
     * then it will update the device name with the correct one.
     * This only has to check for uses of devices in Key Mapper 2.2 and older.
     *
     * @return whether any fingerprint maps were updated
     */
    private suspend fun fixUnknownDeviceNamesInFingerprintMaps(fingerprintMapList: List<FingerprintMapEntity>): Boolean {
        val fingerprintMapsToUpdate = mutableListOf<FingerprintMapEntity>()
        val connectedInputDevices =
            devicesAdapter.connectedInputDevices.first { it is State.Data } as State.Data

        if (connectedInputDevices.data.isEmpty()) {
            return false
        }

        for (fingerprintMap in fingerprintMapList) {
            var updateFingerprintMap = false

            val newActions = fingerprintMap.actionList.map { action ->
                if (action.type == ActionEntity.Type.KEY_EVENT) {
                    val deviceDescriptor =
                        action.extras.find { it.id == ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR }?.data
                    val oldDeviceName =
                        action.extras.find { it.id == ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME }?.data

                    if (deviceDescriptor != null && oldDeviceName.isNullOrBlank()) {
                        val newDeviceName =
                            connectedInputDevices.data.find { it.descriptor == deviceDescriptor }?.name

                        if (newDeviceName != null) {
                            updateFingerprintMap = true

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

            if (updateFingerprintMap) {
                val newKeyMap = fingerprintMap.copy(actionList = newActions)

                fingerprintMapsToUpdate.add(newKeyMap)
            }
        }

        if (fingerprintMapsToUpdate.isNotEmpty()) {
            dao.update(*fingerprintMapsToUpdate.toTypedArray())
        }

        return fingerprintMapsToUpdate.isNotEmpty()
    }

    private suspend fun requestBackup() {
        fingerprintMapList.first().ifIsData {
            requestBackup.emit(it)
        }
    }
}

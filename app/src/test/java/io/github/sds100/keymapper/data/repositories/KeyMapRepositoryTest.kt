package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.TestDispatcherProvider
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity
import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class KeyMapRepositoryTest {

    companion object {
        private val FAKE_KEYBOARD = InputDeviceInfo(
            descriptor = "fake_keyboard_descriptor",
            name = "fake keyboard",
            id = 1,
            isExternal = true,
            isGameController = false,
        )
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: RoomKeyMapRepository
    private lateinit var devicesAdapter: FakeDevicesAdapter
    private lateinit var mockDao: KeyMapDao

    private lateinit var keyMaps: MutableSharedFlow<List<KeyMapEntity>>

    @Before
    fun init() {
        keyMaps = MutableSharedFlow(replay = 1)
        devicesAdapter = FakeDevicesAdapter()

        mockDao = mock {
            on { getAll() }.then { keyMaps }
        }

        repository = RoomKeyMapRepository(
            mockDao,
            devicesAdapter,
            testScope,
            dispatchers = TestDispatcherProvider(testDispatcher),
        )
    }

    /**
     * issue #641
     */
    @Test
    fun `if modifying a huge number of key maps then split job into batches`() =
        runTest(testDispatcher) {
            // GIVEN
            val keyMapList = sequence {
                repeat(991) {
                    yield(KeyMapEntity(id = it.toLong()))
                }
            }.toList()

            keyMaps.emit(keyMapList)

            inOrder(mockDao) {
                // WHEN, THEN
                // split job up into batches of 200 key maps
                repository.enableById(*keyMapList.map { it.uid }.toTypedArray())
                verify(mockDao, times(5)).enableKeymapByUid(anyVararg())

                repository.disableById(*keyMapList.map { it.uid }.toTypedArray())
                verify(mockDao, times(5)).disableKeymapByUid(anyVararg())

                repository.delete(*keyMapList.map { it.uid }.toTypedArray())
                verify(mockDao, times(5)).deleteById(anyVararg())

                repository.duplicate(*keyMapList.map { it.uid }.toTypedArray())
                verify(mockDao, times(5)).insert(anyVararg())

                repository.insert(*keyMapList.toTypedArray())
                verify(mockDao, times(5)).insert(anyVararg())

                repository.update(*keyMapList.toTypedArray())
                verify(mockDao, times(5)).update(anyVararg())
            }
        }

    @Test
    fun `key map with key event action from device and proper device name extra, do not update action device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                ),
            )

            val keyMap = KeyMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(listOf(FAKE_KEYBOARD))

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `key map with key event action from device and blank device name extra, if device for action is disconnected, do not update action device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, ""),
                ),
            )

            val keyMap = KeyMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `key map with key event action from device and blank device name extra, if device for action is connected, update action device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, ""),
                ),
            )

            val keyMap = KeyMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD),
            )

            // WHEN
            keyMaps.emit(listOf(keyMap))

            val expectedAction = action.copy(
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                ),
            )

            // THEN
            verify(mockDao, times(1)).update(
                keyMap.copy(actionList = listOf(expectedAction)),
            )
        }

    @Test
    fun `key map with key event action from device and no device name extra, if device for action is connected, update action device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extra = Extra(
                    ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR,
                    FAKE_KEYBOARD.descriptor,
                ),
            )

            val keyMap = KeyMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD),
            )

            // WHEN
            keyMaps.emit(listOf(keyMap))

            val expectedAction = action.copy(
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                ),
            )

            // THEN
            verify(mockDao, times(1)).update(
                keyMap.copy(actionList = listOf(expectedAction)),
            )
        }

    @Test
    fun `key map with key event action from device and no device name extra, if device for action is disconnected, update action device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extra = Extra(
                    ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR,
                    FAKE_KEYBOARD.descriptor,
                ),
            )

            val keyMap = KeyMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `key map with device name for trigger key, if device for trigger key is connected, do not update trigger key device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val triggerKey = TriggerKeyEntity(
                keyCode = 1,
                deviceId = FAKE_KEYBOARD.descriptor,
                deviceName = FAKE_KEYBOARD.name,
            )

            val keyMap = KeyMapEntity(id = 0, trigger = TriggerEntity(keys = listOf(triggerKey)))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD),
            )

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `key map with device name for trigger key, if device for trigger key is disconnected, do not update trigger key device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val triggerKey = TriggerKeyEntity(
                keyCode = 1,
                deviceId = FAKE_KEYBOARD.descriptor,
                deviceName = FAKE_KEYBOARD.name,
            )

            val keyMap = KeyMapEntity(id = 0, trigger = TriggerEntity(keys = listOf(triggerKey)))

            devicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `key map with no device name for trigger key, if device for trigger key is connected, update trigger key device name`() =
        runTest(testDispatcher) {
            // GIVEN
            val triggerKey = TriggerKeyEntity(
                keyCode = 1,
                deviceId = FAKE_KEYBOARD.descriptor,
                deviceName = "",
            )

            val keyMap = KeyMapEntity(id = 0, trigger = TriggerEntity(keys = listOf(triggerKey)))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD),
            )

            // WHEN
            keyMaps.emit(listOf(keyMap))

            // THEN
            val expectedTriggerKey = triggerKey.copy(deviceName = FAKE_KEYBOARD.name)

            verify(mockDao, times(1))
                .update(keyMap.copy(trigger = TriggerEntity(listOf(expectedTriggerKey))))
        }
}

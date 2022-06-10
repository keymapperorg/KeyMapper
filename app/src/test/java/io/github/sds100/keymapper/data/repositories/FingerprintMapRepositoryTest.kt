package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.TestDispatcherProvider
import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FingerprintMapRepositoryTest {

    companion object {
        private val FAKE_KEYBOARD = InputDeviceInfo(
            descriptor = "fake_keyboard_descriptor",
            name = "fake keyboard",
            id = 1,
            isExternal = true,
            isGameController = false
        )
    }

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)
    private val dispatchers = TestDispatcherProvider(testDispatcher)

    private lateinit var repository: RoomFingerprintMapRepository
    private lateinit var devicesAdapter: FakeDevicesAdapter
    private lateinit var mockDao: FingerprintMapDao

    private lateinit var fingerprintMaps: MutableSharedFlow<List<FingerprintMapEntity>>

    @Before
    fun init() {
        fingerprintMaps = MutableSharedFlow()
        devicesAdapter = FakeDevicesAdapter()

        mockDao = mock {
            on { getAll() }.then { fingerprintMaps }
        }

        repository = RoomFingerprintMapRepository(
            mockDao,
            coroutineScope,
            devicesAdapter,
            dispatchers = dispatchers
        )

        coroutineScope.advanceUntilIdle()
    }

    @Test
    fun `only swipe down fingerprint map in database, insert 3 blank fingerprint maps for the other fingerprint maps`() =
        runTest(testDispatcher) {
            repository.fingerprintMapList.launchIn(coroutineScope)

            fingerprintMaps.emit(listOf(FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_DOWN)))

            verify(mockDao, times(1)).insert(
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_UP),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_LEFT),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_RIGHT)
            )
        }

    @Test
    fun `no fingerprint maps in database, insert 4 blank fingerprint maps`() =
        runTest(testDispatcher) {
            repository.fingerprintMapList.launchIn(coroutineScope)

            fingerprintMaps.emit(emptyList())

            verify(mockDao, times(1)).insert(
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_DOWN),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_UP),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_LEFT),
                FingerprintMapEntity(id = FingerprintMapEntity.ID_SWIPE_RIGHT)
            )
        }

    @Test
    fun `fingerprint map with key event action from device and proper device name extra, do not update action device name`() =
        runTest(testDispatcher) {
            //GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                )
            )

            val fingerprintMap = FingerprintMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(listOf(FAKE_KEYBOARD))

            //WHEN
            fingerprintMaps.emit(listOf(fingerprintMap))

            //THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `fingerprint map with key event action from device and blank device name extra, if device for action is disconnected, do not update action device name`() =
        runTest(testDispatcher) {
            //GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, ""),
                )
            )

            val fingerprintMap = FingerprintMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            //WHEN
            fingerprintMaps.emit(listOf(fingerprintMap))

            //THEN
            verify(mockDao, never()).update(any())
        }

    @Test
    fun `fingerprint map with key event action from device and blank device name extra, if device for action is connected, update action device name`() =
        runTest(testDispatcher) {
            //GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, ""),
                )
            )

            val fingerprintMap = FingerprintMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD)
            )

            //WHEN
            fingerprintMaps.emit(
                listOf(
                    fingerprintMap,
                    FingerprintMapEntity(id = 1),
                    FingerprintMapEntity(id = 2),
                    FingerprintMapEntity(id = 3),
                )
            )

            val expectedAction = action.copy(
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                )
            )

            //THEN
            verify(mockDao, times(1)).update(
                fingerprintMap.copy(actionList = listOf(expectedAction))
            )
        }

    @Test
    fun `fingerprint map with key event action from device and no device name extra, if device for action is connected, update action device name`() =
        runTest(testDispatcher) {
            //GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extra = Extra(
                    ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR,
                    FAKE_KEYBOARD.descriptor
                )
            )

            val fingerprintMap = FingerprintMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(
                listOf(FAKE_KEYBOARD)
            )
            advanceUntilIdle()

            //WHEN
            fingerprintMaps.emit(
                listOf(
                    fingerprintMap,
                    FingerprintMapEntity(id = 1),
                    FingerprintMapEntity(id = 2),
                    FingerprintMapEntity(id = 3),
                )
            )

            val expectedAction = action.copy(
                extras = listOf(
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR, FAKE_KEYBOARD.descriptor),
                    Extra(ActionEntity.EXTRA_KEY_EVENT_DEVICE_NAME, FAKE_KEYBOARD.name),
                )
            )

            //THEN
            verify(mockDao, times(1)).update(
                fingerprintMap.copy(actionList = listOf(expectedAction))
            )
        }

    @Test
    fun `fingerprint map with key event action from device and no device name extra, if device for action is disconnected, update action device name`() =
        runTest(testDispatcher) {
            //GIVEN
            val action = ActionEntity(
                type = ActionEntity.Type.KEY_EVENT,
                data = "1",
                extra = Extra(
                    ActionEntity.EXTRA_KEY_EVENT_DEVICE_DESCRIPTOR,
                    FAKE_KEYBOARD.descriptor
                )
            )

            val fingerprintMap = FingerprintMapEntity(id = 0, actionList = listOf(action))

            devicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            //WHEN
            fingerprintMaps.emit(listOf(fingerprintMap))

            //THEN
            verify(mockDao, never()).update(any())
        }
}
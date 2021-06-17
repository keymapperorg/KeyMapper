package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeMessenger
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformActionsUseCaseTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var useCase: PerformActionsUseCaseImpl
    private lateinit var mockKeyMapperImeMessenger: KeyMapperImeMessenger
    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter

    @Before
    fun init() {
        mockKeyMapperImeMessenger = mock()
        fakeDevicesAdapter = FakeDevicesAdapter()

        useCase = PerformActionsUseCaseImpl(
            coroutineScope,
            accessibilityService = mock(),
            inputMethodAdapter = mock(),
            fileAdapter = mock(),
            suAdapter = mock(),
            shellAdapter = mock(),
            intentAdapter = mock(),
            getActionError = mock(),
            keyMapperImeMessenger = mockKeyMapperImeMessenger,
            packageManagerAdapter = mock(),
            appShortcutAdapter = mock(),
            popupMessageAdapter = mock(),
            deviceAdapter = fakeDevicesAdapter,
            phoneAdapter = mock(),
            volumeAdapter = mock(),
            cameraAdapter = mock(),
            displayAdapter = mock(),
            lockScreenAdapter = mock(),
            mediaAdapter = mock(),
            airplaneModeAdapter = mock(),
            networkAdapter = mock(),
            bluetoothAdapter = mock(),
            nfcAdapter = mock(),
            openUrlAdapter = mock(),
            resourceProvider = mock(),
            preferenceRepository = mock()
        )
    }

    /**
     * issue #637
     */
    @Test
    fun `perform key event action with device name and multiple devices connected with same descriptor and none support the key code, ensure action is still performed`() =
        coroutineScope.runBlockingTest {

            //GIVEN
            val descriptor = "fake_device_descriptor"

            val action = KeyEventAction(
                keyCode = 1,
                metaState = 0,
                useShell = false,
                device = KeyEventAction.Device(descriptor = descriptor, name = "fake_name_2")
            )

            fakeDevicesAdapter.connectedInputDevices.value = State.Data(
                listOf(
                    InputDeviceInfo(
                        descriptor = descriptor,
                        name = "fake_name_1",
                        id = 10,
                        isExternal = true
                    ),
                    InputDeviceInfo(
                        descriptor = descriptor,
                        name = "fake_name_2",
                        id = 11,
                        isExternal = true
                    )
                )
            )

            //none of the devices support the key code
            fakeDevicesAdapter.deviceHasKey = { id, keyCode ->
                false
            }

            //WHEN
            useCase.perform(action, inputEventType = InputEventType.DOWN_UP, keyMetaState = 0)

            //THEN
            verify(mockKeyMapperImeMessenger, times(1)).inputKeyEvent(
                InputKeyModel(
                    keyCode = 1,
                    inputType = InputEventType.DOWN_UP,
                    metaState = 0,
                    deviceId = 11,
                    scanCode = 0,
                    repeat = 0
                )
            )
        }

    @Test
    fun `perform key event action with no device name, ensure action is still performed with correct device id`() =
        coroutineScope.runBlockingTest {

            //GIVEN
            val descriptor = "fake_device_descriptor"

            val action = KeyEventAction(
                keyCode = 1,
                metaState = 0,
                useShell = false,
                device = KeyEventAction.Device(descriptor = descriptor, name = "")
            )

            fakeDevicesAdapter.connectedInputDevices.value = State.Data(
                listOf(
                    InputDeviceInfo(
                        descriptor = descriptor,
                        name = "fake_name",
                        id = 10,
                        isExternal = true
                    )
                )
            )

            //WHEN
            useCase.perform(action, inputEventType = InputEventType.DOWN_UP, keyMetaState = 0)

            //THEN
            verify(mockKeyMapperImeMessenger, times(1)).inputKeyEvent(
                InputKeyModel(
                    keyCode = 1,
                    inputType = InputEventType.DOWN_UP,
                    metaState = 0,
                    deviceId = 10,
                    scanCode = 0,
                    repeat = 0
                )
            )
        }
}
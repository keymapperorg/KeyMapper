package io.github.sds100.keymapper.actions

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Created by sds100 on 01/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformActionsUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var useCase: PerformActionsUseCaseImpl
    private lateinit var mockImeInputEventInjector: ImeInputEventInjector
    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter
    private lateinit var mockAccessibilityService: IAccessibilityService
    private lateinit var mockToastAdapter: PopupMessageAdapter

    @Before
    fun init() {
        mockImeInputEventInjector = mock()
        fakeDevicesAdapter = FakeDevicesAdapter()
        mockAccessibilityService = mock()
        mockToastAdapter = mock()

        useCase = PerformActionsUseCaseImpl(
            testScope,
            accessibilityService = mockAccessibilityService,
            inputMethodAdapter = mock(),
            fileAdapter = mock(),
            suAdapter = mock {
                on { isGranted }.then { MutableStateFlow(false) }
            },
            shellAdapter = mock(),
            intentAdapter = mock(),
            getActionError = mock(),
            imeInputEventInjector = mockImeInputEventInjector,
            packageManagerAdapter = mock(),
            appShortcutAdapter = mock(),
            popupMessageAdapter = mockToastAdapter,
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
            preferenceRepository = mock(),
            soundsManager = mock(),
            shizukuInputEventInjector = mock(),
            permissionAdapter = mock(),
            notificationReceiverAdapter = mock(),
            ringtoneAdapter = mock(),
        )
    }

    /**
     * issue #771
     */
    @Test
    fun `dont show accessibility service not found error for open menu action`() = runTest(testDispatcher) {
        // GIVEN
        val action = ActionData.OpenMenu

        whenever(
            mockAccessibilityService.performActionOnNode(
                any(),
                any(),
            ),
        ).doReturn(Error.FailedToFindAccessibilityNode)

        // WHEN
        useCase.perform(action)

        // THEN
        verify(mockToastAdapter, never()).showPopupMessage(anyOrNull())
    }

    /**
     * issue #772
     */
    @Test
    fun `set the device id of key event actions to a connected game controller if is a game pad key code`() = runTest(testDispatcher) {
        // GIVEN
        val fakeGamePad = InputDeviceInfo(
            descriptor = "game_pad",
            name = "Game pad",
            id = 1,
            isExternal = true,
            isGameController = true,
        )

        fakeDevicesAdapter.connectedInputDevices.value = State.Data(listOf(fakeGamePad))

        val action = ActionData.InputKeyEvent(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            device = null,
        )

        // WHEN
        useCase.perform(action)

        // THEN
        val expectedInputKeyModel = InputKeyModel(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            inputType = InputEventType.DOWN_UP,
            metaState = 0,
            deviceId = fakeGamePad.id,
            scanCode = 0,
            repeat = 0,
            source = InputDevice.SOURCE_GAMEPAD,
        )

        verify(mockImeInputEventInjector, times(1)).inputKeyEvent(expectedInputKeyModel)
    }

    /**
     * issue #772
     */
    @Test
    fun `don't set the device id of key event actions to a connected game controller if there are no connected game controllers`() = runTest(testDispatcher) {
        // GIVEN
        fakeDevicesAdapter.connectedInputDevices.value = State.Data(emptyList())

        val action = ActionData.InputKeyEvent(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            device = null,
        )

        // WHEN
        useCase.perform(action)

        // THEN
        val expectedInputKeyModel = InputKeyModel(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            inputType = InputEventType.DOWN_UP,
            metaState = 0,
            deviceId = 0,
            scanCode = 0,
            repeat = 0,
            source = InputDevice.SOURCE_GAMEPAD,
        )

        verify(mockImeInputEventInjector, times(1)).inputKeyEvent(expectedInputKeyModel)
    }

    /**
     * issue #772
     */
    @Test
    fun `don't set the device id of key event actions to a connected game controller if the action has a custom device set`() = runTest(testDispatcher) {
        // GIVEN
        val fakeGamePad = InputDeviceInfo(
            descriptor = "game_pad",
            name = "Game pad",
            id = 1,
            isExternal = true,
            isGameController = true,
        )

        val fakeKeyboard = InputDeviceInfo(
            descriptor = "keyboard",
            name = "Keyboard",
            id = 2,
            isExternal = true,
            isGameController = false,
        )

        fakeDevicesAdapter.connectedInputDevices.value =
            State.Data(listOf(fakeGamePad, fakeKeyboard))

        val action = ActionData.InputKeyEvent(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            device = ActionData.InputKeyEvent.Device(
                descriptor = "keyboard",
                name = "Keyboard",
            ),
        )

        // WHEN
        useCase.perform(action)

        // THEN
        val expectedInputKeyModel = InputKeyModel(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            inputType = InputEventType.DOWN_UP,
            metaState = 0,
            deviceId = fakeKeyboard.id,
            scanCode = 0,
            repeat = 0,
            source = InputDevice.SOURCE_GAMEPAD,
        )

        verify(mockImeInputEventInjector, times(1)).inputKeyEvent(expectedInputKeyModel)
    }

    /**
     * issue #637
     */
    @Test
    fun `perform key event action with device name and multiple devices connected with same descriptor and none support the key code, ensure action is still performed`() = runTest(testDispatcher) {
        // GIVEN
        val descriptor = "fake_device_descriptor"

        val action = ActionData.InputKeyEvent(
            keyCode = 1,
            metaState = 0,
            useShell = false,
            device = ActionData.InputKeyEvent.Device(
                descriptor = descriptor,
                name = "fake_name_2",
            ),
        )

        fakeDevicesAdapter.connectedInputDevices.value = State.Data(
            listOf(
                InputDeviceInfo(
                    descriptor = descriptor,
                    name = "fake_name_1",
                    id = 10,
                    isExternal = true,
                    isGameController = false,
                ),

                InputDeviceInfo(
                    descriptor = descriptor,
                    name = "fake_name_2",
                    id = 11,
                    isExternal = true,
                    isGameController = false,
                ),
            ),
        )

        // none of the devices support the key code
        fakeDevicesAdapter.deviceHasKey = { id, keyCode -> false }

        // WHEN
        useCase.perform(action, inputEventType = InputEventType.DOWN_UP, keyMetaState = 0)

        // THEN
        verify(mockImeInputEventInjector, times(1)).inputKeyEvent(
            InputKeyModel(
                keyCode = 1,
                inputType = InputEventType.DOWN_UP,
                metaState = 0,
                deviceId = 11,
                scanCode = 0,
                repeat = 0,
                source = InputDevice.SOURCE_KEYBOARD,
            ),
        )
    }

    @Test
    fun `perform key event action with no device name, ensure action is still performed with correct device id`() = runTest(testDispatcher) {
        // GIVEN
        val descriptor = "fake_device_descriptor"

        val action = ActionData.InputKeyEvent(
            keyCode = 1,
            metaState = 0,
            useShell = false,
            device = ActionData.InputKeyEvent.Device(descriptor = descriptor, name = ""),
        )

        fakeDevicesAdapter.connectedInputDevices.value = State.Data(
            listOf(
                InputDeviceInfo(
                    descriptor = descriptor,
                    name = "fake_name",
                    id = 10,
                    isExternal = true,
                    isGameController = false,
                ),
            ),
        )

        // WHEN
        useCase.perform(action, inputEventType = InputEventType.DOWN_UP, keyMetaState = 0)

        // THEN
        verify(mockImeInputEventInjector, times(1)).inputKeyEvent(
            InputKeyModel(
                keyCode = 1,
                inputType = InputEventType.DOWN_UP,
                metaState = 0,
                deviceId = 10,
                scanCode = 0,
                repeat = 0,
                source = InputDevice.SOURCE_KEYBOARD,
            ),
        )
    }
}

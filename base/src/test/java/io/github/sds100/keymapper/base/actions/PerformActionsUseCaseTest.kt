package io.github.sds100.keymapper.base.actions

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.base.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.popup.ToastAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformActionsUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var useCase: PerformActionsUseCaseImpl
    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter
    private lateinit var mockAccessibilityService: IAccessibilityService
    private lateinit var mockToastAdapter: ToastAdapter
    private lateinit var mockInputEventHub: InputEventHub

    @Before
    fun init() {
        fakeDevicesAdapter = FakeDevicesAdapter()
        mockAccessibilityService = mock()
        mockToastAdapter = mock()
        mockInputEventHub = mock {
            on { runBlocking { injectKeyEvent(any()) } }.then { Success(Unit) }
        }

        useCase = PerformActionsUseCaseImpl(
            service = mockAccessibilityService,
            inputMethodAdapter = mock(),
            fileAdapter = mock(),
            suAdapter = mock {},
            shell = mock(),
            intentAdapter = mock(),
            getActionErrorUseCase = mock(),
            keyMapperImeMessenger = mock(),
            packageManagerAdapter = mock(),
            appShortcutAdapter = mock(),
            toastAdapter = mockToastAdapter,
            devicesAdapter = fakeDevicesAdapter,
            phoneAdapter = mock(),
            audioAdapter = mock(),
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
            settingsRepository = mock(),
            soundsManager = mock(),
            notificationReceiverAdapter = mock(),
            ringtoneAdapter = mock(),
            inputEventHub = mockInputEventHub,
            systemBridgeConnectionManager = mock(),
        )
    }

    /**
     * issue #771
     */
    @Test
    fun `dont show accessibility service not found error for open menu action`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = ActionData.OpenMenu

            whenever(
                mockAccessibilityService.performActionOnNode(
                    any(),
                    any(),
                ),
            ).doReturn(KMError.FailedToFindAccessibilityNode)

            // WHEN
            useCase.perform(action)

            // THEN
            verify(mockToastAdapter, never()).show(anyOrNull())
        }

    /**
     * issue #772
     */
    @Test
    fun `set the device id of key event actions to a connected game controller if is a game pad key code`() =
        runTest(testDispatcher) {
            // GIVEN
            val fakeGamePad = InputDeviceInfo(
                descriptor = "game_pad",
                name = "Game pad",
                id = 1,
                isExternal = true,
                isGameController = true,
                sources = InputDevice.SOURCE_GAMEPAD,
            )

            fakeDevicesAdapter.connectedInputDevices.value = State.Data(listOf(fakeGamePad))

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            // WHEN
            useCase.perform(action)

            // THEN
            val expectedDownEvent = InjectKeyEventModel(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = fakeGamePad.id,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_GAMEPAD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent)
        }

    /**
     * issue #772
     */
    @Test
    fun `don't set the device id of key event actions to a connected game controller if there are no connected game controllers`() =
        runTest(testDispatcher) {
            // GIVEN
            fakeDevicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            // WHEN
            useCase.perform(action)

            // THEN
            val expectedDownEvent = InjectKeyEventModel(

                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = 0,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_GAMEPAD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent)
        }

    /**
     * issue #772
     */
    @Test
    fun `don't set the device id of key event actions to a connected game controller if the action has a custom device set`() =
        runTest(testDispatcher) {
            // GIVEN
            val fakeGamePad = InputDeviceInfo(
                descriptor = "game_pad",
                name = "Game pad",
                id = 1,
                isExternal = true,
                isGameController = true,
                sources = InputDevice.SOURCE_GAMEPAD,
            )

            val fakeKeyboard = InputDeviceInfo(
                descriptor = "keyboard",
                name = "Keyboard",
                id = 2,
                isExternal = true,
                isGameController = false,
                sources = InputDevice.SOURCE_GAMEPAD,
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
            val expectedDownEvent = InjectKeyEventModel(

                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = fakeKeyboard.id,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_GAMEPAD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent)
        }

    /**
     * issue #637
     */
    @Test
    fun `perform key event action with device name and multiple devices connected with same descriptor and none support the key code, ensure action is still performed`() =
        runTest(testDispatcher) {
            // GIVEN
            val descriptor = "fake_device_descriptor"

            val action = ActionData.InputKeyEvent(
                keyCode = 1,
                metaState = 0,
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
                        sources = InputDevice.SOURCE_GAMEPAD,
                    ),

                    InputDeviceInfo(
                        descriptor = descriptor,
                        name = "fake_name_2",
                        id = 11,
                        isExternal = true,
                        isGameController = false,
                        sources = InputDevice.SOURCE_GAMEPAD,
                    ),
                ),
            )

            // none of the devices support the key code
            fakeDevicesAdapter.deviceHasKey = { id, keyCode -> false }

            // WHEN
            useCase.perform(action, inputEventAction = InputEventAction.DOWN_UP, keyMetaState = 0)

            // THEN
            val expectedDownEvent = InjectKeyEventModel(

                keyCode = 1,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = 11,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_KEYBOARD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent)
        }

    @Test
    fun `perform key event action with no device name, ensure action is still performed with correct device id`() =
        runTest(testDispatcher) {
            // GIVEN
            val descriptor = "fake_device_descriptor"

            val action = ActionData.InputKeyEvent(
                keyCode = 1,
                metaState = 0,
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
                        sources = InputDevice.SOURCE_GAMEPAD,
                    ),
                ),
            )

            // WHEN
            useCase.perform(action, inputEventAction = InputEventAction.DOWN_UP, keyMetaState = 0)

            // THEN
            val expectedDownEvent = InjectKeyEventModel(

                keyCode = 1,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = 10,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_KEYBOARD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent)
        }
}

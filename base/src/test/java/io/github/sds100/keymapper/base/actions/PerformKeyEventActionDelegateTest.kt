package io.github.sds100.keymapper.base.actions

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.base.system.devices.FakeDevicesAdapter
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.data.Keys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformKeyEventActionDelegateTest {

    companion object {
        private val FAKE_CONTROLLER_EVDEV_DEVICE = EvdevDeviceInfo(
            name = "Fake Controller",
            bus = 1,
            vendor = 2,
            product = 1,
        )
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher)
    private lateinit var delegate: PerformKeyEventActionDelegate
    private lateinit var fakePreferenceRepository: FakePreferenceRepository
    private lateinit var mockInputEventHub: InputEventHub

    private lateinit var fakeDevicesAdapter: FakeDevicesAdapter

    @Before
    fun init() {
        fakePreferenceRepository = FakePreferenceRepository()
        fakeDevicesAdapter = FakeDevicesAdapter()

        mockInputEventHub = mock {
            on { runBlocking { injectKeyEvent(any(), any()) } }.then { Success(Unit) }
            on {
                runBlocking { injectEvdevEventKeyCode(any(), any(), any()) }
            }.then { Success(Unit) }
        }

        delegate = PerformKeyEventActionDelegate(
            coroutineScope = testCoroutineScope,
            settingsRepository = fakePreferenceRepository,
            inputEventHub = mockInputEventHub,
            devicesAdapter = fakeDevicesAdapter,
        )
    }

    @Test
    fun `use trigger device id if no device specified for action`() = runTest(testDispatcher) {
        val action = ActionData.InputKeyEvent(
            keyCode = KeyEvent.KEYCODE_A,
            device = null,
        )

        delegate.perform(
            action,
            inputEventAction = InputEventAction.DOWN,
            keyMetaState = 0,
            triggerDevice = PerformActionTriggerDevice.AndroidDevice(deviceId = 3),
        )

        val expectedDownEvent = InjectKeyEventModel(
            keyCode = KeyEvent.KEYCODE_A,
            action = KeyEvent.ACTION_DOWN,
            metaState = 0,
            deviceId = 3,
            scanCode = 0,
            repeatCount = 0,
            source = InputDevice.SOURCE_KEYBOARD,
        )

        verify(mockInputEventHub).injectKeyEvent(
            expectedDownEvent,
            useSystemBridgeIfAvailable = false,
        )
    }

    @Test
    fun `inject evdev event if action device set as a non-evdev device but it is disconnected`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = ActionData.InputKeyEvent.Device(
                    descriptor = "keyboard_descriptor",
                    name = "Keyboard",
                ),
            )

            // The Keyboard is not connected, so still perform the action through the evdev device
            // that triggered it.
            fakeDevicesAdapter.connectedInputDevices.value = State.Data(emptyList())

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 1),
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 1,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 1,
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 1,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 0,
            )
        }

    @Test
    fun `do not inject evdev event if triggered by evdev device and action device is set`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            fakeDevicesAdapter.connectedInputDevices.value = State.Data(
                listOf(
                    InputDeviceInfo(
                        descriptor = "keyboard_descriptor",
                        name = "Keyboard",
                        id = 10,
                        isExternal = true,
                        isGameController = false,
                        sources = InputDevice.SOURCE_KEYBOARD,
                    ),
                ),
            )

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                device = ActionData.InputKeyEvent.Device(
                    descriptor = "keyboard_descriptor",
                    name = "Keyboard",
                ),
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 1),
            )

            val expectedDownEvent = InjectKeyEventModel(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_DOWN,
                metaState = 0,
                deviceId = 10,
                scanCode = 0,
                repeatCount = 0,
                source = InputDevice.SOURCE_KEYBOARD,
            )

            val expectedUpEvent = expectedDownEvent.copy(action = KeyEvent.ACTION_UP)

            verify(mockInputEventHub).injectKeyEvent(expectedDownEvent, true)
            verify(mockInputEventHub).injectKeyEvent(expectedUpEvent, true)
            verify(mockInputEventHub, never()).injectEvdevEventKeyCode(any(), any(), any())
        }

    @Test
    fun `inject evdev event if triggered by evdev device and action device set to the same device`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = ActionData.InputKeyEvent.Device(
                    descriptor = "descriptor",
                    name = FAKE_CONTROLLER_EVDEV_DEVICE.name,
                ),
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 1),
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 1,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 1,
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 1,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 0,
            )
        }

    @Test
    fun `inject evdev event if triggered by evdev device and action device not set`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 0),
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 0,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 1,
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 0,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 0,
            )
        }

    @Test
    fun `do not inject evdev event if not using system bridge for key event actions`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, false)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 0),
            )

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

            verify(mockInputEventHub).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub).injectKeyEvent(expectedUpEvent, false)
            verify(mockInputEventHub, never()).injectEvdevEventKeyCode(any(), any(), any())
        }

    @Test
    fun `inject down evdev event if triggered by evdev device and action device not set`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 0),
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 0,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 1,
            )
        }

    @Test
    fun `inject up evdev event if triggered by evdev device and action device not set`() =
        runTest(testDispatcher) {
            fakePreferenceRepository.set(Keys.keyEventActionsUseSystemBridge, true)

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            delegate.perform(
                action,
                inputEventAction = InputEventAction.UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Evdev(deviceId = 0),
            )

            verify(mockInputEventHub).injectEvdevEventKeyCode(
                deviceId = 0,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                value = 0,
            )
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

            fakeDevicesAdapter.connectedInputDevices.value =
                State.Data(listOf(fakeGamePad))

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            // WHEN

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Default,
            )

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

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent, false)
        }

    /**
     * issue #772
     */
    @Test
    fun `don't set the device id of key event actions to a connected game controller if there are no connected game controllers`() =
        runTest(testDispatcher) {
            // GIVEN
            fakeDevicesAdapter.connectedInputDevices.value =
                State.Data(emptyList())

            val action = ActionData.InputKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                device = null,
            )

            // WHEN

            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Default,
            )

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

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent, false)
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
            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Default,
            )

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

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent, false)
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

            fakeDevicesAdapter.connectedInputDevices.value =
                State.Data(
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
            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Default,
            )

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

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent, false)
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
            delegate.perform(
                action,
                inputEventAction = InputEventAction.DOWN_UP,
                keyMetaState = 0,
                triggerDevice = PerformActionTriggerDevice.Default,
            )

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

            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedDownEvent, false)
            verify(mockInputEventHub, times(1)).injectKeyEvent(expectedUpEvent, false)
        }
}

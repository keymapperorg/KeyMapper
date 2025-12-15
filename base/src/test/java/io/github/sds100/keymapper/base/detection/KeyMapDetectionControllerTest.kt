package io.github.sds100.keymapper.base.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.utils.parallelTrigger
import io.github.sds100.keymapper.base.utils.singleKeyTrigger
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.models.GrabDeviceRequest
import io.github.sds100.keymapper.system.inputevents.Scancode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.collection.IsEmptyCollection.empty
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class KeyMapDetectionControllerTest {

    companion object {
        private val FAKE_CONTROLLER_EVDEV_DEVICE = EvdevDeviceInfo(
            name = "Fake Controller",
            bus = 1,
            vendor = 2,
            product = 1,
        )

        private val FAKE_CONTROLLER_EVDEV_DEVICE_2 = EvdevDeviceInfo(
            name = "Fake Controller 2",
            bus = 1,
            vendor = 3,
            product = 2,
        )
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var algorithm: KeyMapAlgorithm
    private lateinit var mockedKeyEvent: MockedStatic<KeyEvent>

    @Before
    fun init() {
        mockedKeyEvent = mockStatic(KeyEvent::class.java)
        mockedKeyEvent.`when`<Int> { KeyEvent.getMaxKeyCode() }.thenReturn(1000)

        algorithm = KeyMapAlgorithm(
            coroutineScope = testScope,
            useCase = mock(),
            performActionsUseCase = mock(),
            detectConstraints = mock(),
        )
    }

    @After
    fun tearDown() {
        mockedKeyEvent.close()
    }

    @Test
    fun `Only grab the same device once if multiple key maps add key events`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    Action(data = ActionData.OpenCamera),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_Y),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(
                        KeyEvent.KEYCODE_BUTTON_X,
                        KeyEvent.KEYCODE_BUTTON_Y,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Grab multiple evdev devices from multiple triggers with key event actions`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    Action(data = ActionData.OpenCamera),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_Y),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_X),
                ),
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_Y),
                ),
            ),
        )
    }

    @Test
    fun `Grab multiple evdev devices from multiple triggers`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Grab multiple evdev devices from the same trigger`() {
        loadKeyMaps(
            KeyMap(
                trigger = parallelTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Grab evdev device for evdev trigger`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)
        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Do not grab evdev device if key map has no actions`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = emptyList(),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)
        assertThat(grabRequests, empty())
    }

    @Test
    fun `Grab evdev device with extra key codes if action inputs key events`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_B)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_B),
                ),
            ),
        )
    }

    @Test
    fun `Do not grab evdev device for key event trigger`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    KeyEventTriggerKey(
                        keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                        device = KeyEventTriggerDevice.Any,
                    ),
                ),
            ),
        )
        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)
        assertThat(grabRequests, empty())
    }

    @Test
    fun `do not grab any evdev devices if no triggers`() {
        algorithm.loadKeyMaps(emptyList())
        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)
        assertThat(grabRequests, empty())
    }

    // ==================== TRIGGER KEY COMBINATION EDGE CASES ====================

    @Test
    fun `Same key on different devices in parallel trigger should grab both devices`() {
        loadKeyMaps(
            KeyMap(
                trigger = parallelTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Long press trigger should grab device`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.LONG_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Double press trigger should grab device`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.DOUBLE_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Same key with different click types in different key maps should grab device once`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.LONG_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.GoHome)),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.DOUBLE_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.GoBack)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Mixed evdev and key event triggers in parallel should only grab evdev device`() {
        loadKeyMaps(
            KeyMap(
                trigger = parallelTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                    KeyEventTriggerKey(
                        keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                        device = KeyEventTriggerDevice.Any,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    // ==================== ACTION EDGE CASES ====================

    @Test
    fun `Multiple key event actions should add all key codes to extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_Y),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_L1),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(
                        KeyEvent.KEYCODE_BUTTON_X,
                        KeyEvent.KEYCODE_BUTTON_Y,
                        KeyEvent.KEYCODE_BUTTON_L1,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Duplicate key event actions should not duplicate extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
        )
    }

    @Test
    fun `Action that inputs same key code as trigger key should include in extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_A),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_A),
                ),
            ),
        )
    }

    @Test
    fun `Key map with only non-key-event actions should have empty extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    Action(data = ActionData.OpenCamera),
                    Action(data = ActionData.GoHome),
                    Action(data = ActionData.Screenshot),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Key event action with meta state should include key code in extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    Action(
                        data = ActionData.InputKeyEvent(
                            keyCode = KeyEvent.KEYCODE_C,
                            metaState = KeyEvent.META_CTRL_ON,
                        ),
                    ),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_C),
                ),
            ),
        )
    }

    @Test
    fun `Mixed key event and non-key-event actions should only include key codes from key event actions`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(
                    Action(data = ActionData.OpenCamera),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X),
                    Action(data = ActionData.GoHome),
                    buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_Y),
                    Action(data = ActionData.Screenshot),
                ),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(
                        KeyEvent.KEYCODE_BUTTON_X,
                        KeyEvent.KEYCODE_BUTTON_Y,
                    ),
                ),
            ),
        )
    }

    // ==================== DEVICE HANDLING EDGE CASES ====================

    @Test
    fun `Disabled key map should not produce grab requests`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
                isEnabled = false,
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(grabRequests, empty())
    }

    @Test
    fun `Mix of enabled and disabled key maps should only grab for enabled ones`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
                isEnabled = true,
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.GoHome)),
                isEnabled = false,
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    @Test
    fun `Disabled key map with key event actions should not contribute extra key codes`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X)),
                isEnabled = true,
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_Y)),
                isEnabled = false,
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
        )
    }

    // ==================== EMPTY/NULL AND DUPLICATE HANDLING ====================

    @Test
    fun `Key map with empty trigger keys should not produce grab requests`() {
        loadKeyMaps(
            KeyMap(
                trigger = Trigger(keys = emptyList()),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(grabRequests, empty())
    }

    @Test
    fun `Same extra key code from multiple key maps on same device should not duplicate`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X)),
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_BUTTON_X)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(KeyEvent.KEYCODE_BUTTON_X),
                ),
            ),
        )
    }

    @Test
    fun `All key maps disabled should produce empty grab requests`() {
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
                isEnabled = false,
            ),
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE_2,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.GoHome)),
                isEnabled = false,
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(grabRequests, empty())
    }

    @Test
    fun `Large number of key maps should correctly aggregate grab requests`() {
        val keyMaps = (0 until 10).map { index ->
            KeyMap(
                trigger = singleKeyTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A + index,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A + index,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(buildKeyEventAction(KeyEvent.KEYCODE_0 + index)),
            )
        }
        loadKeyMaps(*keyMaps.toTypedArray())

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        val expectedExtraKeyCodes = (0 until 10).map { KeyEvent.KEYCODE_0 + it }.toIntArray()

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = expectedExtraKeyCodes,
                ),
            ),
        )
    }

    @Test
    fun `Parallel trigger with multiple keys on same device should grab once`() {
        loadKeyMaps(
            KeyMap(
                trigger = parallelTrigger(
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_A,
                        keyCode = KeyEvent.KEYCODE_BUTTON_A,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_B,
                        keyCode = KeyEvent.KEYCODE_BUTTON_B,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                    EvdevTriggerKey(
                        scanCode = Scancode.BTN_X,
                        keyCode = KeyEvent.KEYCODE_BUTTON_X,
                        clickType = ClickType.SHORT_PRESS,
                        device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    ),
                ),
                actionList = listOf(Action(data = ActionData.OpenCamera)),
            ),
        )

        val grabRequests = KeyMapDetectionController.getEvdevGrabRequests(algorithm)

        assertThat(
            grabRequests,
            contains(
                GrabDeviceRequest(
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    extraKeyCodes = intArrayOf(),
                ),
            ),
        )
    }

    private fun buildKeyEventAction(keyCode: Int): Action {
        return Action(
            data = ActionData.InputKeyEvent(keyCode = keyCode),
        )
    }

    private fun loadKeyMaps(vararg keyMap: KeyMap) {
        val models = listOf(*keyMap).map { DetectKeyMapModel(it) }
        algorithm.loadKeyMaps(models)
    }
}

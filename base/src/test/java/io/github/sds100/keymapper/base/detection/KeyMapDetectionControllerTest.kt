package io.github.sds100.keymapper.base.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
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
import org.junit.Before
import org.junit.Test
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

    @Before
    fun before() {
        algorithm = KeyMapAlgorithm(
            coroutineScope = testScope,
            useCase = mock(),
            performActionsUseCase = mock(),
            detectConstraints = mock(),
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

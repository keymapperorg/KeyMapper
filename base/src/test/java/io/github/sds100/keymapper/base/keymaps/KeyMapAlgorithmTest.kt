package io.github.sds100.keymapper.base.keymaps

import android.view.InputDevice
import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.actions.RepeatMode
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.detection.DetectKeyMapModel
import io.github.sds100.keymapper.base.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.base.detection.KeyMapAlgorithm
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerKey
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.utils.TestConstraintSnapshot
import io.github.sds100.keymapper.base.utils.parallelTrigger
import io.github.sds100.keymapper.base.utils.sequenceTrigger
import io.github.sds100.keymapper.base.utils.singleKeyTrigger
import io.github.sds100.keymapper.base.utils.triggerKey
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.common.utils.InputEventAction
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputevents.Scancode
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class KeyMapAlgorithmTest {

    companion object {
        private const val FAKE_KEYBOARD_DEVICE_ID = 123
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
        private val FAKE_KEYBOARD_TRIGGER_KEY_DEVICE = KeyEventTriggerDevice.External(
            descriptor = FAKE_KEYBOARD_DESCRIPTOR,
            name = "Fake Keyboard",
        )
        private val FAKE_INTERNAL_DEVICE = InputDeviceInfo(
            descriptor = "volume_keys",
            name = "Volume keys",
            id = 0,
            isExternal = false,
            isGameController = false,
            sources = InputDevice.SOURCE_UNKNOWN,
        )

        private const val FAKE_HEADPHONE_DESCRIPTOR = "fake_headphone"
        private val FAKE_HEADPHONE_TRIGGER_KEY_DEVICE = KeyEventTriggerDevice.External(
            descriptor = FAKE_HEADPHONE_DESCRIPTOR,
            name = "Fake Headphones",
        )

        private const val FAKE_CONTROLLER_DESCRIPTOR = "fake_controller"
        private val FAKE_CONTROLLER_TRIGGER_KEY_DEVICE = KeyEventTriggerDevice.External(
            descriptor = FAKE_CONTROLLER_DESCRIPTOR,
            name = "Fake Controller",
        )
        private val FAKE_CONTROLLER_INPUT_DEVICE = InputDeviceInfo(
            descriptor = FAKE_CONTROLLER_DESCRIPTOR,
            name = "Fake Controller",
            id = 1,
            isExternal = true,
            isGameController = true,
            sources = InputDevice.SOURCE_GAMEPAD,
        )

        private val FAKE_CONTROLLER_EVDEV_DEVICE = EvdevDeviceInfo(
            name = "Fake Controller",
            bus = 1,
            vendor = 2,
            product = 1,
        )

        private val FAKE_VOLUME_EVDEV_DEVICE = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        private const val FAKE_PACKAGE_NAME = "test_package"

        private const val LONG_PRESS_DELAY = 500L
        private const val DOUBLE_PRESS_DELAY = 300L
        private const val FORCE_VIBRATE = false
        private const val REPEAT_RATE = 50L
        private const val REPEAT_DELAY = 400L
        private const val SEQUENCE_TRIGGER_TIMEOUT = 2000L
        private const val VIBRATION_DURATION = 100L
        private const val HOLD_DOWN_DURATION = 1000L

        private val TEST_ACTION: Action = Action(
            data = ActionData.Flashlight.Toggle(CameraLens.BACK, strengthPercent = null),
        )

        private val TEST_ACTION_2: Action = Action(
            data = ActionData.App(FAKE_PACKAGE_NAME),
        )
    }

    private lateinit var controller: KeyMapAlgorithm
    private lateinit var detectKeyMapsUseCase: DetectKeyMapsUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase
    private lateinit var mockedKeyEvent: MockedStatic<KeyEvent>

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun loadKeyMaps(vararg keyMap: KeyMap) {
        controller.loadKeyMaps(keyMap.map { DetectKeyMapModel(it) })
    }

    private fun loadKeyMaps(vararg keyMap: DetectKeyMapModel) {
        controller.loadKeyMaps(keyMap.toList())
    }

    @Before
    fun init() {
        detectKeyMapsUseCase = mock {
            MutableStateFlow(VIBRATION_DURATION).apply {
                on { defaultVibrateDuration } doReturn this
            }

            MutableStateFlow(SEQUENCE_TRIGGER_TIMEOUT).apply {
                on { defaultSequenceTriggerTimeout } doReturn this
            }

            MutableStateFlow(LONG_PRESS_DELAY).apply {
                on { defaultLongPressDelay } doReturn this
            }

            MutableStateFlow(FORCE_VIBRATE).apply {
                on { forceVibrate } doReturn this
            }

            MutableStateFlow(DOUBLE_PRESS_DELAY).apply {
                on { defaultDoublePressDelay } doReturn this
            }
        }

        whenever(detectKeyMapsUseCase.currentTime).thenAnswer { testScope.currentTime }

        performActionsUseCase = mock {
            MutableStateFlow(REPEAT_DELAY).apply {
                on { defaultRepeatDelay } doReturn this
            }

            MutableStateFlow(REPEAT_RATE).apply {
                on { defaultRepeatRate } doReturn this
            }

            MutableStateFlow(HOLD_DOWN_DURATION).apply {
                on { defaultHoldDownDuration } doReturn this
            }

            on { getErrorSnapshot() } doReturn object : ActionErrorSnapshot {
                override fun getError(action: ActionData): KMError? = null
                override fun getErrors(actions: List<ActionData>): Map<ActionData, KMError?> {
                    return emptyMap()
                }
            }
        }

        detectConstraintsUseCase = mock {
            on { getSnapshot() } doReturn TestConstraintSnapshot()
        }

        mockedKeyEvent = mockStatic(KeyEvent::class.java)
        mockedKeyEvent.`when`<Int> { KeyEvent.getMaxKeyCode() }.thenReturn(1000)

        controller = KeyMapAlgorithm(
            testScope,
            detectKeyMapsUseCase,
            performActionsUseCase,
            detectConstraintsUseCase,
        )
    }

    @After
    fun tearDown() {
        mockedKeyEvent.close()
    }

    @Test
    fun `Detect mouse button which only has scan code`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_UNKNOWN,
                scanCode = Scancode.BTN_LEFT,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
        )
        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        inputDownEvdevEvent(
            KeyEvent.KEYCODE_UNKNOWN,
            Scancode.BTN_LEFT,
            FAKE_CONTROLLER_EVDEV_DEVICE,
        )
        inputUpEvdevEvent(KeyEvent.KEYCODE_UNKNOWN, Scancode.BTN_LEFT, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Detect evdev trigger with scan code if key has unknown key code`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_UNKNOWN,
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                ),
            )
            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            inputDownEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Detect evdev trigger with scan code if user setting enabled`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
                detectWithScanCodeUserSetting = true,
            ),
        )
        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        inputDownEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputUpEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Detect evdev trigger with scan code when scan code matches but key code differs`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    detectWithScanCodeUserSetting = true,
                ),
            )
            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            // Input with different key code but matching scan code
            inputDownEvdevEvent(KeyEvent.KEYCODE_C, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_C, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Do not detect evdev trigger when scan code differs`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
                detectWithScanCodeUserSetting = true,
            ),
        )
        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        // Input with matching key code but different scan code
        inputDownEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_C, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputUpEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_C, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
    }

    @Test
    fun `Sequence trigger with multiple evdev keys and scan code detection is triggered`() =
        runTest(testDispatcher) {
            val trigger = sequenceTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    // Different scan code
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    detectWithScanCodeUserSetting = true,
                ),
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_C,
                    // Different scan code
                    scanCode = Scancode.KEY_D,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    detectWithScanCodeUserSetting = true,
                ),
            )

            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            // Input with scan codes that match the trigger
            inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

            inputDownEvdevEvent(KeyEvent.KEYCODE_Y, Scancode.KEY_D, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_Y, Scancode.KEY_D, FAKE_CONTROLLER_EVDEV_DEVICE)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Parallel trigger with multiple evdev keys and scan code detection is triggered`() =
        runTest(testDispatcher) {
            val trigger = parallelTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    // Different scan code
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    detectWithScanCodeUserSetting = true,
                ),
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_C,
                    // Different scan code
                    scanCode = Scancode.KEY_D,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                    detectWithScanCodeUserSetting = true,
                ),
            )

            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            // Input both keys simultaneously with scan codes that match the trigger
            inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputDownEvdevEvent(KeyEvent.KEYCODE_Y, Scancode.KEY_D, FAKE_CONTROLLER_EVDEV_DEVICE)

            inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_Y, Scancode.KEY_D, FAKE_CONTROLLER_EVDEV_DEVICE)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Scan code detection works with long press evdev trigger`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
                clickType = ClickType.LONG_PRESS,
                detectWithScanCodeUserSetting = true,
            ),
        )
        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        // Wait for long press duration
        delay(LONG_PRESS_DELAY + 100L)
        inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Scan code detection works with double press evdev trigger`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
                clickType = ClickType.DOUBLE_PRESS,
                detectWithScanCodeUserSetting = true,
            ),
        )
        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        // First press
        inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        delay(50L)
        inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        delay(50L)

        // Second press
        inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        delay(50L)
        inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Scan code detection fails when device differs for evdev trigger`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_EVDEV_DEVICE,
                ),
            )
            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            // Input from different device
            inputDownEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_VOLUME_EVDEV_DEVICE)
            inputUpEvdevEvent(KeyEvent.KEYCODE_X, Scancode.KEY_B, FAKE_VOLUME_EVDEV_DEVICE)

            verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
        }

    @Test
    fun `Detect key event trigger with scan code if key has unknown key code`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_UNKNOWN,
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    // It will be automatically enabled even if the user hasn't explicitly turned it on
                    detectWithScanCodeUserSetting = false,
                ),
            )
            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_B,
                action = KeyEvent.ACTION_DOWN,
                device = FAKE_CONTROLLER_INPUT_DEVICE,
                scanCode = Scancode.KEY_B,
            )
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_B,
                action = KeyEvent.ACTION_UP,
                device = FAKE_CONTROLLER_INPUT_DEVICE,
                scanCode = Scancode.KEY_B,
            )

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Detect key event trigger with scan code if user setting enabled`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    scanCode = Scancode.KEY_B,
                    device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    detectWithScanCodeUserSetting = true,
                ),
            )
            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_B,
                action = KeyEvent.ACTION_DOWN,
                device = FAKE_CONTROLLER_INPUT_DEVICE,
                scanCode = Scancode.KEY_B,
            )
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_B,
                action = KeyEvent.ACTION_UP,
                device = FAKE_CONTROLLER_INPUT_DEVICE,
                scanCode = Scancode.KEY_B,
            )

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Sequence trigger with multiple evdev keys is triggered`() = runTest(testDispatcher) {
        val trigger = sequenceTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_A,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_B,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
        )

        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        inputDownEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_A, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputUpEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_A, FAKE_CONTROLLER_EVDEV_DEVICE)

        inputDownEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputUpEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Parallel trigger with multiple evdev keys is triggered`() = runTest(testDispatcher) {
        val trigger = parallelTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_A,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_B,
                scanCode = Scancode.KEY_B,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
        )

        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        inputDownEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_A, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputDownEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        inputUpEvdevEvent(KeyEvent.KEYCODE_A, Scancode.KEY_A, FAKE_CONTROLLER_EVDEV_DEVICE)
        inputUpEvdevEvent(KeyEvent.KEYCODE_B, Scancode.KEY_B, FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Evdev trigger is not triggered from events from other devices`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_POWER,
                    scanCode = Scancode.KEY_POWER,
                    device = FAKE_VOLUME_EVDEV_DEVICE,
                ),
            )

            loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

            mockEvdevKeyInput(trigger.keys[0], FAKE_CONTROLLER_EVDEV_DEVICE)

            verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
        }

    @Test
    fun `Short press trigger evdev trigger from external device`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_A,
                scanCode = Scancode.KEY_POWER,
                device = FAKE_CONTROLLER_EVDEV_DEVICE,
            ),
        )

        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        mockEvdevKeyInput(trigger.keys[0], FAKE_CONTROLLER_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Short press trigger evdev trigger from internal device`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_POWER,
                scanCode = Scancode.KEY_POWER,
                device = FAKE_VOLUME_EVDEV_DEVICE,
            ),
        )

        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)))

        mockEvdevKeyInput(trigger.keys[0], FAKE_VOLUME_EVDEV_DEVICE)

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Do not perform if one group constraint set is not satisfied`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN))
        loadKeyMaps(
            DetectKeyMapModel(
                keyMap = KeyMap(
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION),
                    constraintState = ConstraintState(
                        constraints = setOf(
                            Constraint(data = ConstraintData.WifiOn),
                            Constraint(data = ConstraintData.DeviceIsLocked),
                        ),
                        mode = ConstraintMode.OR,
                    ),
                ),
                groupConstraintStates = listOf(
                    ConstraintState(
                        constraints = setOf(
                            Constraint(data = ConstraintData.LockScreenNotShowing),
                            Constraint(data = ConstraintData.DeviceIsLocked),
                        ),
                        mode = ConstraintMode.AND,
                    ),
                    ConstraintState(
                        constraints = setOf(
                            Constraint(data = ConstraintData.AppInForeground(packageName = "app")),
                            Constraint(data = ConstraintData.DeviceIsUnlocked),
                        ),
                        mode = ConstraintMode.OR,
                    ),
                ),
            ),
        )

        whenever(detectConstraintsUseCase.getSnapshot())
            .thenReturn(
                TestConstraintSnapshot(
                    isWifiEnabled = true,
                    isLocked = true,
                    isLockscreenShowing = true,
                    appInForeground = "app",
                ),
            )

        mockTriggerKeyInput(trigger.keys[0])

        verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
    }

    @Test
    fun `Perform if all group constraints and key map constraints are satisfied`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN))
            loadKeyMaps(
                DetectKeyMapModel(
                    keyMap = KeyMap(
                        trigger = trigger,
                        actionList = listOf(TEST_ACTION),
                        constraintState = ConstraintState(
                            constraints = setOf(
                                Constraint(data = ConstraintData.WifiOn),
                                Constraint(data = ConstraintData.DeviceIsLocked),
                            ),
                            mode = ConstraintMode.OR,
                        ),
                    ),
                    groupConstraintStates = listOf(
                        ConstraintState(
                            constraints = setOf(
                                Constraint(data = ConstraintData.LockScreenNotShowing),
                                Constraint(data = ConstraintData.DeviceIsLocked),
                            ),
                            mode = ConstraintMode.AND,
                        ),
                        ConstraintState(
                            constraints = setOf(
                                Constraint(
                                    data = ConstraintData.AppInForeground(packageName = "app"),
                                ),
                                Constraint(data = ConstraintData.DeviceIsUnlocked),
                            ),
                            mode = ConstraintMode.OR,
                        ),
                    ),
                ),
            )

            whenever(detectConstraintsUseCase.getSnapshot())
                .thenReturn(
                    TestConstraintSnapshot(
                        isWifiEnabled = true,
                        isLocked = true,
                        isLockscreenShowing = false,
                        appInForeground = "app",
                    ),
                )

            mockTriggerKeyInput(trigger.keys[0])

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and long and double press trigger times out`() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val longPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val doublePressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.DOUBLE_PRESS,
                    ),
                ),
                vibrate = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
                KeyMap(2, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(shortPressTrigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and double press trigger times out`() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val doublePressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.DOUBLE_PRESS,
                    ),
                ),
                vibrate = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(shortPressTrigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and double press trigger`() = runTest(testDispatcher) {
        // GIVEN
        val shortPressTrigger = Trigger(
            keys = listOf(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            vibrate = true,
        )

        val doublePressTrigger = Trigger(
            keys = listOf(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            vibrate = true,
        )

        loadKeyMaps(
            KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
            KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
        )

        // WHEN
        mockTriggerKeyInput(doublePressTrigger.keys[0])
        advanceUntilIdle()

        // THEN
        verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
    }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and long press trigger times out`() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val longPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                longPressDoubleVibration = true,
                vibrate = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(shortPressTrigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and long press trigger with double vibration and long press times out`() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val longPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                longPressDoubleVibration = true,
                vibrate = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(shortPressTrigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate twice with short press and long press trigger with double vibration`() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                vibrate = true,
            )

            val longPressTrigger = Trigger(
                keys = listOf(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                longPressDoubleVibration = true,
                vibrate = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(longPressTrigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(detectKeyMapsUseCase, times(2)).vibrate(VIBRATION_DURATION)
        }

    /**
     * #1507
     */
    @Test
    fun `vibrate once with short press and long press trigger`() = runTest(testDispatcher) {
        // GIVEN
        val shortPressTrigger = Trigger(
            keys = listOf(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            vibrate = true,
        )

        val longPressTrigger = Trigger(
            keys = listOf(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            vibrate = true,
        )

        loadKeyMaps(
            KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
            KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
        )

        // WHEN
        mockTriggerKeyInput(longPressTrigger.keys[0])
        advanceUntilIdle()

        // THEN
        verify(detectKeyMapsUseCase, times(1)).vibrate(VIBRATION_DURATION)
    }

    @Test
    fun `Sequence trigger with fingerprint gesture and key code`() = runTest(testDispatcher) {
        // GIVEN
        loadKeyMaps(
            KeyMap(
                trigger = sequenceTrigger(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                    FingerprintTriggerKey(
                        type = FingerprintGestureType.SWIPE_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                actionList = listOf(TEST_ACTION),
            ),
        )

        // WHEN
        inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN)
        inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP)
        controller.onFingerprintGesture(FingerprintGestureType.SWIPE_DOWN)

        // THEN
        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Input fingerprint gesture`() = runTest(testDispatcher) {
        // GIVEN
        loadKeyMaps(
            KeyMap(
                trigger = singleKeyTrigger(
                    FingerprintTriggerKey(
                        type = FingerprintGestureType.SWIPE_DOWN,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
                actionList = listOf(TEST_ACTION),
            ),
        )

        // WHEN
        controller.onFingerprintGesture(FingerprintGestureType.SWIPE_DOWN)

        // THEN
        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    /**
     * Issue #1386
     */
    @Test
    fun `Do not trigger both triggers if sequence trigger is triggered while waiting`() =
        runTest(testDispatcher) {
            // GIVEN
            val copyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_J))
            val copyAction = Action(data = ActionData.CopyText)

            val sequenceTrigger =
                sequenceTrigger(triggerKey(KeyEvent.KEYCODE_J), triggerKey(KeyEvent.KEYCODE_K))
            val enterAction = Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            loadKeyMaps(
                KeyMap(0, trigger = copyTrigger, actionList = listOf(copyAction)),
                KeyMap(1, trigger = sequenceTrigger, actionList = listOf(enterAction)),
            )

            // WHEN
            mockTriggerKeyInput(copyTrigger.keys[0])
            mockTriggerKeyInput(sequenceTrigger.keys[0])
            mockTriggerKeyInput(sequenceTrigger.keys[1])
            advanceTimeBy(SEQUENCE_TRIGGER_TIMEOUT)

            // THEN
            verify(performActionsUseCase, never()).perform(copyAction.data)
            verify(performActionsUseCase, times(1)).perform(enterAction.data)
        }

    /**
     * Issue #1386
     */
    @Test
    fun `Wait for longest sequence trigger timeout before triggering overlapping parallel triggers if multiple sequence triggers`() =
        runTest(testDispatcher) {
            // GIVEN
            val copyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_J))
            val copyAction = Action(data = ActionData.CopyText)

            val sequenceTrigger1 =
                Trigger(
                    keys = listOf(
                        triggerKey(KeyEvent.KEYCODE_J),
                        triggerKey(KeyEvent.KEYCODE_K),
                    ),
                    mode = TriggerMode.Sequence,
                    sequenceTriggerTimeout = 500,
                )
            val sequenceTriggerAction1 =
                Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            // This has a different second key.
            val sequenceTrigger2 =
                Trigger(
                    keys = listOf(
                        triggerKey(KeyEvent.KEYCODE_J),
                        triggerKey(KeyEvent.KEYCODE_A),
                    ),
                    mode = TriggerMode.Sequence,
                    sequenceTriggerTimeout = 1000,
                )
            val sequenceTriggerAction2 =
                Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            loadKeyMaps(
                KeyMap(0, trigger = copyTrigger, actionList = listOf(copyAction)),
                KeyMap(1, trigger = sequenceTrigger1, actionList = listOf(sequenceTriggerAction1)),
                KeyMap(2, trigger = sequenceTrigger2, actionList = listOf(sequenceTriggerAction2)),
            )

            // WHEN
            mockTriggerKeyInput(copyTrigger.keys[0])

            // THEN
            inOrder(performActionsUseCase) {
                // The single key trigger should not be executed straight away. Wait for
                // the longer sequence trigger delay.
                verify(performActionsUseCase, never()).perform(copyAction.data)

                // It still shouldn't be executed after the first sequence trigger delay.
                advanceTimeBy(500)
                verify(performActionsUseCase, never()).perform(copyAction.data)

                advanceTimeBy(1000)
                verify(performActionsUseCase, times(1)).perform(copyAction.data)
                verify(performActionsUseCase, never()).perform(sequenceTriggerAction1.data)
                verify(performActionsUseCase, never()).perform(sequenceTriggerAction2.data)
            }
        }

    /**
     * Issue #1386
     */
    @Test
    fun `Wait for sequence trigger timeout before triggering overlapping parallel triggers 1`() =
        runTest(testDispatcher) {
            // GIVEN
            val copyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_J))
            val copyAction = Action(data = ActionData.CopyText)

            val pasteTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_K))
            val pasteAction = Action(data = ActionData.PasteText)

            val sequenceTrigger =
                sequenceTrigger(triggerKey(KeyEvent.KEYCODE_J), triggerKey(KeyEvent.KEYCODE_K))
            val enterAction = Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            loadKeyMaps(
                KeyMap(0, trigger = copyTrigger, actionList = listOf(copyAction)),
                KeyMap(1, trigger = pasteTrigger, actionList = listOf(pasteAction)),
                KeyMap(2, trigger = sequenceTrigger, actionList = listOf(enterAction)),
            )

            // WHEN
            mockTriggerKeyInput(copyTrigger.keys[0])
            advanceTimeBy(SEQUENCE_TRIGGER_TIMEOUT)

            // THEN
            verify(performActionsUseCase, times(1)).perform(copyAction.data)
            verify(performActionsUseCase, never()).perform(pasteAction.data)
            verify(performActionsUseCase, never()).perform(enterAction.data)
        }

    /**
     * Issue #1386
     */
    @Test
    fun `Immediately trigger a key that is the 2nd key in a sequence trigger`() =
        runTest(testDispatcher) {
            // GIVEN
            val copyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_J))
            val copyAction = Action(data = ActionData.CopyText)

            val pasteTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_K))
            val pasteAction = Action(data = ActionData.PasteText)

            val sequenceTrigger =
                sequenceTrigger(triggerKey(KeyEvent.KEYCODE_J), triggerKey(KeyEvent.KEYCODE_K))
            val enterAction = Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            loadKeyMaps(
                KeyMap(0, trigger = copyTrigger, actionList = listOf(copyAction)),
                KeyMap(1, trigger = pasteTrigger, actionList = listOf(pasteAction)),
                KeyMap(2, trigger = sequenceTrigger, actionList = listOf(enterAction)),
            )

            // WHEN
            mockTriggerKeyInput(pasteTrigger.keys[0])

            // THEN
            verify(performActionsUseCase, never()).perform(copyAction.data)
            verify(performActionsUseCase, times(1)).perform(pasteAction.data)
            verify(performActionsUseCase, never()).perform(enterAction.data)
        }

    /**
     * Issue #1386
     */
    @Test
    fun `Do not trigger parallel trigger if a sequence trigger with the same keys is triggered`() =
        runTest(testDispatcher) {
            // GIVEN
            val copyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_J))
            val copyAction = Action(data = ActionData.CopyText)

            val pasteTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_K))
            val pasteAction = Action(data = ActionData.PasteText)

            val sequenceTrigger =
                sequenceTrigger(triggerKey(KeyEvent.KEYCODE_J), triggerKey(KeyEvent.KEYCODE_K))
            val enterAction = Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ENTER))

            loadKeyMaps(
                KeyMap(0, trigger = copyTrigger, actionList = listOf(copyAction)),
                KeyMap(1, trigger = pasteTrigger, actionList = listOf(pasteAction)),
                KeyMap(2, trigger = sequenceTrigger, actionList = listOf(enterAction)),
            )

            // WHEN
            mockTriggerKeyInput(sequenceTrigger.keys[0])
            mockTriggerKeyInput(sequenceTrigger.keys[1])

            // THEN
            verify(performActionsUseCase, never()).perform(copyAction.data)
            verify(performActionsUseCase, never()).perform(pasteAction.data)
            verify(performActionsUseCase, times(1)).perform(enterAction.data)
        }

    @Test
    fun `Hold down key event action while DPAD button is held down via motion events`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    clickType = ClickType.SHORT_PRESS,
                    requiresIme = true,
                    device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
                ),
            )

            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_Q),
                holdDown = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = trigger, actionList = listOf(action)),
            )

            inOrder(performActionsUseCase) {
                inputMotionEvent(axisHatX = -1.0f)
                verify(performActionsUseCase, times(1)).perform(action.data, InputEventAction.DOWN)

                delay(1000) // Hold down the DPAD button for 1 second.
                inputMotionEvent(axisHatX = 0.0f)
                verify(performActionsUseCase, times(1)).perform(action.data, InputEventAction.UP)
            }
        }

    @Test
    fun `Trigger short press key map from DPAD motion event while another DPAD button is held down`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    clickType = ClickType.SHORT_PRESS,
                    requiresIme = true,
                    device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
                ),
            )

            loadKeyMaps(
                KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)),
            )

            var motionEvent = createMotionEvent(axisHatY = 1.0f)
            val consumeDown1 = controller.onMotionEvent(motionEvent)
            assertThat(consumeDown1, `is`(false))

            motionEvent = motionEvent.copy(axisHatX = -1.0f)
            val consumeDown2 = controller.onMotionEvent(motionEvent)
            assertThat(consumeDown2, `is`(true))

            motionEvent = motionEvent.copy(axisHatX = 0.0f)
            val consumeUp2 = controller.onMotionEvent(motionEvent)
            assertThat(consumeUp2, `is`(true))

            motionEvent = motionEvent.copy(axisHatY = 0.0f)
            val consumeUp1 = controller.onMotionEvent(motionEvent)
            assertThat(consumeUp1, `is`(false))

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Trigger short press key map from DPAD motion event while a volume button is held down`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    clickType = ClickType.SHORT_PRESS,
                    requiresIme = true,
                    device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
                ),
            )

            loadKeyMaps(
                KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)),
            )

            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN)

            val consumeDown = inputMotionEvent(axisHatX = -1.0f)
            assertThat(consumeDown, `is`(true))

            val consumeUp = inputMotionEvent(axisHatX = 0.0f)

            assertThat(consumeUp, `is`(true))

            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun `Trigger long press key map from DPAD motion event`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            triggerKey(
                KeyEvent.KEYCODE_DPAD_LEFT,
                clickType = ClickType.LONG_PRESS,
                requiresIme = true,
                device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
            ),
        )

        loadKeyMaps(
            KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)),
        )

        val consumeDown = inputMotionEvent(axisHatX = -1.0f)
        assertThat(consumeDown, `is`(true))

        delay(LONG_PRESS_DELAY)

        val consumeUp = inputMotionEvent(axisHatX = 0.0f)

        assertThat(consumeUp, `is`(true))

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    @Test
    fun `Trigger short press key map from DPAD motion event`() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            triggerKey(
                KeyEvent.KEYCODE_DPAD_LEFT,
                clickType = ClickType.SHORT_PRESS,
                requiresIme = true,
                device = FAKE_CONTROLLER_TRIGGER_KEY_DEVICE,
            ),
        )

        loadKeyMaps(
            KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)),
        )

        val consumeDown = inputMotionEvent(axisHatX = -1.0f)
        assertThat(consumeDown, `is`(true))

        val consumeUp = inputMotionEvent(axisHatX = 0.0f)

        assertThat(consumeUp, `is`(true))

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    /**
     * Issue #491. While a DPAD button is held down many key events are sent with increasing
     * repeatCount values.
     */
    @Test
    fun `Trigger short press key map once when a key event is repeated`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    clickType = ClickType.SHORT_PRESS,
                    requiresIme = true,
                ),
            )

            loadKeyMaps(
                KeyMap(
                    0,
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION),
                ),
            )

            val consumeFirstDown =
                inputKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_DOWN, repeatCount = 0)

            assertThat(consumeFirstDown, `is`(true))

            repeat(10) { count ->
                val consumeRepeatedDown =
                    inputKeyEvent(
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.ACTION_DOWN,
                        repeatCount = count + 1,
                    )

                assertThat(consumeRepeatedDown, `is`(true))

                delay(50)
            }

            val consumeUp =
                inputKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_UP, repeatCount = 0)

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)

            assertThat(consumeUp, `is`(true))
        }

    /**
     * Issue #491. While a DPAD button is held down many key events are sent with increasing
     * repeatCount values. If a long press trigger is used then all these key events must
     * be consumed.
     */
    @Test
    fun `Consume repeated DPAD key events for a long press trigger`() = runTest(testDispatcher) {
        val longPressTrigger = singleKeyTrigger(
            triggerKey(
                KeyEvent.KEYCODE_DPAD_LEFT,
                clickType = ClickType.LONG_PRESS,
                requiresIme = true,
            ),
        )

        loadKeyMaps(
            KeyMap(
                0,
                trigger = longPressTrigger,
                actionList = listOf(TEST_ACTION),
            ),
        )

        repeat(20) { count ->
            val consumeDown =
                inputKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_DOWN, repeatCount = count)

            assertThat(consumeDown, `is`(true))

            delay(50)
        }

        val consumeUp =
            inputKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_UP, repeatCount = 0)

        assertThat(consumeUp, `is`(true))

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    /**
     * #1271 but with long press trigger instead of double press.
     */
    @Test
    fun `Trigger short press key map if constraints allow it and a long press key map to the same button is not allowed`() =
        runTest(testDispatcher) {
            val shortPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
            )
            val shortPressConstraints =
                ConstraintState(constraints = setOf(Constraint(data = ConstraintData.WifiOn)))

            val longPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )
            val doublePressConstraints =
                ConstraintState(constraints = setOf(Constraint(data = ConstraintData.WifiOff)))

            loadKeyMaps(
                KeyMap(
                    0,
                    trigger = shortPressTrigger,
                    actionList = listOf(TEST_ACTION),
                    constraintState = shortPressConstraints,
                ),
                KeyMap(
                    1,
                    trigger = longPressTrigger,
                    actionList = listOf(TEST_ACTION_2),
                    constraintState = doublePressConstraints,
                ),
            )

            // Only the short press trigger is allowed.
            val constraintSnapshot = TestConstraintSnapshot(isWifiEnabled = true)
            whenever(detectConstraintsUseCase.getSnapshot()).thenReturn(constraintSnapshot)

            mockTriggerKeyInput(shortPressTrigger.keys.first())

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
            verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
        }

    /**
     * #1271
     */
    @Test
    fun `ignore double press key maps overlapping short press key maps if the constraints aren't satisfied`() =
        runTest(testDispatcher) {
            val shortPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
            )
            val shortPressConstraints =
                ConstraintState(constraints = setOf(Constraint(data = ConstraintData.WifiOn)))

            val doublePressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.DOUBLE_PRESS),
            )
            val doublePressConstraints =
                ConstraintState(constraints = setOf(Constraint(data = ConstraintData.WifiOff)))

            loadKeyMaps(
                KeyMap(
                    0,
                    trigger = shortPressTrigger,
                    actionList = listOf(TEST_ACTION),
                    constraintState = shortPressConstraints,
                ),
                KeyMap(
                    1,
                    trigger = doublePressTrigger,
                    actionList = listOf(TEST_ACTION_2),
                    constraintState = doublePressConstraints,
                ),
            )

            // Only the short press trigger is allowed.
            val constraintSnapshot = TestConstraintSnapshot(isWifiEnabled = true)
            whenever(detectConstraintsUseCase.getSnapshot()).thenReturn(constraintSnapshot)

            mockTriggerKeyInput(shortPressTrigger.keys.first())

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
            verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
        }

    @Test
    fun `Don't imitate button if 1 long press trigger is successful and another with a longer delay fails`() =
        runTest(testDispatcher) {
            // GIVEN

            val longerTrigger =
                singleKeyTrigger(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                )
                    .copy(longPressDelay = 900)

            val shorterTrigger =
                singleKeyTrigger(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                )
                    .copy(longPressDelay = 500)

            loadKeyMaps(
                KeyMap(0, trigger = longerTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = shorterTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            inOrder(performActionsUseCase, detectKeyMapsUseCase) {
                // If only the shorter trigger is detected

                mockTriggerKeyInput(shorterTrigger.keys[0], 600L)

                verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
                verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )

                // If both triggers are detected

                mockTriggerKeyInput(shorterTrigger.keys[0], 1000L)

                verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
                verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )

                // If no triggers are detected
                mockTriggerKeyInput(shorterTrigger.keys[0], 100L)

                verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
                verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
                verify(detectKeyMapsUseCase, times(2)).imitateKeyEvent(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    /**
     * #739
     */
    @Test
    fun `Long press trigger shouldn't be triggered if the constraints are changed by the actions`() =
        runTest(testDispatcher) {
            // GIVEN
            val actionData = ActionData.Flashlight.Toggle(CameraLens.BACK, strengthPercent = null)

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                actionList = listOf(Action(data = actionData)),
                constraintState = ConstraintState(
                    constraints = setOf(
                        Constraint(data = ConstraintData.FlashlightOn(lens = CameraLens.BACK)),
                    ),
                ),
            )

            loadKeyMaps(keyMap)

            var isFlashlightEnabled = false

            // WHEN THEN
            whenever(detectConstraintsUseCase.getSnapshot()).then {
                mock<ConstraintSnapshot> {
                    on { isSatisfied(any()) }.then { isFlashlightEnabled }
                }
            }

            whenever(performActionsUseCase.perform(any(), any(), any())).doAnswer {
                isFlashlightEnabled = !isFlashlightEnabled
            }

            inOrder(performActionsUseCase) {
                // flashlight is initially disabled so don't trigger.
                mockTriggerKeyInput(keyMap.trigger.keys[0])
                verify(performActionsUseCase, never()).perform(any(), any(), any())

                isFlashlightEnabled = true
                // trigger because flashlight is enabled. Triggering the action will disable the flashlight.
                mockTriggerKeyInput(keyMap.trigger.keys[0])
                verify(performActionsUseCase, times(1)).perform(any(), any(), any())

                // Don't trigger because the flashlight is now disabled
                mockTriggerKeyInput(keyMap.trigger.keys[0])
                verify(performActionsUseCase, never()).perform(any(), any(), any())
            }
        }

    /**
     * #693
     */
    @Test
    fun `multiple key maps with the same long press trigger but different long press delays should all work`() =
        runTest(testDispatcher) {
            // GIVEN
            val keyMap1 = KeyMap(
                trigger = Trigger(
                    keys = listOf(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN)),
                    longPressDelay = 500,
                ),
                actionList = listOf(TEST_ACTION),
            )

            val keyMap2 = KeyMap(
                trigger = Trigger(
                    keys = listOf(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN)),
                    longPressDelay = 1000,
                ),
                actionList = listOf(TEST_ACTION_2),
            )

            loadKeyMaps(keyMap1, keyMap2)

            // WHEN
            inOrder(performActionsUseCase) {
                assertThat(
                    inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
                    `is`(true),
                )
                delay(600)
                assertThat(
                    inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
                    `is`(true),
                )
                advanceUntilIdle()

                // THEN
                verify(performActionsUseCase, times(1)).perform(keyMap1.actionList[0].data)

                // WHEN
                assertThat(
                    inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
                    `is`(true),
                )
                delay(1100)
                assertThat(
                    inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
                    `is`(true),
                )
                advanceUntilIdle()

                // THEN
                verify(performActionsUseCase, times(1)).perform(keyMap1.actionList[0].data)
                verify(performActionsUseCase, times(1)).perform(keyMap2.actionList[0].data)
            }
        }

    /**
     * #694
     */
    @Test
    fun `don't consume down and up event if no valid actions to perform`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val actionList = listOf(Action(data = ActionData.InputKeyEvent(2)))

            loadKeyMaps(KeyMap(trigger = trigger, actionList = actionList))

            // WHEN
            whenever(performActionsUseCase.getErrorSnapshot()).thenReturn(object :
                ActionErrorSnapshot {
                override fun getError(action: ActionData): KMError {
                    return KMError.NoCompatibleImeChosen
                }

                override fun getErrors(actions: List<ActionData>): Map<ActionData, KMError?> {
                    return mapOf(actionList[0].data to KMError.NoCompatibleImeChosen)
                }
            })

            assertThat(
                inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
                `is`(false),
            )
            assertThat(inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP), `is`(false))
            advanceUntilIdle()

            // THEN
            verify(performActionsUseCase, never()).perform(actionList[0].data)
        }

    /**
     * #689
     */
    @Test
    fun `perform all actions once when key map is triggered`() = runTest(testDispatcher) {
        // GIVEN
        val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

        val actionList = listOf(
            Action(data = ActionData.InputKeyEvent(1), delayBeforeNextAction = 1000),
            Action(data = ActionData.InputKeyEvent(2)),
        )

        loadKeyMaps(
            KeyMap(trigger = trigger, actionList = actionList),
        )

        // WHEN
        assertThat(inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN), `is`(true))
        assertThat(inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP), `is`(true))
        advanceUntilIdle()

        // THEN
        verify(performActionsUseCase, times(1)).perform(actionList[0].data)
        verify(performActionsUseCase, times(1)).perform(actionList[1].data)
    }

    /**
     * #663
     */
    @Test
    fun `action with repeat until limit reached shouldn't stop repeating when trigger is released`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val action = Action(
                data = ActionData.InputKeyEvent(1),
                repeat = true,
                repeatMode = RepeatMode.LIMIT_REACHED,
                repeatLimit = 2,
            )

            loadKeyMaps(
                KeyMap(trigger = trigger, actionList = listOf(action)),
            )

            // WHEN
            assertThat(
                inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
                `is`(true),
            )
            assertThat(inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP), `is`(true))
            advanceUntilIdle()

            // THEN
            // 3 times because it performs once and then repeats twice
            verify(performActionsUseCase, times(3)).perform(action.data)
        }

    @Test
    fun `key map with multiple actions and delay in between, perform all actions even when trigger is released`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                delayBeforeNextAction = 500,
            )

            val action2 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                delayBeforeNextAction = 1000,
            )

            val action3 = Action(
                data = ActionData.InputKeyEvent(keyCode = 3),
            )

            loadKeyMaps(
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(action1, action2, action3),
                ),
            )

            // WHEN

            // ensure consumed
            mockTriggerKeyInput(trigger.keys[0])

            // THEN

            advanceUntilIdle()
            verify(performActionsUseCase, times(1)).perform(action1.data)
            verify(performActionsUseCase, times(1)).perform(action2.data)
            verify(performActionsUseCase, times(1)).perform(action3.data)
        }

    @Test
    fun `multiple key maps with same trigger, perform both key maps`() = runTest(testDispatcher) {
        // GIVEN
        val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

        loadKeyMaps(
            KeyMap(
                trigger = trigger,
                actionList = listOf(TEST_ACTION),
            ),
            KeyMap(
                trigger = trigger,
                actionList = listOf(TEST_ACTION_2),
            ),
        )

        // WHEN

        // ensure consumed
        assertThat(
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
            `is`(true),
        )
        delay(50)
        assertThat(inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP), `is`(true))

        // THEN

        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
    }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until limit reached, then stop repeating when the limit has been reached and not when the trigger is released`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.LIMIT_REACHED,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0])
            advanceUntilIdle()

            // THEN
            verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
        }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when the trigger has been pressed again`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
                repeatLimit = 10,
                repeatRate = 100,
                repeatDelay = 100,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0])
            testScheduler.apply {
                advanceTimeBy(200)
                runCurrent()
            }
            mockTriggerKeyInput(keyMap.trigger.keys[0])

            // THEN
            verify(performActionsUseCase, times(4)).perform(action.data)
        }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when limit reached and trigger hasn't been pressed again`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0])
            testScheduler.apply {
                advanceTimeBy(5000)
                runCurrent()
            }
            mockTriggerKeyInput(keyMap.trigger.keys[0])

            // THEN
            // performed an extra 2 times each time the trigger is pressed. This is the expected behaviour even for the option to repeat until pressed again.
            verify(performActionsUseCase, times(action.repeatLimit!! + 2)).perform(action.data)
        }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until released with repeat limit, then stop repeating when the trigger has been released`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_RELEASED,
                repeatLimit = 10,
                repeatRate = 100,
                repeatDelay = 100,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0], delay = 300)

            // THEN
            verify(performActionsUseCase, times(3)).perform(action.data)
        }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until released with repeat limit, then stop repeating when the limit has been reached and the action is still being held down`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_RELEASED,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)

            // WHEN

            mockTriggerKeyInput(keyMap.trigger.keys[0], delay = 5000)

            // THEN

            verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
        }

    /**
     * issue #653
     */
    @Test
    fun `overlapping triggers 3`() = runTest(testDispatcher) {
        // GIVEN
        val keyMaps = arrayOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 45))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 81))),
            ),
        )

        loadKeyMaps(*keyMaps)

        inOrder(performActionsUseCase) {
            // WHEN
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                action = KeyEvent.ACTION_DOWN,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                action = KeyEvent.ACTION_UP,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_UP,
            )

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[1].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[0].actionList[0].data)

            // WHEN

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_UP,
            )

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[0].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[1].actionList[0].data)
        }
    }

    /**
     * issue #653
     */
    @Test
    fun `overlapping triggers 2`() = runTest(testDispatcher) {
        // GIVEN
        val keyMaps = arrayOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_P),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 45))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_META_LEFT),
                    triggerKey(KeyEvent.KEYCODE_P),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 81))),
            ),
        )

        loadKeyMaps(*keyMaps)

        inOrder(performActionsUseCase) {
            // WHEN
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_META_LEFT,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_META_LEFT_ON or KeyEvent.META_META_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_P,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_META_LEFT_ON or KeyEvent.META_META_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_META_LEFT,
                action = KeyEvent.ACTION_UP,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_P,
                action = KeyEvent.ACTION_UP,
            )

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[1].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[0].actionList[0].data)

            // WHEN
            mockParallelTrigger(keyMaps[0].trigger)

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[0].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[1].actionList[0].data)
        }
    }

    /**
     * issue #653
     */
    @Test
    fun `overlapping triggers 1`() = runTest(testDispatcher) {
        // GIVEN
        val keyMaps = arrayOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_CTRL_LEFT),
                    triggerKey(KeyEvent.KEYCODE_SHIFT_LEFT),
                    triggerKey(KeyEvent.KEYCODE_1),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 1))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_CTRL_LEFT),
                    triggerKey(KeyEvent.KEYCODE_1),
                ),
                actionList = listOf(Action(data = ActionData.InputKeyEvent(keyCode = 2))),
            ),
        )

        loadKeyMaps(*keyMaps)

        inOrder(performActionsUseCase) {
            // WHEN
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_CTRL_LEFT,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_SHIFT_LEFT,
                action = KeyEvent.ACTION_DOWN,
                metaState =
                KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON or
                    KeyEvent.META_SHIFT_LEFT_ON or
                    KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_1,
                action = KeyEvent.ACTION_DOWN,
                metaState =
                KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON or
                    KeyEvent.META_SHIFT_LEFT_ON or
                    KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_CTRL_LEFT,
                action = KeyEvent.ACTION_UP,
                metaState = KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_SHIFT_LEFT,
                action = KeyEvent.ACTION_UP,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_1,
                action = KeyEvent.ACTION_UP,
            )

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[0].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[1].actionList[0].data)

            // WHEN
            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_CTRL_LEFT,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_1,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_CTRL_LEFT,
                action = KeyEvent.ACTION_UP,
                metaState = KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_1,
                action = KeyEvent.ACTION_UP,
            )

            // THEN
            verify(performActionsUseCase, times(1)).perform(keyMaps[1].actionList[0].data)
            verify(performActionsUseCase, never()).perform(keyMaps[0].actionList[0].data)
        }
    }

    /**
     * issue #664
     */
    @Test
    fun `imitate button presses when a short press trigger with multiple keys fails`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger = parallelTrigger(
                triggerKey(keyCode = 1),
                triggerKey(keyCode = 2),
            )

            loadKeyMaps(
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION),
                ),
            )

            inOrder(detectKeyMapsUseCase, performActionsUseCase) {
                // WHEN
                inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_DOWN)
                inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_UP)

                // THEN
                verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                    keyCode = 1,
                    action = KeyEvent.ACTION_DOWN,
                )
                verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                    keyCode = 1,
                    action = KeyEvent.ACTION_UP,
                )
                verifyNoMoreInteractions()

                // verify nothing happens and no key events are consumed when the 2nd key in the trigger is pressed
                // WHEN
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_DOWN), `is`(false))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_UP), `is`(false))

                // THEN
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    keyCode = 1,
                    action = KeyEvent.ACTION_DOWN,
                )
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    keyCode = 1,
                    action = KeyEvent.ACTION_UP,
                )
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    keyCode = 2,
                    action = KeyEvent.ACTION_DOWN,
                )
                verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                    keyCode = 2,
                    action = KeyEvent.ACTION_UP,
                )
                verify(performActionsUseCase, never()).perform(action = TEST_ACTION.data)

                // verify the action is performed and no keys are imitated when triggering the key map
                // WHEN
                assertThat(inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_DOWN), `is`(true))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_DOWN), `is`(true))
                assertThat(inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_UP), `is`(true))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_UP), `is`(true))

                // THEN
                verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)

                // change the order of the keys being released
                // WHEN
                assertThat(inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_DOWN), `is`(true))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_DOWN), `is`(true))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_UP), `is`(true))
                assertThat(inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_UP), `is`(true))

                // THEN
                verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
            }
        }

    /**
     * issue #664
     */
    @Test
    fun `don't imitate button press when a short press trigger is triggered`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger = parallelTrigger(
                triggerKey(keyCode = 1),
                triggerKey(keyCode = 2),
            )

            loadKeyMaps(
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION),
                ),
            )

            // WHEN
            inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_DOWN)
            inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_DOWN)
            inputKeyEvent(keyCode = 1, action = KeyEvent.ACTION_UP)
            inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_UP)

            // THEN
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                keyCode = 1,
                action = KeyEvent.ACTION_DOWN,
            )
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                keyCode = 1,
                action = KeyEvent.ACTION_UP,
            )
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                keyCode = 2,
                action = KeyEvent.ACTION_DOWN,
            )
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                keyCode = 2,
                action = KeyEvent.ACTION_UP,
            )
        }

    /**
     * issue #662
     */
    @Test
    fun `don't repeat when trigger is released for an action that has these options when the trigger is held down`() =
        runTest(testDispatcher) {
            // GIVEN
            val action = Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                delayBeforeNextAction = 10,
                repeatDelay = 10,
                repeatRate = 190,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = 2)),
                actionList = listOf(action),
            )

            loadKeyMaps(keyMap)
            // WHEN

            mockTriggerKeyInput(triggerKey(keyCode = 2), delay = 1)

            // see if the action repeats
            testScope.testScheduler.apply {
                advanceTimeBy(500)
                runCurrent()
            }
            controller.reset()

            // THEN
            verify(performActionsUseCase, times(1)).perform(action.data)
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until released
     * 2. Long press, same single key, action that repeats
     *
     * Expected:
     * On short press: trigger but don't start repeating #1 and don't trigger #2
     * On long press: don't trigger #1 and start repeating #2
     */
    @Test
    fun `don't initialise repeating if repeat when trigger is released after failed long press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = Action(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
            )

            performActionsUseCase.inOrder {
                // when short press
                mockParallelTrigger(trigger1)
                delay(2000) // let it try to repeat

                // then
                verify(performActionsUseCase, times(1)).perform(action1.data)
                verifyNoMoreInteractions()

                // when long press
                mockParallelTrigger(trigger2, delay = 2000) // let it repeat

                // then
                verify(performActionsUseCase, atLeast(2)).perform(action2.data)
            }
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until released
     * 2. Double press, same single key, action that doesn't repeat
     *
     * Expected:
     * On short press: trigger but don't start repeating #1 and don't trigger #2
     * On double press: don't trigger #1 and trigger #2
     */
    @Test
    fun `don't initialise repeating if repeat when trigger is released after failed failed double press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action2 = Action(data = ActionData.InputKeyEvent(keyCode = 3))

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
            )

            performActionsUseCase.inOrder {
                // when short press
                mockParallelTrigger(trigger1)
                delay(2000) // let it repeat

                // then
                verify(performActionsUseCase, times(1)).perform(action1.data)
                verifyNoMoreInteractions()

                // when double press
                mockTriggerKeyInput(trigger2.keys[0])

                // then
                verify(performActionsUseCase, times(1)).perform(action2.data)
            }
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until released
     * 2. Long press, same single key, action that repeats
     * 3. Double press, same single key, action that doesn't repeat
     *
     * Expected:
     * On short press: trigger but don't repeat #1, don't trigger #2 don't trigger #3
     * On long press: don't trigger #1, start repeating #2, don't trigger #3
     * On double press: don't trigger #1, don't trigger #2, trigger #3
     */
    @Test
    fun `don't initialise repeating if repeat when trigger is released after failed double press and failed long press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = Action(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            val trigger3 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action3 = Action(data = ActionData.InputKeyEvent(keyCode = 4))

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
                KeyMap(2, trigger = trigger3, actionList = listOf(action3)),
            )

            performActionsUseCase.inOrder {
                // when short press
                mockParallelTrigger(trigger1)
                advanceUntilIdle()

                // then
                verify(performActionsUseCase, times(1)).perform(action1.data)
                verifyNoMoreInteractions()

                // when long press
                mockParallelTrigger(trigger2, delay = 2000) // let it repeat

                // then
                verify(performActionsUseCase, atLeast(2)).perform(action2.data)

                // when double press
                mockTriggerKeyInput(trigger3.keys[0])

                // then
                verify(performActionsUseCase, times(1)).perform(action3.data)
            }
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until pressed again
     * 2. Long press, same single key, action that repeats
     *
     * Expected:
     * On short press: start repeating #1 and don't trigger #2
     * On long press: don't trigger #1 and start repeating #2
     */
    @Test
    fun `initialise repeating if repeat until pressed again on failed long press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = Action(data = ActionData.InputKeyEvent(keyCode = 3), repeat = true)

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
            )

            performActionsUseCase.inOrder {
                // when short press
                mockParallelTrigger(trigger1)
                delay(2000) // let it repeat

                // then
                mockParallelTrigger(trigger1) // press the key again to stop it repeating

                verify(performActionsUseCase, atLeast(2)).perform(action1.data)
                verifyNoMoreInteractions()

                // when long press
                mockParallelTrigger(trigger2, delay = 2000) // let it repeat

                // then
                verify(performActionsUseCase, atLeast(2)).perform(action2.data)
            }
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until pressed again
     * 2. Double press, same single key, action that doesn't repeat
     *
     * Expected:
     * On short press: start repeating #1 and don't trigger #2
     * On double press: don't trigger #1 and trigger #2
     */
    @Test
    fun `initialise repeating if repeat until pressed again on failed double press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action2 = Action(data = ActionData.InputKeyEvent(keyCode = 3))

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
            )

            performActionsUseCase.inOrder {
                // when short press
                mockParallelTrigger(trigger1)
                delay(2000) // let it repeat

                // then

                mockParallelTrigger(trigger1) // press the key again to stop it repeating
                advanceUntilIdle()

                verify(performActionsUseCase, atLeast(2)).perform(action1.data)
                verifyNoMoreInteractions()

                // when double press
                mockTriggerKeyInput(trigger2.keys[0])
                advanceUntilIdle()

                // then
                verify(performActionsUseCase, times(1)).perform(action2.data)
            }
        }

    /**
     * See #626 in issue tracker
     *
     * Key maps:
     * 1. Short press, single key, action that repeats until pressed again
     * 2. Long press, same single key, action that repeats
     * 3. Double press, same single key, action that doesn't repeat
     *
     * Expected:
     * On short press: start repeating #1, don't trigger #2 don't trigger #3
     * On long press: don't trigger #1, start repeating #2, don't trigger #3
     * On double press: don't trigger #1, don't trigger #2, trigger #3
     */
    @Test
    fun `initialise repeating if repeat until pressed again on failed double press and failed long press`() =
        runTest(testDispatcher) {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = Action(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = Action(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            val trigger3 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action3 = Action(data = ActionData.InputKeyEvent(keyCode = 4))

            loadKeyMaps(
                KeyMap(0, trigger = trigger1, actionList = listOf(action1)),
                KeyMap(1, trigger = trigger2, actionList = listOf(action2)),
                KeyMap(2, trigger = trigger3, actionList = listOf(action3)),
            )

            // when short press
            mockParallelTrigger(trigger1)

            delay(2000) // let it repeat

            performActionsUseCase.inOrder {
                // then
                mockParallelTrigger(trigger1) // press the key again to stop it repeating
                advanceUntilIdle()

                verify(performActionsUseCase, atLeast(2)).perform(action1.data)
                verifyNoMoreInteractions()

                // when long press
                mockParallelTrigger(trigger2, delay = 2000) // let it repeat

                // then
                verify(performActionsUseCase, atLeast(2)).perform(action2.data)

                // have a delay after a long press of the key is released so a double press isn't detected
                delay(1000)

                // when double press
                mockTriggerKeyInput(trigger3.keys[0])

                // then
                verify(performActionsUseCase, times(1)).perform(action3.data)
                verifyNoMoreInteractions()
            }
        }

    /**
     * this helped fix issue #608
     */
    @Test
    fun `short press key and double press same key sequence trigger, double press key, don't perform action`() =
        runTest(testDispatcher) {
            val trigger = sequenceTrigger(
                triggerKey(KeyEvent.KEYCODE_A),
                triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.DOUBLE_PRESS),
            )

            loadKeyMaps(
                KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)),
            )

            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.DOUBLE_PRESS))

            verify(performActionsUseCase, never()).perform(any(), any(), any())

            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A))
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.DOUBLE_PRESS))

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    /**
     * issue #563
     */
    @Test
    fun sendKeyEventActionWhenImitatingButtonPresses() = runTest(testDispatcher) {
        val trigger = singleKeyTrigger(
            triggerKey(
                keyCode = KeyEvent.KEYCODE_META_LEFT,
                device = FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
            ),
        )

        val action = Action(
            data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_META_LEFT),
            holdDown = true,
        )

        loadKeyMaps(KeyMap(trigger = trigger, actionList = listOf(action)))

        val metaState = KeyEvent.META_META_ON.withFlag(KeyEvent.META_META_LEFT_ON)

        inOrder(detectKeyMapsUseCase, performActionsUseCase) {
            inputKeyEvent(
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 117,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_E,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 33,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.ACTION_UP,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 117,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_E,
                KeyEvent.ACTION_UP,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                scanCode = 33,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.DOWN,
                metaState,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                KeyEvent.ACTION_DOWN,
                scanCode = 33,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.UP,
                0,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                KeyEvent.KEYCODE_E,
                0,
                FAKE_KEYBOARD_DEVICE_ID,
                KeyEvent.ACTION_UP,
                scanCode = 33,
            )

            inputKeyEvent(
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 117,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_E,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 33,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_E,
                KeyEvent.ACTION_UP,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState,
                scanCode = 33,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_META_LEFT,
                KeyEvent.ACTION_UP,
                triggerKeyDeviceToInputDevice(
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    FAKE_KEYBOARD_DEVICE_ID,
                ),
                metaState = 0,
                scanCode = 117,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.DOWN,
                metaState,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                KeyEvent.ACTION_DOWN,
                scanCode = 33,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                KeyEvent.ACTION_UP,
                scanCode = 33,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.UP,
                0,
            )

            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `parallel trigger with 2 keys and the 2nd key is another trigger, press 2 key trigger, only the action for 2 key trigger should be performed `() =
        runTest(testDispatcher) {
            // GIVEN
            val twoKeyTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_SHIFT_LEFT),
                triggerKey(KeyEvent.KEYCODE_A),
            )

            val oneKeyTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A),
            )

            loadKeyMaps(
                KeyMap(0, trigger = oneKeyTrigger, actionList = listOf(TEST_ACTION_2)),
                KeyMap(1, trigger = twoKeyTrigger, actionList = listOf(TEST_ACTION)),
            )

            inOrder(performActionsUseCase) {
                // test 1. test triggering 2 key trigger
                // WHEN
                inputKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.ACTION_DOWN)
                inputKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.ACTION_DOWN)

                inputKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.ACTION_UP)
                inputKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.ACTION_UP)
                // THEN
                verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
                verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)

                // test 2. test triggering 1 key trigger
                // WHEN
                inputKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.ACTION_DOWN)

                inputKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.ACTION_UP)
                advanceUntilIdle()

                // THEN
                verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
                verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
            }
        }

    @Test
    fun `trigger for a specific device and trigger for any device, input trigger from a different device, only detect trigger for any device`() =
        runTest(testDispatcher) {
            // GIVEN
            val triggerKeyboard = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE),
            )

            val triggerAnyDevice = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    device = KeyEventTriggerDevice.Any,
                ),
            )

            loadKeyMaps(
                KeyMap(0, trigger = triggerKeyboard, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = triggerAnyDevice, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE))

            // THEN
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
        }

    @Test
    fun `trigger for a specific device, input trigger from a different device, do not detect trigger`() =
        runTest(testDispatcher) {
            // GIVEN
            val triggerHeadphone = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A, FAKE_HEADPHONE_TRIGGER_KEY_DEVICE),
            )

            loadKeyMaps(
                KeyMap(0, trigger = triggerHeadphone, actionList = listOf(TEST_ACTION)),
            )

            // WHEN
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE))

            // THEN
            verify(performActionsUseCase, never()).perform(any(), any(), any())
        }

    @Test
    fun `long press trigger and action with Hold Down until pressed again flag, input valid long press, hold down until long pressed again`() =
        runTest(testDispatcher) {
            // GIVEN
            val trigger =
                singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS))

            val action = Action(
                data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_B),
                holdDown = true,
                stopHoldDownWhenTriggerPressedAgain = true,
            )

            val keymap = KeyMap(
                0,
                trigger = trigger,
                actionList = listOf(action),
            )

            loadKeyMaps(keymap)

            // WHEN
            mockTriggerKeyInput(trigger.keys[0])

            // THEN
            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.DOWN,
            )

            // WHEN
            mockTriggerKeyInput(trigger.keys[0])

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventAction.UP,
            )
        }

    /**
     * #478
     */
    @Test
    fun `trigger with modifier key and modifier keycode action, don't include metastate from the trigger modifier key when an unmapped modifier key is pressed`() =
        runTest(testDispatcher) {
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_CTRL_LEFT))

            loadKeyMaps(
                KeyMap(
                    0,
                    trigger = trigger,
                    actionList = listOf(
                        Action(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ALT_LEFT)),
                    ),
                ),
            )

            // imitate how modifier keys are sent on Android by also changing the metastate of the keyevent

            inputKeyEvent(
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.ACTION_DOWN,
                metaState =
                KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON +
                    KeyEvent.META_SHIFT_ON,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_C,
                KeyEvent.ACTION_DOWN,
                metaState =
                KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON +
                    KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.ACTION_UP,
                metaState =
                KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON +
                    KeyEvent.META_SHIFT_ON,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.ACTION_UP,
                metaState = KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(KeyEvent.KEYCODE_C, KeyEvent.ACTION_UP)

            inOrder(detectKeyMapsUseCase) {
                verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                    any(),
                    metaState = eq(
                        KeyEvent.META_ALT_LEFT_ON + KeyEvent.META_ALT_ON +
                            KeyEvent.META_SHIFT_LEFT_ON +
                            KeyEvent.META_SHIFT_ON,
                    ),
                    any(),
                    any(),
                    any(),
                    any(),
                )

                verify(detectKeyMapsUseCase, times(1)).imitateKeyEvent(
                    any(),
                    metaState = eq(0),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `2x key sequence trigger and 3x key sequence trigger with the last 2 keys being the same, trigger 3x key trigger, ignore the first 2x key trigger`() =
        runTest(testDispatcher) {
            val firstTrigger = sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = KeyEventTriggerDevice.Any,
                ),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            val secondTrigger = sequenceTrigger(
                triggerKey(KeyEvent.KEYCODE_HOME),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = KeyEventTriggerDevice.Any,
                ),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            loadKeyMaps(
                KeyMap(0, trigger = firstTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = secondTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_HOME))
            mockTriggerKeyInput(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = KeyEventTriggerDevice.Any,
                ),
            )
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_UP))

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
        }

    @Test
    fun `2x key long press parallel trigger with HOME or RECENTS keycode, trigger successfully, don't do normal action`() =
        runTest(testDispatcher) {
            /*
            HOME
             */

            val homeTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_HOME, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            loadKeyMaps(
                KeyMap(0, trigger = homeTrigger, actionList = listOf(TEST_ACTION)),
            )

            val consumedHomeDown =
                inputKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, FAKE_INTERNAL_DEVICE)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, FAKE_INTERNAL_DEVICE)

            advanceUntilIdle()

            inputKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP, FAKE_INTERNAL_DEVICE)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, FAKE_INTERNAL_DEVICE)

            assertThat(consumedHomeDown, `is`(true))

            /*
            RECENTS
             */

            val recentsTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_APP_SWITCH, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            loadKeyMaps(
                KeyMap(0, trigger = recentsTrigger, actionList = listOf(TEST_ACTION)),
            )

            val consumedRecentsDown =
                inputKeyEvent(
                    KeyEvent.KEYCODE_APP_SWITCH,
                    KeyEvent.ACTION_DOWN,
                    FAKE_INTERNAL_DEVICE,
                )
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, FAKE_INTERNAL_DEVICE)

            advanceUntilIdle()

            inputKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_UP, FAKE_INTERNAL_DEVICE)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, FAKE_INTERNAL_DEVICE)

            assertThat(consumedRecentsDown, `is`(true))
        }

    @Test
    fun shortPressTriggerDoublePressTrigger_holdDown_onlyDetectDoublePressTrigger() =
        runTest(testDispatcher) {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val doublePressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.DOUBLE_PRESS),
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            )

            // then
            // the first action performed shouldn't be the short press action
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)

            /*
            rerun the test to see if the short press trigger action is performed correctly.
             */

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            advanceUntilIdle()

            // then
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun shortPressTriggerLongPressTrigger_holdDown_onlyDetectLongPressTrigger() =
        runTest(testDispatcher) {
            // GIVEN
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val longPressTrigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.LONG_PRESS,
                ),
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            mockTriggerKeyInput(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )
            advanceUntilIdle()

            // THEN
            // the first action performed shouldn't be the short press action
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)

            // WHEN
            // rerun the test to see if the short press trigger action is performed correctly.
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            // THEN
            // the first action performed shouldn't be the short press action
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    @Parameters(method = "params_repeatAction")
    fun parallelTrigger_holdDown_repeatAction10Times(description: String, trigger: Trigger) =
        runTest(testDispatcher) {
            // given
            val action = Action(
                data = ActionData.Volume.Up(showVolumeUi = false),
                repeat = true,
            )

            loadKeyMaps(KeyMap(0, trigger = trigger, actionList = listOf(action)))

            when (trigger.mode) {
                is TriggerMode.Parallel -> mockParallelTrigger(trigger, delay = 2000L)
                TriggerMode.Undefined -> mockTriggerKeyInput(trigger.keys[0], delay = 2000L)
                TriggerMode.Sequence -> {}
            }

            verify(performActionsUseCase, atLeast(10)).perform(action.data)
        }

    fun params_repeatAction() = listOf(
        arrayOf(
            "long press multiple keys",
            parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP, clickType = ClickType.LONG_PRESS),
            ),
        ),
        arrayOf(
            "long press single key",
            singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            ),
        ),
        arrayOf(
            "short press multiple keys",
            parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            ),
        ),
        arrayOf(
            "short press single key",
            singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            ),
        ),
    )

    @Test
    @Parameters(method = "params_dualParallelTrigger_input2ndKey_doNotConsumeUp")
    fun dualParallelTrigger_input2ndKey_doNotConsumeUp(description: String, trigger: Trigger) =
        runTest(testDispatcher) {
            // given
            loadKeyMaps(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

            // when
            (trigger.keys[1] as KeyEventTriggerKey).let {
                inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_DOWN,
                    triggerKeyDeviceToInputDevice(it.device),
                )
            }

            (trigger.keys[1] as KeyEventTriggerKey).let {
                val consumed = inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(it.device),
                )

                // then
                assertThat(consumed, `is`(false))
            }
        }

    fun params_dualParallelTrigger_input2ndKey_doNotConsumeUp() = listOf(
        arrayOf(
            "long press",
            parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP, clickType = ClickType.LONG_PRESS),
            ),
        ),

        arrayOf(
            "short press",
            parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            ),
        ),
    )

    @Test
    fun dualShortPressParallelTrigger_validInput_consumeUp() = runTest(testDispatcher) {
        // given
        val trigger = parallelTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
        )

        loadKeyMaps(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

        // when
        for (key in trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
            inputKeyEvent(
                key.keyCode,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(key.device),
            )
        }

        var consumedUpCount = 0

        for (key in trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
            val consumed =
                inputKeyEvent(
                    key.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(key.device),
                )

            if (consumed) {
                consumedUpCount += 1
            }
        }

        // then
        assertThat(consumedUpCount, `is`(2))
    }

    @Test
    fun dualLongPressParallelTrigger_validInput_consumeUp() = runTest(testDispatcher) {
        // given
        val trigger = parallelTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP, clickType = ClickType.LONG_PRESS),
        )

        loadKeyMaps(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

        // when
        for (key in trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
            inputKeyEvent(
                key.keyCode,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(key.device),
            )
        }

        advanceUntilIdle()

        var consumedUpCount = 0

        for (key in trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
            val consumed =
                inputKeyEvent(
                    key.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(key.device),
                )

            if (consumed) {
                consumedUpCount += 1
            }
        }

        // then
        assertThat(consumedUpCount, `is`(2))
    }

    @Test
    fun keymappedToLongPressAndDoublePress_invalidLongPress_imitateOnce() =
        runTest(testDispatcher) {
            // given
            val longPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            val doublePressTrigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            )

            loadKeyMaps(
                KeyMap(0, trigger = longPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            advanceUntilIdle()

            // then
            verify(detectKeyMapsUseCase, times(1))
                .imitateKeyEvent(
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                    action = KeyEvent.ACTION_DOWN,
                )
            verify(detectKeyMapsUseCase, times(1))
                .imitateKeyEvent(
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                    action = KeyEvent.ACTION_UP,
                )
        }

    @Test
    fun keymappedToSingleShortPressAndLongPress_validShortPress_onlyPerformActiondoNotImitateKey() =
        runTest(testDispatcher) {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val longPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            // then
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
            verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun keymappedToShortPressAndDoublePress_validShortPress_onlyPerformActionDoNotImitateKey() =
        runTest(testDispatcher) {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val doublePressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.DOUBLE_PRESS),
            )

            loadKeyMaps(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            advanceUntilIdle()

            // then
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)

            // wait for the double press to try and imitate the key.
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun `singleKeyTriggerAndShortPressParallelTriggerWithSameInitialKey validSingleKeyTriggerInput onlyPerformActiondoNotImitateKey`() =
        runTest(testDispatcher) {
            // given
            val singleKeyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val parallelTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            loadKeyMaps(
                KeyMap(0, trigger = singleKeyTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = parallelTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            // then
            verify(detectKeyMapsUseCase, never()).imitateKeyEvent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun longPressSequenceTrigger_invalidLongPress_keyImitated() = runTest(testDispatcher) {
        val trigger = sequenceTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
        )

        loadKeyMaps(
            KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)),
        )

        mockTriggerKeyInput(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            delay = 100L,
        )

        verify(detectKeyMapsUseCase, times(1))
            .imitateKeyEvent(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                action = KeyEvent.ACTION_DOWN,
            )

        verify(detectKeyMapsUseCase, times(1))
            .imitateKeyEvent(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN, action = KeyEvent.ACTION_UP)
    }

    @Test
    @Parameters(method = "params_multipleActionsPerformed")
    fun validInput_multipleActionsPerformed(description: String, trigger: Trigger) =
        runTest(testDispatcher) {
            val actionList = listOf(TEST_ACTION, TEST_ACTION_2)
            // GIVEN
            loadKeyMaps(
                KeyMap(trigger = trigger, actionList = actionList),
            )

            // WHEN
            if (trigger.mode is TriggerMode.Parallel) {
                mockParallelTrigger(trigger)
            } else {
                trigger.keys.forEach {
                    mockTriggerKeyInput(it)
                }
            }

            // THEN
            actionList.forEach { action ->
                verify(performActionsUseCase, times(1)).perform(action.data)
            }
        }

    fun params_multipleActionsPerformed() = listOf(
        arrayOf(
            "undefined",
            singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                ),
            ),
        ),
        arrayOf(
            "sequence",
            sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                ),
            ),
        ),
        arrayOf(
            "parallel",
            parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            ),
        ),
    )

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun invalidInput_downNotConsumed(description: String, keyMap: KeyMap) =
        runTest(testDispatcher) {
            // GIVEN
            loadKeyMaps(keyMap)

            // WHEN
            var consumedCount = 0

            for (key in keyMap.trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
                val consumed =
                    inputKeyEvent(
                        999,
                        KeyEvent.ACTION_DOWN,
                        triggerKeyDeviceToInputDevice(key.device),
                    )

                if (consumed) {
                    consumedCount++
                }
            }

            // THEN
            assertThat(consumedCount, `is`(0))
        }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun validInput_downConsumed(description: String, keyMap: KeyMap) = runTest(testDispatcher) {
        // GIVEN
        loadKeyMaps(keyMap)

        var consumedCount = 0

        for (key in keyMap.trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
            val consumed =
                inputKeyEvent(
                    key.keyCode,
                    KeyEvent.ACTION_DOWN,
                    triggerKeyDeviceToInputDevice(key.device),
                )

            if (consumed) {
                consumedCount++
            }
        }

        assertThat(consumedCount, `is`(keyMap.trigger.keys.size))
    }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinationsdoNotConsume")
    @TestCaseName("{0}")
    fun validInput_doNotConsumeFlag_doNotConsumeDown(description: String, keyMap: KeyMap) =
        runTest(testDispatcher) {
            loadKeyMaps(keyMap)

            var consumedCount = 0

            for (key in keyMap.trigger.keys.mapNotNull { it as? KeyEventTriggerKey }) {
                val consumed =
                    inputKeyEvent(
                        key.keyCode,
                        KeyEvent.ACTION_DOWN,
                        triggerKeyDeviceToInputDevice(key.device),
                    )

                if (consumed) {
                    consumedCount++
                }
            }

            assertThat(consumedCount, `is`(0))
        }

    fun params_allTriggerKeyCombinationsdoNotConsume(): List<Array<Any>> {
        val triggerAndDescriptions = listOf(
            "undefined single short-press this-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    consume = false,
                ),
            ),
            "undefined single long-press this-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),
            "undefined single double-press this-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "undefined single short-press any-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    consume = false,
                ),
            ),
            "undefined single long-press any-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),
            "undefined single double-press any-device, do not consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple short-press this-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple long-press this-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple double-press this-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix this-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix external-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple short-press mixed-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple long-press mixed-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple double-press mixed-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix mixed-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix mixed-device, do not consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple short-press this-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple long-press this-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple short-press external-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple long-press external-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple short-press mix-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple long-press mix-device, do not consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),
        )

        return triggerAndDescriptions.mapIndexed { i, triggerAndDescription ->
            arrayOf(
                triggerAndDescription.first,
                KeyMap(
                    i.toLong(),
                    trigger = triggerAndDescription.second,
                    actionList = listOf(TEST_ACTION),
                ),
            )
        }
    }

    fun params_allTriggerKeyCombinations(): List<Array<Any>> {
        val triggerAndDescriptions = listOf(
            "undefined single short-press this-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                ),
            ),
            "undefined single long-press this-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "undefined single double-press this-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "undefined single short-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                ),
            ),
            "undefined single long-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "undefined single double-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "sequence multiple short-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "sequence multiple long-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "sequence multiple double-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix external-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "sequence multiple short-press mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "sequence multiple long-press mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "sequence multiple double-press mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "parallel multiple short-press this-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "parallel multiple long-press this-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "parallel multiple short-press external-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "parallel multiple long-press external-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "parallel multiple short-press mix-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "parallel multiple long-press mix-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEventTriggerDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEventTriggerDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
        )

        return triggerAndDescriptions.mapIndexed { i, triggerAndDescription ->
            arrayOf(
                triggerAndDescription.first,
                KeyMap(
                    i.toLong(),
                    trigger = triggerAndDescription.second,
                    actionList = listOf(TEST_ACTION),
                ),
            )
        }
    }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinations")
    @TestCaseName("{0}")
    fun validInput_actionPerformed(description: String, keyMap: KeyMap) = runTest(testDispatcher) {
        // GIVEN
        loadKeyMaps(keyMap)

        if (keyMap.trigger.mode is TriggerMode.Parallel) {
            // WHEN
            mockParallelTrigger(keyMap.trigger)
            advanceUntilIdle()
        } else {
            // WHEN
            keyMap.trigger.keys.forEach {
                mockTriggerKeyInput(it)
            }

            advanceUntilIdle()
        }

        // THEN
        verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
    }

    private suspend fun mockTriggerKeyInput(key: TriggerKey, delay: Long? = null) {
        if (key !is KeyEventTriggerKey) {
            throw IllegalArgumentException("Key must be a KeyEventTriggerKey")
        }

        val inputDevice = triggerKeyDeviceToInputDevice(key.device)
        val pressDuration: Long = delay ?: when (key.clickType) {
            ClickType.LONG_PRESS -> LONG_PRESS_DELAY + 100L
            else -> 50L
        }

        inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, inputDevice)

        when (key.clickType) {
            ClickType.SHORT_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, inputDevice)
            }

            ClickType.LONG_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, inputDevice)
            }

            ClickType.DOUBLE_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, inputDevice)
                delay(pressDuration)

                inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, inputDevice)
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, inputDevice)
            }
        }
    }

    private suspend fun mockEvdevKeyInput(
        key: TriggerKey,
        evdevDevice: EvdevDeviceInfo,
        delay: Long? = null,
    ) {
        if (key !is EvdevTriggerKey) {
            throw IllegalArgumentException("Key must be an EvdevTriggerKey")
        }

        val pressDuration: Long = delay ?: when (key.clickType) {
            ClickType.LONG_PRESS -> LONG_PRESS_DELAY + 100L
            else -> 50L
        }

        inputDownEvdevEvent(key.keyCode, key.scanCode, evdevDevice)

        when (key.clickType) {
            ClickType.SHORT_PRESS -> {
                delay(pressDuration)
                inputUpEvdevEvent(key.keyCode, key.scanCode, evdevDevice)
            }

            ClickType.LONG_PRESS -> {
                delay(pressDuration)
                inputUpEvdevEvent(key.keyCode, key.scanCode, evdevDevice)
            }

            ClickType.DOUBLE_PRESS -> {
                delay(pressDuration)
                inputUpEvdevEvent(key.keyCode, key.scanCode, evdevDevice)
                delay(pressDuration)

                inputDownEvdevEvent(key.keyCode, key.scanCode, evdevDevice)
                delay(pressDuration)
                inputUpEvdevEvent(key.keyCode, key.scanCode, evdevDevice)
            }
        }
    }

    private fun createMotionEvent(
        axisHatX: Float = 0.0f,
        axisHatY: Float = 0.0f,
        device: InputDeviceInfo = FAKE_CONTROLLER_INPUT_DEVICE,
    ): KMGamePadEvent {
        return KMGamePadEvent(
            metaState = 0,
            device = device,
            axisHatX = axisHatX,
            axisHatY = axisHatY,
            eventTime = System.currentTimeMillis(),
        )
    }

    private fun inputMotionEvent(
        axisHatX: Float = 0.0f,
        axisHatY: Float = 0.0f,
        device: InputDeviceInfo = FAKE_CONTROLLER_INPUT_DEVICE,
    ): Boolean = controller.onMotionEvent(
        KMGamePadEvent(
            metaState = 0,
            device = device,
            axisHatX = axisHatX,
            axisHatY = axisHatY,
            eventTime = System.currentTimeMillis(),
        ),
    )

    private fun inputKeyEvent(
        keyCode: Int,
        action: Int,
        device: InputDeviceInfo = FAKE_INTERNAL_DEVICE,
        metaState: Int? = null,
        scanCode: Int = 0,
        repeatCount: Int = 0,
    ): Boolean = controller.onInputEvent(
        KMKeyEvent(
            keyCode = keyCode,
            action = action,
            metaState = metaState ?: 0,
            scanCode = scanCode,
            device = device,
            repeatCount = repeatCount,
            source = 0,
            eventTime = System.currentTimeMillis(),
        ),
    )

    private fun inputDownEvdevEvent(keyCode: Int, scanCode: Int, device: EvdevDeviceInfo): Boolean =
        controller.onInputEvent(
            KMEvdevEvent(
                type = KMEvdevEvent.TYPE_KEY_EVENT,
                device = EvdevDeviceHandle(
                    path = "/dev/input${device.name}",
                    name = device.name,
                    bus = device.bus,
                    vendor = device.vendor,
                    product = device.product,
                ),
                code = scanCode,
                androidCode = keyCode,
                value = KMEvdevEvent.VALUE_DOWN,
                timeSec = testScope.currentTime,
                timeUsec = 0,
            ),
        )

    private fun inputUpEvdevEvent(keyCode: Int, scanCode: Int, device: EvdevDeviceInfo): Boolean =
        controller.onInputEvent(
            KMEvdevEvent(
                type = KMEvdevEvent.TYPE_KEY_EVENT,
                device = EvdevDeviceHandle(
                    path = "/dev/input${device.name}",
                    name = device.name,
                    bus = device.bus,
                    vendor = device.vendor,
                    product = device.product,
                ),
                code = scanCode,
                androidCode = keyCode,
                value = KMEvdevEvent.VALUE_UP,
                timeSec = testScope.currentTime,
                timeUsec = 0,
            ),
        )

    private suspend fun mockParallelTrigger(trigger: Trigger, delay: Long? = null) {
        require(trigger.mode is TriggerMode.Parallel)
        require(trigger.keys.all { it is KeyEventTriggerKey })

        for (key in trigger.keys) {
            if (key !is KeyEventTriggerKey) {
                continue
            }

            val inputDevice = triggerKeyDeviceToInputDevice(key.device)

            inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, inputDevice)
        }

        if (delay != null) {
            delay(delay)
        } else {
            when (trigger.mode.clickType) {
                ClickType.SHORT_PRESS -> delay(50)
                ClickType.LONG_PRESS -> delay(LONG_PRESS_DELAY + 100L)
                ClickType.DOUBLE_PRESS -> {}
            }
        }

        for (key in trigger.keys) {
            if (key !is KeyEventTriggerKey) {
                continue
            }

            val inputDevice = triggerKeyDeviceToInputDevice(key.device)

            inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, inputDevice)
        }
    }

    private fun triggerKeyDeviceToInputDevice(
        device: KeyEventTriggerDevice,
        deviceId: Int = 0,
        isGameController: Boolean = false,
    ): InputDeviceInfo = when (device) {
        KeyEventTriggerDevice.Any -> InputDeviceInfo(
            descriptor = "any_device",
            name = "any_device_name",
            isExternal = false,
            id = deviceId,
            isGameController = isGameController,
            sources = if (isGameController) {
                InputDevice.SOURCE_GAMEPAD
            } else {
                InputDevice.SOURCE_KEYBOARD
            },
        )

        is KeyEventTriggerDevice.External -> InputDeviceInfo(
            descriptor = device.descriptor,
            name = "device_name",
            isExternal = true,
            id = deviceId,
            isGameController = isGameController,
            sources = if (isGameController) {
                InputDevice.SOURCE_GAMEPAD
            } else {
                InputDevice.SOURCE_KEYBOARD
            },
        )

        KeyEventTriggerDevice.Internal -> InputDeviceInfo(
            descriptor = "internal_device",
            name = "internal_device_name",
            isExternal = false,
            id = deviceId,
            isGameController = isGameController,
            sources = if (isGameController) {
                InputDevice.SOURCE_GAMEPAD
            } else {
                InputDevice.SOURCE_KEYBOARD
            },
        )
    }
}

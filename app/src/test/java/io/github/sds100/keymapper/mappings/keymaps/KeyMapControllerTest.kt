package io.github.sds100.keymapper.mappings.keymaps

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.KeyMapController
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineExceptionHandler
import kotlinx.coroutines.test.createTestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 17/05/2020.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class KeyMapControllerTest {

    companion object {
        private const val FAKE_KEYBOARD_DEVICE_ID = 123
        private const val FAKE_KEYBOARD_DESCRIPTOR = "fake_keyboard"
        private val FAKE_KEYBOARD_TRIGGER_KEY_DEVICE = TriggerKeyDevice.External(
            descriptor = FAKE_KEYBOARD_DESCRIPTOR,
            name = "Fake Keyboard",
        )

        private const val FAKE_HEADPHONE_DESCRIPTOR = "fake_headphone"
        private val FAKE_HEADPHONE_TRIGGER_KEY_DEVICE = TriggerKeyDevice.External(
            descriptor = FAKE_HEADPHONE_DESCRIPTOR,
            name = "Fake HeadPhones",
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

        private val TEST_ACTION: KeyMapAction = KeyMapAction(
            data = ActionData.Flashlight.Toggle(CameraLens.BACK),
        )

        private val TEST_ACTION_2: KeyMapAction = KeyMapAction(
            data = ActionData.App(FAKE_PACKAGE_NAME),
        )
    }

    private lateinit var controller: KeyMapController
    private lateinit var detectKeyMapsUseCase: DetectKeyMapsUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase
    private lateinit var keyMapListFlow: MutableStateFlow<List<KeyMap>>

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope =
        createTestCoroutineScope(TestCoroutineDispatcher() + TestCoroutineExceptionHandler() + testDispatcher)

    @Before
    fun init() {
        keyMapListFlow = MutableStateFlow(emptyList())

        detectKeyMapsUseCase = mock {
            on { allKeyMapList } doReturn keyMapListFlow

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

        whenever(detectKeyMapsUseCase.currentTime).thenAnswer { coroutineScope.currentTime }

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
        }

        detectConstraintsUseCase = mock {
            on { getSnapshot() } doReturn mock()
        }

        controller = KeyMapController(
            coroutineScope,
            detectKeyMapsUseCase,
            performActionsUseCase,
            detectConstraintsUseCase,
        )
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun `Don't imitate button if 1 long press trigger is successful and another with a longer delay fails`() =
        coroutineScope.runBlockingTest {
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

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = longerTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = shorterTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            inOrder(performActionsUseCase, detectKeyMapsUseCase) {
                // If only the shorter trigger is detected

                mockTriggerKeyInput(shorterTrigger.keys[0], 600L)

                verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
                verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
                verify(detectKeyMapsUseCase, never()).imitateButtonPress(
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
                verify(detectKeyMapsUseCase, never()).imitateButtonPress(
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
                verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val actionData = ActionData.Flashlight.Toggle(CameraLens.BACK)

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(
                    triggerKey(
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        clickType = ClickType.LONG_PRESS,
                    ),
                ),
                actionList = listOf(KeyMapAction(data = actionData)),
                constraintState = ConstraintState(
                    constraints = setOf(Constraint.FlashlightOn(CameraLens.BACK)),
                ),
            )

            keyMapListFlow.value = listOf(keyMap)

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
        coroutineScope.runBlockingTest {
            // GIVEN
            val keyMap1 = KeyMap(
                trigger = KeyMapTrigger(
                    keys = listOf(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN)),
                    longPressDelay = 500,
                ),
                actionList = listOf(TEST_ACTION),
            )

            val keyMap2 = KeyMap(
                trigger = KeyMapTrigger(
                    keys = listOf(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN)),
                    longPressDelay = 1000,
                ),
                actionList = listOf(TEST_ACTION_2),
            )

            keyMapListFlow.value = listOf(keyMap1, keyMap2)

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
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(2)))

            keyMapListFlow.value = listOf(KeyMap(trigger = trigger, actionList = actionList))

            // WHEN
            whenever(performActionsUseCase.getError(actionList[0].data)).thenReturn(Error.NoCompatibleImeChosen)

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
    fun `perform all actions once when key map is triggered`() = coroutineScope.runBlockingTest {
        // GIVEN
        val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

        val actionList = listOf(
            KeyMapAction(data = ActionData.InputKeyEvent(1), delayBeforeNextAction = 1000),
            KeyMapAction(data = ActionData.InputKeyEvent(2)),
        )

        keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val action = KeyMapAction(
                data = ActionData.InputKeyEvent(1),
                repeat = true,
                repeatMode = RepeatMode.LIMIT_REACHED,
                repeatLimit = 2,
            )

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 1),
                delayBeforeNextAction = 500,
            )

            val action2 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                delayBeforeNextAction = 1000,
            )

            val action3 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 3),
            )

            val keyMaps = listOf(
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(action1, action2, action3),
                ),
            )

            keyMapListFlow.value = keyMaps

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
    fun `multiple key maps with same trigger, perform both key maps`() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val keyMaps = listOf(
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION),
                ),
                KeyMap(
                    trigger = trigger,
                    actionList = listOf(TEST_ACTION_2),
                ),
            )

            keyMapListFlow.value = keyMaps

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
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.LIMIT_REACHED,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            keyMapListFlow.value = listOf(keyMap)

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
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
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

            keyMapListFlow.value = listOf(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0])
            advanceTimeBy(200)
            mockTriggerKeyInput(keyMap.trigger.keys[0])

            // THEN
            verify(performActionsUseCase, times(4)).perform(action.data)
        }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when limit reached and trigger hasn't been pressed again`() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            keyMapListFlow.value = listOf(keyMap)

            // WHEN
            mockTriggerKeyInput(keyMap.trigger.keys[0])
            advanceTimeBy(5000)
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
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

            keyMapListFlow.value = listOf(keyMap)

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
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_RELEASED,
                repeatLimit = 10,
            )

            val keyMap = KeyMap(
                trigger = singleKeyTrigger(triggerKey(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)),
                actionList = listOf(action),
            )

            keyMapListFlow.value = listOf(keyMap)

            // WHEN

            mockTriggerKeyInput(keyMap.trigger.keys[0], delay = 5000)

            // THEN

            verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
        }

    /**
     * issue #653
     */
    @Test
    fun `overlapping triggers 3`() = coroutineScope.runBlockingTest {
        // GIVEN
        val keyMaps = listOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 45))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 81))),
            ),
        )

        keyMapListFlow.value = keyMaps

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
    fun `overlapping triggers 2`() = coroutineScope.runBlockingTest {
        // GIVEN
        val keyMaps = listOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_P),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 45))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_META_LEFT),
                    triggerKey(KeyEvent.KEYCODE_P),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 81))),
            ),
        )

        keyMapListFlow.value = keyMaps

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
    fun `overlapping triggers 1`() = coroutineScope.runBlockingTest {
        // GIVEN
        val keyMaps = listOf(
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_CTRL_LEFT),
                    triggerKey(KeyEvent.KEYCODE_SHIFT_LEFT),
                    triggerKey(KeyEvent.KEYCODE_1),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 1))),
            ),
            KeyMap(
                trigger = parallelTrigger(
                    triggerKey(KeyEvent.KEYCODE_CTRL_LEFT),
                    triggerKey(KeyEvent.KEYCODE_1),
                ),
                actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 2))),
            ),
        )

        keyMapListFlow.value = keyMaps

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
                metaState = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                keyCode = KeyEvent.KEYCODE_1,
                action = KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON,
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = parallelTrigger(
                triggerKey(keyCode = 1),
                triggerKey(keyCode = 2),
            )

            keyMapListFlow.value = listOf(
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
                verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(keyCode = 1)
                verifyNoMoreInteractions()

                // verify nothing happens and no key events are consumed when the 2nd key in the trigger is pressed
                // WHEN
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_DOWN), `is`(false))
                assertThat(inputKeyEvent(keyCode = 2, action = KeyEvent.ACTION_UP), `is`(false))

                // THEN
                verify(detectKeyMapsUseCase, never()).imitateButtonPress(keyCode = 1)
                verify(detectKeyMapsUseCase, never()).imitateButtonPress(keyCode = 2)
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger = parallelTrigger(
                triggerKey(keyCode = 1),
                triggerKey(keyCode = 2),
            )

            keyMapListFlow.value = listOf(
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
            verify(detectKeyMapsUseCase, never()).imitateButtonPress(keyCode = 1)
            verify(detectKeyMapsUseCase, never()).imitateButtonPress(keyCode = 2)
        }

    /**
     * issue #662
     */
    @Test
    fun `don't repeat when trigger is released for an action that has these options when the trigger is held down`() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val action = KeyMapAction(
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

            keyMapListFlow.value = listOf(keyMap)
            // WHEN

            mockTriggerKeyInput(triggerKey(keyCode = 2), delay = 1)

            // see if the action repeats
            coroutineScope.advanceTimeBy(500)
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action2 = KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 3))

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            val trigger3 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action3 = KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 4))

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 3), repeat = true)

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action2 = KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 3))

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // given
            val trigger1 = parallelTrigger(triggerKey(keyCode = 1))
            val action1 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 2),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
            )

            val trigger2 =
                parallelTrigger(triggerKey(clickType = ClickType.LONG_PRESS, keyCode = 1))
            val action2 = KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 3),
                repeat = true,
            )

            val trigger3 =
                sequenceTrigger(triggerKey(clickType = ClickType.DOUBLE_PRESS, keyCode = 1))
            val action3 = KeyMapAction(data = ActionData.InputKeyEvent(keyCode = 4))

            keyMapListFlow.value = listOf(
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

                delay(1000) // have a delay after a long press of the key is released so a double press isn't detected

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
        coroutineScope.runBlockingTest {
            val trigger = sequenceTrigger(
                triggerKey(KeyEvent.KEYCODE_A),
                triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.DOUBLE_PRESS),
            )

            keyMapListFlow.value = listOf(
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
    fun sendKeyEventActionWhenImitatingButtonPresses() = coroutineScope.runBlockingTest {
        val trigger = singleKeyTrigger(
            triggerKey(
                keyCode = KeyEvent.KEYCODE_META_LEFT,
                device = FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
            ),
        )

        val action = KeyMapAction(
            data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_META_LEFT),
            holdDown = true,
        )

        keyMapListFlow.value = listOf(KeyMap(trigger = trigger, actionList = listOf(action)))

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
                InputEventType.DOWN,
                metaState,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                InputEventType.DOWN,
                scanCode = 33,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventType.UP,
                0,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                KeyEvent.KEYCODE_E,
                0,
                FAKE_KEYBOARD_DEVICE_ID,
                InputEventType.UP,
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
                InputEventType.DOWN,
                metaState,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                InputEventType.DOWN,
                scanCode = 33,
            )

            verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                KeyEvent.KEYCODE_E,
                metaState,
                FAKE_KEYBOARD_DEVICE_ID,
                InputEventType.UP,
                scanCode = 33,
            )

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventType.UP,
                0,
            )

            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `parallel trigger with 2 keys and the 2nd key is another trigger, press 2 key trigger, only the action for 2 key trigger should be performed `() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val twoKeyTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_SHIFT_LEFT),
                triggerKey(KeyEvent.KEYCODE_A),
            )

            val oneKeyTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A),
            )

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val triggerKeyboard = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE),
            )

            val triggerAnyDevice = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    device = TriggerKeyDevice.Any,
                ),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = triggerKeyboard, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = triggerAnyDevice, actionList = listOf(TEST_ACTION_2)),
            )

            // WHEN
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE))

            // THEN
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
        }

    @Test
    fun `trigger for a specific device, input trigger from a different device, dont detect trigger`() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val triggerHeadphone = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_A, FAKE_HEADPHONE_TRIGGER_KEY_DEVICE),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = triggerHeadphone, actionList = listOf(TEST_ACTION)),
            )

            // WHEN
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_A, FAKE_KEYBOARD_TRIGGER_KEY_DEVICE))

            // THEN
            verify(performActionsUseCase, never()).perform(any(), any(), any())
        }

    @Test
    fun `long press trigger and action with Hold Down until pressed again flag, input valid long press, hold down until long pressed again`() =
        coroutineScope.runBlockingTest {
            // GIVEN
            val trigger =
                singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS))

            val action = KeyMapAction(
                data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_B),
                holdDown = true,
                stopHoldDownWhenTriggerPressedAgain = true,
            )

            val keymap = KeyMap(
                0,
                trigger = trigger,
                actionList = listOf(action),
            )

            keyMapListFlow.value = listOf(keymap)

            // WHEN
            mockTriggerKeyInput(trigger.keys[0])

            // THEN
            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventType.DOWN,
            )

            // WHEN
            mockTriggerKeyInput(trigger.keys[0])

            verify(performActionsUseCase, times(1)).perform(
                action.data,
                InputEventType.UP,
            )
        }

    /**
     * #478
     */
    @Test
    fun `trigger with modifier key and modifier keycode action, don't include metastate from the trigger modifier key when an unmapped modifier key is pressed`() =
        coroutineScope.runBlockingTest {
            val trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_CTRL_LEFT))

            keyMapListFlow.value = listOf(
                KeyMap(
                    0,
                    trigger = trigger,
                    actionList = listOf(KeyMapAction(data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_ALT_LEFT))),
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
                metaState = KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_C,
                KeyEvent.ACTION_DOWN,
                metaState = KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.ACTION_UP,
                metaState = KeyEvent.META_CTRL_LEFT_ON + KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON,
            )
            inputKeyEvent(
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.ACTION_UP,
                metaState = KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON,
            )

            inputKeyEvent(KeyEvent.KEYCODE_C, KeyEvent.ACTION_UP)

            inOrder(detectKeyMapsUseCase) {
                verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                    any(),
                    metaState = eq(KeyEvent.META_ALT_LEFT_ON + KeyEvent.META_ALT_ON + KeyEvent.META_SHIFT_LEFT_ON + KeyEvent.META_SHIFT_ON),
                    any(),
                    any(),
                    any(),
                )

                verify(detectKeyMapsUseCase, times(1)).imitateButtonPress(
                    any(),
                    metaState = eq(0),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `2x key sequence trigger and 3x key sequence trigger with the last 2 keys being the same, trigger 3x key trigger, ignore the first 2x key trigger`() =
        coroutineScope.runBlockingTest {
            val firstTrigger = sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = TriggerKeyDevice.Any,
                ),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            val secondTrigger = sequenceTrigger(
                triggerKey(KeyEvent.KEYCODE_HOME),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = TriggerKeyDevice.Any,
                ),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = firstTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = secondTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_HOME))
            mockTriggerKeyInput(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    device = TriggerKeyDevice.Any,
                ),
            )
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_UP))

            verify(performActionsUseCase, times(1)).perform(TEST_ACTION_2.data)
        }

    @Test
    fun `2x key long press parallel trigger with HOME or RECENTS keycode, trigger successfully, don't do normal action`() =
        coroutineScope.runBlockingTest {
            /*
            HOME
             */

            val homeTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_HOME, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = homeTrigger, actionList = listOf(TEST_ACTION)),
            )

            val consumedHomeDown = inputKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, null)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, null)

            advanceUntilIdle()

            inputKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP, null)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, null)

            assertThat(consumedHomeDown, `is`(true))

            /*
            RECENTS
             */

            val recentsTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_APP_SWITCH, clickType = ClickType.LONG_PRESS),
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = recentsTrigger, actionList = listOf(TEST_ACTION)),
            )

            val consumedRecentsDown =
                inputKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_DOWN, null)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, null)

            advanceUntilIdle()

            inputKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_UP, null)
            inputKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, null)

            assertThat(consumedRecentsDown, `is`(true))
        }

    @Test
    fun shortPressTriggerDoublePressTrigger_holdDown_onlyDetectDoublePressTrigger() =
        coroutineScope.runBlockingTest {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val doublePressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.DOUBLE_PRESS),
            )

            keyMapListFlow.value = listOf(
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
        coroutineScope.runBlockingTest {
            // GIVEN
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val longPressTrigger = singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.LONG_PRESS,
                ),
            )

            keyMapListFlow.value = listOf(
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
    fun parallelTrigger_holdDown_repeatAction10Times(description: String, trigger: KeyMapTrigger) =
        coroutineScope.runBlockingTest {
            // given
            val action = KeyMapAction(
                data = ActionData.Volume.Up(showVolumeUi = false),
                repeat = true,
            )

            keyMapListFlow.value = listOf(KeyMap(0, trigger = trigger, actionList = listOf(action)))

            when (trigger.mode) {
                is TriggerMode.Parallel -> mockParallelTrigger(trigger, delay = 2000L)
                TriggerMode.Undefined -> mockTriggerKeyInput(trigger.keys[0], delay = 2000L)
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
    @Parameters(method = "params_dualParallelTrigger_input2ndKey_dontConsumeUp")
    fun dualParallelTrigger_input2ndKey_dontConsumeUp(description: String, trigger: KeyMapTrigger) =
        coroutineScope.runBlockingTest {
            // given
            keyMapListFlow.value =
                listOf(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

            // when
            trigger.keys[1].let {
                inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_DOWN,
                    triggerKeyDeviceToInputDevice(it.device),
                )
            }

            trigger.keys[1].let {
                val consumed = inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(it.device),
                )

                // then
                assertThat(consumed, `is`(false))
            }
        }

    fun params_dualParallelTrigger_input2ndKey_dontConsumeUp() = listOf(
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
    fun dualShortPressParallelTrigger_validInput_consumeUp() = coroutineScope.runBlockingTest {
        // given
        val trigger = parallelTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
        )

        keyMapListFlow.value =
            listOf(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

        // when
        trigger.keys.forEach {
            inputKeyEvent(
                it.keyCode,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(it.device),
            )
        }

        var consumedUpCount = 0

        trigger.keys.forEach {
            val consumed =
                inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(it.device),
                )

            if (consumed) {
                consumedUpCount += 1
            }
        }

        // then
        assertThat(consumedUpCount, `is`(2))
    }

    @Test
    fun dualLongPressParallelTrigger_validInput_consumeUp() = coroutineScope.runBlockingTest {
        // given
        val trigger = parallelTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP, clickType = ClickType.LONG_PRESS),
        )

        keyMapListFlow.value =
            listOf(KeyMap(0, trigger = trigger, actionList = listOf(TEST_ACTION)))

        // when
        trigger.keys.forEach {
            inputKeyEvent(
                it.keyCode,
                KeyEvent.ACTION_DOWN,
                triggerKeyDeviceToInputDevice(it.device),
            )
        }

        advanceUntilIdle()

        var consumedUpCount = 0

        trigger.keys.forEach {
            val consumed =
                inputKeyEvent(
                    it.keyCode,
                    KeyEvent.ACTION_UP,
                    triggerKeyDeviceToInputDevice(it.device),
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
        coroutineScope.runBlockingTest {
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

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = longPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            advanceUntilIdle()

            // then
            verify(
                detectKeyMapsUseCase,
                times(1),
            ).imitateButtonPress(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)
        }

    @Test
    fun keymappedToSingleShortPressAndLongPress_validShortPress_onlyPerformActionDontImitateKey() =
        coroutineScope.runBlockingTest {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val longPressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = longPressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            // then
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
            verify(performActionsUseCase, never()).perform(TEST_ACTION_2.data)
            verify(detectKeyMapsUseCase, never()).imitateButtonPress(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun keymappedToShortPressAndDoublePress_validShortPress_onlyPerformActionDoNotImitateKey() =
        coroutineScope.runBlockingTest {
            // given
            val shortPressTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            val doublePressTrigger = singleKeyTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.DOUBLE_PRESS),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = shortPressTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = doublePressTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            advanceUntilIdle()

            // then
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)

            // wait for the double press to try and imitate the key.
            verify(detectKeyMapsUseCase, never()).imitateButtonPress(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun singleKeyTriggerAndShortPressParallelTriggerWithSameInitialKey_validSingleKeyTriggerInput_onlyPerformActionDontImitateKey() =
        coroutineScope.runBlockingTest {
            // given
            val singleKeyTrigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))
            val parallelTrigger = parallelTrigger(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
            )

            keyMapListFlow.value = listOf(
                KeyMap(0, trigger = singleKeyTrigger, actionList = listOf(TEST_ACTION)),
                KeyMap(1, trigger = parallelTrigger, actionList = listOf(TEST_ACTION_2)),
            )

            // when
            mockTriggerKeyInput(triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN))

            // then
            verify(detectKeyMapsUseCase, never()).imitateButtonPress(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
            verify(performActionsUseCase, times(1)).perform(TEST_ACTION.data)
        }

    @Test
    fun longPressSequenceTrigger_invalidLongPress_keyImitated() = coroutineScope.runBlockingTest {
        val trigger = sequenceTrigger(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            triggerKey(KeyEvent.KEYCODE_VOLUME_UP),
        )

        keyMapListFlow.value = listOf(
            KeyMap(trigger = trigger, actionList = listOf(TEST_ACTION)),
        )

        mockTriggerKeyInput(
            triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN, clickType = ClickType.LONG_PRESS),
            delay = 100L,
        )

        verify(
            detectKeyMapsUseCase,
            times(1),
        ).imitateButtonPress(keyCode = KeyEvent.KEYCODE_VOLUME_DOWN)
    }

    @Test
    @Parameters(method = "params_multipleActionsPerformed")
    fun validInput_multipleActionsPerformed(description: String, trigger: KeyMapTrigger) =
        coroutineScope.runBlockingTest {
            val actionList = listOf(TEST_ACTION, TEST_ACTION_2)
            // GIVEN
            keyMapListFlow.value = listOf(
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
                    TriggerKeyDevice.Any,
                ),
            ),
        ),
        arrayOf(
            "sequence",
            sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
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
        coroutineScope.runBlockingTest {
            // GIVEN
            keyMapListFlow.value = listOf(keyMap)

            // WHEN
            var consumedCount = 0

            keyMap.trigger.keys.forEach {
                val consumed =
                    inputKeyEvent(
                        999,
                        KeyEvent.ACTION_DOWN,
                        triggerKeyDeviceToInputDevice(it.device),
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
    fun validInput_downConsumed(description: String, keyMap: KeyMap) =
        coroutineScope.runBlockingTest {
            // GIVEN
            keyMapListFlow.value = listOf(keyMap)

            var consumedCount = 0

            keyMap.trigger.keys.forEach {
                val consumed =
                    inputKeyEvent(
                        it.keyCode,
                        KeyEvent.ACTION_DOWN,
                        triggerKeyDeviceToInputDevice(it.device),
                    )

                if (consumed) {
                    consumedCount++
                }
            }

            assertThat(consumedCount, `is`(keyMap.trigger.keys.size))
        }

    @Test
    @Parameters(method = "params_allTriggerKeyCombinationsDontConsume")
    @TestCaseName("{0}")
    fun validInput_dontConsumeFlag_dontConsumeDown(description: String, keyMap: KeyMap) =
        coroutineScope.runBlockingTest {
            keyMapListFlow.value = listOf(keyMap)

            var consumedCount = 0

            keyMap.trigger.keys.forEach {
                val consumed =
                    inputKeyEvent(
                        it.keyCode,
                        KeyEvent.ACTION_DOWN,
                        triggerKeyDeviceToInputDevice(it.device),
                    )

                if (consumed) {
                    consumedCount++
                }
            }

            assertThat(consumedCount, `is`(0))
        }

    fun params_allTriggerKeyCombinationsDontConsume(): List<Array<Any>> {
        val triggerAndDescriptions = listOf(
            "undefined single short-press this-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    consume = false,
                ),
            ),
            "undefined single long-press this-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),
            "undefined single double-press this-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "undefined single short-press any-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    consume = false,
                ),
            ),
            "undefined single long-press any-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),
            "undefined single double-press any-device, dont consume" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple short-press this-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple long-press this-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple double-press this-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix this-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix external-device, dont consume" to sequenceTrigger(
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

            "sequence multiple short-press mixed-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple long-press mixed-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple double-press mixed-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix mixed-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    FAKE_KEYBOARD_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                    consume = false,
                ),
            ),

            "sequence multiple mix mixed-device, dont consume" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
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
                    TriggerKeyDevice.Internal,
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

            "parallel multiple short-press this-device, dont consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple long-press this-device, dont consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
            ),

            "parallel multiple short-press external-device, dont consume" to parallelTrigger(
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

            "parallel multiple long-press external-device, dont consume" to parallelTrigger(
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

            "parallel multiple short-press mix-device, dont consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
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

            "parallel multiple long-press mix-device, dont consume" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                    consume = false,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
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
                    TriggerKeyDevice.Internal,
                ),
            ),
            "undefined single long-press this-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "undefined single double-press this-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "undefined single short-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                ),
            ),
            "undefined single long-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "undefined single double-press any-device" to singleKeyTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),

            "sequence multiple short-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "sequence multiple long-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
            ),
            "sequence multiple double-press this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix this-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Any,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Any,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.DOUBLE_PRESS,
                ),
            ),
            "sequence multiple mix mixed-device" to sequenceTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Any,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    FAKE_HEADPHONE_TRIGGER_KEY_DEVICE,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
            "parallel multiple long-press this-device" to parallelTrigger(
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_A,
                    TriggerKeyDevice.Internal,
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
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.SHORT_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
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
                    TriggerKeyDevice.Internal,
                    clickType = ClickType.LONG_PRESS,
                ),
                triggerKey(
                    KeyEvent.KEYCODE_VOLUME_UP,
                    TriggerKeyDevice.Any,
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
    fun validInput_actionPerformed(description: String, keyMap: KeyMap) =
        coroutineScope.runBlockingTest {
            // GIVEN
            keyMapListFlow.value = listOf(keyMap)

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
        val deviceDescriptor = triggerKeyDeviceToInputDevice(key.device)
        val pressDuration: Long = delay ?: when (key.clickType) {
            ClickType.LONG_PRESS -> LONG_PRESS_DELAY + 100L
            else -> 50L
        }

        inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)

        when (key.clickType) {
            ClickType.SHORT_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            ClickType.LONG_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }

            ClickType.DOUBLE_PRESS -> {
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
                delay(pressDuration)

                inputKeyEvent(key.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)
                delay(pressDuration)
                inputKeyEvent(key.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
            }
        }
    }

    private fun inputKeyEvent(
        keyCode: Int,
        action: Int,
        device: InputDeviceInfo? = null,
        metaState: Int? = null,
        scanCode: Int = 0,
    ): Boolean = controller.onKeyEvent(
        keyCode = keyCode,
        action = action,
        metaState = metaState ?: 0,
        scanCode = scanCode,
        device = device,
    )

    private suspend fun mockParallelTrigger(
        trigger: KeyMapTrigger,
        delay: Long? = null,
    ) {
        require(trigger.mode is TriggerMode.Parallel)

        trigger.keys.forEach {
            val deviceDescriptor = triggerKeyDeviceToInputDevice(it.device)

            inputKeyEvent(it.keyCode, KeyEvent.ACTION_DOWN, deviceDescriptor)
        }

        if (delay != null) {
            delay(delay)
        } else {
            when ((trigger.mode as TriggerMode.Parallel).clickType) {
                ClickType.SHORT_PRESS -> delay(50)
                ClickType.LONG_PRESS -> delay(LONG_PRESS_DELAY + 100L)
            }
        }

        trigger.keys.forEach {
            val deviceDescriptor = triggerKeyDeviceToInputDevice(it.device)

            inputKeyEvent(it.keyCode, KeyEvent.ACTION_UP, deviceDescriptor)
        }
    }

    private fun triggerKeyDeviceToInputDevice(
        device: TriggerKeyDevice,
        deviceId: Int = 0,
        isGameController: Boolean = false,
    ): InputDeviceInfo = when (device) {
        TriggerKeyDevice.Any -> InputDeviceInfo(
            descriptor = "any_device",
            name = "any_device_name",
            isExternal = false,
            id = deviceId,
            isGameController = isGameController,
        )

        is TriggerKeyDevice.External -> InputDeviceInfo(
            descriptor = device.descriptor,
            name = "device_name",
            isExternal = true,
            id = deviceId,
            isGameController = isGameController,
        )

        TriggerKeyDevice.Internal -> InputDeviceInfo(
            descriptor = "internal_device",
            name = "internal_device_name",
            isExternal = false,
            id = deviceId,
            isGameController = isGameController,
        )
    }
}

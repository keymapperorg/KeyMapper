package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.sds100.keymapper.TestLoggingTree
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.DelayController
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import timber.log.Timber

/**
 * Created by sds100 on 20/05/2022.
 */
@ExperimentalCoroutinesApi
class KeyMapControllerTest {

    companion object {
        private const val LONG_PRESS_DELAY = 500L
        private const val DOUBLE_PRESS_DELAY = 300L
        private const val FORCE_VIBRATE = false
        private const val REPEAT_RATE = 50L
        private const val REPEAT_DELAY = 400L
        private const val SEQUENCE_TRIGGER_TIMEOUT = 2000L
        private const val VIBRATION_DURATION = 100L
        private const val HOLD_DOWN_DURATION = 1000L
    }

    private lateinit var controller: KeyMapController
    private lateinit var detectKeyMapsUseCase: DetectKeyMapsUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase
    private lateinit var keyMapListFlow: MutableStateFlow<List<KeyMap>>

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private val TEST_ACTION: KeyMapAction = KeyMapAction(
        data = ActionData.Flashlight.Toggle(CameraLens.BACK)
    )

    private val TEST_ACTION_2: KeyMapAction = KeyMapAction(
        data = ActionData.Flashlight.Enable(CameraLens.BACK)
    )

    private val TEST_ACTION_3: KeyMapAction = KeyMapAction(
        data = ActionData.Flashlight.Disable(CameraLens.BACK)
    )

    private val TEST_HOLD_DOWN_ACTION: KeyMapAction = KeyMapAction(
        data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_A),
        holdDown = true
    )

    private val TEST_REPEAT_ACTION: KeyMapAction = KeyMapAction(
        data = ActionData.InputKeyEvent(KeyEvent.KEYCODE_A),
        repeat = true,
        repeatDelay = 300,
        repeatRate = 50
    )

    @Before
    fun setUp() {
        keyMapListFlow = MutableStateFlow(emptyList())
        val defaultKeyMapOptions = DefaultKeyMapOptions(
            LONG_PRESS_DELAY,
            DOUBLE_PRESS_DELAY,
            SEQUENCE_TRIGGER_TIMEOUT,
            VIBRATION_DURATION,
            FORCE_VIBRATE
        )

        detectKeyMapsUseCase = mock {
            on { allKeyMapList }.doReturn(keyMapListFlow)
            on { defaultOptions } doReturn MutableStateFlow(defaultKeyMapOptions)
        }

        whenever(detectKeyMapsUseCase.currentTime).thenAnswer { coroutineScope.currentTime }

        performActionsUseCase = mock()
        detectConstraintsUseCase = mock()

        Timber.plant(TestLoggingTree())

        controller = KeyMapController(
            coroutineScope,
            detectKeyMapsUseCase,
            performActionsUseCase,
            detectConstraintsUseCase
        )
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun longPress() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS)),
                actionList = listOf(TEST_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
        advanceTimeBy(500)
        verify(performActionsUseCase).perform(TEST_ACTION.data, InputEventType.DOWN_UP, 0)

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
    }

    @Test
    fun longPress_releaseEarly_doNotPerformAction() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS)),
                actionList = listOf(TEST_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
        advanceTimeBy(100) //release early

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))

        advanceTimeBy(2000) //wait to see if the action is performed
        verify(performActionsUseCase, never()).perform(TEST_ACTION.data)
    }

    @Test
    fun longPress_releaseEarly_imitate() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS)),
                actionList = listOf(TEST_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
        advanceTimeBy(100) //release early

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))

        advanceUntilIdle()

        verify(detectKeyMapsUseCase).imitateButtonPress(KeyEvent.KEYCODE_A)
    }

    @Test
    fun longPress_repeatAction() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A, clickType = ClickType.LONG_PRESS)),
                actionList = listOf(TEST_REPEAT_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))

        advanceTimeBy(500)
        verify(performActionsUseCase).perform(TEST_REPEAT_ACTION.data)

        advanceTimeBy(2000)

        verify(performActionsUseCase, atLeast(4)).perform(TEST_REPEAT_ACTION.data)

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
        verifyNoMoreInteractions(performActionsUseCase)
    }

    @Test
    fun shortPress() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(0, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)), actionList = listOf(TEST_ACTION))
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
        verify(performActionsUseCase).perform(TEST_ACTION.data)

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
    }

    @Test
    fun multipleShortPressTriggers() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(0, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)), actionList = listOf(TEST_ACTION)),
            KeyMap(1, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_B)), actionList = listOf(TEST_ACTION_2)),
            KeyMap(2, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_C)), actionList = listOf(TEST_ACTION_3)),
        )

        performActionsUseCase.inOrder {
            assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
            verify().perform(TEST_ACTION.data)

            assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))

            assertThat(inputDown(KeyEvent.KEYCODE_B), `is`(true))
            verify().perform(TEST_ACTION_2.data)

            assertThat(inputUp(KeyEvent.KEYCODE_B), `is`(true))

            assertThat(inputDown(KeyEvent.KEYCODE_C), `is`(true))
            verify().perform(TEST_ACTION_3.data)

            assertThat(inputUp(KeyEvent.KEYCODE_C), `is`(true))
        }
    }

    @Test
    fun shortPress_holdDownAction() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)),
                actionList = listOf(TEST_HOLD_DOWN_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
        verify(performActionsUseCase).perform(TEST_HOLD_DOWN_ACTION.data, InputEventType.DOWN, 0)

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
        verify(performActionsUseCase).perform(TEST_HOLD_DOWN_ACTION.data, InputEventType.UP, 0)
    }

    @Test
    fun shortPress_repeatAction() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(
                0,
                trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)),
                actionList = listOf(TEST_REPEAT_ACTION)
            )
        )

        assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))

        verify(performActionsUseCase).perform(TEST_REPEAT_ACTION.data)

        advanceTimeBy(500)

        verify(performActionsUseCase, atLeast(4)).perform(TEST_REPEAT_ACTION.data)

        assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
        verifyNoMoreInteractions(performActionsUseCase)
    }

    /**
     * You should be able to trigger a key map again after using it.
     */
    @Test
    fun triggerMultipleTimes() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(0, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)), actionList = listOf(TEST_ACTION))
        )

        performActionsUseCase.inOrder {
            assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
            verify(performActionsUseCase).perform(TEST_ACTION.data)

            assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))

            assertThat(inputDown(KeyEvent.KEYCODE_A), `is`(true))
            verify(performActionsUseCase).perform(TEST_ACTION.data)

            assertThat(inputUp(KeyEvent.KEYCODE_A), `is`(true))
        }
    }

    private fun DelayController.setKeyMaps(vararg keyMap: KeyMap) {
        keyMapListFlow.value = keyMap.toList()
        advanceUntilIdle()
    }

    private fun inputDown(
        keyCode: Int,
        device: InputDeviceInfo? = null,
        metaState: Int = 0,
        scanCode: Int = 0,
    ): Boolean {
        return controller.onKeyEvent(
            keyCode = keyCode,
            keyEventAction = KeyEvent.ACTION_DOWN,
            metaState = metaState,
            scanCode = scanCode,
            device = device
        )
    }

    private fun inputUp(
        keyCode: Int,
        device: InputDeviceInfo? = null,
        metaState: Int = 0,
        scanCode: Int = 0,
    ): Boolean {
        return controller.onKeyEvent(
            keyCode = keyCode,
            keyEventAction = KeyEvent.ACTION_UP,
            metaState = metaState,
            scanCode = scanCode,
            device = device
        )
    }
}
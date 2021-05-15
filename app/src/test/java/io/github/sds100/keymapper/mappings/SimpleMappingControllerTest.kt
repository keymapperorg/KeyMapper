package io.github.sds100.keymapper.mappings

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.FakeAction
import io.github.sds100.keymapper.actions.KeyEventAction
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 15/05/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class SimpleMappingControllerTest {

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

    private val testDispatcher = TestCoroutineDispatcher()
    private val coroutineScope = TestCoroutineScope(testDispatcher)

    private lateinit var controller: SimpleMappingController
    private lateinit var detectMappingUseCase: DetectMappingUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase

    @Before
    fun init() {
        detectMappingUseCase = mock {

            MutableStateFlow(REPEAT_RATE).apply {
                on { defaultRepeatRate } doReturn this
            }

            MutableStateFlow(HOLD_DOWN_DURATION).apply {
                on { defaultHoldDownDuration } doReturn this
            }

            MutableStateFlow(VIBRATION_DURATION).apply {
                on { defaultVibrateDuration } doReturn this
            }

            MutableStateFlow(FORCE_VIBRATE).apply {
                on { forceVibrate } doReturn this
            }
        }

        performActionsUseCase = mock()

        detectConstraintsUseCase = mock {
            on { getSnapshot() } doReturn ConstraintSnapshot(
                appInForeground = null,
                appsPlayingMedia = emptyList(),
                orientation = Orientation.ORIENTATION_0,
                connectedBluetoothDevices = emptySet(),
                isScreenOn = true
            )
        }

        controller = FakeSimpleMappingController(coroutineScope,
            detectMappingUseCase,
            performActionsUseCase,
            detectConstraintsUseCase)
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until limit reached, then stop repeating when the limit has been reached`() = coroutineScope.runBlockingTest {
        //GIVEN
        val action = FakeAction(
            data = KeyEventAction(keyCode = 1),
            repeat = true,
            repeatLimit = 10
        )

        //WHEN
        controller.onDetected("id", FakeMapping(actionList = listOf(action)))
        advanceUntilIdle()

        //THEN
        verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
    }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when the trigger has been pressed again`() = coroutineScope.runBlockingTest {
        //GIVEN
        val action = FakeAction(
            data = KeyEventAction(keyCode = 1),
            repeat = true,
            repeatLimit = 10,
            repeatRate = 100,
        )

        //WHEN
        controller.onDetected("id", FakeMapping(actionList = listOf(action)))
        advanceTimeBy(200)
        controller.onDetected("id", FakeMapping(actionList = listOf(action)))

        //THEN
        verify(performActionsUseCase, times(3)).perform(action.data)
    }

    /**
     * issue #663
     */
    @Test
    fun `when triggering action that repeats until pressed again with repeat limit, then stop repeating when limit reached and trigger hasn't been pressed again`() = coroutineScope.runBlockingTest {
        //GIVEN
        val action = FakeAction(
            data = KeyEventAction(keyCode = 1),
            repeat = true,
            repeatLimit = 10
        )

        //WHEN
        controller.onDetected("id", FakeMapping(actionList = listOf(action)))
        advanceTimeBy(5000)
        controller.onDetected("id", FakeMapping(actionList = listOf(action)))

        //THEN
        verify(performActionsUseCase, times(action.repeatLimit!! + 1)).perform(action.data)
    }
}
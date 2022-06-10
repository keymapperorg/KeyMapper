package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.ConstraintSnapshotImpl
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 23/06/2021.
 */

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class TriggerKeyMapFromOtherAppsControllerTest {

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

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)

    private lateinit var controller: TriggerKeyMapFromOtherAppsController
    private lateinit var detectKeyMapsUseCase: DetectKeyMapsUseCase
    private lateinit var performActionsUseCase: PerformActionsUseCase
    private lateinit var detectConstraintsUseCase: DetectConstraintsUseCase
    private lateinit var keyMapListFlow: MutableStateFlow<List<KeyMap>>

    @Before
    fun init() {
        keyMapListFlow = MutableStateFlow(emptyList())

        detectKeyMapsUseCase = mock {
            on { keyMapsToTriggerFromOtherApps } doReturn keyMapListFlow

            MutableStateFlow(VIBRATION_DURATION).apply {
                on { defaultVibrateDuration } doReturn this
            }

            MutableStateFlow(FORCE_VIBRATE).apply {
                on { forceVibrate } doReturn this
            }
        }

        performActionsUseCase = mock {
            MutableStateFlow(REPEAT_RATE).apply {
                on { defaultRepeatRate } doReturn this
            }

            MutableStateFlow(HOLD_DOWN_DURATION).apply {
                on { defaultHoldDownDuration } doReturn this
            }
        }

        detectConstraintsUseCase = mock {
            on { getSnapshot() } doReturn ConstraintSnapshotImpl(
                accessibilityService = mock(),
                mediaAdapter = mock(),
                devicesAdapter = mock(),
                displayAdapter = mock(),
                cameraAdapter = mock(),
                networkAdapter = mock(),
                inputMethodAdapter = mock(),
                lockScreenAdapter = mock(),
                phoneAdapter = mock(),
                powerAdapter = mock()
            )
        }

        controller = TriggerKeyMapFromOtherAppsController(
            coroutineScope,
            detectKeyMapsUseCase,
            performActionsUseCase,
            detectConstraintsUseCase
        )
    }

    /**
     * #707
     */
    @Test
    fun `Key map with repeat option, don't repeat when triggered if repeat until released`() = runTest(testDispatcher) {
        //GIVEN
        val action =
            KeyMapAction(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_RELEASED
            )
        val keyMap = KeyMap(actionList = listOf(action), trigger = KeyMapTrigger(triggerFromOtherApps = true))
        keyMapListFlow.value = listOf(keyMap)
        runCurrent()

        //WHEN
        controller.onDetected(keyMap.uid)
        advanceTimeBy(500)
        runCurrent()

        controller.reset() //stop any repeating that might be happening
        advanceUntilIdle()

        //THEN
        verify(performActionsUseCase, times(1)).perform(action.data)
    }
}
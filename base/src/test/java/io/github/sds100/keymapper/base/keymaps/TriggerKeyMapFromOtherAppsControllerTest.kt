package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ActionErrorSnapshot
import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.actions.RepeatMode
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.TriggerKeyMapFromOtherAppsController
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.utils.TestConstraintSnapshot
import io.github.sds100.keymapper.common.utils.KMError
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

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

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

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

        controller = TriggerKeyMapFromOtherAppsController(
            testScope,
            detectKeyMapsUseCase,
            performActionsUseCase,
            detectConstraintsUseCase,
        )
    }

    /**
     * #707
     */
    @Test
    fun `Key map with repeat option, don't repeat when triggered if repeat until released`() = runTest(testDispatcher) {
        // GIVEN
        val action =
            Action(
                data = ActionData.InputKeyEvent(keyCode = 1),
                repeat = true,
                repeatMode = RepeatMode.TRIGGER_RELEASED,
            )
        val keyMap = KeyMap(
            actionList = listOf(action),
            trigger = Trigger(triggerFromOtherApps = true),
        )
        keyMapListFlow.value = listOf(keyMap)

        advanceUntilIdle()

        // WHEN
        controller.onDetected(keyMap.uid)
        delay(500)
        controller.reset() // stop any repeating that might be happening
        advanceUntilIdle()

        // THEN
        verify(performActionsUseCase, times(1)).perform(action.data)
    }
}

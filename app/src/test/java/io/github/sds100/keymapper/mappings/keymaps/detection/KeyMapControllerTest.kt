package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Created by sds100 on 20/05/2022.
 */
@ExperimentalCoroutinesApi
class KeyMapControllerTest {

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

        detectKeyMapsUseCase = mock {
            on { allKeyMapList }.doReturn(keyMapListFlow)
        }
        
        performActionsUseCase = mock()
        detectConstraintsUseCase = mock()

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

        inputDown(KeyEvent.KEYCODE_A)
        advanceTimeBy(500)
        verify(performActionsUseCase).perform(TEST_ACTION.data, InputEventType.DOWN_UP, 0)

        inputUp(KeyEvent.KEYCODE_A)
    }

    @Test
    fun shortPress() = coroutineScope.runBlockingTest {
        setKeyMaps(
            KeyMap(0, trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_A)), actionList = listOf(TEST_ACTION))
        )

        inputDown(KeyEvent.KEYCODE_A)
        verify(performActionsUseCase).perform(TEST_ACTION.data, InputEventType.DOWN_UP, 0)

        inputUp(KeyEvent.KEYCODE_A)
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

        inputDown(KeyEvent.KEYCODE_A)
        verify(performActionsUseCase).perform(TEST_HOLD_DOWN_ACTION.data, InputEventType.DOWN, 0)

        inputUp(KeyEvent.KEYCODE_A)
        verify(performActionsUseCase).perform(TEST_HOLD_DOWN_ACTION.data, InputEventType.UP, 0)
    }

    private fun setKeyMaps(vararg keyMap: KeyMap) {
        keyMapListFlow.value = keyMap.toList()
    }

    private fun inputDown(
        keyCode: Int,
        device: InputDeviceInfo? = null,
        metaState: Int? = null,
        scanCode: Int = 0,
    ): Boolean {
        return controller.onKeyEvent(
            keyCode = keyCode,
            keyEventAction = KeyEvent.ACTION_DOWN,
            metaState = metaState ?: 0,
            scanCode = scanCode,
            device = device
        )
    }

    private fun inputUp(
        keyCode: Int,
        device: InputDeviceInfo? = null,
        metaState: Int? = null,
        scanCode: Int = 0,
    ): Boolean {
        return controller.onKeyEvent(
            keyCode = keyCode,
            keyEventAction = KeyEvent.ACTION_UP,
            metaState = metaState ?: 0,
            scanCode = scanCode,
            device = device
        )
    }
}
package io.github.sds100.keymapper

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.FingerprintGestureType
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCaseController
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerType
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.singleKeyTrigger
import io.github.sds100.keymapper.util.triggerKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Created by sds100 on 19/04/2021.
 */

@ExperimentalCoroutinesApi
class ConfigKeyMapUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var useCase: ConfigKeyMapUseCaseController

    @Before
    fun init() {
        useCase = ConfigKeyMapUseCaseController(
            coroutineScope = testScope,
            devicesAdapter = mock(),
            keyMapRepository = mock(),
            preferenceRepository = mock(),
            floatingLayoutRepository = mock(),
            floatingButtonRepository = mock(),
            serviceAdapter = mock(),
        )
    }

    @Test
    fun `Do not allow setting double press for parallel trigger with side key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        useCase.setTriggerDoublePress()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting long press for parallel trigger with side key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        useCase.setTriggerLongPress()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting double press for side key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        useCase.setTriggerDoublePress()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Undefined))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting long press for side key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        useCase.setTriggerLongPress()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Undefined))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if side key added to double press volume button`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )

        useCase.setTriggerDoublePress()

        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if fingerprint gestures added to double press volume button`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )

        useCase.setTriggerDoublePress()

        useCase.addFingerprintGesture(FingerprintGestureType.SWIPE_UP)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if side key added to long press volume button`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )

        useCase.setTriggerLongPress()

        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if fingerprint gestures added to long press volume button`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )

        useCase.setTriggerLongPress()

        useCase.addFingerprintGesture(FingerprintGestureType.SWIPE_UP)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Enable hold down option for key event actions when the trigger is a DPAD button`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())
        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_DPAD_LEFT,
            TriggerKeyDevice.Any,
            KeyEventDetectionSource.INPUT_METHOD,
        )

        useCase.addAction(ActionData.InputKeyEvent(keyCode = KeyEvent.KEYCODE_W))

        val actionList = useCase.keyMap.value.dataOrNull()!!.actionList
        assertThat(actionList[0].holdDown, `is`(true))
        assertThat(actionList[0].repeat, `is`(false))
    }

    /**
     * This ensures that it isn't possible to have two or more assistant triggers when the mode is parallel.
     */
    @Test
    fun `Remove device assistant trigger if setting mode to parallel and voice assistant already exists`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.addAssistantTriggerKey(AssistantTriggerType.VOICE)
        useCase.addAssistantTriggerKey(AssistantTriggerType.DEVICE)
        useCase.setParallelTriggerMode()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.keys, hasSize(2))
        assertThat(trigger.keys[0], instanceOf(KeyCodeTriggerKey::class.java))
        assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
    }

    @Test
    fun `Remove voice assistant trigger if setting mode to parallel and device assistant already exists`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.addAssistantTriggerKey(AssistantTriggerType.DEVICE)
        useCase.addAssistantTriggerKey(AssistantTriggerType.VOICE)
        useCase.setParallelTriggerMode()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.keys, hasSize(2))
        assertThat(trigger.keys[0], instanceOf(KeyCodeTriggerKey::class.java))
        assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
    }

    @Test
    fun `Set click type to short press when adding assistant key to multiple long press trigger keys`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_UP,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.setTriggerLongPress()

        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Set click type to short press when adding assistant key to double press trigger key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.setTriggerDoublePress()
        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Set click type to short press when adding assistant key to long press trigger key`() = runTest(testDispatcher) {
        useCase.keyMap.value = State.Data(KeyMap())

        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            TriggerKeyDevice.Any,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )
        useCase.setTriggerLongPress()
        useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Do not allow long press for parallel trigger with assistant key`() = runTest(testDispatcher) {
        val keyMap = KeyMap(
            trigger = Trigger(
                mode = TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS),
                keys = listOf(
                    triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                    AssistantTriggerKey(
                        type = AssistantTriggerType.ANY,
                        clickType = ClickType.SHORT_PRESS,
                    ),
                ),
            ),
        )

        useCase.keyMap.value = State.Data(keyMap)
        useCase.setTriggerLongPress()

        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
        assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    /**
     * Issue #753. If a modifier key is used as a trigger then it the
     * option to not override the default action must be chosen so that the modifier
     * key can still be used normally.
     */
    @Test
    fun `when add modifier key trigger, enable do not remap option`() = runTest(testDispatcher) {
        val modifierKeys = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_NUM,
            KeyEvent.KEYCODE_FUNCTION,
        )

        for (modifierKeyCode in modifierKeys) {
            // GIVEN
            useCase.keyMap.value = State.Data(KeyMap())

            // WHEN
            useCase.addKeyCodeTriggerKey(
                modifierKeyCode,
                TriggerKeyDevice.Internal,
                detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
            )

            // THEN
            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger

            assertThat(trigger.keys[0].consumeEvent, `is`(false))
        }
    }

    /**
     * Issue #753.
     */
    @Test
    fun `when add non-modifier key trigger, do ont enable do not remap option`() = runTest(testDispatcher) {
        // GIVEN
        useCase.keyMap.value = State.Data(KeyMap())

        // WHEN
        useCase.addKeyCodeTriggerKey(
            KeyEvent.KEYCODE_A,
            TriggerKeyDevice.Internal,
            detectionSource = KeyEventDetectionSource.ACCESSIBILITY_SERVICE,
        )

        // THEN
        val trigger = useCase.keyMap.value.dataOrNull()!!.trigger

        assertThat(trigger.keys[0].consumeEvent, `is`(true))
    }

    /**
     * Issue #852. Add a phone ringing constraint when you add an action
     * to answer a phone call.
     */
    @Test
    fun `when add answer phone call action, then add phone ringing constraint`() = runTest(testDispatcher) {
        // GIVEN
        useCase.keyMap.value = State.Data(KeyMap())
        val action = ActionData.AnswerCall

        // WHEN
        useCase.addAction(action)

        // THEN
        val keyMap = useCase.keyMap.value.dataOrNull()!!
        assertThat(
            keyMap.constraintState.constraints,
            contains(instanceOf(Constraint.PhoneRinging::class.java)),
        )
    }

    /**
     * Issue #852. Add a in phone call constraint when you add an action
     * to end a phone call.
     */
    @Test
    fun `when add end phone call action, then add in phone call constraint`() = runTest(testDispatcher) {
        // GIVEN
        useCase.keyMap.value = State.Data(KeyMap())
        val action = ActionData.EndCall

        // WHEN
        useCase.addAction(action)

        // THEN
        val keyMap = useCase.keyMap.value.dataOrNull()!!
        assertThat(
            keyMap.constraintState.constraints,
            contains(instanceOf(Constraint.InPhoneCall::class.java)),
        )
    }

    /**
     * issue #593
     */
    @Test
    fun `key map with hold down action, load key map, hold down flag shouldn't disappear`() = runTest(testDispatcher) {
        // given
        val action = Action(
            data = ActionData.TapScreen(100, 100, null),
            holdDown = true,
        )

        val keyMap = KeyMap(
            0,
            trigger = singleKeyTrigger(triggerKey(KeyEvent.KEYCODE_0)),
            actionList = listOf(action),
        )

        // when
        useCase.keyMap.value = State.Data(keyMap)

        // then
        assertThat(useCase.keyMap.value.dataOrNull()!!.actionList, `is`(listOf(action)))
    }

    @Test
    fun `add modifier key event action, enable hold down option and disable repeat option`() = runTest(testDispatcher) {
        InputEventUtils.MODIFIER_KEYCODES.forEach { keyCode ->
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addAction(ActionData.InputKeyEvent(keyCode))

            useCase.keyMap.value.dataOrNull()!!.actionList
                .single()
                .let {
                    assertThat(it.holdDown, `is`(true))
                    assertThat(it.repeat, `is`(false))
                }
        }
    }
}

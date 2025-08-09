package io.github.sds100.keymapper.base

import android.view.KeyEvent
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapUseCaseController
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.base.trigger.AssistantTriggerType
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.FloatingButtonKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.utils.parallelTrigger
import io.github.sds100.keymapper.base.utils.sequenceTrigger
import io.github.sds100.keymapper.base.utils.singleKeyTrigger
import io.github.sds100.keymapper.base.utils.triggerKey
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
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
    fun `Adding a non evdev key deletes all evdev keys in the trigger`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(
                KeyMap(
                    trigger = parallelTrigger(
                        FloatingButtonKey(
                            buttonUid = "floating_button",
                            button = null,
                            clickType = ClickType.SHORT_PRESS,
                        ),
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                            scanCode = 123,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                        AssistantTriggerKey(
                            type = AssistantTriggerType.ANY,
                            clickType = ClickType.SHORT_PRESS,
                        ),
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                            scanCode = 100,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                    ),
                ),
            )

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.keys, hasSize(3))
            assertThat(trigger.keys[0], instanceOf(FloatingButtonKey::class.java))
            assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
            assertThat(trigger.keys[2], instanceOf(KeyEventTriggerKey::class.java))
            assertThat(
                (trigger.keys[2] as KeyEventTriggerKey).requiresIme,
                `is`(false),
            )
        }

    @Test
    fun `Adding an evdev key deletes all non evdev keys in the trigger`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(
                KeyMap(
                    trigger = parallelTrigger(
                        FloatingButtonKey(
                            buttonUid = "floating_button",
                            button = null,
                            clickType = ClickType.SHORT_PRESS,
                        ),
                        triggerKey(
                            KeyEvent.KEYCODE_VOLUME_UP,
                            KeyEventTriggerDevice.Internal,
                        ),
                        AssistantTriggerKey(
                            type = AssistantTriggerType.ANY,
                            clickType = ClickType.SHORT_PRESS,
                        ),
                        triggerKey(
                            KeyEvent.KEYCODE_VOLUME_DOWN,
                            KeyEventTriggerDevice.Internal,
                        ),
                    ),
                ),
            )

            val evdevDevice = EvdevDeviceInfo(
                name = "Volume Keys",
                bus = 0,
                vendor = 1,
                product = 2,
            )
            useCase.addEvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                scanCode = 0,
                device = evdevDevice,
            )

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.keys, hasSize(3))
            assertThat(trigger.keys[0], instanceOf(FloatingButtonKey::class.java))
            assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
            assertThat(trigger.keys[2], instanceOf(EvdevTriggerKey::class.java))
            assertThat(
                (trigger.keys[2] as EvdevTriggerKey).device,
                `is`(evdevDevice),
            )
        }

    @Test
    fun `Converting a sequence trigger to parallel trigger removes duplicate evdev keys`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(
                KeyMap(
                    trigger = sequenceTrigger(
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                            scanCode = 0,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                            scanCode = 0,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                    ),
                ),
            )

            useCase.setParallelTriggerMode()

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.keys, hasSize(1))
            assertThat(trigger.keys[0], instanceOf(EvdevTriggerKey::class.java))
            assertThat(
                (trigger.keys[0] as EvdevTriggerKey).keyCode,
                `is`(KeyEvent.KEYCODE_VOLUME_DOWN),
            )
        }

    @Test
    fun `Adding the same evdev trigger key from same device makes the trigger a sequence`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addEvdevTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                device = EvdevDeviceInfo(
                    name = "Volume Keys",
                    bus = 0,
                    vendor = 1,
                    product = 2,
                ),
            )

            useCase.addEvdevTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                device = EvdevDeviceInfo(
                    name = "Volume Keys",
                    bus = 0,
                    vendor = 1,
                    product = 2,
                ),
            )

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Sequence))
        }

    @Test
    fun `Adding an evdev trigger key to a sequence trigger keeps it sequence`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(
                KeyMap(
                    trigger = sequenceTrigger(
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                            scanCode = 0,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                        EvdevTriggerKey(
                            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                            scanCode = 0,
                            device = EvdevDeviceInfo(
                                name = "Volume Keys",
                                bus = 0,
                                vendor = 1,
                                product = 2,
                            ),
                        ),
                    ),
                ),
            )

            // Add a third key and it should still be a sequence trigger now
            useCase.addEvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 0,
                device = EvdevDeviceInfo(
                    name = "Volume Keys",
                    bus = 0,
                    vendor = 1,
                    product = 2,
                ),
            )

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Sequence))
        }

    @Test
    fun `Adding the same evdev trigger key code from different devices keeps the trigger parallel`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addEvdevTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                device = EvdevDeviceInfo(
                    name = "Volume Keys",
                    bus = 0,
                    vendor = 1,
                    product = 2,
                ),
            )

            useCase.addEvdevTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                device = EvdevDeviceInfo(
                    name = "Fake Controller",
                    bus = 1,
                    vendor = 2,
                    product = 1,
                ),
            )

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(ClickType.SHORT_PRESS)))
        }

    @Test
    fun `Do not allow setting double press for parallel trigger with side key`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            useCase.setTriggerDoublePress()

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
            assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
            assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
        }

    @Test
    fun `Do not allow setting long press for parallel trigger with side key`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
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
    fun `Set click type to short press if side key added to double press volume button`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )

            useCase.setTriggerDoublePress()

            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
            assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
            assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
        }

    @Test
    fun `Set click type to short press if fingerprint gestures added to double press volume button`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )

            useCase.setTriggerDoublePress()

            useCase.addFingerprintGesture(FingerprintGestureType.SWIPE_UP)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
            assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
            assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
        }

    @Test
    fun `Set click type to short press if side key added to long press volume button`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )

            useCase.setTriggerLongPress()

            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
            assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
            assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
        }

    @Test
    fun `Set click type to short press if fingerprint gestures added to long press volume button`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )

            useCase.setTriggerLongPress()

            useCase.addFingerprintGesture(FingerprintGestureType.SWIPE_UP)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
            assertThat(trigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
            assertThat(trigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
        }

    @Test
    fun `Enable hold down option for key event actions when the trigger is a DPAD button`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())
            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_DPAD_LEFT,
                0,
                KeyEventTriggerDevice.Internal,
                true,
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
    fun `Remove device assistant trigger if setting mode to parallel and voice assistant already exists`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.addAssistantTriggerKey(AssistantTriggerType.VOICE)
            useCase.addAssistantTriggerKey(AssistantTriggerType.DEVICE)
            useCase.setParallelTriggerMode()

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.keys, hasSize(2))
            assertThat(
                trigger.keys[0],
                instanceOf(KeyEventTriggerKey::class.java),
            )
            assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
        }

    @Test
    fun `Remove voice assistant trigger if setting mode to parallel and device assistant already exists`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.addAssistantTriggerKey(AssistantTriggerType.DEVICE)
            useCase.addAssistantTriggerKey(AssistantTriggerType.VOICE)
            useCase.setParallelTriggerMode()

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.keys, hasSize(2))
            assertThat(
                trigger.keys[0],
                instanceOf(KeyEventTriggerKey::class.java),
            )
            assertThat(trigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
        }

    @Test
    fun `Set click type to short press when adding assistant key to multiple long press trigger keys`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_UP,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.setTriggerLongPress()

            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        }

    @Test
    fun `Set click type to short press when adding assistant key to double press trigger key`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.setTriggerDoublePress()
            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        }

    @Test
    fun `Set click type to short press when adding assistant key to long press trigger key`() =
        runTest(testDispatcher) {
            useCase.keyMap.value = State.Data(KeyMap())

            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                0,
                KeyEventTriggerDevice.Internal,
                false,
            )
            useCase.setTriggerLongPress()
            useCase.addAssistantTriggerKey(AssistantTriggerType.ANY)

            val trigger = useCase.keyMap.value.dataOrNull()!!.trigger
            assertThat(trigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        }

    @Test
    fun `Do not allow long press for parallel trigger with assistant key`() =
        runTest(testDispatcher) {
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
            useCase.addKeyEventTriggerKey(
                modifierKeyCode,
                0,
                KeyEventTriggerDevice.Internal,
                false,
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
    fun `when add non-modifier key trigger, do ont enable do not remap option`() =
        runTest(testDispatcher) {
            // GIVEN
            useCase.keyMap.value = State.Data(KeyMap())

            // WHEN
            useCase.addKeyEventTriggerKey(
                KeyEvent.KEYCODE_A,
                0,
                KeyEventTriggerDevice.Internal,
                false,
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
    fun `when add answer phone call action, then add phone ringing constraint`() =
        runTest(testDispatcher) {
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
    fun `when add end phone call action, then add in phone call constraint`() =
        runTest(testDispatcher) {
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
    fun `key map with hold down action, load key map, hold down flag shouldn't disappear`() =
        runTest(testDispatcher) {
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
    fun `add modifier key event action, enable hold down option and disable repeat option`() =
        runTest(testDispatcher) {
            KeyEventUtils.MODIFIER_KEYCODES.forEach { keyCode ->
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

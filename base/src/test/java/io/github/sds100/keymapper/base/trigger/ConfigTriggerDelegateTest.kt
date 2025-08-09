package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.parallelTrigger
import io.github.sds100.keymapper.base.utils.sequenceTrigger
import io.github.sds100.keymapper.base.utils.singleKeyTrigger
import io.github.sds100.keymapper.base.utils.triggerKey
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test

class ConfigTriggerDelegateTest {

    private lateinit var delegate: ConfigTriggerDelegate

    @Before
    fun before() {
        delegate = ConfigTriggerDelegate()
    }

    /**
     * Issue #761
     */
    @Test
    fun `Do not enable scan code detection if a key in another key map has the same key code, different scan code and is from a different device`() {
        val device1 = KeyEventTriggerDevice.External(
            descriptor = "keyboard0",
            name = "Keyboard",
        )

        val device2 = KeyEventTriggerDevice.External(
            descriptor = "keyboard1",
            name = "Other Keyboard",
        )

        val otherTriggers = listOf(
            KeyEventTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device1,
                clickType = ClickType.SHORT_PRESS
            ),
        )

        val newTrigger = delegate.addKeyEventTriggerKey(
            trigger = Trigger(),
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device2, // Different device
            requiresIme = false,
            otherTriggers
        )

        assertThat(
            (newTrigger.keys[0] as KeyEventTriggerKey).detectWithScanCodeUserSetting,
            `is`(false),
        )
    }

    /**
     * Issue #761
     */
    @Test
    fun `Do not enable scan code detection if a key in the trigger has the same key code, different scan code and is from a different device`() {
        val device1 = KeyEventTriggerDevice.External(
            descriptor = "keyboard0",
            name = "Keyboard",
        )

        val device2 = KeyEventTriggerDevice.External(
            descriptor = "keyboard1",
            name = "Other Keyboard",
        )

        val trigger = singleKeyTrigger(
            KeyEventTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device1,
                clickType = ClickType.SHORT_PRESS
            ),
        )

        val newTrigger = delegate.addKeyEventTriggerKey(
            trigger = trigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device2, // Different device
            requiresIme = false,
        )

        assertThat(
            (newTrigger.keys[1] as KeyEventTriggerKey).detectWithScanCodeUserSetting,
            `is`(false),
        )
    }

    /**
     * Issue #761
     */
    @Test
    fun `Enable scan code detection for an evdev trigger if a key in another key map has the same key code but different scan code`() {
        val device = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        val otherTriggers = listOf(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device,
            ),
        )

        val newTrigger = delegate.addEvdevTriggerKey(
            trigger = Trigger(),
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device,
            otherTriggers
        )

        assertThat(
            (newTrigger.keys[0] as EvdevTriggerKey).detectWithScanCodeUserSetting,
            `is`(true),
        )
    }

    /**
     * Issue #761
     */
    @Test
    fun `Enable scan code detection for a key event trigger if a key in another key map has the same key code but different scan code`() {
        val device = KeyEventTriggerDevice.External(
            descriptor = "keyboard0",
            name = "Keyboard",
        )

        val otherTriggers = listOf(
            KeyEventTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device,
                clickType = ClickType.SHORT_PRESS
            ),
        )

        val newTrigger = delegate.addKeyEventTriggerKey(
            trigger = Trigger(),
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device,
            requiresIme = false,
            otherTriggers
        )

        assertThat(
            (newTrigger.keys[0] as KeyEventTriggerKey).detectWithScanCodeUserSetting,
            `is`(true),
        )
    }

    /**
     * Issue #761
     */
    @Test
    fun `Enable scan code detection if another key event key exists in the trigger with the same key code but different scan code`() {
        val device = KeyEventTriggerDevice.External(
            descriptor = "keyboard0",
            name = "Keyboard",
        )

        val trigger = singleKeyTrigger(
            KeyEventTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device,
                clickType = ClickType.SHORT_PRESS
            ),
        )

        val newTrigger = delegate.addKeyEventTriggerKey(
            trigger = trigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device,
            requiresIme = false,
        )

        assertThat(
            (newTrigger.keys[1] as KeyEventTriggerKey).detectWithScanCodeUserSetting,
            `is`(true),
        )
    }

    /**
     * Issue #761
     */
    @Test
    fun `Enable scan code detection if another evdev key exists in the trigger with the same key code but different scan code`() {
        val device = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        val trigger = singleKeyTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                scanCode = 123,
                device = device,
            ),
        )

        val newTrigger = delegate.addEvdevTriggerKey(
            trigger = trigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 124,
            device = device,
        )

        assertThat(
            (newTrigger.keys[1] as EvdevTriggerKey).detectWithScanCodeUserSetting,
            `is`(true),
        )
    }

    @Test
    fun `Adding a non evdev key deletes all evdev keys in the trigger`() {
        val trigger = parallelTrigger(
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
            )
        )

        val newTrigger = delegate.addKeyEventTriggerKey(
            trigger,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            0,
            KeyEventTriggerDevice.Internal,
            false,
        )

        assertThat(newTrigger.keys, hasSize(3))
        assertThat(newTrigger.keys[0], instanceOf(FloatingButtonKey::class.java))
        assertThat(newTrigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
        assertThat(newTrigger.keys[2], instanceOf(KeyEventTriggerKey::class.java))
        assertThat(
            (newTrigger.keys[2] as KeyEventTriggerKey).requiresIme,
            `is`(false),
        )
    }

    @Test
    fun `Adding an evdev key deletes all non evdev keys in the trigger`() {
        val trigger = parallelTrigger(
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
        )

        val evdevDevice = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        val newTrigger = delegate.addEvdevTriggerKey(
            trigger = trigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = evdevDevice,
        )

        assertThat(newTrigger.keys, hasSize(3))
        assertThat(newTrigger.keys[0], instanceOf(FloatingButtonKey::class.java))
        assertThat(newTrigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
        assertThat(newTrigger.keys[2], instanceOf(EvdevTriggerKey::class.java))
        assertThat(
            (newTrigger.keys[2] as EvdevTriggerKey).device,
            `is`(evdevDevice),
        )
    }

    @Test
    fun `Converting a sequence trigger to parallel trigger removes duplicate evdev keys`() {
        val trigger = sequenceTrigger(
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
        )

        val newTrigger = delegate.setParallelTriggerMode(trigger)

        assertThat(newTrigger.keys, hasSize(1))
        assertThat(newTrigger.keys[0], instanceOf(EvdevTriggerKey::class.java))
        assertThat(
            (newTrigger.keys[0] as EvdevTriggerKey).keyCode,
            `is`(KeyEvent.KEYCODE_VOLUME_DOWN),
        )
    }

    @Test
    fun `Adding the same evdev trigger key from same device makes the trigger a sequence`() {
        val emptyTrigger = Trigger()
        val device = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        val triggerWithFirstKey = delegate.addEvdevTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = device,
        )

        val triggerWithSecondKey = delegate.addEvdevTriggerKey(
            trigger = triggerWithFirstKey,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = device,
        )

        assertThat(triggerWithSecondKey.mode, `is`(TriggerMode.Sequence))
    }

    @Test
    fun `Adding an evdev trigger key to a sequence trigger keeps it sequence`() {
        val device = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )

        val trigger = sequenceTrigger(
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                scanCode = 0,
                device = device,
            ),
            EvdevTriggerKey(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                scanCode = 0,
                device = device,
            ),
        )

        // Add a third key and it should still be a sequence trigger now
        val newTrigger = delegate.addEvdevTriggerKey(
            trigger = trigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 0,
            device = device,
        )

        assertThat(newTrigger.mode, `is`(TriggerMode.Sequence))
    }

    @Test
    fun `Adding the same evdev trigger key code from different devices keeps the trigger parallel`() {
        val emptyTrigger = Trigger()
        val device1 = EvdevDeviceInfo(
            name = "Volume Keys",
            bus = 0,
            vendor = 1,
            product = 2,
        )
        val device2 = EvdevDeviceInfo(
            name = "Fake Controller",
            bus = 1,
            vendor = 2,
            product = 1,
        )

        val triggerWithFirstKey = delegate.addEvdevTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = device1,
        )

        val triggerWithSecondKey = delegate.addEvdevTriggerKey(
            trigger = triggerWithFirstKey,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = device2,
        )

        assertThat(triggerWithSecondKey.mode, `is`(TriggerMode.Parallel(ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Do not allow setting double press for parallel trigger with side key`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithKeyEvent,
            type = AssistantTriggerType.ANY
        )

        val finalTrigger = delegate.setTriggerDoublePress(triggerWithAssistant)

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting long press for parallel trigger with side key`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithKeyEvent,
            type = AssistantTriggerType.ANY
        )

        val finalTrigger = delegate.setTriggerLongPress(triggerWithAssistant)

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting double press for side key`() {
        val emptyTrigger = Trigger()

        val triggerWithAssistant = delegate.addAssistantTriggerKey(
            trigger = emptyTrigger,
            type = AssistantTriggerType.ANY
        )

        val finalTrigger = delegate.setTriggerDoublePress(triggerWithAssistant)

        assertThat(finalTrigger.mode, `is`(TriggerMode.Undefined))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Do not allow setting long press for side key`() {
        val emptyTrigger = Trigger()

        val triggerWithAssistant = delegate.addAssistantTriggerKey(
            trigger = emptyTrigger,
            type = AssistantTriggerType.ANY
        )

        val finalTrigger = delegate.setTriggerLongPress(triggerWithAssistant)

        assertThat(finalTrigger.mode, `is`(TriggerMode.Undefined))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if side key added to double press volume button`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithDoublePress = delegate.setTriggerDoublePress(triggerWithKeyEvent)

        val finalTrigger = delegate.addAssistantTriggerKey(
            trigger = triggerWithDoublePress,
            type = AssistantTriggerType.ANY
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if fingerprint gestures added to double press volume button`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithDoublePress = delegate.setTriggerDoublePress(triggerWithKeyEvent)

        val finalTrigger = delegate.addFingerprintGesture(
            trigger = triggerWithDoublePress,
            type = FingerprintGestureType.SWIPE_UP
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if side key added to long press volume button`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithLongPress = delegate.setTriggerLongPress(triggerWithKeyEvent)

        val finalTrigger = delegate.addAssistantTriggerKey(
            trigger = triggerWithLongPress,
            type = AssistantTriggerType.ANY
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    @Test
    fun `Set click type to short press if fingerprint gestures added to long press volume button`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithLongPress = delegate.setTriggerLongPress(triggerWithKeyEvent)

        val finalTrigger = delegate.addFingerprintGesture(
            trigger = triggerWithLongPress,
            type = FingerprintGestureType.SWIPE_UP
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
        assertThat(finalTrigger.keys[0].clickType, `is`(ClickType.SHORT_PRESS))
        assertThat(finalTrigger.keys[1].clickType, `is`(ClickType.SHORT_PRESS))
    }

    /**
     * This ensures that it isn't possible to have two or more assistant triggers when the mode is parallel.
     */
    @Test
    fun `Remove device assistant trigger if setting mode to parallel and voice assistant already exists`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithVoiceAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithKeyEvent,
            type = AssistantTriggerType.VOICE
        )

        val triggerWithDeviceAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithVoiceAssistant,
            type = AssistantTriggerType.DEVICE
        )

        val finalTrigger = delegate.setParallelTriggerMode(triggerWithDeviceAssistant)

        assertThat(finalTrigger.keys, hasSize(2))
        assertThat(
            finalTrigger.keys[0],
            instanceOf(KeyEventTriggerKey::class.java),
        )
        assertThat(finalTrigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
    }

    @Test
    fun `Remove voice assistant trigger if setting mode to parallel and device assistant already exists`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithDeviceAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithKeyEvent,
            type = AssistantTriggerType.DEVICE
        )

        val triggerWithVoiceAssistant = delegate.addAssistantTriggerKey(
            trigger = triggerWithDeviceAssistant,
            type = AssistantTriggerType.VOICE
        )

        val finalTrigger = delegate.setParallelTriggerMode(triggerWithVoiceAssistant)

        assertThat(finalTrigger.keys, hasSize(2))
        assertThat(
            finalTrigger.keys[0],
            instanceOf(KeyEventTriggerKey::class.java),
        )
        assertThat(finalTrigger.keys[1], instanceOf(AssistantTriggerKey::class.java))
    }

    @Test
    fun `Set click type to short press when adding assistant key to multiple long press trigger keys`() {
        val emptyTrigger = Trigger()

        val triggerWithFirstKey = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithSecondKey = delegate.addKeyEventTriggerKey(
            trigger = triggerWithFirstKey,
            keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithLongPress = delegate.setTriggerLongPress(triggerWithSecondKey)

        val finalTrigger = delegate.addAssistantTriggerKey(
            trigger = triggerWithLongPress,
            type = AssistantTriggerType.ANY
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Set click type to short press when adding assistant key to double press trigger key`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithDoublePress = delegate.setTriggerDoublePress(triggerWithKeyEvent)

        val finalTrigger = delegate.addAssistantTriggerKey(
            trigger = triggerWithDoublePress,
            type = AssistantTriggerType.ANY
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Set click type to short press when adding assistant key to long press trigger key`() {
        val emptyTrigger = Trigger()

        val triggerWithKeyEvent = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        val triggerWithLongPress = delegate.setTriggerLongPress(triggerWithKeyEvent)

        val finalTrigger = delegate.addAssistantTriggerKey(
            trigger = triggerWithLongPress,
            type = AssistantTriggerType.ANY
        )

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    @Test
    fun `Do not allow long press for parallel trigger with assistant key`() {
        val trigger = Trigger(
            mode = TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS),
            keys = listOf(
                triggerKey(KeyEvent.KEYCODE_VOLUME_DOWN),
                AssistantTriggerKey(
                    type = AssistantTriggerType.ANY,
                    clickType = ClickType.SHORT_PRESS,
                ),
            ),
        )

        val finalTrigger = delegate.setTriggerLongPress(trigger)

        assertThat(finalTrigger.mode, `is`(TriggerMode.Parallel(clickType = ClickType.SHORT_PRESS)))
    }

    /**
     * Issue #753. If a modifier key is used as a trigger then it the
     * option to not override the default action must be chosen so that the modifier
     * key can still be used normally.
     */
    @Test
    fun `when add modifier key trigger, enable do not remap option`() {
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
            val emptyTrigger = Trigger()

            // WHEN
            val trigger = delegate.addKeyEventTriggerKey(
                trigger = emptyTrigger,
                keyCode = modifierKeyCode,
                scanCode = 0,
                device = KeyEventTriggerDevice.Internal,
                requiresIme = false,
            )

            // THEN
            assertThat((trigger.keys[0] as KeyEventTriggerKey).consumeEvent, `is`(false))
        }
    }

    /**
     * Issue #753.
     */
    @Test
    fun `when add non-modifier key trigger, do not enable do not remap option`() {
        // GIVEN
        val emptyTrigger = Trigger()

        // WHEN
        val trigger = delegate.addKeyEventTriggerKey(
            trigger = emptyTrigger,
            keyCode = KeyEvent.KEYCODE_A,
            scanCode = 0,
            device = KeyEventTriggerDevice.Internal,
            requiresIme = false,
        )

        // THEN
        assertThat((trigger.keys[0] as KeyEventTriggerKey).consumeEvent, `is`(true))
    }
}
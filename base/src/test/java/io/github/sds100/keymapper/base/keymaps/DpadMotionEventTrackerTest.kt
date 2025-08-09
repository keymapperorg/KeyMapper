package io.github.sds100.keymapper.base.keymaps

import android.view.InputDevice
import android.view.KeyEvent
import io.github.sds100.keymapper.base.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.common.utils.InputDeviceInfo
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(JUnitParamsRunner::class)
class DpadMotionEventTrackerTest {

    companion object {
        private val CONTROLLER_1_DEVICE = InputDeviceInfo(
            id = 0,
            descriptor = "controller_1",
            name = "Controller 1",
            isExternal = true,
            isGameController = true,
            sources = InputDevice.SOURCE_GAMEPAD,
        )

        private val CONTROLLER_2_DEVICE = InputDeviceInfo(
            id = 1,
            descriptor = "controller_2",
            name = "Controller 2",
            isExternal = true,
            isGameController = true,
            sources = InputDevice.SOURCE_GAMEPAD,
        )
    }

    private lateinit var tracker: DpadMotionEventTracker

    @Before
    fun init() {
        tracker = DpadMotionEventTracker()
    }

    @Test
    fun `Detect multiple key events if two DPAD buttons changed in the same motion event`() {
        var motionEvent = createMotionEvent(axisHatX = -1.0f)
        tracker.convertMotionEvent(motionEvent)

        motionEvent = motionEvent.copy(axisHatY = -1.0f)
        tracker.convertMotionEvent(motionEvent)

        motionEvent = motionEvent.copy(axisHatX = 0.0f, axisHatY = 0.0f)
        val keyEvents = tracker.convertMotionEvent(motionEvent)

        assertThat(keyEvents, hasSize(2))
        assertThat(
            keyEvents,
            hasItem(
                KMKeyEvent(
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.ACTION_UP,
                    metaState = 0,
                    scanCode = 0,
                    device = CONTROLLER_1_DEVICE,
                    repeatCount = 0,
                    source = InputDevice.SOURCE_DPAD,
                    eventTime = motionEvent.eventTime,
                ),
            ),
        )
        assertThat(
            keyEvents,
            hasItem(
                KMKeyEvent(
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.ACTION_UP,
                    metaState = 0,
                    scanCode = 0,
                    device = CONTROLLER_1_DEVICE,
                    repeatCount = 0,
                    source = InputDevice.SOURCE_DPAD,
                    eventTime = motionEvent.eventTime,
                ),
            ),
        )
    }

    @Test
    fun `Consume DPAD key events when joystick motion events are received while multiple DPAD buttons are pressed`() {
        var motionEvent = createMotionEvent(axisHatX = -1.0f)
        tracker.convertMotionEvent(motionEvent)

        motionEvent = motionEvent.copy(axisHatY = -1.0f)
        tracker.convertMotionEvent(motionEvent)

        var consume =
            tracker.onKeyEvent(createDownKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(true))
        consume =
            tracker.onKeyEvent(createDownKeyEvent(KeyEvent.KEYCODE_DPAD_UP, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(true))

        motionEvent = motionEvent.copy(axisHatX = 0.0f)
        tracker.convertMotionEvent(motionEvent)
        motionEvent = motionEvent.copy(axisHatY = 0.0f)
        tracker.convertMotionEvent(motionEvent)

        consume =
            tracker.onKeyEvent(createUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(false))
        tracker.onKeyEvent(createUpKeyEvent(KeyEvent.KEYCODE_DPAD_UP, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(false))
    }

    @Test
    fun `Consume DPAD key events when joystick motion events are received while processing DPAD motion event`() {
        tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))
        var consume =
            tracker.onKeyEvent(createDownKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(true))

        tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))

        consume =
            tracker.onKeyEvent(createUpKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, CONTROLLER_1_DEVICE))
        assertThat(consume, `is`(false))
    }

    @Test
    fun `Track DPAD left and right key events from two controllers`() { // Press DPAD left
        tracker.convertMotionEvent(
            createMotionEvent(
                axisHatX = -1.0f,
                device = CONTROLLER_1_DEVICE,
            ),
        )
        val keyEvent = tracker.convertMotionEvent(
            createMotionEvent(
                axisHatX = 1.0f,
                device = CONTROLLER_2_DEVICE,
            ),
        )

        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))
    }

    @Test
    fun `Track DPAD left key events from two controllers`() { // Press DPAD left
        tracker.convertMotionEvent(
            createMotionEvent(
                axisHatX = -1.0f,
                device = CONTROLLER_1_DEVICE,
            ),
        )
        val keyEvent = tracker.convertMotionEvent(
            createMotionEvent(
                axisHatX = -1.0f,
                device = CONTROLLER_2_DEVICE,
            ),
        )

        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))
    }

    @Test
    fun `Interleave press and release of two dpad buttons`() {
        var motionEvent = createMotionEvent(axisHatX = -1.0f)
        // Press DPAD left
        tracker.convertMotionEvent(motionEvent)

        // Press DPAD up
        motionEvent = motionEvent.copy(axisHatY = -1.0f)
        var keyEvent = tracker.convertMotionEvent(motionEvent)

        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))

        // Release DPAD left
        motionEvent = motionEvent.copy(axisHatX = 0.0f)
        keyEvent = tracker.convertMotionEvent(motionEvent)
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))

        // Release DPAD up
        motionEvent = motionEvent.copy(axisHatY = 0.0f)
        keyEvent = tracker.convertMotionEvent(motionEvent)
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `Track DPAD up key event while left is pressed down`() {
        val motionEvent = createMotionEvent(axisHatX = -1.0f)
        // Press DPAD left
        tracker.convertMotionEvent(motionEvent)

        // Press DPAD up
        var keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatY = -1.0f))

        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))

        // Release DPAD up
        keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatY = 0.0f))
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))

        // Release DPAD left
        keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatX = 0.0f))
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `Track DPAD right key event after left is pressed down and released`() {
        // Press DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))
        // Release DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))

        // Press DPAD right
        var keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 1.0f))
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))

        // Release DPAD right
        keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `DPAD left key event is UP on release`() {
        // Press DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))

        // Release DPAD left
        val keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))
        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `DPAD left key event is DOWN when pressed down`() {
        // Press DPAD left
        val keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))

        assertThat(keyEvent.first().keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent.first().action, `is`(KeyEvent.ACTION_DOWN))
    }

    private fun createMotionEvent(
        axisHatX: Float = 0.0f,
        axisHatY: Float = 0.0f,
        device: InputDeviceInfo = CONTROLLER_1_DEVICE,
    ): KMGamePadEvent {
        return KMGamePadEvent(
            metaState = 0,
            device = device,
            axisHatX = axisHatX,
            axisHatY = axisHatY,
            eventTime = System.currentTimeMillis(),
        )
    }

    private fun createDownKeyEvent(keyCode: Int, device: InputDeviceInfo): KMKeyEvent {
        return KMKeyEvent(
            keyCode = keyCode,
            action = KeyEvent.ACTION_DOWN,
            metaState = 0,
            scanCode = 0,
            device = device,
            repeatCount = 0,
            source = 0,
            eventTime = System.currentTimeMillis(),
        )
    }

    private fun createUpKeyEvent(keyCode: Int, device: InputDeviceInfo): KMKeyEvent {
        return KMKeyEvent(
            keyCode = keyCode,
            action = KeyEvent.ACTION_UP,
            metaState = 0,
            scanCode = 0,
            device = device,
            repeatCount = 0,
            source = 0,
            eventTime = System.currentTimeMillis(),

        )
    }
}

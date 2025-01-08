package io.github.sds100.keymapper.mappings.keymaps

import android.view.KeyEvent
import io.github.sds100.keymapper.mappings.keymaps.detection.DpadMotionEventTracker
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by sds100 on 15/05/2021.
 */

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
        )

        private val CONTROLLER_2_DEVICE = InputDeviceInfo(
            id = 1,
            descriptor = "controller_2",
            name = "Controller 2",
            isExternal = true,
            isGameController = true,
        )
    }

    private lateinit var tracker: DpadMotionEventTracker

    @Before
    fun init() {
        tracker = DpadMotionEventTracker()
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

        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_DOWN))
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

        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_DOWN))
    }

    @Test
    fun `Track DPAD up key event while left is pressed down`() {
        val motionEvent = createMotionEvent(axisHatX = -1.0f)
        // Press DPAD left
        tracker.convertMotionEvent(motionEvent)

        // Press DPAD up
        var keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatY = -1.0f))

        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_DOWN))

        // Release DPAD up
        keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatY = 0.0f))
        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_UP))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_UP))

        // Release DPAD left
        keyEvent = tracker.convertMotionEvent(motionEvent.copy(axisHatX = 0.0f))
        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `Track DPAD right key event after left is pressed down and released`() {
        // Press DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))
        // Release DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))

        // Press DPAD right
        var keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 1.0f))
        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_DOWN))

        // Release DPAD right
        keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))
        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `DPAD left key event is UP on release`() {
        // Press DPAD left
        tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))

        // Release DPAD left
        val keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = 0.0f))
        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_UP))
    }

    @Test
    fun `DPAD left key event is DOWN when pressed down`() {
        // Press DPAD left
        val keyEvent = tracker.convertMotionEvent(createMotionEvent(axisHatX = -1.0f))

        assertThat(keyEvent?.keyCode, `is`(KeyEvent.KEYCODE_DPAD_LEFT))
        assertThat(keyEvent?.action, `is`(KeyEvent.ACTION_DOWN))
    }

    private fun createMotionEvent(
        axisHatX: Float = 0.0f,
        axisHatY: Float = 0.0f,
        device: InputDeviceInfo = CONTROLLER_1_DEVICE,
        isDpad: Boolean = true,
    ): MyMotionEvent {
        return MyMotionEvent(
            metaState = 0,
            device = device,
            axisHatX = axisHatX,
            axisHatY = axisHatY,
            isDpad = isDpad,
        )
    }
}

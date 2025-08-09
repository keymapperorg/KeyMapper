package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.system.inputevents.Scancode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic

class TriggerKeyTest {

    private lateinit var mockedKeyEvent: MockedStatic<KeyEvent>

    @Before
    fun setUp() {
        mockedKeyEvent = mockStatic(KeyEvent::class.java)
        mockedKeyEvent.`when`<Int> { KeyEvent.getMaxKeyCode() }.thenReturn(1000)
    }

    @After
    fun tearDown() {
        mockedKeyEvent.close()
    }
    
    @Test
    fun `User can not change scan code detection if the scan code is null`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = null,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.isScanCodeDetectionUserConfigurable(), `is`(false))
    }

    @Test
    fun `User can not change scan code detection if the key code is unknown and scan code is non null`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_UNKNOWN,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.isScanCodeDetectionUserConfigurable(), `is`(false))
    }

    @Test
    fun `User can change scan code detection if the key code is known and scan code is non null`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.isScanCodeDetectionUserConfigurable(), `is`(true))
    }

    @Test
    fun `detect with scan code if key code is unknown and user setting enabled`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = 0,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.detectWithScancode(), `is`(true))
    }

    @Test
    fun `detect with scan code if key code is unknown and user setting disabled`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = 0,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = false
        )
        assertThat(triggerKey.detectWithScancode(), `is`(true))
    }

    @Test
    fun `detect with scan code if user setting enabled and scan code non null`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.detectWithScancode(), `is`(true))
    }

    @Test
    fun `detect with key code if user setting enabled and scan code is null`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = null,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = true
        )
        assertThat(triggerKey.detectWithScancode(), `is`(false))
    }

    @Test
    fun `detect with key code if user setting false and key code is known`() {
        val triggerKey = KeyEventTriggerKey(
            keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            scanCode = Scancode.KEY_VOLUMEDOWN,
            device = KeyEventTriggerDevice.Internal,
            clickType = ClickType.SHORT_PRESS,
            detectWithScanCodeUserSetting = false
        )
        assertThat(triggerKey.detectWithScancode(), `is`(false))
    }
}
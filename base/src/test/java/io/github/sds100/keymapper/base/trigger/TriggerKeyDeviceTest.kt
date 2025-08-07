package io.github.sds100.keymapper.base.trigger

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TriggerKeyDeviceTest {
    @Test
    fun `external device is same as another external device`() {
        val device1 = KeyEventTriggerDevice.External("keyboard0", "Keyboard 0")
        val device2 = KeyEventTriggerDevice.External("keyboard0", "Keyboard 0")
        assertThat(device1.isSameDevice(device2), `is`(true))
    }

    @Test
    fun `external device is not the same as a different external device`() {
        val device1 = KeyEventTriggerDevice.External("keyboard0", "Keyboard 0")
        val device2 = KeyEventTriggerDevice.External("keyboard1", "Keyboard 1")
        assertThat(device1.isSameDevice(device2), `is`(false))
    }

    @Test
    fun `external device is not the same as a different external device with the same name`() {
        val device1 = KeyEventTriggerDevice.External("keyboard0", "Keyboard 0")
        val device2 = KeyEventTriggerDevice.External("keyboard1", "Keyboard 0")
        assertThat(device1.isSameDevice(device2), `is`(false))
    }

    @Test
    fun `internal device is not the same as a an external`() {
        val device1 = KeyEventTriggerDevice.Internal
        val device2 = KeyEventTriggerDevice.External("keyboard1", "Keyboard 0")
        assertThat(device1.isSameDevice(device2), `is`(false))
    }

    @Test
    fun `internal device is the same as an internal device`() {
        val device1 = KeyEventTriggerDevice.Internal
        val device2 = KeyEventTriggerDevice.Internal
        assertThat(device1.isSameDevice(device2), `is`(true))
    }

    @Test
    fun `any device is the same as an internal device`() {
        val device1 = KeyEventTriggerDevice.Any
        val device2 = KeyEventTriggerDevice.Internal
        assertThat(device1.isSameDevice(device2), `is`(true))
    }

    @Test
    fun `any device is the same as any device`() {
        val device1 = KeyEventTriggerDevice.Any
        val device2 = KeyEventTriggerDevice.Any
        assertThat(device1.isSameDevice(device2), `is`(true))
    }

    @Test
    fun `any device is the same as an external device`() {
        val device1 = KeyEventTriggerDevice.Any
        val device2 = KeyEventTriggerDevice.External("keyboard1", "Keyboard 0")
        assertThat(device1.isSameDevice(device2), `is`(true))
    }
}
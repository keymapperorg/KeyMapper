package io.github.sds100.keymapper.base.trigger

import android.view.KeyEvent
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.inputevents.Scancode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TriggerErrorSnapshotTest {

    @Test
    fun `do not show error for a key event trigger that did not support screen off remapping to migrate with pro mode`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(false),
        )
    }

    @Test
    fun `do not show error for an evdev trigger to migrate with pro mode`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                    scanCode = Scancode.KEY_VOLUMEDOWN,
                    device = EvdevDeviceInfo("gpio_keys", 0, 0, 0),
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(false),
        )
    }

    @Test
    fun `do not show error for a volume trigger that does not have screen off remap enabled`() {
        val trigger = Trigger(
            legacyDetectScreenOff = false,
            keys = listOf(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(false),
        )
    }

    @Test
    fun `show error for legacy screen off volume triggers with also an evdev key to migrate with pro mode`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                EvdevTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                    scanCode = Scancode.KEY_VOLUMEDOWN,
                    device = EvdevDeviceInfo("gpio_keys", 0, 0, 0),
                ),
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(false),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[1]),
            `is`(true),
        )
    }

    @Test
    fun `show error for legacy screen off volume triggers with multiple keys to migrate with pro mode`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_A,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
                FingerprintTriggerKey(
                    type = FingerprintGestureType.SWIPE_DOWN,
                    clickType = ClickType.LONG_PRESS,
                ),
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(false),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[1]),
            `is`(false),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[2]),
            `is`(true),
        )
    }

    @Test
    fun `show error for legacy screen off volume triggers to migrate with pro mode if pro mode is enabled`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        assertThat(
            TriggerErrorSnapshot.isScreenOffTriggerMigrationRequired(trigger, trigger.keys[0]),
            `is`(true),
        )
    }

    @Test
    fun `show error for legacy screen off volume triggers to migrate with pro mode if pro mode is disabled`() {
        val trigger = Trigger(
            legacyDetectScreenOff = true,
            keys = listOf(
                KeyEventTriggerKey(
                    keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                    clickType = ClickType.SHORT_PRESS,
                    device = KeyEventTriggerDevice.Internal,
                ),
            ),
        )

        val keyMap = KeyMap(trigger = trigger)

        val error = createTriggerErrorSnapshot(
            isSystemBridgeConnected = false,
        ).getTriggerError(keyMap, trigger.keys[0])
        assertThat(error, `is`(TriggerError.MIGRATE_SCREEN_OFF_TRIGGER))
    }

    private fun createTriggerErrorSnapshot(
        isKeyMapperImeChosen: Boolean = true,
        isDndAccessGranted: Boolean = true,
        isRootGranted: Boolean = false,
        purchases: KMResult<Set<ProductId>> = Success(emptySet()),
        showDpadImeSetupError: Boolean = false,
        isSystemBridgeConnected: Boolean? = true,
        evdevDevices: List<EvdevDeviceInfo>? = emptyList(),
    ): TriggerErrorSnapshot {
        return TriggerErrorSnapshot(
            isKeyMapperImeChosen = isKeyMapperImeChosen,
            isDndAccessGranted = isDndAccessGranted,
            isRootGranted = isRootGranted,
            purchases = purchases,
            showDpadImeSetupError = showDpadImeSetupError,
            isSystemBridgeConnected = isSystemBridgeConnected,
            evdevDevices = evdevDevices,
        )
    }
}


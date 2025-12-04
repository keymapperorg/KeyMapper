package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class ConfigTriggerViewModelTest {

    @Test
    fun `switch is visible when system bridge is connected`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isVisible, `is`(true))
    }

    @Test
    fun `switch is not visible when system bridge is disconnected`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Disconnected(
                time = 0L,
                isStoppedByUser = true,
            ),
        )

        assertThat(result.isVisible, `is`(false))
    }

    @Test
    fun `switch is not visible when system bridge is disconnected unexpectedly`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Disconnected(
                time = 0L,
                isStoppedByUser = false,
            ),
        )

        assertThat(result.isVisible, `is`(false))
    }

    @Test
    fun `switch is checked when pro mode recording is enabled`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = true,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isChecked, `is`(true))
    }

    @Test
    fun `switch is not checked when pro mode recording is disabled`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isChecked, `is`(false))
    }

    @Test
    fun `switch is enabled when record trigger state is idle`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isEnabled, `is`(true))
    }

    @Test
    fun `switch is enabled when record trigger state is completed`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Completed(emptyList()),
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isEnabled, `is`(true))
    }

    @Test
    fun `switch is disabled when record trigger state is counting down`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.CountingDown(timeLeft = 3),
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isEnabled, `is`(false))
    }

    @Test
    fun `switch is disabled when counting down even if pro mode recording is enabled`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.CountingDown(timeLeft = 5),
            isProModeRecordingEnabled = true,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isEnabled, `is`(false))
        assertThat(result.isChecked, `is`(true))
    }

    @Test
    fun `switch is visible and checked when connected and enabled`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = true,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isVisible, `is`(true))
        assertThat(result.isChecked, `is`(true))
        assertThat(result.isEnabled, `is`(true))
    }

    @Test
    fun `switch is not visible when disconnected even if recording is enabled`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.Idle,
            isProModeRecordingEnabled = true,
            systemBridgeState = SystemBridgeConnectionState.Disconnected(
                time = 0L,
                isStoppedByUser = true,
            ),
        )

        assertThat(result.isVisible, `is`(false))
        assertThat(result.isChecked, `is`(true))
        assertThat(result.isEnabled, `is`(true))
    }

    @Test
    fun `switch is visible but disabled when counting down and connected`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.CountingDown(timeLeft = 1),
            isProModeRecordingEnabled = true,
            systemBridgeState = SystemBridgeConnectionState.Connected(time = 0L),
        )

        assertThat(result.isVisible, `is`(true))
        assertThat(result.isChecked, `is`(true))
        assertThat(result.isEnabled, `is`(false))
    }

    @Test
    fun `switch is not visible and disabled when counting down and disconnected`() {
        val result = BaseConfigTriggerViewModel.buildProModeSwitchState(
            recordTriggerState = RecordTriggerState.CountingDown(timeLeft = 2),
            isProModeRecordingEnabled = false,
            systemBridgeState = SystemBridgeConnectionState.Disconnected(
                time = 0L,
                isStoppedByUser = false,
            ),
        )

        assertThat(result.isVisible, `is`(false))
        assertThat(result.isChecked, `is`(false))
        assertThat(result.isEnabled, `is`(false))
    }
}

package io.github.sds100.keymapper.base.reroutekeyevents

import android.view.KeyEvent
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputevents.MyKeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.base.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch



/**
 * This is used for the feature created in issue #618 to fix the device IDs of key events
 * on Android 11. There was a bug in the system where enabling an accessibility service
 * would reset the device ID of key events to -1.
 */
class RerouteKeyEventsController(
    private val coroutineScope: CoroutineScope,
    private val useCase: RerouteKeyEventsUseCase,
) {
    /**
     * The job of the key that should be repeating. This should be a down key event for the last
     * key that has been pressed down.
     * The old job should be cancelled whenever the key has been released
     * or a new key has been pressed down
     */
    private var repeatJob: Job? = null

    fun onKeyEvent(event: MyKeyEvent): Boolean {
        if (!useCase.shouldRerouteKeyEvent(event.device?.descriptor)) {
            return false
        }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(
                event.keyCode,
                event.device,
                event.metaState,
                event.scanCode,
            )

            KeyEvent.ACTION_UP -> onKeyUp(
                event.keyCode,
                event.device,
                event.metaState,
                event.scanCode,
            )

            else -> false
        }
    }

    /**
     * @return whether to consume the key event.
     */
    private fun onKeyDown(
        keyCode: Int,
        device: InputDeviceInfo?,
        metaState: Int,
        scanCode: Int = 0,
    ): Boolean {
        val inputKeyModel = InputKeyModel(
            keyCode = keyCode,
            inputType = InputEventType.DOWN,
            metaState = metaState,
            deviceId = device?.id ?: 0,
            scanCode = scanCode,
            repeat = 0,
        )

        useCase.inputKeyEvent(inputKeyModel)

        repeatJob?.cancel()

        repeatJob = coroutineScope.launch {
            delay(400)

            var repeatCount = 1

            while (isActive) {
                useCase.inputKeyEvent(inputKeyModel.copy(repeat = repeatCount))
                delay(50)
                repeatCount++
            }
        }

        return true
    }

    private fun onKeyUp(
        keyCode: Int,
        device: InputDeviceInfo?,
        metaState: Int,
        scanCode: Int = 0,
    ): Boolean {
        repeatJob?.cancel()

        val inputKeyModel = InputKeyModel(
            keyCode = keyCode,
            inputType = InputEventType.UP,
            metaState = metaState,
            deviceId = device?.id ?: 0,
            scanCode = scanCode,
            repeat = 0,
        )

        useCase.inputKeyEvent(inputKeyModel)

        return true
    }
}

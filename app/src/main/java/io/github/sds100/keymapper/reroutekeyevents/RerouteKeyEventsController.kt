package io.github.sds100.keymapper.reroutekeyevents

import android.view.KeyEvent
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Created by sds100 on 27/04/2021.
 */
class RerouteKeyEventsController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val useCase: RerouteKeyEventsUseCase
) {
    /**
     * The job of the key that should be repeating. This should be a down key event for the last
     * key that has been pressed down.
     * The old job should be cancelled whenever the key has been released
     * or a new key has been pressed down
     */
    private var repeatJob: Job? = null

    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ): Boolean {

        return when (action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(
                keyCode,
                device,
                metaState,
                scanCode
            )

            KeyEvent.ACTION_UP -> onKeyUp(
                keyCode,
                device,
                metaState,
                scanCode
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
        scanCode: Int = 0
    ): Boolean {

        if (device != null && !useCase.shouldRerouteKeyEvent(device.descriptor)) {
            return false
        }

        val inputKeyModel = InputKeyModel(
            keyCode = keyCode,
            inputType = InputEventType.DOWN,
            metaState = metaState,
            deviceId = device?.id ?: 0,
            scanCode = scanCode,
            repeat = 0
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
        scanCode: Int = 0
    ): Boolean {

        if (device != null && !useCase.shouldRerouteKeyEvent(device.descriptor)) {
            return false
        }

        repeatJob?.cancel()

        val inputKeyModel = InputKeyModel(
            keyCode = keyCode,
            inputType = InputEventType.UP,
            metaState = metaState,
            deviceId = device?.id ?: 0,
            scanCode = scanCode,
            repeat = 0
        )

        useCase.inputKeyEvent(inputKeyModel)

        return true
    }
}
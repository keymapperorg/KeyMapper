package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import androidx.lifecycle.LiveData
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.util.ImitateButtonPress
import io.github.sds100.keymapper.util.KeyEventAction
import kotlinx.coroutines.*

/**
 * Created by sds100 on 12/03/2021.
 */

class RerouteKeyEventsDelegate(
    private val coroutineScope: CoroutineScope,
    private val isCompatibleImeChosen: () -> Boolean
) {
    var devicesToRerouteKeyEvents = emptySet<String>()

    private val _imitateButtonPress = LiveEvent<ImitateButtonPress>()
    val imitateButtonPress: LiveData<ImitateButtonPress> = _imitateButtonPress

    /**
     * The job of the key that should be repeating. This should be a down key event for the last
     * key that has been pressed down.
     * The old job should be cancelled whenever the key has been released
     * or a new key has been pressed down
     */
    private var repeatJob: RepeatJob? = null

    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int,
        scanCode: Int = 0
    ): Boolean {

        return when (action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(
                keyCode,
                descriptor,
                isExternal,
                metaState,
                deviceId,
                scanCode
            )

            KeyEvent.ACTION_UP -> onKeyUp(
                keyCode,
                descriptor,
                isExternal,
                metaState,
                deviceId,
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
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int,
        scanCode: Int = 0
    ): Boolean {

        /* only reroute the key event if no key maps are detected and a Key Mapper keyboard
        * is being used */
        if (!devicesToRerouteKeyEvents.contains(descriptor) || !isCompatibleImeChosen.invoke()) {
            return false
        }

        val event = ImitateButtonPress(
            keyCode,
            metaState,
            deviceId,
            KeyEventAction.DOWN,
            scanCode,
            repeat = 0
        )

        _imitateButtonPress.value = event

        repeatJob?.cancel()

        repeatJob = RepeatJob(keyCode, descriptor) {
            coroutineScope.launch {
                delay(400)

                var repeatCount = 1

                while (isActive) {
                    _imitateButtonPress.value = event.copy(repeat = repeatCount)
                    delay(50)
                    repeatCount++
                }
            }
        }

        return true
    }

    private fun onKeyUp(
        keyCode: Int,
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int,
        scanCode: Int = 0
    ): Boolean {

        /* only reroute the key event if no key maps are detected and a Key Mapper keyboard
         * is being used */
        if (!devicesToRerouteKeyEvents.contains(descriptor) || !isCompatibleImeChosen.invoke()) {
            return false
        }

        repeatJob?.cancel()

        _imitateButtonPress.value = ImitateButtonPress(
            keyCode,
            metaState,
            deviceId,
            KeyEventAction.UP,
            scanCode
        )

        return true
    }

    private class RepeatJob(
        val keyCode: Int,
        val descriptor: String,
        launch: () -> Job
    ) : Job by launch.invoke()
}
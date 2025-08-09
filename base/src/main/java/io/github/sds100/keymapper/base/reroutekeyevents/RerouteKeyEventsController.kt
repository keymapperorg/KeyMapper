package io.github.sds100.keymapper.base.reroutekeyevents

import android.view.KeyEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventDetectionSource
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.input.InputEventHubCallback
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
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
// TODO remove this feature because it is extra maintenance for a bug that only exists on a small amount of devices.
// TODO update changelog and website, remove strings.
class RerouteKeyEventsController @AssistedInject constructor(
    @Assisted
    private val coroutineScope: CoroutineScope,
    private val keyMapperImeMessenger: ImeInputEventInjector,
    private val useCase: RerouteKeyEventsUseCase,
    private val inputEventHub: InputEventHub,
) : InputEventHubCallback {

    companion object {
        private const val INPUT_EVENT_HUB_ID = "reroute_key_events"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
        ): RerouteKeyEventsController
    }

    /**
     * The job of the key that should be repeating. This should be a down key event for the last
     * key that has been pressed down.
     * The old job should be cancelled whenever the key has been released
     * or a new key has been pressed down
     */
    private var repeatJob: Job? = null

    init {
        coroutineScope.launch {
            useCase.isReroutingEnabled.collect { isEnabled ->
                if (isEnabled) {
                    inputEventHub.registerClient(
                        INPUT_EVENT_HUB_ID,
                        this@RerouteKeyEventsController,
                        listOf(KMEvdevEvent.TYPE_KEY_EVENT),
                    )
                } else {
                    inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
                }
            }
        }
    }

    fun teardown() {
        inputEventHub.unregisterClient(INPUT_EVENT_HUB_ID)
    }

    override fun onInputEvent(
        event: KMInputEvent,
        detectionSource: InputEventDetectionSource,
    ): Boolean {
        if (event !is KMKeyEvent) {
            return false
        }

        if (!useCase.shouldRerouteKeyEvent(event.device.descriptor)) {
            return false
        }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(event)
            KeyEvent.ACTION_UP -> onKeyUp(event)
            else -> false
        }
    }

    /**
     * @return whether to consume the key event.
     */
    private fun onKeyDown(
        event: KMKeyEvent,
    ): Boolean {
        val injectModel = InjectKeyEventModel(
            keyCode = event.keyCode,
            action = KeyEvent.ACTION_DOWN,
            metaState = event.metaState,
            deviceId = event.deviceId,
            scanCode = event.scanCode,
            repeatCount = event.repeatCount,
            source = event.source,
        )

        useCase.inputKeyEvent(injectModel)

        repeatJob?.cancel()

        repeatJob = coroutineScope.launch {
            delay(400)

            var repeatCount = 1

            while (isActive) {
                useCase.inputKeyEvent(injectModel.copy(repeatCount = repeatCount))
                delay(50)
                repeatCount++
            }
        }

        return true
    }

    private fun onKeyUp(event: KMKeyEvent): Boolean {
        repeatJob?.cancel()

        val inputKeyModel = InjectKeyEventModel(
            keyCode = event.keyCode,
            action = KeyEvent.ACTION_UP,
            metaState = event.metaState,
            deviceId = event.deviceId,
            scanCode = event.scanCode,
            repeatCount = event.repeatCount,
            source = event.source,
        )

        useCase.inputKeyEvent(inputKeyModel)

        return true
    }
}

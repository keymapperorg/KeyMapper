package io.github.sds100.keymapper.base.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.input.IInputManager
import android.os.SystemClock
import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputevents.InputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.base.utils.InputEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

@SuppressLint("PrivateApi")
class ShizukuInputEventInjector : InputEventInjector {

    companion object {
        // private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0

        private const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    }

    private val iInputManager: IInputManager by lazy {
        val binder =
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.INPUT_SERVICE))
        IInputManager.Stub.asInterface(binder)
    }

    override suspend fun inputKeyEvent(model: InputKeyModel) {
        Timber.d("Inject input event with Shizuku ${KeyEvent.keyCodeToString(model.keyCode)}, $model")

        val action = when (model.inputType) {
            InputEventType.DOWN, InputEventType.DOWN_UP -> KeyEvent.ACTION_DOWN
            InputEventType.UP -> KeyEvent.ACTION_UP
        }

        val eventTime = SystemClock.uptimeMillis()

        val keyEvent = createInjectedKeyEvent(eventTime, action, model)

        withContext(Dispatchers.IO) {
            // MUST wait for the application to finish processing the event before sending the next one.
            // Otherwise, rapidly repeating input events will go in a big queue and all inputs
            // into the application will be delayed or overloaded.
            iInputManager.injectInputEvent(keyEvent, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)

            if (model.inputType == InputEventType.DOWN_UP) {
                val upEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP)

                iInputManager.injectInputEvent(upEvent, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
            }
        }
    }
}

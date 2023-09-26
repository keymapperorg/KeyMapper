package io.github.sds100.keymapper.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.input.IInputManager
import android.os.SystemClock
import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.util.InputEventType
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

/**
 * Created by sds100 on 21/04/2021.
 */

@SuppressLint("PrivateApi")
class ShizukuInputEventInjector : InputEventInjector {

    companion object {
        private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    }

    private val iInputManager: IInputManager by lazy {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.INPUT_SERVICE))
        IInputManager.Stub.asInterface(binder)
    }

    override fun inputKeyEvent(model: InputKeyModel) {
        Timber.d("Inject input event with Shizuku ${KeyEvent.keyCodeToString(model.keyCode)}, $model")

        val action = when (model.inputType) {
            InputEventType.DOWN, InputEventType.DOWN_UP -> KeyEvent.ACTION_DOWN
            InputEventType.UP -> KeyEvent.ACTION_UP
        }

        val eventTime = SystemClock.uptimeMillis()

        val keyEvent = KeyEvent(
            eventTime,
            eventTime,
            action,
            model.keyCode,
            model.repeat,
            model.metaState,
            model.deviceId,
            model.scanCode
        )

        iInputManager.injectInputEvent(keyEvent, INJECT_INPUT_EVENT_MODE_ASYNC)

        if (model.inputType == InputEventType.DOWN_UP) {
            val upEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP)

            iInputManager.injectInputEvent(upEvent, INJECT_INPUT_EVENT_MODE_ASYNC)
        }
    }
}

interface InputEventInjector {
    fun inputKeyEvent(model: InputKeyModel)
}
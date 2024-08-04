package io.github.sds100.keymapper.system.inputmethod

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import io.github.sds100.keymapper.api.KeyEventRelayServiceWrapper
import io.github.sds100.keymapper.shizuku.InputEventInjector
import io.github.sds100.keymapper.util.InputEventType
import timber.log.Timber

/**
 * Created by sds100 on 21/04/2021.
 */

/**
 * This class handles communicating with the Key Mapper input method services
 * so key events and text can be inputted.
 */
class KeyMapperImeMessengerImpl(
    context: Context,
    private val keyEventRelayService: KeyEventRelayServiceWrapper,
    private val inputMethodAdapter: InputMethodAdapter,
) : KeyMapperImeMessenger {

    companion object {
        // DON'T CHANGE THESE!!!
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN_UP"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_DOWN"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_UP"
        private const val KEY_MAPPER_INPUT_METHOD_ACTION_TEXT =
            "io.github.sds100.keymapper.inputmethod.ACTION_INPUT_TEXT"

        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_KEY_EVENT"
        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"
    }

    private val ctx = context.applicationContext

    override fun inputKeyEvent(model: InputKeyModel) {
        Timber.d("Inject key event with input method ${KeyEvent.keyCodeToString(model.keyCode)}, $model")

        val imePackageName = inputMethodAdapter.chosenIme.value?.packageName

        if (imePackageName == null) {
            Timber.e("Can't input key event action because no ime is chosen.")
            return
        }

        // Only use the new key event relay service on Android 14+ because
        // it introduced a 1 second delay for broadcasts to context-registered
        // receivers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            inputKeyEventRelayService(model, imePackageName)
        } else {
            inputKeyEventBroadcast(model, imePackageName)
        }
    }

    private fun inputKeyEventBroadcast(model: InputKeyModel, imePackageName: String) {
        val intentAction = when (model.inputType) {
            InputEventType.DOWN -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN
            InputEventType.DOWN_UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP
            InputEventType.UP -> KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP
        }

        Intent(intentAction).apply {
            setPackage(imePackageName)

            val action = when (model.inputType) {
                InputEventType.DOWN, InputEventType.DOWN_UP -> KeyEvent.ACTION_DOWN
                InputEventType.UP -> KeyEvent.ACTION_UP
            }

            val eventTime = SystemClock.uptimeMillis()

            val keyEvent = createKeyEvent(eventTime, action, model)

            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT, keyEvent)

            ctx.sendBroadcast(this)
        }
    }

    private fun createKeyEvent(
        eventTime: Long,
        action: Int,
        model: InputKeyModel,
    ): KeyEvent = KeyEvent(
        eventTime,
        eventTime,
        action,
        model.keyCode,
        model.repeat,
        model.metaState,
        model.deviceId,
        model.scanCode,
    )

    private fun inputKeyEventRelayService(model: InputKeyModel, imePackageName: String) {
        val eventTime = SystemClock.uptimeMillis()

        when (model.inputType) {
            InputEventType.DOWN_UP -> {
                val downKeyEvent = createKeyEvent(eventTime, KeyEvent.ACTION_DOWN, model)
                keyEventRelayService.sendKeyEvent(downKeyEvent, imePackageName)

                val upKeyEvent = createKeyEvent(eventTime, KeyEvent.ACTION_UP, model)
                keyEventRelayService.sendKeyEvent(upKeyEvent, imePackageName)
            }

            InputEventType.DOWN -> {
                val downKeyEvent = createKeyEvent(eventTime, KeyEvent.ACTION_DOWN, model)
                keyEventRelayService.sendKeyEvent(downKeyEvent, imePackageName)
            }

            InputEventType.UP -> {
                val upKeyEvent = createKeyEvent(eventTime, KeyEvent.ACTION_UP, model)
                keyEventRelayService.sendKeyEvent(upKeyEvent, imePackageName)
            }
        }
    }

    override fun inputText(text: String) {
        Timber.d("Input text through IME $text")

        val imePackageName = inputMethodAdapter.chosenIme.value?.packageName

        if (imePackageName == null) {
            Timber.e("Can't input text action because no ime is chosen.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            inputTextRelayService(text, imePackageName)
        } else {
            inputTextBroadcast(text, imePackageName)
        }
    }

    private fun inputTextBroadcast(text: String, imePackageName: String) {
        Intent(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT).apply {
            setPackage(imePackageName)

            putExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT, text)
            ctx.sendBroadcast(this)
        }
    }

    private fun inputTextRelayService(text: String, imePackageName: String) {
        // taken from android.view.inputmethod.BaseInputConnection.sendCurrentText()

        if (text.isEmpty()) {
            return
        }

        if (text.length == 1) {
            // If it's 1 character, we have a chance of being
            // able to generate normal key events...
            val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

            val chars = text.toCharArray(startIndex = 0, endIndex = 1)

            val events: Array<KeyEvent> = keyCharacterMap.getEvents(chars)

            for (i in events.indices) {
                keyEventRelayService.sendKeyEvent(events[i], imePackageName)
            }

            return
        }

        // Otherwise, revert to the special key event containing
        // the actual characters.
        val event = KeyEvent(
            SystemClock.uptimeMillis(),
            text,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
        )

        keyEventRelayService.sendKeyEvent(event, imePackageName)
    }
}

interface KeyMapperImeMessenger : InputEventInjector {
    fun inputText(text: String)
}

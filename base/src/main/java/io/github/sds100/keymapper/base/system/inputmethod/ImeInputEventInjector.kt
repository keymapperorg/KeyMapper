package io.github.sds100.keymapper.base.system.inputmethod

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyEventRelayServiceWrapper
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * This class handles communicating with the Key Mapper input method services
 * so key events and text can be inputted.
 */
@Singleton
class ImeInputEventInjectorImpl @Inject constructor(
    private val keyEventRelayService: KeyEventRelayServiceWrapper,
    private val inputMethodAdapter: InputMethodAdapter,
) : ImeInputEventInjector {
    companion object {
        private const val CALLBACK_ID_INPUT_METHOD = "input_method"
    }

    override fun inputKeyEvent(event: InjectKeyEventModel) {
        Timber.d("Inject key event with input method $event")

        val imePackageName = inputMethodAdapter.chosenIme.value?.packageName

        if (imePackageName == null) {
            Timber.e("Can't input key event action because no ime is chosen.")
            return
        }

        keyEventRelayService.sendKeyEvent(
            event.toAndroidKeyEvent(),
            imePackageName,
            CALLBACK_ID_INPUT_METHOD,
        )
    }

    override fun inputText(text: String) {
        Timber.d("Input text through IME $text")

        val imePackageName = inputMethodAdapter.chosenIme.value?.packageName

        if (imePackageName == null) {
            Timber.e("Can't input text action because no ime is chosen.")
            return
        }

        inputTextRelayService(text, imePackageName)
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

            val events: Array<KeyEvent>? = keyCharacterMap.getEvents(chars)

            // The events can be null if there isn't a way to input the character
            // with the current key character map.
            if (events != null) {
                for (e in events) {
                    keyEventRelayService.sendKeyEvent(
                        e,
                        imePackageName,
                        CALLBACK_ID_INPUT_METHOD,
                    )
                }

                return
            }
        }

        // Otherwise, revert to the special key event containing
        // the actual characters.
        val event = KeyEvent(
            SystemClock.uptimeMillis(),
            text,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
        )

        keyEventRelayService.sendKeyEvent(
            event,
            imePackageName,
            CALLBACK_ID_INPUT_METHOD,
        )
    }
}

interface ImeInputEventInjector {
    fun inputText(text: String)
    fun inputKeyEvent(event: InjectKeyEventModel)
}

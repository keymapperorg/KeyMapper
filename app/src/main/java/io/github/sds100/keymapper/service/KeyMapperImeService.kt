package io.github.sds100.keymapper.service

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.result.KeyMapperImeNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager

/**
 * Created by sds100 on 31/03/2020.
 */

class KeyMapperImeService : InputMethodService(), LifecycleOwner {
    companion object {
        const val EVENT_INPUT_DOWN_UP = "input_down_up"
        const val EVENT_INPUT_TEXT = "input_text"

        fun isServiceEnabled(): Boolean {
            val enabledMethods = inputMethodManager.enabledInputMethodList ?: return false

            return enabledMethods.any { it.packageName == PACKAGE_NAME }
        }

        /**
         * Get the id for the Key Mapper input input_method.
         */
        fun getImeId(): Result<String> {

            val inputMethod = inputMethodManager.inputMethodList.find { it.packageName == PACKAGE_NAME }
                ?: return KeyMapperImeNotFound()

            return Success(inputMethod.id)
        }

        /**
         * @return whether the Key Mapper input input_method is chosen
         */
        fun isInputMethodChosen(): Boolean {
            //get the current input input_method
            val chosenImeId = Settings.Secure.getString(
                appCtx.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )

            return inputMethodManager.inputMethodList.find { it.id == chosenImeId }?.packageName == PACKAGE_NAME
        }

        private lateinit var BUS: MutableLiveData<Event<Pair<String, Any?>>>

        @MainThread
        fun provideBus(): MutableLiveData<Event<Pair<String, Any?>>> {
            BUS = if (::BUS.isInitialized) BUS else MutableLiveData()

            return BUS
        }
    }

    private lateinit var mLifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        mLifecycleRegistry = LifecycleRegistry(this)
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED

        provideBus().observe(this, EventObserver {
            when (it.first) {
                EVENT_INPUT_TEXT -> {
                    val text = it.second as String

                    currentInputConnection?.commitText(text, 1)
                }

                EVENT_INPUT_DOWN_UP -> {
                    val keyCode = (it.second as IntArray)[0]
                    val metaState = (it.second as IntArray)[1]

                    val eventTime = SystemClock.uptimeMillis()

                    val downEvent = KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_DOWN, keyCode, 0, metaState)

                    currentInputConnection?.sendKeyEvent(downEvent)

                    val upEvent = KeyEvent(eventTime, SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP, keyCode, 0)

                    currentInputConnection?.sendKeyEvent(upEvent)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = mLifecycleRegistry

    private val InputConnection.charCount: Int
        get() {
            val request = ExtractedTextRequest().apply {
                token = 0
            }

            return getExtractedText(request, 0).text.length
        }
}
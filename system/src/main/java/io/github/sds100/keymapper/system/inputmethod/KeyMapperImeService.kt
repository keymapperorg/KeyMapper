package io.github.sds100.keymapper.system.inputmethod

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.UserManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * DO NOT MOVE. Must stay in this package so the user's input method settings are not reset
 * when the component name changes.
 */
@AndroidEntryPoint
class KeyMapperImeService : InputMethodService() {
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

        private const val KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_TEXT"
        const val KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT =
            "io.github.sds100.keymapper.inputmethod.EXTRA_KEY_EVENT"

        private const val CALLBACK_ID_ACCESSIBILITY_SERVICE = "accessibility_service"
        private const val CALLBACK_ID_INPUT_METHOD = "input_method"
    }

    @Inject
    lateinit var buildConfigProvider: BuildConfigProvider
    private val userManager: UserManager? by lazy { getSystemService<UserManager>() }
    private val inputMethodManager: InputMethodManager? by lazy {
        getSystemService<InputMethodManager>()
    }

    private val keyguardManager: KeyguardManager? by lazy {
        getSystemService<KeyguardManager>()
    }

    @Inject
    lateinit var serviceAdapter: AccessibilityServiceAdapter

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            when (action) {
                KEY_MAPPER_INPUT_METHOD_ACTION_TEXT -> {
                    val text = intent.getStringExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT) ?: return

                    currentInputConnection?.commitText(text, 1)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP -> {
                    val downEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )
                    currentInputConnection?.sendKeyEvent(downEvent)

                    val upEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_UP)
                    currentInputConnection?.sendKeyEvent(upEvent)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN -> {
                    var downEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )

                    downEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_DOWN)

                    currentInputConnection?.sendKeyEvent(downEvent)
                }

                KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP -> {
                    var upEvent = intent.getParcelableExtra<KeyEvent>(
                        KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT,
                    )

                    upEvent = KeyEvent.changeAction(upEvent, KeyEvent.ACTION_UP)

                    currentInputConnection?.sendKeyEvent(upEvent)
                }
            }
        }
    }

    private val keyEventReceiverCallback: IKeyEventRelayServiceCallback =
        object : IKeyEventRelayServiceCallback.Stub() {
            override fun onKeyEvent(event: KeyEvent?): Boolean {
                // Only accept key events from Key Mapper
                return currentInputConnection?.sendKeyEvent(event) ?: false
            }

            override fun onMotionEvent(event: MotionEvent?): Boolean {
                // Do nothing if the IME receives a motion event.
                return false
            }
        }

    @Inject
    lateinit var keyEventRelayServiceWrapper: KeyEventRelayServiceWrapper

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP)
        filter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT)

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        keyEventRelayServiceWrapper.registerClient(
            CALLBACK_ID_INPUT_METHOD,
            keyEventReceiverCallback
        )
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // IMPORTANT! Select a keyboard with an actual GUI if the user needs
        // to unlock their device. This must not be in onCreate because
        // the switchInputMethod does not work there.
        if (userManager?.isUserUnlocked == false) {
            selectNonBasicKeyboard()
        } else if (!restarting && keyguardManager?.isDeviceLocked == true) {
            selectNonBasicKeyboard()
        }

        // Only send the start input event on versions before the accessibility
        // input method API was introduced
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && attribute != null) {
            serviceAdapter.sendAsync(
                AccessibilityServiceEvent.OnKeyMapperImeStartInput(
                    attribute = attribute,
                    restarting = restarting
                )
            )
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event ?: return super.onGenericMotionEvent(null)

        val consume = keyEventRelayServiceWrapper.sendMotionEvent(
            event = event,
            targetPackageName = buildConfigProvider.packageName,
            callbackId = CALLBACK_ID_ACCESSIBILITY_SERVICE,
        )

        return if (consume) {
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event ?: return super.onKeyDown(keyCode, null)

        val consume = keyEventRelayServiceWrapper.sendKeyEvent(
            event = event,
            targetPackageName = buildConfigProvider.packageName,
            callbackId = CALLBACK_ID_ACCESSIBILITY_SERVICE,
        )

        return if (consume) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event ?: return super.onKeyUp(keyCode, null)

        val consume = keyEventRelayServiceWrapper.sendKeyEvent(
            event = event,
            targetPackageName = buildConfigProvider.packageName,
            callbackId = CALLBACK_ID_ACCESSIBILITY_SERVICE,
        )

        return if (consume) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        keyEventRelayServiceWrapper.unregisterClient(CALLBACK_ID_INPUT_METHOD)

        super.onDestroy()
    }

    @SuppressLint("LogNotTimber")
    private fun selectNonBasicKeyboard() {
        inputMethodManager ?: return

        inputMethodManager!!.enabledInputMethodList
            .filter {
                it.packageName != "io.github.sds100.keymapper" &&
                    it.packageName != "io.github.sds100.keymapper.debug" &&
                    it.packageName != "io.github.sds100.keymapper.ci"
            }
            // Select a random one in case one of them can't be used on the lock screen such as
            // the Google Voice Typing keyboard. This is critical because if an input method can't be used
            // then it will select the Key Mapper Basic Input method again and loop forever.
            .randomOrNull()
            ?.also {
                Timber.e(
                    KeyMapperImeService::class.simpleName,
                    "Device is locked! Select ${it.id} input method",
                )
                switchInputMethod(it.id)
            }
    }
}

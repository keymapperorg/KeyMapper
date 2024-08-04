package io.github.sds100.keymapper.api

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.DeadObjectException
import android.os.IBinder
import android.view.KeyEvent
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * This service is used as a relay between the accessibility service and input method service to pass
 * key events back and forth. A separate service has to be used because you can't bind to an
 * accessibility or input method service.
 *
 * This is used for actions that input key events. The input method service registers a callback
 * and the accessibility service sends the key events.
 *
 * This was originally implemented in issue #850 for the action to answer phone calls
 * because Android doesn't pass volume down key events to the accessibility service
 * when the phone is ringing or it is in a phone call.
 * The accessibility service registers a callback and the input method service
 * sends the key events.
 */
class KeyEventRelayService : Service() {
    companion object {
        const val ACTION_REBIND_RELAY_SERVICE =
            "io.github.sds100.keymapper.ACTION_REBIND_RELAY_SERVICE"
    }

    val permittedPackages = KeyMapperImeHelper.KEY_MAPPER_IME_PACKAGE_LIST

    private val binderInterface: IKeyEventRelayService = object : IKeyEventRelayService.Stub() {
        override fun sendKeyEvent(event: KeyEvent?, targetPackageName: String?): Boolean {
            synchronized(callbackLock) {
                Timber.d("KeyEventRelayService: onKeyEvent ${event?.keyCode}")

                val callback = callbacks[targetPackageName]

                if (callback == null) {
                    return false
                } else {
                    try {
                        // Get the process ID of the app that called this service.
                        val sourcePackageName = getCallerPackageName() ?: return false

                        if (!permittedPackages.contains(sourcePackageName)) {
                            Timber.d("An unrecognized package $sourcePackageName tried to send a key event.")

                            return false
                        }

                        return callback.onKeyEvent(event, targetPackageName)
                    } catch (e: DeadObjectException) {
                        // If the application is no longer connected then delete the callback.
                        callbacks.remove(targetPackageName)
                        return false
                    }
                }
            }
        }

        override fun registerCallback(client: IKeyEventRelayServiceCallback?) {
            val sourcePackageName = getCallerPackageName() ?: return

            if (client == null || !permittedPackages.contains(sourcePackageName)) {
                Timber.d("An unrecognized package $sourcePackageName tried to register a key event relay callback.")
                return
            }

            synchronized(callbackLock) {
                Timber.d("Package $sourcePackageName registered a key event relay callback.")
                callbacks[sourcePackageName] = client
            }
        }

        override fun unregisterCallback() {
            synchronized(callbackLock) {
                val sourcePackageName = getCallerPackageName() ?: return

                Timber.d("Package $sourcePackageName unregistered a key event relay callback.")

                callbacks.remove(sourcePackageName)
            }
        }
    }

    private val callbackLock: Any = Any()
    private var callbacks: ConcurrentHashMap<String, IKeyEventRelayServiceCallback> =
        ConcurrentHashMap()

    override fun onCreate() {
        super.onCreate()

        val intent = Intent(ACTION_REBIND_RELAY_SERVICE)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This service is explicitly started and stopped as needed
        // so the system shouldn't stop it automatically.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = binderInterface.asBinder()

    private fun getCallerPackageName(): String? {
        val sourceUid = Binder.getCallingUid()
        return applicationContext.packageManager.getNameForUid(sourceUid)
    }
}

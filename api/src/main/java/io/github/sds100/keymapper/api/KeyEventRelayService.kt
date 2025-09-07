package io.github.sds100.keymapper.api

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.DeadObjectException
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.view.KeyEvent
import android.view.MotionEvent
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
 * when the phone is ringing or it is in a phone call. Later, in Android 14 this relay must be
 * used because they also introduced a 1-second delay to context-registered broadcast receivers.
 * And who knows what other restrictions will be added in the future :)
 *
 * The accessibility service registers a callback and the input method service
 * sends the key events.
 */
class KeyEventRelayService : Service() {
    companion object {
        const val ACTION_REBIND_RELAY_SERVICE =
            "io.github.sds100.keymapper.ACTION_REBIND_RELAY_SERVICE"

        /**
         * Used when a client registers a callback without specifying an ID.
         */
        private const val CALLBACK_ID_DEFAULT = "default"

        const val KEY_MAPPER_GUI_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.latin"

        private const val KEY_MAPPER_LEANBACK_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.leanback"

        private const val KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.hackers"
    }

    val permittedPackages by lazy {
        arrayOf(
            packageName,
            KEY_MAPPER_GUI_IME_PACKAGE,
            KEY_MAPPER_LEANBACK_IME_PACKAGE,
            KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE,
        )
    }

    private val binderInterface: IKeyEventRelayService = object : IKeyEventRelayService.Stub() {
        override fun sendKeyEvent(
            event: KeyEvent?,
            targetPackageName: String?,
            callbackId: String?,
        ): Boolean {
            event ?: return false
            targetPackageName ?: return false

            val key = ClientKey(targetPackageName, callbackId ?: CALLBACK_ID_DEFAULT)

            try {
                val connection = clientConnections[key] ?: return false
                var consumeKeyEvent = false

                if (connection.callback.onKeyEvent(event)) {
                    consumeKeyEvent = true
                }

                return consumeKeyEvent
            } catch (e: DeadObjectException) {
                // If the application is no longer connected then delete the callback.
                synchronized(callbackLock) {
                    removeConnection(key)
                }
                return false
            }
        }

        override fun sendMotionEvent(
            event: MotionEvent?,
            targetPackageName: String?,
            callbackId: String?,
        ): Boolean {
            event ?: return false
            targetPackageName ?: return false
            val key = ClientKey(targetPackageName, callbackId ?: CALLBACK_ID_DEFAULT)

            try {
                val connection = clientConnections[key] ?: return false
                var consumeMotionEvent = false

                if (connection.callback.onMotionEvent(event)) {
                    consumeMotionEvent = true
                }

                return consumeMotionEvent
            } catch (e: DeadObjectException) {
                // If the application is no longer connected then delete the callback.
                synchronized(callbackLock) {
                    removeConnection(key)
                }
                return false
            }
        }

        override fun registerCallback(client: IKeyEventRelayServiceCallback?, id: String?) {
            val sourcePackageName = getCallerPackageName() ?: return

            if (client == null || !permittedPackages.contains(sourcePackageName)) {
                Timber.d("An unrecognized package $sourcePackageName tried to register a key event relay callback.")
                return
            }

            synchronized(callbackLock) {
                Timber.d("Package $sourcePackageName registered a key event relay callback.")

                val key = ClientKey(sourcePackageName, id ?: CALLBACK_ID_DEFAULT)

                // Handle disconnecting the client. Unlink the death recipient.
                if (clientConnections.containsKey(key)) {
                    removeConnection(key)
                }

                val connection = ClientConnection(key, client)
                clientConnections[key] = connection
                client.asBinder().linkToDeath(connection, 0)
            }
        }

        override fun unregisterCallback(callbackId: String?) {
            val sourcePackageName = getCallerPackageName() ?: return

            // The callback id is not passed on older versions of the API.
            if (callbackId == null) {
                Timber.d("Package $sourcePackageName unregistered all key event relay callbacks.")

                removeAllConnections(sourcePackageName)
            } else {
                Timber.d("Package $sourcePackageName unregistered a key event relay callback.")

                removeConnection(ClientKey(sourcePackageName, callbackId))
            }
        }
    }

    private val callbackLock: Any = Any()
    private var clientConnections: ConcurrentHashMap<ClientKey, ClientConnection> =
        ConcurrentHashMap()

    override fun onCreate() {
        super.onCreate()

        Timber.d("Relay service: onCreate")
        val intent = Intent(ACTION_REBIND_RELAY_SERVICE)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This service is explicitly started and stopped as needed
        // so the system shouldn't stop it automatically.
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("Relay service: onDestroy")
        clientConnections.clear()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = binderInterface.asBinder()

    /**
     * IMPORTANT! This takes about 1ms to execute so do not use it when latency is critical.
     */
    private fun getCallerPackageName(): String? {
        val sourceUid = Binder.getCallingUid()
        return applicationContext.packageManager.getNameForUid(sourceUid)
    }

    private fun removeConnection(key: ClientKey) {
        val connection = clientConnections.remove(key) ?: return

        // Unlink the death recipient from the connection to remove and
        // delete it from the list of connections for the package.
        connection.callback.asBinder().unlinkToDeath(connection, 0)
    }

    private fun removeAllConnections(packageName: String) {
        synchronized(callbackLock) {
            for (key in clientConnections.keys()) {
                if (key.packageName == packageName) {
                    removeConnection(key)
                }
            }
        }
    }

    private inner class ClientConnection(
        private val clientKey: ClientKey,
        val callback: IKeyEventRelayServiceCallback,
    ) : DeathRecipient {
        override fun binderDied() {
            Timber.d("Client binder died: $clientKey")
            synchronized(callbackLock) {
                removeConnection(clientKey)
            }
        }
    }

    data class ClientKey(val packageName: String, val callbackId: String)
}

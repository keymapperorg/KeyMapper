package io.github.sds100.keymapper.priv.provider

import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import moe.shizuku.api.BinderContainer

/**
 *
 *
 * This provider receives binder from Shizuku server. When app process starts,
 * Shizuku server (it runs under adb/root) will send the binder to client apps with this provider.
 *
 *
 *
 * Add the provider to your manifest like this:
 *
 * <pre class="prettyprint">&lt;manifest&gt;
 * ...
 * &lt;application&gt;
 * ...
 * &lt;provider
 * android:name="rikka.shizuku.ShizukuProvider"
 * android:authorities="${applicationId}.shizuku"
 * android:exported="true"
 * android:multiprocess="false"
 * android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"
 * &lt;/provider&gt;
 * ...
 * &lt;/application&gt;
 * &lt;/manifest&gt;</pre>
 *
 *
 *
 * There are something needs you attention:
 *
 *
 *  1. `android:permission` shoule be a permission that granted to Shell (com.android.shell)
 * but not normal apps (e.g., android.permission.INTERACT_ACROSS_USERS_FULL), so that it can only
 * be used by the app itself and Shizuku server.
 *  1. `android:exported` must be `true` so that the provider can be accessed
 * from Shizuku server runs under adb.
 *  1. `android:multiprocess` must be `false`
 * since Shizuku server only gets uid when app starts.
 *
 *
 *
 * If your app runs in multiple processes, this provider also provides the functionality of sharing
 * the binder across processes. See [.enableMultiProcessSupport].
 *
 */
class PrivBinderProvider : ContentProvider() {
    override fun attachInfo(context: Context?, info: ProviderInfo) {
        super.attachInfo(context, info)

        check(!info.multiprocess) { "android:multiprocess must be false" }

        check(info.exported) { "android:exported must be true" }

        isProviderProcess = true
    }

    override fun onCreate(): Boolean {
        if (enableSuiInitialization && !Sui.isSui()) {
            val result: Boolean = Sui.init(context!!.packageName)
            Log.d(TAG, "Initialize Sui: " + result)
        }
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (Sui.isSui()) {
            Log.w(
                TAG,
                "Provider called when Sui is available. Are you using Shizuku and Sui at the same time?"
            )
            return Bundle()
        }

        if (extras == null) {
            return null
        }

        extras.classLoader = BinderContainer::class.java.getClassLoader()

        val reply = Bundle()
        when (method) {
            METHOD_SEND_BINDER -> {
                handleSendBinder(extras)
            }

            METHOD_GET_BINDER -> {
                if (!handleGetBinder(reply)) {
                    return null
                }
            }
        }
        return reply
    }

    private fun handleSendBinder(extras: Bundle) {
        if (Shizuku.pingBinder()) {
            Log.d(TAG, "sendBinder is called when already a living binder")
            return
        }

        val container: BinderContainer? = extras.getParcelable(EXTRA_BINDER)
        if (container != null && container.binder != null) {
            Log.d(TAG, "binder received")

            Shizuku.onBinderReceived(container.binder, context!!.packageName)

            if (enableMultiProcess) {
                Log.d(TAG, "broadcast binder")

                val intent: Intent = Intent(ACTION_BINDER_RECEIVED)
                    .putExtra(EXTRA_BINDER, container)
                    .setPackage(context!!.packageName)
                context!!.sendBroadcast(intent)
            }
        }
    }

    private fun handleGetBinder(reply: Bundle): Boolean {
        // Other processes in the same app can read the provider without permission
        val binder: IBinder? = Shizuku.getBinder()
        if (binder == null || !binder.pingBinder()) return false

        reply.putParcelable(EXTRA_BINDER, BinderContainer(binder))
        return true
    }

    // no other provider methods
    override fun query(
        uri: Uri,
        projection: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        return 0
    }

    companion object {
        private const val TAG = "ShizukuProvider"

        // For receive Binder from Shizuku
        const val METHOD_SEND_BINDER: String = "sendBinder"

        // For share Binder between processes
        const val METHOD_GET_BINDER: String = "getBinder"

        const val ACTION_BINDER_RECEIVED: String = "moe.shizuku.api.action.BINDER_RECEIVED"

        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"

        const val PERMISSION: String = "moe.shizuku.manager.permission.API_V23"

        const val MANAGER_APPLICATION_ID: String = "moe.shizuku.privileged.api"

        private const val enableMultiProcess = false

        private var isProviderProcess = false

        private const val enableSuiInitialization = true

        fun setIsProviderProcess(isProviderProcess: Boolean) {
            ShizukuProvider.isProviderProcess = isProviderProcess
        }

        /**
         * Enables built-in multi-process support.
         *
         *
         * This method MUST be called as early as possible (e.g., static block in Application).
         */
        fun enableMultiProcessSupport(isProviderProcess: Boolean) {
            Log.d(
                TAG,
                "Enable built-in multi-process support (from " + (if (isProviderProcess) "provider process" else "non-provider process") + ")"
            )

            ShizukuProvider.isProviderProcess = isProviderProcess
            ShizukuProvider.enableMultiProcess = true
        }

        /**
         * Disable automatic Sui initialization.
         */
        fun disableAutomaticSuiInitialization() {
            ShizukuProvider.enableSuiInitialization = false
        }

        /**
         * Require binder for non-provider process, should have [.enableMultiProcessSupport] called first.
         *
         * @param context Context
         */
        fun requestBinderForNonProviderProcess(context: Context) {
            if (isProviderProcess) {
                return
            }

            Log.d(TAG, "request binder in non-provider process")

            val receiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val container: BinderContainer? = intent.getParcelableExtra(EXTRA_BINDER)
                    if (container != null && container.binder != null) {
                        Log.i(TAG, "binder received from broadcast")
                        Shizuku.onBinderReceived(container.binder, context.packageName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(ACTION_BINDER_RECEIVED),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, IntentFilter(ACTION_BINDER_RECEIVED))
            }

            var reply: Bundle?
            try {
                reply = context.contentResolver.call(
                    Uri.parse("content://" + context.packageName + ".shizuku"),
                    ShizukuProvider.METHOD_GET_BINDER, null, Bundle()
                )
            } catch (tr: Throwable) {
                reply = null
            }

            if (reply != null) {
                reply.classLoader = BinderContainer::class.java.getClassLoader()

                val container: BinderContainer? = reply.getParcelable(EXTRA_BINDER)
                if (container != null && container.binder != null) {
                    Log.i(TAG, "Binder received from other process")
                    Shizuku.onBinderReceived(container.binder, context.packageName)
                }
            }
        }
    }
}

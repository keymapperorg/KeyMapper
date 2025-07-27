package io.github.sds100.keymapper.sysbridge.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManagerImpl
import timber.log.Timber

/**
 * Taken from the ShizukuProvider class.
 *
 * This provider receives the Binder from the system bridge. When app process starts,
 * the system bridge (it runs under adb/root) will send the binder to client apps with this provider.
 */
internal class SystemBridgeBinderProvider : ContentProvider() {
    companion object {
        // For receive Binder from Shizuku
        const val METHOD_SEND_BINDER: String = "sendBinder"

        const val EXTRA_BINDER = "io.github.sds100.keymapper.sysbridge.EXTRA_BINDER"
    }

    private val systemBridgeManager: SystemBridgeManagerImpl by lazy {
        val appContext = context?.applicationContext ?: throw IllegalStateException()
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                appContext,
                SystemBridgeProviderEntryPoint::class.java
            )

        hiltEntryPoint.systemBridgeManager()
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (extras == null) {
            return null
        }

        extras.classLoader = BinderContainer::class.java.getClassLoader()

        val reply = Bundle()
        when (method) {
            METHOD_SEND_BINDER -> {
                handleSendBinder(extras)
            }
        }
        return reply
    }

    private fun handleSendBinder(extras: Bundle) {
        if (systemBridgeManager.pingBinder()) {
            Timber.d("sendBinder is called when there is already a Binder from the system bridge.")
            return
        }

        val container: BinderContainer? = BundleCompat.getParcelable(
            extras, EXTRA_BINDER,
            BinderContainer::class.java
        )

        if (container != null && container.binder != null) {
            Timber.d("binder received")

            systemBridgeManager.onBinderReceived(container.binder)
        }
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

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SystemBridgeProviderEntryPoint {
        fun systemBridgeManager(): SystemBridgeManagerImpl
    }
}
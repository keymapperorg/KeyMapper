package io.github.sds100.keymapper

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import io.github.sds100.keymapper.data.automaticBackupLocation
import io.github.sds100.keymapper.data.darkThemeMode
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.FileAccessDenied
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.MainScope
import timber.log.Timber
import java.io.OutputStream

/**
 * Created by sds100 on 19/05/2020.
 */
class MyApplication : MultiDexApplication(),
    IContentResolver, INotificationManagerWrapper, INotificationController {
    val appCoroutineScope = MainScope()

    val notificationController by lazy {
        NotificationController(
            appCoroutineScope,
            manager = this,
            ServiceLocator.globalPreferences(this),
            iNotificationController = this
        )
    }

    override fun onCreate() {

        ServiceLocator.globalPreferences(this@MyApplication)
            .darkThemeMode
            .firstBlocking()
            .let { AppCompatDelegate.setDefaultNightMode(it) }

        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun openOutputStream(uriString: String): Result<OutputStream> {
        val uri = Uri.parse(globalPreferences.automaticBackupLocation.firstBlocking())

        return try {
            val outputStream = contentResolver.openOutputStream(uri)!!

            Success(outputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> FileAccessDenied()
                else -> GenericFailure(e)
            }
        }
    }

    override fun showNotification(notification: AppNotification) {
        NotificationUtils.showNotification(this, notification)
    }

    override fun dismissNotification(notificationId: Int) {
        NotificationUtils.dismissNotification(this, notificationId)
    }

    override fun createChannel(vararg channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(this, *channelId)
        }
    }

    override fun deleteChannel(channelId: String) {
        NotificationUtils.deleteChannel(this, channelId)
    }

    override fun isAccessibilityServiceEnabled(): Boolean {
        return AccessibilityUtils.isServiceEnabled(this)
    }

    override fun haveWriteSecureSettingsPermission(): Boolean {
        return PermissionUtils.haveWriteSecureSettingsPermission(this)
    }
}
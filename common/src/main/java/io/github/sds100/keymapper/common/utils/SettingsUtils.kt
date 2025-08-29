package io.github.sds100.keymapper.common.utils

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.os.bundleOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

object SettingsUtils {

    /**
     * @return If the setting can't be found, it returns null
     */
    inline fun <reified T> getSystemSetting(ctx: Context, name: String): T? {
        val contentResolver = ctx.contentResolver

        return try {
            when (T::class) {
                Int::class -> Settings.System.getInt(contentResolver, name) as T?
                String::class -> Settings.System.getString(contentResolver, name) as T?
                Float::class -> Settings.System.getFloat(contentResolver, name) as T?
                Long::class -> Settings.System.getLong(contentResolver, name) as T?

                else -> {
                    throw Exception("Setting type ${T::class} is not supported")
                }
            }
        } catch (e: Settings.SettingNotFoundException) {
            null
        }
    }

    /**
     * @return If the setting can't be found, it returns null
     */
    inline fun <reified T> getSecureSetting(ctx: Context, name: String): T? {
        val contentResolver = ctx.contentResolver

        return try {
            when (T::class) {
                Int::class -> Settings.Secure.getInt(contentResolver, name) as T?
                String::class -> Settings.Secure.getString(contentResolver, name) as T?
                Float::class -> Settings.Secure.getFloat(contentResolver, name) as T?
                Long::class -> Settings.Secure.getLong(contentResolver, name) as T?

                else -> {
                    throw Exception("Setting type ${T::class} is not supported")
                }
            }
        } catch (e: Settings.SettingNotFoundException) {
            null
        }
    }

    /**
     * @return If the setting can't be found, it returns null
     */
    inline fun <reified T> getGlobalSetting(ctx: Context, name: String): T? {
        val contentResolver = ctx.contentResolver

        return try {
            when (T::class) {
                Int::class -> Settings.Global.getInt(contentResolver, name) as T?
                String::class -> Settings.Global.getString(contentResolver, name) as T?
                Float::class -> Settings.Global.getFloat(contentResolver, name) as T?
                Long::class -> Settings.Global.getLong(contentResolver, name) as T?

                else -> {
                    throw Exception("Setting type ${T::class} is not supported")
                }
            }
        } catch (e: Settings.SettingNotFoundException) {
            null
        }
    }

    /**
     * @return whether the setting was changed successfully
     */
    @RequiresPermission(Manifest.permission.WRITE_SETTINGS)
    inline fun <reified T> putSystemSetting(ctx: Context, name: String, value: T): Boolean {
        val contentResolver = ctx.contentResolver

        return when (T::class) {
            Int::class -> Settings.System.putInt(contentResolver, name, value as Int)
            String::class -> Settings.System.putString(contentResolver, name, value as String)
            Float::class -> Settings.System.putFloat(contentResolver, name, value as Float)
            Long::class -> Settings.System.putLong(contentResolver, name, value as Long)

            else -> {
                throw Exception("Setting type ${T::class} is not supported")
            }
        }
    }

    /**
     * @return whether the setting was changed successfully
     */
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    inline fun <reified T> putSecureSetting(ctx: Context, name: String, value: T): Boolean {
        val contentResolver = ctx.contentResolver

        return when (T::class) {
            Int::class -> Settings.Secure.putInt(contentResolver, name, value as Int)
            String::class -> Settings.Secure.putString(contentResolver, name, value as String)
            Float::class -> Settings.Secure.putFloat(contentResolver, name, value as Float)
            Long::class -> Settings.Secure.putLong(contentResolver, name, value as Long)

            else -> {
                throw Exception("Setting type ${T::class} is not supported")
            }
        }
    }

    /**
     * @return whether the setting was changed successfully
     */
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    inline fun <reified T> putGlobalSetting(ctx: Context, name: String, value: T): Boolean {
        val contentResolver = ctx.contentResolver

        return when (T::class) {
            Int::class -> Settings.Global.putInt(contentResolver, name, value as Int)
            String::class -> Settings.Global.putString(contentResolver, name, value as String)
            Float::class -> Settings.Global.putFloat(contentResolver, name, value as Float)
            Long::class -> Settings.Global.putLong(contentResolver, name, value as Long)

            else -> {
                throw Exception("Setting type ${T::class} is not supported")
            }
        }
    }

    fun launchSettingsScreen(ctx: Context, action: String, fragmentArg: String? = null) {
        val intent = Intent(action).apply {
            if (fragmentArg != null) {
                val fragmentArgKey = ":settings:fragment_args_key"
                val showFragmentArgsKey = ":settings:show_fragment_args"

                putExtra(fragmentArgKey, fragmentArg)

                val bundle = bundleOf(fragmentArgKey to fragmentArg)
                putExtra(showFragmentArgsKey, bundle)
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e("Failed to start Settings activity: $e")
        }
    }

    fun settingsCallbackFlow(ctx: Context, uri: Uri): Flow<Unit> = callbackFlow {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)

                    trySend(Unit)
                }
            }

        ctx.contentResolver.registerContentObserver(uri, false, observer)

        this.awaitClose { ctx.contentResolver.unregisterContentObserver(observer) }
    }
}
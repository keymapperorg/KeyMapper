package io.github.sds100.keymapper.system.settings

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSettingsAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsAdapter {
    private val ctx = context.applicationContext

    override fun getSystemSettingKeys(): List<String> {
        return getSettingKeys(Settings.System.CONTENT_URI)
    }

    override fun getSecureSettingKeys(): List<String> {
        return getSettingKeys(Settings.Secure.CONTENT_URI)
    }

    override fun getGlobalSettingKeys(): List<String> {
        return getSettingKeys(Settings.Global.CONTENT_URI)
    }

    override fun getSystemSettingValue(key: String): String? {
        return SettingsUtils.getSystemSetting<String>(ctx, key)
    }

    override fun getSecureSettingValue(key: String): String? {
        return SettingsUtils.getSecureSetting<String>(ctx, key)
    }

    override fun getGlobalSettingValue(key: String): String? {
        return SettingsUtils.getGlobalSetting<String>(ctx, key)
    }

    override fun modifySetting(settingType: SettingType, key: String, value: String): KMResult<*> {
        val success = when (settingType) {
            SettingType.SYSTEM -> when {
                value.toIntOrNull() != null -> SettingsUtils.putSystemSetting(ctx, key, value.toInt())
                value.toLongOrNull() != null -> SettingsUtils.putSystemSetting(ctx, key, value.toLong())
                value.toFloatOrNull() != null -> SettingsUtils.putSystemSetting(ctx, key, value.toFloat())
                else -> SettingsUtils.putSystemSetting(ctx, key, value)
            }
            SettingType.SECURE -> when {
                value.toIntOrNull() != null -> SettingsUtils.putSecureSetting(ctx, key, value.toInt())
                value.toLongOrNull() != null -> SettingsUtils.putSecureSetting(ctx, key, value.toLong())
                value.toFloatOrNull() != null -> SettingsUtils.putSecureSetting(ctx, key, value.toFloat())
                else -> SettingsUtils.putSecureSetting(ctx, key, value)
            }
            SettingType.GLOBAL -> when {
                value.toIntOrNull() != null -> SettingsUtils.putGlobalSetting(ctx, key, value.toInt())
                value.toLongOrNull() != null -> SettingsUtils.putGlobalSetting(ctx, key, value.toLong())
                value.toFloatOrNull() != null -> SettingsUtils.putGlobalSetting(ctx, key, value.toFloat())
                else -> SettingsUtils.putGlobalSetting(ctx, key, value)
            }
        }

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToModifySystemSetting(key)
        }
    }

    private fun getSettingKeys(uri: Uri): List<String> {
        val keys = mutableListOf<String>()
        var cursor: Cursor? = null
        try {
            cursor = ctx.contentResolver.query(
                uri,
                arrayOf("name"),
                null,
                null,
                null,
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex("name")
                if (nameIndex >= 0) {
                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            keys.add(name)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Some devices may not allow querying all settings
        }
        return keys.sorted()
    }
}

interface SettingsAdapter {
    fun getSystemSettingKeys(): List<String>
    fun getSecureSettingKeys(): List<String>
    fun getGlobalSettingKeys(): List<String>
    
    fun getSystemSettingValue(key: String): String?
    fun getSecureSettingValue(key: String): String?
    fun getGlobalSettingValue(key: String): String?
    
    fun modifySetting(settingType: SettingType, key: String, value: String): KMResult<*>
}

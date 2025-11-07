package io.github.sds100.keymapper.system.settings

import android.content.Context
import android.database.Cursor
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

    override fun getAll(settingType: SettingType): Map<String, String?> {
        val uri = when (settingType) {
            SettingType.SYSTEM -> Settings.System.CONTENT_URI
            SettingType.SECURE -> Settings.Secure.CONTENT_URI
            SettingType.GLOBAL -> Settings.Global.CONTENT_URI
        }

        val settings = mutableMapOf<String, String?>()
        var cursor: Cursor?
        try {
            cursor = ctx.contentResolver.query(
                uri,
                arrayOf("name", "value"),
                null,
                null,
                null,
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex("name")
                val valueIndex = it.getColumnIndex("value")
                if (nameIndex >= 0) {
                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            val value = if (valueIndex >= 0) {
                                it.getString(valueIndex)
                            } else {
                                null
                            }
                            settings[name] = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Some devices may not allow querying all settings
        }
        return settings.toSortedMap()
    }

    override fun getValue(settingType: SettingType, key: String): String? {
        return when (settingType) {
            SettingType.SYSTEM -> SettingsUtils.getSystemSetting<String>(ctx, key)
            SettingType.SECURE -> SettingsUtils.getSecureSetting<String>(ctx, key)
            SettingType.GLOBAL -> SettingsUtils.getGlobalSetting<String>(ctx, key)
        }
    }

    override fun modifySetting(settingType: SettingType, key: String, value: String): KMResult<*> {
        val success = when (settingType) {
            SettingType.SYSTEM -> SettingsUtils.putSystemSetting(ctx, key, value)
            SettingType.SECURE -> SettingsUtils.putSecureSetting(ctx, key, value)
            SettingType.GLOBAL -> SettingsUtils.putGlobalSetting(ctx, key, value)
        }

        return if (success) {
            Success(Unit)
        } else {
            KMError.FailedToModifySystemSetting(key)
        }
    }
}

interface SettingsAdapter {
    fun getAll(settingType: SettingType): Map<String, String?>
    fun getValue(settingType: SettingType, key: String): String?
    fun modifySetting(settingType: SettingType, key: String, value: String): KMResult<*>
}

package io.github.sds100.keymapper.system.settings

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ctx = context.applicationContext

    fun getSystemSettingKeys(): List<String> {
        return getSettingKeys(Settings.System.CONTENT_URI)
    }

    fun getSecureSettingKeys(): List<String> {
        return getSettingKeys(Settings.Secure.CONTENT_URI)
    }

    fun getGlobalSettingKeys(): List<String> {
        return getSettingKeys(Settings.Global.CONTENT_URI)
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

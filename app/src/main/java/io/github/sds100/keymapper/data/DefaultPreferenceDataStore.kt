package io.github.sds100.keymapper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.createDataStore
import io.github.sds100.keymapper.util.defaultSharedPreferences
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 20/02/2020.
 */

class DefaultPreferenceDataStore(ctx: Context) : IPreferenceDataStore {

    private val ctx = ctx.applicationContext

    private val prefs: SharedPreferences
        get() = ctx.defaultSharedPreferences

    override val fingerprintGestureDataStore = ctx.createDataStore("fingerprint_gestures")
    private val preferenceDataStore = ctx.createDataStore("preferences")

    override fun getBoolPref(key: Int): Boolean {
        return prefs.getBoolean(ctx.str(key), false)
    }

    override fun setBoolPref(key: Int, value: Boolean) {
        prefs.edit {
            putBoolean(ctx.str(key), value)
        }
    }

    override suspend fun <T> get(key: Preferences.Key<T>): Flow<T?> {
        return preferenceDataStore.data.map { it[key] }
    }

    override suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        preferenceDataStore.edit {
            it[key] = value
        }
    }

    override fun getStringPref(key: Int): String? {
        return prefs.getString(ctx.str(key), null)
    }

    override fun setStringPref(key: Int, value: String) {
        prefs.edit {
            putString(ctx.str(key), value)
        }
    }
}
package io.github.sds100.keymapper.data.db

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore
import io.github.sds100.keymapper.Constants

/**
 * Created by sds100 on 20/02/2020.
 */

class DefaultDataStoreManager(context: Context) : IDataStoreManager {

    companion object {
        private const val DEFAULT_SHARED_PREFS_NAME = "${Constants.PACKAGE_NAME}_preferences"
    }

    private val ctx = context.applicationContext

    private val sharedPreferencesMigration = SharedPreferencesMigration(
        ctx,
        DEFAULT_SHARED_PREFS_NAME
    )

    private val Context.fingerprintGestureDataStore by preferencesDataStore("fingerprint_gestures")
    override val fingerprintGestureDataStore
        get() = ctx.fingerprintGestureDataStore

    private val Context.globalPreferenceDataStore by preferencesDataStore(
        name = "preferences",
        migrations = listOf(sharedPreferencesMigration)
    )

    override val globalPreferenceDataStore
        get() = ctx.globalPreferenceDataStore
}
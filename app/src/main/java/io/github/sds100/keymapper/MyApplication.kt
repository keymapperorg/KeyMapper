package io.github.sds100.keymapper

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import io.github.sds100.keymapper.data.AppPreferences
import timber.log.Timber

/**
 * Created by sds100 on 19/05/2020.
 */
class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppPreferences.darkThemeMode)
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
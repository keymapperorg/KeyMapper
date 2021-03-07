package io.github.sds100.keymapper

import androidx.multidex.MultiDexApplication
import timber.log.Timber

/**
 * Created by sds100 on 19/05/2020.
 */
class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
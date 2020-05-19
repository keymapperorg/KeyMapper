package io.github.sds100.keymapper

import android.app.Application
import timber.log.Timber

/**
 * Created by sds100 on 19/05/2020.
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
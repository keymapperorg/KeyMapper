package io.github.sds100.keymapper

import android.app.Application
import androidx.fragment.app.FragmentFactory
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.ui.fragment.TriggerFragment
import timber.log.Timber

/**
 * Created by sds100 on 19/05/2020.
 */
class MyApplication : Application() {

    val keymapRepository: KeymapRepository
        get() = ServiceLocator.provideKeymapRepository(this)

    val deviceInfoRepository: DeviceInfoRepository
        get() = ServiceLocator.provideDeviceInfoRepository(this)

    val onboardingState: IOnboardingState
        get() = ServiceLocator.provideOnboardingState(this)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
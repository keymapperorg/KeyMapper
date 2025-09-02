package io.github.sds100.keymapper.system.accessibility

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.system.JobSchedulerHelper
import javax.inject.Inject

@AndroidEntryPoint
class ObserveEnabledAccessibilityServicesJob : JobService() {

    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapter

    override fun onStartJob(params: JobParameters?): Boolean {
        accessibilityServiceAdapter.invalidateState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeEnabledAccessibilityServices(application!!)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

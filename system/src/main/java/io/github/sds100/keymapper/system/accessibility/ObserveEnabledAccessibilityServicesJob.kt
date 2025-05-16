package io.github.sds100.keymapper.system.accessibility

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import io.github.sds100.keymapper.KeyMapperApp
import io.github.sds100.keymapper.system.JobSchedulerHelper


class ObserveEnabledAccessibilityServicesJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        (application!! as KeyMapperApp).accessibilityServiceAdapter.updateWhetherServiceIsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeEnabledAccessibilityServices(application!!)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

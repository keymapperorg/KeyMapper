package io.github.sds100.keymapper.system.notifications

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import io.github.sds100.keymapper.KeyMapperApp
import io.github.sds100.keymapper.system.JobSchedulerHelper

/**
 * Created by sds100 on 02/04/2021.
 */
class ObserveNotificationListenersJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        (applicationContext as KeyMapperApp).permissionAdapter.onPermissionsChanged()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeEnabledNotificationListeners(applicationContext)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

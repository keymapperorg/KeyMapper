package io.github.sds100.keymapper.system.inputmethod

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import io.github.sds100.keymapper.KeyMapperApp
import io.github.sds100.keymapper.system.JobSchedulerHelper


class ObserveInputMethodsJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        (applicationContext as KeyMapperApp).inputMethodAdapter.onInputMethodsUpdate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeInputMethods(applicationContext)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

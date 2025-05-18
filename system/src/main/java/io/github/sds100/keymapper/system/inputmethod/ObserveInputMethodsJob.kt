package io.github.sds100.keymapper.system.inputmethod

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.system.JobSchedulerHelper
import javax.inject.Inject

@AndroidEntryPoint
class ObserveInputMethodsJob : JobService() {

    @Inject
    lateinit var inputMethodAdapter: AndroidInputMethodAdapter

    override fun onStartJob(params: JobParameters?): Boolean {
        inputMethodAdapter.onInputMethodsUpdate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeInputMethods(applicationContext)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}

package io.github.sds100.keymapper.reportbug

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.github.appintro.AppIntro2
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Created by sds100 on 30/06/2021.
 */
@AndroidEntryPoint
class ReportBugActivity : AppIntro2() {

    private val viewModel by viewModels<ReportBugViewModel>()

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    private val chooseReportLocationLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.onChooseBugReportLocation(it.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.showPopups(this, findViewById(R.id.background))

        isSkipButtonEnabled = false

        requestPermissionDelegate =
            RequestPermissionDelegate(this, showDialogs = false, permissionAdapter, notificationReceiverAdapter)

        lifecycleScope.launchWhenCreated {
            viewModel.slides.collectLatest { slides ->
                slides.forEach { addSlide(it.id) }
            }
        }

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.chooseBugReportLocation.collectLatest {
                chooseReportLocationLauncher.launch(ReportBugUtils.createReportFileName())
            }
        }

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.goToNextSlide.collectLatest {
                goToNextSlide()
            }
        }
    }

    override fun onIntroFinished() {
        super.onIntroFinished()

        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        finish()
    }

    private fun addSlide(slide: String) {
        val args = bundleOf(ReportBugSlideFragment.KEY_SLIDE to slide)

        ReportBugSlideFragment().apply {
            arguments = args
            addSlide(this)
        }
    }
}
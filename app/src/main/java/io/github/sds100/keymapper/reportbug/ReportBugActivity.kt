package io.github.sds100.keymapper.reportbug

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.FeedbackUtils
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 30/06/2021.
 */
class ReportBugActivity : AppIntro2() {

    private val viewModel by viewModels<ReportBugViewModel> {
        Inject.reportBugViewModel(this)
    }

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    private val chooseReportLocationLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.onChooseBugReportLocation(it.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = false)

        addRepeatingJob(Lifecycle.State.CREATED) {
            viewModel.openUrl.collectLatest {
                UrlUtils.openUrl(this@ReportBugActivity, it)
            }
        }

        viewModel.slides.forEach {
            addSlide(it)
        }

        addRepeatingJob(Lifecycle.State.CREATED) {
            viewModel.chooseBugReportLocation.collectLatest {
                chooseReportLocationLauncher.launch(ReportBugUtils.createReportFileName())
            }
        }

        addRepeatingJob(Lifecycle.State.CREATED) {
            viewModel.goToNextSlide.collectLatest {
                goToNextSlide()
            }
        }

        addRepeatingJob(Lifecycle.State.CREATED) {
            viewModel.emailDeveloper.collectLatest {
                FeedbackUtils.emailBugReport(this@ReportBugActivity, it)
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
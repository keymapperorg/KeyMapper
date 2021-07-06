package io.github.sds100.keymapper.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 30/06/2021.
 */
class FixAppKillingActivity : AppIntro2() {

    private val viewModel by viewModels<FixAppKillingViewModel> {
        Inject.fixCrashViewModel(this)
    }

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = false)

        addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.openUrl.collectLatest {
                UrlUtils.openUrl(this@FixAppKillingActivity, it)
            }
        }

        addRepeatingJob(Lifecycle.State.CREATED) {
            viewModel.goToNextSlide.collectLatest {
                goToNextSlide()
            }
        }

        viewModel.allSlides.forEach {
            addSlide(it.id)
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
        val args = bundleOf(FixAppKillingSlideFragment.KEY_SLIDE to slide)

        FixAppKillingSlideFragment().apply {
            arguments = args
            addSlide(this)
        }
    }
}
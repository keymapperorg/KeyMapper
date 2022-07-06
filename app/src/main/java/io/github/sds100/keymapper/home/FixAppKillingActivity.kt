package io.github.sds100.keymapper.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.github.appintro.AppIntro2
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
class FixAppKillingActivity : AppIntro2() {

    private val viewModel by viewModels<FixAppKillingViewModel>()

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.showPopups(this, findViewById(R.id.background))

        isSkipButtonEnabled = false

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = false, permissionAdapter, notificationReceiverAdapter)

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
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
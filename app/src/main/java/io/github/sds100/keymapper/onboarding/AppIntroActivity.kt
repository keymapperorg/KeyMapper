package io.github.sds100.keymapper.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.github.appintro.AppIntro2
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.MainActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Created by sds100 on 07/07/2019.
 */

@AndroidEntryPoint
class AppIntroActivity : AppIntro2() {

    companion object {
        const val EXTRA_SLIDES = "extra_slides"
    }

    private val viewModel by viewModels<AppIntroViewModel>()

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.showPopups(this, findViewById(R.id.background))

        isSkipButtonEnabled = false

        requestPermissionDelegate =
            RequestPermissionDelegate(this, showDialogs = false, permissionAdapter, notificationReceiverAdapter)

        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionAdapter.request.collectLatest { permission ->
                requestPermissionDelegate.requestPermission(
                    permission,
                    null
                )
            }
        }

        viewModel.slidesToShow.forEach {
            val args = bundleOf(AppIntroFragment.KEY_SLIDE to it)

            AppIntroFragment().apply {
                arguments = args
                addSlide(this)
            }
        }
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        viewModel.onDoneClick()

        startActivity(Intent(this, MainActivity::class.java))

        finish()
    }
}
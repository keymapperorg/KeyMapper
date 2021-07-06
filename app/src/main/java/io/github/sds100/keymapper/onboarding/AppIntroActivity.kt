package io.github.sds100.keymapper.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.MainActivity
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 07/07/2019.
 */

class AppIntroActivity : AppIntro2() {

    companion object {
        const val EXTRA_SLIDES = "extra_slides"
    }

    private val viewModel by viewModels<AppIntroViewModel> {
        val slides = intent.getStringArrayExtra(EXTRA_SLIDES)

        Inject.appIntroViewModel(this, slides!!.toList())
    }

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = false)

        addRepeatingJob(Lifecycle.State.RESUMED) {
            ServiceLocator.permissionAdapter(this@AppIntroActivity).request.collectLatest { permission ->
                requestPermissionDelegate.requestPermission(
                    permission,
                    null
                )
            }
        }

        addRepeatingJob(Lifecycle.State.RESUMED){
            viewModel.openUrl.collectLatest {
                UrlUtils.openUrl(this@AppIntroActivity, it)
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
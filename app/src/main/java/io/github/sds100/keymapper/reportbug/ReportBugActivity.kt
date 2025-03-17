package io.github.sds100.keymapper.reportbug

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
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
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) {
            it ?: return@registerForActivityResult

            viewModel.onChooseBugReportLocation(it.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Must come after onCreate
        showStatusBar(true)

        val rootView: View = findViewById(R.id.background)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val insets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            // Only show behind status bar and not behind the navigation bar.
            v.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        viewModel.showPopups(this, rootView)

        isSkipButtonEnabled = false

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = false)

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

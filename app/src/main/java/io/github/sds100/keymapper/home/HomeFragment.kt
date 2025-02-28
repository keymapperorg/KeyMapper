package io.github.sds100.keymapper.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import io.github.sds100.keymapper.databinding.FragmentHomeBinding
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.ui.setupNavigation
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    private val backupMappingsLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) {
            it ?: return@registerForActivityResult

            homeViewModel.onChoseBackupFile(it.toString())
        }

    private val backupFingerprintMapsLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) {
            it ?: return@registerForActivityResult

            homeViewModel.backupFingerprintMaps(it.toString())
        }

    private val backupKeyMapsLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) {
            it ?: return@registerForActivityResult

            homeViewModel.backupSelectedKeyMaps(it.toString())
        }

    private val restoreMappingsLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            homeViewModel.onChoseRestoreFile(it.toString())
        }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
//            if (position == 0) {
//                binding.fab.show()
//            } else {
//                binding.fab.hide()
//            }
        }
    }

    private var quickStartGuideTapTarget: MaterialTapTargetPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        homeViewModel.setupNavigation(this)
        homeViewModel.keymapListViewModel.setupNavigation(this)
        homeViewModel.fingerprintMapListViewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentHomeBinding.inflate(inflater, container, false).apply {
            _binding = this

            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
//                    KeyMapperTheme {
                    HomeScreen(viewModel = homeViewModel)
//                    }
                }
            }
            return this.root
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

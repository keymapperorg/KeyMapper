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
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    private val backupMappingsLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ZIP)) {
            it ?: return@registerForActivityResult

            homeViewModel.onChoseBackupFile(it.toString())
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

    // TODO: quick start guide tap target. Delete tap target library?
    private var quickStartGuideTapTarget: MaterialTapTargetPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        homeViewModel.setupNavigation(this)
        homeViewModel.keymapListViewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentComposeBinding.inflate(inflater, container, false).apply {
            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    KeyMapperTheme {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onMenuClick = {
                                findNavController().navigate(NavAppDirections.toSettingsFragment())
                            },
                        )
                    }
                }
            }
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.showPopups(this, view)
        homeViewModel.keymapListViewModel.showPopups(this, view)
        homeViewModel.listFloatingLayoutsViewModel.showPopups(this, view)
    }
}

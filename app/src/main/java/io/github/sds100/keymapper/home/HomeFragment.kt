package io.github.sds100.keymapper.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomappbar.BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
import com.google.android.material.bottomappbar.BottomAppBar.FAB_ALIGNMENT_MODE_END
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.data.db.SeedDatabaseWorker
import io.github.sds100.keymapper.databinding.FragmentHomeBinding
import io.github.sds100.keymapper.fixError
import io.github.sds100.keymapper.success
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.QuickStartGuideTapTarget
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.strArray
import io.github.sds100.keymapper.util.ui.TextListItem
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
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
            if (position == 0) {
                binding.fab.show()
            } else {
                binding.fab.hide()
            }
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
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.showPopups(this, binding)
        homeViewModel.menuViewModel.showPopups(this, binding)
        homeViewModel.keymapListViewModel.showPopups(this, binding)
        homeViewModel.fingerprintMapListViewModel.showPopups(this, binding)

        binding.viewModel = this@HomeFragment.homeViewModel

        val pagerAdapter = HomePagerAdapter(this@HomeFragment)
        // set the initial tabs so that the current tab is remembered on rotate
        pagerAdapter.invalidateFragments(homeViewModel.tabsState.value.tabs)

        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = strArray(R.array.home_tab_titles)[position]
        }.apply {
            attach()
        }

        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_help -> {
                    UrlUtils.openUrl(
                        requireContext(),
                        str(R.string.url_quick_start_guide),
                    )
                    true
                }

                R.id.action_seed_database -> {
                    val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
                    WorkManager.getInstance(requireContext()).enqueue(request)
                    true
                }

                R.id.action_select_all -> {
                    homeViewModel.onSelectAllClick()
                    true
                }

                R.id.action_enable -> {
                    homeViewModel.onEnableSelectedKeymapsClick()
                    true
                }

                R.id.action_disable -> {
                    homeViewModel.onDisableSelectedKeymapsClick()
                    true
                }

                R.id.action_duplicate_keymap -> {
                    homeViewModel.onDuplicateSelectedKeymapsClick()
                    true
                }

                R.id.action_backup -> {
                    backupKeyMapsLauncher.launch(BackupUtils.createKeyMapsFileName())
                    true
                }

                else -> false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            homeViewModel.onBackPressed()
        }

        binding.appBar.setNavigationOnClickListener {
            homeViewModel.onAppBarNavigationButtonClick()
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.appBarState.collectLatest {
                /*
                Do not use setFabAlignmentModeAndReplaceMenu because then there is big jank.
                 */
                if (it == HomeAppBarState.MULTI_SELECTING) {
                    binding.appBar.fabAlignmentMode = FAB_ALIGNMENT_MODE_END
                    binding.appBar.replaceMenu(R.menu.menu_multi_select)
                } else {
                    binding.appBar.fabAlignmentMode = FAB_ALIGNMENT_MODE_CENTER
                    binding.appBar.replaceMenu(R.menu.menu_home)
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.showQuickStartGuideHint.collectLatest { show ->
                if (show && quickStartGuideTapTarget?.state != MaterialTapTargetPrompt.STATE_REVEALED) {
                    quickStartGuideTapTarget?.dismiss()

                    quickStartGuideTapTarget =
                        QuickStartGuideTapTarget().show(this@HomeFragment, R.id.action_help) {
                            homeViewModel.approvedQuickStartGuideTapTarget()
                        }
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.tabsState.collectLatest { state ->
                pagerAdapter.invalidateFragments(state.tabs)
                binding.viewPager.isUserInputEnabled = state.enableViewPagerSwiping
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.showMenu.collectLatest {
                findNavController().navigate(R.id.action_global_menuFragment)
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.closeKeyMapper.collectLatest {
                requireActivity().finish()
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.menuViewModel.chooseBackupFile.collectLatest {
                try {
                    backupMappingsLauncher.launch(BackupUtils.createMappingsFileName())
                } catch (e: ActivityNotFoundException) {
                    homeViewModel.menuViewModel.onCreateBackupFileActivityNotFound()
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.menuViewModel.chooseRestoreFile.collectLatest {
                try {
                    restoreMappingsLauncher.launch(FileUtils.MIME_TYPE_ALL)
                } catch (e: ActivityNotFoundException) {
                    homeViewModel.menuViewModel.onChooseRestoreFileActivityNotFound()
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.shareBackup.collectLatest { uri ->
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))

                    type = FileUtils.MIME_TYPE_ZIP

                    startActivity(Intent.createChooser(this, getText(R.string.sharesheet_title)))
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.errorListState.collectLatest { state ->
                binding.cardViewRecyclerViewErrors.isVisible = state.isVisible

                binding.recyclerViewError.withModels {
                    state.listItems.forEach { listItem ->
                        if (listItem is TextListItem.Error) {
                            fixError {
                                id(listItem.id)

                                model(listItem)

                                onFixClick { _ ->
                                    homeViewModel.onFixErrorListItemClick(listItem.id)
                                }
                            }
                        }

                        if (listItem is TextListItem.Success) {
                            success {
                                id(listItem.id)

                                model(listItem)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            homeViewModel.fingerprintMapListViewModel.requestFingerprintMapsBackup.collectLatest {
                backupFingerprintMapsLauncher.launch(BackupUtils.createFingerprintMapsFileName())
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_sort -> {
                        findNavController().navigate(R.id.action_global_sortingFragment)
                        true
                    }

                    else -> false
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        _binding = null
        super.onDestroyView()
    }
}

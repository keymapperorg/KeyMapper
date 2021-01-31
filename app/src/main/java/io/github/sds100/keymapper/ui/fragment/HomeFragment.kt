package io.github.sds100.keymapper.ui.fragment

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.model.ChooseAppStoreModel
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.showGuiKeyboardAd
import io.github.sds100.keymapper.data.viewmodel.*
import io.github.sds100.keymapper.databinding.DialogChooseAppStoreBinding
import io.github.sds100.keymapper.databinding.FragmentHomeBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.adapter.HomePagerAdapter
import io.github.sds100.keymapper.ui.view.StatusLayout
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.NoCompatibleImeEnabled
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.worker.SeedDatabaseWorker
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.systemservices.powerManager
import splitties.toast.longToast
import splitties.toast.toast
import java.util.*

class HomeFragment : Fragment() {

    private val keymapListViewModel: KeymapListViewModel by activityViewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val fingerprintMapListViewModel: FingerprintMapListViewModel by activityViewModels {
        InjectorUtils.provideFingerprintMapListViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    private val expandedHeader = MutableLiveData(false)
    private val collapsedStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val accessibilityServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val imeServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val dndAccessStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val writeSettingsStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val batteryOptimisationState = MutableLiveData(StatusLayout.State.ERROR)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                /*when the input method changes, update the action descriptions in case any need to show an error
                * that they need the input method to be enabled. */
                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    keymapListViewModel.rebuildModels()
                    fingerprintMapListViewModel.rebuildModels()
                }

                MyAccessibilityService.ACTION_ON_START -> {
                    accessibilityServiceStatusState.value = StatusLayout.State.POSITIVE
                }

                MyAccessibilityService.ACTION_ON_STOP -> {
                    accessibilityServiceStatusState.value = StatusLayout.State.ERROR
                }
            }
        }
    }

    private val backupAllKeymapsLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            backupRestoreViewModel
                .backupAll(requireContext().contentResolver.openOutputStream(it))
        }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            backupRestoreViewModel.restore(requireContext().contentResolver.openInputStream(it))
        }

    private val backupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val requestAccessNotificationPolicy =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateStatusLayouts()
        }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position == 0) {
                fab.show()
            } else {
                fab.hide()
            }
        }
    }

    private lateinit var recoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            requireContext().registerReceiver(broadcastReceiver, this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        recoverFailureDelegate = RecoverFailureDelegate(
            "HomeFragment",
            requireActivity().activityResultRegistry,
            viewLifecycleOwner) {

            keymapListViewModel.rebuildModels()
        }

        FragmentHomeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@HomeFragment
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            val pagerAdapter = HomePagerAdapter(
                this@HomeFragment,
                fingerprintMapListViewModel.fingerprintGesturesAvailable.value ?: false
            )

            viewPager.adapter = pagerAdapter

            fingerprintMapListViewModel.fingerprintGesturesAvailable.observe(viewLifecycleOwner, {
                pagerAdapter.invalidateFragments(it ?: false)
                isFingerprintGestureDetectionAvailable = it ?: false
            })

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.home_tab_titles)[position]
            }.apply {
                attach()
            }

            viewPager.registerOnPageChangeCallback(onPageChangeCallback)

            setOnNewKeymapClick {
                val direction =
                    HomeFragmentDirections.actionToConfigKeymap(ConfigKeymapViewModel.NEW_KEYMAP_ID)
                findNavController().navigate(direction)
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_help -> {
                        UrlUtils.launchCustomTab(
                            requireContext(),
                            str(R.string.url_quick_start_guide)
                        )
                        true
                    }

                    R.id.action_seed_database -> {
                        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
                        WorkManager.getInstance(requireContext()).enqueue(request)
                        true
                    }

                    R.id.action_select_all -> {
                        keymapListViewModel.selectionProvider.selectAll()
                        true
                    }

                    R.id.action_enable -> {
                        keymapListViewModel
                            .enableSelectedKeymaps()
                        true
                    }

                    R.id.action_disable -> {
                        keymapListViewModel
                            .disableSelectedKeymaps()
                        true
                    }

                    R.id.action_duplicate_keymap -> {
                        keymapListViewModel
                            .duplicate(*keymapListViewModel.selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_backup -> {
                        keymapListViewModel.requestBackupSelectedKeymaps()
                        true
                    }

                    else -> false
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (keymapListViewModel.selectionProvider.isSelectable.value == true) {
                    keymapListViewModel.selectionProvider.stopSelecting()
                } else {
                    requireActivity().finish()
                }
            }

            appBar.setNavigationOnClickListener {
                if (keymapListViewModel.selectionProvider.isSelectable.value == true) {
                    keymapListViewModel.selectionProvider.stopSelecting()
                } else {
                    findNavController().navigate(R.id.action_global_menuFragment)
                }
            }

            keymapListViewModel.selectionProvider.isSelectable
                .observe(viewLifecycleOwner, { isSelectable ->
                    viewPager.isUserInputEnabled = !isSelectable

                    if (isSelectable) {
                        appBar.replaceMenu(R.menu.menu_multi_select)
                    } else {
                        appBar.replaceMenu(R.menu.menu_home)
                    }
                })

            isSelectable = keymapListViewModel.selectionProvider.isSelectable
            selectionCount = keymapListViewModel.selectionProvider.selectedCount

            setOnConfirmSelectionClick {
                keymapListViewModel.delete(*keymapListViewModel.selectionProvider.selectedIds)
                keymapListViewModel.selectionProvider.stopSelecting()
            }

            backupRestoreViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is MessageEvent -> toast(it.textRes)
                    is ShowErrorMessage -> toast(it.failure.getFullMessage(requireContext()))
                    is RequestRestore -> restoreLauncher.launch(FileUtils.MIME_TYPE_ALL)
                    is RequestBackupAll ->
                        backupAllKeymapsLauncher.launch(BackupUtils.createFileName())
                }
            })

            expanded = expandedHeader
            collapsedStatusLayoutState = this@HomeFragment.collapsedStatusState
            accessibilityServiceStatusState = this@HomeFragment.accessibilityServiceStatusState
            imeServiceStatusState = this@HomeFragment.imeServiceStatusState
            dndAccessStatusState = this@HomeFragment.dndAccessStatusState
            writeSettingsStatusState = this@HomeFragment.writeSettingsStatusState
            batteryOptimisationState = this@HomeFragment.batteryOptimisationState

            buttonCollapse.setOnClickListener {
                expandedHeader.value = false
            }

            layoutCollapsed.setOnClickListener {
                expandedHeader.value = true
            }

            setEnableAccessibilityService {
                AccessibilityUtils.enableService(requireContext())
            }

            setEnableImeService {
                lifecycleScope.launchWhenStarted {

                    KeyboardUtils.enableCompatibleInputMethods(requireContext())

                    viewLifecycleScope.launch {
                        delay(3000)

                        updateStatusLayouts()
                    }
                }
            }

            setGrantWriteSecureSettingsPermission {
                PermissionUtils.requestWriteSecureSettingsPermission(
                    requireContext(),
                    findNavController())
            }

            setGrantDndAccess {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PermissionUtils.requestAccessNotificationPolicy(requestAccessNotificationPolicy)
                }
            }

            setDisableBatteryOptimisation {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        longToast(R.string.error_battery_optimisation_activity_not_found)
                    }
                }
            }

            expandedHeader.observe(viewLifecycleOwner, {
                if (it == true) {
                    expandableLayout.expand()
                } else {
                    expandableLayout.collapse()

                    val transition = Fade()
                    TransitionManager.beginDelayedTransition(layoutCollapsed, transition)
                }
            })

            updateStatusLayouts()

            val appUpdateManager = ServiceLocator.appUpdateManager(requireContext())

            viewLifecycleScope.launchWhenResumed {
                val oldVersion = appUpdateManager.getLastVersionCodeHomeScreen()

                if (oldVersion == Constants.VERSION_CODE) return@launchWhenResumed

                val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                    R.string.whats_new,
                    R.string.url_changelog
                )
                findNavController().navigate(direction)

                appUpdateManager.handledAppUpdateOnHomeScreen()
            }

            setGetNewGuiKeyboard {
                requireContext().alertDialog {
                    messageResource = R.string.dialog_message_select_app_store_gui_keyboard

                    DialogChooseAppStoreBinding.inflate(layoutInflater).apply {
                        model = ChooseAppStoreModel(
                            playStoreLink = str(R.string.url_play_store_keymapper_gui_keyboard),
                            githubLink = str(R.string.url_github_keymapper_gui_keyboard),
                            fdroidLink = str(R.string.url_fdroid_keymapper_gui_keyboard)
                        )

                        setView(this.root)
                    }

                    cancelButton()

                    show()
                }
            }


            setDismissNewGuiKeyboardAd {
                globalPreferences.set(Keys.showGuiKeyboardAd, false)
            }

            showNewGuiKeyboardAd = globalPreferences.showGuiKeyboardAd.firstBlocking()

            globalPreferences.showGuiKeyboardAd.collectWhenResumed(viewLifecycleOwner, {
                binding.showNewGuiKeyboardAd = it
            })

            keymapListViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is FixFailure -> coordinatorLayout.showFixActionSnackBar(
                        it.failure,
                        requireContext(),
                        recoverFailureDelegate,
                        findNavController())
                }
            })

            fingerprintMapListViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is FixFailure -> coordinatorLayout.showFixActionSnackBar(
                        it.failure,
                        requireContext(),
                        recoverFailureDelegate,
                        findNavController())
                }
            })

            viewLifecycleScope.launchWhenResumed {
                QuickStartGuideTapTarget().show(
                    this@HomeFragment,
                    R.id.action_help)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        keymapListViewModel.rebuildModels()
        fingerprintMapListViewModel.rebuildModels()

        updateStatusLayouts()

        if (PackageUtils.isAppInstalled(requireContext(), KeyboardUtils.KEY_MAPPER_GUI_IME_PACKAGE)
            || Build.VERSION.SDK_INT < KeyboardUtils.KEY_MAPPER_GUI_IME_MIN_API) {
            globalPreferences.set(Keys.showGuiKeyboardAd, false)
        }

        ServiceLocator.notificationController(requireContext())
            .onEvent(DismissFingerprintFeatureNotification)
    }

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        requireContext().unregisterReceiver(broadcastReceiver)

        super.onDestroy()
    }

    private fun updateStatusLayouts() {
        binding.hideAlerts = globalPreferences
            .getFlow(Keys.hideHomeScreenAlerts).firstBlocking()

        if (AccessibilityUtils.isServiceEnabled(requireContext())) {
            accessibilityServiceStatusState.value = StatusLayout.State.POSITIVE

        } else {
            accessibilityServiceStatusState.value = StatusLayout.State.ERROR
        }

        if (requireContext().haveWriteSecureSettingsPermission) {
            writeSettingsStatusState.value = StatusLayout.State.POSITIVE
        } else {
            writeSettingsStatusState.value = StatusLayout.State.WARN
        }

        if (KeyboardUtils.isCompatibleImeEnabled()) {
            imeServiceStatusState.value = StatusLayout.State.POSITIVE

        } else if (keymapListViewModel.model.value is Data) {

            if ((keymapListViewModel.model.value as Data<List<KeymapListItemModel>>)
                    .data.any { keymap ->
                        keymap.actionList.any { it.error is NoCompatibleImeEnabled }
                    }) {

                imeServiceStatusState.value = StatusLayout.State.ERROR
            }

        } else {
            imeServiceStatusState.value = StatusLayout.State.WARN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.isPermissionGranted(
                    requireContext(),
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {

                dndAccessStatusState.value = StatusLayout.State.POSITIVE
            } else {
                dndAccessStatusState.value = StatusLayout.State.WARN
            }

            if (powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {
                batteryOptimisationState.value = StatusLayout.State.POSITIVE
            } else {
                batteryOptimisationState.value = StatusLayout.State.WARN
            }
        }

        val states = listOf(
            accessibilityServiceStatusState,
            writeSettingsStatusState,
            imeServiceStatusState,
            dndAccessStatusState,
            batteryOptimisationState
        )

        when {
            states.all { it.value == StatusLayout.State.POSITIVE } -> {
                expandedHeader.value = false
                collapsedStatusState.value = StatusLayout.State.POSITIVE
            }

            states.any { it.value == StatusLayout.State.ERROR } -> {
                expandedHeader.value = true
                collapsedStatusState.value = StatusLayout.State.ERROR
            }

            states.any { it.value == StatusLayout.State.WARN } -> {
                expandedHeader.value = false
                collapsedStatusState.value = StatusLayout.State.WARN
            }
        }
    }
}
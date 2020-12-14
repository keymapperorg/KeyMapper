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
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.ChooseAppStoreModel
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintMapListViewModel
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
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

/**
 * A placeholder fragment containing a simple view.
 */
class HomeFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mKeyMapListViewModel: KeymapListViewModel by activityViewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val mFingerprintMapListViewModel: FingerprintMapListViewModel by activityViewModels {
        InjectorUtils.provideFingerprintMapListViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentHomeBinding? = null
    private val mBinding: FragmentHomeBinding
        get() = _binding!!

    private val mExpanded = MutableLiveData(false)
    private val mCollapsedStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mAccessibilityServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mImeServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mDndAccessStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mWriteSettingsStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mBatteryOptimisationState = MutableLiveData(StatusLayout.State.ERROR)

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                /*when the input method changes, update the action descriptions in case any need to show an error
                * that they need the input method to be enabled. */
                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    mKeyMapListViewModel.rebuildModels()
                    mFingerprintMapListViewModel.rebuildModels()
                }

                MyAccessibilityService.ACTION_ON_START -> {
                    mAccessibilityServiceStatusState.value = StatusLayout.State.POSITIVE
                }

                MyAccessibilityService.ACTION_ON_STOP -> {
                    mAccessibilityServiceStatusState.value = StatusLayout.State.ERROR
                }
            }
        }
    }

    private val mBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel
                .backupAll(requireActivity().contentResolver.openOutputStream(it))
        }

    private val mRestoreLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel.restore(requireContext().contentResolver.openInputStream(it))
        }

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val mRequestAccessNotificationPolicy =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateStatusLayouts()
        }

    private lateinit var mPagerAdapter: HomePagerAdapter
    private lateinit var mTabLayoutMediator: TabLayoutMediator

    private val mOnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position == 0) {
                fab.show()
            } else {
                fab.hide()
            }
        }
    }

    private lateinit var mRecoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            requireActivity().registerReceiver(mBroadcastReceiver, this)
        }

        mRecoverFailureDelegate = RecoverFailureDelegate(
            "HomeFragment",
            requireActivity().activityResultRegistry,
            this) {

            mKeyMapListViewModel.rebuildModels()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentHomeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@HomeFragment
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            mPagerAdapter = HomePagerAdapter(this@HomeFragment)
            viewPager.adapter = mPagerAdapter

            mTabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.home_tab_titles)[position]
            }.apply {
                attach()
            }

            viewPager.registerOnPageChangeCallback(mOnPageChangeCallback)

            setOnNewKeymapClick {
                val direction =
                    HomeFragmentDirections.actionToConfigKeymap(ConfigKeymapViewModel.NEW_KEYMAP_ID)
                findNavController().navigate(direction)
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_help -> {
                        val direction = HomeFragmentDirections.actionGlobalHelpFragment()
                        findNavController().navigate(direction)

                        true
                    }

                    R.id.action_seed_database -> {
                        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
                        WorkManager.getInstance(requireContext()).enqueue(request)
                        true
                    }

                    R.id.action_select_all -> {
                        mKeyMapListViewModel.selectionProvider.selectAll()
                        true
                    }

                    R.id.action_enable -> {
                        mKeyMapListViewModel
                            .enableSelectedKeymaps()
                        true
                    }

                    R.id.action_disable -> {
                        mKeyMapListViewModel
                            .disableSelectedKeymaps()
                        true
                    }

                    R.id.action_duplicate_keymap -> {
                        mKeyMapListViewModel
                            .duplicate(*mKeyMapListViewModel.selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_backup -> {
                        mKeyMapListViewModel.requestBackupSelectedKeymaps()
                        true
                    }

                    else -> false
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (mKeyMapListViewModel.selectionProvider.isSelectable.value == true) {
                    mKeyMapListViewModel.selectionProvider.stopSelecting()
                } else {
                    requireActivity().finish()
                }
            }

            appBar.setNavigationOnClickListener {
                if (mKeyMapListViewModel.selectionProvider.isSelectable.value == true) {
                    mKeyMapListViewModel.selectionProvider.stopSelecting()
                } else {
                    findNavController().navigate(R.id.action_global_menuFragment)
                }
            }

            mKeyMapListViewModel.selectionProvider.isSelectable
                .observe(viewLifecycleOwner, { isSelectable ->
                    viewPager.isUserInputEnabled = !isSelectable

                    if (isSelectable) {
                        appBar.replaceMenu(R.menu.menu_multi_select)
                    } else {
                        appBar.replaceMenu(R.menu.menu_home)
                    }
                })

            isSelectable = mKeyMapListViewModel.selectionProvider.isSelectable
            selectionCount = mKeyMapListViewModel.selectionProvider.selectedCount

            setOnConfirmSelectionClick {
                mKeyMapListViewModel.delete(*mKeyMapListViewModel.selectionProvider.selectedIds)
                mKeyMapListViewModel.selectionProvider.stopSelecting()
            }

            mBackupRestoreViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is MessageEvent -> toast(it.textRes)
                    is ShowErrorMessage -> toast(it.failure.getFullMessage(requireContext()))
                    is RequestRestore -> mRestoreLauncher.launch(FileUtils.MIME_TYPE_ALL)
                    is RequestBackupAll -> mBackupLauncher.launch(BackupUtils.createFileName())
                }
            })

            expanded = mExpanded
            collapsedStatusLayoutState = mCollapsedStatusState
            accessibilityServiceStatusState = mAccessibilityServiceStatusState
            imeServiceStatusState = mImeServiceStatusState
            dndAccessStatusState = mDndAccessStatusState
            writeSettingsStatusState = mWriteSettingsStatusState
            batteryOptimisationState = mBatteryOptimisationState

            buttonCollapse.setOnClickListener {
                mExpanded.value = false
            }

            layoutCollapsed.setOnClickListener {
                mExpanded.value = true
            }

            setEnableAccessibilityService {
                AccessibilityUtils.enableService(requireActivity())
            }

            setEnableImeService {
                lifecycleScope.launchWhenStarted {

                    KeyboardUtils.enableCompatibleInputMethods()

                    lifecycleScope.launch {
                        delay(3000)

                        updateStatusLayouts()
                    }
                }
            }

            setGrantWriteSecureSettingsPermission {
                PermissionUtils.requestWriteSecureSettingsPermission(requireActivity())
            }

            setGrantDndAccess {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PermissionUtils.requestAccessNotificationPolicy(mRequestAccessNotificationPolicy)
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

            mExpanded.observe(viewLifecycleOwner, {
                if (it == true) {
                    expandableLayout.expand()
                } else {
                    expandableLayout.collapse()

                    val transition = Fade()
                    TransitionManager.beginDelayedTransition(layoutCollapsed, transition)
                }
            })

            isFingerprintGestureDetectionAvailable =
                AppPreferences.isFingerprintGestureDetectionAvailable

            updateStatusLayouts()

            if (AppPreferences.lastInstalledVersionCode != Constants.VERSION_CODE) {
                val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                    R.string.whats_new,
                    R.string.url_changelog
                )
                findNavController().navigate(direction)

                AppPreferences.lastInstalledVersionCode = Constants.VERSION_CODE
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
                AppPreferences.showGuiKeyboardAd = false
            }

            showNewGuiKeyboardAd = AppPreferences.showGuiKeyboardAd

            mKeyMapListViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is FixFailure -> coordinatorLayout.showFixActionSnackBar(
                        it.failure,
                        requireActivity(),
                        mRecoverFailureDelegate)
                }
            })

            mFingerprintMapListViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is FixFailure -> coordinatorLayout.showFixActionSnackBar(
                        it.failure,
                        requireActivity(),
                        mRecoverFailureDelegate)
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()

        mKeyMapListViewModel.rebuildModels()
        mFingerprintMapListViewModel.rebuildModels()

        updateStatusLayouts()
        requireContext().defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        if (PackageUtils.isAppInstalled(KeyboardUtils.KEY_MAPPER_GUI_IME_PACKAGE)
            || Build.VERSION.SDK_INT < KeyboardUtils.KEY_MAPPER_GUI_IME_MIN_API) {
            AppPreferences.showGuiKeyboardAd = false
        }
    }

    override fun onDestroyView() {
        mBinding.viewPager.unregisterOnPageChangeCallback(mOnPageChangeCallback)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().unregisterReceiver(mBroadcastReceiver)
        requireContext().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        when (key) {
            str(R.string.key_pref_show_gui_keyboard_ad) ->
                mBinding.showNewGuiKeyboardAd = AppPreferences.showGuiKeyboardAd

            str(R.string.key_pref_fingerprint_gesture_available) -> {
                mBinding.isFingerprintGestureDetectionAvailable =
                    AppPreferences.isFingerprintGestureDetectionAvailable

                mPagerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateStatusLayouts() {
        mBinding.hideAlerts = AppPreferences.hideHomeScreenAlerts

        if (AccessibilityUtils.isServiceEnabled(requireActivity())) {
            mAccessibilityServiceStatusState.value = StatusLayout.State.POSITIVE

        } else {
            mAccessibilityServiceStatusState.value = StatusLayout.State.ERROR
        }

        if (PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            mWriteSettingsStatusState.value = StatusLayout.State.POSITIVE
        } else {
            mWriteSettingsStatusState.value = StatusLayout.State.WARN
        }

        if (KeyboardUtils.isCompatibleImeEnabled()) {
            mImeServiceStatusState.value = StatusLayout.State.POSITIVE

        } else if (mKeyMapListViewModel.keymapModelList.value is Data) {

            if ((mKeyMapListViewModel.keymapModelList.value as Data<List<KeymapListItemModel>>).data.any { keymap ->
                    keymap.actionList.any { it.error is NoCompatibleImeEnabled }
                }) {

                mImeServiceStatusState.value = StatusLayout.State.ERROR
            }

        } else {
            mImeServiceStatusState.value = StatusLayout.State.WARN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
                mDndAccessStatusState.value = StatusLayout.State.POSITIVE
            } else {
                mDndAccessStatusState.value = StatusLayout.State.WARN
            }

            if (powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {
                mBatteryOptimisationState.value = StatusLayout.State.POSITIVE
            } else {
                mBatteryOptimisationState.value = StatusLayout.State.WARN
            }
        }

        val states = listOf(
            mAccessibilityServiceStatusState,
            mWriteSettingsStatusState,
            mImeServiceStatusState,
            mDndAccessStatusState,
            mBatteryOptimisationState
        )

        when {
            states.all { it.value == StatusLayout.State.POSITIVE } -> {
                mExpanded.value = false
                mCollapsedStatusState.value = StatusLayout.State.POSITIVE
            }

            states.any { it.value == StatusLayout.State.ERROR } -> {
                mExpanded.value = true
                mCollapsedStatusState.value = StatusLayout.State.ERROR
            }

            states.any { it.value == StatusLayout.State.WARN } -> {
                mExpanded.value = false
                mCollapsedStatusState.value = StatusLayout.State.WARN
            }
        }
    }
}
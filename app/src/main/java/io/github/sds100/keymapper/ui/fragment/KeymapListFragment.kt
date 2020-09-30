package io.github.sds100.keymapper.ui.fragment

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.ChooseAppStoreModel
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.DialogChooseAppStoreBinding
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding
import io.github.sds100.keymapper.keymapSimple
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.callback.SelectionCallback
import io.github.sds100.keymapper.ui.view.StatusLayout
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.NoCompatibleImeEnabled
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.worker.SeedDatabaseWorker
import kotlinx.android.synthetic.main.fragment_keymap_list.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.experimental.ExperimentalSplittiesApi
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.toast.toast

/**
 * A placeholder fragment containing a simple view.
 */
@ExperimentalSplittiesApi
class KeymapListFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mViewModel: KeymapListViewModel by activityViewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val selectionProvider: ISelectionProvider
        get() = mViewModel.selectionProvider

    private val mController = KeymapController()

    private lateinit var mBinding: FragmentKeymapListBinding

    private val mExpanded = MutableLiveData(false)
    private val mCollapsedStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mAccessibilityServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mImeServiceStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mDndAccessStatusState = MutableLiveData(StatusLayout.State.ERROR)
    private val mWriteSettingsStatusState = MutableLiveData(StatusLayout.State.ERROR)

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                /*when the input method changes, update the action descriptions in case any need to show an error
                * that they need the input method to be enabled. */
                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    mViewModel.rebuildModels()
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

    private val mRestoreLauncher by lazy {
        requireActivity().registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel.restore(requireContext().contentResolver.openInputStream(it))
        }
    }

    private val mBackupLauncher by lazy {
        requireActivity().registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel.backup(requireActivity().contentResolver.openOutputStream(it),
                *selectionProvider.selectedIds)

            selectionProvider.stopSelecting()
        }
    }

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            requireActivity().registerReceiver(mBroadcastReceiver, this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentKeymapListBinding.inflate(inflater, container, false).apply {
            mBinding = this
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            setOnNewKeymapClick {
                val direction =
                    KeymapListFragmentDirections.actionToConfigKeymap(ConfigKeymapViewModel.NEW_KEYMAP_ID)
                findNavController().navigate(direction)
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_help -> {
                        val direction = KeymapListFragmentDirections.actionGlobalHelpFragment()
                        findNavController().navigate(direction)

                        true
                    }

                    R.id.action_seed_database -> {
                        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
                        WorkManager.getInstance(requireContext()).enqueue(request)
                        true
                    }

                    R.id.action_select_all -> {
                        selectionProvider.selectAll()
                        true
                    }

                    R.id.action_enable -> {
                        mViewModel.enableKeymaps(*selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_disable -> {
                        mViewModel.disableKeymaps(*selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_duplicate_keymap -> {
                        mViewModel.duplicate(*selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_backup -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mBackupLauncher.launch(BackupUtils.createFileName())
                        }

                        true
                    }

                    else -> false
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (selectionProvider.isSelectable.value == true) {
                    selectionProvider.stopSelecting()
                } else {
                    requireActivity().finish()
                }
            }

            appBar.setNavigationOnClickListener {
                if (selectionProvider.isSelectable.value == true) {
                    selectionProvider.stopSelecting()
                } else {
                    findNavController().navigate(R.id.action_global_menuFragment)
                }
            }

            setOnConfirmSelectionClick {
                mViewModel.delete(*mViewModel.selectionProvider.selectedIds)
                selectionProvider.stopSelecting()
            }

            mViewModel.apply {

                keymapModelList.observe(viewLifecycleOwner) { keymapList ->
                    mController.keymapList = keymapList
                }

                selectionProvider.apply {

                    isSelectable.observe(viewLifecycleOwner, Observer { isSelectable ->
                        mController.requestModelBuild()

                        if (isSelectable) {
                            appBar.replaceMenu(R.menu.menu_multi_select)
                        } else {
                            appBar.replaceMenu(R.menu.menu_keymap_list)
                        }
                    })

                    callback = mController
                }

                rebuildModelsEvent.observe(viewLifecycleOwner, EventObserver {
                    viewLifecycleOwner.lifecycleScope.launch {
                        mViewModel.setModelList(buildModelList(it))
                    }
                })
            }

            mBackupRestoreViewModel.showMessageStringRes.observe(viewLifecycleOwner, EventObserver { messageRes ->
                when (messageRes) {
                    else -> toast(messageRes)
                }
            })

            mBackupRestoreViewModel.showErrorMessage.observe(viewLifecycleOwner, EventObserver { failure ->
                toast(failure.getFullMessage(requireContext()))
            })

            mBackupRestoreViewModel.requestRestore.observe(viewLifecycleOwner, EventObserver {
                mRestoreLauncher.launch(FileUtils.MIME_TYPE_JSON)
            })

            expanded = mExpanded
            collapsedStatusLayoutState = mCollapsedStatusState
            accessibilityServiceStatusState = mAccessibilityServiceStatusState
            imeServiceStatusState = mImeServiceStatusState
            dndAccessStatusState = mDndAccessStatusState
            writeSettingsStatusState = mWriteSettingsStatusState

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
                PermissionUtils.requestPermission(requireActivity(), Manifest.permission.WRITE_SECURE_SETTINGS) {
                    updateStatusLayouts()
                }
            }

            setGrantDndAccess {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PermissionUtils.requestPermission(requireActivity(),
                        Manifest.permission.ACCESS_NOTIFICATION_POLICY) {
                        updateStatusLayouts()
                    }
                }
            }

            mExpanded.observe(viewLifecycleOwner) {
                if (it == true) {
                    expandableLayout.expand()
                } else {
                    expandableLayout.collapse()

                    val transition = Fade()
                    TransitionManager.beginDelayedTransition(layoutCollapsed, transition)
                }
            }

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

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //assign in onViewCreated in case context is required when building the models.
        epoxyRecyclerView.adapter = mController.adapter
    }

    private suspend fun buildModelList(keymapList: List<KeyMap>) =
        keymapList.map { keymap ->
            KeymapListItemModel(
                id = keymap.id,
                actionList = keymap.actionList.map { it.buildChipModel(requireContext()) },
                triggerDescription = keymap.trigger.buildDescription(requireContext(), mViewModel.getDeviceInfoList()),
                constraintList = keymap.constraintList.map { it.buildModel(requireContext()) },
                constraintMode = keymap.constraintMode,
                flagsDescription = keymap.trigger.buildTriggerFlagsDescription(requireContext()),
                isEnabled = keymap.isEnabled
            )
        }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildModels()
        updateStatusLayouts()
        requireContext().defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        if (PackageUtils.isAppInstalled(KeyboardUtils.KEY_MAPPER_GUI_IME_PACKAGE)
            || Build.VERSION.SDK_INT < KeyboardUtils.KEY_MAPPER_GUI_IME_MIN_API) {
            AppPreferences.showGuiKeyboardAd = false
        }
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

        } else if (mViewModel.keymapModelList.value?.any { keymap ->
                keymap.actionList.any { it.error is NoCompatibleImeEnabled }
            } == true) {

            mImeServiceStatusState.value = StatusLayout.State.ERROR
        } else {
            mImeServiceStatusState.value = StatusLayout.State.WARN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
                mDndAccessStatusState.value = StatusLayout.State.POSITIVE
            } else {
                mDndAccessStatusState.value = StatusLayout.State.WARN
            }
        }

        val states = listOf(
            mAccessibilityServiceStatusState,
            mWriteSettingsStatusState,
            mImeServiceStatusState,
            mDndAccessStatusState
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

    inner class KeymapController : EpoxyController(), SelectionCallback {
        var keymapList: List<KeymapListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            keymapList.forEach {
                keymapSimple {
                    id(it.id)
                    model(it)
                    isSelectable(selectionProvider.isSelectable.value)
                    isSelected(selectionProvider.isSelected(it.id))

                    onErrorClick(object : ErrorClickCallback {
                        override fun onErrorClick(failure: Failure) {
                            mBinding.coordinatorLayout.longSnack(failure.getFullMessage(requireContext())) {

                                //only add an action to fix the error if the error can be recovered from
                                if (failure is RecoverableFailure) {
                                    action(R.string.snackbar_fix) {
                                        lifecycleScope.launch {
                                            failure.recover(requireActivity()) {
                                                mViewModel.rebuildModels()
                                            }
                                        }
                                    }
                                }

                                setAnchorView(R.id.fab)
                                show()
                            }
                        }
                    })

                    onClick { _ ->
                        val id = it.id

                        if (selectionProvider.isSelectable.value == true) {
                            selectionProvider.toggleSelection(id)
                        } else {
                            val direction = KeymapListFragmentDirections.actionToConfigKeymap(id)
                            findNavController().navigate(direction)
                        }
                    }

                    onLongClick { _ ->
                        selectionProvider.run {
                            val startedSelecting = startSelecting()

                            if (startedSelecting) {
                                toggleSelection(it.id)
                            }

                            startedSelecting
                        }
                    }
                }
            }
        }

        override fun onSelect(id: Long) {
            requestModelBuild()
        }

        override fun onUnselect(id: Long) {
            requestModelBuild()
        }

        override fun onSelectAll() {
            requestModelBuild()
        }
    }
}
package io.github.sds100.keymapper.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.automaticBackupLocation
import io.github.sds100.keymapper.data.hasRootPermission
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.SettingsViewModel
import io.github.sds100.keymapper.databinding.FragmentSettingsBinding
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.*

class SettingsFragment : Fragment() {

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentSettingsBinding? = null
    val binding: FragmentSettingsBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentSettingsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_help -> {
                    UrlUtils.openUrl(requireContext(), str(R.string.url_settings_guide))
                    true
                }

                else -> false
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    companion object {

        private val KEYS_REQUIRING_WRITE_SECURE_SETTINGS = sequence {
            if (KeyboardUtils.IS_WRITE_SECURE_SETTINGS_REQUIRED_TO_SWITCH_KEYBOARD) {
                yield(Keys.autoChangeImeOnDeviceConnect)
                yield(Keys.toggleKeyboardOnToggleKeymaps)
                yield(Keys.showToggleKeyboardNotification)
                yield(Keys.devicesThatToggleKeyboard)
            }
        }.toList()
    }

    private val backupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val chooseAutomaticBackupLocationLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                globalPreferences.set(Keys.automaticBackupLocation, it.toString())

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

                backupRestoreViewModel.backupAll(
                    requireContext().contentResolver.openOutputStream(
                        it
                    )
                )
            }
        }

    private val viewModel by viewModels<SettingsViewModel> {
        InjectorUtils.provideSettingsViewModel(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = viewModel.sharedPrefsDataStoreWrapper
        addPreferencesFromResource(R.xml.preferences_empty)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populatePreferenceScreen()
    }

    override fun onResume() {
        super.onResume()

        KEYS_REQUIRING_WRITE_SECURE_SETTINGS.forEach {
            findPreference<Preference>(it.name)?.isEnabled =
                PermissionUtils.haveWriteSecureSettingsPermission(requireContext())
        }
    }

    private fun populateDevicesPreferences() {
        val devices = InputDeviceUtils.createDeviceInfoModelsForExternal()

        //the user will see the names of the devices
        val preferences = arrayOf<MultiSelectListPreference?>(
            findPreference(Keys.devicesThatShowImePicker.name),
            findPreference(Keys.devicesThatToggleKeyboard.name),
            findPreference(Keys.devicesToRerouteKeyEvents.name)
        )

        preferences.forEach { preference ->
            preference ?: return@forEach
            preference.entries = devices.map { it.name }.toTypedArray()
            preference.entryValues = devices.map { it.descriptor }.toTypedArray()

            if (devices.isEmpty()) {
                preference.setDialogMessage(R.string.dialog_message_no_external_devices_connected)
            } else {
                preference.dialogMessage = null
            }
        }
    }

    private fun populatePreferenceScreen() = preferenceScreen.apply {
        //dark theme
        DropDownPreference(requireContext()).apply {
            key = Keys.darkTheme.name
            setDefaultValue(PreferenceDefaults.DARK_THEME)

            setTitle(R.string.title_pref_dark_theme)
            entries = strArray(R.array.pref_dark_theme_entries)
            entryValues = arrayOf("0", "1", "2")
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            isSingleLineTitle = false

            addPreference(this)
        }

        //automatic backup location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Preference(requireContext()).apply {
                key = Keys.automaticBackupLocation.name
                setDefaultValue("")

                setTitle(R.string.title_pref_automatic_backup_location)
                isSingleLineTitle = false

                viewLifecycleScope.launchWhenResumed {
                    globalPreferences.automaticBackupLocation.collectLatest { backupLocation ->
                        summary = if (backupLocation.isBlank()) {
                            str(R.string.summary_pref_automatic_backup_location_disabled)
                        } else {
                            backupLocation
                        }
                    }
                }

                setOnPreferenceClickListener {
                    val backupLocation = globalPreferences.automaticBackupLocation.firstBlocking()

                    if (backupLocation.isBlank()) {
                        chooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)

                    } else {
                        requireContext().alertDialog {
                            messageResource = R.string.dialog_message_change_location_or_disable

                            positiveButton(R.string.pos_change_location) {
                                chooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)
                            }

                            negativeButton(R.string.neg_turn_off) {
                                globalPreferences.set(Keys.automaticBackupLocation, "")
                            }

                            show()
                        }
                    }

                    true
                }
                addPreference(this)
            }
        }

        //hide home screen alerts
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.hideHomeScreenAlerts.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_hide_home_screen_alerts)
            setSummary(R.string.summary_pref_hide_home_screen_alerts)
            isSingleLineTitle = false

            addPreference(this)
        }

        //force vibrate
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.forceVibrate.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_force_vibrate)
            setSummary(R.string.summary_pref_force_vibrate)
            isSingleLineTitle = false

            addPreference(this)
        }

        //show device descriptors
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.showDeviceDescriptors.name
            setDefaultValue(false)
            isSingleLineTitle = false

            setTitle(R.string.title_pref_show_device_descriptors)
            setSummary(R.string.summary_pref_show_device_descriptors)
            isSingleLineTitle = false

            addPreference(this)
        }

        //toggle key maps notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showToggleKeymapsNotification.name

                setTitle(R.string.title_pref_show_toggle_keymaps_notification)
                setSummary(R.string.summary_pref_show_toggle_keymaps_notification)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationUtils.CHANNEL_TOGGLE_KEYMAPS
                    )

                    true
                }

                addPreference(this)
            }

        } else {
            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.showToggleKeymapsNotification.name
                setDefaultValue(true)

                setTitle(R.string.title_pref_show_toggle_keymaps_notification)
                setSummary(R.string.summary_pref_show_toggle_keymaps_notification)
                isSingleLineTitle = false

                addPreference(this)
            }
        }

        //android 11 device id reset bug work around
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val category = PreferenceCategory(requireContext())

            addPreference(category)

            SwitchPreference(requireContext()).apply {
                key = Keys.rerouteKeyEvents.name
                setDefaultValue(false)

                setTitle(R.string.title_pref_devices_to_reroute_keyevents)
                setSummary(R.string.summary_pref_devices_to_reroute_keyevents)
                isSingleLineTitle = false

                category.addPreference(this)
            }

            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_install_gui_keyboard)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    KeyboardUtils.showDialogToInstallKeyMapperGuiKeyboard(requireContext())

                    true
                }

                category.addPreference(this)
            }

            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_enable_ime)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    KeyboardUtils.enableCompatibleInputMethods(requireContext())

                    true
                }

                category.addPreference(this)
            }

            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_choose_ime)
                isSingleLineTitle = false
                setOnPreferenceClickListener {
                    KeyboardUtils.chooseCompatibleInputMethod(requireContext())

                    true
                }

                category.addPreference(this)
            }

            createDevicesPreference(
                Keys.devicesToRerouteKeyEvents.name,
                R.string.title_pref_devices_to_reroute_keyevents_choose_devices
            ).apply {
                category.addPreference(this)
            }

            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_guide)
                setOnPreferenceClickListener {
                    UrlUtils.openUrl(
                        requireContext(),
                        str(R.string.url_android_11_bug_reset_id_work_around_setting_guide)
                    )

                    true
                }

                category.addPreference(this)
            }

            globalPreferences.getFlow(Keys.rerouteKeyEvents)
                .onEach { enabled ->
                    for (i in 0 until category.preferenceCount) {
                        category.getPreference(i).apply {
                            if (this.key != Keys.rerouteKeyEvents.name) {
                                this.isVisible = enabled ?: false
                            }
                        }
                    }
                }
                .launchIn(viewLifecycleScope)

            viewLifecycleScope.launch {
                for (i in 0 until category.preferenceCount) {
                    category.getPreference(i).apply {
                        if (this.key != Keys.rerouteKeyEvents.name) {
                            this.isVisible = globalPreferences.get(Keys.rerouteKeyEvents) ?: false
                        }
                    }
                }
            }
        }

        createCategoryDefaults()

        //apps can't show the keyboard picker when in the background from Android 8.1+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            createKeyboardPickerCategory()
        }

        createCategoryToAutomaticallySwitchTheKeyboard()
        createRootCategory()
    }

    @SuppressLint("NewApi")
    private fun createRootCategory() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_root)
        preferenceScreen.addPreference(this)

        Preference(requireContext()).apply {
            isSelectable = false
            setSummary(R.string.summary_pref_category_root)
            isSingleLineTitle = false

            addPreference(this)
        }

        //root permission switch
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.hasRootPermission.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_root_permission)
            setSummary(R.string.summary_pref_root_permission)
            isSingleLineTitle = false

            addPreference(this)
        }

        //only show the options to show the keyboard picker when rooted in these versions
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P) {
            viewLifecycleScope.launchWhenResumed {
                globalPreferences.hasRootPermission.collectLatest {
                    findPreference<Preference>(Keys.showImePickerNotification.name)?.isEnabled = it
                    findPreference<Preference>(Keys.autoShowImePickerOnDeviceConnect.name)?.isEnabled =
                        it
                    findPreference<Preference>(Keys.devicesThatShowImePicker.name)?.isEnabled = it
                }
            }

            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showImePickerNotification.name

                setTitle(R.string.title_pref_show_ime_picker_notification)
                setSummary(R.string.summary_pref_show_ime_picker_notification)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationUtils.CHANNEL_IME_PICKER
                    )

                    true
                }

                addPreference(this)
            }

            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.autoShowImePickerOnDeviceConnect.name
                setDefaultValue(false)

                setTitle(R.string.title_pref_auto_show_ime_picker)
                setSummary(R.string.summary_pref_auto_show_ime_picker)
                isSingleLineTitle = false

                addPreference(this)
            }

            createDevicesPreference(Keys.devicesThatShowImePicker.name).apply {
                addPreference(this)
            }
        }
    }

    private fun createCategoryToAutomaticallySwitchTheKeyboard() {
        val category = PreferenceCategory(requireContext())
        preferenceScreen.addPreference(category)

        //accessibility services in Android 11+ can change the input method without needing special permissions
        val requireWriteSecureSettingsPermission =
            KeyboardUtils.IS_WRITE_SECURE_SETTINGS_REQUIRED_TO_SWITCH_KEYBOARD

        if (requireWriteSecureSettingsPermission) {
            category.setTitle(R.string.title_pref_category_write_secure_settings)

            Preference(requireContext()).apply {
                isSelectable = false
                setSummary(R.string.summary_pref_category_write_secure_settings)
                isSingleLineTitle = false

                category.addPreference(this)
            }
        }

        //automatically change the keyboard when a device (dis)connects
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.autoChangeImeOnDeviceConnect.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_auto_change_ime_on_connection)
            setSummary(R.string.summary_pref_auto_change_ime_on_connection)
            isSingleLineTitle = false

            if (requireWriteSecureSettingsPermission) {
                isEnabled = PermissionUtils.haveWriteSecureSettingsPermission(requireContext())
            }

            category.addPreference(this)
        }

        createDevicesPreference(Keys.devicesThatToggleKeyboard.name).apply {
            category.addPreference(this)
        }

        //toggle keyboard when toggling key maps
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.toggleKeyboardOnToggleKeymaps.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_toggle_keyboard_on_toggle_keymaps)
            setSummary(R.string.summary_pref_toggle_keyboard_on_toggle_keymaps)
            isSingleLineTitle = false

            if (requireWriteSecureSettingsPermission) {
                isEnabled = PermissionUtils.haveWriteSecureSettingsPermission(requireContext())
            }

            category.addPreference(this)
        }

        //toggle keyboard notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showToggleKeyboardNotification.name

                setTitle(R.string.title_pref_show_toggle_keyboard_notification)
                setSummary(R.string.summary_pref_show_toggle_keyboard_notification)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationUtils.CHANNEL_TOGGLE_KEYBOARD
                    )

                    true
                }

                category.addPreference(this)
            }

        } else {
            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.showToggleKeyboardNotification.name
                setDefaultValue(true)

                setTitle(R.string.title_pref_show_toggle_keyboard_notification)
                setSummary(R.string.summary_pref_show_toggle_keyboard_notification)
                isSingleLineTitle = false

                category.addPreference(this)
            }
        }
    }

    private fun createKeyboardPickerCategory() = PreferenceCategory(requireContext()).apply {

        setTitle(R.string.title_pref_category_ime_picker)
        preferenceScreen.addPreference(this)

        //show keyboard picker notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showImePickerNotification.name

                setTitle(R.string.title_pref_show_ime_picker_notification)
                setSummary(R.string.summary_pref_show_ime_picker_notification)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationUtils.CHANNEL_IME_PICKER
                    )

                    true
                }

                addPreference(this)
            }
        } else {
            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.showImePickerNotification.name
                setDefaultValue(false)

                setTitle(R.string.title_pref_show_ime_picker_notification)
                setSummary(R.string.summary_pref_show_ime_picker_notification)
                isSingleLineTitle = false

                addPreference(this)
            }
        }

        //auto show keyboard picker
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.autoShowImePickerOnDeviceConnect.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_auto_show_ime_picker)
            setSummary(R.string.summary_pref_auto_show_ime_picker)
            isSingleLineTitle = false

            addPreference(this)
        }

        createDevicesPreference(Keys.devicesThatShowImePicker.name).apply {
            addPreference(this)
        }
    }

    private fun createCategoryDefaults() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_defaults)
        preferenceScreen.addPreference(this)

        //long press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.longPressDelay.name
            setDefaultValue(PreferenceDefaults.LONG_PRESS_DELAY)

            setTitle(R.string.title_pref_long_press_delay)
            setSummary(R.string.summary_pref_long_press_delay)
            isSingleLineTitle = false

            min = int(R.integer.long_press_delay_min)
            max = int(R.integer.long_press_delay_max)
            showSeekBarValue = true

            addPreference(this)
        }

        //double press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.doublePressDelay.name
            setDefaultValue(PreferenceDefaults.DOUBLE_PRESS_DELAY)

            setTitle(R.string.title_pref_double_press_delay)
            setSummary(R.string.summary_pref_double_press_delay)
            isSingleLineTitle = false

            min = int(R.integer.double_press_delay_min)
            max = int(R.integer.double_press_delay_max)
            showSeekBarValue = true

            addPreference(this)
        }

        //vibration duration
        SeekBarPreference(requireContext()).apply {
            key = Keys.vibrateDuration.name
            setDefaultValue(PreferenceDefaults.VIBRATION_DURATION)

            setTitle(R.string.title_pref_vibration_duration)
            setSummary(R.string.summary_pref_vibration_duration)
            isSingleLineTitle = false

            min = int(R.integer.vibrate_duration_min)
            max = int(R.integer.vibrate_duration_max)
            showSeekBarValue = true

            addPreference(this)
        }

        //repeat delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.repeatDelay.name
            setDefaultValue(PreferenceDefaults.REPEAT_DELAY)

            setTitle(R.string.title_pref_repeat_delay)
            setSummary(R.string.summary_pref_repeat_delay)
            isSingleLineTitle = false

            min = int(R.integer.repeat_delay_min)
            max = int(R.integer.repeat_delay_max)
            showSeekBarValue = true

            addPreference(this)
        }

        //repeat rate
        SeekBarPreference(requireContext()).apply {
            key = Keys.repeatRate.name
            setDefaultValue(PreferenceDefaults.REPEAT_RATE)

            setTitle(R.string.title_pref_repeat_rate)
            setSummary(R.string.summary_pref_repeat_rate)
            isSingleLineTitle = false

            min = int(R.integer.repeat_rate_min)
            max = int(R.integer.repeat_rate_max)
            showSeekBarValue = true

            addPreference(this)
        }

        //sequence trigger timeout
        SeekBarPreference(requireContext()).apply {
            key = Keys.sequenceTriggerTimeout.name
            setDefaultValue(PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT)

            setTitle(R.string.title_pref_sequence_trigger_timeout)
            setSummary(R.string.summary_pref_sequence_trigger_timeout)
            isSingleLineTitle = false

            min = int(R.integer.sequence_trigger_timeout_min)
            max = int(R.integer.sequence_trigger_timeout_max)
            showSeekBarValue = true

            addPreference(this)
        }
    }

    private fun createDevicesPreference(
        key: String,
        @StringRes title: Int = R.string.title_pref_choose_devices
    ) = MultiSelectListPreference(requireContext()).apply {
        this.key = key

        setTitle(title)
        isSingleLineTitle = false

        setOnPreferenceClickListener { preference ->
            populateDevicesPreferences()

            if ((preference as MultiSelectListPreference).entries.isNullOrEmpty()) {
                return@setOnPreferenceClickListener false
            }

            true
        }
    }
}
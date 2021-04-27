package io.github.sds100.keymapper.settings

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.databinding.FragmentSettingsBinding
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationUtils
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.CancellableMultiSelectListPreference
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import splitties.alertdialog.appcompat.*

class SettingsFragment : Fragment() {

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentSettingsBinding? = null
    val binding: FragmentSettingsBinding
        get() = _binding!!

    private val viewModel by activityViewModels<SettingsViewModel> {
        Inject.settingsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        viewModel.showPopups(this, binding)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    companion object {
        private val KEYS_REQUIRING_WRITE_SECURE_SETTINGS = arrayOf(
            Keys.changeImeOnDeviceConnect,
            Keys.toggleKeyboardOnToggleKeymaps,
            Keys.showToggleKeyboardNotification,
            Keys.devicesThatChangeIme
        )

        private const val KEY_ENABLE_COMPATIBLE_IME = "pref_key_enable_compatible_ime"
        private const val KEY_CHOSE_COMPATIBLE_IME = "pref_key_chose_compatible_ime"
    }

    private var showingNoPairedDevicesDialog = false

    private val chooseAutomaticBackupLocationLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.setAutomaticBackupLocation(it.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
        }

    private val viewModel by activityViewModels<SettingsViewModel> {
        Inject.settingsViewModel(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = viewModel.sharedPrefsDataStoreWrapper
        addPreferencesFromResource(R.xml.preferences_empty)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleScope.launchWhenResumed {
            populatePreferenceScreen()
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.showWriteSecureSettingsSection.collectLatest { show ->
                KEYS_REQUIRING_WRITE_SECURE_SETTINGS.forEach {
                    findPreference<Preference>(it.name)?.isEnabled = show
                }
            }
        }
    }

    private fun populatePreferenceScreen() = preferenceScreen.apply {
        //dark theme
        DropDownPreference(requireContext()).apply {
            key = Keys.darkTheme.name
            setDefaultValue(PreferenceDefaults.DARK_THEME)
            isSingleLineTitle = false

            setTitle(R.string.title_pref_dark_theme)
            entries = strArray(R.array.pref_dark_theme_entries)
            entryValues = ThemeUtils.THEMES.map { it.toString() }.toTypedArray()
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

            addPreference(this)
        }

        //automatic backup location
        Preference(requireContext()).apply {
            key = Keys.automaticBackupLocation.name
            setDefaultValue("")

            setTitle(R.string.title_pref_automatic_backup_location)
            isSingleLineTitle = false

            viewModel.automaticBackupLocation.collectWhenResumed(viewLifecycleOwner, {
                summary = if (it.isBlank()) {
                    str(R.string.summary_pref_automatic_backup_location_disabled)
                } else {
                    it
                }
            })

            setOnPreferenceClickListener {
                val backupLocation = viewModel.automaticBackupLocation.firstBlocking()

                if (backupLocation.isBlank()) {
                    chooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)

                } else {
                    requireContext().alertDialog {
                        messageResource = R.string.dialog_message_change_location_or_disable

                        positiveButton(R.string.pos_change_location) {
                            chooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)
                        }

                        negativeButton(R.string.neg_turn_off) {
                            viewModel.disableAutomaticBackup()
                        }

                        show()
                    }
                }

                true
            }
            addPreference(this)
        }

        //hide home screen alerts
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.hideHomeScreenAlerts.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_hide_home_screen_alerts)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_hide_home_screen_alerts)

            addPreference(this)
        }

        //force vibrate
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.forceVibrate.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_force_vibrate)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_force_vibrate)

            addPreference(this)
        }

        //show device descriptors
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.showDeviceDescriptors.name
            setDefaultValue(false)
            isSingleLineTitle = false

            setTitle(R.string.title_pref_show_device_descriptors)
            setSummary(R.string.summary_pref_show_device_descriptors)

            addPreference(this)
        }

        //toggle key maps notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showToggleKeymapsNotification.name

                setTitle(R.string.title_pref_show_toggle_keymaps_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_toggle_keymaps_notification)

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationController.CHANNEL_TOGGLE_KEYMAPS
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
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_toggle_keymaps_notification)

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

            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_install_gui_keyboard)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    DialogUtils.showDialogToGetGuiKeyboard(requireContext())

                    true
                }

                category.addPreference(this)
            }

            Preference(requireContext()).apply {
                key = KEY_ENABLE_COMPATIBLE_IME
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_enable_ime)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    viewModel.onEnableCompatibleImeClick()

                    true
                }

                category.addPreference(this)

                viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
                    viewModel.isCompatibleImeEnabled.collectLatest { isCompatibleImeEnabled ->
                        icon = if (isCompatibleImeEnabled) {
                            drawable(R.drawable.ic_outline_check_circle_outline_24)
                        } else {
                            drawable(R.drawable.ic_baseline_error_outline_24)
                        }
                    }
                }
            }

            Preference(requireContext()).apply {
                key = KEY_CHOSE_COMPATIBLE_IME
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_choose_ime)
                isSingleLineTitle = false
                setOnPreferenceClickListener {
                    viewModel.onChooseCompatibleImeClick()

                    true
                }

                category.addPreference(this)

                viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
                    viewModel.isCompatibleImeChosen.collectLatest { isCompatibleImeChosen ->
                        icon = if (isCompatibleImeChosen) {
                            drawable(R.drawable.ic_outline_check_circle_outline_24)
                        } else {
                            drawable(R.drawable.ic_baseline_error_outline_24)
                        }
                    }
                }
            }

            category.addPreference(
                createChooseDevicesPreference(
                    Keys.devicesToRerouteKeyEvents.name,
                    R.string.title_pref_devices_to_reroute_keyevents_choose_devices
                )
            )

            viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
                viewModel.rerouteKeyEvents.collectLatest { enabled ->
                    for (i in 0 until category.preferenceCount) {
                        category.getPreference(i).apply {
                            if (this.key != Keys.rerouteKeyEvents.name) {
                                this.isVisible = enabled
                            }
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

        createWriteSecureSettingsCategory()
        createRootCategory()
    }

    @SuppressLint("NewApi")
    private fun createRootCategory() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_root)
        preferenceScreen.addPreference(this)

        Preference(requireContext()).apply {
            isSelectable = false
            setSummary(R.string.summary_pref_category_root)

            addPreference(this)
        }

        //root permission switch
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.hasRootPermission.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_root_permission)
            setSummary(R.string.summary_pref_root_permission)

            addPreference(this)
        }

        //only show the options to show the keyboard picker when rooted in these versions
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P) {
            viewModel.hasRootPermission.collectWhenResumed(viewLifecycleOwner) {
                findPreference<Preference>(Keys.showImePickerNotification.name)?.isEnabled = it
                findPreference<Preference>(Keys.showImePickerOnDeviceConnect.name)?.isEnabled = it
                findPreference<Preference>(Keys.devicesThatShowImePicker.name)?.isEnabled =
                    it
            }

            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showImePickerNotification.name

                setTitle(R.string.title_pref_show_ime_picker_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_ime_picker_notification)

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationController.CHANNEL_IME_PICKER
                    )

                    true
                }

                addPreference(this)
            }

            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.showImePickerOnDeviceConnect.name
                setDefaultValue(false)

                setTitle(R.string.title_pref_auto_show_ime_picker)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_auto_show_ime_picker)

                addPreference(this)
            }

            addPreference(createChooseDevicesPreference(Keys.devicesThatShowImePicker.name))
        }
    }

    private fun createWriteSecureSettingsCategory() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_write_secure_settings)
        preferenceScreen.addPreference(this)

        Preference(requireContext()).apply {
            isSelectable = false
            setSummary(R.string.summary_pref_category_write_secure_settings)

            addPreference(this)
        }

        //automatically change the keyboard when a bluetooth device (dis)connects
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.changeImeOnDeviceConnect.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_auto_change_ime_on_connection)
            setSummary(R.string.summary_pref_auto_change_ime_on_connection)

            addPreference(this)
        }

        addPreference(createChooseDevicesPreference(Keys.devicesThatChangeIme.name))

        //toggle keyboard when toggling key maps
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.toggleKeyboardOnToggleKeymaps.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_toggle_keyboard_on_toggle_keymaps)
            setSummary(R.string.summary_pref_toggle_keyboard_on_toggle_keymaps)

            addPreference(this)
        }

        //toggle keyboard notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showToggleKeyboardNotification.name

                setTitle(R.string.title_pref_show_toggle_keyboard_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_toggle_keyboard_notification)

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationController.CHANNEL_TOGGLE_KEYBOARD
                    )

                    true
                }

                addPreference(this)
            }

        } else {
            SwitchPreferenceCompat(requireContext()).apply {
                key = Keys.showToggleKeyboardNotification.name
                setDefaultValue(true)

                setTitle(R.string.title_pref_show_toggle_keyboard_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_toggle_keyboard_notification)

                addPreference(this)
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
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_ime_picker_notification)

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationController.CHANNEL_IME_PICKER
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
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_ime_picker_notification)

                addPreference(this)
            }
        }

        //auto show keyboard picker
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.showImePickerOnDeviceConnect.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_auto_show_ime_picker)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_auto_show_ime_picker)

            addPreference(this)
        }

        addPreference(createChooseDevicesPreference(Keys.devicesThatShowImePicker.name))
    }

    private fun createCategoryDefaults() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_defaults)
        preferenceScreen.addPreference(this)

        //long press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultLongPressDelay.name
            setDefaultValue(PreferenceDefaults.LONG_PRESS_DELAY)

            setTitle(R.string.title_pref_long_press_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_long_press_delay)
            min = OptionMinimums.TRIGGER_LONG_PRESS_DELAY
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }

        //double press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultDoublePressDelay.name
            setDefaultValue(PreferenceDefaults.DOUBLE_PRESS_DELAY)

            setTitle(R.string.title_pref_double_press_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_double_press_delay)
            min = OptionMinimums.TRIGGER_DOUBLE_PRESS_DELAY
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }

        //vibration duration
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultVibrateDuration.name
            setDefaultValue(PreferenceDefaults.VIBRATION_DURATION)

            setTitle(R.string.title_pref_vibration_duration)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_vibration_duration)
            min = OptionMinimums.VIBRATION_DURATION
            max = 1000
            showSeekBarValue = true

            addPreference(this)
        }

        //repeat delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultRepeatDelay.name
            setDefaultValue(PreferenceDefaults.REPEAT_DELAY)

            setTitle(R.string.title_pref_repeat_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_repeat_delay)
            min = OptionMinimums.ACTION_REPEAT_DELAY
            max = SliderMaximums.ACTION_REPEAT_DELAY
            showSeekBarValue = true

            addPreference(this)
        }

        //repeat rate
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultRepeatRate.name
            setDefaultValue(PreferenceDefaults.REPEAT_RATE)

            setTitle(R.string.title_pref_repeat_rate)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_repeat_rate)
            min = OptionMinimums.ACTION_REPEAT_RATE
            max = SliderMaximums.ACTION_REPEAT_RATE
            showSeekBarValue = true

            addPreference(this)
        }

        //sequence trigger timeout
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultSequenceTriggerTimeout.name
            setDefaultValue(PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT)

            setTitle(R.string.title_pref_sequence_trigger_timeout)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_sequence_trigger_timeout)
            min = OptionMinimums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }
    }

    private fun createChooseDevicesPreference(
        key: String,
        @StringRes title: Int = R.string.title_pref_choose_devices
    ): Preference {
        return CancellableMultiSelectListPreference(requireContext()).apply {
            this.key = key

            setTitle(title)
            isSingleLineTitle = false

            setOnPreferenceClickListener { preference ->
                val devicesAdapter = ServiceLocator.devicesAdapter(requireContext())
                val devices = devicesAdapter.connectedInputDevices.value
                    .dataOrNull()
                    ?.filter { it.isExternal }
                    ?: emptyList()

                (preference as MultiSelectListPreference).entries =
                    devices.map { it.name }.toTypedArray()

                //the unique addresses of the device will be saved to shared preferences
                preference.entryValues = devices.map { it.descriptor }.toTypedArray()

                //if there are no bluetooth device entries, explain to the user why
                if ((preference as CancellableMultiSelectListPreference).entries.isNullOrEmpty()) {

                    /* This awkward way of showing the "can't find any paired devices" dialog
                     * with a CancellableMultiSelectPreference is necessary since you can't
                     * cancel showing the dialog once the preference has been clicked.*/

                    if (!showingNoPairedDevicesDialog) {
                        showingNoPairedDevicesDialog = true

                        requireContext().alertDialog {
                            message =
                                getString(R.string.dialog_message_settings_no_external_devices_connected)
                            okButton { dialog ->
                                showingNoPairedDevicesDialog = false
                                dialog.dismiss()
                            }

                            //if the dialog is closed by clicking outside the dialog
                            setOnCancelListener { showingNoPairedDevicesDialog = false }
                        }.show()
                    }

                    return@setOnPreferenceClickListener false
                }

                true
            }
        }
    }
}
package io.github.sds100.keymapper.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupUtils
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationUtils
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.collectLatest
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton

/**
 * Created by sds100 on 16/07/2021.
 */
@AndroidEntryPoint
class MainSettingsFragment : BaseSettingsFragment() {

    companion object {
        private const val KEY_GRANT_WRITE_SECURE_SETTINGS = "pref_key_grant_write_secure_settings"
        private const val CATEGORY_KEY_GRANT_WRITE_SECURE_SETTINGS =
            "category_key_grant_write_secure_settings"
        private const val KEY_GRANT_SHIZUKU = "pref_key_grant_shizuku"
        private const val KEY_AUTOMATICALLY_CHANGE_IME_LINK =
            "pref_automatically_change_ime_link"
    }

    private val chooseAutomaticBackupLocationLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.setAutomaticBackupLocation(it.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = viewModel.sharedPrefsDataStoreWrapper
        addPreferencesFromResource(R.xml.preferences_empty)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleScope.launchWhenResumed {
            if (preferenceScreen.isEmpty()) {
                populatePreferenceScreen()
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.automaticBackupLocation.collectLatest { backupLocation ->
                val preference =
                    findPreference<Preference>(Keys.automaticBackupLocation.name)
                        ?: return@collectLatest
                preference.summary = if (backupLocation.isBlank()) {
                    str(R.string.summary_pref_automatic_backup_location_disabled)
                } else {
                    backupLocation
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.isWriteSecureSettingsPermissionGranted.collectLatest { isGranted ->
                val writeSecureSettingsCategory =
                    findPreference<PreferenceCategory>(CATEGORY_KEY_GRANT_WRITE_SECURE_SETTINGS)

                findPreference<Preference>(KEY_GRANT_WRITE_SECURE_SETTINGS)?.apply {
                    isEnabled = !isGranted

                    if (isGranted) {
                        setTitle(R.string.title_pref_grant_write_secure_settings_granted)
                        setIcon(R.drawable.ic_outline_check_circle_outline_24)
                    } else {
                        setTitle(R.string.title_pref_grant_write_secure_settings_not_granted)
                        setIcon(R.drawable.ic_baseline_error_outline_24)
                    }
                }

                writeSecureSettingsCategory
                    ?.findPreference<Preference>(KEY_AUTOMATICALLY_CHANGE_IME_LINK)?.apply {
                        isEnabled = isGranted
                    }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.isShizukuPermissionGranted.collectLatest { isGranted ->
                findPreference<Preference>(KEY_GRANT_SHIZUKU)?.apply {
                    if (isGranted) {
                        setTitle(R.string.title_pref_grant_shizuku_granted)
                        setIcon(R.drawable.ic_outline_check_circle_outline_24)
                    } else {
                        setTitle(R.string.title_pref_grant_shizuku_not_granted)
                        setIcon(R.drawable.ic_baseline_error_outline_24)
                    }
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

        //default options
        Preference(requireContext()).apply {
            setTitle(R.string.title_pref_default_options)
            setSummary(R.string.summary_pref_default_options)
            isSingleLineTitle = false

            setOnPreferenceClickListener {
                val direction = MainSettingsFragmentDirections.toDefaultOptionsSettingsFragment()
                findNavController().navigate(direction)

                true
            }

            addPreference(this)
        }

        //apps can't show the keyboard picker when in the background from Android 8.1+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_category_ime_picker)
                setSummary(R.string.summary_pref_category_ime_picker)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    val direction = MainSettingsFragmentDirections.toImePickerSettingsFragment()
                    findNavController().navigate(direction)

                    true
                }

                addPreference(this)
            }
        }

        //delete sound files
        Preference(requireContext()).apply {
            setTitle(R.string.title_pref_delete_sound_files)
            setSummary(R.string.summary_pref_delete_sound_files)
            isSingleLineTitle = false

            setOnPreferenceClickListener {
                viewModel.onDeleteSoundFilesClick()

                true
            }

            addPreference(this)
        }

        //link to settings to automatically change the ime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addPreference(automaticallyChangeImeSettingsLink())
        }

        //android 11 device id reset work around
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_reroute_keyevents_link)
                setSummary(R.string.summary_pref_reroute_keyevents_link)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    val direction =
                        MainSettingsFragmentDirections.toAndroid11BugWorkaroundSettingsFragment()
                    findNavController().navigate(direction)

                    true
                }

                addPreference(this)
            }
        }

        //Shizuku
        //shizuku is only supported on Marhsmallow+
        if (ShizukuUtils.isSupportedForSdkVersion()) {
            Preference(requireContext()).apply {
                setTitle(R.string.title_pref_category_shizuku)
                setSummary(R.string.summary_pref_category_shizuku)
                isSingleLineTitle = false

                setOnPreferenceClickListener {
                    val direction =
                        MainSettingsFragmentDirections.toShizukuSettingsFragment()
                    findNavController().navigate(direction)

                    true
                }

                addPreference(this)
            }
        }

        //write secure settings
        PreferenceCategory(requireContext()).apply {
            key = CATEGORY_KEY_GRANT_WRITE_SECURE_SETTINGS
            setTitle(R.string.title_pref_category_write_secure_settings)

            preferenceScreen.addPreference(this)

            Preference(requireContext()).apply {
                isSelectable = false
                setSummary(R.string.summary_pref_category_write_secure_settings)

                addPreference(this)
            }

            Preference(requireContext()).apply {
                key = KEY_GRANT_WRITE_SECURE_SETTINGS

                if (viewModel.isWriteSecureSettingsPermissionGranted.firstBlocking()) {
                    setTitle(R.string.title_pref_grant_write_secure_settings_granted)
                    setIcon(R.drawable.ic_outline_check_circle_outline_24)
                } else {
                    setTitle(R.string.title_pref_grant_write_secure_settings_not_granted)
                    setIcon(R.drawable.ic_baseline_error_outline_24)
                }

                setOnPreferenceClickListener {
                    viewModel.requestWriteSecureSettingsPermission()
                    true
                }

                addPreference(this)
            }

            //accessibility services can change the ime on Android 11+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                addPreference(automaticallyChangeImeSettingsLink())
            }
        }

        //root
        createRootCategory()

        //log
        createLogCategory()
    }

    private fun automaticallyChangeImeSettingsLink() = Preference(requireContext()).apply {
        key = KEY_AUTOMATICALLY_CHANGE_IME_LINK

        setTitle(R.string.title_pref_automatically_change_ime)
        setSummary(R.string.summary_pref_automatically_change_ime)
        isSingleLineTitle = false

        setOnPreferenceClickListener {
            val direction = MainSettingsFragmentDirections.toAutomaticallyChangeImeSettings()
            findNavController().navigate(direction)

            true
        }
    }

    private fun createLogCategory() = PreferenceCategory(requireContext()).apply {
        setTitle(R.string.title_pref_category_log)
        preferenceScreen.addPreference(this)

        Preference(requireContext()).apply {
            isSelectable = false
            setSummary(R.string.summary_pref_category_log)

            addPreference(this)
        }

        //enable logging
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.log.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_toggle_logging)

            addPreference(this)
        }

        //open log fragment
        Preference(requireContext()).apply {
            isSingleLineTitle = false
            setTitle(R.string.title_pref_view_and_share_log)

            setOnPreferenceClickListener {
                findNavController().navigate(MainSettingsFragmentDirections.toLogFragment())

                true
            }

            addPreference(this)
        }

        //report issue to developer
        Preference(requireContext()).apply {
            isSingleLineTitle = false
            setTitle(R.string.title_pref_report_issue)

            setOnPreferenceClickListener {
                findNavController().navigate(NavAppDirections.goToReportBugActivity())

                true
            }

            addPreference(this)
        }
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

            addPreference(
                SettingsUtils.createChooseDevicesPreference(
                    requireContext(),
                    viewModel,
                    Keys.devicesThatShowImePicker
                )
            )
        }
    }
}
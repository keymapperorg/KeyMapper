package io.github.sds100.keymapper.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.databinding.FragmentSettingsBinding
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.alertdialog.appcompat.*

class SettingsFragment : Fragment() {

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentSettingsBinding? = null
    val binding: FragmentSettingsBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class SettingsPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val mShowImeNotificationPreference by lazy {
        findPreference<Preference>(str(R.string.key_pref_show_ime_notification))
    }

    private val mToggleRemappingsNotificationPref by lazy {
        findPreference<SwitchPreference>(str(R.string.key_pref_show_toggle_remappings_notification))
    }

    private val mToggleKeyboardNotificationPref by lazy {
        findPreference<Preference>(str(R.string.key_pref_toggle_keyboard_notification))
    }

    private val mBluetoothDevicesPreferences by lazy {
        findPreference<MultiSelectListPreference>(str(R.string.key_pref_bluetooth_devices))!!
    }

    private val mAutoShowIMEDialogPreference by lazy {
        findPreference<SwitchPreference>(str(R.string.key_pref_auto_show_ime_picker))
    }

    private val mRootPrefCategory by lazy {
        findPreference<PreferenceCategory>(str(R.string.key_pref_category_root))!!
    }

    private val mSecureSettingsCategory by lazy {
        findPreference<PreferenceCategory>(str(R.string.key_pref_category_secure_settings))!!
    }

    private val mRootPermissionPreference by lazy {
        findPreference<SwitchPreference>(str(R.string.key_pref_root_permission))!!
    }

    private val mNotificationSettingsPreference by lazy {
        findPreference<Preference>(str(R.string.key_pref_notification_settings))!!
    }

    private val mDarkThemePreference by lazy {
        findPreference<DropDownPreference>(str(R.string.key_pref_dark_theme_mode))
    }

    private val mAutomaticBackupLocation by lazy {
        findPreference<Preference>(str(R.string.key_pref_automatic_backup_location))
    }

    private var mShowingNoPairedDevicesDialog = false

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val mChooseAutomaticBackupLocationLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it ?: return@registerForActivityResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppPreferences.automaticBackupLocation = it.toString()

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

            mBackupRestoreViewModel.backupAll(requireContext().contentResolver.openOutputStream(it))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationSettingsPreference.setOnPreferenceClickListener {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, PACKAGE_NAME)

                    startActivity(this)
                }

                true
            }

            mShowImeNotificationPreference?.setOnPreferenceClickListener {
                NotificationUtils.openChannelSettings(NotificationUtils.CHANNEL_IME_PICKER)

                true
            }

            mToggleKeyboardNotificationPref?.setOnPreferenceClickListener {
                NotificationUtils.openChannelSettings(NotificationUtils.CHANNEL_TOGGLE_KEYBOARD)

                true
            }

        } else {
            mToggleRemappingsNotificationPref?.onPreferenceChangeListener = this
            mShowImeNotificationPreference?.onPreferenceChangeListener = this
            mToggleKeyboardNotificationPref?.onPreferenceChangeListener = this
        }

        mBluetoothDevicesPreferences.setOnPreferenceClickListener {
            populateBluetoothDevicesPreference()

            //if there are no bluetooth device entries, explain to the user why
            if (mBluetoothDevicesPreferences.entries.isEmpty()) {

                /* This awkward way of showing the "can't find any paired devices" dialog
                 * with a CancellableMultiSelectPreference is necessary since you can't
                 * cancel showing the dialog once the preference has been clicked.*/

                if (!mShowingNoPairedDevicesDialog) {
                    mShowingNoPairedDevicesDialog = true

                    requireActivity().alertDialog {
                        title = getString(R.string.dialog_title_cant_find_paired_devices)
                        message = getString(R.string.dialog_message_cant_find_paired_devices)
                        okButton { dialog ->
                            mShowingNoPairedDevicesDialog = false
                            dialog.dismiss()
                        }

                        //if the dialog is closed by clicking outside the dialog
                        setOnCancelListener { mShowingNoPairedDevicesDialog = false }
                    }.show()
                }

                return@setOnPreferenceClickListener false
            }

            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            invalidateAutomaticBackupLocationSummary()

            mAutomaticBackupLocation?.setOnPreferenceClickListener {
                val backupLocation = BackupUtils.getAutomaticBackupLocation().valueOrNull()

                if (backupLocation.isNullOrBlank()) {
                    mChooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)

                } else {
                    requireContext().alertDialog {
                        messageResource = R.string.dialog_message_change_location_or_disable

                        positiveButton(R.string.pos_change_location) {
                            mChooseAutomaticBackupLocationLauncher.launch(BackupUtils.DEFAULT_AUTOMATIC_BACKUP_NAME)
                        }

                        negativeButton(R.string.neg_turn_off) {
                            AppPreferences.automaticBackupLocation = ""
                        }

                        show()
                    }
                }

                true
            }
        }

        enableRootPreferences(mRootPermissionPreference.isChecked)

        mAutoShowIMEDialogPreference?.onPreferenceChangeListener = this
        mRootPermissionPreference.onPreferenceChangeListener = this

        requireContext().defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onPreferenceChange(
            findPreference(key!!),
            //Use .all[index] because we don't know the data type
            sharedPreferences!!.all[key]
        )
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (newValue is Boolean) {
            when (preference) {

                mShowImeNotificationPreference -> {
                    //show/hide the notification when the preference is toggled
                    if (newValue) {
                        NotificationUtils.showIMEPickerNotification(requireContext())
                    } else {
                        NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
                    }
                }

                mToggleKeyboardNotificationPref -> {
                    //show/hide the notification when the preference is toggled
                    if (newValue) {
                        WidgetsManager.invalidateNotifications(requireContext())
                    } else {
                        NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYBOARD)
                    }
                }

                mToggleRemappingsNotificationPref -> {

                    if (newValue) {
                        WidgetsManager.invalidateNotifications(requireContext())
                    } else {

                        NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_KEYMAPS)
                    }
                }

                //Only enable the root preferences if the user has enabled root features
                mRootPermissionPreference -> {
                    enableRootPreferences(newValue)

                    //the pending intents need to be updated so they don't use the root methods
                    WidgetsManager.invalidateNotifications(requireContext())
                }

                mAutomaticBackupLocation -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        invalidateAutomaticBackupLocationSummary()
                    }
                }
            }
        }

        if (newValue is String) {
            when (preference) {
                mDarkThemePreference -> {
                    val mode = AppPreferences.getSdkNightMode(newValue)
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }

        return true
    }

    override fun onResume() {
        super.onResume()

        //only enable the WRITE_SECURE_SETTINGS prefs if WRITE_SECURE_SETTINGS permisison is granted
        mSecureSettingsCategory.isEnabled = isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)

        if (!isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            //uncheck all prefs which require WRITE_SECURE_SETTINGS permission
            for (i in 0 until mSecureSettingsCategory.preferenceCount) {
                val preference = mSecureSettingsCategory.getPreference(i)

                if (preference is SwitchPreference) {
                    preference.isChecked = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        requireContext().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun populateBluetoothDevicesPreference() {
        val pairedDevices = BluetoothUtils.getPairedDevices()

        if (pairedDevices != null) {
            //the user will see the names of the devices
            mBluetoothDevicesPreferences.entries = pairedDevices.map { it.name }.toTypedArray()

            //the unique addresses of the device will be saved to shared preferences
            mBluetoothDevicesPreferences.entryValues =
                pairedDevices.map { it.address }.toTypedArray()
        }
    }

    private fun enableRootPreferences(enabled: Boolean) {
        loop@ for (i in 0 until mRootPrefCategory.preferenceCount) {
            val preference = mRootPrefCategory.getPreference(i)

            when (preference) {
                mRootPermissionPreference -> continue@loop

                else -> {
                    //If disabling the preferences, turn them off.
                    if (!enabled && preference is SwitchPreference) {
                        preference.isChecked = false
                    }
                }
            }

            preference.isEnabled = enabled
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun invalidateAutomaticBackupLocationSummary() {
        val backupLocation = BackupUtils.getAutomaticBackupLocation().valueOrNull()

        if (backupLocation.isNullOrBlank()) {
            mAutomaticBackupLocation?.summary = str(R.string.summary_pref_automatic_backup_location_disabled)
        } else {
            mAutomaticBackupLocation?.summary = backupLocation
        }
    }
}
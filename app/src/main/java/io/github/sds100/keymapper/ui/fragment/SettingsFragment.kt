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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.databinding.FragmentSettingsBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.BluetoothUtils
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.PermissionUtils.isPermissionGranted
import io.github.sds100.keymapper.util.defaultSharedPreferences
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.title
import splitties.resources.appStr

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentSettingsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            requireActivity().onBackPressedDispatcher.addCallback {
                findNavController().navigateUp()
            }

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            return this.root
        }
    }
}

class SettingsPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val mShowImeNotificationPreference by lazy {
        findPreference<Preference>(appStr(R.string.key_pref_show_ime_notification))
    }

    private val mToggleRemappingsNotificationPref by lazy {
        findPreference<SwitchPreference>(appStr(R.string.key_pref_show_toggle_remappings_notification))
    }

    private val mBluetoothDevicesPreferences by lazy {
        findPreference<MultiSelectListPreference>(appStr(R.string.key_pref_bluetooth_devices))!!
    }

    private val mAutoShowIMEDialogPreference by lazy {
        findPreference<SwitchPreference>(appStr(R.string.key_pref_auto_show_ime_picker))
    }

    private val mRootPrefCategory by lazy {
        findPreference<PreferenceCategory>(appStr(R.string.key_pref_category_root))!!
    }

    private val mSecureSettingsCategory by lazy {
        findPreference<PreferenceCategory>(appStr(R.string.key_pref_category_secure_settings))!!
    }

    private val mRootPermissionPreference by lazy {
        findPreference<SwitchPreference>(appStr(R.string.key_pref_root_permission))!!
    }

    private val mNotificationSettingsPreference by lazy {
        findPreference<Preference>(appStr(R.string.key_pref_notification_settings))!!
    }

    private val mDarkThemePreference by lazy {
        findPreference<DropDownPreference>(appStr(R.string.key_pref_dark_theme_mode))
    }

    private var mShowingNoPairedDevicesDialog = false

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
        } else {
            mToggleRemappingsNotificationPref?.onPreferenceChangeListener = this
            mShowImeNotificationPreference?.onPreferenceChangeListener = this
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
        when (preference) {

            mShowImeNotificationPreference -> {
                //show/hide the notification when the preference is toggled
                if (newValue as Boolean) {
                    NotificationUtils.showIMEPickerNotification(requireContext())
                } else {
                    NotificationUtils.dismissNotification(NotificationUtils.ID_IME_PICKER)
                }
            }

            mToggleRemappingsNotificationPref -> {

                if (newValue as Boolean) {
                    WidgetsManager.invalidateNotifications(requireContext())
                } else {
                    //when the user turns this off, resume the remappings because otherwise they can't without
                    //the notification
                    requireContext().sendBroadcast(Intent(MyAccessibilityService.ACTION_RESUME_REMAPPINGS))
                    NotificationUtils.dismissNotification(NotificationUtils.ID_TOGGLE_REMAPS)
                }
            }

            //Only enable the root preferences if the user has enabled root features
            mRootPermissionPreference -> {
                enableRootPreferences(newValue as Boolean)

                //the pending intents need to be updated so they don't use the root methods
                WidgetsManager.invalidateNotifications(requireContext())
            }

            mDarkThemePreference -> {

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
}
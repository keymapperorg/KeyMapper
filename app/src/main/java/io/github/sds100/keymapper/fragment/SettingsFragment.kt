package io.github.sds100.keymapper.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.BluetoothUtils
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.haveWriteSecureSettingsPermission
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.okButton

class SettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val mShowImeNotificationPreference by lazy {
        findPreference<SwitchPreference>(getString(R.string.key_pref_show_ime_notification))!!
    }

    private val mToggleRemappingsNotificationPref by lazy {
        findPreference<SwitchPreference>(getString(R.string.key_pref_show_toggle_remappings_notification))!!
    }

    private val mBluetoothDevicesPreferences by lazy {
        findPreference<MultiSelectListPreference>(getString(R.string.key_pref_bluetooth_devices))!!
    }

    private val mAutoShowIMEDialogPreference by lazy {
        findPreference<SwitchPreference>(getString(R.string.key_pref_auto_show_ime_picker))!!
    }

    private val mRootPrefCategory by lazy {
        findPreference<PreferenceCategory>(getString(R.string.key_pref_category_root))!!
    }

    private val mSecureSettingsCategory by lazy {
        findPreference<PreferenceCategory>(getString(R.string.key_pref_category_secure_settings))!!
    }

    private val mRootPermissionPreference by lazy {
        findPreference<SwitchPreference>(getString(R.string.key_pref_root_permission))!!
    }

    private var mShowingNoPairedDevicesDialog = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        mBluetoothDevicesPreferences.setOnPreferenceClickListener {
            populateBluetoothDevicesPreference()

            //if there are no bluetooth device entries, explain to the user why
            if (mBluetoothDevicesPreferences.entries.isEmpty()) {

                /* This awkward way of showing the "can't find any paired devices" dialog
                 * with a CancellableMultiSelectPreference is necessary since you can't
                 * cancel showing the dialog once the preference has been clicked.*/

                if (!mShowingNoPairedDevicesDialog) {
                    mShowingNoPairedDevicesDialog = true

                    context!!.alert {
                        title = getString(R.string.dialog_title_cant_find_paired_devices)
                        message = getString(R.string.dialog_message_cant_find_paired_devices)
                        okButton { dialog ->
                            mShowingNoPairedDevicesDialog = false
                            dialog.dismiss()
                        }

                        //if the dialog is closed by clicking outside the dialog
                        onCancelled { mShowingNoPairedDevicesDialog = false }
                    }.show()
                }

                return@setOnPreferenceClickListener false
            }

            true
        }

        enableRootPreferences(mRootPermissionPreference.isChecked)

        mAutoShowIMEDialogPreference.onPreferenceChangeListener = this
        mShowImeNotificationPreference.onPreferenceChangeListener = this
        mRootPermissionPreference.onPreferenceChangeListener = this
        mToggleRemappingsNotificationPref.onPreferenceChangeListener = this

        context!!.defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
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
                    NotificationUtils.showIMEPickerNotification(context!!)
                } else {
                    NotificationUtils.dismissNotification(context!!, NotificationUtils.ID_IME_PERSISTENT)
                }
            }

            mToggleRemappingsNotificationPref -> {

                if (newValue as Boolean) {
                    WidgetsManager.invalidateNotification(context!!)
                } else {
                    //when the user turns this off, resume the remappings because otherwise they can't without
                    //the notification
                    context!!.sendBroadcast(Intent(MyAccessibilityService.ACTION_RESUME_REMAPPINGS))
                    NotificationUtils.dismissNotification(context!!, NotificationUtils.ID_TOGGLE_REMAPPING_PERSISTENT)
                }
            }

            //Only enable the root preferences if the user has enabled root features
            mRootPermissionPreference -> {
                enableRootPreferences(newValue as Boolean)

                //the pending intents need to be updated so they don't use the root methods
                WidgetsManager.invalidateNotification(context!!)
            }
        }

        return true
    }

    override fun onResume() {
        super.onResume()

        //only enable the WRITE_SECURE_SETTINGS prefs if WRITE_SECURE_SETTINGS permisison is granted
        mSecureSettingsCategory.isEnabled = context!!.haveWriteSecureSettingsPermission

        if (!context!!.haveWriteSecureSettingsPermission) {
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

        context!!.defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
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
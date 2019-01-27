package io.github.sds100.keymapper.Fragment

import android.os.Bundle
import androidx.preference.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.BluetoothUtils
import io.github.sds100.keymapper.Utils.NotificationUtils
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private val mShowNotificationPreference by lazy {
        findPreference(getString(R.string.key_pref_show_notification)) as SwitchPreference
    }

    private val mShowNotificationOnBootPreference by lazy {
        findPreference(getString(R.string.key_pref_show_notification_on_boot))
                as SwitchPreference
    }

    private val mBluetoothDevicesPreferences by lazy {
        findPreference(getString(R.string.key_pref_bluetooth_devices)) as MultiSelectListPreference
    }

    private val mAutoShowIMEDialogPreference by lazy {
        findPreference(getString(R.string.key_pref_auto_show_ime_picker)) as SwitchPreference
    }

    private val mRootPrefCategory by lazy {
        findPreference(getString(R.string.key_pref_category_root)) as PreferenceCategory
    }

    private val mEnableRootFeaturesPreference by lazy {
        findPreference(getString(R.string.key_pref_allow_root_features)) as SwitchPreference
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

        enableRootPreferences(mEnableRootFeaturesPreference.isChecked)

        mAutoShowIMEDialogPreference.onPreferenceChangeListener = this
        mShowNotificationPreference.onPreferenceChangeListener = this
        mEnableRootFeaturesPreference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference) {

            /* if the user doesn't want to see the notification, don't allow them
             * to toggle whether it is shown on boot */
            mShowNotificationPreference -> {
                mShowNotificationOnBootPreference.isEnabled = newValue as Boolean

                //show/hide the notification when the preference is toggled
                if (newValue) {
                    NotificationUtils.showIMEPickerNotification(context!!)
                } else {
                    NotificationUtils.hideImePickerNotification(context!!)
                }
            }
        }

        return true
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
        for (i in 0 until mRootPrefCategory.preferenceCount) {
            val preference = mRootPrefCategory.getPreference(i)

            if (preference == mEnableRootFeaturesPreference) continue

            preference.isEnabled = enabled
        }
    }
}
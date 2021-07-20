package io.github.sds100.keymapper.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.ui.CancellableMultiSelectListPreference

/**
 * Created by sds100 on 18/07/2021.
 */
object SettingsUtils {

    fun createChooseDevicesPreference(
        ctx: Context,
        settingsViewModel: SettingsViewModel,
        key: String,
        @StringRes title: Int = R.string.title_pref_choose_devices
    ): Preference {
        return CancellableMultiSelectListPreference(ctx).apply {
            this.key = key

            setTitle(title)
            isSingleLineTitle = false

            setOnPreferenceClickListener { preference ->
                val devicesAdapter = ServiceLocator.devicesAdapter(ctx)
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

                    settingsViewModel.showNoPairedDevicesDialog()

                    return@setOnPreferenceClickListener false
                }

                true
            }
        }
    }
}
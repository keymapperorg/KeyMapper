package io.github.sds100.keymapper.base.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.isEmpty
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.base.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationUtils
import io.github.sds100.keymapper.base.utils.ui.viewLifecycleScope

class ImePickerSettingsFragment : BaseSettingsFragment() {

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
    }

    private fun populatePreferenceScreen() = preferenceScreen.apply {
        // show keyboard picker notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showImePickerNotification.name

                setTitle(R.string.title_pref_show_ime_picker_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_ime_picker_notification)

                setOnPreferenceClickListener {
                    onImePickerNotificationClick()

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

        // auto show keyboard picker
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
                Keys.devicesThatShowImePicker,
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onImePickerNotificationClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !viewModel.isNotificationPermissionGranted()
        ) {
            viewModel.requestNotificationsPermission()
            return
        }

        NotificationUtils.openChannelSettings(
            requireContext(),
            NotificationController.CHANNEL_IME_PICKER,
        )
    }
}

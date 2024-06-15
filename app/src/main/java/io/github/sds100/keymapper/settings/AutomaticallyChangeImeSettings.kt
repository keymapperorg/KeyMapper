package io.github.sds100.keymapper.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.isEmpty
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationUtils
import io.github.sds100.keymapper.util.viewLifecycleScope

/**
 * Created by sds100 on 19/07/2021.
 */
class AutomaticallyChangeImeSettings : BaseSettingsFragment() {

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
        // show on-screen messages when changing keyboards
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.showToastWhenAutoChangingIme.name

            setDefaultValue(PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME)
            isSingleLineTitle = false
            setTitle(R.string.title_pref_show_toast_when_auto_changing_ime)

            addPreference(this)
        }

        // automatically change ime on input focus
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.changeImeOnInputFocus.name

            setDefaultValue(false)
            isSingleLineTitle = false
            setTitle(R.string.title_pref_auto_change_ime_on_input_focus)
            setSummary(R.string.summary_pref_auto_change_ime_on_input_focus)

            addPreference(this)
        }

        // automatically change the keyboard when a bluetooth device (dis)connects
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.changeImeOnDeviceConnect.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_auto_change_ime_on_connection)
            setSummary(R.string.summary_pref_auto_change_ime_on_connection)

            addPreference(this)
        }

        addPreference(
            SettingsUtils.createChooseDevicesPreference(
                requireContext(),
                viewModel,
                Keys.devicesThatChangeIme,
            ),
        )

        // toggle keyboard when toggling key maps
        SwitchPreferenceCompat(requireContext()).apply {
            key = Keys.toggleKeyboardOnToggleKeymaps.name
            setDefaultValue(false)

            isSingleLineTitle = false
            setTitle(R.string.title_pref_toggle_keyboard_on_toggle_keymaps)
            setSummary(R.string.summary_pref_toggle_keyboard_on_toggle_keymaps)

            addPreference(this)
        }

        // toggle keyboard notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // show a preference linking to the notification management screen
            Preference(requireContext()).apply {
                key = Keys.showToggleKeyboardNotification.name

                setTitle(R.string.title_pref_show_toggle_keyboard_notification)
                isSingleLineTitle = false
                setSummary(R.string.summary_pref_show_toggle_keyboard_notification)

                setOnPreferenceClickListener {
                    NotificationUtils.openChannelSettings(
                        requireContext(),
                        NotificationController.CHANNEL_TOGGLE_KEYBOARD,
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
}

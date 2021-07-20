package io.github.sds100.keymapper.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.isEmpty
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.system.leanback.LeanbackUtils
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.DialogUtils
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 18/07/2021.
 */
class Android11BugWorkaroundSettingsFragment : BaseSettingsFragment() {

    companion object {
        private const val KEY_ENABLE_COMPATIBLE_IME = "pref_key_enable_compatible_ime"
        private const val KEY_CHOSE_COMPATIBLE_IME = "pref_key_chose_compatible_ime"
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
    }

    private fun populatePreferenceScreen() = preferenceScreen.apply {
        val isTvDevice = LeanbackUtils.isTvDevice(requireContext())

        SwitchPreference(requireContext()).apply {
            key = Keys.rerouteKeyEvents.name
            setDefaultValue(false)

            setTitle(R.string.title_pref_devices_to_reroute_keyevents)
            setSummary(R.string.summary_pref_devices_to_reroute_keyevents_choose_devices)
            isSingleLineTitle = false

            addPreference(this)
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

            addPreference(this)
        }

        Preference(requireContext()).apply {
            if (isTvDevice) {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_install_leanback_keyboard)
            } else {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_install_gui_keyboard)
            }

            isSingleLineTitle = false

            setOnPreferenceClickListener {
                DialogUtils.getCompatibleOnScreenKeyboardDialog(requireContext()).show()

                true
            }

            addPreference(this)
        }

        Preference(requireContext()).apply {
            key = KEY_ENABLE_COMPATIBLE_IME

            if (isTvDevice) {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_enable_ime_leanback)
            } else {
                setTitle(R.string.title_pref_devices_to_reroute_keyevents_enable_ime_gui)
            }

            isSingleLineTitle = false

            setOnPreferenceClickListener {
                viewModel.onEnableCompatibleImeClick()

                true
            }

            addPreference(this)

            viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
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

            addPreference(this)

            viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.isCompatibleImeChosen.collectLatest { isCompatibleImeChosen ->
                    icon = if (isCompatibleImeChosen) {
                        drawable(R.drawable.ic_outline_check_circle_outline_24)
                    } else {
                        drawable(R.drawable.ic_baseline_error_outline_24)
                    }
                }
            }
        }

        addPreference(
            SettingsUtils.createChooseDevicesPreference(
                requireContext(),
                viewModel,
                Keys.devicesToRerouteKeyEvents.name,
                R.string.title_pref_devices_to_reroute_keyevents_choose_devices
            )
        )

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.rerouteKeyEvents.collectLatest { enabled ->
                for (i in 0 until preferenceCount) {
                    getPreference(i).apply {
                        if (this.key != Keys.rerouteKeyEvents.name) {
                            this.isVisible = enabled
                        }
                    }
                }
            }
        }
    }
}
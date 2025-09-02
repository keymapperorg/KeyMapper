package io.github.sds100.keymapper.base.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.isEmpty
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ChooseAppStoreModel
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.drawable
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.base.utils.ui.viewLifecycleScope
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.system.leanback.LeanbackUtils
import io.github.sds100.keymapper.system.url.UrlUtils
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
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

            setTitle(R.string.title_pref_reroute_keyevents)
            setSummary(R.string.summary_pref_reroute_keyevents)
            isSingleLineTitle = false

            addPreference(this)
        }

        Preference(requireContext()).apply {
            setTitle(R.string.title_pref_devices_to_reroute_keyevents_guide)
            setOnPreferenceClickListener {
                UrlUtils.openUrl(
                    requireContext(),
                    str(R.string.url_android_11_bug_reset_id_work_around_setting_guide),
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
                viewLifecycleScope.launchWhenResumed {
                    if (isTvDevice) {
                        val chooseAppStoreDialog = DialogModel.ChooseAppStore(
                            title = getString(R.string.dialog_title_choose_download_leanback_keyboard),
                            message = getString(R.string.dialog_message_choose_download_leanback_keyboard),
                            model = ChooseAppStoreModel(
                                githubLink = getString(R.string.url_github_keymapper_leanback_keyboard),
                            ),
                            negativeButtonText = str(R.string.neg_cancel),
                        )

                        viewModel.showDialog("download_leanback_ime", chooseAppStoreDialog)
                    } else {
                        val chooseAppStoreDialog = DialogModel.ChooseAppStore(
                            title = getString(R.string.dialog_title_choose_download_gui_keyboard),
                            message = getString(R.string.dialog_message_choose_download_gui_keyboard),
                            model = ChooseAppStoreModel(
                                playStoreLink = getString(R.string.url_play_store_keymapper_gui_keyboard),
                                fdroidLink = getString(R.string.url_fdroid_keymapper_gui_keyboard),
                                githubLink = getString(R.string.url_github_keymapper_gui_keyboard),
                            ),
                            negativeButtonText = str(R.string.neg_cancel),
                        )

                        viewModel.showDialog("download_gui_keyboard", chooseAppStoreDialog)
                    }
                }

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
                Keys.devicesToRerouteKeyEvents,
                R.string.title_pref_devices_to_reroute_keyevents_choose_devices,
            ),
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

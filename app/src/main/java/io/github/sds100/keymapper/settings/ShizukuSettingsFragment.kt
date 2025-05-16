package io.github.sds100.keymapper.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.isEmpty
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn


class ShizukuSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = viewModel.sharedPrefsDataStoreWrapper
        setPreferencesFromResource(R.xml.preferences_empty, rootKey)
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
        // summary
        Preference(requireContext()).apply {
            setSummary(R.string.summary_pref_category_shizuku_follow_steps)
            addPreference(this)
        }

        // install shizuku
        Preference(requireContext()).apply {
            viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.isShizukuInstalled.collectLatest { isInstalled ->
                    if (isInstalled) {
                        icon = drawable(R.drawable.ic_outline_check_circle_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_install_app_installed)
                        isEnabled = false
                    } else {
                        icon = drawable(R.drawable.ic_baseline_error_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_install_app_not_installed)
                        isEnabled = true
                    }
                }
            }

            isSingleLineTitle = false

            setOnPreferenceClickListener {
                if (!viewModel.isShizukuInstalled.value) {
                    viewModel.downloadShizuku()
                }

                true
            }

            addPreference(this)
        }

        // start shizuku
        Preference(requireContext()).apply {
            viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    viewModel.isShizukuInstalled,
                    viewModel.isShizukuStarted,
                ) { isInstalled, isStarted ->
                    isEnabled = isInstalled

                    if (isStarted) {
                        icon = drawable(R.drawable.ic_outline_check_circle_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_started)
                    } else {
                        icon = drawable(R.drawable.ic_baseline_error_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_not_started)
                    }
                }.launchIn(this)
            }

            isSingleLineTitle = false

            setOnPreferenceClickListener {
                if (!viewModel.isShizukuStarted.value) {
                    viewModel.openShizukuApp()
                }

                true
            }

            addPreference(this)
        }

        // grant shizuku permission
        Preference(requireContext()).apply {
            viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    viewModel.isShizukuStarted,
                    viewModel.isShizukuPermissionGranted,
                ) { isStarted, isGranted ->
                    isEnabled = isStarted

                    if (isGranted) {
                        icon = drawable(R.drawable.ic_outline_check_circle_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_granted)
                    } else {
                        icon = drawable(R.drawable.ic_baseline_error_outline_24)
                        setTitle(R.string.title_pref_grant_shizuku_not_granted)
                    }
                }.launchIn(this)
            }

            isSingleLineTitle = false

            setOnPreferenceClickListener {
                if (viewModel.isShizukuPermissionGranted.value) {
                    UrlUtils.openUrl(requireContext(), str(R.string.url_shizuku_setting_benefits))
                } else {
                    viewModel.requestShizukuPermission()
                }

                true
            }

            addPreference(this)
        }
    }
}

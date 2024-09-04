package io.github.sds100.keymapper.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.isEmpty
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 16/07/2021.
 */
class DefaultOptionsSettingsFragment : BaseSettingsFragment() {

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

        // these must all start after the preference screen has been populated so that findPreference works.
        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultLongPressDelay.collectLatest { value ->
                val preference = findPreference<SeekBarPreference>(Keys.defaultLongPressDelay.name)
                    ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultDoublePressDelay.collectLatest { value ->
                val preference =
                    findPreference<SeekBarPreference>(Keys.defaultDoublePressDelay.name)
                        ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultSequenceTriggerTimeout.collectLatest { value ->
                val preference =
                    findPreference<SeekBarPreference>(Keys.defaultSequenceTriggerTimeout.name)
                        ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultRepeatRate.collectLatest { value ->
                val preference = findPreference<SeekBarPreference>(Keys.defaultRepeatRate.name)
                    ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultRepeatDelay.collectLatest { value ->
                val preference = findPreference<SeekBarPreference>(Keys.defaultRepeatDelay.name)
                    ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.defaultVibrateDuration.collectLatest { value ->
                val preference = findPreference<SeekBarPreference>(Keys.defaultVibrateDuration.name)
                    ?: return@collectLatest

                if (preference.value != value) {
                    preference.value = value
                }
            }
        }
    }

    private fun populatePreferenceScreen() = preferenceScreen.apply {
        // long press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultLongPressDelay.name
            setDefaultValue(PreferenceDefaults.LONG_PRESS_DELAY)

            setTitle(R.string.title_pref_long_press_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_long_press_delay)
            min = OptionMinimums.TRIGGER_LONG_PRESS_DELAY
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }

        // double press delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultDoublePressDelay.name
            setDefaultValue(PreferenceDefaults.DOUBLE_PRESS_DELAY)

            setTitle(R.string.title_pref_double_press_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_double_press_delay)
            min = OptionMinimums.TRIGGER_DOUBLE_PRESS_DELAY
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }

        // vibration duration
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultVibrateDuration.name
            setDefaultValue(PreferenceDefaults.VIBRATION_DURATION)

            setTitle(R.string.title_pref_vibration_duration)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_vibration_duration)
            min = OptionMinimums.VIBRATION_DURATION
            max = 1000
            showSeekBarValue = true

            addPreference(this)
        }

        // repeat delay
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultRepeatDelay.name
            setDefaultValue(PreferenceDefaults.REPEAT_DELAY)

            setTitle(R.string.title_pref_repeat_delay)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_repeat_delay)
            min = OptionMinimums.ACTION_REPEAT_DELAY
            max = SliderMaximums.ACTION_REPEAT_DELAY
            showSeekBarValue = true

            addPreference(this)
        }

        // repeat rate
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultRepeatRate.name
            setDefaultValue(PreferenceDefaults.REPEAT_RATE)

            setTitle(R.string.title_pref_repeat_rate)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_repeat_rate)
            min = OptionMinimums.ACTION_REPEAT_RATE
            max = SliderMaximums.ACTION_REPEAT_RATE
            showSeekBarValue = true

            addPreference(this)
        }

        // sequence trigger timeout
        SeekBarPreference(requireContext()).apply {
            key = Keys.defaultSequenceTriggerTimeout.name
            setDefaultValue(PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT)

            setTitle(R.string.title_pref_sequence_trigger_timeout)
            isSingleLineTitle = false
            setSummary(R.string.summary_pref_sequence_trigger_timeout)
            min = OptionMinimums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT
            max = 5000
            showSeekBarValue = true

            addPreference(this)
        }

        Preference(requireContext()).apply {
            setTitle(R.string.title_pref_reset_defaults)

            setOnPreferenceClickListener {
                viewModel.resetDefaultMappingOptions()
                true
            }

            addPreference(this)
        }
    }
}

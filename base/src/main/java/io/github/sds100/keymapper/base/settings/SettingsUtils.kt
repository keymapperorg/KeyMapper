package io.github.sds100.keymapper.base.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import androidx.preference.Preference
import io.github.sds100.keymapper.base.R

object SettingsUtils {

    fun createChooseDevicesPreference(
        ctx: Context,
        settingsViewModel: SettingsViewModel,
        key: Preferences.Key<Set<String>>,
        @StringRes title: Int = R.string.title_pref_choose_devices,
    ): Preference = Preference(ctx).apply {
        this.key = key.name

        setTitle(title)
        isSingleLineTitle = false

        setOnPreferenceClickListener {
            settingsViewModel.chooseDevicesForPreference(key)

            true
        }
    }
}

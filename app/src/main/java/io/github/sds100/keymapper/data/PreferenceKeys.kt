package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.preferencesKey

/**
 * Created by sds100 on 19/01/21.
 */
object PreferenceKeys {
    val darkTheme = preferencesKey<String>("pref_dark_theme_mode")
    val appIntro = preferencesKey<Boolean>("pref_first_time")
    val approvedFingerprintFeaturePrompt =
        preferencesKey<Boolean>("pref_approved_fingerprint_feature_prompt")
}